package com.snowsplit.app.platform.sync

import com.snowsplit.app.core.designsystem.ThemeRegistry
import com.snowsplit.app.core.designsystem.parseHexColorOrNull
import com.snowsplit.app.core.designsystem.toHexString
import com.snowsplit.app.domain.model.EightBitVariant
import com.snowsplit.app.domain.model.ThemeAppearanceConfig
import com.snowsplit.app.domain.model.ThemeMode
import com.snowsplit.app.domain.model.WeatherThemeStyle
import java.util.Locale

data class SnowAppearanceSyncPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val sourcePackage: String,
    val syncId: String,
    val updatedAtEpochMillis: Long,
    val themeMode: String,
    val presetId: String,
    val customAccentHex: String,
    val gamingAccentHex: String,
    val eightBitVariant: String,
    val glassOpacity: Float,
    val reduceLiveBlur: Boolean,
    val hasCustomBackground: Boolean,
    val customBackgroundHash: String? = null,
    val customBackgroundContentUri: String? = null,
    val weatherStyle: String = WeatherThemeStyle.CUTE_2D.name,
    val autoSeasonalThemeEnabled: Boolean = false
) {
    fun toJson(): String = buildString {
        append('{')
        appendIntField("schemaVersion", schemaVersion).append(',')
        appendStringField("sourcePackage", sourcePackage).append(',')
        appendStringField("syncId", syncId).append(',')
        appendLongField("updatedAtEpochMillis", updatedAtEpochMillis).append(',')
        appendStringField("themeMode", themeMode).append(',')
        appendStringField("presetId", presetId).append(',')
        appendStringField("customAccentHex", customAccentHex).append(',')
        appendStringField("gamingAccentHex", gamingAccentHex).append(',')
        appendStringField("eightBitVariant", eightBitVariant).append(',')
        appendDoubleField("glassOpacity", glassOpacity.toDouble()).append(',')
        appendBooleanField("reduceLiveBlur", reduceLiveBlur).append(',')
        appendBooleanField("hasCustomBackground", hasCustomBackground).append(',')
        appendNullableStringField("customBackgroundHash", customBackgroundHash).append(',')
        appendNullableStringField("customBackgroundContentUri", customBackgroundContentUri).append(',')
        appendStringField("weatherStyle", weatherStyle).append(',')
        appendBooleanField("autoSeasonalThemeEnabled", autoSeasonalThemeEnabled)
        append('}')
    }

    fun toThemeAppearanceConfig(resolvedBackgroundUri: String?): ThemeAppearanceConfig {
        val normalizedMode = ThemeMode.PRESET
        val normalizedPresetId = ThemeRegistry.findPreset(presetId).id
        val normalizedCustomHex = parseHexColorOrNull(customAccentHex)?.toHexString() ?: "#D0BCFF"
        val normalizedGamingHex = parseHexColorOrNull(gamingAccentHex)?.toHexString() ?: "#00E5FF"
        val normalizedEightBit = when (eightBitVariant.uppercase(Locale.US)) {
            "PASTEL_BLUE" -> EightBitVariant.PASTEL_BLUE
            else -> EightBitVariant.PASTEL_PINK
        }
        val normalizedWeatherStyle = when (weatherStyle.uppercase(Locale.US)) {
            "REALISTIC" -> WeatherThemeStyle.REALISTIC
            else -> WeatherThemeStyle.CUTE_2D
        }
        return ThemeAppearanceConfig(
            presetId = normalizedPresetId,
            mode = normalizedMode,
            customAccentHex = normalizedCustomHex,
            gamingAccentHex = normalizedGamingHex,
            eightBitVariant = normalizedEightBit,
            weatherStyle = normalizedWeatherStyle,
            autoSeasonalThemeEnabled = autoSeasonalThemeEnabled,
            glassOpacity = glassOpacity.coerceIn(0.10f, 0.55f),
            backgroundImageUri = resolvedBackgroundUri
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        fun fromAppearance(
            sourcePackage: String,
            syncId: String,
            updatedAtEpochMillis: Long,
            appearance: ThemeAppearanceConfig,
            reduceLiveBlur: Boolean,
            localBackgroundHash: String?
        ): SnowAppearanceSyncPayload {
            val hasCustomBg = !appearance.backgroundImageUri.isNullOrBlank() &&
                !localBackgroundHash.isNullOrBlank()
            return SnowAppearanceSyncPayload(
                schemaVersion = CURRENT_SCHEMA_VERSION,
                sourcePackage = sourcePackage,
                syncId = syncId,
                updatedAtEpochMillis = updatedAtEpochMillis,
                themeMode = ThemeMode.PRESET.name,
                presetId = appearance.effectivePresetId(),
                customAccentHex = appearance.customAccentHex,
                gamingAccentHex = appearance.gamingAccentHex,
                eightBitVariant = appearance.eightBitVariant.name,
                glassOpacity = appearance.glassOpacity.coerceIn(0.10f, 0.55f),
                reduceLiveBlur = reduceLiveBlur,
                hasCustomBackground = hasCustomBg,
                customBackgroundHash = if (hasCustomBg) localBackgroundHash else null,
                customBackgroundContentUri = if (hasCustomBg) {
                    PartnerAppConfig.OWN_BACKGROUND_CONTENT_URI
                } else {
                    null
                },
                weatherStyle = appearance.weatherStyle.name,
                autoSeasonalThemeEnabled = appearance.autoSeasonalThemeEnabled
            )
        }

        fun fromJson(rawJson: String): SnowAppearanceSyncPayload? {
            val map = runCatching { SimpleJsonObjectParser.parse(rawJson) }.getOrNull() ?: return null
            val schemaVersion = (map["schemaVersion"] as? Number)?.toInt() ?: return null
            if (schemaVersion != CURRENT_SCHEMA_VERSION) return null
            val sourcePackage = (map["sourcePackage"] as? String)?.takeIf { it.isNotBlank() } ?: return null
            val syncId = (map["syncId"] as? String)?.takeIf { it.isNotBlank() } ?: return null
            val updatedAt = (map["updatedAtEpochMillis"] as? Number)?.toLong() ?: return null
            val themeMode = (map["themeMode"] as? String)?.takeIf { it.isNotBlank() } ?: "PRESET"
            val presetId = (map["presetId"] as? String)?.takeIf { it.isNotBlank() } ?: "standard_m3"
            val customAccentHex = (map["customAccentHex"] as? String)?.takeIf { it.isNotBlank() } ?: "#D0BCFF"
            val gamingAccentHex = (map["gamingAccentHex"] as? String)?.takeIf { it.isNotBlank() } ?: "#00E5FF"
            val eightBitVariant = (map["eightBitVariant"] as? String)?.takeIf { it.isNotBlank() } ?: "PASTEL_PINK"
            val glassOpacity = ((map["glassOpacity"] as? Number)?.toFloat() ?: 0.24f).coerceIn(0.10f, 0.55f)
            val reduceLiveBlur = (map["reduceLiveBlur"] as? Boolean) ?: false
            val hasCustomBackground = (map["hasCustomBackground"] as? Boolean) ?: false
            val customBackgroundHash = (map["customBackgroundHash"] as? String)?.takeIf { it.isNotBlank() }
            val customBackgroundContentUri = (map["customBackgroundContentUri"] as? String)?.takeIf { it.isNotBlank() }
            val weatherStyle = (map["weatherStyle"] as? String)?.takeIf { it.isNotBlank() }
                ?: WeatherThemeStyle.CUTE_2D.name
            val autoSeasonal = (map["autoSeasonalThemeEnabled"] as? Boolean) ?: false

            return SnowAppearanceSyncPayload(
                schemaVersion = schemaVersion,
                sourcePackage = sourcePackage,
                syncId = syncId,
                updatedAtEpochMillis = updatedAt,
                themeMode = themeMode,
                presetId = presetId,
                customAccentHex = customAccentHex,
                gamingAccentHex = gamingAccentHex,
                eightBitVariant = eightBitVariant,
                glassOpacity = glassOpacity,
                reduceLiveBlur = reduceLiveBlur,
                hasCustomBackground = hasCustomBackground,
                customBackgroundHash = if (hasCustomBackground) customBackgroundHash else null,
                customBackgroundContentUri = if (hasCustomBackground) customBackgroundContentUri else null,
                weatherStyle = weatherStyle,
                autoSeasonalThemeEnabled = autoSeasonal
            )
        }
    }
}

object SyncLoopGuard {
    fun shouldApply(
        payload: SnowAppearanceSyncPayload,
        ownPackage: String,
        lastAppliedSyncId: String,
        lastSyncedRevision: Long
    ): Boolean {
        if (payload.sourcePackage.isBlank() || payload.sourcePackage == ownPackage) {
            return false
        }
        if (payload.syncId.isBlank() || payload.syncId == lastAppliedSyncId) {
            return false
        }
        if (payload.updatedAtEpochMillis <= lastSyncedRevision) {
            return false
        }
        return true
    }

    fun shouldCopyBackground(
        hasCustomBackground: Boolean,
        incomingHash: String?,
        localBackgroundHash: String?,
        hasExistingLocalFile: Boolean
    ): Boolean {
        if (!hasCustomBackground) return false
        if (incomingHash.isNullOrBlank()) return true
        return !hasExistingLocalFile || !incomingHash.equals(localBackgroundHash, ignoreCase = true)
    }
}

private fun StringBuilder.appendJsonString(value: String): StringBuilder {
    append('"')
    for (ch in value) {
        when (ch) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (ch.code < 0x20) {
                append("\\u%04x".format(ch.code))
            } else {
                append(ch)
            }
        }
    }
    append('"')
    return this
}

