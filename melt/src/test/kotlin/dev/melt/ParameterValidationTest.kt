package dev.melt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ParameterValidationTest {

    @Test
    fun `all style presets are valid`() {
        // Constructing the companion triggers every preset's init block.
        listOf(MeltStyle.Drip, MeltStyle.Wax, MeltStyle.Goo, MeltStyle.Slump, MeltStyle.Wavy)
        assertEquals(MeltStyle.Drip, MeltStyle.Default)
    }

    @Test
    fun `style rejects out of range values`() {
        assertThrows(IllegalArgumentException::class.java) { MeltStyle(dripCount = 0) }
        assertThrows(IllegalArgumentException::class.java) { MeltStyle(dripCount = 65) }
        assertThrows(IllegalArgumentException::class.java) { MeltStyle(dripLength = 4.1f) }
        assertThrows(IllegalArgumentException::class.java) { MeltStyle(dripWidth = 0f) }
        assertThrows(IllegalArgumentException::class.java) { MeltStyle(lengthVariance = 1.1f) }
        assertThrows(IllegalArgumentException::class.java) { MeltStyle(bulb = 0.9f) }
        assertThrows(IllegalArgumentException::class.java) { MeltStyle(fillet = -1f) }
        assertThrows(IllegalArgumentException::class.java) { MeltStyle(stagger = 1f) }
        assertThrows(IllegalArgumentException::class.java) { MeltStyle(wobble = -1f) }
        assertThrows(IllegalArgumentException::class.java) { MeltStyle(sag = 0.5f) }
        assertThrows(IllegalArgumentException::class.java) { MeltStyle(gloss = 2f) }
    }

    @Test
    fun `style accepts range boundaries`() {
        MeltStyle(dripCount = 1, dripLength = 0f, dripWidth = 0.05f, lengthVariance = 0f, bulb = 1f, fillet = 0f, stagger = 0f, sag = 0f, gloss = 0f)
        MeltStyle(dripCount = 64, dripLength = 4f, dripWidth = 1f, lengthVariance = 1f, bulb = 2.5f, fillet = 3f, stagger = 0.9f, sag = 0.3f, gloss = 1.5f)
    }

    @Test
    fun `speed presets are ordered and validated`() {
        assertEquals(MeltSpeed.Normal, MeltSpeed.Default)
        val presets = listOf(MeltSpeed.Slow, MeltSpeed.Normal, MeltSpeed.Fast, MeltSpeed.VeryFast)
        assertEquals(presets.sortedByDescending { it.durationMillis }, presets)
        assertThrows(IllegalArgumentException::class.java) { MeltSpeed(0) }
        assertThrows(IllegalArgumentException::class.java) { MeltSpeed(-100) }
    }

    @Test
    fun `config rejects negative delay`() {
        assertEquals(MeltConfig(), MeltConfig.Default)
        assertThrows(IllegalArgumentException::class.java) { MeltConfig(delayMillis = -1) }
    }
}
