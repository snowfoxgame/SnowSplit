package com.snowsplit.app.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.snowsplit.app.domain.model.EightBitVariant
import com.snowsplit.app.domain.model.ThemeAppearanceConfig
import com.snowsplit.app.domain.model.ThemeMode
import com.snowsplit.app.domain.model.WeatherConditionKind
import com.snowsplit.app.domain.model.WeatherThemeStyle

@Stable
interface ThemeConfig {
    val id: String
    val displayName: String

    fun colorScheme(darkTheme: Boolean): ColorScheme
    fun typography(): Typography
    fun shapes(tokens: SnowDosDesignTokens): Shapes
    fun tokens(darkTheme: Boolean): SnowDosDesignTokens
}

@Immutable
data class PresetPaletteSpec(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerHigh: Color,
    val primary: Color,
    val secondaryAccent: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color
)

enum class ThemeCategory {
    NORMAL,
    EVENTS,
    PLAYFUL,
    INTERACTIVE
}

@Immutable
data class ThemePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: ThemeCategory,
    val hasPatternBackground: Boolean,
    val previewPrimary: Color,
    val previewSurface: Color,
    val previewSecondary: Color,
    val paletteSpec: PresetPaletteSpec? = null,
    val createConfig: () -> ThemeConfig
)

object ThemeRegistry {
    val presets: List<ThemePreset> = listOf(
        ThemePreset(
            id = "standard_m3",
            title = "Normal",
            subtitle = "Clean & focused",
            category = ThemeCategory.NORMAL,
            hasPatternBackground = true,
            previewPrimary = Color(0xFFD0BCFF),
            previewSurface = Color(0xFF17181C),
            previewSecondary = Color(0xFF8F94A3),
            createConfig = { StandardMaterial3ThemeConfig() }
        ),
        themedPreset(
            id = "winter",
            title = "Winter",
            subtitle = "Snowflakes & frost",
            category = ThemeCategory.NORMAL,
            spec = PresetPaletteSpec(
                isDark = false,
                background = Color(0xFFCDE2F6),
                surface = Color(0xFFDCEBF9),
                surfaceContainer = Color(0xFFEAF4FE),
                surfaceContainerLow = Color(0xFFF3F9FF),
                surfaceContainerHigh = Color(0xFFDDF0FE),
                primary = Color(0xFF1D6FA5),
                secondaryAccent = Color(0xFF5FA8D3),
                onBackground = Color(0xFF0F2438),
                onSurfaceVariant = Color(0xFF3E5973)
            )
        ),
        themedPreset(
            id = "halloween",
            title = "Halloween",
            subtitle = "Pumpkins & midnight",
            category = ThemeCategory.EVENTS,
            spec = PresetPaletteSpec(
                isDark = true,
                background = Color(0xFF141316),
                surface = Color(0xFF141316),
                surfaceContainer = Color(0xFF222026),
                surfaceContainerLow = Color(0xFF1B191E),
                surfaceContainerHigh = Color(0xFF2C2830),
                primary = Color(0xFFE67E22),
                secondaryAccent = Color(0xFF9E9E9E),
                onBackground = Color(0xFFF4EFEA),
                onSurfaceVariant = Color(0xFFB8B0AA)
            )
        ),
        themedPreset(
            id = "christmas",
            title = "Christmas",
            subtitle = "Festive pine & holly",
            category = ThemeCategory.EVENTS,
            spec = PresetPaletteSpec(
                isDark = true,
                background = Color(0xFF133322),
                surface = Color(0xFF133322),
                surfaceContainer = Color(0xFF1D4630),
                surfaceContainerLow = Color(0xFF183C29),
                surfaceContainerHigh = Color(0xFF24533A),
                primary = Color(0xFFD34545),
                secondaryAccent = Color(0xFFF2F5F3),
                onBackground = Color(0xFFF5F7F5),
                onSurfaceVariant = Color(0xFFC2D4C9)
            )
        ),
        themedPreset(
            id = "summer",
            title = "Summer",
            subtitle = "Beach & ocean breeze",
            category = ThemeCategory.NORMAL,
            spec = PresetPaletteSpec(
                isDark = false,
                background = Color(0xFFBBE4F2),
                surface = Color(0xFFCFEFF9),
                surfaceContainer = Color(0xFFE5F8FD),
                surfaceContainerLow = Color(0xFFF0FBFE),
                surfaceContainerHigh = Color(0xFFD5F1FA),
                primary = Color(0xFF007A8C),
                secondaryAccent = Color(0xFFF59E0B),
                onBackground = Color(0xFF0B2834),
                onSurfaceVariant = Color(0xFF335966)
            )
        ),
        themedPreset(
            id = "autumn",
            title = "Autumn",
            subtitle = "Warm leaves & amber",
            category = ThemeCategory.NORMAL,
            spec = PresetPaletteSpec(
                isDark = true,
                background = Color(0xFF331C11),
                surface = Color(0xFF331C11),
                surfaceContainer = Color(0xFF452719),
                surfaceContainerLow = Color(0xFF3C2115),
                surfaceContainerHigh = Color(0xFF533020),
                primary = Color(0xFFD97736),
                secondaryAccent = Color(0xFFE0A952),
                onBackground = Color(0xFFFBF1E8),
                onSurfaceVariant = Color(0xFFD8BEAC)
            )
        ),
        themedPreset(
            id = "spring",
            title = "Spring",
            subtitle = "Fresh meadow & blooms",
            category = ThemeCategory.NORMAL,
            spec = PresetPaletteSpec(
                isDark = false,
                background = Color(0xFFC5E6CC),
                surface = Color(0xFFD6EEDC),
                surfaceContainer = Color(0xFFE7F6EA),
                surfaceContainerLow = Color(0xFFF1FAF3),
                surfaceContainerHigh = Color(0xFFD9EEDF),
                primary = Color(0xFF2E7D4F),
                secondaryAccent = Color(0xFFECA129),
                onBackground = Color(0xFF122B1A),
                onSurfaceVariant = Color(0xFF3D5C47)
            )
        ),
        themedPreset(
            id = "easter",
            title = "Easter",
            subtitle = "Soft pastel & eggs",
            category = ThemeCategory.EVENTS,
            spec = PresetPaletteSpec(
                isDark = false,
                background = Color(0xFFDFC8F0),
                surface = Color(0xFFEAD9F6),
                surfaceContainer = Color(0xFFF4EAFC),
                surfaceContainerLow = Color(0xFFFAF4FE),
                surfaceContainerHigh = Color(0xFFEBDCF7),
                primary = Color(0xFF7D489E),
                secondaryAccent = Color(0xFF4BA896),
                onBackground = Color(0xFF281533),
                onSurfaceVariant = Color(0xFF5A4369)
            )
        ),
        themedPreset(
            id = "new_year",
            title = "New Year",
            subtitle = "Midnight & fireworks",
            category = ThemeCategory.EVENTS,
            spec = PresetPaletteSpec(
                isDark = true,
                background = Color(0xFF0F1326),
                surface = Color(0xFF0F1326),
                surfaceContainer = Color(0xFF191F3A),
                surfaceContainerLow = Color(0xFF141930),
                surfaceContainerHigh = Color(0xFF22294A),
                primary = Color(0xFFE0B44A),
                secondaryAccent = Color(0xFF4FC3F7),
                onBackground = Color(0xFFF2F4FC),
                onSurfaceVariant = Color(0xFFB5BCD6)
            )
        ),
        themedPreset(
            id = "gaming",
            title = "Gaming",
            subtitle = "Dark LED & controllers",
            category = ThemeCategory.PLAYFUL,
            spec = PresetPaletteSpec(
                isDark = true,
                background = Color(0xFF090C14),
                surface = Color(0xFF090C14),
                surfaceContainer = Color(0xFF141926),
                surfaceContainerLow = Color(0xFF0F131E),
                surfaceContainerHigh = Color(0xFF1C2336),
                primary = Color(0xFF00E5FF),
                secondaryAccent = Color(0xFFFF2E93),
                onBackground = Color(0xFFF0F4FF),
                onSurfaceVariant = Color(0xFF9AA7C4)
            )
        ),
        themedPreset(
            id = "8bit_cute",
            title = "8-Bit Cute",
            subtitle = "Retro pixels & paws",
            category = ThemeCategory.PLAYFUL,
            spec = PresetPaletteSpec(
                isDark = false,
                background = Color(0xFFFFEEF4),
                surface = Color(0xFFFFEEF4),
                surfaceContainer = Color(0xFFFFDCE8),
                surfaceContainerLow = Color(0xFFFFF5F8),
                surfaceContainerHigh = Color(0xFFFFC9DC),
                primary = Color(0xFFD83B78),
                secondaryAccent = Color(0xFF72B5FF),
                onBackground = Color(0xFF311424),
                onSurfaceVariant = Color(0xFF6B3A54)
            )
        ),
        themedPreset(
            id = "interactive_bubbles",
            title = "Interactive Bubbles",
            subtitle = "Floating touch-reactive orbs",
            category = ThemeCategory.INTERACTIVE,
            spec = PresetPaletteSpec(
                isDark = true,
                background = Color(0xFF0D1426),
                surface = Color(0xFF0D1426),
                surfaceContainer = Color(0xFF16203B),
                surfaceContainerLow = Color(0xFF111A30),
                surfaceContainerHigh = Color(0xFF1E2B4D),
                primary = Color(0xFF6AE2FF),
                secondaryAccent = Color(0xFFB388FF),
                onBackground = Color(0xFFF0F5FF),
                onSurfaceVariant = Color(0xFFB3C2E0)
            )
        ),
        themedPreset(
            id = "mini_city",
            title = "Cutesy Mini-City",
            subtitle = "Slanted roads & tiny cars",
            category = ThemeCategory.PLAYFUL,
            spec = PresetPaletteSpec(
                isDark = false,
                background = Color(0xFFF1F5EE),
                surface = Color(0xFFF1F5EE),
                surfaceContainer = Color(0xFFE3ECE0),
                surfaceContainerLow = Color(0xFFECF2EA),
                surfaceContainerHigh = Color(0xFFD5E2D3),
                primary = Color(0xFF2F7E68),
                secondaryAccent = Color(0xFFE57A63),
                onBackground = Color(0xFF1A2622),
                onSurfaceVariant = Color(0xFF4D6059)
            )
        ),
        themedPreset(
            id = "weather_time",
            title = "Weather & Sky",
            subtitle = "Live sun, moon & weather",
            category = ThemeCategory.INTERACTIVE,
            spec = PresetPaletteSpec(
                isDark = false,
                background = Color(0xFFEAF3FF),
                surface = Color(0xFFEAF3FF),
                surfaceContainer = Color(0xFFDCEBFF),
                surfaceContainerLow = Color(0xFFE5F0FF),
                surfaceContainerHigh = Color(0xFFCDE1FD),
                primary = Color(0xFF2B6CB0),
                secondaryAccent = Color(0xFFF5A623),
                onBackground = Color(0xFF132238),
                onSurfaceVariant = Color(0xFF475C78)
            )
        ),
        eightBitPixelPreset(
            id = "8bit_cat",
            title = "8-Bit Cat",
            subtitle = "Pastel pink pixels & paws",
            outlineColor = Color(0xFF521B35),
            spec = PresetPaletteSpec(
                isDark = false,
                background = Color(0xFFFFE4EE),
                surface = Color(0xFFFFE4EE),
                surfaceContainer = Color(0xFFFFD1E2),
                surfaceContainerLow = Color(0xFFFFF5F9),
                surfaceContainerHigh = Color(0xFFFFBDD6),
                primary = Color(0xFFD63374),
                secondaryAccent = Color(0xFFFF8AB5),
                onBackground = Color(0xFF331020),
                onSurfaceVariant = Color(0xFF6B364F)
            )
        ),
        eightBitPixelPreset(
            id = "8bit_fox",
            title = "8-Bit White Fox",
            subtitle = "Snowy white & icy fox",
            outlineColor = Color(0xFF1D3554),
            spec = PresetPaletteSpec(
                isDark = false,
                background = Color(0xFFEEF5FC),
                surface = Color(0xFFEEF5FC),
                surfaceContainer = Color(0xFFDCE9F7),
                surfaceContainerLow = Color(0xFFFFFFFF),
                surfaceContainerHigh = Color(0xFFC9DDF2),
                primary = Color(0xFFD95D24),
                secondaryAccent = Color(0xFF5B8EC9),
                onBackground = Color(0xFF132236),
                onSurfaceVariant = Color(0xFF3E5570)
            )
        ),
        eightBitPixelPreset(
            id = "8bit_bunny",
            title = "8-Bit Bunny",
            subtitle = "Warm caramel & carrots",
            outlineColor = Color(0xFF472B1A),
            spec = PresetPaletteSpec(
                isDark = false,
                background = Color(0xFFF6EDE2),
                surface = Color(0xFFF6EDE2),
                surfaceContainer = Color(0xFFEBDBC7),
                surfaceContainerLow = Color(0xFFFFF9F2),
                surfaceContainerHigh = Color(0xFFDEC7AD),
                primary = Color(0xFF9C5427),
                secondaryAccent = Color(0xFFE07B39),
                onBackground = Color(0xFF2D1B10),
                onSurfaceVariant = Color(0xFF634836)
            )
        ),
        eightBitPixelPreset(
            id = "8bit_snake",
            title = "8-Bit Snake",
            subtitle = "Emerald pixels & leaves",
            outlineColor = Color(0xFF16422A),
            spec = PresetPaletteSpec(
                isDark = false,
                background = Color(0xFFE3F3E6),
                surface = Color(0xFFE3F3E6),
                surfaceContainer = Color(0xFFCDE8D3),
                surfaceContainerLow = Color(0xFFF2FBF4),
                surfaceContainerHigh = Color(0xFFB5DBC0),
                primary = Color(0xFF1F7A4C),
                secondaryAccent = Color(0xFF4CAF7D),
                onBackground = Color(0xFF0E291A),
                onSurfaceVariant = Color(0xFF345E46)
            )
        )
    )

