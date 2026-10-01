package com.example.happyhunt.data

import com.example.happyhunt.domain.Area
import com.example.happyhunt.domain.GeoPoint
import com.example.happyhunt.domain.PhotonParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale

/**
 * Finds neighbourhoods, towns and addresses with Photon, the OpenStreetMap
 * search run by komoot. Nothing but the typed text and a rough position (to
 * rank nearby matches first) is sent.
 */
class AreasRepository(
    private val client: OkHttpClient,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val labels = HashMap<String, String?>()

    /** Matches for what was typed, the closest to [near] first; empty when nothing matches, null when the search failed. */
    suspend fun search(query: String, near: GeoPoint?): List<Area>? = withContext(io) {
        val url = BASE.toHttpUrl().newBuilder().addPathSegments("api/")
            .addQueryParameter("q", query)
            .addQueryParameter("limit", "8")
            .addQueryParameter("lang", language())
            .apply {
                if (near != null) {
                    addQueryParameter("lat", coordinate(near.lat))
                    addQueryParameter("lon", coordinate(near.lon))
                }
            }
            .build()
        get(url.toString())?.let(PhotonParser::areas)
    }

    /** "Trinity-Bellwoods, Toronto" for a point, or null when it cannot be named. */
    suspend fun label(point: GeoPoint): String? = withContext(io) {
        val key = "${coordinate(point.lat)},${coordinate(point.lon)}"
        synchronized(labels) { if (key in labels) return@withContext labels[key] }
        val url = BASE.toHttpUrl().newBuilder().addPathSegments("reverse")
            .addQueryParameter("lat", coordinate(point.lat))
            .addQueryParameter("lon", coordinate(point.lon))
            .addQueryParameter("lang", language())
            .build()
        val body = get(url.toString()) ?: return@withContext null
        PhotonParser.label(body).also { synchronized(labels) { labels[key] = it } }
    }

    private suspend fun get(url: String): String? = try {
        client.newCall(Request.Builder().url(url).build()).await().use { response ->
            if (response.isSuccessful) response.body.string() else null
        }
    } catch (_: IOException) {
        null
    }

    /** Photon names places in English, German, French or Italian; anything else gets the local names. */
    private fun language() = Locale.getDefault().language.takeIf { it in setOf("en", "de", "fr", "it") } ?: "default"

    private fun coordinate(value: Double) = String.format(Locale.ROOT, "%.5f", value)

    private companion object {
        const val BASE = "https://photon.komoot.io/"
    }
}
