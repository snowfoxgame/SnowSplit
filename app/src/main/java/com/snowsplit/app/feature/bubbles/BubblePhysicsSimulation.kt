package com.snowsplit.app.feature.bubbles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import com.snowsplit.app.domain.model.PersonBubble
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal data class BubbleNodeState(
    val personId: Long,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var anchorX: Float,
    var anchorY: Float,
    val phaseX: Float,
    val phaseY: Float,
    val freqX: Float,
    val freqY: Float,
    var isDragged: Boolean = false
)

/**
 * Decoupled 120Hz physics state for contact bubbles floating softly around the center "Me" bubble.
 * Reads of [frameVersion] and node coordinates happen inside draw/placement lambdas so Compose
 * executes zero recompositions per animation frame.
 */
class BubblePhysicsSimulation {

    var frameVersion: Long by mutableLongStateOf(0L)
        private set

    var centerX: Float = 0f
        private set

    var centerY: Float = 0f
        private set

    var meRadiusPx: Float = 0f
        private set

    var contactRadiusPx: Float = 0f
        private set

    private var viewportWidthPx: Float = 0f
    private var viewportHeightPx: Float = 0f
    private var topSafePx: Float = 0f
    private var bottomSafePx: Float = 0f
    private var sideMarginPx: Float = 0f
    private var lineClearancePx: Float = 0f
    private var contactSpacingPx: Float = 0f
    private var bobAmplitudePx: Float = 0f

    private val nodes = mutableListOf<BubbleNodeState>()
    private val nodesById = HashMap<Long, BubbleNodeState>()
    private var orderedPeopleIds: List<Long> = emptyList()

    fun updateLayoutMetrics(
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        topSafePx: Float,
        bottomSafePx: Float,
        sideMarginPx: Float,
        meRadiusPx: Float,
        contactRadiusPx: Float,
        lineClearancePx: Float,
        contactSpacingPx: Float,
        bobAmplitudePx: Float
    ) {
        if (viewportWidthPx <= 0f || viewportHeightPx <= 0f) return
        val changed = this.viewportWidthPx != viewportWidthPx ||
            this.viewportHeightPx != viewportHeightPx ||
            this.topSafePx != topSafePx ||
            this.bottomSafePx != bottomSafePx ||
            this.sideMarginPx != sideMarginPx ||
            this.meRadiusPx != meRadiusPx ||
            this.contactRadiusPx != contactRadiusPx ||
            this.lineClearancePx != lineClearancePx ||
            this.contactSpacingPx != contactSpacingPx ||
            this.bobAmplitudePx != bobAmplitudePx

        if (!changed) return

        this.viewportWidthPx = viewportWidthPx
        this.viewportHeightPx = viewportHeightPx
        this.topSafePx = topSafePx
        this.bottomSafePx = bottomSafePx
        this.sideMarginPx = sideMarginPx
        this.meRadiusPx = meRadiusPx
        this.contactRadiusPx = contactRadiusPx
        this.lineClearancePx = lineClearancePx
        this.contactSpacingPx = contactSpacingPx
        this.bobAmplitudePx = bobAmplitudePx

        recomputeCenter()
        recomputeAnchors(snapNewOrUnpositioned = false)
        clampAllNodes()
        frameVersion++
    }

    fun syncPeople(people: List<PersonBubble>) {
        val nextIds = people.map { it.id }
        if (nextIds == orderedPeopleIds && nodes.size == people.size) return

        orderedPeopleIds = nextIds
        val existing = HashMap(nodesById)
        nodes.clear()
        nodesById.clear()

        people.forEachIndexed { index, person ->
            val prev = existing[person.id]
            val seed = (person.id * 2654435761L).toInt()
            val phaseX = ((seed and 0xFF) / 255f) * TWO_PI
            val phaseY = (((seed ushr 8) and 0xFF) / 255f) * TWO_PI
            val freqX = 0.44f + ((seed ushr 16) and 0x1F) / 31f * 0.24f
            val freqY = 0.39f + ((seed ushr 21) and 0x1F) / 31f * 0.22f

            val node = prev ?: BubbleNodeState(
                personId = person.id,
                x = centerX,
                y = centerY,
                anchorX = centerX,
                anchorY = centerY,
                phaseX = phaseX,
                phaseY = phaseY,
                freqX = freqX,
                freqY = freqY
            )
            nodes.add(node)
            nodesById[person.id] = node
            if (prev == null && index >= 0 && viewportWidthPx > 0f && viewportHeightPx > 0f) {
                val (ax, ay) = computeAnchorForSlot(index, people.size)
                node.anchorX = ax
                node.anchorY = ay
                node.x = ax
                node.y = ay
            }
        }

        recomputeAnchors(snapNewOrUnpositioned = false)
        clampAllNodes()
        frameVersion++
    }

