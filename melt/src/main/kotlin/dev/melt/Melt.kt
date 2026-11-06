package dev.melt

import android.provider.Settings
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import dev.melt.internal.EdgeProfile
import dev.melt.internal.EdgeSampler
import dev.melt.internal.MeltRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

/**
 * Makes this composable melt: its lower half sags and drips run down from its bottom edge.
 *
 * Every parameter has a default, so the shortest form just works:
 *
 * ```
 * Button(onClick = {}, modifier = Modifier.melt()) { Text("Hello") }
 *
 * Modifier.melt(style = MeltStyle.Wax, speed = MeltSpeed.Fast)
 * Modifier.melt(speed = MeltSpeed(durationMillis = 800))
 * Modifier.melt(style = MeltStyle.Goo.copy(dripCount = 12))
 * ```
 *
 * The melt starts as soon as the composable appears and runs on its own for [speed].
 * Toggle [enabled] to melt on demand: `false` un-melts the composable, `true` melts it again.
 *
 * The effect is purely visual: layout size and touch area stay the same, and drips are drawn
 * below the composable's bounds. If a parent clips its children, the drips get clipped too.
 *
 * @param style Shape of the melt. See [MeltStyle] for presets.
 * @param speed How long the melt takes. See [MeltSpeed] for presets.
 * @param enabled Whether the composable should be melted.
 * @param config Delay, easing, repeat mode and seed.
 */
public fun Modifier.melt(
    style: MeltStyle = MeltStyle.Default,
    speed: MeltSpeed = MeltSpeed.Default,
    enabled: Boolean = true,
    config: MeltConfig = MeltConfig.Default,
): Modifier = this then MeltElement(style, speed, enabled, config)

private data class MeltElement(
    val style: MeltStyle,
    val speed: MeltSpeed,
    val enabled: Boolean,
    val config: MeltConfig,
) : ModifierNodeElement<MeltNode>() {

    override fun create(): MeltNode = MeltNode(style, speed, enabled, config)

    override fun update(node: MeltNode) {
        node.update(style, speed, enabled, config)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "melt"
        properties["style"] = style
        properties["speed"] = speed
        properties["enabled"] = enabled
        properties["config"] = config
    }
}

private const val TAG = "compose-melt"

internal class MeltNode(
    private var style: MeltStyle,
    private var speed: MeltSpeed,
    private var enabled: Boolean,
    private var config: MeltConfig,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private var animatable = Animatable(0f)
    private var animationJob: Job? = null

    private var graphicsContext: GraphicsContext? = null
    private var layer: GraphicsLayer? = null
    private var renderer: MeltRenderer? = null

    // The content's real bottom outline and its colours, sampled once per size.
    private var fill: EdgeProfile? = null
    private var fillSize: IntSize = IntSize.Zero
    private var captureJob: Job? = null
    private var captureFailedFor: IntSize? = null

    override fun onAttach() {
        val context = requireGraphicsContext()
        graphicsContext = context
        layer = context.createGraphicsLayer()
        renderer = MeltRenderer()
        startAnimation()
    }

    override fun onDetach() {
        animationJob = null // the node's coroutineScope is cancelled for us
        captureJob = null
        layer?.let { graphicsContext?.releaseGraphicsLayer(it) }
        layer = null
        graphicsContext = null
        renderer = null
        fill = null
        fillSize = IntSize.Zero
        captureFailedFor = null
    }

    override fun onReset() {
        // Reused by a lazy list for a different item: start intact again.
        animatable = Animatable(0f)
        fill = null
        fillSize = IntSize.Zero
    }

    fun update(style: MeltStyle, speed: MeltSpeed, enabled: Boolean, config: MeltConfig) {
        // A style change only affects drawing; timing changes need a new animation.
        val restart = this.speed != speed || this.enabled != enabled || this.config != config

        this.style = style
        this.speed = speed
        this.enabled = enabled
        this.config = config

        if (restart && isAttached) startAnimation()
        invalidateDraw()
    }

    private fun startAnimation() {
        animationJob?.cancel()
        animationJob = null

        val cfg = config
        val duration = speed.durationMillis
        val target = animatable
        val melt = enabled
        val reduced = cfg.respectReducedMotion && isReducedMotion()
        if (reduced && melt) {
            Log.i(TAG, "System animations are off (animator duration scale = 0), not melting. " +
                "Pass MeltConfig(respectReducedMotion = false) to melt anyway.")
        }

        animationJob = coroutineScope.launch {
            if (reduced) {
                target.snapTo(0f)
                return@launch
            }
            if (!melt) {
                target.animateTo(0f, tween((duration / 2).coerceAtLeast(1), easing = FastOutSlowInEasing))
                return@launch
            }
            if (cfg.delayMillis > 0 && target.value == 0f) delay(cfg.delayMillis.toLong())

            val once = tween<Float>(durationMillis = duration, easing = cfg.easing)
            when (cfg.repeat) {
                MeltRepeat.Once -> target.animateTo(1f, once)
                MeltRepeat.Restart -> {
                    target.snapTo(0f)
                    target.animateTo(1f, infiniteRepeatable(once, RepeatMode.Restart))
                }
                MeltRepeat.Reverse -> target.animateTo(1f, infiniteRepeatable(once, RepeatMode.Reverse))
            }
        }
    }

    private fun isReducedMotion(): Boolean {
        val context = currentValueOf(LocalContext)
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        return scale == 0f
    }

    override fun ContentDrawScope.draw() {
        val p = animatable.value.coerceIn(0f, 1f)
        val layer = layer
        val renderer = renderer
        if (p <= 0f || layer == null || renderer == null || size.width <= 0f || size.height <= 0f) {
            drawContent()
            return
        }

        val intSize = IntSize(ceil(size.width).toInt(), ceil(size.height).toInt())
        if (fillSize != intSize) fill = null

        with(renderer) { drawMelted(layer, p, style, config.seed.toFloat(), fill) }

        // The layer now holds this frame; snapshot it once (per size) to measure the real bottom
        // outline and its colours. Until then, and if that fails, a flat-edge fallback is drawn.
        if (fill == null && captureJob?.isActive != true && captureFailedFor != intSize) {
            val d = density
            captureJob = coroutineScope.launch {
                val profile = try {
                    val snapshot = layer.toImageBitmap()
                    withContext(Dispatchers.Default) { EdgeSampler.sample(snapshot, d) }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.w(TAG, "Couldn't snapshot the content, using the flat-edge fallback.", e)
                    null
                }
                if (profile == null) {
                    captureFailedFor = intSize
                    Log.w(TAG, "No opaque bottom outline found at $intSize, using the flat-edge fallback.")
                } else {
                    fill = profile
                    fillSize = intSize
                }
                invalidateDraw()
            }
        }
    }
}
