package hr.azzi.socialgames.alias.Service.RoundVideo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import hr.azzi.socialgames.alias.R
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Draws the round video's overlay (Figma "12 AI Play - Guessing", node 3207:1203).
 *
 * Split into a static part and small dynamic patches on purpose, exactly as the iOS
 * `RoundOverlayRenderer` is. Cards, borders, shadows, sparkles and the brand pill never
 * change, so they are drawn once; only the clock, the two counters and the word do, and
 * each of those is a small bitmap cached per distinct value. Everything here happens on the
 * CPU with `Canvas`, so it must happen off the per-frame path — a round of 60 seconds draws
 * roughly 60 clock patches and one per word, and nothing else.
 */
class RoundOverlayRenderer(private val context: Context) {

    /** Canvas points → render pixels. */
    private val scale = RoundOverlayLayout.scale

    /** Below the PiP feed: just the card the feed sits on. */
    val chromeBelow: Bitmap by lazy { renderChromeBelow() }

    /** Above everything: badges, cards, pills, sparkles. */
    val chromeAbove: Bitmap by lazy { renderChromeAbove() }

    private val timerCache = mutableMapOf<Int, Bitmap>()
    private val correctCache = mutableMapOf<Int, Bitmap>()
    private val missedCache = mutableMapOf<Int, Bitmap>()
    private val wordCache = mutableMapOf<String, Bitmap>()

