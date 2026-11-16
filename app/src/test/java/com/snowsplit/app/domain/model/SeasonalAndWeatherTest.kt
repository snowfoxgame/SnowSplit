package com.snowsplit.app.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.snowsplit.app.core.designsystem.ThemeCategory
import com.snowsplit.app.core.designsystem.ThemeRegistry
import com.snowsplit.app.data.local.weather.OpenMeteoWeatherParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SeasonalAndWeatherTest {

    @Test
    fun `theme registry contains all 18 presets including live and 8-bit pixel pet designs`() {
        assertEquals(18, ThemeRegistry.presets.size)
        val ids = ThemeRegistry.presets.map { it.id }.toSet()
        assertTrue("interactive_bubbles" in ids)
        assertTrue("mini_city" in ids)
        assertTrue("weather_time" in ids)
        assertTrue("8bit_cat" in ids)
        assertTrue("8bit_fox" in ids)
        assertTrue("8bit_bunny" in ids)
        assertTrue("8bit_snake" in ids)
    }

    @Test
    fun `every theme preset maps to its expected ThemeCategory and filter helper partitions cleanly`() {
        val expectedByCategory = mapOf(
            ThemeCategory.NORMAL to setOf("standard_m3", "spring", "summer", "autumn", "winter"),
            ThemeCategory.EVENTS to setOf("easter", "halloween", "christmas", "new_year"),
            ThemeCategory.PLAYFUL to setOf(
                "gaming",
                "8bit_cute",
                "mini_city",
                "8bit_cat",
                "8bit_fox",
                "8bit_bunny",
                "8bit_snake"
            ),
            ThemeCategory.INTERACTIVE to setOf("interactive_bubbles", "weather_time")
        )

        expectedByCategory.forEach { (category, expectedIds) ->
            val actualIds = ThemeRegistry.presetsFor(category).map { it.id }.toSet()
            assertEquals(expectedIds, actualIds)
            expectedIds.forEach { id ->
                assertEquals(category, ThemeRegistry.findPreset(id).category)
            }
        }

        assertEquals(ThemeRegistry.presets, ThemeRegistry.presetsFor(null))
        assertEquals(
            ThemeRegistry.presets.size,
            expectedByCategory.values.sumOf { it.size }
        )
    }

    @Test
    fun `all 4 new 8-bit pixel pet presets enable retro pixel tokens`() {
        listOf("8bit_cat", "8bit_fox", "8bit_bunny", "8bit_snake").forEach { presetId ->
            val config = ThemeRegistry.resolve(
                ThemeAppearanceConfig(mode = ThemeMode.PRESET, presetId = presetId)
            )
            val tokens = config.tokens(darkTheme = false)
            assertTrue(tokens.surface.usePixelRetroStyle)
            assertEquals(0.dp, tokens.surface.blurRadius)
            assertEquals(2.dp, tokens.surface.borderWidth)
            assertEquals(4.dp, tokens.surface.pixelShadowOffsetDp)
            assertEquals(2.dp, tokens.radius.card)
            assertEquals(0.dp, tokens.radius.chip)
        }
    }

    @Test
    fun `seasonal resolver resolves holiday windows, easter computus, and meteorological seasons`() {
        assertEquals("new_year", SeasonalThemeResolver.resolvePresetId(LocalDate.of(2026, 12, 31)))
        assertEquals("new_year", SeasonalThemeResolver.resolvePresetId(LocalDate.of(2026, 1, 6)))
        assertEquals("christmas", SeasonalThemeResolver.resolvePresetId(LocalDate.of(2026, 12, 24)))
        assertEquals("halloween", SeasonalThemeResolver.resolvePresetId(LocalDate.of(2026, 10, 31)))

        // Easter Sunday 2026 is April 5; Good Friday (-2) .. Easter Tuesday (+2) resolve to easter
        assertEquals(LocalDate.of(2026, 4, 5), SeasonalThemeResolver.computeEasterSunday(2026))
        assertEquals("easter", SeasonalThemeResolver.resolvePresetId(LocalDate.of(2026, 4, 3)))
        assertEquals("easter", SeasonalThemeResolver.resolvePresetId(LocalDate.of(2026, 4, 6)))

        assertEquals("spring", SeasonalThemeResolver.resolvePresetId(LocalDate.of(2026, 5, 10)))
        assertEquals("summer", SeasonalThemeResolver.resolvePresetId(LocalDate.of(2026, 7, 15)))
        assertEquals("autumn", SeasonalThemeResolver.resolvePresetId(LocalDate.of(2026, 10, 10)))
        assertEquals("winter", SeasonalThemeResolver.resolvePresetId(LocalDate.of(2026, 2, 10)))
    }

    @Test
    fun `effectivePresetId uses seasonal resolution only in PRESET mode when enabled`() {
        val seasonalDate = LocalDate.of(2026, 10, 31)
        val seasonal = ThemeAppearanceConfig(
            mode = ThemeMode.PRESET,
            presetId = "winter",
            autoSeasonalThemeEnabled = true
        )
        assertEquals("halloween", seasonal.effectivePresetId(seasonalDate))

        val customMode = seasonal.copy(mode = ThemeMode.CUSTOM)
        assertEquals("winter", customMode.effectivePresetId(seasonalDate))

        val disabled = seasonal.copy(autoSeasonalThemeEnabled = false)
        assertEquals("winter", disabled.effectivePresetId(seasonalDate))
    }

    @Test
    fun `weather condition mapper covers all WMO codes 0 to 99 and day-night fallback window`() {
        assertEquals(WeatherConditionKind.CLEAR, WeatherConditionMapper.fromWmoCode(0))
        assertEquals(WeatherConditionKind.PARTLY_CLOUDY, WeatherConditionMapper.fromWmoCode(2))
        assertEquals(WeatherConditionKind.OVERCAST, WeatherConditionMapper.fromWmoCode(3))
        assertEquals(WeatherConditionKind.FOG, WeatherConditionMapper.fromWmoCode(45))
        assertEquals(WeatherConditionKind.DRIZZLE, WeatherConditionMapper.fromWmoCode(53))
        assertEquals(WeatherConditionKind.RAIN, WeatherConditionMapper.fromWmoCode(61))
        assertEquals(WeatherConditionKind.HEAVY_RAIN, WeatherConditionMapper.fromWmoCode(82))
        assertEquals(WeatherConditionKind.FREEZING_RAIN, WeatherConditionMapper.fromWmoCode(66))
        assertEquals(WeatherConditionKind.SNOW_LIGHT, WeatherConditionMapper.fromWmoCode(71))
        assertEquals(WeatherConditionKind.SNOW_HEAVY, WeatherConditionMapper.fromWmoCode(75))
        assertEquals(WeatherConditionKind.THUNDERSTORM_HAIL, WeatherConditionMapper.fromWmoCode(95))

        val zone = ZoneId.of("UTC")
        val noon = LocalDate.of(2026, 6, 15).atTime(LocalTime.of(12, 0)).atZone(zone).toInstant().toEpochMilli()
        val night = LocalDate.of(2026, 6, 15).atTime(LocalTime.of(23, 0)).atZone(zone).toInstant().toEpochMilli()

        assertTrue(WeatherConditionMapper.resolveTimeFallbackSnapshot(noon, zone).isDay)
        assertFalse(WeatherConditionMapper.resolveTimeFallbackSnapshot(night, zone).isDay)
    }

    @Test
    fun `open meteo parser extracts wmo code, isDay, and sunrise-sunset timestamps`() {
        val sampleJson = """
            {
              "current": {"time": "2026-06-15T14:00", "weather_code": 61, "is_day": 1},
              "daily": {"sunrise": ["2026-06-15T05:15"], "sunset": ["2026-06-15T21:30"]}
            }
        """.trimIndent()

        val parsed = OpenMeteoWeatherParser.parseResponse(
            json = sampleJson,
            latitude = 52.52,
            longitude = 13.40,
            nowMillis = 1_781_520_000_000L,
            zoneId = ZoneId.of("UTC")
        )
        assertNotNull(parsed)
        assertEquals(61, parsed!!.wmoCode)
        assertEquals(WeatherConditionKind.RAIN, parsed.condition)
        assertTrue(parsed.isDay)
        assertTrue(parsed.isLocationBased)
    }

    @Test
    fun `polished light themes winter, summer, spring, easter match exact reference palette`() {
        val winter = ThemeRegistry.findPreset("winter").paletteSpec!!
        assertFalse(winter.isDark)
        assertEquals(Color(0xFFCDE2F6), winter.background)
        assertEquals(Color(0xFF1D6FA5), winter.primary)
        assertEquals(Color(0xFFF3F9FF), winter.surfaceContainerLow)

        val summer = ThemeRegistry.findPreset("summer").paletteSpec!!
        assertFalse(summer.isDark)
        assertEquals(Color(0xFFBBE4F2), summer.background)
        assertEquals(Color(0xFF007A8C), summer.primary)
        assertEquals(Color(0xFFF0FBFE), summer.surfaceContainerLow)

        val spring = ThemeRegistry.findPreset("spring").paletteSpec!!
        assertFalse(spring.isDark)
        assertEquals(Color(0xFFC5E6CC), spring.background)
        assertEquals(Color(0xFF2E7D4F), spring.primary)
        assertEquals(Color(0xFFF1FAF3), spring.surfaceContainerLow)

        val easter = ThemeRegistry.findPreset("easter").paletteSpec!!
        assertFalse(easter.isDark)
        assertEquals(Color(0xFFDFC8F0), easter.background)
        assertEquals(Color(0xFF7D489E), easter.primary)
        assertEquals(Color(0xFFFAF4FE), easter.surfaceContainerLow)
    }
}
