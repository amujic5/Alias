package hr.azzi.socialgames.alias.Service.RoundVideo

import android.graphics.RectF

/**
 * Where everything sits in the round video, in the Figma canvas' points. Ported verbatim
 * from the iOS `RoundOverlayLayout` so the two platforms produce the same clip; both are
 * the same Figma frame, and the numbers are the design's, not derived.
 *
 * Everything here is scaled by [RoundVideoSpec.RENDER_WIDTH] / [RoundVideoSpec.CANVAS_WIDTH]
 * on render.
 */
object RoundOverlayLayout {

    /** The white card the front feed sits on, and the feed's own rounded window. */
    val pipCard = RectF(16f, 58f, 16f + 118f, 58f + 196f)
    const val PIP_CARD_RADIUS = 20f
    val pipFeed = RectF(20f, 62f, 20f + 110f, 62f + 188f)
    const val PIP_FEED_RADIUS = 17f

    /** The app icon hanging off the bottom of the PiP card. */
    val pipBadge = RectF(52f, 234f, 52f + 42f, 234f + 42f)

    val correctBadge = RectF(160f, 62f, 160f + 99f, 62f + 69f)
    val missedBadge = RectF(271f, 62f, 271f + 99f, 62f + 69f)
    val timerBadge = RectF(286f, 147f, 286f + 80f, 147f + 80f)
    const val BADGE_RADIUS = 16f

    val wordCard = RectF(20f, 600f, 20f + 350f, 600f + 148f)
    const val WORD_CARD_RADIUS = 28f
    val wordCardBadge = RectF(316f, 697f, 316f + 47f, 697f + 47f)
    val wordPill = RectF(119f, 582f, 119f + 152f, 582f + 38f)
    val brandPill = RectF(134f, 780f, 134f + 122f, 780f + 38f)
    val wordTextRect = RectF(30f, 630f, 30f + 330f, 630f + 76f)

    /** Canvas points → render pixels. */
    val scale: Float get() = RoundVideoSpec.RENDER_WIDTH / RoundVideoSpec.CANVAS_WIDTH

    fun scaled(rect: RectF): RectF {
        val factor = scale
        return RectF(rect.left * factor, rect.top * factor, rect.right * factor, rect.bottom * factor)
    }
}

/** The clip's palette, from the same Figma frame. */
object RoundOverlayColor {
    const val BRAND_BLUE = 0xFF1E88EA.toInt()
    const val DEEP_BLUE = 0xFF0F70D1.toInt()
    const val LIGHT_BLUE = 0xFF3AA0F5.toInt()
    const val GREEN = 0xFF34C759.toInt()
    const val RED = 0xFFFF453A.toInt()
    const val SHADOW = 0xFF0E1726.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
}
