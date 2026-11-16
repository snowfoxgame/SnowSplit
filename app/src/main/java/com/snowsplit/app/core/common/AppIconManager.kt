package com.snowsplit.app.core.common

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.snowsplit.app.domain.model.AppSettings
import com.snowsplit.app.domain.model.ThemeAppearanceConfig
import com.snowsplit.app.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Switches SnowSplit's launcher icon dynamically across all 11 `<activity-alias>` entries.
 * Always enables the new alias with [PackageManager.DONT_KILL_APP] BEFORE disabling old aliases,
 * and applies queued switches cleanly when the activity moves to background (`onStop`) to prevent
 * launcher-change restarts mid-interaction.
 */
@Singleton
class AppIconManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    @Volatile
    private var desiredAliasClassName: String? = null

    fun scheduleDesiredIcon(settings: AppSettings) {
        scheduleDesiredIcon(
            appearance = settings.appearance,
            syncWithTheme = settings.syncAppIconWithTheme
        )
    }

    fun scheduleDesiredIcon(
        appearance: ThemeAppearanceConfig,
        syncWithTheme: Boolean
    ) {
        desiredAliasClassName = resolveAliasClassName(
            appearance = appearance,
            syncWithTheme = syncWithTheme
        )
    }

    fun applyPendingIconIfNeeded() {
        val targetAlias = desiredAliasClassName ?: return
        applyAliasNow(targetAlias)
    }

    internal fun applyAliasNow(targetAliasClassName: String) {
        val packageManager = context.packageManager
        val packageName = context.packageName
        val validTarget = if (targetAliasClassName in ALL_ALIAS_CLASSES) {
            targetAliasClassName
        } else {
            ALIAS_NORMAL
        }

        // Step 1: Enable target alias FIRST so there is never zero active launcher components.
        val targetComponent = ComponentName(packageName, validTarget)
        if (!isAliasCurrentlyEnabled(packageManager, packageName, validTarget)) {
            packageManager.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        // Step 2: Disable all other aliases safely with DONT_KILL_APP.
        for (aliasClass in ALL_ALIAS_CLASSES) {
            if (aliasClass == validTarget) continue
            if (isAliasCurrentlyEnabled(packageManager, packageName, aliasClass)) {
                packageManager.setComponentEnabledSetting(
                    ComponentName(packageName, aliasClass),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }

    private fun isAliasCurrentlyEnabled(
        packageManager: PackageManager,
        packageName: String,
        aliasClassName: String
    ): Boolean {
        val state = packageManager.getComponentEnabledSetting(ComponentName(packageName, aliasClassName))
        return when (state) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> aliasClassName == ALIAS_NORMAL
            else -> false
        }
    }

    companion object {
        private const val PREFIX = "com.snowsplit.app"

        const val ALIAS_NORMAL = "$PREFIX.LauncherNormal"
        const val ALIAS_WINTER = "$PREFIX.LauncherWinter"
        const val ALIAS_HALLOWEEN = "$PREFIX.LauncherHalloween"
        const val ALIAS_CHRISTMAS = "$PREFIX.LauncherChristmas"
        const val ALIAS_SUMMER = "$PREFIX.LauncherSummer"
        const val ALIAS_AUTUMN = "$PREFIX.LauncherAutumn"
        const val ALIAS_SPRING = "$PREFIX.LauncherSpring"
        const val ALIAS_EASTER = "$PREFIX.LauncherEaster"
        const val ALIAS_NEW_YEAR = "$PREFIX.LauncherNewYear"
        const val ALIAS_GAMING = "$PREFIX.LauncherGaming"
        const val ALIAS_EIGHT_BIT = "$PREFIX.LauncherEightBit"
        const val ALIAS_INTERACTIVE_BUBBLES = "$PREFIX.LauncherInteractiveBubbles"
        const val ALIAS_MINI_CITY = "$PREFIX.LauncherMiniCity"
        const val ALIAS_WEATHER_TIME = "$PREFIX.LauncherWeatherTime"

        val ALL_ALIAS_CLASSES: List<String> = listOf(
            ALIAS_NORMAL,
            ALIAS_WINTER,
            ALIAS_HALLOWEEN,
            ALIAS_CHRISTMAS,
            ALIAS_SUMMER,
            ALIAS_AUTUMN,
            ALIAS_SPRING,
            ALIAS_EASTER,
            ALIAS_NEW_YEAR,
            ALIAS_GAMING,
            ALIAS_EIGHT_BIT,
            ALIAS_INTERACTIVE_BUBBLES,
            ALIAS_MINI_CITY,
            ALIAS_WEATHER_TIME
        )

        fun resolveAliasClassName(
            appearance: ThemeAppearanceConfig,
            syncWithTheme: Boolean
        ): String {
            if (!syncWithTheme || appearance.mode == ThemeMode.CUSTOM) {
                return ALIAS_NORMAL
            }
            return when (appearance.effectivePresetId()) {
                "standard_m3" -> ALIAS_NORMAL
                "winter" -> ALIAS_WINTER
                "halloween" -> ALIAS_HALLOWEEN
                "christmas" -> ALIAS_CHRISTMAS
                "summer" -> ALIAS_SUMMER
                "autumn" -> ALIAS_AUTUMN
                "spring" -> ALIAS_SPRING
                "easter" -> ALIAS_EASTER
                "new_year" -> ALIAS_NEW_YEAR
                "gaming" -> ALIAS_GAMING
                "8bit_cute" -> ALIAS_EIGHT_BIT
                "interactive_bubbles" -> ALIAS_INTERACTIVE_BUBBLES
                "mini_city" -> ALIAS_MINI_CITY
                "weather_time" -> ALIAS_WEATHER_TIME
                else -> ALIAS_NORMAL
            }
        }
    }
}
