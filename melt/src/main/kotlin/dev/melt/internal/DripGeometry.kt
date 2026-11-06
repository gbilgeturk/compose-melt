package dev.melt.internal

import dev.melt.MeltStyle
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** One drip in one frame. Mutable and pooled so a frame allocates nothing. */
internal class Drip {
    var cx = 0f      // where the drip leaves the bottom edge
    var edge = 0f    // y of the content's bottom outline at cx (after sag)
    var bx = 0f      // centre of the head (differs from cx when wobbling)
    var neck = 0f    // half-width of the column
    var head = 0f    // radius of the round head
    var fillet = 0f  // size of the flare into the bottom edge
    var headY = 0f   // centre y of the head
    var dropX = 0f   // falling drop (dropR == 0 means none)
    var dropY = 0f
    var dropR = 0f
    var dropAlpha = 0f
}

/**
 * Drip physics. Every quantity is driven by smoothstep curves of the drip's own local time,
 * so shapes are continuous with continuous velocity: nothing pops in, nothing jumps.
 *
 * Life of a drip (local time 0..1, after its stagger delay):
 * 1. gather  – a small bump swells out of the bottom edge;
 * 2. flow    – the drip runs down, slow at first, then faster, then settling, thinning as it stretches;
 * 3. release – (some drips) the head swells, a drop pinches off and falls with gravity while fading,
 *              and the drip springs back a little.
 */
internal object DripGeometry {

    const val MAX_DRIPS = 64

    /**
     * Fills [out] with the drips for [progress] (0..1) and returns how many were written.
     *
     * Drips are spread across the columns the content actually covers ([profile]) and hang from
     * its real bottom outline there. A drip is skipped where the outline curves away steeply
     * under it (a pill's or circle's sides) or where there is no content (gaps between glyphs).
     *
     * @param h Original height of the content (used for proportions).
     * @param split / [sagScale] The body sags below `split` by this vertical scale.
     */
    fun compute(
        out: Array<Drip>,
        profile: EdgeProfile,
        h: Float,
        split: Float,
        sagScale: Float,
        density: Float,
        progress: Float,
        style: MeltStyle,
        seed: Float,
    ): Int {
        val left = profile.minX.toFloat()
        val right = profile.maxX + 1f
        val span = right - left
        if (span < 2f) return 0
        // Small composables (an icon, a FAB) get fewer drips instead of hair-thin ones.
        val n = min(style.dripCount, (span / (12f * density)).toInt()).coerceIn(1, MAX_DRIPS)
        val spacing = span / n
        val tolerance = 1f * density
        val gravity = h * 5f

        var count = 0
        var lastCx = Float.NEGATIVE_INFINITY
        for (i in 0 until n) {
            val delayR = random(i, 1, seed)
            val widthR = random(i, 2, seed)
            val lengthR = random(i, 3, seed)
            val offsetR = random(i, 4, seed)
            val dropR = random(i, 5, seed)

            val delay = style.stagger * delayR
            val t = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
            if (t <= 0f) continue

            val wantedRadius = spacing * style.dripWidth * 0.5f * (0.75f + 0.5f * widthR)
            val maxLength = h * style.dripLength * (1f - style.lengthVariance * lengthR)

            val wanted = run {
                val low = left + wantedRadius
                val high = right - wantedRadius
                val x = left + spacing * (i + 0.5f) + (offsetR - 0.5f) * spacing * 0.35f
                if (low <= high) x.coerceIn(low, high) else (left + right) / 2f
            }

            // A drip needs a flat stretch of outline to hang from: none on the steep side of a pill
            // or circle, thin ones under thin glyph strokes. Look for the nearest such stretch
            // (e.g. the next letter of a Text) and centre the drip on it.
            val reach = wantedRadius * (1f + style.fillet)
            val minFlat = max(wantedRadius * 0.6f, 3f * density)
            val cx = findFlatSpot(profile, wanted, spacing * 0.45f, max(1f, density), left, right, reach, tolerance, minFlat)
            if (cx.isNaN() || abs(cx - lastCx) < wantedRadius * 1.5f) continue
            val e0 = profile.edgeAt(cx)
            if (e0.isNaN()) continue

            val flat = min(
                flatRun(profile, cx, e0, -1f, reach, tolerance),
                flatRun(profile, cx, e0, 1f, reach, tolerance),
            )
            val radius = min(wantedRadius, flat * 0.85f)
            if (radius < 1.5f * density || flat < minFlat) continue
            lastCx = cx
            val maxFillet = max(0f, flat - radius) + radius * 0.25f

            val edge = if (e0 <= split) e0 else split + (e0 - split) * sagScale

            val gather = smoothstep(0f, 0.25f, t)
            val flow = smoothstep(0.1f, 1f, t)

            var length = radius * 1.2f * gather + max(0f, maxLength - radius * 1.2f) * flow
            var neck = radius * (0.35f + 0.65f * gather) * (1f - 0.3f * flow)
            // The head only swells as the drip gets long and heavy; early on it's a smooth lobe.
            var head = radius * (1f + (style.bulb - 1f) * flow) * (0.6f + 0.4f * gather)

            val d = out[count]
            d.dropR = 0f
            val drops = style.droplets && dropR < 0.45f && maxLength > radius * 3f
            var s = 0f // time since the drop let go (0..1)
            var released = 0f
            if (drops) {
                val swell = smoothstep(0.5f, 0.72f, t)
                s = ((t - 0.72f) / 0.28f).coerceIn(0f, 1f)
                released = smoothstep(0f, 0.25f, s)
                head *= 1f + 0.15f * swell - 0.35f * released
                length *= 1f - 0.08f * released
            }

            if (length < 0.25f || head < 0.25f) continue

            head = min(head, length * 0.5f + radius * 0.15f).coerceAtLeast(0.25f)
            neck = min(neck, head)
            val headY = edge + length - head

            if (drops && s > 0f) {
                // At the moment of release the drop *is* the head (same circle), so nothing pops;
                // it then falls with zero initial speed, shrinks a little and fades out.
                val full = head / (1f - 0.35f * released).coerceAtLeast(0.01f)
                d.dropR = full + (radius * style.bulb * 0.6f - full) * released
                val fall = s * 0.6f
                d.dropY = headY + 0.5f * gravity * fall * fall
                d.dropAlpha = 1f - smoothstep(0.55f, 1f, s)
            }
            d.cx = cx
            d.edge = edge
            d.bx = cx + style.wobble * density * sin(i * 2.3f + seed) * flow
            d.neck = neck
            d.head = head
            d.fillet = minOf(style.fillet * radius * gather, max(0f, headY - edge) * 0.9f, maxFillet)
            d.headY = headY
            d.dropX = d.bx
            count++
        }
        return count
    }

