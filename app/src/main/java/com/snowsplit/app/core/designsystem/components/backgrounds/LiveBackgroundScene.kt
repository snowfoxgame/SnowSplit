package com.snowsplit.app.core.designsystem.components.backgrounds

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import com.snowsplit.app.domain.model.WeatherSnapshot
import com.snowsplit.app.domain.model.WeatherThemeStyle

@Immutable
sealed interface LiveBackgroundScene {
    data object None : LiveBackgroundScene
    data object InteractiveBubbles : LiveBackgroundScene
    data object MiniCity : LiveBackgroundScene
    data class WeatherTime(
        val style: WeatherThemeStyle,
        val snapshot: WeatherSnapshot
    ) : LiveBackgroundScene
}

/**
 * Shared passive pointer-impulse bridge passed from the root [PointerEventPass.Initial] observer
 * into live interactive background canvases without consuming UI gestures.
 */
@Stable
class BubbleTouchImpulseState {
    @Volatile
    var pointerPosition: Offset = Offset.Unspecified

    @Volatile
    var pointerVelocity: Offset = Offset.Zero

    @Volatile
    var lastTouchTimeNanos: Long = 0L

    fun recordTouch(position: Offset, delta: Offset, timeNanos: Long) {
        pointerPosition = position
        pointerVelocity = delta
        lastTouchTimeNanos = timeNanos
    }
}