    private fun eightBitPixelPreset(
        id: String,
        title: String,
        subtitle: String,
        outlineColor: Color,
        spec: PresetPaletteSpec
    ): ThemePreset = ThemePreset(
        id = id,
        title = title,
        subtitle = subtitle,
        category = ThemeCategory.PLAYFUL,
        hasPatternBackground = true,
        previewPrimary = spec.primary,
        previewSurface = spec.background,
        previewSecondary = spec.secondaryAccent,
        paletteSpec = spec,
        createConfig = {
            EightBitPixelThemeConfig(
                id = id,
                displayName = title,
                spec = spec,
                outlineColor = outlineColor
            )
        }
    )

    private fun themedPreset(
        id: String,
        title: String,
        subtitle: String,
        category: ThemeCategory,
        spec: PresetPaletteSpec
    ): ThemePreset = ThemePreset(
        id = id,
        title = title,
        subtitle = subtitle,
        category = category,
        hasPatternBackground = true,
        previewPrimary = spec.primary,
        previewSurface = spec.background,
        previewSecondary = spec.secondaryAccent,
        paletteSpec = spec,
        createConfig = {
            PresetThemeConfig(
                id = id,
                displayName = title,
                spec = spec
            )
        }
    )

    fun presetsFor(category: ThemeCategory?): List<ThemePreset> =
        if (category == null) presets else presets.filter { it.category == category }

