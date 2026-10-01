package com.example.happyhunt.domain

import com.example.happyhunt.domain.OpeningHours.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

class OpeningHoursTest {
    /** 28 September 2026 is a Monday; the rest of that week follows. */
    private fun at(day: DayOfWeek, hour: Int, minute: Int = 0): LocalDateTime =
        LocalDateTime.of(2026, 9, 28, hour, minute).plusDays(day.ordinal.toLong())

    private fun hours(text: String): OpeningHours = OpeningHours.parse(text) ?: throw AssertionError("Should parse: $text")

    private fun time(text: String) = LocalTime.parse(text)

    @Test
    fun `always open`() {
        assertEquals(Status.AlwaysOpen, hours("24/7").statusAt(at(DayOfWeek.SUNDAY, 3)))
        assertEquals(Status.AlwaysOpen, hours("Mo-Su 00:00-24:00").statusAt(at(DayOfWeek.MONDAY, 12)))
    }

    @Test
    fun `weekdays and a short saturday`() {
        val hours = hours("Mo-Fr 08:00-18:00; Sa 09:00-14:00")
        assertEquals(Status.Open(time("18:00"), closesSoon = false), hours.statusAt(at(DayOfWeek.MONDAY, 10)))
        assertEquals(Status.Open(time("18:00"), closesSoon = true), hours.statusAt(at(DayOfWeek.MONDAY, 17, 30)))
        assertEquals(Status.Closed(time("08:00"), DayOfWeek.MONDAY, opensToday = true), hours.statusAt(at(DayOfWeek.MONDAY, 7)))
        assertEquals(Status.Closed(time("08:00"), DayOfWeek.TUESDAY, opensToday = false), hours.statusAt(at(DayOfWeek.MONDAY, 18)))
        // Nothing on Sunday, so after Saturday closes the next opening is Monday.
        assertEquals(Status.Closed(time("08:00"), DayOfWeek.MONDAY, opensToday = false), hours.statusAt(at(DayOfWeek.SATURDAY, 15)))
        assertTrue(hours.rangesOn(DayOfWeek.SUNDAY).isEmpty())
    }

    @Test
    fun `hours past midnight belong to the evening before`() {
        val hours = hours("Mo-Sa 18:00-01:00, Su 18:00-23:30")
        assertEquals(Status.Open(time("01:00"), closesSoon = true), hours.statusAt(at(DayOfWeek.TUESDAY, 0, 30)))
        // Sunday closes before midnight, so early Monday is closed.
        assertEquals(Status.Closed(time("18:00"), DayOfWeek.MONDAY, opensToday = true), hours.statusAt(at(DayOfWeek.MONDAY, 0, 30)))
        assertEquals(listOf(time("18:00") to time("01:00")), hours.rangesOn(DayOfWeek.MONDAY))
    }

    @Test
    fun `closing at midnight`() {
        val hours = hours("Mo-We 11:30-00:00; Th 11:30-03:00")
        assertEquals(Status.Open(time("00:00"), closesSoon = true), hours.statusAt(at(DayOfWeek.MONDAY, 23, 30)))
        assertEquals(Status.Open(time("03:00"), closesSoon = false), hours.statusAt(at(DayOfWeek.FRIDAY, 1)))
    }

    @Test
    fun `a break in the afternoon`() {
        val hours = hours("Mo-Fr 11:30-14:30,17:00-22:00; Sa 11:30-22:00; Su 15:00-22:00")
        assertEquals(Status.Closed(time("17:00"), DayOfWeek.MONDAY, opensToday = true), hours.statusAt(at(DayOfWeek.MONDAY, 15)))
        assertEquals(listOf(time("11:30") to time("14:30"), time("17:00") to time("22:00")), hours.rangesOn(DayOfWeek.MONDAY))
    }

    @Test
    fun `an open end`() {
        val hours = hours("11:00+")
        assertEquals(Status.Open(null, closesSoon = false), hours.statusAt(at(DayOfWeek.MONDAY, 23)))
        assertEquals(listOf(time("11:00") to null), hours.rangesOn(DayOfWeek.WEDNESDAY))
    }

