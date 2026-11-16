package com.snowsplit.app.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import com.snowsplit.app.domain.model.ThemeAppearanceConfig
import com.snowsplit.app.domain.model.WeatherConditionKind

val LocalThemeConfig = staticCompositionLocalOf<ThemeConfig> {
    StandardMaterial3ThemeConfig()
}

val LocalDesignTokens = staticCompositionLocalOf<SnowDosDesignTokens> {
    StandardMaterial3ThemeConfig().tokens(darkTheme = false)
}

object SnowDosThemeTokens {
    val current: SnowDosDesignTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalDesignTokens.current
}

@Composable
fun SnowDosTheme(
    appearance: ThemeAppearanceConfig = ThemeAppearanceConfig(),
    themeConfig: ThemeConfig? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceLiveBlur: Boolean = false,
    weatherIsDay: Boolean = true,
    weatherCondition: WeatherConditionKind = WeatherConditionKind.CLEAR,
    content: @Composable () -> Unit
) {
    val resolvedConfig = remember(appearance, themeConfig, weatherIsDay, weatherCondition) {
        themeConfig ?: ThemeRegistry.resolve(
            appearance = appearance,
            weatherIsDay = weatherIsDay,
            weatherCondition = weatherCondition
        )
    }
    val tokens = remember(resolvedConfig, darkTheme, reduceLiveBlur) {
        val baseTokens = resolvedConfig.tokens(darkTheme)
        if (reduceLiveBlur) {
            baseTokens.copy(
                surface = baseTokens.surface.copy(
                    blurRadius = 0.dp,
                    reduceLiveBlur = true
                )
            )
        } else {
            baseTokens
        }
    }
    val colorScheme = remember(resolvedConfig, darkTheme) {
        resolvedConfig.colorScheme(darkTheme)
    }
    val shapes = remember(resolvedConfig, tokens) {
        resolvedConfig.shapes(tokens)
    }
    val typography = remember(resolvedConfig) {
        resolvedConfig.typography()
    }

    CompositionLocalProvider(
        LocalThemeConfig provides resolvedConfig,
        LocalDesignTokens provides tokens,
        LocalContentColor provides colorScheme.onBackground
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
