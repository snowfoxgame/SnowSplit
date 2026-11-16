package com.snowsplit.app.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Deterministically picks the built-in seasonal/holiday theme preset ID for a given calendar date.
 */
object SeasonalThemeResolver {

    fun resolvePresetId(date: LocalDate = LocalDate.now()): String {
        val month = date.monthValue
        val day = date.dayOfMonth

        // New Year window: Dec 31 .. Jan 6
        if ((month == 12 && day == 31) || (month == 1 && day <= 6)) {
            return "new_year"
        }

        // Christmas season: Dec 1 .. Dec 26
        if (month == 12 && day in 1..26) {
            return "christmas"
        }

        // Halloween window: Oct 24 .. Nov 2
        if ((month == 10 && day >= 24) || (month == 11 && day <= 2)) {
            return "halloween"
        }

        // Easter window: Good Friday -1 .. Easter Tuesday (Easter Sunday -3 .. +2 days)
        val easterSunday = computeEasterSunday(date.year)
        val daysFromEaster = ChronoUnit.DAYS.between(easterSunday, date)
        if (daysFromEaster in -3L..2L) {
            return "easter"
        }

        // Astronomical/meteorological seasons
        return when {
            (month == 3 && day >= 20) || month == 4 || month == 5 || (month == 6 && day <= 20) -> "spring"
            (month == 6 && day >= 21) || month == 7 || month == 8 || (month == 9 && day <= 22) -> "summer"
            (month == 9 && day >= 23) || month == 10 || month == 11 -> "autumn"
            else -> "winter"
        }
    }

    /**
     * Anonymous Gregorian Easter algorithm (Computus).
     */
    fun computeEasterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }
}