    @Test
    fun `an open end does not run into the next day`() {
        // "From 11:00" says nothing about the next morning: closed until 11:00.
        assertEquals(Status.Closed(time("11:00"), DayOfWeek.TUESDAY, opensToday = true), hours("11:00+").statusAt(at(DayOfWeek.TUESDAY, 8)))
        assertEquals(Status.Closed(time("11:00"), DayOfWeek.TUESDAY, opensToday = true), hours("11:00+").statusAt(at(DayOfWeek.TUESDAY, 1)))
        assertEquals(
            Status.Closed(time("17:00"), DayOfWeek.MONDAY, opensToday = false),
            hours("Mo-Sa 17:00+; Su off").statusAt(at(DayOfWeek.SUNDAY, 12)),
        )
    }

    @Test
    fun `an evening open end lasts into the small hours`() {
        val club = hours("Fr-Sa 22:00+")
        assertEquals(Status.Open(null, closesSoon = false), club.statusAt(at(DayOfWeek.SATURDAY, 0, 30)))
        assertEquals(Status.Closed(time("22:00"), DayOfWeek.SATURDAY, opensToday = true), club.statusAt(at(DayOfWeek.SATURDAY, 4)))
        assertEquals(Status.Open(null, closesSoon = false), hours("Mo-Sa 17:00+; Su off").statusAt(at(DayOfWeek.SUNDAY, 1)))
    }

    @Test
    fun `round the clock however it is written`() {
        assertEquals(Status.AlwaysOpen, hours("Mo-Su,PH 00:00-24:00").statusAt(at(DayOfWeek.MONDAY, 23, 30)))
        assertEquals(Status.AlwaysOpen, hours("Mo-Su 0:00-24:00; PH off").statusAt(at(DayOfWeek.FRIDAY, 3)))
        assertEquals(Status.AlwaysOpen, hours("Mo-Su 00:00-12:00,12:00-24:00").statusAt(at(DayOfWeek.MONDAY, 12, 30)))
        assertEquals(Status.AlwaysOpen, hours("Mo-Su 00:00-24:00, 22:00-02:00").statusAt(at(DayOfWeek.MONDAY, 12, 30)))
    }

    @Test
    fun `ranges that carry on do not close`() {
        // Open all of Saturday too: no closing time to show, and certainly not "closing soon".
        assertEquals(
            Status.Open(null, closesSoon = false),
            hours("Mo-Th 06:00-01:00; Fr-Sa 00:00-24:00").statusAt(at(DayOfWeek.FRIDAY, 23, 30)),
        )
        // Into the small hours of the next day, the closing time still shows.
        assertEquals(
            Status.Open(time("03:00"), closesSoon = false),
            hours("Fr 00:00-24:00; Sa 00:00-03:00").statusAt(at(DayOfWeek.FRIDAY, 23, 30)),
        )
        // A Saturday that runs on to 03:00 outlasts Sunday's own 00:00-01:00.
        assertEquals(
            Status.Open(time("03:00"), closesSoon = false),
            hours("Mo-Su 00:00-01:00, 11:00-24:00; Fr-Sa 11:00-03:00").statusAt(at(DayOfWeek.SUNDAY, 0, 30)),
        )
        // A late open end next to an early-morning range keeps the place open.
        assertEquals(Status.Open(null, closesSoon = false), hours("Fr 11:00-01:00, 23:00+").statusAt(at(DayOfWeek.SATURDAY, 0, 30)))
        // Open round the clock however it is split: no made-up closing time.
        assertEquals(Status.Open(null, closesSoon = false), hours("Mo-Su 12:00-12:00").statusAt(at(DayOfWeek.MONDAY, 13)))
        // Lunch and dinner written back to back.
        assertEquals(
            Status.Open(time("22:00"), closesSoon = false),
            hours("11:00-14:00,14:00-22:00").statusAt(at(DayOfWeek.WEDNESDAY, 13, 30)),
        )
    }

