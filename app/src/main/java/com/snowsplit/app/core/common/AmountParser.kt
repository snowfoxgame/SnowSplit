package com.snowsplit.app.core.common

import java.math.BigDecimal
import java.math.RoundingMode

object AmountParser {
    /**
     * Parses user-entered decimal strings like "20", "12.50", or "12,50" into exact minor units (cents).
     * Returns null if invalid or <= 0.
     */
    fun parsePositiveCents(raw: String): Long? {
        val cleaned = raw.trim()
            .replace(" ", "")
            .replace(',', '.')
        if (cleaned.isEmpty()) return null
        val decimal = runCatching { BigDecimal(cleaned) }.getOrNull() ?: return null
        if (decimal.compareTo(BigDecimal.ZERO) <= 0) return null
        val cents = decimal
            .setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
        return runCatching { cents.longValueExact() }.getOrNull()?.takeIf { it > 0L }
    }

    fun formatInputFromCents(cents: Long): String {
        val whole = cents / 100L
        val frac = (cents % 100L).toInt()
        return if (frac == 0) {
            whole.toString()
        } else {
            "$whole.${frac.toString().padStart(2, '0')}"
        }
    }
}