    fun findPreset(presetId: String): ThemePreset =
        presets.firstOrNull { it.id == presetId } ?: presets.first()

    fun isEffectiveDarkTheme(
        appearance: ThemeAppearanceConfig,
        systemDarkTheme: Boolean,
        weatherIsDay: Boolean = true,
        weatherCondition: WeatherConditionKind = WeatherConditionKind.CLEAR
    ): Boolean = if (appearance.mode == ThemeMode.PRESET) {
        val resolvedId = appearance.effectivePresetId()
        if (resolvedId == "weather_time") {
            !weatherIsDay || weatherCondition == WeatherConditionKind.THUNDERSTORM_HAIL
        } else {
            findPreset(resolvedId).paletteSpec?.isDark ?: systemDarkTheme
        }
    } else {
        systemDarkTheme
    }

    fun resolve(
        appearance: ThemeAppearanceConfig,
        weatherIsDay: Boolean = true,
        weatherCondition: WeatherConditionKind = WeatherConditionKind.CLEAR
    ): ThemeConfig {
        val preset = findPreset(appearance.effectivePresetId())
        return if (appearance.mode == ThemeMode.CUSTOM) {
            val base = presets.first().createConfig()
            val accent = parseHexColorOrNull(appearance.customAccentHex) ?: preset.previewPrimary
            CustomAccentThemeConfig(
                accentColor = accent,
                glassOpacity = appearance.glassOpacity,
                base = base
            )
        } else {
            when (preset.id) {
                "gaming" -> GamingThemeConfig(
                    accentColor = parseHexColorOrNull(appearance.gamingAccentHex)
                        ?: preset.previewPrimary
                )
                "8bit_cute" -> EightBitCuteThemeConfig(variant = appearance.eightBitVariant)
                "weather_time" -> WeatherTimeThemeConfig(
                    isDay = weatherIsDay,
                    condition = weatherCondition,
                    style = appearance.weatherStyle
                )
                else -> preset.createConfig()
            }
        }
    }
}

