package dev.melt.internal

import dev.melt.MeltStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DripGeometryTest {

    private val density = 2f
    private val width = 600f
    private val height = 120f

    private fun compute(
        progress: Float,
        style: MeltStyle = MeltStyle.Default,
        seed: Float = 0f,
        profile: EdgeProfile = EdgeProfile.flat(width, height, density),
    ): List<DripSnapshot> {
        val out = Array(DripGeometry.MAX_DRIPS) { Drip() }
        val n = DripGeometry.compute(out, profile, height, height / 2f, 1f, density, progress, style, seed)
        return out.take(n).map(::DripSnapshot)
    }

    @Test
    fun `no drips before the melt starts`() {
        assertEquals(0, compute(0f).size)
    }

    @Test
    fun `fully melted content has drips, never more than requested`() {
        for (style in listOf(MeltStyle.Drip, MeltStyle.Wax, MeltStyle.Goo, MeltStyle.Slump, MeltStyle.Wavy)) {
            val drips = compute(1f, style)
            assertTrue("$style produced no drips", drips.isNotEmpty())
            assertTrue("$style produced too many drips", drips.size <= style.dripCount)
        }
    }

    @Test
    fun `drips hang below the edge and stay inside the content horizontally`() {
        val profile = EdgeProfile.flat(width, height, density)
        for (d in compute(1f, profile = profile)) {
            assertTrue(d.headY >= d.edge)
            assertTrue(d.head > 0f && d.neck > 0f && d.neck <= d.head)
            assertTrue(d.cx >= profile.minX && d.cx <= profile.maxX + 1)
        }
    }

    @Test
    fun `drips grow over time`() {
        val early = compute(0.3f, MeltStyle.Wax).associateBy { it.cx }
        val late = compute(0.9f, MeltStyle.Wax).associateBy { it.cx }
        val common = early.keys intersect late.keys
        assertTrue(common.isNotEmpty())
        for (cx in common) assertTrue(late.getValue(cx).headY >= early.getValue(cx).headY)
    }

    @Test
    fun `same seed gives the same pattern, different seeds differ`() {
        assertEquals(compute(0.8f, seed = 3f), compute(0.8f, seed = 3f))
        assertNotEquals(compute(0.8f, seed = 3f), compute(0.8f, seed = 11f))
    }

    @Test
    fun `no falling drops when droplets are off`() {
        val style = MeltStyle.Goo.copy(droplets = false)
        for (p in listOf(0.5f, 0.8f, 1f)) {
            assertTrue(compute(p, style).all { it.dropR == 0f })
        }
    }

    @Test
    fun `empty outline gives no drips`() {
        val empty = EdgeProfile(null, FloatArray(width.toInt()) { Float.NaN }, 0, width.toInt() - 1)
        assertEquals(0, compute(1f, profile = empty).size)
    }

    @Test
    fun `tiny content gets a single drip at most`() {
        val tiny = EdgeProfile.flat(40f, 40f, density)
        assertTrue(compute(1f, MeltStyle.Goo, profile = tiny).size <= 1)
    }

    @Test
    fun `smoothstep is clamped and monotonic`() {
        assertEquals(0f, DripGeometry.smoothstep(0.2f, 0.8f, 0f), 0f)
        assertEquals(1f, DripGeometry.smoothstep(0.2f, 0.8f, 1f), 0f)
        assertEquals(0.5f, DripGeometry.smoothstep(0f, 1f, 0.5f), 1e-6f)
        var previous = 0f
        for (i in 0..100) {
            val v = DripGeometry.smoothstep(0f, 1f, i / 100f)
            assertTrue(v >= previous)
            previous = v
        }
    }

    private data class DripSnapshot(
        val cx: Float, val edge: Float, val neck: Float, val head: Float,
        val headY: Float, val dropR: Float,
    ) {
        constructor(d: Drip) : this(d.cx, d.edge, d.neck, d.head, d.headY, d.dropR)
    }
}