    private val baloo: Typeface? by lazy { ResourcesCompat.getFont(context, R.font.baloo2) }
    private val jakarta: Typeface? by lazy { ResourcesCompat.getFont(context, R.font.plusjakartasans) }
    /**
     * The app icon, as pixels. Deliberately not `mipmap/ic_launcher`: from API 26 that
     * resolves to the adaptive-icon XML, which `BitmapFactory` cannot decode at all — it
     * returns null and the ring ends up filled with flat blue.
     */
    private val appIcon: Bitmap? by lazy {
        runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.alias_rounded) }.getOrNull()
    }

    enum class Patch { TIMER, CORRECT, MISSED, WORD }

    /** Where a cached patch belongs, in render pixels. */
    fun rectFor(patch: Patch): RectF = RoundOverlayLayout.scaled(
        when (patch) {
            Patch.TIMER -> RoundOverlayLayout.timerBadge
            Patch.CORRECT -> numberRect(RoundOverlayLayout.correctBadge)
            Patch.MISSED -> numberRect(RoundOverlayLayout.missedBadge)
            Patch.WORD -> RoundOverlayLayout.wordTextRect
        },
    )

    /**
     * The counters' numerals are placed off their badges rather than written out twice — in
     * the design the two sit 3pt apart, which is a stray drag, not a difference worth
     * carrying into code.
     */
    private fun numberRect(badge: RectF) =
        RectF(badge.left + 3f, badge.top + 20f, badge.left + 3f + 92f, badge.top + 20f + 48f)

    // MARK: - Cached dynamic patches

    /** The seconds left, as a plain number. Cached per value. */
    fun timer(remainingSeconds: Int): Bitmap = timerCache.getOrPut(remainingSeconds) {
        patch(rectFor(Patch.TIMER)) { canvas, bounds ->
            text(
                canvas, remainingSeconds.toString(), bounds,
                paint(baloo, 800, s(27f), RoundOverlayColor.BRAND_BLUE), center = true,
            )
        }
    }

    fun correct(value: Int): Bitmap = correctCache.getOrPut(value) {
        patch(rectFor(Patch.CORRECT)) { canvas, bounds ->
            text(canvas, value.toString(), bounds, paint(baloo, 800, s(30f), Color.WHITE), center = true)
        }
    }

    fun missed(value: Int): Bitmap = missedCache.getOrPut(value) {
        patch(rectFor(Patch.MISSED)) { canvas, bounds ->
            text(canvas, value.toString(), bounds, paint(baloo, 800, s(30f), Color.WHITE), center = true)
        }
    }

    /** The word, shrunk to fit the card rather than clipped. */
    fun word(word: String): Bitmap = wordCache.getOrPut(word) {
        patch(rectFor(Patch.WORD)) { canvas, bounds ->
            var size = 56f
            var p = paint(baloo, 800, s(size), RoundOverlayColor.DEEP_BLUE)
            while (size > 20f && p.measureText(word) > bounds.width()) {
                size -= 2f
                p = paint(baloo, 800, s(size), RoundOverlayColor.DEEP_BLUE)
            }
            text(canvas, word, bounds, p, center = true)
        }
    }

    // MARK: - Static chrome

    private fun renderChromeBelow(): Bitmap = full { canvas ->
        // The white card the PiP feed is inset into.
        val card = RoundOverlayLayout.scaled(RoundOverlayLayout.pipCard)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(s(8f), 0f, s(6f), withAlpha(RoundOverlayColor.SHADOW, 0.28f))
        }
        canvas.drawRoundRect(card, s(RoundOverlayLayout.PIP_CARD_RADIUS), s(RoundOverlayLayout.PIP_CARD_RADIUS), fill)
    }

    private fun renderChromeAbove(): Bitmap = full { canvas ->
        circularIcon(canvas, RoundOverlayLayout.scaled(RoundOverlayLayout.pipBadge), s(3f))
        timerBadge(canvas)
        badge(
            canvas, RoundOverlayLayout.correctBadge, RoundOverlayColor.GREEN, "✓",
            context.getString(R.string.ai_video_correct),
        )
        badge(
            canvas, RoundOverlayLayout.missedBadge, RoundOverlayColor.RED, "✕",
            context.getString(R.string.ai_video_missed),
        )
        wordCard(canvas)
        wordPill(canvas)
        brandPill(canvas)
    }

    // MARK: - Pieces

    /** A white disc with a blue ring; the design simplified the clock down to the number. */
    private fun timerBadge(canvas: Canvas) {
        val r = RoundOverlayLayout.scaled(RoundOverlayLayout.timerBadge)
        borderedShape(
            canvas, r, r.height() / 2f, Color.WHITE, RoundOverlayColor.BRAND_BLUE, s(4f),
            shadowColor = withAlpha(RoundOverlayColor.SHADOW, 0.20f), shadowDy = s(6f), shadowBlur = s(14f),
        )
    }

    private fun badge(canvas: Canvas, layout: RectF, fill: Int, glyph: String, title: String) {
        val r = RoundOverlayLayout.scaled(layout)
        val radius = s(RoundOverlayLayout.BADGE_RADIUS)
        canvas.drawRoundRect(
            r, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fill
                setShadowLayer(s(6f), 0f, s(4f), withAlpha(RoundOverlayColor.SHADOW, 0.22f))
            },
        )

        // Translucent border, so it has to sit on top of the fill rather than replace it at
        // the edge; inset by half its width and shrink the radius to match.
        val inset = s(1f)
        val border = RectF(r.left + inset, r.top + inset, r.right - inset, r.bottom - inset)
        canvas.drawRoundRect(
            border, radius - inset, radius - inset,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = s(2f)
                color = withAlpha(Color.WHITE, 0.7f)
            },
        )

        // White disc with the tick/cross in the badge colour, per the design.
        val disc = RoundOverlayLayout.scaled(
            RectF(layout.left + 7f, layout.top + 6f, layout.left + 25f, layout.top + 24f),
        )
        canvas.drawOval(disc, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        text(canvas, glyph, disc, paint(jakarta, 800, s(11f), fill), center = true)

        val titleRect = RoundOverlayLayout.scaled(
            RectF(layout.left + 29f, layout.top + 8f, layout.right - 4f, layout.top + 24f),
        )
        text(canvas, title, titleRect, paint(jakarta, 700, s(10f), Color.WHITE), center = false)
    }

    private fun wordCard(canvas: Canvas) {
        val r = RoundOverlayLayout.scaled(RoundOverlayLayout.wordCard)
        val radius = s(RoundOverlayLayout.WORD_CARD_RADIUS)
        borderedShape(
            canvas, r, radius, Color.WHITE, RoundOverlayColor.BRAND_BLUE, s(4f),
            shadowColor = withAlpha(RoundOverlayColor.SHADOW, 0.3f), shadowDy = s(8f), shadowBlur = s(22f),
        )

        // Sparkles, clipped to the card like the design's overflow-clip.
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(r, radius, radius, Path.Direction.CW) })
        val card = RoundOverlayLayout.wordCard
        sparkle(canvas, card.left + 28f, card.top + 30f, 20f, 0xFF9FD4FF.toInt())
        sparkle(canvas, card.left + 38f, card.top + 46f, 12f, 0xFFCFE9FF.toInt())
        sparkle(canvas, card.left + 304f, card.top + 34f, 16f, 0xFFFFD23D.toInt())
        sparkle(canvas, card.left + 31f, card.top + 115f, 14f, RoundOverlayColor.LIGHT_BLUE)
        canvas.restore()

        circularIcon(canvas, RoundOverlayLayout.scaled(RoundOverlayLayout.wordCardBadge), s(3f))
    }

    private fun wordPill(canvas: Canvas) {
        val r = RoundOverlayLayout.scaled(RoundOverlayLayout.wordPill)
        borderedShape(
            canvas, r, r.height() / 2f, RoundOverlayColor.BRAND_BLUE, Color.WHITE, s(3f),
            shadowColor = withAlpha(RoundOverlayColor.SHADOW, 0.25f), shadowDy = s(4f), shadowBlur = s(12f),
        )
        text(
            canvas, context.getString(R.string.ai_video_the_word_is), r,
            paint(jakarta, 800, s(14f), Color.WHITE).apply { letterSpacing = 1.2f / 14f },
            center = true,
        )
    }

    private fun brandPill(canvas: Canvas) {
        val r = RoundOverlayLayout.scaled(RoundOverlayLayout.brandPill)
        borderedShape(
            canvas, r, r.height() / 2f, Color.WHITE, BRAND_BORDER, s(3f),
            shadowColor = withAlpha(RoundOverlayColor.SHADOW, 0.18f), shadowDy = s(3f), shadowBlur = s(10f),
        )

        // ALIAS, one colour per letter.
        val letters = listOf(
            "A" to RoundOverlayColor.LIGHT_BLUE, "L" to 0xFFFF8A3D.toInt(),
            "I" to 0xFF4CD964.toInt(), "A" to 0xFF9B6DFF.toInt(), "S" to 0xFFF7BE1E.toInt(),
        )
        val p = paint(baloo, 800, s(20f), Color.WHITE)
        val tracking = s(2f)
        val widths = letters.map { p.measureText(it.first) + tracking }
        var x = r.centerX() - widths.sum() / 2f
        val metrics = p.fontMetrics
        val baseline = r.centerY() - (metrics.ascent + metrics.descent) / 2f
        for ((index, letter) in letters.withIndex()) {
            p.color = letter.second
            canvas.drawText(letter.first, x, baseline, p)
            x += widths[index]
        }
    }

    /** The app icon in a white ring; falls back to a plain disc if it isn't loadable. */
    private fun circularIcon(canvas: Canvas, rect: RectF, borderWidth: Float) {
        canvas.save()
        canvas.clipPath(Path().apply { addOval(rect, Path.Direction.CW) })
        val icon = appIcon
        if (icon != null) {
            canvas.drawBitmap(icon, Rect(0, 0, icon.width, icon.height), rect, Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            canvas.drawRect(rect, Paint().apply { color = RoundOverlayColor.LIGHT_BLUE })
        }
        canvas.restore()
        val ring = RectF(rect).apply { inset(borderWidth / 2f, borderWidth / 2f) }
        canvas.drawOval(
            ring,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = borderWidth
                color = Color.WHITE
            },
        )
    }

    /**
     * Four-point star, the exact geometry of the design's sparkle asset — the inner radius
     * is 0.3 of the outer one, taken from the exported path data.
     */
    private fun sparkle(canvas: Canvas, centerX: Float, centerY: Float, size: Float, color: Int) {
        val cx = centerX * scale
        val cy = centerY * scale
        val outer = s(size) / 2f
        val inner = outer * 0.3f
        val path = Path()
        for (step in 0 until 8) {
            val radius = if (step % 2 == 0) outer else inner
            val angle = step * Math.PI / 4.0 - Math.PI / 2.0
            val x = cx + (cos(angle) * radius).toFloat()
            val y = cy + (sin(angle) * radius).toFloat()
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    // MARK: - Drawing helpers

    private fun s(value: Float) = value * scale

    private fun withAlpha(color: Int, alpha: Float) =
        Color.argb((alpha * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    private fun paint(face: Typeface?, weight: Int, size: Float, color: Int) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = face
            // Both fonts are variable; without this every weight renders at 400.
            fontVariationSettings = "'wght' $weight"
            textSize = size
            this.color = color
        }

    private fun text(canvas: Canvas, value: String, rect: RectF, paint: Paint, center: Boolean) {
        val metrics = paint.fontMetrics
        val baseline = rect.centerY() - (metrics.ascent + metrics.descent) / 2f
        val x = if (center) rect.centerX() - paint.measureText(value) / 2f else rect.left
        canvas.drawText(value, x, baseline, paint)
    }

    /**
     * A bordered card, drawn border-first.
     *
     * The obvious way — fill, then stroke the border on top — leaves the fill reaching the
     * outer edge, where its anti-aliased pixels leak out from under the equally anti-aliased
     * stroke. Over a light background that is invisible; over dark video it reads as a
     * second, paler outline around the real one. Filling the border shape and then insetting
     * the fill means the outer edge is one colour.
     */
    private fun borderedShape(
        canvas: Canvas,
        rect: RectF,
        radius: Float,
        fill: Int,
        border: Int,
        borderWidth: Float,
        shadowColor: Int? = null,
        shadowDy: Float = 0f,
        shadowBlur: Float = 0f,
    ) {
        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = border
                if (shadowColor != null) setShadowLayer(shadowBlur, 0f, shadowDy, shadowColor)
            },
        )
        // The inner radius has to shrink with the inset, or the border thins out around the
        // curves and thickens on the straights.
        val inner = RectF(rect).apply { inset(borderWidth, borderWidth) }
        canvas.drawRoundRect(
            inner, max(0f, radius - borderWidth), max(0f, radius - borderWidth),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill },
        )
    }

    /** Full-size transparent canvas, in the layout's top-left space. */
    private fun full(body: (Canvas) -> Unit): Bitmap {
        val bitmap = Bitmap.createBitmap(
            RoundVideoSpec.RENDER_WIDTH, RoundVideoSpec.RENDER_HEIGHT, Bitmap.Config.ARGB_8888,
        )
        body(Canvas(bitmap))
        return bitmap
    }

    /** A patch only as large as `rect`, drawn in its own local space. */
    private fun patch(rect: RectF, body: (Canvas, RectF) -> Unit): Bitmap {
        val width = max(1, rect.width().toInt())
        val height = max(1, rect.height().toInt())
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        body(Canvas(bitmap), RectF(0f, 0f, width.toFloat(), height.toFloat()))
        return bitmap
    }

    fun release() {
        (timerCache.values + correctCache.values + missedCache.values + wordCache.values)
            .forEach { it.recycle() }
        timerCache.clear()
        correctCache.clear()
        missedCache.clear()
        wordCache.clear()
    }

    private companion object {
        val BRAND_BORDER = 0xFF1D6CF9.toInt()
    }
}
