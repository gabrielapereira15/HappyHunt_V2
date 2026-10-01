package com.example.happyhunt.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.Locale

/** Builds the one Overpass query that finds every kind of place around a point. */
object OverpassQuery {
    fun around(center: GeoPoint, radiusMeters: Int, timeoutSeconds: Int = 25): String {
        // Locale.ROOT: in French or German a comma would replace the decimal point and break the query.
        val around = String.format(Locale.ROOT, "around:%d,%.6f,%.6f", radiusMeters, center.lat, center.lon)
        val notPrivate = "[\"access\"!~\"^(private|no)$\"]"
        return """
            [out:json][timeout:$timeoutSeconds];
            (
              nwr($around)["amenity"~"^(restaurant|fast_food|food_court|cafe|ice_cream|arts_centre|theatre|cinema)$"]["name"];
              nwr($around)["shop"~"^(bakery|pastry|confectionery|chocolate|ice_cream)$"]["name"];
              nwr($around)["leisure"~"^(park|garden|nature_reserve|water_park|miniature_golf|bowling_alley|amusement_arcade)$"]["name"]$notPrivate;
              nwr($around)["leisure"="playground"]$notPrivate;
              nwr($around)["tourism"~"^(attraction|theme_park|zoo|aquarium|viewpoint|museum|gallery)$"]["name"];
            );
            out center tags;
        """.trimIndent()
    }

    /** Looks up a single place again, for one opened from a link or after the app was closed. */
    fun byId(placeId: String): String? {
        val type = when (placeId.firstOrNull()) {
            'n' -> "node"
            'w' -> "way"
            'r' -> "relation"
            else -> return null
        }
        val id = placeId.drop(1).toLongOrNull() ?: return null
        return "[out:json][timeout:15];$type($id);out center tags;"
    }
}

/** Turns an Overpass answer into places. Anything that is not the JSON answer (an HTML error page, say) is an error. */
object OverpassParser {
    class NotAnAnswer(message: String) : Exception(message)

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String): List<Place> {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrElse {
            throw NotAnAnswer("Not JSON: ${body.take(80)}")
        }
        val elements = root["elements"]?.jsonArray ?: throw NotAnAnswer("No elements")
        // "remark" carries runtime errors such as a timeout, which come with a partial answer.
        val remark = root["remark"]?.jsonPrimitive?.contentOrNull
        if (remark != null && "error" in remark.lowercase()) throw NotAnAnswer(remark)
        return elements.mapNotNull { element ->
            val obj = element.jsonObject
            val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val id = obj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val point = point(obj) ?: return@mapNotNull null
            val tags = obj["tags"]?.jsonObject?.mapValues { it.value.jsonPrimitive.contentOrNull.orEmpty() }.orEmpty()
            OsmPlaces.place(type, id, point, tags)
        }.distinctBy { it.id }
    }

    /** Nodes carry lat/lon; ways and relations carry the "center" asked for with "out center". */
    private fun point(obj: JsonObject): GeoPoint? {
        val source = obj["center"]?.jsonObject ?: obj
        val lat = source["lat"]?.jsonPrimitive?.doubleOrNull ?: return null
        val lon = source["lon"]?.jsonPrimitive?.doubleOrNull ?: return null
        return GeoPoint(lat, lon)
    }
}
