package com.example.happyhunt.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class GeoTest {
    @Test
    fun `distance along a meridian`() {
        assertEquals(111_195.08, Geo.distanceMeters(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0)), 0.01)
    }

    @Test
    fun `distance across a neighbourhood`() {
        val ossington = GeoPoint(43.6465, -79.4197)
        val timHortons = GeoPoint(43.6523976, -79.4062653)
        assertEquals(1264.3, Geo.distanceMeters(ossington, timHortons), 0.5)
        assertEquals(0.0, Geo.distanceMeters(ossington, ossington), 0.0)
    }

    @Test
    fun `walking time`() {
        assertEquals(1, Geo.walkingMinutes(0.0))
        assertEquals(13, Geo.walkingMinutes(800.0))
        assertEquals(20, Geo.walkingMinutes(1264.3))
    }

    @Test
    fun `units by country`() {
        assertEquals(Units.IMPERIAL, Geo.unitsFor(Locale.US))
        assertEquals(Units.METRIC, Geo.unitsFor(Locale.CANADA))
        assertEquals(Units.METRIC, Geo.unitsFor(Locale.UK))
        assertEquals(Units.METRIC, Geo.unitsFor(Locale.forLanguageTag("pt-BR")))
    }

    @Test
    fun `same clock or not`() {
        val toronto = GeoPoint(43.65, -79.4)
        assertEquals(true, Geo.likelySameClock(toronto, -4 * 60))
        assertEquals(true, Geo.likelySameClock(toronto, -5 * 60))
        assertEquals(false, Geo.likelySameClock(GeoPoint(38.72, -9.14), -4 * 60))
        assertEquals(true, Geo.likelySameClock(GeoPoint(40.42, -3.70), 2 * 60))
        assertEquals(true, Geo.likelySameClock(GeoPoint(-23.55, -46.63), -3 * 60))
        assertEquals(false, Geo.likelySameClock(GeoPoint(35.68, 139.69), -4 * 60))
    }

    @Test
    fun `metric distances`() {
        fun format(meters: Double) = Distances.format(meters, Units.METRIC, Locale.US)
        assertEquals("10 m", format(3.0))
        assertEquals("350 m", format(348.0))
        assertEquals("990 m", format(994.0))
        assertEquals("1 km", format(995.0))
        assertEquals("5 km", format(5000.0))
        assertEquals("1.2 km", format(1234.0))
        assertEquals("12 km", format(12_345.0))
        assertEquals("1,2 km", Distances.format(1234.0, Units.METRIC, Locale.GERMANY))
    }

    @Test
    fun `imperial distances`() {
        fun format(meters: Double) = Distances.format(meters, Units.IMPERIAL, Locale.US)
        assertEquals("350 ft", format(100.0))
        assertEquals("0.4 mi", format(643.7))
        assertEquals("1 mi", format(1609.344))
        assertEquals("12 mi", format(20_000.0))
    }
}
