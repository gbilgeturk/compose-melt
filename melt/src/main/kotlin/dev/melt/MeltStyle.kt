package dev.melt

import androidx.compose.runtime.Immutable

/**
 * The *shape* of a melt: how much the body sags, how many drips hang from its bottom edge,
 * how long and wide they are, and how glossy the liquid looks.
 *
 * Use one of the presets ([Drip], [Wax], [Goo], [Slump], [Wavy]), tweak one with `copy(...)`,
 * or build your own:
 *
 * ```
 * Modifier.melt(style = MeltStyle.Wax)
 * Modifier.melt(style = MeltStyle.Drip.copy(dripCount = 10, droplets = false))
 * Modifier.melt(style = MeltStyle(dripCount = 5, dripLength = 1.2f, bulb = 1.6f))
 * ```
 *
 * @param dripCount How many drips hang across the width.
 * @param dripLength Length of the longest drip as a fraction of the composable's height (0..4).
 *   Drips are drawn *below* the composable's bounds; layout size never changes.
 * @param dripWidth Drip thickness as a fraction of the space available per drip (0.05..1).
 *   1 = neighbouring drips touch.
 * @param lengthVariance How much drip lengths differ from each other (0..1). 0 = all equal.
 * @param bulb Size of the round head at each tip relative to the drip's thickness (1..2.5).
 *   1 = straight, rounded end; higher = a fat teardrop head.
 * @param fillet How softly a drip flares out of the bottom edge, relative to its thickness (0..3).
 * @param stagger How spread out in time the drips start (0..0.9). 0 = all at once.
 * @param droplets Whether some drips let a drop fall off their tip during the melt.
 * @param wobble Sideways bend of the drips, in dp. 0 = straight drips.
 * @param sag How much the lower half of the composable itself sags, as a fraction of its height (0..0.3).
 * @param gloss Strength of the liquid highlights (0..1.5). 0 = flat colour.
 */
@Immutable
public data class MeltStyle(
    val dripCount: Int = 7,
    val dripLength: Float = 1f,
    val dripWidth: Float = 0.65f,
    val lengthVariance: Float = 0.6f,
    val bulb: Float = 1.4f,
    val fillet: Float = 1.4f,
    val stagger: Float = 0.35f,
    val droplets: Boolean = true,
    val wobble: Float = 0f,
    val sag: Float = 0.06f,
    val gloss: Float = 1f,
) {
    init {
        require(dripCount in 1..64) { "dripCount must be in 1..64, was $dripCount" }
        require(dripLength in 0f..4f) { "dripLength must be in 0..4, was $dripLength" }
        require(dripWidth in 0.05f..1f) { "dripWidth must be in 0.05..1, was $dripWidth" }
        require(lengthVariance in 0f..1f) { "lengthVariance must be in 0..1, was $lengthVariance" }
        require(bulb in 1f..2.5f) { "bulb must be in 1..2.5, was $bulb" }
        require(fillet in 0f..3f) { "fillet must be in 0..3, was $fillet" }
        require(stagger in 0f..0.9f) { "stagger must be in 0..0.9, was $stagger" }
        require(wobble >= 0f) { "wobble must be >= 0, was $wobble" }
        require(sag in 0f..0.3f) { "sag must be in 0..0.3, was $sag" }
        require(gloss in 0f..1.5f) { "gloss must be in 0..1.5, was $gloss" }
    }

    public companion object {
        /** Glossy paint drips with round heads; a few drops fall off. The default. */
        public val Drip: MeltStyle = MeltStyle()

        /** Candle wax: a few thick, slow drips, matte, no falling drops. */
        public val Wax: MeltStyle = MeltStyle(
            dripCount = 4,
            dripLength = 0.75f,
            dripWidth = 0.75f,
            lengthVariance = 0.7f,
            bulb = 1.2f,
            fillet = 1.6f,
            stagger = 0.5f,
            droplets = false,
            sag = 0.04f,
            gloss = 0.5f,
        )

        /** Slime: many long, shiny drips with fat heads. */
        public val Goo: MeltStyle = MeltStyle(
            dripCount = 9,
            dripLength = 1.5f,
            dripWidth = 0.55f,
            lengthVariance = 0.65f,
            bulb = 1.6f,
            fillet = 1f,
            stagger = 0.4f,
            droplets = true,
            sag = 0.05f,
            gloss = 1.3f,
        )

        /** Soft ice cream: the body sags and short, wide lobes merge along the bottom. */
        public val Slump: MeltStyle = MeltStyle(
            dripCount = 5,
            dripLength = 0.35f,
            dripWidth = 1f,
            lengthVariance = 0.3f,
            bulb = 1f,
            fillet = 1.6f,
            stagger = 0.15f,
            droplets = false,
            sag = 0.14f,
            gloss = 0.7f,
        )

        /** Drips that bend sideways as they run. */
        public val Wavy: MeltStyle = MeltStyle(
            dripCount = 6,
            dripLength = 1f,
            dripWidth = 0.6f,
            lengthVariance = 0.5f,
            bulb = 1.4f,
            fillet = 1.3f,
            droplets = true,
            wobble = 6f,
        )

        /** Used when no style is given. */
        public val Default: MeltStyle = Drip
    }
}
