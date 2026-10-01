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
        week[day].orEmpty().map { range -> minute(range.first) to if (range.last >= OPEN_END) null else minute(range.last) }

    fun statusAt(now: LocalDateTime): Status {
        if (alwaysOpen) return Status.AlwaysOpen
        val today = now.dayOfWeek
        val minuteOfDay = now.hour * 60 + now.minute
        // Open now: one of today's ranges, or one of yesterday's that runs past midnight.
        val current = week[today].orEmpty().firstOrNull { minuteOfDay in it.first until it.last }
            ?: week[today.minus(1)].orEmpty()
                .firstOrNull { it.last > DAY && minuteOfDay + DAY in it.first until it.last }
                ?.let { (it.first - DAY)..(it.last - DAY) }
        if (current != null) {
            if (current.last >= OPEN_END) return Status.Open(null, closesSoon = false)
            val left = current.last - minuteOfDay
            return Status.Open(minute(current.last), closesSoon = left <= 60)
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

    companion object {
        private const val DAY = 24 * 60
        private const val OPEN_END = 1_000_000

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
            return OpeningHours(week, alwaysOpen = false)
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