    @Test
    fun `days off and later rules`() {
        val weekend = hours("Mo-Su 11:00-20:00; Sa, Su off")
        assertEquals(Status.Closed(time("11:00"), DayOfWeek.MONDAY, opensToday = false), weekend.statusAt(at(DayOfWeek.SATURDAY, 12)))

        val commas = hours("Tu-Sa 11:00-20:00, Su 11:00-19:00, Mo off")
        assertEquals(Status.Closed(time("11:00"), DayOfWeek.TUESDAY, opensToday = false), commas.statusAt(at(DayOfWeek.MONDAY, 12)))
        assertEquals(Status.Open(time("19:00"), closesSoon = false), commas.statusAt(at(DayOfWeek.SUNDAY, 12)))

        val override = hours("Mo-Su 11:00-22:00; Su 12:00-18:00")
        assertEquals(listOf(time("12:00") to time("18:00")), override.rangesOn(DayOfWeek.SUNDAY))
        assertEquals(listOf(time("11:00") to time("22:00")), override.rangesOn(DayOfWeek.SATURDAY))
    }

    @Test
    fun `day ranges that wrap around the week and day lists`() {
        val wrap = hours("We-Mo 08:00-18:00")
        assertTrue(wrap.rangesOn(DayOfWeek.TUESDAY).isEmpty())
        assertEquals(1, wrap.rangesOn(DayOfWeek.MONDAY).size)

        val list = hours("Mo, We-Su 11:30-22:00")
        assertTrue(list.rangesOn(DayOfWeek.TUESDAY).isEmpty())
        assertEquals(1, list.rangesOn(DayOfWeek.MONDAY).size)
        assertEquals(1, list.rangesOn(DayOfWeek.SUNDAY).size)
    }

    @Test
    fun `public holiday rules leave ordinary days alone`() {
        val hours = hours("Mo-Sa 09:00-17:00; Su 10:00-15:00; PH closed")
        assertEquals(Status.Open(time("15:00"), closesSoon = false), hours.statusAt(at(DayOfWeek.SUNDAY, 11)))
    }

    @Test
    fun `en dashes as people type them`() {
        assertEquals(1, hours("Mo–Fr 09:00–17:00").rangesOn(DayOfWeek.FRIDAY).size)
    }

    @Test
    fun `what it does not understand is unknown, not guessed`() {
        assertNull(OpeningHours.parse(null))
        assertNull(OpeningHours.parse(""))
        assertNull(OpeningHours.parse("\"by event\""))
        assertNull(OpeningHours.parse("sunrise-sunset"))
        assertNull(OpeningHours.parse("Jan-Mar 10:00-16:00"))
        assertNull(OpeningHours.parse("Mo-Fr"))
        assertNull(OpeningHours.parse("PH off"))
    }

    @Test
    fun `every Toronto opening time gives a sensible status at every half hour of the week`() {
        val lines = javaClass.getResource("/toronto-opening-hours.txt")!!.readText().lines().filter { it.isNotBlank() }
        for (text in lines) {
            val hours = OpeningHours.parse(text) ?: continue
            for (step in 0 until 7 * 48) {
                val now = at(DayOfWeek.MONDAY, 0).plusMinutes(step * 30L)
                when (val status = hours.statusAt(now)) {
                    // A closing time is never one that has already passed today.
                    is Status.Open -> status.closesAt?.let { closes ->
                        assertTrue("$text at $now closes at $closes", closes != now.toLocalTime() || status.closesSoon)
                    }
                    is Status.Closed -> assertNotNull("$text at $now has no next opening", status.opensAt)
                    Status.AlwaysOpen -> Unit
                }
            }
        }
    }

    @Test
    fun `every opening time found around Trinity Bellwoods is understood`() {
        val lines = javaClass.getResource("/toronto-opening-hours.txt")!!.readText().lines().filter { it.isNotBlank() }
        val unknown = lines.filter { OpeningHours.parse(it) == null }
        assertEquals(listOf("\"by event\""), unknown)
    }
}
