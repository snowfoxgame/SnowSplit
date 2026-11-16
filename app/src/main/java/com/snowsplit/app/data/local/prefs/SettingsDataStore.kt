package com.snowsplit.app.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.snowsplit.app.domain.model.AccountTabVisibility
import com.snowsplit.app.domain.model.AppLanguage
import com.snowsplit.app.domain.model.AppSettings
import com.snowsplit.app.domain.model.AvatarType
import com.snowsplit.app.domain.model.CurrencyOption
import com.snowsplit.app.domain.model.DebtCalculationMode
import com.snowsplit.app.domain.model.EightBitVariant
import com.snowsplit.app.domain.model.TabTransitionMode
import com.snowsplit.app.domain.model.ThemeAppearanceConfig
import com.snowsplit.app.domain.model.ThemeMode
import com.snowsplit.app.domain.model.WeatherConditionMapper
import com.snowsplit.app.domain.model.WeatherSnapshot
import com.snowsplit.app.domain.model.WeatherThemeStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "snowsplit_settings"
)

data class SyncedAppearanceState(
    val appearance: ThemeAppearanceConfig,
    val reduceLiveBlur: Boolean,
    val syncAppIconWithTheme: Boolean,
    val crossAppSyncEnabled: Boolean,
    val partnerAppConnected: Boolean,
    val lastSyncedRevision: Long,
    val localModifiedAtMillis: Long,
    val lastAppliedSyncId: String,
    val lastPublishedSyncId: String,
    val localBackgroundHash: String?,
    val lastSyncedSignature: String?
) {
    fun currentSignature(): String = appearance.syncSignature(
        reduceLiveBlur = reduceLiveBlur,
        localBackgroundHash = localBackgroundHash
    )
}

fun ThemeAppearanceConfig.syncSignature(
    reduceLiveBlur: Boolean,
    localBackgroundHash: String?
): String = buildString {
    append(mode.name).append('|')
    append(effectivePresetId()).append('|')
    append(customAccentHex.uppercase()).append('|')
    append(gamingAccentHex.uppercase()).append('|')
    append(eightBitVariant.name).append('|')
    append(weatherStyle.name).append('|')
    append(autoSeasonalThemeEnabled).append('|')
    append((glassOpacity * 1000f).roundToInt()).append('|')
    append(reduceLiveBlur).append('|')
    append(localBackgroundHash.orEmpty()).append('|')
    append(backgroundImageUri.orEmpty())
}

