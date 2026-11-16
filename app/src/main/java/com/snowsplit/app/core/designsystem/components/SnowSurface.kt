package com.snowsplit.app.core.designsystem.components

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.snowsplit.app.core.designsystem.SnowDosThemeTokens
import dev.chrisbanes.haze.HazeArea
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

private val blurBackgroundAreaFilter: (HazeArea) -> Boolean = { it.zIndex < 1f }
private val blurChromeAreaFilter: (HazeArea) -> Boolean = { it.zIndex < 2f }
private val blurOverlayAreaFilter: (HazeArea) -> Boolean = { it.zIndex == 0f || it.zIndex == 2f }

@Composable
fun snowSurfaceBorderBrush(highlighted: Boolean = false): Brush {
    val tokens = SnowDosThemeTokens.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val highlightAlpha = tokens.surface.borderHighlightAlpha
    val shadowAlpha = tokens.surface.borderShadowAlpha
    val borderAlpha = tokens.surface.borderAlpha
    val isPixelRetro = tokens.surface.usePixelRetroStyle
    val accentTintBorder = tokens.surface.glassAccentBorderTint

    return remember(
        highlighted,
        primary,
        outline,
        isDarkTheme,
        highlightAlpha,
        shadowAlpha,
        borderAlpha,
        isPixelRetro,
        accentTintBorder
    ) {
        if (isPixelRetro) {
            SolidColor(if (highlighted) primary else outline)
        } else if (accentTintBorder) {
            if (highlighted) {
                val highlight = lerp(primary, Color.White, 0.35f)
                Brush.linearGradient(
                    colors = listOf(
                        highlight.copy(alpha = 0.92f),
                        primary.copy(alpha = 0.56f)
                    )
                )
            } else {
                val topLeft = lerp(primary, Color.White, 0.45f).copy(alpha = highlightAlpha)
                val mid = primary.copy(alpha = borderAlpha)
                val bottomRight = primary.copy(alpha = shadowAlpha)
                Brush.linearGradient(colors = listOf(topLeft, mid, bottomRight))
            }
        } else if (highlighted) {
            val highlight = lerp(primary, Color.White, if (isDarkTheme) 0.42f else 0.25f)
            Brush.linearGradient(
                colors = listOf(
                    highlight.copy(alpha = 0.85f),
                    primary.copy(alpha = 0.48f)
                )
            )
        } else {
            val topLeftColor = Color.White.copy(alpha = highlightAlpha)
            val bottomRightColor = if (isDarkTheme) {
                Color.White.copy(alpha = shadowAlpha)
            } else {
                Color.Black.copy(alpha = shadowAlpha)
            }
            Brush.linearGradient(
                colors = listOf(
                    topLeftColor,
                    Color(
                        red = (topLeftColor.red + bottomRightColor.red) * 0.5f,
                        green = (topLeftColor.green + bottomRightColor.green) * 0.5f,
                        blue = (topLeftColor.blue + bottomRightColor.blue) * 0.5f,
                        alpha = borderAlpha
                    ),
                    bottomRightColor
                )
            )
        }
    }
}

@Composable
fun Modifier.snowSurfaceBorder(
    shape: Shape,
    highlighted: Boolean = false,
    borderWidth: Dp = SnowDosThemeTokens.current.surface.borderWidth
): Modifier = border(
    width = borderWidth,
    brush = snowSurfaceBorderBrush(highlighted = highlighted),
    shape = shape
)

@Composable
fun Modifier.glassReadySurface(
    shape: Shape,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    alpha: Float = SnowDosThemeTokens.current.surface.cardAlpha,
    highlighted: Boolean = false,
    borderWidth: Dp = SnowDosThemeTokens.current.surface.borderWidth,
    blurScope: SnowBlurScope = SnowBlurScope.BACKGROUND,
    hazeState: HazeState? = LocalAppHazeState.current
): Modifier {
    val tokens = SnowDosThemeTokens.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDarkTheme = backgroundColor.luminance() < 0.5f
    val blurRadius = tokens.surface.blurRadius
    val isPixelRetro = tokens.surface.usePixelRetroStyle
    val supportsNativeBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val effectiveReduceLiveBlur = tokens.surface.reduceLiveBlur || !supportsNativeBlur
    val pixelShadowOffset = tokens.surface.pixelShadowOffsetDp
    val pixelShadowColor = MaterialTheme.colorScheme.outline

    val withPixelShadow = if (isPixelRetro && pixelShadowOffset > 0.dp) {
        this.drawBehind {
            val offsetPx = pixelShadowOffset.toPx()
            val outline = shape.createOutline(size, layoutDirection, this)
            translate(left = offsetPx, top = offsetPx) {
                drawOutline(outline = outline, color = pixelShadowColor)
            }
        }
    } else {
        this
    }

    val tintColor = remember(containerColor, alpha, isDarkTheme, isPixelRetro, effectiveReduceLiveBlur) {
        if (isPixelRetro) {
            containerColor.copy(alpha = 1f)
        } else {
            val glassBase = if (isDarkTheme) {
                lerp(containerColor, Color.White, 0.06f)
            } else {
                lerp(containerColor, Color.White, 0.42f)
            }
            val effectiveAlpha = if (effectiveReduceLiveBlur) {
                (alpha + 0.44f).coerceIn(0.64f, 0.88f)
            } else {
                alpha
            }
            glassBase.copy(alpha = effectiveAlpha)
        }
    }

    val areaFilter = when (blurScope) {
        SnowBlurScope.BACKGROUND -> blurBackgroundAreaFilter
        SnowBlurScope.CHROME -> blurChromeAreaFilter
        SnowBlurScope.OVERLAY -> blurOverlayAreaFilter
    }

    val useLiveHaze = supportsNativeBlur && !isPixelRetro && !effectiveReduceLiveBlur &&
        hazeState != null && blurRadius > Dp.Hairline
    val clipped = withPixelShadow.clip(shape)
    val backdrop = if (useLiveHaze) {
        val style = remember(backgroundColor, tintColor, blurRadius) {
            HazeStyle(
                backgroundColor = backgroundColor.copy(alpha = 0.35f),
                tint = HazeTint(tintColor),
                blurRadius = blurRadius,
                noiseFactor = 0f
            )
        }
        clipped.hazeEffect(state = hazeState) {
            this.style = style
            this.canDrawArea = areaFilter
        }
    } else {
        clipped.background(color = tintColor, shape = shape)
    }

    return backdrop.border(
        width = borderWidth,
        brush = snowSurfaceBorderBrush(highlighted = highlighted),
        shape = shape
    )
}

