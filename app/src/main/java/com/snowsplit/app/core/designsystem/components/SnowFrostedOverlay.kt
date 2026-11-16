package com.snowsplit.app.core.designsystem.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.snowsplit.app.core.designsystem.SnowDosThemeTokens

internal val LocalSnowOverlayController = compositionLocalOf<SnowOverlayController?> { null }

internal class OverlaySlot(
    val id: Any,
    initialContent: @Composable () -> Unit
) {
    var content: @Composable () -> Unit by mutableStateOf(initialContent)
}

internal class SnowOverlayController {
    val slots = mutableStateListOf<OverlaySlot>()

    fun upsert(id: Any, content: @Composable () -> Unit) {
        val existing = slots.firstOrNull { it.id == id }
        if (existing != null) {
            existing.content = content
        } else {
            slots.add(OverlaySlot(id = id, initialContent = content))
        }
    }

    fun remove(id: Any) {
        slots.removeAll { it.id == id }
    }
}

@Composable
internal fun SnowOverlayHost(
    controller: SnowOverlayController,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        controller.slots.forEach { slot ->
            key(slot.id) {
                slot.content()
            }
        }
    }
}

@Composable
fun SnowFrostedDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 420.dp,
    dismissOnClickOutside: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    SnowFrostedOverlayPortal(visible = visible) { visibleState ->
        BackHandler(enabled = visible) {
            onDismissRequest()
        }

        val transition = rememberTransition(visibleState, label = "snowFrostedDialog")
        val scrimProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(durationMillis = 220, easing = FastOutSlowInEasing)
                } else {
                    tween(durationMillis = 170, easing = FastOutSlowInEasing)
                }
            },
            label = "dialogScrim"
        ) { shown -> if (shown) 1f else 0f }

        val scale by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMedium
                    )
                } else {
                    tween(durationMillis = 160, easing = FastOutSlowInEasing)
                }
            },
            label = "dialogScale"
        ) { shown -> if (shown) 1f else 0.90f }

        val alpha by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(durationMillis = 180, easing = FastOutSlowInEasing)
                } else {
                    tween(durationMillis = 150, easing = FastOutSlowInEasing)
                }
            },
            label = "dialogAlpha"
        ) { shown -> if (shown) 1f else 0f }

        val tokens = SnowDosThemeTokens.current
        val dialogShape = RoundedCornerShape(
            if (tokens.surface.usePixelRetroStyle) tokens.radius.sheet else 28.dp
        )
        val dialogTintAlpha = (tokens.surface.cardAlpha + 0.14f).coerceIn(0.32f, 0.56f)
        val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.86f

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = scrimProgress }
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (visible && dismissOnClickOutside) {
                            onDismissRequest()
                        }
                    }
            )

            Box(
                modifier = modifier
                    .align(Alignment.Center)
                    .systemBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .fillMaxWidth()
                    .widthIn(min = 280.dp, max = maxWidth)
                    .heightIn(max = maxHeight)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Surface(
                    shape = dialogShape,
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassReadySurface(
                            shape = dialogShape,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            alpha = dialogTintAlpha,
                            borderWidth = tokens.surface.borderWidth,
                            blurScope = SnowBlurScope.OVERLAY
                        )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        content = content
                    )
                }
            }
        }
    }
}

@Composable
fun SnowFrostedSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnClickOutside: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    SnowFrostedOverlayPortal(visible = visible) { visibleState ->
        BackHandler(enabled = visible) {
            onDismissRequest()
        }

        val transition = rememberTransition(visibleState, label = "snowFrostedSheet")
        val scrimProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(durationMillis = 240, easing = FastOutSlowInEasing)
                } else {
                    tween(durationMillis = 190, easing = FastOutSlowInEasing)
                }
            },
            label = "sheetScrim"
        ) { shown -> if (shown) 1f else 0f }

        val slideProgress by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    spring(
                        dampingRatio = 0.86f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                } else {
                    tween(durationMillis = 190, easing = FastOutSlowInEasing)
                }
            },
            label = "sheetSlide"
        ) { shown -> if (shown) 1f else 0f }

        val alpha by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    tween(durationMillis = 180, easing = FastOutSlowInEasing)
                } else {
                    tween(durationMillis = 160, easing = FastOutSlowInEasing)
                }
            },
            label = "sheetAlpha"
        ) { shown -> if (shown) 1f else 0f }

        val tokens = SnowDosThemeTokens.current
        val sheetShape = RoundedCornerShape(
            if (tokens.surface.usePixelRetroStyle) tokens.radius.sheet else 28.dp
        )
        val sheetTintAlpha = (tokens.surface.cardAlpha + 0.14f).coerceIn(0.32f, 0.56f)
        val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = scrimProgress }
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (visible && dismissOnClickOutside) {
                            onDismissRequest()
                        }
                    }
            )

            Box(
                modifier = modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .heightIn(max = maxHeight)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .graphicsLayer {
                        val travel = size.height.coerceAtLeast(1f) * 0.35f
                        translationY = (1f - slideProgress) * travel
                        this.alpha = alpha
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Surface(
                    shape = sheetShape,
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassReadySurface(
                            shape = sheetShape,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            alpha = sheetTintAlpha,
                            borderWidth = tokens.surface.borderWidth,
                            blurScope = SnowBlurScope.OVERLAY
                        )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        content = content
                    )
                }
            }
        }
    }
}

@Composable
private fun SnowFrostedOverlayPortal(
    visible: Boolean,
    overlayContent: @Composable (MutableTransitionState<Boolean>) -> Unit
) {
    val controller = LocalSnowOverlayController.current
    val overlayKey = remember { Any() }
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = visible
    val isActive = visibleState.currentState || visibleState.targetState

    if (controller == null) {
        if (isActive) {
            overlayContent(visibleState)
        }
        return
    }

    if (isActive) {
        SideEffect {
            controller.upsert(overlayKey) {
                overlayContent(visibleState)
            }
        }
    } else {
        SideEffect {
            controller.remove(overlayKey)
        }
    }

    DisposableEffect(controller, overlayKey) {
        onDispose {
            controller.remove(overlayKey)
        }
    }
}
