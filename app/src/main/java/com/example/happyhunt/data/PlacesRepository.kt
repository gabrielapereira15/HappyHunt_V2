package com.example.happyhunt.data

import com.example.happyhunt.domain.GeoPoint
import com.example.happyhunt.domain.OverpassParser
import com.example.happyhunt.domain.OverpassQuery
import com.example.happyhunt.domain.Place
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Places found around a point, and whether they come from an older search because the servers could not be reached. */
data class Nearby(val places: List<Place>, val fetchedAt: Long, val stale: Boolean)

class PlacesUnavailable(val offline: Boolean) : Exception(if (offline) "No connection" else "The map servers are busy")

/**
 * Finds places with the Overpass API, the free read-only door into
 * OpenStreetMap. Several volunteer servers answer the same queries; when one
 * is busy the next is tried. Each search is kept for a day, on disk too, so
 * going back to an area is instant and still works without a connection.
 */
class PlacesRepository(
    client: OkHttpClient,
    private val cacheDir: File,
    private val servers: List<String> = SERVERS,
    private val now: () -> Long = System::currentTimeMillis,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val client = client.newBuilder().callTimeout(35, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val memory = object : LinkedHashMap<String, Cached>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Cached>) = size > 6
    }

    /** Every place seen this session, so a place screen opens instantly from the list or the map. */
    private val known = ConcurrentHashMap<String, Place>()

    @Serializable
    private data class Cached(val fetchedAt: Long, val places: List<Place>)

    fun known(id: String): Place? = known[id]

    suspend fun around(center: GeoPoint, radiusMeters: Int, refresh: Boolean = false): Nearby = mutex.withLock {
        val key = String.format(Locale.ROOT, "%.4f_%.4f_%d", center.lat, center.lon, radiusMeters)
        val cached = memory[key] ?: readDisk(key)?.also { memory[key] = it }
        if (cached != null && !refresh && now() - cached.fetchedAt < FRESH_FOR_MS) {
            return cached.toNearby(stale = false)
        }
        try {
            val places = fetch(OverpassQuery.around(center, radiusMeters))
            val fresh = Cached(now(), places)
            memory[key] = fresh
            writeDisk(key, fresh)
            fresh.toNearby(stale = false)
        } catch (e: PlacesUnavailable) {
            // An older answer beats no answer: places rarely move.
            cached?.toNearby(stale = true) ?: throw e
        }
    }

    /** One place by id, for a saved place whose details are refreshed when it is opened. */
    suspend fun place(id: String): Place? {
        known[id]?.let { return it }
        val query = OverpassQuery.byId(id) ?: return null
        return fetch(query).firstOrNull()
    }

    private suspend fun fetch(query: String): List<Place> = withContext(io) {
        // "Offline" only when every server failed to even connect; a timeout means busy.
        var offline = true
        var triedAll = false
        val places = withTimeoutOrNull(TOTAL_WAIT_MS) {
            for (server in servers) {
                try {
                    val request = Request.Builder().url(server).post(FormBody.Builder().add("data", query).build()).build()
                    client.newCall(request).await().use { response ->
                        offline = false
                        if (response.isSuccessful) return@withTimeoutOrNull OverpassParser.parse(response.body.string())
                    }
                } catch (e: IOException) {
                    if (!e.isOffline) offline = false
                } catch (_: OverpassParser.NotAnAnswer) {
                    offline = false
                }
            }
            triedAll = true
            null
        } ?: throw PlacesUnavailable(offline && triedAll)
        places.forEach { known[it.id] = it }
        places
    }

    private fun Cached.toNearby(stale: Boolean): Nearby {
        places.forEach { known[it.id] = it }
        return Nearby(places, fetchedAt, stale)
    }

    private suspend fun readDisk(key: String): Cached? = withContext(io) {
        val file = File(directory(), "$key.json")
        runCatching { json.decodeFromString<Cached>(file.readText()) }.getOrNull()
            ?.takeIf { now() - it.fetchedAt < KEEP_FOR_MS }
    }

    private suspend fun writeDisk(key: String, cached: Cached) = withContext(io) {
        runCatching {
            val dir = directory()
            File(dir, "$key.json").writeText(json.encodeToString(cached))
            // Keep the folder small: only the most recent searches.
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(MAX_FILES)?.forEach { it.delete() }
        }
    }

    private fun directory() = File(cacheDir, "places").apply { mkdirs() }

    companion object {
        val SERVERS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://lz4.overpass-api.de/api/interpreter",
            "https://z.overpass-api.de/api/interpreter",
            "https://overpass.private.coffee/api/interpreter",
        )
        private const val FRESH_FOR_MS = 24 * 60 * 60 * 1000L
        private const val KEEP_FOR_MS = 30 * FRESH_FOR_MS
        private const val TOTAL_WAIT_MS = 75_000L
        private const val MAX_FILES = 20
    }
}