private fun StringBuilder.appendStringField(key: String, value: String): StringBuilder =
    appendJsonString(key).append(':').appendJsonString(value)

private fun StringBuilder.appendNullableStringField(key: String, value: String?): StringBuilder =
    appendJsonString(key).append(':').apply {
        if (value == null) append("null") else appendJsonString(value)
    }

private fun StringBuilder.appendIntField(key: String, value: Int): StringBuilder =
    appendJsonString(key).append(':').append(value)

private fun StringBuilder.appendLongField(key: String, value: Long): StringBuilder =
    appendJsonString(key).append(':').append(value)

private fun StringBuilder.appendDoubleField(key: String, value: Double): StringBuilder =
    appendJsonString(key).append(':').append(String.format(Locale.US, "%.4f", value))

private fun StringBuilder.appendBooleanField(key: String, value: Boolean): StringBuilder =
    appendJsonString(key).append(':').append(value)

private class SimpleJsonObjectParser(private val input: String) {
    private var index = 0

    fun parseObject(): Map<String, Any?> {
        skipWhitespace()
        requireChar('{')
        skipWhitespace()
        val result = linkedMapOf<String, Any?>()
        if (peek() == '}') {
            index++
            return result
        }
        while (index < input.length) {
            skipWhitespace()
            val key = readString()
            skipWhitespace()
            requireChar(':')
            skipWhitespace()
            val value = readValue()
            result[key] = value
            skipWhitespace()
            when (peek()) {
                ',' -> index++
                '}' -> {
                    index++
                    return result
                }
                else -> throw IllegalArgumentException("Expected ',' or '}' at $index")
            }
        }
        throw IllegalArgumentException("Unterminated JSON object")
    }

