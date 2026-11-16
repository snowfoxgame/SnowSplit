package com.snowsplit.app.core.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.snowsplit.app.domain.model.AppLanguage

object LocaleHelper {
    fun applyLanguage(language: AppLanguage) {
        val desired = when (language) {
            AppLanguage.DE -> LocaleListCompat.forLanguageTags("de")
            AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
        }
        if (AppCompatDelegate.getApplicationLocales() != desired) {
            AppCompatDelegate.setApplicationLocales(desired)
        }
    }
}
