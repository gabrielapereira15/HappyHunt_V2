package com.example.happyhunt.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A place to hunt around: a neighbourhood, a city, a park, an address. */
@Serializable
data class Area(
    val name: String,
    /** "Toronto, Ontario, Canada": what tells two areas with the same name apart. */
    val detail: String?,
    val point: GeoPoint,
)

/** Reads answers from Photon, the OpenStreetMap search by komoot. */
object PhotonParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun areas(body: String): List<Area> = features(body).mapNotNull { feature ->
        val props = feature["properties"]?.jsonObject ?: return@mapNotNull null
        val point = point(feature) ?: return@mapNotNull null
        val name = props.text("name") ?: listOfNotNull(props.text("housenumber"), props.text("street")).joinToString(" ").ifBlank { null }
            ?: return@mapNotNull null
        val detail = listOf("district", "locality", "city", "state", "country")
            .mapNotNull { props.text(it) }
            .filter { it != name }
            .distinct()
            .take(3)
            .joinToString(", ")
            .ifBlank { null }
        Area(name, detail, point)
    }.distinctBy { it.name to it.detail }

    /**
     * A short name for where a point is, from a reverse lookup: the
     * neighbourhood and the city ("Little Portugal, Toronto"), or whichever of
     * them is known.
     */
    fun label(body: String): String? {
        val props = features(body).firstOrNull()?.get("properties")?.jsonObject ?: return null
        val near = props.text("district") ?: props.text("locality")
        val city = props.text("city") ?: props.text("county")
        return listOfNotNull(near, city).distinct().joinToString(", ").ifBlank { null }
    }

    private fun features(body: String): List<JsonObject> =
        runCatching { json.parseToJsonElement(body).jsonObject["features"]?.jsonArray?.map { it.jsonObject } }.getOrNull().orEmpty()

    private fun point(feature: JsonObject): GeoPoint? {
        val coordinates = feature["geometry"]?.jsonObject?.get("coordinates")?.jsonArray ?: return null
        // GeoJSON order: longitude first.
        val lon = coordinates.getOrNull(0)?.jsonPrimitive?.doubleOrNull ?: return null
        val lat = coordinates.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: return null
        return GeoPoint(lat, lon)
    }

    private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null }
}
