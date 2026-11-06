package dev.melt.internal

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * The real bottom outline of the content, measured from a snapshot.
 *
 * @param edges For every pixel column, the y just below the lowest opaque pixel, or NaN when the
 *   column is empty. A pill, a circle, a card and the glyphs of a Text all get their true outline.
 * @param brush Fills drips with the colour of the content right above that outline, per column:
 *   a 1px-high image whose shader is clamped vertically, so it extends down through every drip.
 *   Null for the [flat] fallback, which fills drips by stretching the content itself.
 */
internal class EdgeProfile(
    val brush: Brush?,
    private val edges: FloatArray,
    val minX: Int,
    val maxX: Int,
) {
    fun edgeAt(x: Float): Float {
        val i = x.toInt()
        return if (i in edges.indices) edges[i] else Float.NaN
    }

    companion object {
        /** How far above the bounds the fallback edge sits; see [flat]. */
        const val FLAT_INSET_DP = 4f

        /**
         * Used until the real outline has been measured (or if measuring fails): a flat bottom edge,
         * kept away from the corners so it also looks right on pill-shaped buttons.
         *
         * The edge sits [FLAT_INSET_DP] above the bounds: Material buttons and FABs enlarge their
         * touch area to 48dp, which leaves a transparent band of about 4dp under the visible shape.
         * On full-bleed content the drips simply start slightly inside it, which isn't visible.
         */
        fun flat(w: Float, h: Float, density: Float): EdgeProfile {
            val width = ceil(w).toInt().coerceAtLeast(1)
            val margin = minOf(h * 0.5f, w * 0.2f, 32f * density).toInt()
            val edge = (h - FLAT_INSET_DP * density).coerceAtLeast(h * 0.5f)
            val edges = FloatArray(width) { x -> if (x >= margin && x < width - margin) edge else Float.NaN }
            val minX = margin.coerceAtMost(width - 1)
            val maxX = (width - 1 - margin).coerceAtLeast(minX)
            return EdgeProfile(null, edges, minX, maxX)
        }
    }
}

internal object EdgeSampler {

    private const val OPAQUE = 128

    /** Runs off the main thread: scans every column once. */
    fun sample(snapshot: ImageBitmap, density: Float): EdgeProfile? {
        var bitmap = snapshot.asAndroidBitmap()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return null
        }
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return null

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val edges = FloatArray(w) { Float.NaN }
        val colors = IntArray(w)
        val inset = (density).roundToInt().coerceAtLeast(1)
        var minX = -1
        var maxX = -1

        for (x in 0 until w) {
            var y = h - 1
            while (y >= 0 && (pixels[y * w + x] ushr 24) < OPAQUE) y--
            if (y < 0) continue
            edges[x] = (y + 1).toFloat()
            // Take the colour slightly above the very edge, away from its anti-aliased rim.
            var cy = y - inset
            if (cy < 0 || (pixels[cy * w + x] ushr 24) < OPAQUE) cy = y
            colors[x] = pixels[cy * w + x] or (0xFF shl 24)
            if (minX < 0) minX = x
            maxX = x
        }
        if (minX < 0) return null

        val row = Bitmap.createBitmap(colors, w, 1, Bitmap.Config.ARGB_8888)
        val brush = ShaderBrush(ImageShader(row.asImageBitmap(), TileMode.Clamp, TileMode.Clamp))
        return EdgeProfile(brush, edges, minX, maxX)
    }
}
