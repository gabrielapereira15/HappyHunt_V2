package com.example.happyhunt.domain

import kotlinx.serialization.Serializable

/** The six kinds of outing the app looks for. */
enum class Category { EAT, TREATS, PARKS, PLAYGROUNDS, ATTRACTIONS, CULTURE }

/**
 * What a place is, read from its OpenStreetMap tags. Listed in order of
 * precedence: a museum with a café in it is a museum, and a park that is also
 * tagged as an attraction is a park.
 */
enum class Kind(val category: Category, vararg val tags: Pair<String, String>) {
    MUSEUM(Category.CULTURE, "tourism" to "museum"),
    GALLERY(Category.CULTURE, "tourism" to "gallery"),
    ZOO(Category.ATTRACTIONS, "tourism" to "zoo"),
    AQUARIUM(Category.ATTRACTIONS, "tourism" to "aquarium"),
    THEME_PARK(Category.ATTRACTIONS, "tourism" to "theme_park"),
    WATER_PARK(Category.ATTRACTIONS, "leisure" to "water_park"),
    PLAYGROUND(Category.PLAYGROUNDS, "leisure" to "playground"),
    PARK(Category.PARKS, "leisure" to "park"),
    GARDEN(Category.PARKS, "leisure" to "garden"),
    NATURE_RESERVE(Category.PARKS, "leisure" to "nature_reserve"),
    RESTAURANT(Category.EAT, "amenity" to "restaurant"),
    FAST_FOOD(Category.EAT, "amenity" to "fast_food"),
    FOOD_COURT(Category.EAT, "amenity" to "food_court"),
    CAFE(Category.TREATS, "amenity" to "cafe"),
    ICE_CREAM(Category.TREATS, "amenity" to "ice_cream", "shop" to "ice_cream"),
    BAKERY(Category.TREATS, "shop" to "bakery"),
    PASTRY(Category.TREATS, "shop" to "pastry"),
    SWEETS(Category.TREATS, "shop" to "confectionery", "shop" to "chocolate"),
    ARTS_CENTRE(Category.CULTURE, "amenity" to "arts_centre"),
    THEATRE(Category.CULTURE, "amenity" to "theatre"),
    CINEMA(Category.CULTURE, "amenity" to "cinema"),
    MINI_GOLF(Category.ATTRACTIONS, "leisure" to "miniature_golf"),
    BOWLING(Category.ATTRACTIONS, "leisure" to "bowling_alley"),
    ARCADE(Category.ATTRACTIONS, "leisure" to "amusement_arcade"),
    VIEWPOINT(Category.ATTRACTIONS, "tourism" to "viewpoint"),
    ATTRACTION(Category.ATTRACTIONS, "tourism" to "attraction");

    companion object {
        fun of(tags: Map<String, String>): Kind? = entries.firstOrNull { kind -> kind.tags.any { (key, value) -> tags[key] == value } }
    }
}

/** Something worth knowing before going, read from the tags. */
enum class Highlight { WHEELCHAIR, PARTLY_WHEELCHAIR, FREE, OUTDOOR_SEATING, VEGAN, VEGETARIAN, TAKEAWAY, DOGS, CHANGING_TABLE, TOILETS, WIFI }

/**
 * A place from OpenStreetMap, with only the tags the app shows. It is
 * serializable so a saved place keeps a full copy of itself: it still shows
 * offline, and still shows if it is ever removed from the map.
 */
