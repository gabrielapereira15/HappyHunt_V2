package com.example.happyhunt.domain

import kotlinx.serialization.Serializable
import java.util.Locale
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class GeoPoint(val lat: Double, val lon: Double)

enum class Units { METRIC, IMPERIAL }

object Geo {
    private const val EARTH_RADIUS_M = 6_371_008.8

    /** Straight-line distance in metres (haversine). Good enough for "how far is it" across a city. */
    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /**
     * Minutes on foot at an easy 4.8 km/h. Streets are not straight lines, so the
     * distance is stretched by a quarter first, the usual allowance for a city grid.
     */
    fun walkingMinutes(meters: Double): Int = ceil(meters * 1.25 / 80.0).toInt().coerceAtLeast(1)

    /** The units people expect where they are: miles in the US, Liberia and Myanmar, metres elsewhere. */
    fun unitsFor(locale: Locale): Units =
        if (locale.country in setOf("US", "LR", "MM")) Units.IMPERIAL else Units.METRIC

    /**
     * Whether a place is likely on the phone's clock. Opening hours are local to
     * the place, and the app has no time-zone map, so "open now" is only shown
     * where the sun's time there (15 degrees of longitude an hour) is within a
     * couple of hours of the phone's. Exploring Lisbon from Toronto shows the
     * hours, not a status that would be five hours off.
     */
    fun likelySameClock(point: GeoPoint, utcOffsetMinutes: Int): Boolean =
        kotlin.math.abs(point.lon / 15.0 - utcOffsetMinutes / 60.0) <= 2.5
}

object Distances {
    /** "350 m", "1 km", "1.2 km", "12 km"; or "500 ft", "0.4 mi", "12 mi". */
    fun format(meters: Double, units: Units, locale: Locale = Locale.getDefault()): String = when (units) {
        Units.METRIC -> when {
            meters < 995 -> "${(meters / 10).roundToInt().coerceAtLeast(1) * 10} m"
            meters < 9_950 -> "${tenths(meters / 1000, locale)} km"
            else -> "${(meters / 1000).roundToInt()} km"
        }
        Units.IMPERIAL -> {
            val miles = meters / 1609.344
            when {
                miles < 0.1 -> "${((meters * 3.28084) / 50).roundToInt().coerceAtLeast(1) * 50} ft"
                miles < 9.95 -> "${tenths(miles, locale)} mi"
                else -> "${miles.roundToInt()} mi"
            }
        }
    }

    /** One decimal, without a trailing ".0": "1.2", but "1" rather than "1.0". */
    private fun tenths(value: Double, locale: Locale): String {
        val rounded = (value * 10).roundToInt()
        return if (rounded % 10 == 0) (rounded / 10).toString() else String.format(locale, "%.1f", rounded / 10.0)
    }
}
