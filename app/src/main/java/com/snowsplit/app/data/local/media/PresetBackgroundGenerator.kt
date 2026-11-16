package com.snowsplit.app.data.local.media

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import com.snowsplit.app.core.common.DispatcherProvider
import com.snowsplit.app.core.designsystem.ThemePreset
import com.snowsplit.app.core.designsystem.ThemeRegistry
import com.snowsplit.app.core.designsystem.parseHexColorOrNull
import com.snowsplit.app.domain.model.EightBitVariant
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

@Singleton
class PresetBackgroundGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) {
    private val mutex = Mutex()
    private val cacheDir: File by lazy {
        File(context.filesDir, "theme_backgrounds").apply { mkdirs() }
    }

    suspend fun getOrCreateBackgroundUri(
        presetId: String,
        darkTheme: Boolean = isSystemDarkTheme(),
        gamingAccentHex: String = "#00E5FF",
        eightBitVariant: EightBitVariant = EightBitVariant.PASTEL_PINK
    ): String? =
        withContext(dispatcherProvider.io) {
            val preset = ThemeRegistry.findPreset(presetId)
            if (!preset.hasPatternBackground) return@withContext null

            val resolvedGamingColor = resolveGamingAccentColorInt(gamingAccentHex)
            val isStandard = preset.id == "standard_m3"
            val variantKey = resolveVariantKey(
                presetId = preset.id,
                darkTheme = darkTheme,
                gamingAccentColorInt = resolvedGamingColor,
                eightBitVariant = eightBitVariant
            )
            val file = File(cacheDir, "${variantKey}_$CACHE_VERSION.png")
            if (file.exists() && file.length() > 0L) {
                return@withContext Uri.fromFile(file).toString()
            }

            mutex.withLock {
                if (!file.exists() || file.length() == 0L) {
                    cleanupStaleFiles(presetId = preset.id, variantKey = variantKey, isThumbnail = false)
                    renderAndSave(
                        preset = preset,
                        darkTheme = darkTheme,
                        gamingAccentColorInt = resolvedGamingColor,
                        eightBitVariant = eightBitVariant,
                        width = FULL_WIDTH,
                        height = FULL_HEIGHT,
                        symbolCount = when {
                            preset.id == "standard_m3" -> 68
                            preset.id == "gaming" -> 58
                            preset.id.startsWith("8bit_") -> 64
                            else -> 54
                        },
                        baseRadius = if (isStandard) 18f else 26f,
                        minDistance = if (isStandard) 108f else 124f,
                        targetFile = file
                    )
                }
            }
            Uri.fromFile(file).toString()
        }

    suspend fun getOrCreateAllThumbnails(
        gamingAccentHex: String = "#00E5FF",
        eightBitVariant: EightBitVariant = EightBitVariant.PASTEL_PINK
    ): Map<String, String> =
        withContext(dispatcherProvider.io) {
            val isDark = isSystemDarkTheme()
            val resolvedGamingColor = resolveGamingAccentColorInt(gamingAccentHex)
            ThemeRegistry.presets
                .filter { it.hasPatternBackground }
                .associate { preset ->
                    val isStandard = preset.id == "standard_m3"
                    val variantKey = resolveVariantKey(
                        presetId = preset.id,
                        darkTheme = isDark,
                        gamingAccentColorInt = resolvedGamingColor,
                        eightBitVariant = eightBitVariant
                    )
                    val thumbFile = File(cacheDir, "${variantKey}_thumb_$CACHE_VERSION.png")
                    if (!thumbFile.exists() || thumbFile.length() == 0L) {
                        mutex.withLock {
                            if (!thumbFile.exists() || thumbFile.length() == 0L) {
                                cleanupStaleFiles(presetId = preset.id, variantKey = variantKey, isThumbnail = true)
                                renderAndSave(
                                    preset = preset,
                                    darkTheme = isDark,
                                    gamingAccentColorInt = resolvedGamingColor,
                                    eightBitVariant = eightBitVariant,
                                    width = THUMB_WIDTH,
                                    height = THUMB_HEIGHT,
                                    symbolCount = if (isStandard) 18 else 14,
                                    baseRadius = if (isStandard) 11f else 15f,
                                    minDistance = if (isStandard) 38f else 46f,
                                    targetFile = thumbFile
                                )
                            }
                        }
                    }
                    preset.id to Uri.fromFile(thumbFile).toString()
                }
        }

    private fun resolveGamingAccentColorInt(hex: String): Int =
        parseHexColorOrNull(hex)?.toArgb() ?: Color.rgb(0, 229, 255)

    private fun resolveVariantKey(
        presetId: String,
        darkTheme: Boolean,
        gamingAccentColorInt: Int,
        eightBitVariant: EightBitVariant
    ): String = when (presetId) {
        "standard_m3" -> "${presetId}_${if (darkTheme) "dark" else "light"}"
        "gaming" -> "${presetId}_${Integer.toHexString(gamingAccentColorInt and 0xFFFFFF)}"
        "8bit_cute" -> "${presetId}_${eightBitVariant.name.lowercase()}"
        else -> presetId
    }

    private fun cleanupStaleFiles(
        presetId: String,
        variantKey: String,
        isThumbnail: Boolean
    ) {
        val suffix = if (isThumbnail) "_thumb" else ""
        listOf("v1", "v2", "v3", "v4", "v4_snowsplit", "v5", "v6").forEach { oldVersion ->
            File(cacheDir, "$variantKey${suffix}_$oldVersion.png").delete()
            File(cacheDir, "$presetId${suffix}_$oldVersion.png").delete()
        }
        if (presetId == "gaming") {
            val activeName = "${variantKey}${suffix}_$CACHE_VERSION.png"
            cacheDir.listFiles { _, name ->
                name.startsWith("gaming_") &&
                    (if (isThumbnail) name.contains("_thumb_") else !name.contains("_thumb_")) &&
                    name != activeName
            }?.forEach { it.delete() }
        }
    }

