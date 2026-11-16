package com.snowsplit.app.feature.bubbles

import com.snowsplit.app.domain.model.PersonBubble
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class BubblePhysicsSimulationTest {

    private fun createConfiguredSimulation(count: Int): BubblePhysicsSimulation {
        val sim = BubblePhysicsSimulation()
        sim.updateLayoutMetrics(
            viewportWidthPx = 1080f,
            viewportHeightPx = 2340f,
            topSafePx = 320f,
            bottomSafePx = 280f,
            sideMarginPx = 24f,
            meRadiusPx = 140f,
            contactRadiusPx = 110f,
            lineClearancePx = 110f,
            contactSpacingPx = 36f,
            bobAmplitudePx = 20f
        )
        val people = (1L..count.toLong()).map { id ->
            PersonBubble(
                id = id,
                name = "Friend $id"
            )
        }
        sim.syncPeople(people)
        return sim
    }

    @Test
    fun `bubbles stay inside safe vertical and horizontal boundaries across simulation steps`() {
        val sim = createConfiguredSimulation(count = 7)
        repeat(240) { frame ->
            sim.step(dtSeconds = 1f / 120f, totalTimeSeconds = frame / 120f)
        }

        val minX = 24f + 110f
        val maxX = 1080f - 24f - 110f
        val minY = 320f + 110f
        val maxY = 2340f - 280f - 110f

        for (id in 1L..7L) {
            val x = sim.positionX(id)
            val y = sim.positionY(id)
            assertTrue("x=$x should be >= $minX", x >= minX - 0.5f)
            assertTrue("x=$x should be <= $maxX", x <= maxX + 0.5f)
            assertTrue("y=$y should be >= $minY", y >= minY - 0.5f)
            assertTrue("y=$y should be <= $maxY", y <= maxY + 0.5f)
        }
    }

    @Test
    fun `contact bubbles never overlap each other or center me bubble`() {
        val sim = createConfiguredSimulation(count = 6)
        repeat(180) { frame ->
            sim.step(dtSeconds = 1f / 60f, totalTimeSeconds = frame / 60f)
        }

        val minMeCenterDist = 140f + 110f
        val minPairCenterDist = 110f * 2f

        for (idA in 1L..6L) {
            val ax = sim.positionX(idA)
            val ay = sim.positionY(idA)
            val distToMe = hypot(ax - sim.centerX, ay - sim.centerY)
            assertTrue("distToMe=$distToMe should keep clear of center bubble", distToMe >= minMeCenterDist - 1f)

            for (idB in (idA + 1)..6L) {
                val bx = sim.positionX(idB)
                val by = sim.positionY(idB)
                val pairDist = hypot(ax - bx, ay - by)
                assertTrue("pairDist($idA,$idB)=$pairDist must avoid overlap", pairDist >= minPairCenterDist - 1f)
            }
        }
    }

    @Test
    fun `dragging a bubble updates position cleanly and increments frameVersion`() {
        val sim = createConfiguredSimulation(count = 3)
        val initialVersion = sim.frameVersion
        val startX = sim.positionX(1L)

        sim.beginDrag(1L)
        assertTrue(sim.isDragged(1L))

        sim.dragBy(personId = 1L, dx = 85f, dy = -40f, dtSeconds = 0.016f)
        assertTrue(sim.frameVersion > initialVersion)
        assertTrue(sim.positionX(1L) != startX)

        sim.endDrag(1L)
        assertTrue(!sim.isDragged(1L))
    }
}
