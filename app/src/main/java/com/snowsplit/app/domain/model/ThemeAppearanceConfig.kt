package com.snowsplit.app.domain.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

enum class ThemeMode {
    PRESET,
    CUSTOM
}

enum class EightBitVariant {
    PASTEL_PINK,
    PASTEL_BLUE
}

@Immutable
data class ThemeAppearanceConfig(
    val presetId: String = "standard_m3",
    val mode: ThemeMode = ThemeMode.PRESET,
    val customAccentHex: String = "#D0BCFF",
    val gamingAccentHex: String = "#00E5FF",
    val eightBitVariant: EightBitVariant = EightBitVariant.PASTEL_PINK,
    val weatherStyle: WeatherThemeStyle = WeatherThemeStyle.CUTE_2D,
    val autoSeasonalThemeEnabled: Boolean = false,
    val glassOpacity: Float = 0.24f,
    val backgroundImageUri: String? = null
) {
    fun effectivePresetId(date: LocalDate = LocalDate.now()): String =
        if (mode == ThemeMode.PRESET && autoSeasonalThemeEnabled) {
            SeasonalThemeResolver.resolvePresetId(date)
        } else {
            presetId
        }
}
