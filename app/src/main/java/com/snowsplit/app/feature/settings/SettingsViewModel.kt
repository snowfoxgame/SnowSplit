package com.snowsplit.app.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowsplit.app.core.common.LocaleHelper
import com.snowsplit.app.data.local.media.ContactAvatarStorage
import com.snowsplit.app.domain.model.AccountTabVisibility
import com.snowsplit.app.domain.model.AppLanguage
import com.snowsplit.app.domain.model.AppSettings
import com.snowsplit.app.domain.model.AvatarType
import com.snowsplit.app.domain.model.CurrencyOption
import com.snowsplit.app.domain.model.DebtCalculationMode
import com.snowsplit.app.domain.model.TabTransitionMode
import com.snowsplit.app.domain.repository.SettingsRepository
import com.snowsplit.app.platform.sync.ThemeSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val contactAvatarStorage: ContactAvatarStorage,
    private val themeSyncManager: ThemeSyncManager
) : ViewModel() {

    val settingsState: StateFlow<AppSettings> = settingsRepository.observeSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings()
        )

    fun setDebtCalculationMode(mode: DebtCalculationMode) {
        viewModelScope.launch { settingsRepository.setDebtCalculationMode(mode) }
    }

    fun setCurrencyOption(currency: CurrencyOption) {
        viewModelScope.launch { settingsRepository.setCurrencyOption(currency) }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setAppLanguage(language)
            LocaleHelper.applyLanguage(language)
        }
    }

    fun setTabTransitionMode(mode: TabTransitionMode) {
        viewModelScope.launch { settingsRepository.setTabTransitionMode(mode) }
    }

    fun setAccountTabVisibility(visibility: AccountTabVisibility) {
        viewModelScope.launch { settingsRepository.setAccountTabVisibility(visibility) }
    }

    fun setSyncAppIconWithTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSyncAppIconWithTheme(enabled) }
    }

    fun setCrossAppSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCrossAppSyncEnabled(enabled)
            themeSyncManager.onCrossAppSyncToggled(enabled)
        }
    }

    fun setReduceLiveBlur(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReduceLiveBlur(enabled) }
    }

    suspend fun importMeAvatarPhoto(uri: Uri): Result<String> =
        contactAvatarStorage.importFromUri(uri, previousLocalUri = settingsState.value.mePhotoUri)

    fun setMeProfile(
        displayName: String,
        avatarType: AvatarType,
        iconKey: String,
        photoUri: String?
    ) {
        viewModelScope.launch {
            if (avatarType == AvatarType.ICON && settingsState.value.mePhotoUri != null) {
                contactAvatarStorage.deleteLocalAvatarIfManaged(settingsState.value.mePhotoUri)
            }
            settingsRepository.setMeProfile(displayName, avatarType, iconKey, photoUri)
        }
    }
}
