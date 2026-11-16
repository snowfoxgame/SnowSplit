package com.snowsplit.app.core.designsystem.components.backgrounds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.random.Random

private data class CityRoadSegment(
    val startNorm: Offset,
    val endNorm: Offset,
    val widthDp: Float
)

private class CityCarAgent(
    var roadIndex: Int,
    var progress: Float,
    var direction: Float,
    var speedNormPerSec: Float,
    var pauseSeconds: Float,
    val color: Color,
    val scale: Float
)

private val MINI_CITY_ROADS = listOf(
    // Slanted diagonal roads + intersections
    CityRoadSegment(Offset(-0.08f, 0.26f), Offset(1.08f, 0.11f), widthDp = 34f),
    CityRoadSegment(Offset(-0.08f, 0.78f), Offset(1.08f, 0.62f), widthDp = 34f),
    CityRoadSegment(Offset(0.14f, -0.06f), Offset(0.56f, 1.06f), widthDp = 30f),
    CityRoadSegment(Offset(0.84f, -0.06f), Offset(0.42f, 1.06f), widthDp = 28f),
    // Horizontal cross street
    CityRoadSegment(Offset(-0.08f, 0.48f), Offset(1.08f, 0.50f), widthDp = 32f)
)

private val CUTESY_CAR_COLORS = listOf(
    Color(0xFF5D9CEC), // Soft Sky Blue
    Color(0xFFE86F68), // Warm Coral Red
    Color(0xFF58B98D), // Mint Green
    Color(0xFFEBB852), // Honey Yellow
    Color(0xFFA085E8), // Pastel Lavender
    Color(0xFFF093B0)  // Strawberry Pink
)

/**
 * Stylized low-contrast miniature city with horizontal and diagonal roads and cute 2D cars
 * moving at randomized speeds and intervals so foreground frosted-glass cards remain crisp.
 */
