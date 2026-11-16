package com.snowsplit.app.feature.settings

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowsplit.app.R
import com.snowsplit.app.data.local.media.BackgroundImageStorage
import com.snowsplit.app.data.local.media.PresetBackgroundGenerator
import com.snowsplit.app.data.local.weather.WeatherRepository
import com.snowsplit.app.domain.model.EightBitVariant
import com.snowsplit.app.domain.model.ThemeAppearanceConfig
import com.snowsplit.app.domain.model.ThemeMode
import com.snowsplit.app.domain.model.WeatherSnapshot
import com.snowsplit.app.domain.model.WeatherThemeStyle
import com.snowsplit.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class AppearanceUiState(
    val appearance: ThemeAppearanceConfig = ThemeAppearanceConfig(),
    val presetThumbnails: Map<String, String> = emptyMap(),
    val isImportingBackground: Boolean = false,
    @StringRes val statusMessageResId: Int? = null,
    val weatherSnapshot: WeatherSnapshot? = null,
    val hasLocationPermission: Boolean = false
)

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backgroundImageStorage: BackgroundImageStorage,
    private val presetBackgroundGenerator: PresetBackgroundGenerator,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val isImporting = MutableStateFlow(false)
    private val statusMessageResId = MutableStateFlow<Int?>(null)
    private val presetThumbnails = MutableStateFlow<Map<String, String>>(emptyMap())
    private val locationPermissionState = MutableStateFlow(weatherRepository.hasLocationPermission())

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings()
                .map { it.appearance.gamingAccentHex to it.appearance.eightBitVariant }
                .distinctUntilChanged()
                .collect { (gamingHex, eightBitVariant) ->
                    presetThumbnails.value = presetBackgroundGenerator.getOrCreateAllThumbnails(
                        gamingAccentHex = gamingHex,
                        eightBitVariant = eightBitVariant
                    )
                }
        }
    }

    val uiState: StateFlow<AppearanceUiState> = combine(
        settingsRepository.observeSettings(),
        presetThumbnails,
        isImporting,
        statusMessageResId,
        locationPermissionState
    ) { settings, thumbs, importing, messageRes, hasLocation ->
        AppearanceUiState(
            appearance = settings.appearance,
            presetThumbnails = thumbs,
            isImportingBackground = importing,
            statusMessageResId = messageRes,
            weatherSnapshot = settings.weatherSnapshot,
            hasLocationPermission = hasLocation
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppearanceUiState()
    )

    fun selectPreset(presetId: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(ThemeMode.PRESET)
            settingsRepository.setThemeId(presetId)
        }
    }

    fun updateGamingAccentHex(hex: String) {
        viewModelScope.launch { settingsRepository.setGamingAccentHex(hex) }
    }

    fun selectEightBitVariant(variant: EightBitVariant) {
        viewModelScope.launch { settingsRepository.setEightBitVariant(variant) }
    }

    fun selectWeatherStyle(style: WeatherThemeStyle) {
        viewModelScope.launch { settingsRepository.setWeatherStyle(style) }
    }

    fun setAutoSeasonalTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoSeasonalThemeEnabled(enabled) }
    }

    fun refreshWeather(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            locationPermissionState.value = weatherRepository.hasLocationPermission()
            weatherRepository.getEffectiveWeather(forceRefresh = forceRefresh)
        }
    }

    fun updateGlassOpacity(opacity: Float) {
        viewModelScope.launch { settingsRepository.setGlassOpacity(opacity) }
    }

    fun importBackgroundFromUri(uri: Uri) {
        viewModelScope.launch {
            isImporting.value = true
            statusMessageResId.value = null
            backgroundImageStorage.importFromUriWithHash(uri)
                .onSuccess { (storedUri, hash) ->
                    settingsRepository.setCustomBackground(storedUri, hash)
                    statusMessageResId.value = R.string.appearance_bg_applied
                }
                .onFailure {
                    statusMessageResId.value = R.string.appearance_bg_failed
                }
            isImporting.value = false
        }
    }

    fun resetBackground() {
        viewModelScope.launch {
            backgroundImageStorage.clearStoredImages()
            settingsRepository.setBackgroundImageUri(null)
            statusMessageResId.value = R.string.appearance_bg_reset_msg
        }
    }

    fun consumeMessage() {
        statusMessageResId.value = null
    }
}