class StandardMaterial3ThemeConfig : ThemeConfig {
    override val id: String = "standard_m3"
    override val displayName: String = "Normal"

    override fun colorScheme(darkTheme: Boolean): ColorScheme =
        if (darkTheme) darkColorScheme() else lightColorScheme()

    override fun typography(): Typography = Typography()

    override fun shapes(tokens: SnowDosDesignTokens): Shapes = Shapes(
        small = RoundedCornerShape(tokens.radius.chip),
        medium = RoundedCornerShape(tokens.radius.card),
        large = RoundedCornerShape(tokens.radius.sheet)
    )

    override fun tokens(darkTheme: Boolean): SnowDosDesignTokens {
        val semantic = if (darkTheme) {
            SnowDosSemanticColors(
                positive = Color(0xFF81C784),
                negative = Color(0xFFE57373),
                warning = Color(0xFFFFB74D)
            )
        } else {
            SnowDosSemanticColors(
                positive = Color(0xFF2E7D32),
                negative = Color(0xFFC62828),
                warning = Color(0xFFEF6C00)
            )
        }
        val surfaceTokens = createFrostedSurfaceTokens(
            darkTheme = darkTheme,
            baseGlassAlpha = 0.24f
        )
        return SnowDosDesignTokens(
            surface = surfaceTokens,
            semantic = semantic
        )
    }
}

