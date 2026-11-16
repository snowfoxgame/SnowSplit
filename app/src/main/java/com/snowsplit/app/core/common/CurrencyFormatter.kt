package com.snowsplit.app.core.common

import com.snowsplit.app.domain.model.CurrencyOption
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {

    fun resolveEffectiveCurrency(
        option: CurrencyOption,
        locale: Locale = Locale.getDefault()
    ): CurrencyOption = when (option) {
        CurrencyOption.AUTO -> when (locale.country.uppercase(Locale.ROOT)) {
            "US" -> CurrencyOption.USD
            "GB" -> CurrencyOption.GBP
            else -> CurrencyOption.EUR
        }
        CurrencyOption.EUR,
        CurrencyOption.USD,
        CurrencyOption.GBP -> option
    }

    fun formatCents(
        amountCents: Long,
        option: CurrencyOption,
        locale: Locale = Locale.getDefault()
    ): String {
        val effective = resolveEffectiveCurrency(option, locale)
        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(effective.code ?: "EUR")
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return formatter.format(amountCents / 100.0)
    }
}