    private fun isSystemDarkTheme(): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    private fun renderAndSave(
        preset: ThemePreset,
        darkTheme: Boolean,
        gamingAccentColorInt: Int,
        eightBitVariant: EightBitVariant,
        width: Int,
        height: Int,
        symbolCount: Int,
        baseRadius: Float,
        minDistance: Float,
        targetFile: File
    ) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgColor = when {
            preset.id == "standard_m3" && darkTheme -> Color.rgb(21, 22, 26)
            preset.id == "standard_m3" -> Color.rgb(242, 243, 247)
            preset.id == "8bit_cute" && eightBitVariant == EightBitVariant.PASTEL_BLUE ->
                Color.rgb(236, 245, 255)
            preset.id == "8bit_cute" -> Color.rgb(255, 238, 244)
            else -> preset.paletteSpec?.background?.toArgb() ?: preset.previewSurface.toArgb()
        }
        canvas.drawColor(bgColor)
        drawLightThemeGradientIfNeeded(canvas, preset.id, width, height)

        when (preset.id) {
            "interactive_bubbles" -> drawStaticBubblesScene(canvas, width, height)
            "mini_city" -> drawStaticMiniCityScene(canvas, width, height)
            "weather_time" -> drawStaticWeatherScene(canvas, width, height)
            else -> {
                val rng = Random(preset.id.hashCode() * 31 + width + eightBitVariant.ordinal * 7)
                val placed = mutableListOf<Pair<Float, Float>>()
                val margin = baseRadius * 1.4f
                val maxAttempts = symbolCount * 24

                var attempts = 0
                while (placed.size < symbolCount && attempts < maxAttempts) {
                    attempts++
                    val x = margin + rng.nextFloat() * (width - margin * 2f)
                    val y = margin + rng.nextFloat() * (height - margin * 2f)
                    if (placed.none { (ox, oy) -> hypot(x - ox, y - oy) < minDistance }) {
                        placed += x to y
                        val scale = when {
                            preset.id == "standard_m3" -> 0.78f + rng.nextFloat() * 0.45f
                            preset.id.startsWith("8bit_") -> 0.9f + rng.nextFloat() * 0.45f
                            else -> 0.75f + rng.nextFloat() * 0.6f
                        }
                        val rotation = if (preset.id.startsWith("8bit_")) 0f else rng.nextFloat() * 360f
                        val variant = rng.nextInt(
                            when (preset.id) {
                                "standard_m3", "gaming" -> 4
                                else -> 3
                            }
                        )
                        canvas.save()
                        canvas.translate(x, y)
                        if (rotation != 0f) {
                            canvas.rotate(rotation)
                        }
                        canvas.scale(scale, scale)
                        drawPresetMotif(
                            canvas = canvas,
                            presetId = preset.id,
                            r = baseRadius,
                            variant = variant,
                            darkTheme = darkTheme,
                            gamingAccentColorInt = gamingAccentColorInt,
                            eightBitVariant = eightBitVariant,
                            rng = rng
                        )
                        canvas.restore()
                    }
                }
            }
        }