internal fun createFrostedSurfaceTokens(
    darkTheme: Boolean,
    baseGlassAlpha: Float
): SnowDosSurfaceTokens {
    val clamped = baseGlassAlpha.coerceIn(0.10f, 0.55f)
    val cardAlpha = if (darkTheme) clamped else (clamped + 0.08f).coerceIn(0.16f, 0.62f)
    val interactiveAlpha = (cardAlpha - 0.02f).coerceIn(0.10f, 0.60f)
    val selectedAlpha = (cardAlpha + 0.16f).coerceIn(0.24f, 0.72f)
    val navPillAlpha = (cardAlpha + 0.06f).coerceIn(0.16f, 0.68f)
    return SnowDosSurfaceTokens(
        cardAlpha = cardAlpha,
        interactiveAlpha = interactiveAlpha,
        selectedAlpha = selectedAlpha,
        navPillAlpha = navPillAlpha,
        blurRadius = 14.dp,
        borderWidth = 1.dp,
        borderHighlightAlpha = if (darkTheme) 0.34f else 0.72f,
        borderShadowAlpha = if (darkTheme) 0.08f else 0.14f,
        borderAlpha = if (darkTheme) 0.20f else 0.14f
    )
}

class PresetThemeConfig(
    override val id: String,
    override val displayName: String,
    private val spec: PresetPaletteSpec
) : ThemeConfig {

    override fun colorScheme(darkTheme: Boolean): ColorScheme {
        val base = if (spec.isDark) darkColorScheme() else lightColorScheme()
        val onPrimary = if (spec.primary.luminance() > 0.45f) Color(0xFF1A1C1E) else Color.White
        val containerMixTarget = if (spec.isDark) Color.Black else Color.White
        val neutralButtonContainer = if (spec.isDark) {
            lerp(spec.surfaceContainerHigh, Color.White, 0.10f)
        } else {
            lerp(spec.surfaceContainerHigh, Color.Black, 0.06f)
        }

        return base.copy(
            primary = spec.primary,
            onPrimary = onPrimary,
            primaryContainer = lerp(spec.primary, containerMixTarget, 0.72f),
            onPrimaryContainer = spec.onBackground,
            secondary = spec.secondaryAccent,
            secondaryContainer = neutralButtonContainer,
            onSecondaryContainer = spec.onBackground,
            background = spec.background,
            onBackground = spec.onBackground,
            surface = spec.surface,
            onSurface = spec.onBackground,
            surfaceVariant = spec.surfaceContainerHigh,
            onSurfaceVariant = spec.onSurfaceVariant,
            surfaceContainer = spec.surfaceContainer,
            surfaceContainerLow = spec.surfaceContainerLow,
            surfaceContainerHigh = spec.surfaceContainerHigh,
            surfaceContainerHighest = lerp(spec.surfaceContainerHigh, spec.onBackground, 0.06f),
            surfaceTint = spec.primary
        )
    }

    override fun typography(): Typography = Typography()

    override fun shapes(tokens: SnowDosDesignTokens): Shapes = Shapes(
        small = RoundedCornerShape(tokens.radius.chip),
        medium = RoundedCornerShape(tokens.radius.card),
        large = RoundedCornerShape(tokens.radius.sheet)
    )

    override fun tokens(darkTheme: Boolean): SnowDosDesignTokens {
        val baseTokens = StandardMaterial3ThemeConfig().tokens(spec.isDark)
        return baseTokens.copy(
            surface = createFrostedSurfaceTokens(
                darkTheme = spec.isDark,
                baseGlassAlpha = 0.22f
            )
        )
    }
}