    fun step(dtSeconds: Float, totalTimeSeconds: Float) {
        if (nodes.isEmpty() || viewportWidthPx <= 0f || viewportHeightPx <= 0f) return

        val dt = dtSeconds.coerceIn(0.001f, 0.033f)
        val springK = 5.4f
        val dampingRate = 3.8f
        val dampingFactor = exp(-dampingRate * dt)

        for (i in 0 until nodes.size) {
            val node = nodes[i]
            if (node.isDragged) continue

            val bobX = sin(totalTimeSeconds * node.freqX + node.phaseX) * bobAmplitudePx
            val bobY = cos(totalTimeSeconds * node.freqY + node.phaseY) * bobAmplitudePx
            val targetX = node.anchorX + bobX
            val targetY = node.anchorY + bobY

            val ax = (targetX - node.x) * springK
            val ay = (targetY - node.y) * springK

            node.vx = (node.vx + ax * dt) * dampingFactor
            node.vy = (node.vy + ay * dt) * dampingFactor
            node.x += node.vx * dt
            node.y += node.vy * dt
        }

        // Two gentle relaxation passes guarantee zero overlap between bubbles.
        repeat(2) {
            resolveCollisions()
            clampAllNodes()
        }

        frameVersion++
    }

    fun beginDrag(personId: Long) {
        val node = nodesById[personId] ?: return
        node.isDragged = true
        node.vx = 0f
        node.vy = 0f
    }

    fun dragBy(personId: Long, dx: Float, dy: Float, dtSeconds: Float) {
        val node = nodesById[personId] ?: return
        node.x += dx
        node.y += dy
        clampNode(node)

        val safeDt = dtSeconds.coerceIn(0.004f, 0.05f)
        val instVx = (dx / safeDt).coerceIn(-MAX_THROW_SPEED_PX, MAX_THROW_SPEED_PX)
        val instVy = (dy / safeDt).coerceIn(-MAX_THROW_SPEED_PX, MAX_THROW_SPEED_PX)
        node.vx = node.vx * 0.6f + instVx * 0.4f
        node.vy = node.vy * 0.6f + instVy * 0.4f
        frameVersion++
    }

    fun endDrag(personId: Long) {
        val node = nodesById[personId] ?: return
        node.isDragged = false
    }

    fun positionX(personId: Long): Float = nodesById[personId]?.x ?: centerX

    fun positionY(personId: Long): Float = nodesById[personId]?.y ?: centerY

    fun isDragged(personId: Long): Boolean = nodesById[personId]?.isDragged ?: false

    private fun recomputeCenter() {
        val safeTop = topSafePx.coerceIn(0f, viewportHeightPx * 0.45f)
        val safeBottom = (viewportHeightPx - bottomSafePx).coerceIn(safeTop + 1f, viewportHeightPx)
        centerX = viewportWidthPx * 0.5f
        centerY = (safeTop + safeBottom) * 0.5f
    }

    private fun recomputeAnchors(snapNewOrUnpositioned: Boolean) {
        val count = nodes.size
        if (count == 0 || viewportWidthPx <= 0f || viewportHeightPx <= 0f) return
        for (i in 0 until count) {
            val node = nodes[i]
            val (ax, ay) = computeAnchorForSlot(i, count)
            val wasUninitialized = node.x == 0f && node.y == 0f
            node.anchorX = ax
            node.anchorY = ay
            if (snapNewOrUnpositioned || wasUninitialized) {
                node.x = ax
                node.y = ay
            }
        }
    }

