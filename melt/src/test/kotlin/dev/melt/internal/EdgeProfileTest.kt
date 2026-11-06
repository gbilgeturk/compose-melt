package dev.melt.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeProfileTest {

    @Test
    fun `flat fallback keeps away from the corners`() {
        val density = 2f
        val profile = EdgeProfile.flat(w = 400f, h = 100f, density = density)
        assertTrue(profile.edgeAt(0f).isNaN())
        assertTrue(profile.edgeAt(399f).isNaN())
        assertEquals(100f - EdgeProfile.FLAT_INSET_DP * density, profile.edgeAt(200f), 0f)
        assertTrue(profile.minX > 0 && profile.maxX < 399 && profile.minX <= profile.maxX)
    }

    @Test
    fun `out of range columns have no edge`() {
        val profile = EdgeProfile.flat(w = 100f, h = 50f, density = 1f)
        assertTrue(profile.edgeAt(-5f).isNaN())
        assertTrue(profile.edgeAt(1000f).isNaN())
    }

    @Test
    fun `flat fallback survives degenerate sizes`() {
        val profile = EdgeProfile.flat(w = 0.5f, h = 0.5f, density = 3f)
        assertTrue(profile.minX <= profile.maxX)
    }
}
