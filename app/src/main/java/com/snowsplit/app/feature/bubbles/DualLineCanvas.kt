package com.snowsplit.app.feature.bubbles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowsplit.app.core.common.CurrencyFormatter
import com.snowsplit.app.core.designsystem.SnowDosThemeTokens
import com.snowsplit.app.domain.model.CurrencyOption
import com.snowsplit.app.domain.model.PersonBalanceSummary
import kotlin.math.hypot

@Immutable
private data class MeasuredConnectionLabels(
    val personId: Long,
    val greenLayout: TextLayoutResult?,
    val redLayout: TextLayoutResult?
)

/**
 * Draws all "Me" <-> Contact connection lines, directional arrows, and formatted amount pills
 * in a single Canvas pass with zero recompositions per animation frame.
 */
@Composable
fun DualLineCanvas(
    summaries: List<PersonBalanceSummary>,
    currencyOption: CurrencyOption,
    simulation: BubblePhysicsSimulation,
    modifier: Modifier = Modifier
) {
    val tokens = SnowDosThemeTokens.current
    val density = LocalDensity.current
    val textMeasurer: TextMeasurer = rememberTextMeasurer()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val greenColor = tokens.semantic.positive
    val redColor = tokens.semantic.negative
    val neutralLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    val pillBaseSurface = MaterialTheme.colorScheme.surfaceContainerHigh
    val usePixelRetro = tokens.surface.usePixelRetroStyle

    val greenTextColor = remember(greenColor, isDark) {
        if (isDark) lerp(greenColor, Color.White, 0.72f) else lerp(greenColor, Color.Black, 0.35f)
    }
    val redTextColor = remember(redColor, isDark) {
        if (isDark) lerp(redColor, Color.White, 0.72f) else lerp(redColor, Color.Black, 0.35f)
    }

    val measuredConnections = remember(
        summaries,
        currencyOption,
        greenTextColor,
        redTextColor,
        density.density,
        density.fontScale
    ) {
        val greenStyle = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = greenTextColor
        )
        val redStyle = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = redTextColor
        )
        summaries.map { summary ->
            val greenLayout = if (summary.displayGreenCents > 0L) {
                val formatted = CurrencyFormatter.formatCents(summary.displayGreenCents, currencyOption)
                textMeasurer.measure(text = formatted, style = greenStyle, maxLines = 1)
            } else {
                null
            }
            val redLayout = if (summary.displayRedCents > 0L) {
                val formatted = CurrencyFormatter.formatCents(summary.displayRedCents, currencyOption)
                textMeasurer.measure(text = formatted, style = redStyle, maxLines = 1)
            } else {
                null
            }
            MeasuredConnectionLabels(
                personId = summary.person.id,
                greenLayout = greenLayout,
                redLayout = redLayout
            )
        }
    }

    val activeStrokePx = with(density) { 2.8.dp.toPx() }
    val dualStrokePx = with(density) { 3.dp.toPx() }
    val settledStrokePx = with(density) { 1.4.dp.toPx() }
    val edgeGapPx = with(density) { 2.dp.toPx() }
    val arrowSizePx = with(density) { 7.dp.toPx() }
    val pillPadHPx = with(density) { 7.dp.toPx() }
    val pillPadVPx = with(density) { 3.dp.toPx() }
    val pillPerpSpreadPx = with(density) { 14.dp.toPx() }
    val pillBorderWidthPx = with(density) { 1.dp.toPx() }
    val pillCornerRadiusPx = with(density) { if (usePixelRetro) 4.dp.toPx() else 999.dp.toPx() }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Subscribe purely to Draw-phase snapshot ticks (0 recompositions & 0 layouts per frame).
        @Suppress("UNUSED_VARIABLE")
        val tick = simulation.frameVersion

        val meX = simulation.centerX
        val meY = simulation.centerY
        val meRadius = simulation.meRadiusPx
        val contactRadius = simulation.contactRadiusPx

        for (i in 0 until measuredConnections.size) {
            val item = measuredConnections[i]
            val cx = simulation.positionX(item.personId)
            val cy = simulation.positionY(item.personId)

            val dx = cx - meX
            val dy = cy - meY
            val dist = hypot(dx, dy)
            val minDrawDist = meRadius + contactRadius + edgeGapPx * 2f + 4f
            if (dist <= minDrawDist) continue

            val ux = dx / dist
            val uy = dy / dist
            // Perpendicular unit normal
            val nx = -uy
            val ny = ux

            val startOffset = meRadius + edgeGapPx
            val endOffset = dist - contactRadius - edgeGapPx
            val sx = meX + ux * startOffset
            val sy = meY + uy * startOffset
            val ex = meX + ux * endOffset
            val ey = meY + uy * endOffset

            val hasGreen = item.greenLayout != null
            val hasRed = item.redLayout != null

            when {
                hasGreen && hasRed -> {
                    // Two parallel lines directly side-by-side with zero gap.
                    val halfStroke = dualStrokePx * 0.5f
                    val gOffX = -nx * halfStroke
                    val gOffY = -ny * halfStroke
                    val rOffX = nx * halfStroke
                    val rOffY = ny * halfStroke

                    drawLine(
                        color = greenColor.copy(alpha = 0.92f),
                        start = Offset(sx + gOffX, sy + gOffY),
                        end = Offset(ex + gOffX, ey + gOffY),
                        strokeWidth = dualStrokePx,
                        cap = StrokeCap.Butt
                    )
                    drawLine(
                        color = redColor.copy(alpha = 0.92f),
                        start = Offset(sx + rOffX, sy + rOffY),
                        end = Offset(ex + rOffX, ey + rOffY),
                        strokeWidth = dualStrokePx,
                        cap = StrokeCap.Butt
                    )

                    // Green arrow points toward "Me" (-u); Red arrow points toward Contact (+u).
                    val greenTipX = sx + (ex - sx) * 0.16f + gOffX
                    val greenTipY = sy + (ey - sy) * 0.16f + gOffY
                    drawDirectionalChevron(
                        tip = Offset(greenTipX, greenTipY),
                        dirX = -ux,
                        dirY = -uy,
                        normX = nx,
                        normY = ny,
                        sizePx = arrowSizePx,
                        strokeWidthPx = dualStrokePx * 0.85f,
                        color = greenColor
                    )

                    val redTipX = sx + (ex - sx) * 0.84f + rOffX
                    val redTipY = sy + (ey - sy) * 0.84f + rOffY
                    drawDirectionalChevron(
                        tip = Offset(redTipX, redTipY),
                        dirX = ux,
                        dirY = uy,
                        normX = nx,
                        normY = ny,
                        sizePx = arrowSizePx,
                        strokeWidthPx = dualStrokePx * 0.85f,
                        color = redColor
                    )

                    item.greenLayout?.let { layout ->
                        val gCenter = Offset(
                            x = sx + (ex - sx) * 0.37f - nx * pillPerpSpreadPx,
                            y = sy + (ey - sy) * 0.37f - ny * pillPerpSpreadPx
                        )
                        drawAmountPill(
                            center = gCenter,
                            textLayout = layout,
                            accentColor = greenColor,
                            pillBaseSurface = pillBaseSurface,
                            isDarkTheme = isDark,
                            padHPx = pillPadHPx,
                            padVPx = pillPadVPx,
                            borderWidthPx = pillBorderWidthPx,
                            cornerRadiusPx = pillCornerRadiusPx
                        )
                    }
                    item.redLayout?.let { layout ->
                        val rCenter = Offset(
                            x = sx + (ex - sx) * 0.63f + nx * pillPerpSpreadPx,
                            y = sy + (ey - sy) * 0.63f + ny * pillPerpSpreadPx
                        )
                        drawAmountPill(
                            center = rCenter,
                            textLayout = layout,
                            accentColor = redColor,
                            pillBaseSurface = pillBaseSurface,
                            isDarkTheme = isDark,
                            padHPx = pillPadHPx,
                            padVPx = pillPadVPx,
                            borderWidthPx = pillBorderWidthPx,
                            cornerRadiusPx = pillCornerRadiusPx
                        )
                    }
                }

                hasGreen -> {
                    drawLine(
                        color = greenColor.copy(alpha = 0.90f),
                        start = Offset(sx, sy),
                        end = Offset(ex, ey),
                        strokeWidth = activeStrokePx,
                        cap = StrokeCap.Round
                    )
                    drawDirectionalChevron(
                        tip = Offset(sx + (ex - sx) * 0.16f, sy + (ey - sy) * 0.16f),
                        dirX = -ux,
                        dirY = -uy,
                        normX = nx,
                        normY = ny,
                        sizePx = arrowSizePx,
                        strokeWidthPx = activeStrokePx,
                        color = greenColor
                    )
                    item.greenLayout?.let { layout ->
                        drawAmountPill(
                            center = Offset((sx + ex) * 0.5f, (sy + ey) * 0.5f),
                            textLayout = layout,
                            accentColor = greenColor,
                            pillBaseSurface = pillBaseSurface,
                            isDarkTheme = isDark,
                            padHPx = pillPadHPx,
                            padVPx = pillPadVPx,
                            borderWidthPx = pillBorderWidthPx,
                            cornerRadiusPx = pillCornerRadiusPx
                        )
                    }
                }

                hasRed -> {
                    drawLine(
                        color = redColor.copy(alpha = 0.90f),
                        start = Offset(sx, sy),
                        end = Offset(ex, ey),
                        strokeWidth = activeStrokePx,
                        cap = StrokeCap.Round
                    )
                    drawDirectionalChevron(
                        tip = Offset(sx + (ex - sx) * 0.84f, sy + (ey - sy) * 0.84f),
                        dirX = ux,
                        dirY = uy,
                        normX = nx,
                        normY = ny,
                        sizePx = arrowSizePx,
                        strokeWidthPx = activeStrokePx,
                        color = redColor
                    )
                    item.redLayout?.let { layout ->
                        drawAmountPill(
                            center = Offset((sx + ex) * 0.5f, (sy + ey) * 0.5f),
                            textLayout = layout,
                            accentColor = redColor,
                            pillBaseSurface = pillBaseSurface,
                            isDarkTheme = isDark,
                            padHPx = pillPadHPx,
                            padVPx = pillPadVPx,
                            borderWidthPx = pillBorderWidthPx,
                            cornerRadiusPx = pillCornerRadiusPx
                        )
                    }
                }

                else -> {
                    // Balanced (0): subtle semi-transparent line without amount pills.
                    drawLine(
                        color = neutralLineColor,
                        start = Offset(sx, sy),
                        end = Offset(ex, ey),
                        strokeWidth = settledStrokePx,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawDirectionalChevron(
    tip: Offset,
    dirX: Float,
    dirY: Float,
    normX: Float,
    normY: Float,
    sizePx: Float,
    strokeWidthPx: Float,
    color: Color
) {
    val backX = tip.x - dirX * sizePx
    val backY = tip.y - dirY * sizePx
    val wingSpread = sizePx * 0.65f
    val wing1 = Offset(backX + normX * wingSpread, backY + normY * wingSpread)
    val wing2 = Offset(backX - normX * wingSpread, backY - normY * wingSpread)

    drawLine(
        color = color,
        start = tip,
        end = wing1,
        strokeWidth = strokeWidthPx,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = tip,
        end = wing2,
        strokeWidth = strokeWidthPx,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawAmountPill(
    center: Offset,
    textLayout: TextLayoutResult,
    accentColor: Color,
    pillBaseSurface: Color,
    isDarkTheme: Boolean,
    padHPx: Float,
    padVPx: Float,
    borderWidthPx: Float,
    cornerRadiusPx: Float
) {
    val pillW = textLayout.size.width + padHPx * 2f
    val pillH = textLayout.size.height + padVPx * 2f
    val topLeft = Offset(center.x - pillW * 0.5f, center.y - pillH * 0.5f)
    val pillSize = Size(pillW, pillH)
    val radius = CornerRadius(cornerRadiusPx, cornerRadiusPx)

    val bgFill = lerp(pillBaseSurface, accentColor, if (isDarkTheme) 0.24f else 0.16f)
        .copy(alpha = 0.92f)

    drawRoundRect(
        color = bgFill,
        topLeft = topLeft,
        size = pillSize,
        cornerRadius = radius
    )
    drawRoundRect(
        color = accentColor.copy(alpha = 0.78f),
        topLeft = topLeft,
        size = pillSize,
        cornerRadius = radius,
        style = Stroke(width = borderWidthPx)
    )
    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(
            x = topLeft.x + padHPx,
            y = topLeft.y + padVPx
        )
    )
}