class CustomAccentThemeConfig(
    private val accentColor: Color,
    private val glassOpacity: Float = 0.24f,
    private val base: ThemeConfig
) : ThemeConfig {
    override val id: String = "custom"
    override val displayName: String = "Custom (${accentColor.toHexString()})"

    override fun colorScheme(darkTheme: Boolean): ColorScheme {
        val baseScheme = base.colorScheme(darkTheme)
        val primary = if (darkTheme) {
            lerp(accentColor, Color.White, 0.22f)
        } else {
            accentColor
        }
        val onPrimary = if (primary.luminance() > 0.45f) Color(0xFF1C1B1F) else Color.White

        val primaryContainer = if (darkTheme) {
            lerp(accentColor, Color(0xFF141218), 0.52f)
        } else {
            lerp(accentColor, Color.White, 0.80f)
        }
        val onPrimaryContainer = if (darkTheme) {
            lerp(accentColor, Color.White, 0.82f)
        } else {
            lerp(accentColor, Color.Black, 0.65f)
        }

        val secondaryContainer = if (darkTheme) {
            lerp(accentColor, Color(0xFF1D1B20), 0.62f)
        } else {
            lerp(accentColor, Color.White, 0.84f)
        }
        val onSecondaryContainer = if (darkTheme) {
            lerp(accentColor, Color.White, 0.80f)
        } else {
            lerp(accentColor, Color.Black, 0.62f)
        }

        return baseScheme.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = primary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            surfaceTint = primary
        )
    }

    override fun typography(): Typography = base.typography()

    override fun shapes(tokens: SnowDosDesignTokens): Shapes = base.shapes(tokens)

    override fun tokens(darkTheme: Boolean): SnowDosDesignTokens {
        val baseTokens = base.tokens(darkTheme)
        return baseTokens.copy(
            surface = createFrostedSurfaceTokens(
                darkTheme = darkTheme,
                baseGlassAlpha = glassOpacity
            )
        )
    }
}

class GamingThemeConfig(
    private val accentColor: Color = Color(0xFF00E5FF)
) : ThemeConfig {
    override val id: String = "gaming"
    override val displayName: String = "Gaming"

    override fun colorScheme(darkTheme: Boolean): ColorScheme {
        val background = Color(0xFF090C14)
        val surfaceContainerLow = Color(0xFF0F131E)
        val surfaceContainer = Color(0xFF141926)
        val surfaceContainerHigh = Color(0xFF1C2336)
        val onBackground = Color(0xFFF0F4FF)
        val onSurfaceVariant = Color(0xFF9AA7C4)
        val primary = accentColor
        val onPrimary = if (primary.luminance() > 0.45f) Color(0xFF090C14) else Color.White

        return darkColorScheme().copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = lerp(primary, Color(0xFF05070C), 0.72f),
            onPrimaryContainer = lerp(primary, Color.White, 0.80f),
            secondary = primary,
            secondaryContainer = lerp(primary, surfaceContainerHigh, 0.74f),
            onSecondaryContainer = lerp(primary, Color.White, 0.84f),
            background = background,
            onBackground = onBackground,
            surface = background,
            onSurface = onBackground,
            surfaceVariant = surfaceContainerHigh,
            onSurfaceVariant = onSurfaceVariant,
            surfaceContainer = surfaceContainer,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = lerp(surfaceContainerHigh, primary, 0.14f),
            surfaceTint = primary
        )
    }

    override fun typography(): Typography = Typography()

    override fun shapes(tokens: SnowDosDesignTokens): Shapes = Shapes(
        small = RoundedCornerShape(tokens.radius.chip),
        medium = RoundedCornerShape(tokens.radius.card),
        large = RoundedCornerShape(tokens.radius.sheet)
    )

    override fun tokens(darkTheme: Boolean): SnowDosDesignTokens {
        val baseTokens = StandardMaterial3ThemeConfig().tokens(darkTheme = true)
        return baseTokens.copy(
            surface = createFrostedSurfaceTokens(
                darkTheme = true,
                baseGlassAlpha = 0.24f
            ).copy(
                glassAccentBorderTint = true,
                borderHighlightAlpha = 0.62f,
                borderAlpha = 0.34f
            )
        )
    }
}

