package com.example.happyhunt.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.example.happyhunt.domain.GeoPoint
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * The phone's position, from the platform location service directly, so the
 * app needs no Google Play services. The position is sent to the map
 * services only as the centre of a search, with no account or identifier,
 * and is kept on the phone (not in backups) to reopen the same area.
 */
class Locator(private val context: Context) {
    sealed interface Result {
        data class Found(val point: GeoPoint) : Result
        data object NoPermission : Result
        data object LocationOff : Result
        data object NotFound : Result
    }

    private val manager = context.getSystemService(LocationManager::class.java)
    private val executor = Executors.newSingleThreadExecutor()

    fun hasPermission(): Boolean = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    @SuppressLint("MissingPermission") // Checked by hasPermission().
    suspend fun locate(): Result {
        if (!hasPermission()) return Result.NoPermission
        if (manager == null || !LocationManagerCompat.isLocationEnabled(manager)) return Result.LocationOff

        // A recent fix is good enough to search around and saves waiting for the GPS.
        val recent = providers().mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { ageMillis(it) < RECENT_ENOUGH_MS }
            .minByOrNull { it.accuracy }
        if (recent != null) return Result.Found(recent.toPoint())

        // Ask every provider at once and take the first answer: indoors the network answers in a
        // second while GPS may never; outdoors without a data connection only GPS will.
        val current = withTimeoutOrNull(WAIT_MS) {
            channelFlow { providers().forEach { provider -> launch { current(provider)?.let { send(it) } } } }.firstOrNull()
        }
        if (current != null) return Result.Found(current.toPoint())

        val older = providers().mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { ageMillis(it) < OLD_BUT_USEFUL_MS }
            .maxByOrNull { it.elapsedRealtimeNanos }
        return older?.let { Result.Found(it.toPoint()) } ?: Result.NotFound
    }

    @SuppressLint("MissingPermission")
    private suspend fun current(provider: String): Location? = suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }
        runCatching {
            LocationManagerCompat.getCurrentLocation(manager!!, provider, signal, executor) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
        }.onFailure { if (continuation.isActive) continuation.resume(null) }
    }

    /** The providers that are switched on: fused where the phone has one, network, and GPS. */
    private fun providers(): List<String> {
        val enabled = manager?.getProviders(true).orEmpty()
        val order = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }
        return order.filter { it in enabled }
    }

    private fun ageMillis(location: Location) = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000

    private fun Location.toPoint() = GeoPoint(latitude, longitude)

    private companion object {
        const val RECENT_ENOUGH_MS = 2 * 60 * 1000L
        const val OLD_BUT_USEFUL_MS = 60 * 60 * 1000L
        const val WAIT_MS = 15_000L
    }
}