    private fun computeAnchorForSlot(index: Int, totalCount: Int): Pair<Float, Float> {
        if (totalCount <= 0) return centerX to centerY
        val playHalfW = max(contactRadiusPx + 8f, centerX - sideMarginPx - contactRadiusPx)
        val safeBottomY = max(topSafePx + contactRadiusPx * 2f, viewportHeightPx - bottomSafePx)
        val playHalfH = max(
            contactRadiusPx + 8f,
            min(centerY - topSafePx, safeBottomY - centerY) - contactRadiusPx
        )

        val minCenterClearance = meRadiusPx + contactRadiusPx + lineClearancePx
        val ringCapacity = 6
        val ringIndex = index / ringCapacity
        val indexInRing = index % ringCapacity
        val itemsInThisRing = min(ringCapacity, totalCount - ringIndex * ringCapacity).coerceAtLeast(1)

        val angleOffset = if (ringIndex % 2 == 1) (PI.toFloat() / itemsInThisRing) else (-PI.toFloat() / 2f)
        val angle = angleOffset + (TWO_PI * indexInRing / itemsInThisRing)

        val preferredRx = max(minCenterClearance, playHalfW * if (ringIndex == 0) 0.72f else 0.94f)
        val preferredRy = max(minCenterClearance, playHalfH * if (ringIndex == 0) 0.74f else 0.95f)
        val rx = min(preferredRx, playHalfW)
        val ry = min(preferredRy, playHalfH)

        val rawX = centerX + cos(angle) * rx
        val rawY = centerY + sin(angle) * ry
        return clampCoords(rawX, rawY)
    }

    private fun resolveCollisions() {
        val minMeDist = meRadiusPx + contactRadiusPx + lineClearancePx * 0.72f
        val minPairDist = contactRadiusPx * 2f + contactSpacingPx

        for (i in 0 until nodes.size) {
            val a = nodes[i]

            // Repel softly from center "Me" bubble to preserve space for lines and labels.
            val mdx = a.x - centerX
            val mdy = a.y - centerY
            val mDist = hypot(mdx, mdy)
            if (mDist < minMeDist) {
                val nx = if (mDist > 0.001f) mdx / mDist else cos(i * 1.1f)
                val ny = if (mDist > 0.001f) mdy / mDist else sin(i * 1.1f)
                val push = (minMeDist - mDist) * 0.55f
                if (!a.isDragged) {
                    a.x += nx * push
                    a.y += ny * push
                    val inwardVel = a.vx * nx + a.vy * ny
                    if (inwardVel < 0f) {
                        a.vx -= nx * inwardVel * 0.7f
                        a.vy -= ny * inwardVel * 0.7f
                    }
                }
            }

            for (j in i + 1 until nodes.size) {
                val b = nodes[j]
                val dx = b.x - a.x
                val dy = b.y - a.y
                val dist = hypot(dx, dy)
                if (dist < minPairDist) {
                    val nx = if (dist > 0.001f) dx / dist else cos((i + j) * 0.9f)
                    val ny = if (dist > 0.001f) dy / dist else sin((i + j) * 0.9f)
                    val overlap = minPairDist - dist
                    when {
                        a.isDragged && !b.isDragged -> {
                            b.x += nx * overlap
                            b.y += ny * overlap
                        }
                        !a.isDragged && b.isDragged -> {
                            a.x -= nx * overlap
                            a.y -= ny * overlap
                        }
                        !a.isDragged && !b.isDragged -> {
                            val half = overlap * 0.5f
                            a.x -= nx * half
                            a.y -= ny * half
                            b.x += nx * half
                            b.y += ny * half
                        }
                    }
                }
            }
        }
    }

    private fun clampAllNodes() {
        for (i in 0 until nodes.size) {
            clampNode(nodes[i])
        }
    }

    private fun clampNode(node: BubbleNodeState) {
        val minX = sideMarginPx + contactRadiusPx
        val maxX = max(minX, viewportWidthPx - sideMarginPx - contactRadiusPx)
        val minY = topSafePx + contactRadiusPx
        val maxY = max(minY, viewportHeightPx - bottomSafePx - contactRadiusPx)

        if (node.x < minX) {
            node.x = minX
            if (node.vx < 0f) node.vx *= -0.2f
        } else if (node.x > maxX) {
            node.x = maxX
            if (node.vx > 0f) node.vx *= -0.2f
        }

        if (node.y < minY) {
            node.y = minY
            if (node.vy < 0f) node.vy *= -0.2f
        } else if (node.y > maxY) {
            node.y = maxY
            if (node.vy > 0f) node.vy *= -0.2f
        }
    }

    private fun clampCoords(x: Float, y: Float): Pair<Float, Float> {
        val minX = sideMarginPx + contactRadiusPx
        val maxX = max(minX, viewportWidthPx - sideMarginPx - contactRadiusPx)
        val minY = topSafePx + contactRadiusPx
        val maxY = max(minY, viewportHeightPx - bottomSafePx - contactRadiusPx)
        return x.coerceIn(minX, maxX) to y.coerceIn(minY, maxY)
    }

    companion object {
        private const val TWO_PI = (PI * 2.0).toFloat()
        private const val MAX_THROW_SPEED_PX = 1400f
    }
}
