package com.example.happyhunt.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotonTest {
    private fun fixture(name: String) = javaClass.getResource("/$name")!!.readText()

    @Test
    fun `search results become areas`() {
        val areas = PhotonParser.areas(fixture("photon-search.json"))
        assertEquals(
            listOf(
                Area("Trinity Bellwoods Park", "Spadina–Fort York, Toronto, Ontario", GeoPoint(43.647644, -79.4139122)),
                Area("Trinity-Bellwoods", "Toronto, Ontario, Canada", GeoPoint(43.6506294, -79.4152912)),
                Area("Trinity Bellwoods Farmers Market", "Toronto, Ontario, Canada", GeoPoint(43.6495009, -79.4174508)),
            ),
            areas,
        )
    }

    @Test
    fun `a reverse lookup becomes a short label`() {
        assertEquals("Spadina–Fort York, Toronto", PhotonParser.label(fixture("photon-reverse.json")))
    }

    @Test
    fun `nothing found`() {
        assertEquals(emptyList<Area>(), PhotonParser.areas("""{"type":"FeatureCollection","features":[]}"""))
        assertEquals(emptyList<Area>(), PhotonParser.areas("Bad gateway"))
        assertNull(PhotonParser.label("""{"type":"FeatureCollection","features":[]}"""))
    }
}
