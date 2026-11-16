package com.snowsplit.app.data.repository

import com.snowsplit.app.data.local.prefs.SettingsDataStore
import com.snowsplit.app.domain.model.AccountTabVisibility
import com.snowsplit.app.domain.model.AppLanguage
import com.snowsplit.app.domain.model.AppSettings
import com.snowsplit.app.domain.model.AvatarType
import com.snowsplit.app.domain.model.CurrencyOption
import com.snowsplit.app.domain.model.DebtCalculationMode
import com.snowsplit.app.domain.model.EightBitVariant
import com.snowsplit.app.domain.model.TabTransitionMode
import com.snowsplit.app.domain.model.ThemeMode
import com.snowsplit.app.domain.model.WeatherSnapshot
import com.snowsplit.app.domain.model.WeatherThemeStyle
import com.snowsplit.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineSettingsRepository @Inject constructor(
    private val dataStore: SettingsDataStore
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> = dataStore.settingsFlow

    override suspend fun setThemeId(presetId: String) = dataStore.setThemeId(presetId)

    override suspend fun setThemeMode(mode: ThemeMode) = dataStore.setThemeMode(mode)

    override suspend fun setCustomAccentHex(hex: String) = dataStore.setCustomAccentHex(hex)

    override suspend fun setGamingAccentHex(hex: String) = dataStore.setGamingAccentHex(hex)

    override suspend fun setEightBitVariant(variant: EightBitVariant) =
        dataStore.setEightBitVariant(variant)

    override suspend fun setWeatherStyle(style: WeatherThemeStyle) =
        dataStore.setWeatherStyle(style)

    override suspend fun setAutoSeasonalThemeEnabled(enabled: Boolean) =
        dataStore.setAutoSeasonalThemeEnabled(enabled)

    override suspend fun saveWeatherSnapshot(snapshot: WeatherSnapshot) =
        dataStore.saveWeatherSnapshot(snapshot)

    override suspend fun setGlassOpacity(opacity: Float) = dataStore.setGlassOpacity(opacity)

    override suspend fun setBackgroundImageUri(uri: String?) = dataStore.setBackgroundImageUri(uri)

    override suspend fun setCustomBackground(uri: String?, hash: String?) =
        dataStore.setCustomBackground(uri, hash)

    override suspend fun setReduceLiveBlur(enabled: Boolean) = dataStore.setReduceLiveBlur(enabled)

    override suspend fun setDebtCalculationMode(mode: DebtCalculationMode) =
        dataStore.setDebtCalculationMode(mode)

    override suspend fun setCurrencyOption(currency: CurrencyOption) =
        dataStore.setCurrencyOption(currency)

    override suspend fun setAppLanguage(language: AppLanguage) = dataStore.setAppLanguage(language)

    override suspend fun setTabTransitionMode(mode: TabTransitionMode) =
        dataStore.setTabTransitionMode(mode)

    override suspend fun setAccountTabVisibility(visibility: AccountTabVisibility) =
        dataStore.setAccountTabVisibility(visibility)

    override suspend fun setSyncAppIconWithTheme(enabled: Boolean) =
        dataStore.setSyncAppIconWithTheme(enabled)

    override suspend fun setCrossAppSyncEnabled(enabled: Boolean) =
        dataStore.setCrossAppSyncEnabled(enabled)

    override suspend fun setMeProfile(
        displayName: String,
        avatarType: AvatarType,
        iconKey: String,
        photoUri: String?
    ) = dataStore.setMeProfile(displayName, avatarType, iconKey, photoUri)
}