@Composable
fun SnowGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SnowDosThemeTokens.current.radius.card),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    alpha: Float = SnowDosThemeTokens.current.surface.cardAlpha,
    highlighted: Boolean = false,
    borderWidth: Dp = SnowDosThemeTokens.current.surface.borderWidth,
    blurScope: SnowBlurScope = SnowBlurScope.BACKGROUND,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit
) {
    Surface(
        shape = shape,
        color = Color.Transparent,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier.glassReadySurface(
            shape = shape,
            containerColor = containerColor,
            alpha = alpha,
            highlighted = highlighted,
            borderWidth = borderWidth,
            blurScope = blurScope
        ),
        content = content
    )
}

@Composable
fun SnowGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    shape: Shape = RoundedCornerShape(SnowDosThemeTokens.current.radius.pill),
    blurScope: SnowBlurScope = SnowBlurScope.BACKGROUND,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val tokens = SnowDosThemeTokens.current
    val interactionSource = remember { MutableInteractionSource() }
    val containerColor = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (highlighted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val alpha = if (highlighted) {
        tokens.surface.selectedAlpha
    } else {
        tokens.surface.interactiveAlpha
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = shape,
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp,
            hoveredElevation = 0.dp,
            focusedElevation = 0.dp
        ),
        modifier = modifier
            .snowPressFeedback(interactionSource, enabled = enabled)
            .glassReadySurface(
                shape = shape,
                containerColor = containerColor,
                alpha = alpha,
                highlighted = highlighted,
                blurScope = blurScope
            ),
        content = content
    )
}

@Composable
fun SnowGlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SnowDosThemeTokens.current.radius.pill),
    blurScope: SnowBlurScope = SnowBlurScope.BACKGROUND,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val tokens = SnowDosThemeTokens.current
    val interactionSource = remember { MutableInteractionSource() }
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = leadingIcon,
        interactionSource = interactionSource,
        shape = shape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            selectedContainerColor = Color.Transparent
        ),
        elevation = FilterChipDefaults.filterChipElevation(
            elevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            draggedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        border = null,
        modifier = modifier
            .snowPressFeedback(interactionSource)
            .glassReadySurface(
                shape = shape,
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                alpha = if (selected) tokens.surface.selectedAlpha else tokens.surface.interactiveAlpha,
                highlighted = selected,
                blurScope = blurScope
            ),
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
fun SnowGlassSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    blurScope: SnowBlurScope = SnowBlurScope.BACKGROUND
) {
    val tokens = SnowDosThemeTokens.current
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        SnowGlassCard(
            shape = RoundedCornerShape(tokens.radius.compactCard),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            alpha = (tokens.surface.cardAlpha + 0.18f).coerceIn(0.36f, 0.58f),
            blurScope = blurScope,
            modifier = Modifier.padding(horizontal = tokens.spacing.md)
        ) {
            Row(
                modifier = Modifier.padding(
                    start = tokens.spacing.md,
                    end = if (data.visuals.actionLabel != null) tokens.spacing.xs else tokens.spacing.md,
                    top = 6.dp,
                    bottom = 6.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)
            ) {
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 6.dp)
                )
                val actionLabel = data.visuals.actionLabel
                if (actionLabel != null) {
                    TextButton(onClick = { data.performAction() }) {
                        Text(
                            text = actionLabel,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

fun Modifier.snowPressFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    enableHaptics: Boolean = true
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(isPressed, enabled, enableHaptics) {
        if (isPressed && enabled && enableHaptics) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "snowPressScale"
    )

    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