        FileOutputStream(targetFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 92, out)
        }
        bitmap.recycle()
    }

    private fun drawPresetMotif(
        canvas: Canvas,
        presetId: String,
        r: Float,
        variant: Int,
        darkTheme: Boolean,
        gamingAccentColorInt: Int,
        eightBitVariant: EightBitVariant,
        rng: Random
    ) {
        when (presetId) {
            "standard_m3" -> when (variant) {
                0 -> drawMinimalBubblePair(canvas, r, darkTheme)
                1 -> drawMinimalSparkle(canvas, r, darkTheme)
                2 -> drawMinimalSplitArc(canvas, r, darkTheme)
                else -> drawMinimalRing(canvas, r, darkTheme)
            }
            "winter" -> drawSnowflake(canvas, r)
            "halloween" -> when (variant) {
                0 -> drawPumpkin(canvas, r)
                1 -> drawGhost(canvas, r)
                else -> drawCandy(canvas, r)
            }
            "christmas" -> when (variant) {
                0 -> drawSantaHat(canvas, r)
                1 -> drawCandyCane(canvas, r)
                else -> drawGiftBox(canvas, r)
            }
            "summer" -> when (variant) {
                0 -> drawBeachBall(canvas, r)
                1 -> drawShell(canvas, r)
                else -> drawDivingMask(canvas, r)
            }
            "autumn" -> drawAutumnLeaf(canvas, r, variant)
            "spring" -> when (variant) {
                0 -> drawTulip(canvas, r)
                1 -> drawSunflower(canvas, r)
                else -> drawDandelion(canvas, r)
            }
            "easter" -> drawEasterEgg(canvas, r, variant, rng)
            "new_year" -> drawFirework(canvas, r, variant)
            "gaming" -> when (variant) {
                0 -> drawGamingController(canvas, r, gamingAccentColorInt)
                1 -> drawGamingDPad(canvas, r, gamingAccentColorInt)
                2 -> drawGamingActionCluster(canvas, r, gamingAccentColorInt)
                else -> drawGamingLedTrace(canvas, r, gamingAccentColorInt)
            }
            "8bit_cute" -> when (variant) {
                0, 1 -> drawEightBitPaw(canvas, r, eightBitVariant)
                else -> drawEightBitHeartOrStar(canvas, r, eightBitVariant, rng)
            }
            "8bit_cat" -> drawEightBitCatMotif(canvas, r, variant)
            "8bit_fox" -> drawEightBitFoxMotif(canvas, r, variant)
            "8bit_bunny" -> drawEightBitBunnyMotif(canvas, r, variant)
            "8bit_snake" -> drawEightBitSnakeMotif(canvas, r, variant)
        }
    }

    private fun fillPaint(color: Int, alpha: Int = 56): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
        this.alpha = alpha
    }

    private fun strokePaint(color: Int, width: Float, alpha: Int = 64): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.color = color
            this.alpha = alpha
        }

    // Subtle geometric bubble/split motifs for the standard SnowSplit background
    private fun drawMinimalBubblePair(canvas: Canvas, r: Float, darkTheme: Boolean) {
        val c1 = if (darkTheme) Color.rgb(120, 128, 146) else Color.rgb(142, 150, 168)
        val stroke = strokePaint(c1, r * 0.12f, alpha = if (darkTheme) 48 else 62)
        canvas.drawCircle(-r * 0.34f, -r * 0.16f, r * 0.42f, stroke)
        canvas.drawCircle(r * 0.36f, r * 0.22f, r * 0.28f, stroke)
    }

    private fun drawMinimalSparkle(canvas: Canvas, r: Float, darkTheme: Boolean) {
        val c = if (darkTheme) Color.rgb(138, 146, 166) else Color.rgb(124, 132, 150)
        val stroke = strokePaint(c, r * 0.12f, alpha = if (darkTheme) 44 else 58)
        canvas.drawLine(-r * 0.45f, 0f, r * 0.45f, 0f, stroke)
        canvas.drawLine(0f, -r * 0.45f, 0f, r * 0.45f, stroke)
    }

    private fun drawMinimalSplitArc(canvas: Canvas, r: Float, darkTheme: Boolean) {
        val c = if (darkTheme) Color.rgb(126, 134, 152) else Color.rgb(134, 142, 160)
        val stroke = strokePaint(c, r * 0.12f, alpha = if (darkTheme) 44 else 58)
        canvas.drawArc(RectF(-r * 0.55f, -r * 0.55f, r * 0.55f, r * 0.55f), 20f, 130f, false, stroke)
        canvas.drawArc(RectF(-r * 0.55f, -r * 0.55f, r * 0.55f, r * 0.55f), 200f, 130f, false, stroke)
    }

    private fun drawMinimalRing(canvas: Canvas, r: Float, darkTheme: Boolean) {
        val c = if (darkTheme) Color.rgb(132, 140, 158) else Color.rgb(128, 136, 154)
        val stroke = strokePaint(c, r * 0.12f, alpha = if (darkTheme) 42 else 54)
        canvas.drawCircle(0f, 0f, r * 0.48f, stroke)
    }

    private fun drawSnowflake(canvas: Canvas, r: Float) {
        val iceStroke = strokePaint(Color.rgb(92, 148, 204), r * 0.14f, alpha = 128)
        val frostGlow = strokePaint(Color.rgb(245, 251, 255), r * 0.08f, alpha = 158)
        for (i in 0 until 6) {
            val angle = i * 60.0 * PI / 180.0
            val ex = (cos(angle) * r).toFloat()
            val ey = (sin(angle) * r).toFloat()
            val bx = ex * 0.6f
            val by = ey * 0.6f
            val leftAngle = angle + 0.55
            val rightAngle = angle - 0.55
            val lx = bx + (cos(leftAngle) * r * 0.32f).toFloat()
            val ly = by + (sin(leftAngle) * r * 0.32f).toFloat()
            val rx = bx + (cos(rightAngle) * r * 0.32f).toFloat()
            val ry = by + (sin(rightAngle) * r * 0.32f).toFloat()

            canvas.drawLine(0f, 0f, ex, ey, iceStroke)
            canvas.drawLine(bx, by, lx, ly, iceStroke)
            canvas.drawLine(bx, by, rx, ry, iceStroke)

            canvas.drawLine(0f, 0f, ex, ey, frostGlow)
            canvas.drawLine(bx, by, lx, ly, frostGlow)
            canvas.drawLine(bx, by, rx, ry, frostGlow)
        }
    }

    private fun drawPumpkin(canvas: Canvas, r: Float) {
        val bounds = RectF(-r * 1.15f, -r * 1.15f, r * 1.15f, r * 1.15f)
        val saveCount = canvas.saveLayerAlpha(bounds, 78)
        try {
            val stemPaint = fillPaint(Color.rgb(112, 168, 82), alpha = 255)
            val bodyPaint = fillPaint(Color.rgb(255, 132, 34), alpha = 255)
            val ribPaint = strokePaint(Color.rgb(212, 86, 16), r * 0.06f, alpha = 255)
            val carvePaint = fillPaint(Color.rgb(26, 20, 24), alpha = 255)

            canvas.drawRoundRect(RectF(-r * 0.13f, -r * 0.92f, r * 0.13f, -r * 0.48f), r * 0.07f, r * 0.07f, stemPaint)
            val pumpkinBody = Path().apply { addOval(RectF(-r * 0.92f, -r * 0.52f, r * 0.92f, r * 0.70f), Path.Direction.CW) }
            val centerCrown = Path().apply { addOval(RectF(-r * 0.48f, -r * 0.58f, r * 0.48f, r * 0.74f), Path.Direction.CW) }
            pumpkinBody.op(centerCrown, Path.Op.UNION)
            canvas.drawPath(pumpkinBody, bodyPaint)
            canvas.drawArc(RectF(-r * 0.48f, -r * 0.56f, r * 0.48f, r * 0.72f), 105f, 150f, false, ribPaint)
            canvas.drawArc(RectF(-r * 0.48f, -r * 0.56f, r * 0.48f, r * 0.72f), 285f, 150f, false, ribPaint)

            val leftEye = Path().apply {
                moveTo(-r * 0.40f, -r * 0.04f); lineTo(-r * 0.15f, -r * 0.04f); lineTo(-r * 0.27f, -r * 0.25f); close()
            }
            val rightEye = Path().apply {
                moveTo(r * 0.15f, -r * 0.04f); lineTo(r * 0.40f, -r * 0.04f); lineTo(r * 0.27f, -r * 0.25f); close()
            }
            val grin = Path().apply {
                moveTo(-r * 0.40f, r * 0.17f); quadTo(0f, r * 0.48f, r * 0.40f, r * 0.17f); quadTo(0f, r * 0.32f, -r * 0.40f, r * 0.17f); close()
            }
            canvas.drawPath(leftEye, carvePaint)
            canvas.drawPath(rightEye, carvePaint)
            canvas.drawPath(grin, carvePaint)
        } finally {
            canvas.restoreToCount(saveCount)
        }
    }

    private fun drawGhost(canvas: Canvas, r: Float) {
        val body = fillPaint(Color.rgb(235, 230, 245), alpha = 52)
        val eye = fillPaint(Color.rgb(28, 25, 36), alpha = 85)
        val path = Path().apply {
            moveTo(-r * 0.65f, r * 0.7f); lineTo(-r * 0.65f, -r * 0.15f)
            arcTo(RectF(-r * 0.65f, -r * 0.8f, r * 0.65f, r * 0.5f), 180f, 180f, false)
            lineTo(r * 0.65f, r * 0.7f); lineTo(r * 0.32f, r * 0.48f); lineTo(0f, r * 0.7f); lineTo(-r * 0.32f, r * 0.48f); close()
        }
        canvas.drawPath(path, body)
        canvas.drawCircle(-r * 0.22f, -r * 0.15f, r * 0.1f, eye)
        canvas.drawCircle(r * 0.22f, -r * 0.15f, r * 0.1f, eye)
    }

    private fun drawCandy(canvas: Canvas, r: Float) {
        val wrapper = fillPaint(Color.rgb(255, 168, 60), alpha = 64)
        val stripe = fillPaint(Color.rgb(210, 90, 255), alpha = 54)
        val leftWing = Path().apply { moveTo(-r * 0.42f, 0f); lineTo(-r * 0.92f, -r * 0.36f); lineTo(-r * 0.92f, r * 0.36f); close() }
        val rightWing = Path().apply { moveTo(r * 0.42f, 0f); lineTo(r * 0.92f, -r * 0.36f); lineTo(r * 0.92f, r * 0.36f); close() }
        canvas.drawPath(leftWing, stripe)
        canvas.drawPath(rightWing, stripe)
        canvas.drawOval(RectF(-r * 0.5f, -r * 0.36f, r * 0.5f, r * 0.36f), wrapper)
    }

    private fun drawSantaHat(canvas: Canvas, r: Float) {
        val red = fillPaint(Color.rgb(212, 46, 54), alpha = 66)
        val white = fillPaint(Color.rgb(255, 250, 248), alpha = 82)
        val cone = Path().apply { moveTo(-r * 0.75f, r * 0.45f); lineTo(r * 0.1f, -r * 0.82f); lineTo(r * 0.72f, r * 0.45f); close() }
        canvas.drawPath(cone, red)
        canvas.drawRoundRect(RectF(-r * 0.82f, r * 0.35f, r * 0.78f, r * 0.65f), r * 0.15f, r * 0.15f, white)
        canvas.drawCircle(r * 0.15f, -r * 0.82f, r * 0.2f, white)
    }

    private fun drawCandyCane(canvas: Canvas, r: Float) {
        val white = strokePaint(Color.rgb(255, 252, 250), r * 0.28f, alpha = 78)
        val red = strokePaint(Color.rgb(212, 46, 54), r * 0.14f, alpha = 72)
        val path = Path().apply {
            moveTo(r * 0.18f, r * 0.85f); lineTo(r * 0.18f, -r * 0.35f)
            arcTo(RectF(-r * 0.42f, -r * 0.85f, r * 0.18f, -r * 0.2f), 0f, -180f, false)
        }
        canvas.drawPath(path, white)
        canvas.drawPath(path, red)
    }

    private fun drawGiftBox(canvas: Canvas, r: Float) {
        val box = fillPaint(Color.rgb(212, 46, 54), alpha = 62)
        val ribbon = fillPaint(Color.rgb(255, 240, 200), alpha = 80)
        canvas.drawRoundRect(RectF(-r * 0.65f, -r * 0.48f, r * 0.65f, r * 0.68f), r * 0.12f, r * 0.12f, box)
        canvas.drawRect(-r * 0.12f, -r * 0.48f, r * 0.12f, r * 0.68f, ribbon)
        canvas.drawRect(-r * 0.65f, -r * 0.05f, r * 0.65f, r * 0.18f, ribbon)
    }

    private fun drawBeachBall(canvas: Canvas, r: Float) {
        val base = fillPaint(Color.rgb(255, 198, 66), alpha = 122)
        val stripe1 = fillPaint(Color.rgb(0, 152, 186), alpha = 124)
        val stripe2 = fillPaint(Color.rgb(245, 96, 78), alpha = 124)
        val rect = RectF(-r * 0.78f, -r * 0.78f, r * 0.78f, r * 0.78f)
        canvas.drawOval(rect, base)
        canvas.drawArc(rect, -35f, 90f, true, stripe1)
        canvas.drawArc(rect, 145f, 90f, true, stripe2)
    }

    private fun drawShell(canvas: Canvas, r: Float) {
        val shell = fillPaint(Color.rgb(248, 122, 96), alpha = 114)
        val ridge = strokePaint(Color.rgb(255, 248, 240), r * 0.08f, alpha = 136)
        val rect = RectF(-r * 0.8f, -r * 0.6f, r * 0.8f, r * 0.85f)
        canvas.drawArc(rect, 195f, 150f, true, shell)
        for (deg in listOf(-40.0, -15.0, 15.0, 40.0)) {
            val rad = (deg - 90.0) * PI / 180.0
            canvas.drawLine(0f, r * 0.45f, (cos(rad) * r * 0.7f).toFloat(), (sin(rad) * r * 0.7f).toFloat(), ridge)
        }
    }

    private fun drawDivingMask(canvas: Canvas, r: Float) {
        val frame = strokePaint(Color.rgb(0, 134, 168), r * 0.16f, alpha = 126)
        val lens = fillPaint(Color.rgb(172, 238, 255), alpha = 112)
        val rect = RectF(-r * 0.78f, -r * 0.42f, r * 0.78f, r * 0.42f)
        canvas.drawRoundRect(rect, r * 0.25f, r * 0.25f, lens)
        canvas.drawRoundRect(rect, r * 0.25f, r * 0.25f, frame)
        canvas.drawLine(r * 0.78f, 0f, r * 0.95f, -r * 0.72f, frame)
    }

    private fun drawAutumnLeaf(canvas: Canvas, r: Float, variant: Int) {
        val leafColor = when (variant) {
            0 -> Color.rgb(212, 82, 34)
            1 -> Color.rgb(228, 146, 36)
            else -> Color.rgb(168, 64, 42)
        }
        val paint = fillPaint(leafColor, alpha = 64)
        val vein = strokePaint(Color.rgb(120, 48, 20), r * 0.08f, alpha = 58)
        val path = Path().apply {
            moveTo(0f, -r * 0.85f); quadTo(r * 0.75f, -r * 0.2f, 0f, r * 0.65f); quadTo(-r * 0.75f, -r * 0.2f, 0f, -r * 0.85f); close()
        }
        canvas.drawPath(path, paint)
        canvas.drawLine(0f, -r * 0.65f, 0f, r * 0.9f, vein)
    }

    private fun drawTulip(canvas: Canvas, r: Float) {
        val stem = strokePaint(Color.rgb(46, 138, 68), r * 0.13f, alpha = 128)
        val petal = fillPaint(Color.rgb(232, 74, 122), alpha = 126)
        canvas.drawLine(0f, 0f, 0f, r * 0.85f, stem)
        val cup = Path().apply {
            moveTo(-r * 0.45f, -r * 0.55f); lineTo(-r * 0.15f, -r * 0.2f); lineTo(0f, -r * 0.68f)
            lineTo(r * 0.15f, -r * 0.2f); lineTo(r * 0.45f, -r * 0.55f)
            quadTo(r * 0.4f, r * 0.18f, 0f, r * 0.18f); quadTo(-r * 0.4f, r * 0.18f, -r * 0.45f, -r * 0.55f); close()
        }
        canvas.drawPath(cup, petal)
    }

    private fun drawSunflower(canvas: Canvas, r: Float) {
        val petal = fillPaint(Color.rgb(242, 182, 28), alpha = 132)
        val center = fillPaint(Color.rgb(110, 66, 30), alpha = 138)
        for (i in 0 until 8) {
            val angle = i * 45.0 * PI / 180.0
            canvas.drawCircle((cos(angle) * r * 0.52f).toFloat(), (sin(angle) * r * 0.52f).toFloat(), r * 0.26f, petal)
        }
        canvas.drawCircle(0f, 0f, r * 0.34f, center)
    }

    private fun drawDandelion(canvas: Canvas, r: Float) {
        val puff = strokePaint(Color.rgb(74, 122, 90), r * 0.08f, alpha = 116)
        val core = fillPaint(Color.rgb(252, 250, 236), alpha = 148)
        for (i in 0 until 10) {
            val angle = i * 36.0 * PI / 180.0
            val ex = (cos(angle) * r * 0.78f).toFloat()
            val ey = (sin(angle) * r * 0.78f).toFloat()
            canvas.drawLine(0f, 0f, ex, ey, puff)
            canvas.drawCircle(ex, ey, r * 0.11f, core)
        }
        canvas.drawCircle(0f, 0f, r * 0.16f, puff)
    }

    private fun drawEasterEgg(canvas: Canvas, r: Float, variant: Int, rng: Random) {
        val baseColor = when (variant) {
            0 -> Color.rgb(236, 122, 168)
            1 -> Color.rgb(108, 192, 224)
            else -> Color.rgb(242, 196, 86)
        }
        val dotColor = if (rng.nextBoolean()) Color.rgb(148, 102, 214) else Color.rgb(92, 186, 138)
        val egg = fillPaint(baseColor, alpha = 132)
        val accent = fillPaint(dotColor, alpha = 142)
        canvas.drawOval(RectF(-r * 0.52f, -r * 0.72f, r * 0.52f, r * 0.72f), egg)
        canvas.drawCircle(-r * 0.18f, -r * 0.2f, r * 0.11f, accent)
        canvas.drawCircle(r * 0.18f, 0f, r * 0.11f, accent)
        canvas.drawCircle(-r * 0.12f, r * 0.25f, r * 0.11f, accent)
    }

    private fun drawFirework(canvas: Canvas, r: Float, variant: Int) {
        val burstColor = when (variant) {
            0 -> Color.rgb(255, 206, 72)
            1 -> Color.rgb(98, 214, 255)
            else -> Color.rgb(255, 116, 178)
        }
        val ray = strokePaint(burstColor, r * 0.1f, alpha = 72)
        val spark = fillPaint(burstColor, alpha = 88)
        for (i in 0 until 10) {
            val angle = i * 36.0 * PI / 180.0
            val sx = (cos(angle) * r * 0.22f).toFloat()
            val sy = (sin(angle) * r * 0.22f).toFloat()
            val ex = (cos(angle) * r * 0.85f).toFloat()
            val ey = (sin(angle) * r * 0.85f).toFloat()
            canvas.drawLine(sx, sy, ex, ey, ray)
            canvas.drawCircle(ex, ey, r * 0.08f, spark)
        }
    }

    private fun drawGamingController(canvas: Canvas, r: Float, accentColorInt: Int) {
        val bodyStroke = strokePaint(Color.rgb(92, 106, 138), r * 0.12f, alpha = 66)
        val ledGlow = strokePaint(accentColorInt, r * 0.16f, alpha = 28)
        val ledCore = strokePaint(accentColorInt, r * 0.07f, alpha = 86)
        val stickFill = fillPaint(accentColorInt, alpha = 54)
        val bodyPath = Path().apply {
            moveTo(-r * 0.65f, -r * 0.35f); lineTo(r * 0.65f, -r * 0.35f)
            lineTo(r * 0.88f, r * 0.35f); lineTo(r * 0.45f, r * 0.48f)
            lineTo(r * 0.25f, r * 0.16f); lineTo(-r * 0.25f, r * 0.16f)
            lineTo(-r * 0.45f, r * 0.48f); lineTo(-r * 0.88f, r * 0.35f); close()
        }
        canvas.drawPath(bodyPath, bodyStroke)
        canvas.drawLine(-r * 0.28f, -r * 0.25f, r * 0.28f, -r * 0.25f, ledGlow)
        canvas.drawLine(-r * 0.28f, -r * 0.25f, r * 0.28f, -r * 0.25f, ledCore)
        canvas.drawCircle(-r * 0.36f, -r * 0.02f, r * 0.12f, stickFill)
        canvas.drawCircle(r * 0.36f, -r * 0.02f, r * 0.12f, stickFill)
    }

    private fun drawGamingDPad(canvas: Canvas, r: Float, accentColorInt: Int) {
        val baseStroke = strokePaint(Color.rgb(92, 106, 138), r * 0.11f, alpha = 68)
        val ledCore = strokePaint(accentColorInt, r * 0.07f, alpha = 78)
        val arm = r * 0.62f
        val thick = r * 0.22f
        val dpad = Path().apply {
            moveTo(-thick, -arm); lineTo(thick, -arm); lineTo(thick, -thick); lineTo(arm, -thick)
            lineTo(arm, thick); lineTo(thick, thick); lineTo(thick, arm); lineTo(-thick, arm)
            lineTo(-thick, thick); lineTo(-arm, thick); lineTo(-arm, -thick); lineTo(-thick, -thick); close()
        }
        canvas.drawPath(dpad, baseStroke)
        canvas.drawCircle(0f, 0f, r * 0.14f, ledCore)
    }

    private fun drawGamingActionCluster(canvas: Canvas, r: Float, accentColorInt: Int) {
        val strokeW = r * 0.08f
        val triStroke = strokePaint(Color.rgb(78, 224, 178), strokeW, alpha = 76)
        val cirStroke = strokePaint(Color.rgb(255, 104, 136), strokeW, alpha = 76)
        val crossStroke = strokePaint(accentColorInt, strokeW, alpha = 82)
        val sqStroke = strokePaint(Color.rgb(220, 146, 255), strokeW, alpha = 76)
        val d = r * 0.42f
        val s = r * 0.17f
        val tri = Path().apply { moveTo(0f, -d - s); lineTo(s, -d + s * 0.85f); lineTo(-s, -d + s * 0.85f); close() }
        canvas.drawPath(tri, triStroke)
        canvas.drawCircle(d, 0f, s * 0.9f, cirStroke)
        canvas.drawLine(-s * 0.8f, d - s * 0.8f, s * 0.8f, d + s * 0.8f, crossStroke)
        canvas.drawLine(s * 0.8f, d - s * 0.8f, -s * 0.8f, d + s * 0.8f, crossStroke)
        canvas.drawRect(-d - s * 0.85f, -s * 0.85f, -d + s * 0.85f, s * 0.85f, sqStroke)
    }

    private fun drawGamingLedTrace(canvas: Canvas, r: Float, accentColorInt: Int) {
        val glow = strokePaint(accentColorInt, r * 0.18f, alpha = 24)
        val core = strokePaint(accentColorInt, r * 0.06f, alpha = 80)
        val node = fillPaint(accentColorInt, alpha = 88)
        val trace = Path().apply {
            moveTo(-r * 0.78f, r * 0.25f); lineTo(-r * 0.22f, r * 0.25f)
            lineTo(r * 0.18f, -r * 0.25f); lineTo(r * 0.78f, -r * 0.25f)
        }
        canvas.drawPath(trace, glow)
        canvas.drawPath(trace, core)
        canvas.drawCircle(-r * 0.78f, r * 0.25f, r * 0.09f, node)
        canvas.drawCircle(r * 0.78f, -r * 0.25f, r * 0.09f, node)
    }

    private fun drawEightBitPaw(canvas: Canvas, r: Float, variant: EightBitVariant) {
        val mainColor = if (variant == EightBitVariant.PASTEL_BLUE) Color.rgb(122, 182, 246) else Color.rgb(245, 146, 182)
        val highlightColor = if (variant == EightBitVariant.PASTEL_BLUE) Color.rgb(180, 214, 252) else Color.rgb(252, 194, 216)
        val pixelPaint = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL; color = mainColor; alpha = 112 }
        val innerPaint = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL; color = highlightColor; alpha = 128 }
        val p = (r * 0.14f).coerceAtLeast(1.5f)
        val grid = arrayOf(
            "..11...11..",
            ".1221.1221.",
            ".1111.1111.",
            "11.......11",
            "121.111.121",
            "11112221111",
            "..1222221..",
            "..1111111..",
            "...11111..."
        )
        drawPixelGrid(canvas, grid, p, pixelPaint, innerPaint)
    }

    private fun drawEightBitHeartOrStar(canvas: Canvas, r: Float, variant: EightBitVariant, rng: Random) {
        val primaryColor = if (variant == EightBitVariant.PASTEL_BLUE) {
            if (rng.nextBoolean()) Color.rgb(100, 168, 242) else Color.rgb(124, 198, 232)
        } else {
            if (rng.nextBoolean()) Color.rgb(240, 128, 174) else Color.rgb(212, 148, 232)
        }
        val pixelPaint = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL; color = primaryColor; alpha = 104 }
        val innerPaint = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL; color = Color.WHITE; alpha = 90 }
        val p = (r * 0.14f).coerceAtLeast(1.5f)
        val grid = if (rng.nextBoolean()) {
            arrayOf(".11.11.", "1211111", "1111111", ".11111.", "..111..", "...1...")
        } else {
            arrayOf("...1...", "..121..", "1112111", "..111..", "...1...")
        }
        drawPixelGrid(canvas, grid, p, pixelPaint, innerPaint)
    }

    private fun drawEightBitCatMotif(canvas: Canvas, r: Float, variant: Int) {
        val primary = pixelPaint(Color.rgb(214, 51, 116), alpha = 118)
        val highlight = pixelPaint(Color.rgb(255, 162, 198), alpha = 148)
        val accent = pixelPaint(Color.rgb(82, 27, 53), alpha = 128)
        val p = (r * 0.135f).coerceAtLeast(1.5f)
        val grid = when (variant) {
            0 -> arrayOf(
                ".1.....1.",
                "121...121",
                "111111111",
                "113111311",
                "111121111",
                ".1113111.",
                "..11111.."
            )
            1 -> arrayOf(
                "..11...11..",
                ".1221.1221.",
                ".1111.1111.",
                "11.......11",
                "121.111.121",
                "11112221111",
                "..1222221..",
                "..1111111.."
            )
            else -> arrayOf(
                "..1111...1.",
                ".121111.11.",
                "1311111111.",
                ".111111.11.",
                "..1111...1."
            )
        }
        drawPixelGrid(canvas, grid, p, primary, highlight, accent)
    }

    private fun drawEightBitFoxMotif(canvas: Canvas, r: Float, variant: Int) {
        val primary = pixelPaint(Color.rgb(255, 255, 255), alpha = 195)
        val highlight = pixelPaint(Color.rgb(91, 142, 201), alpha = 128)
        val accent = pixelPaint(Color.rgb(217, 93, 36), alpha = 152)
        val p = (r * 0.135f).coerceAtLeast(1.5f)
        val grid = when (variant) {
            0 -> arrayOf(
                "31.....13",
                "321...123",
                "111111111",
                "112111211",
                "111111111",
                ".1113111.",
                "..11111.."
            )
            1 -> arrayOf(
                "....111.",
                "..111121",
                ".111111.",
                "311111..",
                "33111...",
                ".331...."
            )
            else -> arrayOf(
                "...2...",
                "..212..",
                "2113112",
                "..212..",
                "...2..."
            )
        }
        drawPixelGrid(canvas, grid, p, primary, highlight, accent)
    }

    private fun drawEightBitBunnyMotif(canvas: Canvas, r: Float, variant: Int) {
        val primary = pixelPaint(Color.rgb(156, 84, 39), alpha = 120)
        val highlight = pixelPaint(Color.rgb(232, 186, 144), alpha = 148)
        val accent = pixelPaint(Color.rgb(224, 123, 57), alpha = 145)
        val p = (r * 0.135f).coerceAtLeast(1.5f)
        val grid = when (variant) {
            0 -> arrayOf(
                ".11...11.",
                ".12...21.",
                ".12...21.",
                ".1111111.",
                "113111311",
                "111131111",
                ".1111111."
            )
            1 -> arrayOf(
                "....22.",
                "...222.",
                "..333..",
                ".3333..",
                ".333...",
                "33....."
            )
            else -> arrayOf(
                ".11.11.",
                "1211121",
                ".11311.",
                "1211121",
                ".11.11.",
                "...3..."
            )
        }
        drawPixelGrid(canvas, grid, p, primary, highlight, accent)
    }

    private fun drawEightBitSnakeMotif(canvas: Canvas, r: Float, variant: Int) {
        val primary = pixelPaint(Color.rgb(31, 122, 76), alpha = 122)
        val highlight = pixelPaint(Color.rgb(112, 204, 148), alpha = 148)
        val accent = pixelPaint(Color.rgb(214, 72, 92), alpha = 138)
        val p = (r * 0.135f).coerceAtLeast(1.5f)
        val grid = when (variant) {
            0 -> arrayOf(
                "..11111..",
                ".1111111.",
                "112111211",
                "111111111",
                ".1111111.",
                "...3.3...",
                "..3...3.."
            )
            1 -> arrayOf(
                ".11111...",
                "1222211..",
                "11...111.",
                "..111121.",
                ".111111..",
                "12111...."
            )
            else -> arrayOf(
                "....111.",
                "..11221.",
                ".112111.",
                "111111..",
                "1111....",
                "1......."
            )
        }
        drawPixelGrid(canvas, grid, p, primary, highlight, accent)
    }

    private fun pixelPaint(color: Int, alpha: Int): Paint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
        this.color = color
        this.alpha = alpha
    }

    private fun drawStaticBubblesScene(canvas: Canvas, width: Int, height: Int) {
        val w = width.toFloat()
        val h = height.toFloat()
        val colors = intArrayOf(
            Color.argb(90, 106, 226, 255),
            Color.argb(85, 179, 136, 255),
            Color.argb(75, 128, 203, 196)
        )
        val specs = listOf(
            Triple(0.24f, 0.30f, 0.14f),
            Triple(0.68f, 0.25f, 0.11f),
            Triple(0.48f, 0.56f, 0.16f),
            Triple(0.78f, 0.70f, 0.12f),
            Triple(0.20f, 0.76f, 0.10f)
        )
        val minDim = minOf(w, h)
        val rim = strokePaint(Color.WHITE, minDim * 0.008f, alpha = 95)
        specs.forEachIndexed { idx, (nx, ny, nr) ->
            val p = fillPaint(colors[idx % colors.size], alpha = 150)
            canvas.drawCircle(w * nx, h * ny, minDim * nr, p)
            canvas.drawCircle(w * nx, h * ny, minDim * nr, rim)
        }
    }

    private fun drawStaticMiniCityScene(canvas: Canvas, width: Int, height: Int) {
        val w = width.toFloat()
        val h = height.toFloat()
        val roadPaint = strokePaint(Color.rgb(198, 209, 198), minOf(w, h) * 0.11f, alpha = 255)
        val dashPaint = strokePaint(Color.WHITE, minOf(w, h) * 0.014f, alpha = 210)
        canvas.drawLine(-w * 0.1f, h * 0.34f, w * 1.1f, h * 0.18f, roadPaint)
        canvas.drawLine(-w * 0.1f, h * 0.76f, w * 1.1f, h * 0.62f, roadPaint)
        canvas.drawLine(w * 0.22f, -h * 0.1f, w * 0.58f, h * 1.1f, roadPaint)
        canvas.drawLine(-w * 0.1f, h * 0.34f, w * 1.1f, h * 0.18f, dashPaint)
        canvas.drawLine(-w * 0.1f, h * 0.76f, w * 1.1f, h * 0.62f, dashPaint)

        val car1 = fillPaint(Color.rgb(93, 156, 236), alpha = 255)
        val car2 = fillPaint(Color.rgb(232, 111, 104), alpha = 255)
        val car3 = fillPaint(Color.rgb(88, 185, 141), alpha = 255)
        val s = minOf(w, h) * 0.075f
        canvas.drawRoundRect(RectF(w * 0.28f - s, h * 0.27f - s * 0.55f, w * 0.28f + s, h * 0.27f + s * 0.55f), s * 0.3f, s * 0.3f, car1)
        canvas.drawRoundRect(RectF(w * 0.68f - s, h * 0.66f - s * 0.55f, w * 0.68f + s, h * 0.66f + s * 0.55f), s * 0.3f, s * 0.3f, car2)
        canvas.drawRoundRect(RectF(w * 0.42f - s * 0.7f, h * 0.50f - s * 0.55f, w * 0.42f + s * 0.7f, h * 0.50f + s * 0.55f), s * 0.3f, s * 0.3f, car3)
    }

    private fun drawStaticWeatherScene(canvas: Canvas, width: Int, height: Int) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawColor(Color.rgb(176, 222, 253))
        val sun = fillPaint(Color.rgb(253, 224, 71), alpha = 255)
        val minDim = minOf(w, h)
        canvas.drawCircle(w * 0.32f, h * 0.30f, minDim * 0.11f, sun)

        val cloud = fillPaint(Color.WHITE, alpha = 235)
        canvas.drawCircle(w * 0.64f, h * 0.36f, minDim * 0.09f, cloud)
        canvas.drawCircle(w * 0.56f, h * 0.39f, minDim * 0.07f, cloud)
        canvas.drawCircle(w * 0.72f, h * 0.39f, minDim * 0.065f, cloud)

        val hill = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.78f)
            quadTo(w * 0.45f, h * 0.68f, w, h * 0.80f)
            lineTo(w, h)
            close()
        }
        canvas.drawPath(hill, fillPaint(Color.rgb(129, 199, 132), alpha = 255))
    }

    private fun drawLightThemeGradientIfNeeded(
        canvas: Canvas,
        presetId: String,
        width: Int,
        height: Int
    ) {
        val stops: IntArray = when (presetId) {
            "winter" -> intArrayOf(
                Color.rgb(191, 224, 249),
                Color.rgb(210, 231, 248),
                Color.rgb(196, 223, 245)
            )
            "summer" -> intArrayOf(
                Color.rgb(158, 219, 240),
                Color.rgb(188, 230, 244),
                Color.rgb(210, 238, 242)
            )
            "spring" -> intArrayOf(
                Color.rgb(179, 222, 192),
                Color.rgb(200, 232, 207),
                Color.rgb(216, 238, 214)
            )
            "easter" -> intArrayOf(
                Color.rgb(212, 184, 236),
                Color.rgb(226, 204, 242),
                Color.rgb(234, 219, 245)
            )
            else -> return
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width * 0.25f,
                height.toFloat(),
                stops,
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun drawPixelGrid(
        canvas: Canvas,
        rows: Array<String>,
        pixelSize: Float,
        primaryPaint: Paint,
        highlightPaint: Paint,
        accentPaint: Paint = primaryPaint
    ) {
        val h = rows.size
        val w = rows.maxOfOrNull { it.length } ?: 0
        val startX = -w * pixelSize / 2f
        val startY = -h * pixelSize / 2f
        for (rowIdx in rows.indices) {
            val row = rows[rowIdx]
            for (colIdx in row.indices) {
                val cell = row[colIdx]
                if (cell == '.') continue
                val left = startX + colIdx * pixelSize
                val top = startY + rowIdx * pixelSize
                val paint = when (cell) {
                    '2' -> highlightPaint
                    '3' -> accentPaint
                    else -> primaryPaint
                }
                canvas.drawRect(left, top, left + pixelSize + 0.35f, top + pixelSize + 0.35f, paint)
            }
        }
    }

    private companion object {
        const val CACHE_VERSION = "v7"
        const val FULL_WIDTH = 900
        const val FULL_HEIGHT = 1950
        const val THUMB_WIDTH = 320
        const val THUMB_HEIGHT = 200
    }
}
