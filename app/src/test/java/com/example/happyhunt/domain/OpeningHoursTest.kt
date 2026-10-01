package com.example.happyhunt.domain

import com.example.happyhunt.domain.OpeningHours.Status
import org.junit.Assert.assertEquals
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
    fun `every opening time found around Trinity Bellwoods is understood`() {
        val lines = javaClass.getResource("/toronto-opening-hours.txt")!!.readText().lines().filter { it.isNotBlank() }
        val unknown = lines.filter { OpeningHours.parse(it) == null }
        assertEquals(listOf("\"by event\""), unknown)
    }
}
