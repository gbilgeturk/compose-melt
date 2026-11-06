package dev.melt.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import dev.melt.MeltStyle

/**
 * Draws one melted frame:
 * 1. the content, with its lower half sagging slightly;
 * 2. the drips as one anti-aliased path, filled with the content's own bottom-edge colours
 *    ([fill], a 1px-high snapshot of the content stretched downward by a clamped image shader);
 * 3. liquid highlights (a soft streak down each drip, a specular dot on each head);
 * 4. falling drops.
 */
internal class MeltRenderer {

    private val path = Path()
    private val drips = Array(DripGeometry.MAX_DRIPS) { Drip() }

    fun ContentDrawScope.drawMelted(
        layer: GraphicsLayer,
        progress: Float,
        style: MeltStyle,
        seed: Float,
        profile: EdgeProfile?,
    ) {
        val w = size.width
        val h = size.height

        val content = this
        layer.record { content.drawContent() }

        // 1. Body, sagging around a line just below the middle.
        val split = h * 0.55f
        val sag = h * style.sag * DripGeometry.smoothstep(0f, 0.6f, progress)
        val edge = h + sag
        val sagScale = (edge - split) / (h - split)
        if (sag < 0.5f) {
            drawLayer(layer)
        } else {
            clipRect(left = 0f, top = 0f, right = w, bottom = split + 0.5f) { drawLayer(layer) }
            clipRect(left = 0f, top = split, right = w, bottom = edge + 1f) {
                scale(scaleX = 1f, scaleY = sagScale, pivot = Offset(0f, split)) {
                    drawLayer(layer)
                }
            }
        }

        // Until the real outline has been measured, drips hang from a flat bottom edge.
        val outline = profile ?: EdgeProfile.flat(w, h, density)
        val count = DripGeometry.compute(drips, outline, h, split, sagScale, density, progress, style, seed)
        if (count == 0) return

        // 2. Drips.
        path.reset()
        for (i in 0 until count) addDrip(drips[i])
        val brush = outline.brush
        if (brush == null) {
            // Fallback fill: replay the content inside the drips, stretched down from a row just
            // above the bottom edge, so the drips still get the content's colours.
            drawStretchedFill(layer, h, count)
            if (style.gloss > 0f) for (i in 0 until count) drawGloss(drips[i], style.gloss)
            return
        }
        drawPath(path, brush)

        // 3. Highlights.
        if (style.gloss > 0f) {
            for (i in 0 until count) drawGloss(drips[i], style.gloss)
        }

        // 4. Falling drops.
        for (i in 0 until count) {
            val d = drips[i]
            if (d.dropR <= 0.25f || d.dropAlpha <= 0.01f) continue
            drawCircle(brush = brush, radius = d.dropR, center = Offset(d.dropX, d.dropY), alpha = d.dropAlpha)
            if (style.gloss > 0f) {
                drawSpecular(d.dropX, d.dropY, d.dropR, style.gloss * d.dropAlpha)
            }
        }
    }

    private fun DrawScope.drawStretchedFill(layer: GraphicsLayer, h: Float, count: Int) {
        // Sample a row just above the fallback edge (see EdgeProfile.flat).
        val pivot = (h - (EdgeProfile.FLAT_INSET_DP + 2f) * density).coerceAtLeast(h * 0.4f)
        var bottom = h
        for (i in 0 until count) bottom = maxOf(bottom, drips[i].headY + drips[i].head)
        val stretch = maxOf(1f, bottom - pivot + 2f)
        clipPath(path) {
            scale(scaleX = 1f, scaleY = stretch, pivot = Offset(0f, pivot)) { drawLayer(layer) }
        }
    }

    private fun addDrip(d: Drip) {
        val edge = d.edge
        val top = edge - 1.5f
        val cx = d.cx
        val bx = d.bx
        val r = d.neck
        val rb = d.head
        val f = d.fillet
        val headY = d.headY
        val mid = (edge + f + headY) / 2f

        path.moveTo(cx - r - f, top)
        path.lineTo(cx - r - f, edge)
        quadTo(cx - r - f, edge, cx - r, edge, cx - r, edge + f)                 // left fillet
        path.cubicTo(cx - r, mid, bx - rb, mid, bx - rb, headY)                 // left side into the head
        path.arcTo(                                                              // round head, via the bottom
            rect = Rect(bx - rb, headY - rb, bx + rb, headY + rb),
            startAngleDegrees = 180f,
            sweepAngleDegrees = -180f,
            forceMoveTo = false,
        )
        path.cubicTo(bx + rb, mid, cx + r, mid, cx + r, edge + f)               // right side up
        quadTo(cx + r, edge + f, cx + r, edge, cx + r + f, edge)                 // right fillet
        path.lineTo(cx + r + f, top)
        path.close()
    }

    /** A soft light streak down the left of the drip, fading in from the edge, plus a head highlight. */
    private fun DrawScope.drawGloss(d: Drip, gloss: Float) {
        val startY = d.edge + d.fillet * 0.5f
        val endY = d.headY - d.head * 0.15f
        if (endY - startY > d.neck) {
            val start = Offset(d.cx - d.neck * 0.35f, startY)
            val end = Offset(d.bx - d.head * 0.4f, endY)
            drawLine(
                brush = Brush.linearGradient(
                    0f to Color.White.copy(alpha = 0f),
                    0.45f to Color.White.copy(alpha = 0.22f * gloss),
                    1f to Color.White.copy(alpha = 0.12f * gloss),
                    start = start,
                    end = end,
                ),
                start = start,
                end = end,
                strokeWidth = d.neck * 0.32f,
                cap = StrokeCap.Round,
            )
        }
        drawSpecular(d.bx, d.headY, d.head, gloss)
    }

    private fun DrawScope.drawSpecular(x: Float, y: Float, r: Float, gloss: Float) {
        if (r < 3f * density) return
        drawOval(
            color = Color.White.copy(alpha = (0.28f * gloss).coerceAtMost(0.45f)),
            topLeft = Offset(x - r * 0.55f, y - r * 0.45f),
            size = Size(r * 0.36f, r * 0.5f),
        )
    }

    /** Quadratic Bézier from (x0, y0) via (qx, qy) to (x2, y2), written as a cubic. */
    private fun quadTo(x0: Float, y0: Float, qx: Float, qy: Float, x2: Float, y2: Float) {
        path.cubicTo(
            x0 + 2f / 3f * (qx - x0), y0 + 2f / 3f * (qy - y0),
            x2 + 2f / 3f * (qx - x2), y2 + 2f / 3f * (qy - y2),
            x2, y2,
        )
    }
}
