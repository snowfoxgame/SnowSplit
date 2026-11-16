package com.snowsplit.app.domain.model

import androidx.compose.runtime.Immutable

enum class DebtCalculationMode {
    NET,
    GROSS
}

enum class CurrencyOption(val code: String?, val symbol: String?) {
    AUTO(code = null, symbol = null),
    EUR(code = "EUR", symbol = "€"),
    USD(code = "USD", symbol = "$"),
    GBP(code = "GBP", symbol = "£")
}

enum class AppLanguage(val languageTag: String) {
    DE("de"),
    EN("en");

    companion object {
        fun fromStoredValue(raw: String?): AppLanguage = when (raw) {
            "EN" -> EN
            "DE", "SYSTEM", null -> DE
            else -> DE
        }
    }
}

enum class TabTransitionMode {
    ANIMATED,
    INSTANT
}

enum class AccountTabVisibility {
    AUTO,
    ALWAYS,
    IN_SETTINGS;

    fun isVisibleInNavBar(screenWidthDp: Int, fontScale: Float): Boolean = when (this) {
        AUTO -> screenWidthDp >= 380 && fontScale <= 1.15f
        ALWAYS -> true
        IN_SETTINGS -> false
    }
}

@Immutable
data class AppSettings(
    val appearance: ThemeAppearanceConfig = ThemeAppearanceConfig(),
    val reduceLiveBlur: Boolean = false,
    val debtCalculationMode: DebtCalculationMode = DebtCalculationMode.NET,
    val currencyOption: CurrencyOption = CurrencyOption.AUTO,
    val appLanguage: AppLanguage = AppLanguage.DE,
    val tabTransitionMode: TabTransitionMode = TabTransitionMode.ANIMATED,
    val accountTabVisibility: AccountTabVisibility = AccountTabVisibility.AUTO,
    val syncAppIconWithTheme: Boolean = false,
    val crossAppSyncEnabled: Boolean = true,
    val partnerAppConnected: Boolean = false,
    val weatherSnapshot: WeatherSnapshot? = null,
    val meDisplayName: String = "",
    val meAvatarType: AvatarType = AvatarType.ICON,
    val meIconKey: String = "person",
    val mePhotoUri: String? = null
)
