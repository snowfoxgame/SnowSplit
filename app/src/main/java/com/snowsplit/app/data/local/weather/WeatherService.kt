package com.snowsplit.app.data.local.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.snowsplit.app.core.common.DispatcherProvider
import com.snowsplit.app.domain.model.WeatherConditionMapper
import com.snowsplit.app.domain.model.WeatherSnapshot
import com.snowsplit.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class CoarseCoordinates(
    val latitude: Double,
    val longitude: Double
)

object OpenMeteoWeatherParser {
    private val weatherCodeRegex = Regex(""""current"\s*:\s*\{[^}]*"weather_code"\s*:\s*(-?\d+)""")
    private val isDayRegex = Regex(""""current"\s*:\s*\{[^}]*"is_day"\s*:\s*(\d+)""")
    private val sunriseRegex = Regex(""""sunrise"\s*:\s*\[\s*"([^"]+)"""")
    private val sunsetRegex = Regex(""""sunset"\s*:\s*\[\s*"([^"]+)"""")

    fun parseResponse(
        json: String,
        latitude: Double,
        longitude: Double,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): WeatherSnapshot? {
        val code = weatherCodeRegex.find(json)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return null
        val isDayInt = isDayRegex.find(json)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val fallback = WeatherConditionMapper.resolveTimeFallbackSnapshot(nowMillis, zoneId)

        val sunriseMillis = sunriseRegex.find(json)?.groupValues?.getOrNull(1)
            ?.let { parseIsoLocalToMillis(it, zoneId) }
            ?: fallback.sunriseEpochMillis
        val sunsetMillis = sunsetRegex.find(json)?.groupValues?.getOrNull(1)
            ?.let { parseIsoLocalToMillis(it, zoneId) }
            ?: fallback.sunsetEpochMillis

        val isDay = isDayInt?.let { it == 1 } ?: (nowMillis in sunriseMillis until sunsetMillis)

        return WeatherSnapshot(
            wmoCode = code,
            condition = WeatherConditionMapper.fromWmoCode(code),
            isDay = isDay,
            sunriseEpochMillis = sunriseMillis,
            sunsetEpochMillis = sunsetMillis,
            fetchedAtEpochMillis = nowMillis,
            isLocationBased = true,
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun parseIsoLocalToMillis(raw: String, zoneId: ZoneId): Long? =
        runCatching {
            LocalDateTime.parse(raw).atZone(zoneId).toInstant().toEpochMilli()
        }.getOrNull()
}

@Singleton
class CoarseLocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun hasCoarseLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun readLastKnownCoarseCoordinates(): CoarseCoordinates? {
        if (!hasCoarseLocationPermission()) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val candidates = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER
        )
        var best: Location? = null
        for (provider in candidates) {
            val loc = runCatching { manager.getLastKnownLocation(provider) }.getOrNull() ?: continue
            if (best == null || loc.time > best.time) {
                best = loc
            }
        }
        return best?.let { CoarseCoordinates(latitude = it.latitude, longitude = it.longitude) }
    }
}

@Singleton
class WeatherRepository @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val locationProvider: CoarseLocationProvider,
    private val dispatcherProvider: DispatcherProvider
) {
    private val refreshMutex = Mutex()

    fun hasLocationPermission(): Boolean = locationProvider.hasCoarseLocationPermission()

    suspend fun getEffectiveWeather(forceRefresh: Boolean = false): WeatherSnapshot =
        withContext(dispatcherProvider.io) {
            val now = System.currentTimeMillis()
            val settings = settingsRepository.observeSettings().first()
            val cached = settings.weatherSnapshot

            if (!locationProvider.hasCoarseLocationPermission()) {
                return@withContext WeatherConditionMapper.resolveTimeFallbackSnapshot(now)
            }

            if (!forceRefresh && cached != null && cached.isLocationBased &&
                (now - cached.fetchedAtEpochMillis) < CACHE_TTL_MS
            ) {
                return@withContext cached
            }

            refreshMutex.withLock {
                val coords = locationProvider.readLastKnownCoarseCoordinates()
                    ?: cached?.takeIf { it.latitude != null && it.longitude != null }
                        ?.let { CoarseCoordinates(it.latitude!!, it.longitude!!) }
                    ?: return@withLock cached ?: WeatherConditionMapper.resolveTimeFallbackSnapshot(now)

                val fetched = fetchOpenMeteo(coords.latitude, coords.longitude, now)
                if (fetched != null) {
                    settingsRepository.saveWeatherSnapshot(fetched)
                    return@withLock fetched
                }
                cached ?: WeatherConditionMapper.resolveTimeFallbackSnapshot(now)
            }
        }

    private fun fetchOpenMeteo(
        latitude: Double,
        longitude: Double,
        nowMillis: Long
    ): WeatherSnapshot? = runCatching {
        val urlString = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=weather_code,is_day&daily=sunrise,sunset&timezone=auto&forecast_days=1",
            latitude,
            longitude
        )
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            OpenMeteoWeatherParser.parseResponse(
                json = body,
                latitude = latitude,
                longitude = longitude,
                nowMillis = nowMillis
            )
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    companion object {
        private const val CACHE_TTL_MS = 30L * 60L * 1000L
    }
}
