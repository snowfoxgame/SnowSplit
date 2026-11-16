package com.snowsplit.app.core.designsystem.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.snowsplit.app.R
import com.snowsplit.app.core.designsystem.SnowDosThemeTokens
import com.snowsplit.app.core.designsystem.parseHexColorOrNull
import com.snowsplit.app.core.designsystem.toHexString
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun ColorWheelPicker(
    selectedHex: String,
    onColorHexChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = SnowDosThemeTokens.current
    val initialColor = remember(selectedHex) {
        parseHexColorOrNull(selectedHex) ?: Color(0xFF6750A4)
    }

    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var value by remember { mutableFloatStateOf(1f) }
    var hexInput by remember { mutableStateOf(initialColor.toHexString()) }
    var hexError by remember { mutableStateOf(false) }

    LaunchedEffect(selectedHex) {
        val parsed = parseHexColorOrNull(selectedHex) ?: return@LaunchedEffect
        val currentHex = Color.hsv(hue, saturation, value).toHexString()
        if (!parsed.toHexString().equals(currentHex, ignoreCase = true)) {
            val hsv = FloatArray(3)
            AndroidColor.colorToHSV(parsed.toArgb(), hsv)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
            hexInput = parsed.toHexString()
            hexError = false
        }
    }

    val activeColor = Color.hsv(hue, saturation, value)

    fun commitHsv(newHue: Float, newSat: Float, newVal: Float) {
        hue = newHue.coerceIn(0f, 360f)
        saturation = newSat.coerceIn(0f, 1f)
        value = newVal.coerceIn(0.15f, 1f)
        val formatted = Color.hsv(hue, saturation, value).toHexString()
        hexInput = formatted
        hexError = false
        onColorHexChanged(formatted)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.md)
    ) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            val hueColors = remember {
                listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red
                )
            }

            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val (h, s) = offsetToHueSaturation(
                                offset = offset,
                                width = size.width.toFloat(),
                                height = size.height.toFloat()
                            )
                            commitHsv(h, s, value)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val (h, s) = offsetToHueSaturation(
                                offset = change.position,
                                width = size.width.toFloat(),
                                height = size.height.toFloat()
                            )
                            commitHsv(h, s, value)
                        }
                    }
                    .drawWithCache {
                        val radius = size.minDimension / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val sweepBrush = Brush.sweepGradient(colors = hueColors, center = center)
                        val radialWhiteBrush = Brush.radialGradient(
                            colors = listOf(Color.White, Color.Transparent),
                            center = center,
                            radius = radius
                        )
                        val thumbShadowRadius = 12.dp.toPx()
                        val thumbRingRadius = 10.dp.toPx()
                        val thumbCoreRadius = 7.dp.toPx()

                        onDrawBehind {
                            drawCircle(brush = sweepBrush, radius = radius, center = center)
                            drawCircle(brush = radialWhiteBrush, radius = radius, center = center)
                            if (value < 1f) {
                                drawCircle(
                                    color = Color.Black.copy(alpha = 1f - value),
                                    radius = radius,
                                    center = center
                                )
                            }

                            val angleRad = (hue * PI / 180.0)
                            val thumbRadius = saturation * radius
                            val thumbPos = Offset(
                                x = center.x + (thumbRadius * cos(angleRad)).toFloat(),
                                y = center.y + (thumbRadius * sin(angleRad)).toFloat()
                            )
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.45f),
                                radius = thumbShadowRadius,
                                center = thumbPos
                            )
                            drawCircle(
                                color = Color.White,
                                radius = thumbRingRadius,
                                center = thumbPos
                            )
                            drawCircle(
                                color = activeColor,
                                radius = thumbCoreRadius,
                                center = thumbPos
                            )
                        }
                    }
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.color_picker_brightness),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(value * 100).toInt()} %",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = value,
                onValueChange = { newVal -> commitHsv(hue, saturation, newVal) },
                valueRange = 0.15f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = activeColor,
                    activeTrackColor = activeColor
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(tokens.radius.chip))
                    .background(activeColor)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(tokens.radius.chip)
                    )
            )

            OutlinedTextField(
                value = hexInput,
                onValueChange = { raw ->
                    val normalized = if (raw.isNotEmpty() && !raw.startsWith("#")) "#$raw" else raw
                    if (normalized.length <= 7) {
                        hexInput = normalized.uppercase()
                        val parsed = parseHexColorOrNull(normalized)
                        if (parsed != null) {
                            val hsv = FloatArray(3)
                            AndroidColor.colorToHSV(parsed.toArgb(), hsv)
                            hue = hsv[0]
                            saturation = hsv[1]
                            value = hsv[2].coerceAtLeast(0.15f)
                            hexError = false
                            onColorHexChanged(parsed.toHexString())
                        } else {
                            hexError = normalized.length >= 2
                        }
                    }
                },
                label = { Text(stringResource(R.string.color_picker_hex_label)) },
                placeholder = { Text(stringResource(R.string.color_picker_hex_placeholder)) },
                isError = hexError,
                supportingText = if (hexError) {
                    { Text(stringResource(R.string.color_picker_hex_hint)) }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(tokens.radius.chip),
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            )
        }
    }
}

private fun offsetToHueSaturation(
    offset: Offset,
    width: Float,
    height: Float
): Pair<Float, Float> {
    val cx = width / 2f
    val cy = height / 2f
    val dx = offset.x - cx
    val dy = offset.y - cy
    val radius = minOf(cx, cy).coerceAtLeast(1f)
    val distance = hypot(dx, dy).coerceAtMost(radius)
    val saturation = (distance / radius).coerceIn(0f, 1f)
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (angle < 0f) angle += 360f
    return angle to saturation
}
