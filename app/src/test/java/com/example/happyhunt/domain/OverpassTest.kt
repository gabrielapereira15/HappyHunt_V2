package com.example.happyhunt.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.util.Locale

class OverpassTest {
    private fun fixture(name: String) = javaClass.getResource("/$name")!!.readText()

    @Test
    fun `query uses a dot for decimals whatever the phone's language`() {
        val default = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val query = OverpassQuery.around(GeoPoint(43.6465, -79.4197), 1000)
            assertTrue(query, "around:1000,43.646500,-79.419700" in query)
            assertTrue(query.startsWith("[out:json][timeout:25];"))
            assertTrue(query.trimEnd().endsWith("out center tags;"))
            // Playgrounds are the one kind asked for without a name.
            assertTrue(query.lines().any { "\"leisure\"=\"playground\"" in it && "[\"name\"]" !in it })
        } finally {
            Locale.setDefault(default)
        }
    }

    @Test
    fun `query for one place`() {
        assertEquals("[out:json][timeout:15];way(5941460);out center tags;", OverpassQuery.byId("w5941460"))
        assertEquals("[out:json][timeout:15];node(7);out center tags;", OverpassQuery.byId("n7"))
        assertNull(OverpassQuery.byId("x1"))
        assertNull(OverpassQuery.byId("n"))
        assertNull(OverpassQuery.byId(""))
    }

    @Test
    fun `parses a real answer`() {
        val places = OverpassParser.parse(fixture("overpass-sample.json"))
        assertEquals(listOf("n54965507", "n244884014", "w5941460", "n4011951632", "n4922343583"), places.map { it.id })

        val timHortons = places[0]
        assertEquals("Tim Hortons", timHortons.name)
        assertEquals(Kind.CAFE, timHortons.kind)
        assertEquals(Category.TREATS, timHortons.category)
        assertEquals(listOf("Coffee shop"), timHortons.cuisines)
        assertEquals(listOf(Highlight.WHEELCHAIR, Highlight.VEGETARIAN, Highlight.TAKEAWAY, Highlight.WIFI), timHortons.highlights)
        assertEquals(GeoPoint(43.6523976, -79.4062653), timHortons.point)
        assertEquals(OpeningHours.Status.AlwaysOpen, OpeningHours.parse(timHortons.openingHours)!!.statusAt(LocalDateTime.of(2026, 9, 28, 4, 0)))

        val harrys = places[1]
        assertEquals("Harry's Charbroiled", harrys.name)
        assertEquals(Kind.RESTAURANT, harrys.kind)
        assertEquals("293 Palmerston Avenue, Toronto", harrys.address)
        assertEquals("+1-647-342-6307", harrys.phone)
        assertEquals("https://harryscharbroiled.com", harrys.website)
        assertEquals("Mo-Su 12:00-21:00", harrys.openingHours)

        val park = places[2]
        assertEquals(Kind.PARK, park.kind)
        assertEquals("Ways are placed at their centre", GeoPoint(43.6528623, -79.4202801), park.point)

        val playground = places[3]
        assertEquals(Kind.PLAYGROUND, playground.kind)
        assertNull(playground.name)

        assertEquals(emptyList<Highlight>(), places[4].highlights)
    }

    @Test
    fun `an empty answer is no places`() {
        assertEquals(emptyList<Place>(), OverpassParser.parse("""{"version":0.6,"elements":[]}"""))
    }

    @Test
    fun `anything but an answer is an error`() {
        assertThrows(OverpassParser.NotAnAnswer::class.java) {
            OverpassParser.parse("<html><body>504 Gateway Timeout</body></html>")
        }
        assertThrows(OverpassParser.NotAnAnswer::class.java) {
            OverpassParser.parse("""{"version":0.6}""")
        }
        assertThrows(OverpassParser.NotAnAnswer::class.java) {
            OverpassParser.parse("""{"elements":[],"remark":"runtime error: Query timed out in \"query\" at line 3 after 26 seconds."}""")
        }
    }
}
