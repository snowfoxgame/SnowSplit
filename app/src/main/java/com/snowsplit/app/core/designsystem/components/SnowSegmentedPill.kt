package com.snowsplit.app.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.snowsplit.app.core.designsystem.SnowDosThemeTokens

data class SnowSegmentOption<T>(
    val value: T,
    val label: String
)

@Composable
fun <T> SnowSegmentedPill(
    options: List<SnowSegmentOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    blurScope: SnowBlurScope = SnowBlurScope.BACKGROUND
) {
    if (options.isEmpty()) return

    val tokens = SnowDosThemeTokens.current
    val pillShape = if (tokens.surface.usePixelRetroStyle) {
        RoundedCornerShape(tokens.radius.pill)
    } else {
        RoundedCornerShape(percent = 50)
    }
    val selectedIndex = options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    val sliderPosition = remember { Animatable(selectedIndex.toFloat()) }

    LaunchedEffect(selectedIndex) {
        sliderPosition.animateTo(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .glassReadySurface(
                shape = pillShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                alpha = tokens.surface.interactiveAlpha,
                blurScope = blurScope
            ),
        shape = pillShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .height(IntrinsicSize.Min)
        ) {
            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(1f / options.size)
                        .graphicsLayer {
                            translationX = sliderPosition.value * size.width
                        }
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(
                                alpha = tokens.surface.selectedAlpha
                            ),
                            shape = pillShape
                        )
                        .snowSurfaceBorder(shape = pillShape, highlighted = true)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEach { option ->
                    val isSelected = option.value == selected
                    val interactionSource = remember(option.value) { MutableInteractionSource() }
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "segmentedPillTextColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .snowPressFeedback(interactionSource = interactionSource, pressedScale = 0.97f)
                            .clip(pillShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Tab,
                                onClick = { onSelect(option.value) }
                            )
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