class EightBitCuteThemeConfig(
    val variant: EightBitVariant = EightBitVariant.PASTEL_PINK
) : ThemeConfig {
    override val id: String = "8bit_cute"
    override val displayName: String = "8-Bit Cute"

    override fun colorScheme(darkTheme: Boolean): ColorScheme {
        return when (variant) {
            EightBitVariant.PASTEL_PINK -> lightColorScheme().copy(
                primary = Color(0xFFD83B78),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFB5CF),
                onPrimaryContainer = Color(0xFF3E0E24),
                secondary = Color(0xFF8C4A8E),
                secondaryContainer = Color(0xFFFFC7DD),
                onSecondaryContainer = Color(0xFF350E24),
                background = Color(0xFFFFEEF4),
                onBackground = Color(0xFF311424),
                surface = Color(0xFFFFEEF4),
                onSurface = Color(0xFF311424),
                surfaceVariant = Color(0xFFFFC9DC),
                onSurfaceVariant = Color(0xFF6B3A54),
                surfaceContainer = Color(0xFFFFDCE8),
                surfaceContainerLow = Color(0xFFFFF5F8),
                surfaceContainerHigh = Color(0xFFFFC9DC),
                surfaceContainerHighest = Color(0xFFFFB5CF),
                outline = Color(0xFF4A2036),
                outlineVariant = Color(0xFFB56A8E),
                surfaceTint = Color(0xFFD83B78)
            )
            EightBitVariant.PASTEL_BLUE -> lightColorScheme().copy(
                primary = Color(0xFF2B6FD6),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFAFD2FF),
                onPrimaryContainer = Color(0xFF0E2547),
                secondary = Color(0xFF4A6BA8),
                secondaryContainer = Color(0xFFC3DEFF),
                onSecondaryContainer = Color(0xFF102442),
                background = Color(0xFFECF5FF),
                onBackground = Color(0xFF14243A),
                surface = Color(0xFFECF5FF),
                onSurface = Color(0xFF14243A),
                surfaceVariant = Color(0xFFC2DCFF),
                onSurfaceVariant = Color(0xFF3C5578),
                surfaceContainer = Color(0xFFD7E9FF),
                surfaceContainerLow = Color(0xFFF5F9FF),
                surfaceContainerHigh = Color(0xFFC2DCFF),
                surfaceContainerHighest = Color(0xFFAFD2FF),
                outline = Color(0xFF1E3A5F),
                outlineVariant = Color(0xFF648EC2),
                surfaceTint = Color(0xFF2B6FD6)
            )
        }
    }

    override fun typography(): Typography = eightBitRetroTypography()

    override fun shapes(tokens: SnowDosDesignTokens): Shapes = eightBitRetroShapes(tokens)

    override fun tokens(darkTheme: Boolean): SnowDosDesignTokens = eightBitRetroTokens()
}

class EightBitPixelThemeConfig(
    override val id: String,
    override val displayName: String,
    private val spec: PresetPaletteSpec,
    private val outlineColor: Color
) : ThemeConfig {
    override fun colorScheme(darkTheme: Boolean): ColorScheme = lightColorScheme().copy(
        primary = spec.primary,
        onPrimary = Color.White,
        primaryContainer = lerp(spec.surfaceContainerHigh, spec.primary, 0.22f),
        onPrimaryContainer = spec.onBackground,
        secondary = spec.secondaryAccent,
        secondaryContainer = spec.surfaceContainerHigh,
        onSecondaryContainer = spec.onBackground,
        background = spec.background,
        onBackground = spec.onBackground,
        surface = spec.surface,
        onSurface = spec.onBackground,
        surfaceVariant = spec.surfaceContainerHigh,
        onSurfaceVariant = spec.onSurfaceVariant,
        surfaceContainer = spec.surfaceContainer,
        surfaceContainerLow = spec.surfaceContainerLow,
        surfaceContainerHigh = spec.surfaceContainerHigh,
        surfaceContainerHighest = lerp(spec.surfaceContainerHigh, spec.primary, 0.16f),
        outline = outlineColor,
        outlineVariant = lerp(outlineColor, spec.background, 0.45f),
        surfaceTint = spec.primary
    )

    override fun typography(): Typography = eightBitRetroTypography()

    override fun shapes(tokens: SnowDosDesignTokens): Shapes = eightBitRetroShapes(tokens)

    override fun tokens(darkTheme: Boolean): SnowDosDesignTokens = eightBitRetroTokens()
}

private fun eightBitRetroTypography(): Typography {
    val base = Typography()
    fun TextStyle.asMono(): TextStyle = copy(fontFamily = FontFamily.Monospace)
    return base.copy(
        displayLarge = base.displayLarge.asMono(),
        displayMedium = base.displayMedium.asMono(),
        displaySmall = base.displaySmall.asMono(),
        headlineLarge = base.headlineLarge.asMono(),
        headlineMedium = base.headlineMedium.asMono(),
        headlineSmall = base.headlineSmall.asMono(),
        titleLarge = base.titleLarge.asMono(),
        titleMedium = base.titleMedium.asMono(),
        titleSmall = base.titleSmall.asMono(),
        bodyLarge = base.bodyLarge.asMono(),
        bodyMedium = base.bodyMedium.asMono(),
        bodySmall = base.bodySmall.asMono(),
        labelLarge = base.labelLarge.asMono(),
        labelMedium = base.labelMedium.asMono(),
        labelSmall = base.labelSmall.asMono()
    )
}

private fun eightBitRetroShapes(tokens: SnowDosDesignTokens): Shapes = Shapes(
    small = RoundedCornerShape(tokens.radius.chip),
    medium = RoundedCornerShape(tokens.radius.card),
    large = RoundedCornerShape(tokens.radius.sheet)
)

