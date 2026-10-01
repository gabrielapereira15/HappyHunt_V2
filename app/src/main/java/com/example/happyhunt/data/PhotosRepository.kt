package com.example.happyhunt.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * A freely licensed photo of a place. [author] and [license] are what its
 * licence asks to be shown with it; [creditUrl] is its page on Commons.
 */
data class Photo(val url: String, val creditUrl: String, val author: String? = null, val license: String? = null)

/**
 * Many museums, parks and landmarks are linked to Wikidata, which often
 * names a photo of them on Wikimedia Commons. Restaurants and playgrounds
 * rarely are; they get an illustrated banner instead.
 */
class PhotosRepository(
    private val client: OkHttpClient,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val found = HashMap<String, Photo?>()

    suspend fun photo(wikidata: String): Photo? = withContext(io) {
        synchronized(found) { if (wikidata in found) return@withContext found[wikidata] }
        val claims = get(
            "https://www.wikidata.org/w/api.php".toHttpUrl().newBuilder()
                .addQueryParameter("action", "wbgetclaims")
                .addQueryParameter("entity", wikidata)
                .addQueryParameter("property", "P18")
                .addQueryParameter("format", "json")
                .build(),
        ) ?: return@withContext null
        val file = fileFromClaims(claims)
        // The photo's own page gives a thumbnail and who to credit for it, in one answer.
        val photo = file?.let { name ->
            get(
                "https://commons.wikimedia.org/w/api.php".toHttpUrl().newBuilder()
                    .addQueryParameter("action", "query")
                    .addQueryParameter("titles", "File:$name")
                    .addQueryParameter("prop", "imageinfo")
                    .addQueryParameter("iiprop", "url|extmetadata")
                    .addQueryParameter("iiurlwidth", WIDTH.toString())
                    .addQueryParameter("iiextmetadatafilter", "Artist|LicenseShortName")
                    .addQueryParameter("format", "json")
                    .addQueryParameter("formatversion", "2")
                    .build(),
            )?.let(::photoFromImageInfo) ?: photoFor(name)
        }
        synchronized(found) { found[wikidata] = photo }
        photo
    }

    private suspend fun get(url: HttpUrl): String? = try {
        client.newCall(Request.Builder().url(url).build()).await().use { if (it.isSuccessful) it.body.string() else null }
    } catch (_: IOException) {
        null
    }

    companion object {
        /** Commons serves standard widths; 960 px is sharp on a phone at about half the download of the next size up. */
        private const val WIDTH = 960
        private val json = Json { ignoreUnknownKeys = true }

        /** The photo named on a Wikidata item (property P18), if it has one. */
        fun fileFromClaims(body: String): String? = runCatching {
            json.parseToJsonElement(body).jsonObject["claims"]!!.jsonObject["P18"]!!.jsonArray.first()
                .jsonObject["mainsnak"]!!.jsonObject["datavalue"]!!.jsonObject["value"]!!.jsonPrimitive.contentOrNull
        }.getOrNull()

        /** The thumbnail, page, author and licence from a Commons imageinfo answer. */
        fun photoFromImageInfo(body: String): Photo? = runCatching {
            val info = json.parseToJsonElement(body).jsonObject["query"]!!.jsonObject["pages"]!!.jsonArray.first()
                .jsonObject["imageinfo"]!!.jsonArray.first().jsonObject
            val metadata = info["extmetadata"]?.jsonObject
            Photo(
                url = info.text("thumburl") ?: info.text("url")!!,
                creditUrl = info.text("descriptionurl")!!,
                author = metadata?.value("Artist")?.let(::plainText)?.ifBlank { null },
                license = metadata?.value("LicenseShortName")?.let(::plainText)?.ifBlank { null },
            )
        }.getOrNull()

        /** For when the photo's page cannot be read: Commons still serves the file by name. */
        fun photoFor(file: String): Photo {
            val name = file.replace(' ', '_')
            return Photo(
                url = "https://commons.wikimedia.org/wiki/".toHttpUrl().newBuilder()
                    .addPathSegment("Special:FilePath").addPathSegment(name).addQueryParameter("width", WIDTH.toString())
                    .build().toString(),
                creditUrl = "https://commons.wikimedia.org/wiki/".toHttpUrl().newBuilder().addPathSegment("File:$name").build().toString(),
            )
        }

        /** Commons gives the author as HTML, usually a link to their user page. */
        private fun plainText(html: String): String = html
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        private fun JsonObject.text(key: String) = this[key]?.jsonPrimitive?.contentOrNull
        private fun JsonObject.value(key: String) = this[key]?.jsonObject?.text("value")
    }
}
