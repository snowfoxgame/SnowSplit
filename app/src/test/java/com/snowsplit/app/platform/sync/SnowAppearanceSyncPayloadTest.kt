package com.snowsplit.app.platform.sync

import com.snowsplit.app.domain.model.EightBitVariant
import com.snowsplit.app.domain.model.SeasonalThemeResolver
import com.snowsplit.app.domain.model.ThemeAppearanceConfig
import com.snowsplit.app.domain.model.ThemeMode
import com.snowsplit.app.domain.model.WeatherThemeStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnowAppearanceSyncPayloadTest {

    @Test
    fun `payload serializes and deserializes preset configuration losslessly`() {
        val original = SnowAppearanceSyncPayload.fromAppearance(
            sourcePackage = PartnerAppConfig.OWN_PACKAGE,
            syncId = "sync-123",
            updatedAtEpochMillis = 1_763_250_000_000L,
            appearance = ThemeAppearanceConfig(
                presetId = "halloween",
                mode = ThemeMode.PRESET,
                customAccentHex = "#FF6D00",
                gamingAccentHex = "#00E5FF",
                eightBitVariant = EightBitVariant.PASTEL_BLUE,
                glassOpacity = 0.28f,
                backgroundImageUri = null
            ),
            reduceLiveBlur = true,
            localBackgroundHash = null
        )

        val json = original.toJson()
        val decoded = SnowAppearanceSyncPayload.fromJson(json)

        assertNotNull(decoded)
        assertEquals(1, decoded!!.schemaVersion)
        assertEquals(PartnerAppConfig.OWN_PACKAGE, decoded.sourcePackage)
        assertEquals("sync-123", decoded.syncId)
        assertEquals(1_763_250_000_000L, decoded.updatedAtEpochMillis)
        assertEquals("PRESET", decoded.themeMode)
        assertEquals("halloween", decoded.presetId)
        assertEquals("#FF6D00", decoded.customAccentHex)
        assertEquals("#00E5FF", decoded.gamingAccentHex)
        assertEquals("PASTEL_BLUE", decoded.eightBitVariant)
        assertEquals(0.28f, decoded.glassOpacity, 0.001f)
        assertTrue(decoded.reduceLiveBlur)
        assertFalse(decoded.hasCustomBackground)
        assertNull(decoded.customBackgroundHash)
        assertNull(decoded.customBackgroundContentUri)
    }

    @Test
    fun `payload preserves all 4 new 8-bit pixel pet presetIds across sync`() {
        listOf("8bit_cat", "8bit_fox", "8bit_bunny", "8bit_snake").forEach { petId ->
            val original = SnowAppearanceSyncPayload.fromAppearance(
                sourcePackage = PartnerAppConfig.OWN_PACKAGE,
                syncId = "sync-$petId",
                updatedAtEpochMillis = 1_763_250_000_000L,
                appearance = ThemeAppearanceConfig(
                    presetId = petId,
                    mode = ThemeMode.PRESET
                ),
                reduceLiveBlur = false,
                localBackgroundHash = null
            )

            val decoded = SnowAppearanceSyncPayload.fromJson(original.toJson())!!
            assertEquals(petId, decoded.presetId)
            assertEquals(petId, decoded.toThemeAppearanceConfig(resolvedBackgroundUri = null).presetId)
        }
    }

    @Test
    fun `payload serializes custom background metadata when image and hash are present`() {
        val original = SnowAppearanceSyncPayload.fromAppearance(
            sourcePackage = PartnerAppConfig.OWN_PACKAGE,
            syncId = "sync-bg-42",
            updatedAtEpochMillis = 2_000_000_000L,
            appearance = ThemeAppearanceConfig(
                presetId = "8bit_cute",
                mode = ThemeMode.CUSTOM,
                customAccentHex = "#12AB34",
                gamingAccentHex = "#FF00AA",
                eightBitVariant = EightBitVariant.PASTEL_PINK,
                glassOpacity = 0.42f,
                backgroundImageUri = "file:///data/user/0/com.snowsplit.app/files/backgrounds/bg.jpg"
            ),
            reduceLiveBlur = false,
            localBackgroundHash = "abcdef1234567890"
        )

        val decoded = SnowAppearanceSyncPayload.fromJson(original.toJson())!!
        assertEquals("PRESET", decoded.themeMode)
        assertTrue(decoded.hasCustomBackground)
        assertEquals("abcdef1234567890", decoded.customBackgroundHash)
        assertEquals(PartnerAppConfig.OWN_BACKGROUND_CONTENT_URI, decoded.customBackgroundContentUri)

        val mapped = decoded.toThemeAppearanceConfig(resolvedBackgroundUri = "file:///local/bg.jpg")
        assertEquals(ThemeMode.PRESET, mapped.mode)
        assertEquals("8bit_cute", mapped.presetId)
        assertEquals(EightBitVariant.PASTEL_PINK, mapped.eightBitVariant)
        assertEquals("file:///local/bg.jpg", mapped.backgroundImageUri)

        // Backwards compatibility: incoming legacy CUSTOM payload still maps cleanly to PRESET
        val legacyCustom = decoded.copy(themeMode = "CUSTOM")
        val legacyMapped = legacyCustom.toThemeAppearanceConfig(resolvedBackgroundUri = null)
        assertEquals(ThemeMode.PRESET, legacyMapped.mode)
        assertEquals("8bit_cute", legacyMapped.presetId)
    }

    @Test
    fun `fromJson rejects unsupported schema version or missing mandatory fields`() {
        val wrongVersion = """{"schemaVersion":2,"sourcePackage":"com.snowdos.app","syncId":"id1","updatedAtEpochMillis":100}"""
        val missingSyncId = """{"schemaVersion":1,"sourcePackage":"com.snowdos.app","syncId":"","updatedAtEpochMillis":100}"""

        assertNull(SnowAppearanceSyncPayload.fromJson(wrongVersion))
        assertNull(SnowAppearanceSyncPayload.fromJson(missingSyncId))
        assertNull(SnowAppearanceSyncPayload.fromJson("not-json"))
    }

    @Test
    fun `loop prevention guards block own package, duplicate syncId, and stale revisions`() {
        val basePayload = SnowAppearanceSyncPayload(
            sourcePackage = PartnerAppConfig.PARTNER_PACKAGE,
            syncId = "sync-new",
            updatedAtEpochMillis = 5_000L,
            themeMode = "PRESET",
            presetId = "winter",
            customAccentHex = "#D0BCFF",
            gamingAccentHex = "#00E5FF",
            eightBitVariant = "PASTEL_PINK",
            glassOpacity = 0.24f,
            reduceLiveBlur = false,
            hasCustomBackground = false
        )

        // Valid newer partner payload is accepted
        assertTrue(
            SyncLoopGuard.shouldApply(
                payload = basePayload,
                ownPackage = PartnerAppConfig.OWN_PACKAGE,
                lastAppliedSyncId = "sync-old",
                lastSyncedRevision = 4_000L
            )
        )

        // Guard 1: sourcePackage == ownPackage is rejected
        assertFalse(
            SyncLoopGuard.shouldApply(
                payload = basePayload.copy(sourcePackage = PartnerAppConfig.OWN_PACKAGE),
                ownPackage = PartnerAppConfig.OWN_PACKAGE,
                lastAppliedSyncId = "sync-old",
                lastSyncedRevision = 4_000L
            )
        )

        // Guard 2: duplicate syncId is rejected
        assertFalse(
            SyncLoopGuard.shouldApply(
                payload = basePayload.copy(syncId = "sync-old"),
                ownPackage = PartnerAppConfig.OWN_PACKAGE,
                lastAppliedSyncId = "sync-old",
                lastSyncedRevision = 4_000L
            )
        )

        // Guard 3: equal or older revision is rejected
        assertFalse(
            SyncLoopGuard.shouldApply(
                payload = basePayload.copy(updatedAtEpochMillis = 5_000L),
                ownPackage = PartnerAppConfig.OWN_PACKAGE,
                lastAppliedSyncId = "sync-old",
                lastSyncedRevision = 5_000L
            )
        )
        assertFalse(
            SyncLoopGuard.shouldApply(
                payload = basePayload.copy(updatedAtEpochMillis = 3_999L),
                ownPackage = PartnerAppConfig.OWN_PACKAGE,
                lastAppliedSyncId = "sync-old",
                lastSyncedRevision = 5_000L
            )
        )
    }

    @Test
    fun `background hash check skips copying identical hashes and copies changed or missing files`() {
        assertFalse(
            SyncLoopGuard.shouldCopyBackground(
                hasCustomBackground = false,
                incomingHash = "abc",
                localBackgroundHash = "def",
                hasExistingLocalFile = true
            )
        )
        assertFalse(
            SyncLoopGuard.shouldCopyBackground(
                hasCustomBackground = true,
                incomingHash = "ABC123",
                localBackgroundHash = "abc123",
                hasExistingLocalFile = true
            )
        )
        assertTrue(
            SyncLoopGuard.shouldCopyBackground(
                hasCustomBackground = true,
                incomingHash = "new_hash",
                localBackgroundHash = "abc123",
                hasExistingLocalFile = true
            )
        )
        assertTrue(
            SyncLoopGuard.shouldCopyBackground(
                hasCustomBackground = true,
                incomingHash = "abc123",
                localBackgroundHash = "abc123",
                hasExistingLocalFile = false
            )
        )
    }

    @Test
    fun `payload preserves weatherStyle and autoSeasonalThemeEnabled and stays backward compatible with legacy v1`() {
        val withExtensions = SnowAppearanceSyncPayload.fromAppearance(
            sourcePackage = PartnerAppConfig.OWN_PACKAGE,
            syncId = "sync-weather-1",
            updatedAtEpochMillis = 9_999L,
            appearance = ThemeAppearanceConfig(
                presetId = "standard_m3",
                mode = ThemeMode.PRESET,
                weatherStyle = WeatherThemeStyle.REALISTIC,
                autoSeasonalThemeEnabled = true
            ),
            reduceLiveBlur = false,
            localBackgroundHash = null
        )

        // When seasonal theme is enabled, outgoing presetId uses resolved effectivePresetId()
        assertEquals(SeasonalThemeResolver.resolvePresetId(), withExtensions.presetId)

        val decoded = SnowAppearanceSyncPayload.fromJson(withExtensions.toJson())!!
        assertEquals(WeatherThemeStyle.REALISTIC.name, decoded.weatherStyle)
        assertTrue(decoded.autoSeasonalThemeEnabled)
        val mapped = decoded.toThemeAppearanceConfig(resolvedBackgroundUri = null)
        assertEquals(WeatherThemeStyle.REALISTIC, mapped.weatherStyle)
        assertTrue(mapped.autoSeasonalThemeEnabled)

        val legacyJsonWithoutNewFields = """
            {"schemaVersion":1,"sourcePackage":"com.snowdos.app","syncId":"old-1","updatedAtEpochMillis":500,"themeMode":"PRESET","presetId":"winter","customAccentHex":"#D0BCFF","gamingAccentHex":"#00E5FF","eightBitVariant":"PASTEL_PINK","glassOpacity":0.24,"reduceLiveBlur":false,"hasCustomBackground":false}
        """.trimIndent()
        val legacyDecoded = SnowAppearanceSyncPayload.fromJson(legacyJsonWithoutNewFields)!!
        assertEquals(WeatherThemeStyle.CUTE_2D.name, legacyDecoded.weatherStyle)
        assertFalse(legacyDecoded.autoSeasonalThemeEnabled)
    }
}