private fun eightBitRetroTokens(): SnowDosDesignTokens {
    val baseTokens = StandardMaterial3ThemeConfig().tokens(darkTheme = false)
    return baseTokens.copy(
        radius = SnowDosRadiusTokens(
            card = 2.dp,
            compactCard = 2.dp,
            chip = 0.dp,
            pill = 2.dp,
            sheet = 2.dp
        ),
        surface = SnowDosSurfaceTokens(
            cardAlpha = 1f,
            interactiveAlpha = 1f,
            selectedAlpha = 1f,
            navPillAlpha = 1f,
            blurRadius = 0.dp,
            borderWidth = 2.dp,
            borderHighlightAlpha = 1f,
            borderShadowAlpha = 1f,
            borderAlpha = 1f,
            glassAccentBorderTint = false,
            usePixelRetroStyle = true,
            pixelShadowOffsetDp = 4.dp
        )
    )
}

class WeatherTimeThemeConfig(
    private val isDay: Boolean,
    private val condition: WeatherConditionKind,
    private val style: WeatherThemeStyle
) : ThemeConfig {
    override val id: String = "weather_time"
    override val displayName: String = "Weather & Sky"

    private val isStormOrNight: Boolean = !isDay || condition == WeatherConditionKind.THUNDERSTORM_HAIL

    override fun colorScheme(darkTheme: Boolean): ColorScheme {
        val spec = when {
            isStormOrNight -> PresetPaletteSpec(
                isDark = true,
                background = Color(0xFF0E172A),
                surface = Color(0xFF0E172A),
                surfaceContainer = Color(0xFF17243D),
                surfaceContainerLow = Color(0xFF121D33),
                surfaceContainerHigh = Color(0xFF1E2F4F),
                primary = if (condition == WeatherConditionKind.THUNDERSTORM_HAIL) {
                    Color(0xFFFCD34D)
                } else {
                    Color(0xFF8EC5FF)
                },
                secondaryAccent = Color(0xFFA7B8FF),
                onBackground = Color(0xFFF0F5FF),
                onSurfaceVariant = Color(0xFFB3C3DF)
            )
            condition == WeatherConditionKind.OVERCAST ||
                condition == WeatherConditionKind.FOG ||
                condition == WeatherConditionKind.RAIN ||
                condition == WeatherConditionKind.HEAVY_RAIN ||
                condition == WeatherConditionKind.DRIZZLE ||
                condition == WeatherConditionKind.FREEZING_RAIN -> PresetPaletteSpec(
                isDark = false,
                background = Color(0xFFE5ECF4),
                surface = Color(0xFFE5ECF4),
                surfaceContainer = Color(0xFFD6E1ED),
                surfaceContainerLow = Color(0xFFDFEAFA),
                surfaceContainerHigh = Color(0xFFC8D6E6),
                primary = Color(0xFF335E8C),
                secondaryAccent = Color(0xFF6B8FB5),
                onBackground = Color(0xFF162333),
                onSurfaceVariant = Color(0xFF4B5F77)
            )
            else -> PresetPaletteSpec(
                isDark = false,
                background = if (style == WeatherThemeStyle.CUTE_2D) Color(0xFFEAF4FF) else Color(0xFFE4F0FC),
                surface = Color(0xFFEAF4FF),
                surfaceContainer = Color(0xFFDCEBFF),
                surfaceContainerLow = Color(0xFFE5F0FF),
                surfaceContainerHigh = Color(0xFFCDE1FD),
                primary = Color(0xFF2563EB),
                secondaryAccent = Color(0xFFF59E0B),
                onBackground = Color(0xFF132238),
                onSurfaceVariant = Color(0xFF475C78)
            )
        }
        return PresetThemeConfig(id = id, displayName = displayName, spec = spec).colorScheme(spec.isDark)
    }

    override fun typography(): Typography = Typography()

    override fun shapes(tokens: SnowDosDesignTokens): Shapes = Shapes(
        small = RoundedCornerShape(tokens.radius.chip),
        medium = RoundedCornerShape(tokens.radius.card),
        large = RoundedCornerShape(tokens.radius.sheet)
    )

    override fun tokens(darkTheme: Boolean): SnowDosDesignTokens {
        val base = StandardMaterial3ThemeConfig().tokens(darkTheme = isStormOrNight)
        return base.copy(
            surface = createFrostedSurfaceTokens(
                darkTheme = isStormOrNight,
                baseGlassAlpha = if (style == WeatherThemeStyle.REALISTIC) 0.22f else 0.25f
            )
        )
    }
}
