package com.snowsplit.app

import com.snowsplit.app.core.common.CurrencyFormatter
import com.snowsplit.app.core.designsystem.ThemeRegistry
import com.snowsplit.app.domain.model.AccountTabVisibility
import com.snowsplit.app.domain.model.AppLanguage
import com.snowsplit.app.domain.model.AppSettings
import com.snowsplit.app.domain.model.CurrencyOption
import com.snowsplit.app.domain.model.TabTransitionMode
import com.snowsplit.app.domain.model.ThemeAppearanceConfig
import com.snowsplit.app.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class Milestone1CoreTest {

    @Test
    fun `currency auto resolves US to USD, GB to GBP, and DE or EU to EUR`() {
        assertEquals(CurrencyOption.USD, CurrencyFormatter.resolveEffectiveCurrency(CurrencyOption.AUTO, Locale.US))
        assertEquals(CurrencyOption.GBP, CurrencyFormatter.resolveEffectiveCurrency(CurrencyOption.AUTO, Locale.UK))
        assertEquals(CurrencyOption.EUR, CurrencyFormatter.resolveEffectiveCurrency(CurrencyOption.AUTO, Locale.GERMANY))
        assertEquals(CurrencyOption.EUR, CurrencyFormatter.resolveEffectiveCurrency(CurrencyOption.AUTO, Locale.FRANCE))
    }

    @Test
    fun `account tab auto visibility follows 380dp width and 1_15 fontScale threshold`() {
        assertTrue(AccountTabVisibility.AUTO.isVisibleInNavBar(screenWidthDp = 412, fontScale = 1.0f))
        assertTrue(AccountTabVisibility.AUTO.isVisibleInNavBar(screenWidthDp = 380, fontScale = 1.15f))
        assertFalse(AccountTabVisibility.AUTO.isVisibleInNavBar(screenWidthDp = 379, fontScale = 1.0f))
        assertFalse(AccountTabVisibility.AUTO.isVisibleInNavBar(screenWidthDp = 412, fontScale = 1.20f))

        assertTrue(AccountTabVisibility.ALWAYS.isVisibleInNavBar(screenWidthDp = 320, fontScale = 1.5f))
        assertFalse(AccountTabVisibility.IN_SETTINGS.isVisibleInNavBar(screenWidthDp = 800, fontScale = 1.0f))
    }

    @Test
    fun `light and dark presets resolve accurate system bar darkness for icon contrast`() {
        val lightPresets = listOf("winter", "summer", "spring", "easter", "8bit_cute")
        lightPresets.forEach { presetId ->
            assertFalse(
                "$presetId should be light even when system dark is true",
                ThemeRegistry.isEffectiveDarkTheme(
                    ThemeAppearanceConfig(presetId = presetId, mode = ThemeMode.PRESET),
                    systemDarkTheme = true
                )
            )
        }

        val darkPresets = listOf("halloween", "christmas", "autumn", "new_year", "gaming")
        darkPresets.forEach { presetId ->
            assertTrue(
                "$presetId should be dark even when system dark is false",
                ThemeRegistry.isEffectiveDarkTheme(
                    ThemeAppearanceConfig(presetId = presetId, mode = ThemeMode.PRESET),
                    systemDarkTheme = false
                )
            )
        }

        assertTrue(
            ThemeRegistry.isEffectiveDarkTheme(
                ThemeAppearanceConfig(presetId = "standard_m3", mode = ThemeMode.PRESET),
                systemDarkTheme = true
            )
        )
        assertFalse(
            ThemeRegistry.isEffectiveDarkTheme(
                ThemeAppearanceConfig(presetId = "standard_m3", mode = ThemeMode.PRESET),
                systemDarkTheme = false
            )
        )
    }

    @Test
    fun `language only exposes DE and EN with DE default and migrates legacy SYSTEM to DE`() {
        assertEquals(listOf(AppLanguage.DE, AppLanguage.EN), AppLanguage.entries)
        assertEquals(AppLanguage.DE, AppSettings().appLanguage)
        assertEquals(AppLanguage.DE, AppLanguage.fromStoredValue("SYSTEM"))
        assertEquals(AppLanguage.DE, AppLanguage.fromStoredValue("DE"))
        assertEquals(AppLanguage.DE, AppLanguage.fromStoredValue(null))
        assertEquals(AppLanguage.DE, AppLanguage.fromStoredValue("FR"))
        assertEquals(AppLanguage.EN, AppLanguage.fromStoredValue("EN"))
    }

    @Test
    fun `default settings use animated tab transitions and full live blur`() {
        val defaults = AppSettings()
        assertEquals(TabTransitionMode.ANIMATED, defaults.tabTransitionMode)
        assertFalse(defaults.reduceLiveBlur)
    }
}
