package dev.melt

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Immutable

/**
 * Behaviour of the melt animation. Shape lives in [MeltStyle], duration in [MeltSpeed].
 *
 * @param delayMillis Delay before the first melt starts.
 * @param easing Easing of the overall melt timeline. Linear by default: every drip already
 *   eases in and out on its own, so extra easing here makes drips start abruptly.
 * @param repeat Whether the melt plays once or loops.
 * @param seed Changes the drip pattern. Give side-by-side composables different seeds
 *   so they don't drip identically.
 * @param respectReducedMotion When true and the system animator duration scale is 0
 *   ("Remove animations"), the composable is left intact.
 */
@Immutable
public data class MeltConfig(
    val delayMillis: Int = 0,
    val easing: Easing = LinearEasing,
    val repeat: MeltRepeat = MeltRepeat.Once,
    val seed: Int = 0,
    val respectReducedMotion: Boolean = true,
) {
    init {
        require(delayMillis >= 0) { "delayMillis must be >= 0, was $delayMillis" }
    }

    public companion object {
        /** Used when no config is given. */
        public val Default: MeltConfig = MeltConfig()
    }
}

/** How the melt animation repeats. */
public enum class MeltRepeat {
    /** Melts once and stays melted. */
    Once,

    /** Melts, snaps back to intact, melts again, forever. */
    Restart,

    /** Melts and un-melts back and forth, forever. */
    Reverse,
}