    /**
     * Nearest x to [start] (within [search]) whose outline is flat for at least [minFlat] on both
     * sides, re-centred on that flat stretch. NaN when there is none.
     */
    private fun findFlatSpot(
        profile: EdgeProfile,
        start: Float,
        search: Float,
        step: Float,
        left: Float,
        right: Float,
        reach: Float,
        tolerance: Float,
        minFlat: Float,
    ): Float {
        var offset = 0f
        while (offset <= search) {
            for (k in 0..1) {
                if (offset == 0f && k == 1) continue
                val x = if (k == 0) start + offset else start - offset
                if (x < left || x >= right) continue
                val e = profile.edgeAt(x)
                if (e.isNaN()) continue
                val l = flatRun(profile, x, e, -1f, reach, tolerance)
                val r = flatRun(profile, x, e, 1f, reach, tolerance)
                if ((l + r) / 2f < minFlat) continue
                // Re-centre on the stretch and make sure it is flat on *both* sides there,
                // so a point on a slope with a long run in one direction doesn't qualify.
                val c = x + (r - l) / 2f
                val ec = profile.edgeAt(c)
                if (ec.isNaN()) continue
                val lc = flatRun(profile, c, ec, -1f, reach, tolerance)
                val rc = flatRun(profile, c, ec, 1f, reach, tolerance)
                if (min(lc, rc) >= minFlat) return c
            }
            offset += step
        }
        return Float.NaN
    }

    /** Distance from [x] in [direction] over which the outline stays within [tolerance] of [e0]. */
    private fun flatRun(profile: EdgeProfile, x: Float, e0: Float, direction: Float, reach: Float, tolerance: Float): Float {
        var d = 0f
        while (d < reach) {
            val e = profile.edgeAt(x + direction * (d + 1f))
            if (e.isNaN() || abs(e - e0) > tolerance) break
            d += 1f
        }
        return d
    }

    fun smoothstep(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Deterministic pseudo-random value in 0..1 for drip [i], channel [k]. */
    private fun random(i: Int, k: Int, seed: Float): Float {
        val x = sin(i * 127.1f + k * 311.7f + seed * 74.7f) * 43758.5453f
        return x - floor(x)
    }
}
