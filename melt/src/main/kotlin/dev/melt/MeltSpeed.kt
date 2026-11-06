package dev.melt

import androidx.compose.runtime.Immutable

/**
 * How fast a composable melts: the time from intact to fully melted.
 *
 * ```
 * Modifier.melt(speed = MeltSpeed.Fast)
 * Modifier.melt(speed = MeltSpeed(durationMillis = 800))
 * ```
 */
@Immutable
public data class MeltSpeed(val durationMillis: Int) {
    init {
        require(durationMillis > 0) { "durationMillis must be > 0, was $durationMillis" }
    }

    public companion object {
        public val Slow: MeltSpeed = MeltSpeed(4000)
        public val Normal: MeltSpeed = MeltSpeed(2400)
        public val Fast: MeltSpeed = MeltSpeed(1200)
        public val VeryFast: MeltSpeed = MeltSpeed(600)

        /** Used when no speed is given. */
        public val Default: MeltSpeed = Normal
    }
}