@Serializable
data class Place(
    /** "n123", "w123" or "r123": OpenStreetMap ids are only unique within each element type. */
    val id: String,
    /** Null for the many playgrounds that have no name. */
    val name: String?,
    val kind: Kind,
    val point: GeoPoint,
    val tags: Map<String, String> = emptyMap(),
) {
    val category: Category get() = kind.category

    /** "Pizza · Italian", at most two, from cuisine=pizza;italian. */
    val cuisines: List<String>
        get() = tags["cuisine"].orEmpty().split(';')
            .map { it.trim().replace('_', ' ') }
            .filter { it.isNotEmpty() && it != "yes" }
            .map { it.replaceFirstChar(Char::uppercaseChar) }
            .take(2)

    /** "123 Queen Street West, Toronto", or as much of it as is known. */
    val address: String?
        get() {
            val street = listOfNotNull(tags["addr:housenumber"], tags["addr:street"]).joinToString(" ").ifBlank { null }
            return listOfNotNull(street, tags["addr:city"]).joinToString(", ").ifBlank { null }
        }

    val phone: String? get() = (tags["phone"] ?: tags["contact:phone"])?.split(';')?.first()?.trim()?.ifBlank { null }

    val website: String?
        get() = (tags["website"] ?: tags["contact:website"])?.split(';')?.first()?.trim()?.ifBlank { null }
            ?.let { if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it" }

    val openingHours: String? get() = tags["opening_hours"]?.ifBlank { null }

    /** Only the place's own Wikidata item; brand:wikidata is the chain's logo, not the place. */
    val wikidata: String? get() = tags["wikidata"]?.takeIf { it.matches(Regex("Q\\d+")) }

    val highlights: List<Highlight>
        get() = buildList {
            when (tags["wheelchair"]) {
                "yes", "designated" -> add(Highlight.WHEELCHAIR)
                "limited" -> add(Highlight.PARTLY_WHEELCHAIR)
            }
            if (tags["fee"] == "no") add(Highlight.FREE)
            if (tags["outdoor_seating"] == "yes") add(Highlight.OUTDOOR_SEATING)
            when {
                tags["diet:vegan"] in setOf("yes", "only") -> add(Highlight.VEGAN)
                tags["diet:vegetarian"] in setOf("yes", "only") -> add(Highlight.VEGETARIAN)
            }
            if (tags["takeaway"] in setOf("yes", "only")) add(Highlight.TAKEAWAY)
            if (tags["dog"] in setOf("yes", "leashed")) add(Highlight.DOGS)
            if (tags["changing_table"] == "yes") add(Highlight.CHANGING_TABLE)
            if (tags["toilets"] == "yes") add(Highlight.TOILETS)
            if (tags["internet_access"] in setOf("wlan", "yes", "wifi")) add(Highlight.WIFI)
        }

    /** "Ages 2–12" for playgrounds that say so. */
    val ages: IntRange?
        get() {
            val min = tags["min_age"]?.toIntOrNull()
            val max = tags["max_age"]?.toIntOrNull()
            return if (min == null && max == null) null else (min ?: 0)..(max ?: 99)
        }

    /** The page for this place on openstreetmap.org, where anyone can correct it. */
    val osmUrl: String
        get() {
            val type = when (id.first()) {
                'n' -> "node"
                'w' -> "way"
                else -> "relation"
            }
            return "https://www.openstreetmap.org/$type/${id.drop(1)}"
        }
}

object OsmPlaces {
    /** The tags worth keeping; the rest of an element's tags are dropped to keep caches and saved places small. */
    private val kept = setOf(
        "name", "cuisine", "opening_hours", "phone", "contact:phone", "website", "contact:website",
        "addr:housenumber", "addr:street", "addr:city", "wheelchair", "outdoor_seating", "diet:vegan",
        "diet:vegetarian", "dog", "fee", "takeaway", "internet_access", "changing_table", "toilets",
        "wikidata", "min_age", "max_age", "description",
    )

    /**
     * Turns an OpenStreetMap element into a place, or null when it is not one
     * worth showing: private, unnamed (playgrounds excepted), or of no kind
     * the app knows.
     */
    fun place(type: String, id: Long, point: GeoPoint, tags: Map<String, String>): Place? {
        val kind = Kind.of(tags) ?: return null
        if (tags["access"] in setOf("private", "no")) return null
        val name = tags["name"]?.trim()?.ifBlank { null }
        if (name == null && kind != Kind.PLAYGROUND) return null
        val prefix = when (type) {
            "node" -> "n"
            "way" -> "w"
            "relation" -> "r"
            else -> return null
        }
        return Place("$prefix$id", name, kind, point, tags.filterKeys { it in kept })
    }
}
