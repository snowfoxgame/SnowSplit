package com.snowsplit.app.core.designsystem.components

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.snowsplit.app.core.common.AppBitmapCache
import com.snowsplit.app.core.common.loadSampledBitmapFromUri
import com.snowsplit.app.core.designsystem.SnowDosThemeTokens
import com.snowsplit.app.core.designsystem.components.backgrounds.BubbleTouchImpulseState
import com.snowsplit.app.core.designsystem.components.backgrounds.InteractiveBubblesCanvas
import com.snowsplit.app.core.designsystem.components.backgrounds.LiveBackgroundScene
import com.snowsplit.app.core.designsystem.components.backgrounds.MiniCityCanvas
import com.snowsplit.app.core.designsystem.components.backgrounds.WeatherTimeCanvas
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

val LocalAppHazeState = compositionLocalOf<HazeState?> { null }

enum class SnowBlurScope {
    BACKGROUND,
    CHROME,
    OVERLAY
}

@Composable
fun rememberDecodedBackgroundBitmap(
    uriString: String?,
    maxWidthPx: Int = 1080,
    maxHeightPx: Int = 2280
): Bitmap? {
    val context = LocalContext.current
    val safeW = maxWidthPx.coerceAtLeast(64)
    val safeH = maxHeightPx.coerceAtLeast(64)
    val cachedSync = remember(uriString, safeW, safeH) {
        uriString?.takeIf { it.isNotBlank() }?.let {
            AppBitmapCache.get(AppBitmapCache.cacheKey(it, safeW, safeH))?.takeIf { bmp -> !bmp.isRecycled }
        }
    }

    val bitmap by produceState(initialValue = cachedSync, uriString, safeW, safeH) {
        value = if (uriString.isNullOrBlank()) {
            null
        } else {
            cachedSync ?: loadSampledBitmapFromUri(context, uriString, safeW, safeH)
        }
    }
    return bitmap
}

@Composable
fun AppBackgroundContainer(
    backgroundImageUri: String?,
    applyReadabilityScrim: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceLiveBlur: Boolean = false,
    liveScene: LiveBackgroundScene = LiveBackgroundScene.None,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val (targetW, targetH) = remember(config.screenWidthDp, config.screenHeightDp, density) {
        with(density) {
            config.screenWidthDp.dp.roundToPx().coerceAtLeast(360) to
                config.screenHeightDp.dp.roundToPx().coerceAtLeast(640)
        }
    }
    val bitmap = rememberDecodedBackgroundBitmap(
        uriString = backgroundImageUri,
        maxWidthPx = targetW,
        maxHeightPx = targetH
    )
    val backgroundHazeState = rememberHazeState()
    val effectiveReduceBlur = reduceLiveBlur || Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    val appHazeState = if (effectiveReduceBlur) null else backgroundHazeState
    val overlayController = remember { SnowOverlayController() }
    val hasActiveOverlay by remember {
        derivedStateOf { overlayController.slots.isNotEmpty() }
    }
    val bubbleImpulse = remember { BubbleTouchImpulseState() }
    val rootModifier = if (liveScene is LiveBackgroundScene.InteractiveBubbles) {
        modifier
            .fillMaxSize()
            .pointerInput(bubbleImpulse) {
                awaitPointerEventScope {
                    var prevPos = Offset.Unspecified
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val activeChange = event.changes.firstOrNull { it.pressed }
                        if (activeChange != null) {
                            val currentPos = activeChange.position
                            val delta = if (prevPos != Offset.Unspecified) currentPos - prevPos else Offset.Zero
                            bubbleImpulse.recordTouch(
                                position = currentPos,
                                delta = delta,
                                timeNanos = System.nanoTime()
                            )
                            prevPos = currentPos
                        } else {
                            prevPos = Offset.Unspecified
                        }
                    }
                }
            }
    } else {
        modifier.fillMaxSize()
    }

    Box(modifier = rootModifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (appHazeState != null) {
                        Modifier.hazeSource(state = appHazeState, zIndex = 0f)
                    } else {
                        Modifier
                    }
                )
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (liveScene) {
                LiveBackgroundScene.InteractiveBubbles -> InteractiveBubblesCanvas(
                    impulseState = bubbleImpulse,
                    reduceLiveBlur = reduceLiveBlur
                )
                LiveBackgroundScene.MiniCity -> MiniCityCanvas(
                    reduceLiveBlur = reduceLiveBlur
                )
                is LiveBackgroundScene.WeatherTime -> WeatherTimeCanvas(
                    style = liveScene.style,
                    snapshot = liveScene.snapshot,
                    reduceLiveBlur = reduceLiveBlur
                )
                LiveBackgroundScene.None -> {
                    if (bitmap != null) {
                        val isPixelRetro = SnowDosThemeTokens.current.surface.usePixelRetroStyle
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            filterQuality = if (isPixelRetro) FilterQuality.None else FilterQuality.Low,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (applyReadabilityScrim) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.background.copy(
                                            alpha = if (darkTheme) 0.52f else 0.48f
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            LocalAppHazeState provides appHazeState,
            LocalSnowOverlayController provides overlayController
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (appHazeState != null && hasActiveOverlay) {
                            Modifier.hazeSource(state = appHazeState, zIndex = 2f)
                        } else {
                            Modifier
                        }
                    )
            ) {
                content()
            }
            SnowOverlayHost(controller = overlayController)
        }
    }
}
