package com.snowsplit.app.domain.model

import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.sin

enum class WeatherThemeStyle {
    CUTE_2D,
    REALISTIC
}

enum class WeatherConditionKind {
    CLEAR,
    PARTLY_CLOUDY,
    OVERCAST,
    FOG,
    DRIZZLE,
    RAIN,
    HEAVY_RAIN,
    FREEZING_RAIN,
    SNOW_LIGHT,
    SNOW_HEAVY,
    THUNDERSTORM_HAIL
}

@Immutable
data class WeatherSnapshot(
    val wmoCode: Int,
    val condition: WeatherConditionKind,
    val isDay: Boolean,
    val sunriseEpochMillis: Long,
    val sunsetEpochMillis: Long,
    val fetchedAtEpochMillis: Long,
    val isLocationBased: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Immutable
data class CelestialArcState(
    val isDay: Boolean,
    val arcProgress: Float,
    val elevationNormalized: Float
)

object WeatherConditionMapper {
    fun fromWmoCode(code: Int): WeatherConditionKind = when (code) {
        0 -> WeatherConditionKind.CLEAR
        1, 2 -> WeatherConditionKind.PARTLY_CLOUDY
        3 -> WeatherConditionKind.OVERCAST
        45, 48 -> WeatherConditionKind.FOG
        51, 53, 55, 56, 57 -> WeatherConditionKind.DRIZZLE
        61, 63, 80, 81 -> WeatherConditionKind.RAIN
        65, 82 -> WeatherConditionKind.HEAVY_RAIN
        66, 67 -> WeatherConditionKind.FREEZING_RAIN
        71, 77, 85 -> WeatherConditionKind.SNOW_LIGHT
        73, 75, 86 -> WeatherConditionKind.SNOW_HEAVY
        95, 96, 99 -> WeatherConditionKind.THUNDERSTORM_HAIL
        else -> WeatherConditionKind.PARTLY_CLOUDY
    }

    fun resolveTimeFallbackSnapshot(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): WeatherSnapshot {
        val localDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val sunriseMillis = localDate.atTime(LocalTime.of(6, 30)).atZone(zoneId).toInstant().toEpochMilli()
        val sunsetMillis = localDate.atTime(LocalTime.of(19, 30)).atZone(zoneId).toInstant().toEpochMilli()
        val isDay = nowMillis in sunriseMillis until sunsetMillis
        return WeatherSnapshot(
            wmoCode = 0,
            condition = WeatherConditionKind.CLEAR,
            isDay = isDay,
            sunriseEpochMillis = sunriseMillis,
            sunsetEpochMillis = sunsetMillis,
            fetchedAtEpochMillis = nowMillis,
            isLocationBased = false
        )
    }

    fun computeCelestialState(
        nowMillis: Long,
        sunriseMillis: Long,
        sunsetMillis: Long
    ): CelestialArcState {
        val validWindow = sunsetMillis > sunriseMillis
        if (!validWindow) {
            return CelestialArcState(isDay = true, arcProgress = 0.5f, elevationNormalized = 1f)
        }

        return if (nowMillis in sunriseMillis..sunsetMillis) {
            val progress = ((nowMillis - sunriseMillis).toFloat() / (sunsetMillis - sunriseMillis).toFloat())
                .coerceIn(0f, 1f)
            val elevation = sin(progress * PI).toFloat().coerceIn(0f, 1f)
            CelestialArcState(isDay = true, arcProgress = progress, elevationNormalized = elevation)
        } else {
            val daySpanMillis = 24L * 60L * 60L * 1000L
            val nightStart = if (nowMillis > sunsetMillis) sunsetMillis else sunsetMillis - daySpanMillis
            val nightEnd = if (nowMillis > sunsetMillis) sunriseMillis + daySpanMillis else sunriseMillis
            val duration = (nightEnd - nightStart).coerceAtLeast(60_000L)
            val progress = ((nowMillis - nightStart).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            val elevation = sin(progress * PI).toFloat().coerceIn(0f, 1f)
            CelestialArcState(isDay = false, arcProgress = progress, elevationNormalized = elevation)
        }
    }
}
