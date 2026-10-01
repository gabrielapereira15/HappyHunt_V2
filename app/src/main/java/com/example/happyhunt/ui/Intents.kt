package com.example.happyhunt.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.example.happyhunt.domain.Place
import java.util.Locale

/** Opening other apps for a place. Each returns false when no app on the phone can do it. */
object Intents {
    fun directions(context: Context, place: Place, name: String): Boolean {
        val (lat, lon) = coordinates(place)
        val geo = Intent(Intent.ACTION_VIEW, "geo:$lat,$lon?q=$lat,$lon(${Uri.encode(name)})".toUri())
        // Without a maps app, the place on openstreetmap.org has directions too.
        return open(context, geo) || web(context, "https://www.openstreetmap.org/directions?to=$lat%2C$lon")
    }

    fun call(context: Context, phone: String): Boolean =
        open(context, Intent(Intent.ACTION_DIAL, "tel:${phone.filter { it.isDigit() || it == '+' }}".toUri()))

    fun web(context: Context, url: String): Boolean = open(context, Intent(Intent.ACTION_VIEW, url.toUri()))

    fun share(context: Context, place: Place, name: String): Boolean {
        val text = listOfNotNull(name, place.address, place.osmUrl).joinToString("\n")
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, name)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return open(context, Intent.createChooser(send, null))
    }

    private fun coordinates(place: Place) =
        String.format(Locale.ROOT, "%.6f", place.point.lat) to String.format(Locale.ROOT, "%.6f", place.point.lon)

    private fun open(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
