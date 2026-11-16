package com.snowsplit.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snowsplit.app.core.common.AppIconManager
import com.snowsplit.app.core.common.LocaleHelper
import com.snowsplit.app.core.designsystem.SnowDosTheme
import com.snowsplit.app.core.designsystem.ThemeRegistry
import com.snowsplit.app.core.designsystem.components.AppBackgroundContainer
import com.snowsplit.app.feature.settings.SettingsViewModel
import com.snowsplit.app.feature.shell.MainShellScreen
import com.snowsplit.app.feature.shell.MainShellViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var appIconManager: AppIconManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val shellViewModel: MainShellViewModel = hiltViewModel()
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeState by shellViewModel.themeState.collectAsStateWithLifecycle()

            LaunchedEffect(themeState.appLanguage) {
                LocaleHelper.applyLanguage(themeState.appLanguage)
            }

            LaunchedEffect(themeState.appearance, themeState.syncAppIconWithTheme) {
                appIconManager.scheduleDesiredIcon(
                    appearance = themeState.appearance,
                    syncWithTheme = themeState.syncAppIconWithTheme
                )
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, shellViewModel) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        shellViewModel.refreshWeatherIfNeeded()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val effectiveDark = ThemeRegistry.isEffectiveDarkTheme(
                appearance = themeState.appearance,
                systemDarkTheme = isSystemInDarkTheme(),
                weatherIsDay = themeState.weatherIsDay,
                weatherCondition = themeState.weatherCondition
            )

            LaunchedEffect(effectiveDark) {
                val barStyle = if (effectiveDark) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        AndroidColor.TRANSPARENT,
                        AndroidColor.TRANSPARENT
                    )
                }
                enableEdgeToEdge(
                    statusBarStyle = barStyle,
                    navigationBarStyle = barStyle
                )
            }

            SnowDosTheme(
                appearance = themeState.appearance,
                darkTheme = effectiveDark,
                reduceLiveBlur = themeState.reduceLiveBlur,
                weatherIsDay = themeState.weatherIsDay,
                weatherCondition = themeState.weatherCondition
            ) {
                AppBackgroundContainer(
                    backgroundImageUri = themeState.effectiveBackgroundUri,
                    darkTheme = effectiveDark,
                    reduceLiveBlur = themeState.reduceLiveBlur,
                    liveScene = themeState.liveScene
                ) {
                    val shellState by shellViewModel.uiState.collectAsStateWithLifecycle()
                    MainShellScreen(
                        settings = shellState.settings,
                        onToggleCalculationMode = settingsViewModel::setDebtCalculationMode
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        appIconManager.applyPendingIconIfNeeded()
    }
}
