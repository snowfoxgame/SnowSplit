package com.snowsplit.app.feature.shell

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowsplit.app.core.designsystem.ThemeRegistry
import com.snowsplit.app.core.designsystem.components.backgrounds.LiveBackgroundScene
import com.snowsplit.app.data.local.media.PresetBackgroundGenerator
import com.snowsplit.app.data.local.weather.WeatherRepository
import com.snowsplit.app.domain.model.AppLanguage
import com.snowsplit.app.domain.model.AppSettings
import com.snowsplit.app.domain.model.ThemeAppearanceConfig
import com.snowsplit.app.domain.model.ThemeMode
import com.snowsplit.app.domain.model.WeatherConditionKind
import com.snowsplit.app.domain.model.WeatherConditionMapper
import com.snowsplit.app.domain.model.WeatherSnapshot
import com.snowsplit.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppTab {
    ENTRIES,
    BUBBLES,
    ACCOUNT,
    SETTINGS
}

@Immutable
data class MainThemeUiState(
    val appearance: ThemeAppearanceConfig = ThemeAppearanceConfig(),
    val reduceLiveBlur: Boolean = false,
    val appLanguage: AppLanguage = AppLanguage.DE,
    val syncAppIconWithTheme: Boolean = false,
    val effectiveBackgroundUri: String? = null,
    val liveScene: LiveBackgroundScene = LiveBackgroundScene.None,
    val weatherIsDay: Boolean = true,
    val weatherCondition: WeatherConditionKind = WeatherConditionKind.CLEAR
)

@Immutable
data class MainShellUiState(
    val settings: AppSettings = AppSettings(),
    val effectiveBackgroundUri: String? = null,
    val liveScene: LiveBackgroundScene = LiveBackgroundScene.None,
    val weatherIsDay: Boolean = true,
    val weatherCondition: WeatherConditionKind = WeatherConditionKind.CLEAR
)

@HiltViewModel
class MainShellViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val presetBackgroundGenerator: PresetBackgroundGenerator,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val generatedBackgroundUri = MutableStateFlow<String?>(null)
    private val liveWeatherSnapshot = MutableStateFlow<WeatherSnapshot?>(null)

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings()
                .map { it.appearance }
                .distinctUntilChanged()
                .collect { appearance ->
                    val effectivePresetId = appearance.effectivePresetId()
                    if (appearance.mode == ThemeMode.PRESET && effectivePresetId == WEATHER_PRESET_ID) {
                        liveWeatherSnapshot.value = weatherRepository.getEffectiveWeather(forceRefresh = false)
                    }
                    if (!appearance.backgroundImageUri.isNullOrBlank()) {
                        generatedBackgroundUri.value = appearance.backgroundImageUri
                    } else if (appearance.mode == ThemeMode.PRESET && effectivePresetId in LIVE_SCENE_PRESETS) {
                        generatedBackgroundUri.value = null
                    } else {
                        val snapshot = liveWeatherSnapshot.value
                        val celestial = snapshot?.let {
                            WeatherConditionMapper.computeCelestialState(
                                nowMillis = System.currentTimeMillis(),
                                sunriseMillis = it.sunriseEpochMillis,
                                sunsetMillis = it.sunsetEpochMillis
                            )
                        }
                        val isDark = ThemeRegistry.isEffectiveDarkTheme(
                            appearance = appearance,
                            systemDarkTheme = isSystemDarkTheme(),
                            weatherIsDay = celestial?.isDay ?: true,
                            weatherCondition = snapshot?.condition ?: WeatherConditionKind.CLEAR
                        )
                        generatedBackgroundUri.value =
                            presetBackgroundGenerator.getOrCreateBackgroundUri(
                                presetId = effectivePresetId,
                                darkTheme = isDark,
                                gamingAccentHex = appearance.gamingAccentHex,
                                eightBitVariant = appearance.eightBitVariant
                            )
                    }
                }
        }
    }

    fun refreshWeatherIfNeeded() {
        viewModelScope.launch {
            val appearance = uiState.value.settings.appearance
            if (appearance.mode == ThemeMode.PRESET && appearance.effectivePresetId() == WEATHER_PRESET_ID) {
                liveWeatherSnapshot.value = weatherRepository.getEffectiveWeather(forceRefresh = false)
            }
        }
    }

    val uiState: StateFlow<MainShellUiState> = combine(
        settingsRepository.observeSettings(),
        generatedBackgroundUri,
        liveWeatherSnapshot
    ) { settings, bgUri, weather ->
        val appearance = settings.appearance
        val effectivePresetId = appearance.effectivePresetId()
        val effectiveWeather = weather ?: settings.weatherSnapshot
            ?: WeatherConditionMapper.resolveTimeFallbackSnapshot()
        val celestial = WeatherConditionMapper.computeCelestialState(
            nowMillis = System.currentTimeMillis(),
            sunriseMillis = effectiveWeather.sunriseEpochMillis,
            sunsetMillis = effectiveWeather.sunsetEpochMillis
        )
        val liveScene = if (appearance.mode == ThemeMode.PRESET) {
            when (effectivePresetId) {
                "interactive_bubbles" -> LiveBackgroundScene.InteractiveBubbles
                "mini_city" -> LiveBackgroundScene.MiniCity
                WEATHER_PRESET_ID -> LiveBackgroundScene.WeatherTime(
                    style = appearance.weatherStyle,
                    snapshot = effectiveWeather
                )
                else -> LiveBackgroundScene.None
            }
        } else {
            LiveBackgroundScene.None
        }

        MainShellUiState(
            settings = settings,
            effectiveBackgroundUri = if (liveScene != LiveBackgroundScene.None) {
                null
            } else {
                appearance.backgroundImageUri ?: bgUri
            },
            liveScene = liveScene,
            weatherIsDay = celestial.isDay,
            weatherCondition = effectiveWeather.condition
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainShellUiState()
    )

    val themeState: StateFlow<MainThemeUiState> = uiState
        .map { state ->
            MainThemeUiState(
                appearance = state.settings.appearance,
                reduceLiveBlur = state.settings.reduceLiveBlur,
                appLanguage = state.settings.appLanguage,
                syncAppIconWithTheme = state.settings.syncAppIconWithTheme,
                effectiveBackgroundUri = state.effectiveBackgroundUri,
                liveScene = state.liveScene,
                weatherIsDay = state.weatherIsDay,
                weatherCondition = state.weatherCondition
            )
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainThemeUiState()
        )

    private fun isSystemDarkTheme(): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    companion object {
        private const val WEATHER_PRESET_ID = "weather_time"
        private val LIVE_SCENE_PRESETS = setOf("interactive_bubbles", "mini_city", WEATHER_PRESET_ID)
    }
}