@Composable
fun MiniCityCanvas(
    reduceLiveBlur: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = MaterialTheme.colorScheme.background
    val carCount = if (reduceLiveBlur) 4 else 8

    val rng = remember { Random(9182) }
    val cars = remember(carCount) {
        List(carCount) { idx ->
            CityCarAgent(
                roadIndex = idx % MINI_CITY_ROADS.size,
                progress = rng.nextFloat(),
                direction = if (idx % 2 == 0) 1f else -1f,
                speedNormPerSec = 0.065f + rng.nextFloat() * 0.075f,
                pauseSeconds = if (idx % 3 == 0) rng.nextFloat() * 1.4f else 0f,
                color = CUTESY_CAR_COLORS[idx % CUTESY_CAR_COLORS.size],
                scale = 0.88f + rng.nextFloat() * 0.24f
            )
        }
    }

    var frameTick by remember { mutableLongStateOf(0L) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, cars, reduceLiveBlur) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var lastNanos = 0L
            var skipToggle = false
            while (true) {
                withFrameNanos { frameNanos ->
                    if (reduceLiveBlur) {
                        skipToggle = !skipToggle
                        if (skipToggle) {
                            lastNanos = frameNanos
                            return@withFrameNanos
                        }
                    }
                    val dt = if (lastNanos == 0L) {
                        0.016f
                    } else {
                        ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.04f)
                    }
                    lastNanos = frameNanos

                    for (car in cars) {
                        if (car.pauseSeconds > 0f) {
                            car.pauseSeconds -= dt
                            continue
                        }
                        car.progress += car.direction * car.speedNormPerSec * dt
                        if (car.progress > 1.08f || car.progress < -0.08f) {
                            car.roadIndex = rng.nextInt(MINI_CITY_ROADS.size)
                            car.direction = if (rng.nextBoolean()) 1f else -1f
                            car.progress = if (car.direction > 0f) -0.06f else 1.06f
                            car.speedNormPerSec = 0.065f + rng.nextFloat() * 0.085f
                            car.pauseSeconds = 0.35f + rng.nextFloat() * 1.9f
                        }
                    }
                    frameTick = frameNanos
                }
            }
        }
    }

    val roadColor = Color(0xFFCAD4CA)
    val roadEdgeColor = Color(0xFFB3C0B4)
    val laneDashColor = Color(0xFFF6F8F5)
    val treeColor = Color(0xFF9AC5A0)
    val roofColor = Color(0xFFE6C2AF)
    val wallColor = Color(0xFFF8EFE8)

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(18f, 16f), 0f) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        @Suppress("UNUSED_VARIABLE")
        val tick = frameTick

        val decorSpots = listOf(
            Offset(0.18f, 0.14f),
            Offset(0.68f, 0.28f),
            Offset(0.28f, 0.62f),
            Offset(0.76f, 0.84f),
            Offset(0.12f, 0.88f),
            Offset(0.56f, 0.36f)
        )
        decorSpots.forEachIndexed { idx, spot ->
            val p = Offset(spot.x * size.width, spot.y * size.height)
            if (idx % 2 == 0) {
                drawCircle(color = treeColor.copy(alpha = 0.55f), radius = 13.dp.toPx(), center = p)
                drawCircle(
                    color = Color(0xFF7CB084).copy(alpha = 0.45f),
                    radius = 8.dp.toPx(),
                    center = p - Offset(3.dp.toPx(), 2.dp.toPx())
                )
            } else {
                val houseSize = 22.dp.toPx()
                drawRoundRect(
                    color = wallColor,
                    topLeft = p - Offset(houseSize / 2f, houseSize / 2f),
                    size = Size(houseSize, houseSize),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = roofColor,
                    topLeft = p - Offset(houseSize * 0.55f, houseSize * 0.55f),
                    size = Size(houseSize * 1.1f, houseSize * 0.48f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        for (road in MINI_CITY_ROADS) {
            val start = Offset(road.startNorm.x * size.width, road.startNorm.y * size.height)
            val end = Offset(road.endNorm.x * size.width, road.endNorm.y * size.height)
            drawLine(
                color = roadEdgeColor,
                start = start,
                end = end,
                strokeWidth = (road.widthDp + 4f).dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        for (road in MINI_CITY_ROADS) {
            val start = Offset(road.startNorm.x * size.width, road.startNorm.y * size.height)
            val end = Offset(road.endNorm.x * size.width, road.endNorm.y * size.height)
            drawLine(
                color = roadColor,
                start = start,
                end = end,
                strokeWidth = road.widthDp.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = laneDashColor.copy(alpha = 0.85f),
                start = start,
                end = end,
                strokeWidth = 2.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        for (car in cars) {
            if (car.pauseSeconds > 0f) continue
            val road = MINI_CITY_ROADS[car.roadIndex]
            val start = Offset(road.startNorm.x * size.width, road.startNorm.y * size.height)
            val end = Offset(road.endNorm.x * size.width, road.endNorm.y * size.height)
            val dx = end.x - start.x
            val dy = end.y - start.y
            val length = kotlin.math.hypot(dx, dy).coerceAtLeast(1f)
            val unitX = dx / length
            val unitY = dy / length
            val normalX = -unitY
            val normalY = unitX
            val laneOffsetPx = (road.widthDp * 0.22f).dp.toPx() * car.direction

            val center = Offset(
                x = start.x + dx * car.progress + normalX * laneOffsetPx,
                y = start.y + dy * car.progress + normalY * laneOffsetPx
            )

            val angleDeg = (atan2(dy, dx) * (180.0 / PI)).toFloat() +
                if (car.direction < 0f) 180f else 0f

            val bodyW = 24.dp.toPx() * car.scale
            val bodyH = 13.dp.toPx() * car.scale

            rotate(degrees = angleDeg, pivot = center) {
                drawCircle(
                    color = Color(0xFFFFF3B0).copy(alpha = 0.55f),
                    radius = 5.dp.toPx() * car.scale,
                    center = center + Offset(bodyW * 0.56f, 0f)
                )
                val wheelW = 5.dp.toPx() * car.scale
                val wheelH = 2.5.dp.toPx() * car.scale
                val wheelColor = Color(0xFF374151)
                drawRoundRect(
                    color = wheelColor,
                    topLeft = center + Offset(-bodyW * 0.34f, -bodyH * 0.58f),
                    size = Size(wheelW, wheelH),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                drawRoundRect(
                    color = wheelColor,
                    topLeft = center + Offset(bodyW * 0.14f, -bodyH * 0.58f),
                    size = Size(wheelW, wheelH),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                drawRoundRect(
                    color = wheelColor,
                    topLeft = center + Offset(-bodyW * 0.34f, bodyH * 0.42f),
                    size = Size(wheelW, wheelH),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                drawRoundRect(
                    color = wheelColor,
                    topLeft = center + Offset(bodyW * 0.14f, bodyH * 0.42f),
                    size = Size(wheelW, wheelH),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                drawRoundRect(
                    color = car.color,
                    topLeft = center - Offset(bodyW / 2f, bodyH / 2f),
                    size = Size(bodyW, bodyH),
                    cornerRadius = CornerRadius(4.5.dp.toPx(), 4.5.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.82f),
                    topLeft = center - Offset(bodyW * 0.12f, bodyH * 0.36f),
                    size = Size(bodyW * 0.36f, bodyH * 0.72f),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )
            }
        }
    }
}
