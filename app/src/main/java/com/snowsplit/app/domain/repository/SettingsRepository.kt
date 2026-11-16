package com.snowsplit.app.domain.repository

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
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>

    suspend fun setThemeId(presetId: String)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setCustomAccentHex(hex: String)
    suspend fun setGamingAccentHex(hex: String)
    suspend fun setEightBitVariant(variant: EightBitVariant)
    suspend fun setWeatherStyle(style: WeatherThemeStyle)
    suspend fun setAutoSeasonalThemeEnabled(enabled: Boolean)
    suspend fun saveWeatherSnapshot(snapshot: WeatherSnapshot)
    suspend fun setGlassOpacity(opacity: Float)
    suspend fun setBackgroundImageUri(uri: String?)
    suspend fun setCustomBackground(uri: String?, hash: String?)
    suspend fun setReduceLiveBlur(enabled: Boolean)

    suspend fun setDebtCalculationMode(mode: DebtCalculationMode)
    suspend fun setCurrencyOption(currency: CurrencyOption)
    suspend fun setAppLanguage(language: AppLanguage)
    suspend fun setTabTransitionMode(mode: TabTransitionMode)
    suspend fun setAccountTabVisibility(visibility: AccountTabVisibility)
    suspend fun setSyncAppIconWithTheme(enabled: Boolean)
    suspend fun setCrossAppSyncEnabled(enabled: Boolean)
    suspend fun setMeProfile(
        displayName: String,
        avatarType: AvatarType,
        iconKey: String,
        photoUri: String?
    )
}