    private fun readValue(): Any? {
        skipWhitespace()
        return when (peek()) {
            '"' -> readString()
            '{' -> parseObject()
            '[' -> readArray()
            't', 'f' -> readBoolean()
            'n' -> readNull()
            else -> readNumber()
        }
    }

    private fun readArray(): List<Any?> {
        requireChar('[')
        skipWhitespace()
        val list = mutableListOf<Any?>()
        if (peek() == ']') {
            index++
            return list
        }
        while (index < input.length) {
            skipWhitespace()
            list.add(readValue())
            skipWhitespace()
            when (peek()) {
                ',' -> index++
                ']' -> {
                    index++
                    return list
                }
                else -> throw IllegalArgumentException("Expected ',' or ']' at $index")
            }
        }
        throw IllegalArgumentException("Unterminated JSON array")
    }

    private fun readString(): String {
        requireChar('"')
        val sb = StringBuilder()
        while (index < input.length) {
            val ch = input[index++]
            when (ch) {
                '"' -> return sb.toString()
                '\\' -> {
                    if (index >= input.length) throw IllegalArgumentException("Dangling escape")
                    val esc = input[index++]
                    when (esc) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = input.substring(index, index + 4)
                            index += 4
                            sb.append(hex.toInt(16).toChar())
                        }
                        else -> throw IllegalArgumentException("Invalid escape \\$esc")
                    }
                }
                else -> sb.append(ch)
            }
        }
        throw IllegalArgumentException("Unterminated string")
    }

    private fun readBoolean(): Boolean {
        return if (input.startsWith("true", index)) {
            index += 4
            true
        } else if (input.startsWith("false", index)) {
            index += 5
            false
        } else {
            throw IllegalArgumentException("Invalid boolean at $index")
        }
    }

    private fun readNull(): Any? {
        if (!input.startsWith("null", index)) {
            throw IllegalArgumentException("Invalid null at $index")
        }
        index += 4
        return null
    }

    private fun readNumber(): Number {
        val start = index
        while (index < input.length) {
            val c = input[index]
            if (c in '0'..'9' || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                index++
            } else {
                break
            }
        }
        val token = input.substring(start, index)
        if (token.isEmpty()) throw IllegalArgumentException("Expected number at $start")
        return if (token.contains('.') || token.contains('e') || token.contains('E')) {
            token.toDouble()
        } else {
            token.toLong()
        }
    }

    private fun skipWhitespace() {
        while (index < input.length && input[index].isWhitespace()) {
            index++
        }
    }

    private fun requireChar(expected: Char) {
        if (index >= input.length || input[index] != expected) {
            throw IllegalArgumentException("Expected '$expected' at $index")
        }
        index++
    }

    private fun peek(): Char = input.getOrNull(index)
        ?: throw IllegalArgumentException("Unexpected end of JSON input")

    companion object {
        fun parse(rawJson: String): Map<String, Any?> =
            SimpleJsonObjectParser(rawJson).parseObject()
    }
}
