package com.example.happyhunt.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** A freely licensed photo of a place, and the page that credits its author. */
data class Photo(val url: String, val creditUrl: String)

/**
 * Many museums, parks and landmarks are linked to Wikidata, which often
 * names a photo of them on Wikimedia Commons. Restaurants and playgrounds
 * rarely are; they get an illustrated banner instead.
 */
class PhotosRepository(
    private val client: OkHttpClient,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val found = HashMap<String, Photo?>()

    suspend fun photo(wikidata: String): Photo? = withContext(io) {
        synchronized(found) { if (wikidata in found) return@withContext found[wikidata] }
        val url = "https://www.wikidata.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "wbgetclaims")
            .addQueryParameter("entity", wikidata)
            .addQueryParameter("property", "P18")
            .addQueryParameter("format", "json")
            .build()
        val body = try {
            client.newCall(Request.Builder().url(url).build()).await().use { if (it.isSuccessful) it.body.string() else null }
        } catch (_: IOException) {
            null
        } ?: return@withContext null
        val file = runCatching {
            json.parseToJsonElement(body).jsonObject["claims"]!!.jsonObject["P18"]!!.jsonArray.first()
                .jsonObject["mainsnak"]!!.jsonObject["datavalue"]!!.jsonObject["value"]!!.jsonPrimitive.contentOrNull
        }.getOrNull()
        val photo = file?.let(::photoFor)
        synchronized(found) { found[wikidata] = photo }
        photo
    }

    companion object {
        /** Commons serves standard widths; 960 px is sharp on a phone at about half the download of the next size up. */
        fun photoFor(file: String): Photo {
            val name = file.replace(' ', '_')
            val path = "https://commons.wikimedia.org/wiki/".toHttpUrl().newBuilder()
            return Photo(
                url = path.addPathSegment("Special:FilePath").addPathSegment(name).addQueryParameter("width", "960").build().toString(),
                creditUrl = "https://commons.wikimedia.org/wiki/".toHttpUrl().newBuilder().addPathSegment("File:$name").build().toString(),
            )
        }
    }
}