@Singleton
class SettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val PRESET_ID = stringPreferencesKey("theme_preset_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CUSTOM_ACCENT_HEX = stringPreferencesKey("custom_accent_hex")
        val GAMING_ACCENT_HEX = stringPreferencesKey("gaming_accent_hex")
        val EIGHT_BIT_VARIANT = stringPreferencesKey("eight_bit_variant")
        val WEATHER_STYLE = stringPreferencesKey("weather_style")
        val AUTO_SEASONAL_THEME_ENABLED = booleanPreferencesKey("auto_seasonal_theme_enabled")
        val GLASS_OPACITY = floatPreferencesKey("glass_opacity")
        val BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
        val REDUCE_LIVE_BLUR = booleanPreferencesKey("reduce_live_blur")

        val WEATHER_WMO_CODE = intPreferencesKey("weather_wmo_code")
        val WEATHER_IS_DAY = booleanPreferencesKey("weather_is_day")
        val WEATHER_SUNRISE_MS = longPreferencesKey("weather_sunrise_ms")
        val WEATHER_SUNSET_MS = longPreferencesKey("weather_sunset_ms")
        val WEATHER_FETCHED_MS = longPreferencesKey("weather_fetched_ms")
        val WEATHER_LOCATION_BASED = booleanPreferencesKey("weather_location_based")
        val WEATHER_LAT = doublePreferencesKey("weather_lat")
        val WEATHER_LON = doublePreferencesKey("weather_lon")

        val DEBT_CALC_MODE = stringPreferencesKey("debt_calculation_mode")
        val CURRENCY_OPTION = stringPreferencesKey("currency_option")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val TAB_TRANSITION_MODE = stringPreferencesKey("tab_transition_mode")
        val ACCOUNT_TAB_VISIBILITY = stringPreferencesKey("account_tab_visibility")
        val SYNC_APP_ICON_WITH_THEME = booleanPreferencesKey("sync_app_icon_with_theme")
        val ME_DISPLAY_NAME = stringPreferencesKey("me_display_name")
        val ME_AVATAR_TYPE = stringPreferencesKey("me_avatar_type")
        val ME_ICON_KEY = stringPreferencesKey("me_icon_key")
        val ME_PHOTO_URI = stringPreferencesKey("me_photo_uri")

        val CROSS_APP_SYNC_ENABLED = booleanPreferencesKey("cross_app_sync_enabled")
        val PARTNER_APP_CONNECTED = booleanPreferencesKey("partner_app_connected")
        val LAST_SYNCED_REVISION = longPreferencesKey("sync_last_revision")
        val LOCAL_MODIFIED_AT_MILLIS = longPreferencesKey("sync_local_modified_at")
        val LAST_APPLIED_SYNC_ID = stringPreferencesKey("sync_last_applied_id")
        val LAST_PUBLISHED_SYNC_ID = stringPreferencesKey("sync_last_published_id")
        val LOCAL_BACKGROUND_HASH = stringPreferencesKey("sync_local_background_hash")
        val LAST_SYNCED_SIGNATURE = stringPreferencesKey("sync_last_appearance_signature")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map(::mapToAppSettings)

    val syncStateFlow: Flow<SyncedAppearanceState> = context.settingsDataStore.data.map(::mapToSyncState)

    suspend fun readSyncState(): SyncedAppearanceState =
        mapToSyncState(context.settingsDataStore.data.first())

    suspend fun setThemeId(presetId: String) {
        context.settingsDataStore.edit {
            it[Keys.PRESET_ID] = presetId
            it[Keys.AUTO_SEASONAL_THEME_ENABLED] = false
            touchLocalAppearance(it)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit {
            it[Keys.THEME_MODE] = ThemeMode.PRESET.name
            touchLocalAppearance(it)
        }
    }

    suspend fun setCustomAccentHex(hex: String) {
        context.settingsDataStore.edit {
            it[Keys.CUSTOM_ACCENT_HEX] = hex
            touchLocalAppearance(it)
        }
    }

    suspend fun setGamingAccentHex(hex: String) {
        context.settingsDataStore.edit {
            it[Keys.GAMING_ACCENT_HEX] = hex
            touchLocalAppearance(it)
        }
    }

    suspend fun setEightBitVariant(variant: EightBitVariant) {
        context.settingsDataStore.edit {
            it[Keys.EIGHT_BIT_VARIANT] = variant.name
            touchLocalAppearance(it)
        }
    }

    suspend fun setWeatherStyle(style: WeatherThemeStyle) {
        context.settingsDataStore.edit {
            it[Keys.WEATHER_STYLE] = style.name
            touchLocalAppearance(it)
        }
    }

    suspend fun setAutoSeasonalThemeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.AUTO_SEASONAL_THEME_ENABLED] = enabled
            if (enabled) {
                it[Keys.THEME_MODE] = ThemeMode.PRESET.name
            }
            touchLocalAppearance(it)
        }
    }

    suspend fun setGlassOpacity(opacity: Float) {
        context.settingsDataStore.edit {
            it[Keys.GLASS_OPACITY] = opacity.coerceIn(0.10f, 0.55f)
            touchLocalAppearance(it)
        }
    }

    suspend fun setBackgroundImageUri(uri: String?) {
        setCustomBackground(uri = uri, hash = null)
    }

    suspend fun setCustomBackground(uri: String?, hash: String?) {
        context.settingsDataStore.edit { prefs ->
            if (uri.isNullOrBlank()) {
                prefs.remove(Keys.BACKGROUND_IMAGE_URI)
                prefs.remove(Keys.LOCAL_BACKGROUND_HASH)
            } else {
                prefs[Keys.BACKGROUND_IMAGE_URI] = uri
                if (hash.isNullOrBlank()) {
                    prefs.remove(Keys.LOCAL_BACKGROUND_HASH)
                } else {
                    prefs[Keys.LOCAL_BACKGROUND_HASH] = hash
                }
            }
            touchLocalAppearance(prefs)
        }
    }

    suspend fun setReduceLiveBlur(enabled: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.REDUCE_LIVE_BLUR] = enabled
            touchLocalAppearance(it)
        }
    }

    suspend fun saveWeatherSnapshot(snapshot: WeatherSnapshot) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.WEATHER_WMO_CODE] = snapshot.wmoCode
            prefs[Keys.WEATHER_IS_DAY] = snapshot.isDay
            prefs[Keys.WEATHER_SUNRISE_MS] = snapshot.sunriseEpochMillis
            prefs[Keys.WEATHER_SUNSET_MS] = snapshot.sunsetEpochMillis
            prefs[Keys.WEATHER_FETCHED_MS] = snapshot.fetchedAtEpochMillis
            prefs[Keys.WEATHER_LOCATION_BASED] = snapshot.isLocationBased
            if (snapshot.latitude != null && snapshot.longitude != null) {
                prefs[Keys.WEATHER_LAT] = snapshot.latitude
                prefs[Keys.WEATHER_LON] = snapshot.longitude
            }
        }
    }

    suspend fun setDebtCalculationMode(mode: DebtCalculationMode) {
        context.settingsDataStore.edit { it[Keys.DEBT_CALC_MODE] = mode.name }
    }

    suspend fun setCurrencyOption(currency: CurrencyOption) {
        context.settingsDataStore.edit { it[Keys.CURRENCY_OPTION] = currency.name }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
    }

    suspend fun setTabTransitionMode(mode: TabTransitionMode) {
        context.settingsDataStore.edit { it[Keys.TAB_TRANSITION_MODE] = mode.name }
    }

    suspend fun setAccountTabVisibility(visibility: AccountTabVisibility) {
        context.settingsDataStore.edit { it[Keys.ACCOUNT_TAB_VISIBILITY] = visibility.name }
    }

    suspend fun setSyncAppIconWithTheme(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SYNC_APP_ICON_WITH_THEME] = enabled }
    }

    suspend fun setCrossAppSyncEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.CROSS_APP_SYNC_ENABLED] = enabled }
    }

    suspend fun setPartnerAppConnected(connected: Boolean) {
        context.settingsDataStore.edit { it[Keys.PARTNER_APP_CONNECTED] = connected }
    }

    suspend fun setMeProfile(
        displayName: String,
        avatarType: AvatarType,
        iconKey: String,
        photoUri: String?
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.ME_DISPLAY_NAME] = displayName.trim()
            prefs[Keys.ME_AVATAR_TYPE] = avatarType.name
            prefs[Keys.ME_ICON_KEY] = iconKey
            if (avatarType == AvatarType.PHOTO && !photoUri.isNullOrBlank()) {
                prefs[Keys.ME_PHOTO_URI] = photoUri
            } else {
                prefs.remove(Keys.ME_PHOTO_URI)
            }
        }
    }

    suspend fun applyRemoteAppearanceSync(
        appearance: ThemeAppearanceConfig,
        reduceLiveBlur: Boolean,
        localBackgroundHash: String?,
        syncId: String,
        updatedAtEpochMillis: Long
    ): AppSettings {
        val signature = appearance.syncSignature(
            reduceLiveBlur = reduceLiveBlur,
            localBackgroundHash = localBackgroundHash
        )
        val updatedPrefs = context.settingsDataStore.edit { prefs ->
            prefs[Keys.PRESET_ID] = appearance.presetId
            prefs[Keys.THEME_MODE] = appearance.mode.name
            prefs[Keys.CUSTOM_ACCENT_HEX] = appearance.customAccentHex
            prefs[Keys.GAMING_ACCENT_HEX] = appearance.gamingAccentHex
            prefs[Keys.EIGHT_BIT_VARIANT] = appearance.eightBitVariant.name
            prefs[Keys.WEATHER_STYLE] = appearance.weatherStyle.name
            prefs[Keys.AUTO_SEASONAL_THEME_ENABLED] = appearance.autoSeasonalThemeEnabled
            prefs[Keys.GLASS_OPACITY] = appearance.glassOpacity.coerceIn(0.10f, 0.55f)
            if (appearance.backgroundImageUri.isNullOrBlank()) {
                prefs.remove(Keys.BACKGROUND_IMAGE_URI)
            } else {
                prefs[Keys.BACKGROUND_IMAGE_URI] = appearance.backgroundImageUri
            }
            if (localBackgroundHash.isNullOrBlank()) {
                prefs.remove(Keys.LOCAL_BACKGROUND_HASH)
            } else {
                prefs[Keys.LOCAL_BACKGROUND_HASH] = localBackgroundHash
            }
            prefs[Keys.REDUCE_LIVE_BLUR] = reduceLiveBlur
            prefs[Keys.LAST_APPLIED_SYNC_ID] = syncId
            prefs[Keys.LAST_SYNCED_REVISION] = updatedAtEpochMillis
            prefs[Keys.LOCAL_MODIFIED_AT_MILLIS] = updatedAtEpochMillis
            prefs[Keys.LAST_SYNCED_SIGNATURE] = signature
        }
        return mapToAppSettings(updatedPrefs)
    }

    suspend fun recordOutgoingSync(
        syncId: String,
        revision: Long,
        localBackgroundHash: String?,
        signature: String
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LAST_PUBLISHED_SYNC_ID] = syncId
            prefs[Keys.LAST_SYNCED_REVISION] = revision
            prefs[Keys.LOCAL_MODIFIED_AT_MILLIS] = revision
            if (localBackgroundHash.isNullOrBlank()) {
                prefs.remove(Keys.LOCAL_BACKGROUND_HASH)
            } else {
                prefs[Keys.LOCAL_BACKGROUND_HASH] = localBackgroundHash
            }
            prefs[Keys.LAST_SYNCED_SIGNATURE] = signature
        }
    }

    private fun touchLocalAppearance(prefs: MutablePreferences) {
        val now = System.currentTimeMillis()
        val last = prefs[Keys.LOCAL_MODIFIED_AT_MILLIS] ?: 0L
        prefs[Keys.LOCAL_MODIFIED_AT_MILLIS] = maxOf(now, last + 1L)
    }

    private fun mapToAppSettings(prefs: Preferences): AppSettings = AppSettings(
        appearance = mapToAppearance(prefs),
        reduceLiveBlur = prefs[Keys.REDUCE_LIVE_BLUR] ?: false,
        debtCalculationMode = prefs[Keys.DEBT_CALC_MODE].toEnumOrDefault(DebtCalculationMode.NET),
        currencyOption = prefs[Keys.CURRENCY_OPTION].toEnumOrDefault(CurrencyOption.AUTO),
        appLanguage = AppLanguage.fromStoredValue(prefs[Keys.APP_LANGUAGE]),
        tabTransitionMode = prefs[Keys.TAB_TRANSITION_MODE].toEnumOrDefault(TabTransitionMode.ANIMATED),
        accountTabVisibility = prefs[Keys.ACCOUNT_TAB_VISIBILITY].toEnumOrDefault(AccountTabVisibility.AUTO),
        syncAppIconWithTheme = prefs[Keys.SYNC_APP_ICON_WITH_THEME] ?: false,
        crossAppSyncEnabled = prefs[Keys.CROSS_APP_SYNC_ENABLED] ?: true,
        partnerAppConnected = prefs[Keys.PARTNER_APP_CONNECTED] ?: false,
        weatherSnapshot = mapToWeatherSnapshot(prefs),
        meDisplayName = prefs[Keys.ME_DISPLAY_NAME] ?: "",
        meAvatarType = prefs[Keys.ME_AVATAR_TYPE].toEnumOrDefault(AvatarType.ICON),
        meIconKey = prefs[Keys.ME_ICON_KEY] ?: "person",
        mePhotoUri = prefs[Keys.ME_PHOTO_URI]
    )

    private fun mapToSyncState(prefs: Preferences): SyncedAppearanceState = SyncedAppearanceState(
        appearance = mapToAppearance(prefs),
        reduceLiveBlur = prefs[Keys.REDUCE_LIVE_BLUR] ?: false,
        syncAppIconWithTheme = prefs[Keys.SYNC_APP_ICON_WITH_THEME] ?: false,
        crossAppSyncEnabled = prefs[Keys.CROSS_APP_SYNC_ENABLED] ?: true,
        partnerAppConnected = prefs[Keys.PARTNER_APP_CONNECTED] ?: false,
        lastSyncedRevision = prefs[Keys.LAST_SYNCED_REVISION] ?: 0L,
        localModifiedAtMillis = prefs[Keys.LOCAL_MODIFIED_AT_MILLIS] ?: 0L,
        lastAppliedSyncId = prefs[Keys.LAST_APPLIED_SYNC_ID] ?: "",
        lastPublishedSyncId = prefs[Keys.LAST_PUBLISHED_SYNC_ID] ?: "",
        localBackgroundHash = prefs[Keys.LOCAL_BACKGROUND_HASH],
        lastSyncedSignature = prefs[Keys.LAST_SYNCED_SIGNATURE]
    )

    private fun mapToAppearance(prefs: Preferences): ThemeAppearanceConfig {
        val storedMode = prefs[Keys.THEME_MODE].toEnumOrDefault(ThemeMode.PRESET)
        val resolvedPresetId = if (storedMode == ThemeMode.CUSTOM) {
            "standard_m3"
        } else {
            prefs[Keys.PRESET_ID] ?: "standard_m3"
        }
        return ThemeAppearanceConfig(
            presetId = resolvedPresetId,
            mode = ThemeMode.PRESET,
            customAccentHex = prefs[Keys.CUSTOM_ACCENT_HEX] ?: "#D0BCFF",
            gamingAccentHex = prefs[Keys.GAMING_ACCENT_HEX] ?: "#00E5FF",
            eightBitVariant = prefs[Keys.EIGHT_BIT_VARIANT].toEnumOrDefault(EightBitVariant.PASTEL_PINK),
            weatherStyle = prefs[Keys.WEATHER_STYLE].toEnumOrDefault(WeatherThemeStyle.CUTE_2D),
            autoSeasonalThemeEnabled = prefs[Keys.AUTO_SEASONAL_THEME_ENABLED] ?: false,
            glassOpacity = prefs[Keys.GLASS_OPACITY] ?: 0.24f,
            backgroundImageUri = prefs[Keys.BACKGROUND_IMAGE_URI]
        )
    }

    private fun mapToWeatherSnapshot(prefs: Preferences): WeatherSnapshot? {
        val fetchedAt = prefs[Keys.WEATHER_FETCHED_MS] ?: return null
        val wmoCode = prefs[Keys.WEATHER_WMO_CODE] ?: return null
        val sunrise = prefs[Keys.WEATHER_SUNRISE_MS] ?: return null
        val sunset = prefs[Keys.WEATHER_SUNSET_MS] ?: return null
        return WeatherSnapshot(
            wmoCode = wmoCode,
            condition = WeatherConditionMapper.fromWmoCode(wmoCode),
            isDay = prefs[Keys.WEATHER_IS_DAY] ?: true,
            sunriseEpochMillis = sunrise,
            sunsetEpochMillis = sunset,
            fetchedAtEpochMillis = fetchedAt,
            isLocationBased = prefs[Keys.WEATHER_LOCATION_BASED] ?: false,
            latitude = prefs[Keys.WEATHER_LAT],
            longitude = prefs[Keys.WEATHER_LON]
        )
    }
}

private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
    if (this == null) default else runCatching { enumValueOf<T>(this) }.getOrDefault(default)
