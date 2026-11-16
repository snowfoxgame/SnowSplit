package com.snowsplit.app.core.common

import com.snowsplit.app.core.designsystem.ThemeRegistry
import com.snowsplit.app.domain.model.ThemeAppearanceConfig
import com.snowsplit.app.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIconManagerTest {

    @Test
    fun `base 14 presets resolve to distinct aliases and 8-bit pixel pets fall back to normal alias`() {
        val pixelPetIds = setOf("8bit_cat", "8bit_fox", "8bit_bunny", "8bit_snake")
        val basePresets = ThemeRegistry.presets.filterNot { it.id in pixelPetIds }

        val resolvedAliases = basePresets.map { preset ->
            AppIconManager.resolveAliasClassName(
                appearance = ThemeAppearanceConfig(
                    presetId = preset.id,
                    mode = ThemeMode.PRESET
                ),
                syncWithTheme = true
            )
        }

        assertEquals(14, resolvedAliases.size)
        assertEquals(14, resolvedAliases.toSet().size)
        assertTrue(resolvedAliases.all { it in AppIconManager.ALL_ALIAS_CLASSES })

        pixelPetIds.forEach { petId ->
            val fallback = AppIconManager.resolveAliasClassName(
                appearance = ThemeAppearanceConfig(presetId = petId, mode = ThemeMode.PRESET),
                syncWithTheme = true
            )
            assertEquals(AppIconManager.ALIAS_NORMAL, fallback)
        }
    }

    @Test
    fun `custom theme or disabled sync falls back to default normal alias`() {
        val disabledSync = AppIconManager.resolveAliasClassName(
            appearance = ThemeAppearanceConfig(presetId = "halloween", mode = ThemeMode.PRESET),
            syncWithTheme = false
        )
        assertEquals(AppIconManager.ALIAS_NORMAL, disabledSync)

        val customMode = AppIconManager.resolveAliasClassName(
            appearance = ThemeAppearanceConfig(presetId = "gaming", mode = ThemeMode.CUSTOM),
            syncWithTheme = true
        )
        assertEquals(AppIconManager.ALIAS_NORMAL, customMode)
    }
}
