package com.example.happyhunt.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlacesTest {
    private val here = GeoPoint(43.6465, -79.4197)

    private fun place(vararg tags: Pair<String, String>, type: String = "node"): Place? =
        OsmPlaces.place(type, 1, here, mapOf(*tags))

    private fun named(vararg tags: Pair<String, String>): Place = place("name" to "Somewhere", *tags)!!

    @Test
    fun `kinds follow precedence`() {
        assertEquals(Kind.MUSEUM, Kind.of(mapOf("tourism" to "museum", "amenity" to "cafe")))
        assertEquals(Kind.PARK, Kind.of(mapOf("leisure" to "park", "tourism" to "attraction")))
        assertEquals(Kind.SWEETS, Kind.of(mapOf("shop" to "chocolate")))
        assertEquals(Kind.ICE_CREAM, Kind.of(mapOf("shop" to "ice_cream")))
        assertNull(Kind.of(mapOf("amenity" to "bank")))
    }

    @Test
    fun `kinds land in their category`() {
        assertEquals(Category.TREATS, named("amenity" to "cafe").category)
        assertEquals(Category.EAT, named("amenity" to "fast_food").category)
        assertEquals(Category.PLAYGROUNDS, named("leisure" to "playground").category)
        assertEquals(Category.CULTURE, named("amenity" to "cinema").category)
        assertEquals(Category.ATTRACTIONS, named("leisure" to "miniature_golf").category)
    }

    @Test
    fun `places worth showing`() {
        assertNull("private", place("name" to "Club", "leisure" to "park", "access" to "private"))
        assertNull("unnamed restaurant", place("amenity" to "restaurant"))
        assertNull("unknown element type", place("name" to "X", "amenity" to "cafe", type = "area"))
        val playground = place("leisure" to "playground")!!
        assertNull(playground.name)
        assertEquals("n1", playground.id)
        assertEquals("w1", place("name" to "Park", "leisure" to "park", type = "way")!!.id)
        assertEquals("r1", place("name" to "Park", "leisure" to "park", type = "relation")!!.id)
    }

    @Test
    fun `only useful tags are kept`() {
        val place = named("amenity" to "cafe", "source" to "survey", "check_date" to "2024-01-01", "cuisine" to "coffee_shop")
        // The kind is kept on its own, so the tag that gave it is not needed either.
        assertEquals(setOf("name", "cuisine"), place.tags.keys)
    }

    @Test
    fun `cuisines read nicely`() {
        assertEquals(listOf("Pizza", "Italian"), named("amenity" to "restaurant", "cuisine" to "pizza;italian;burger").cuisines)
        assertEquals(listOf("Coffee shop"), named("amenity" to "cafe", "cuisine" to "coffee_shop").cuisines)
        assertEquals(emptyList<String>(), named("amenity" to "cafe", "cuisine" to "yes").cuisines)
    }

    @Test
    fun `contact details`() {
        val place = named(
            "amenity" to "restaurant",
            "phone" to "+1 416 555 0100; +1 416 555 0101",
            "contact:website" to "example.com",
            "addr:housenumber" to "12",
            "addr:street" to "Queen Street West",
            "addr:city" to "Toronto",
        )
        assertEquals("+1 416 555 0100", place.phone)
        assertEquals("https://example.com", place.website)
        assertEquals("12 Queen Street West, Toronto", place.address)
        assertEquals("Toronto", named("amenity" to "cafe", "addr:city" to "Toronto").address)
        assertNull(named("amenity" to "cafe").address)
        assertEquals("http://example.org", named("amenity" to "cafe", "website" to "http://example.org").website)
    }

    @Test
    fun `only the place's own wikidata item`() {
        assertEquals("Q123", named("tourism" to "museum", "wikidata" to "Q123").wikidata)
        assertNull(named("tourism" to "museum", "wikidata" to "q123").wikidata)
        assertNull(named("tourism" to "museum", "brand:wikidata" to "Q38076").wikidata)
    }

    @Test
    fun highlights() {
        val place = named(
            "amenity" to "cafe",
            "wheelchair" to "designated",
            "outdoor_seating" to "yes",
            "diet:vegan" to "only",
            "diet:vegetarian" to "yes",
            "takeaway" to "yes",
            "dog" to "leashed",
            "internet_access" to "wlan",
        )
        assertEquals(
            listOf(Highlight.WHEELCHAIR, Highlight.OUTDOOR_SEATING, Highlight.VEGAN, Highlight.TAKEAWAY, Highlight.DOGS, Highlight.WIFI),
            place.highlights,
        )
        assertEquals(
            listOf(Highlight.PARTLY_WHEELCHAIR, Highlight.FREE, Highlight.CHANGING_TABLE, Highlight.TOILETS),
            named("tourism" to "museum", "wheelchair" to "limited", "fee" to "no", "changing_table" to "yes", "toilets" to "yes").highlights,
        )
        assertEquals(emptyList<Highlight>(), named("tourism" to "gallery", "wheelchair" to "no", "diet:vegan" to "no").highlights)
    }

    @Test
    fun ages() {
        assertEquals(2..12, place("leisure" to "playground", "min_age" to "2", "max_age" to "12")!!.ages)
        assertEquals(5..99, place("leisure" to "playground", "min_age" to "5")!!.ages)
        assertNull(place("leisure" to "playground")!!.ages)
    }

    @Test
    fun `a link to the map page`() {
        assertEquals("https://www.openstreetmap.org/way/5941460", Place("w5941460", "Park", Kind.PARK, here).osmUrl)
        assertEquals("https://www.openstreetmap.org/node/7", Place("n7", "Cafe", Kind.CAFE, here).osmUrl)
    }
}
