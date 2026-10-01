package com.example.happyhunt.domain

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Opening hours in the OpenStreetMap format ("Mo-Fr 08:00-18:00; Sa 09:00-14:00"),
 * reduced to a weekly timetable. Covers what most places actually use: days and
 * day ranges, several time ranges, times past midnight, "24/7", "off" and open
 * ends ("18:00+"). Anything else (months, holidays only, sunrise, comments) is
 * not guessed at: [parse] returns null and the app says the hours are unknown.
 */
class OpeningHours private constructor(
    /** Per day, minutes from that day's midnight. An end past 1440 runs into the next day. */
    private val week: Map<DayOfWeek, List<IntRange>>,
    val alwaysOpen: Boolean,
) {
    sealed interface Status {
        data object AlwaysOpen : Status

        /** [closesAt] is null for an open end ("18:00+"). */
        data class Open(val closesAt: LocalTime?, val closesSoon: Boolean) : Status

        /** [opensAt] is null when the place has no opening at all in the coming week. */
        data class Closed(val opensAt: LocalTime?, val opensOn: DayOfWeek?, val opensToday: Boolean) : Status
    }

    /** The day's ranges as clock times, for the weekly table. Empty means closed that day. */
    fun rangesOn(day: DayOfWeek): List<Pair<LocalTime, LocalTime?>> =
        week[day].orEmpty().map { range ->
            when {
                range.last >= OPEN_END -> minute(range.first) to null
                // A whole day or more ("00:00-24:00, 22:00-02:00") reads as midnight to midnight.
                range.last - range.first >= DAY -> minute(range.first) to minute(range.first)
                else -> minute(range.first) to minute(range.last)
            }
        }

    fun statusAt(now: LocalDateTime): Status {
        if (alwaysOpen) return Status.AlwaysOpen
        val today = now.dayOfWeek
        val minuteOfDay = now.hour * 60 + now.minute
        // Open now: whichever open range lasts longest, today's own or one of yesterday's that runs
        // past midnight (a Saturday that goes on to 03:00 outlasts a Sunday 00:00-01:00).
        val yesterday = week[today.minus(1)].orEmpty()
        val todays = week[today].orEmpty().filter { minuteOfDay in it.first until it.last }.map { it.last }
        val yesterdays = yesterday
            .filter { it.last in (DAY + 1) until OPEN_END && minuteOfDay + DAY in it.first until it.last }
            .map { it.last - DAY }
        // An evening open end ("22:00+") is taken to last into the small hours, up to 03:00, without
        // a closing time; one that starts earlier in the day ("11:00+") is not stretched past midnight.
        val lateOpenEnd = minuteOfDay < LATE_NIGHT_END && yesterday.any { it.last >= OPEN_END && it.first >= EVENING }
        val open = (todays + yesterdays + if (lateOpenEnd) listOf(OPEN_END) else emptyList()).maxOrNull()
        if (open != null) {
            // Follow on into a range that carries on from where this one ends ("11:00-14:00,14:00-22:00",
            // or a night that runs into a day open from 00:00), so it does not "close" when it does not.
            var end: Int = open
            var hops = 0
            while (end < OPEN_END && hops++ < 7) {
                val next = carriesOn(today, end) ?: break
                if (next <= end) break
                end = next
            }
            // A close a day or more away has no clock time worth showing ("until 00:00" would read as tonight).
            if (end >= OPEN_END || end - minuteOfDay >= DAY) return Status.Open(null, closesSoon = false)
            return Status.Open(minute(end), closesSoon = end - minuteOfDay <= 60)
        }
        // Closed: find the next opening, today or in the coming week.
        for (offset in 0..7) {
            val day = today.plus(offset.toLong())
            val next = week[day].orEmpty()
                .filter { offset > 0 || it.first > minuteOfDay }
                .minByOrNull { it.first }
            if (next != null) return Status.Closed(minute(next.first), day, opensToday = offset == 0)
        }
        return Status.Closed(null, null, opensToday = false)
    }

    /**
     * Where opening carries on to from [end] (minutes after today's midnight): the latest end of a
     * range open at that moment, that day's own or the day before's past midnight. Null when it closes.
     */
    private fun carriesOn(today: DayOfWeek, end: Int): Int? {
        val offset = end / DAY
        val local = end - offset * DAY
        val day = today.plus(offset.toLong())
        val sameDay = week[day].orEmpty()
            .filter { it.first <= local && it.last > local }
            .map { if (it.last >= OPEN_END) OPEN_END else it.last + offset * DAY }
        val dayBefore = week[day.minus(1)].orEmpty()
            .filter { it.last in (DAY + 1) until OPEN_END && it.first <= local + DAY && it.last > local + DAY }
            .map { it.last - DAY + offset * DAY }
        return (sameDay + dayBefore).maxOrNull()
    }

    companion object {
        private const val DAY = 24 * 60
        private const val OPEN_END = 1_000_000
        private const val EVENING = 17 * 60
        private const val LATE_NIGHT_END = 3 * 60

        private val days = mapOf(
            "mo" to DayOfWeek.MONDAY, "tu" to DayOfWeek.TUESDAY, "we" to DayOfWeek.WEDNESDAY,
            "th" to DayOfWeek.THURSDAY, "fr" to DayOfWeek.FRIDAY, "sa" to DayOfWeek.SATURDAY, "su" to DayOfWeek.SUNDAY,
        )
        private val dayToken = Regex("(mo|tu|we|th|fr|sa|su)(?:-(mo|tu|we|th|fr|sa|su))?")
        private val holidayToken = Regex("ph|sh")
        private val timeToken = Regex("(\\d{1,2}):(\\d{2})(?:-(\\d{1,2}):(\\d{2})|(\\+))")

        private fun minute(minutes: Int): LocalTime {
            val m = minutes % DAY
            return LocalTime.of(m / 60, m % 60)
        }

        fun parse(text: String?): OpeningHours? {
            val source = text?.trim()?.lowercase()?.replace('–', '-')?.replace(" - ", "-") ?: return null
            if (source.isEmpty() || '"' in source) return null
            if (source == "24/7" || source == "mo-su 00:00-24:00" || source == "00:00-24:00") return OpeningHours(emptyMap(), alwaysOpen = true)

            val week = mutableMapOf<DayOfWeek, List<IntRange>>()
            for (rule in source.split(';').map { it.trim() }.filter { it.isNotEmpty() }) {
                if (!applyRule(rule, week)) return null
            }
            if (week.values.all { it.isEmpty() }) return null
            // Ranges that touch or overlap are one ("00:00-12:00,12:00-24:00" is all day).
            for ((day, ranges) in week) week[day] = merged(ranges)
            // However it is written ("Mo-Su,PH 00:00-24:00", "0:00-24:00"), all day every day is round the clock.
            val allDay = DayOfWeek.entries.all { day -> week[day].orEmpty().any { it.first <= 0 && it.last in DAY until OPEN_END } }
            return OpeningHours(if (allDay) emptyMap() else week, alwaysOpen = allDay)
        }

        /** Joins times that touch or overlap; an open end stays apart, since when it starts matters. */
        private fun merged(ranges: List<IntRange>): List<IntRange> {
            val out = mutableListOf<IntRange>()
            for (range in ranges.sortedBy { it.first }) {
                val last = out.lastOrNull()
                if (last != null && last.last < OPEN_END && range.last < OPEN_END && range.first <= last.last) {
                    out[out.lastIndex] = last.first..maxOf(last.last, range.last)
                } else {
                    out += range
                }
            }
            return out
        }

        /**
         * One rule between semicolons. Within it, a comma either continues a list
         * ("Mo, We-Su", "10:00-14:00, 16:00-22:00") or, when days follow times,
         * starts another rule for other days ("Mo-Sa 11:00-22:00, Su 11:00-21:00").
         * A later rule replaces earlier hours for the days it names.
         */
        private fun applyRule(rule: String, week: MutableMap<DayOfWeek, List<IntRange>>): Boolean {
            val tokens = rule.split(',', ' ').map { it.trim() }.filter { it.isNotEmpty() }
            var ruleDays = mutableSetOf<DayOfWeek>()
            var sawHolidayOnly = false
            var ranges = mutableListOf<IntRange>()
            var closed = false
            var haveTimes = false

            fun commit() {
                val target = if (ruleDays.isEmpty() && !sawHolidayOnly) DayOfWeek.entries.toSet() else ruleDays
                for (day in target) week[day] = if (closed) emptyList() else ranges.toList()
                ruleDays = mutableSetOf()
                sawHolidayOnly = false
                ranges = mutableListOf()
                closed = false
                haveTimes = false
            }

            for (token in tokens) {
                val dayMatch = dayToken.matchEntire(token)
                when {
                    dayMatch != null -> {
                        if (haveTimes) commit()
                        val from = days.getValue(dayMatch.groupValues[1])
                        val to = dayMatch.groupValues[2].takeIf { it.isNotEmpty() }?.let(days::getValue) ?: from
                        var day = from
                        while (true) {
                            ruleDays += day
                            if (day == to) break
                            day = day.plus(1)
                        }
                    }
                    holidayToken.matches(token) -> {
                        if (haveTimes) commit()
                        if (ruleDays.isEmpty()) sawHolidayOnly = true
                    }
                    token == "off" || token == "closed" -> {
                        closed = true
                        haveTimes = true
                    }
                    else -> {
                        val time = timeToken.matchEntire(token) ?: return false
                        val (h1, m1) = time.groupValues[1].toInt() to time.groupValues[2].toInt()
                        if (h1 > 24 || m1 > 59) return false
                        val start = h1 * 60 + m1
                        val end = if (time.groupValues[5] == "+") {
                            OPEN_END
                        } else {
                            val h2 = time.groupValues[3].toInt()
                            val m2 = time.groupValues[4].toInt()
                            if (h2 > 48 || m2 > 59) return false
                            val raw = h2 * 60 + m2
                            if (raw <= start) raw + DAY else raw
                        }
                        ranges += start..end
                        haveTimes = true
                    }
                }
            }
            if (!haveTimes) return false
            // A rule for public holidays alone says nothing about ordinary days.
            if (sawHolidayOnly && ruleDays.isEmpty()) return true
            commit()
            return true
        }
    }
}
