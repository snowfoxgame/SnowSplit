package com.snowsplit.app.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun parseHexColorOrNull(raw: String): Color? {
    val cleaned = raw.trim().removePrefix("#")
    if (cleaned.length != 6) return null
    val rgb = cleaned.toLongOrNull(16) ?: return null
    return Color((0xFF000000L or rgb).toInt())
}

fun Color.toHexString(): String {
    val rgb = toArgb() and 0x00FFFFFF
    return "#%06X".format(rgb)
}
