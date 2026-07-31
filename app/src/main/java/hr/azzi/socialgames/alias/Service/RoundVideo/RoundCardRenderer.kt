package hr.azzi.socialgames.alias.Service.RoundVideo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import hr.azzi.socialgames.alias.R
import kotlin.math.max

/** What the two cards need to say. */
data class RoundCardInfo(
    val teamName: String,
    val explainer: String,
    val guesser: String,
    val correctWords: List<String>,
    val skippedWords: List<String>,
    /** Country backdrop for the intro; null falls back to a plain brand fill. */
    val backgroundRes: Int? = null,
)

/** The cards' palette, from the same Figma frames. */
object RoundCardColor {
    const val BRAND_BLUE = 0xFF1E88EA.toInt()
    const val LIGHT_BLUE = 0xFF3AA0F5.toInt()
    const val DEEP_BLUE = 0xFF0F70D1.toInt()
    const val ROW_BLUE = 0xFF2685F5.toInt()
    const val GREEN = 0xFF34C759.toInt()
    const val ORANGE = 0xFFFF8A3D.toInt()
    const val YELLOW = 0xFFFFC91F.toInt()
    const val AMBER = 0xFFFFD23D.toInt()
    const val INK = 0xFF0E1726.toInt()
    const val DIVIDER = 0xFFE2E8F1.toInt()
    const val SHADOW = 0xFF083873.toInt()
    const val CONFETTI_PINK = 0xFFFF5C8A.toInt()
    const val CONFETTI_PURPLE = 0xFF9B6DFF.toInt()
    const val CONFETTI_TEAL = 0xFF28D9C4.toInt()
    const val CONFETTI_GREEN = 0xFF4CD964.toInt()
}

/**
 * One animated piece of a card. Each is its own small transparent bitmap so the compositor
 * can scale and fade it without re-drawing anything.
 */
class RoundCardLayer(
    val image: Bitmap,
    /**
     * Where the bitmap sits on the card, in render pixels with a top-left origin. May start
     * outside the canvas: the frame is padded to leave room for shadows.
     */
    val frame: RectF,
    /** Seconds from the card's first frame until this layer starts moving. */
    val delay: Double,
    val motion: Motion,
) {
    enum class Motion(val duration: Double) {
        /** Springs up from 62% with a slight overshoot. For headings and cards. */
        POP(0.42),

        /** Slides up into place while fading in. For list rows, where a dozen pops at once
         *  would look like popcorn. */
        RISE(0.30),
    }
}

/** A card as the compositor consumes it: a still backdrop plus the pieces that move. */
class RoundCard(val backdrop: Bitmap, val layers: List<RoundCardLayer>)

/**
 * The full-screen intro and outro cards bookending the round video (Figma nodes 3184:605 and
 * 3181:605).
 *
 * Both are drawn once per clip — they never change during their segment — so a frame that
 * shows one is a single composite plus its moving pieces. Like the overlay, everything is
 * `Canvas`: no artwork beyond the country backdrop the app already ships, and no fonts the
 * project doesn't already use.
 */
class RoundCardRenderer(private val context: Context, private val info: RoundCardInfo) {

    private val scale = RoundOverlayLayout.scale
    private val width = RoundVideoSpec.RENDER_WIDTH
    private val height = RoundVideoSpec.RENDER_HEIGHT

    private val baloo: Typeface? by lazy { ResourcesCompat.getFont(context, R.font.baloo2) }
    private val jakarta: Typeface? by lazy { ResourcesCompat.getFont(context, R.font.plusjakartasans) }

    // Not `by lazy`: the outro can only be drawn once the round is over, and a round that is
    // abandoned never draws it at all. A lazy property would have `release` build the very
    // thing it is trying to throw away.
    private var introCard: RoundCard? = null
    private var outroCard: RoundCard? = null

    val intro: RoundCard get() = introCard ?: buildIntro().also { introCard = it }
    val outro: RoundCard get() = outroCard ?: buildOutro().also { outroCard = it }

    // MARK: - Intro

    /**
     * Backdrop and confetti hold still; the wordmark and the three rows come in one after
     * another. Everything lands inside the intro's hold, so nothing is still moving when the
     * card starts dissolving into the round.
     */
    private fun buildIntro(): RoundCard {
        val backdrop = full { canvas ->
            canvas.drawColor(RoundCardColor.BRAND_BLUE)
            info.backgroundRes?.let { res ->
                runCatching { BitmapFactory.decodeResource(context.resources, res) }.getOrNull()
                    ?.let { image ->
                        canvas.drawBitmap(
                            image, Rect(0, 0, image.width, image.height),
                            aspectFill(image.width.toFloat(), image.height.toFloat()),
                            Paint(Paint.FILTER_BITMAP_FLAG),
                        )
                    }
            }

            // Confetti chips behind the copy.
            chip(canvas, 40f, 133.9f, 52f, 11f, -18f, RoundCardColor.CONFETTI_PINK)
            chip(canvas, 289f, 120f, 40f, 9f, 16f, RoundCardColor.ORANGE)
            chip(canvas, 322f, 287f, 26f, 6f, -30f, RoundCardColor.CONFETTI_TEAL)
            chip(canvas, 60f, 347.8f, 30f, 7f, -24f, RoundCardColor.CONFETTI_GREEN)
            chip(canvas, 287f, 420f, 34f, 7f, 22f, RoundCardColor.AMBER)
            chip(canvas, 36f, 550.9f, 44f, 10f, -12f, RoundCardColor.CONFETTI_PURPLE)
        }

        val layers = mutableListOf<RoundCardLayer>()

        val alias = RectF(20f, 152f, 370f, 228f)
        layers += layer(alias, delay = 0.0) { canvas ->
            shadowText(canvas, "ALIAS", alias, paint(baloo, 800, s(74f), Color.WHITE), dy = 6f, blur = 0f, alpha = 0.35f)
        }
        val words = RectF(20f, 217f, 370f, 293f)
        layers += layer(words, delay = 0.07) { canvas ->
            shadowText(canvas, "WORDS", words, paint(baloo, 800, s(74f), Color.WHITE), dy = 6f, blur = 0f, alpha = 0.35f)
        }

        layers += layer(RectF(55f, 368f, 335f, 436f), delay = 0.20) { canvas ->
            introRow(
                canvas, y = 368f, fill = RowFill.Solid(RoundCardColor.ROW_BLUE), icon = RowIcon.TEAM,
                badgeFill = withAlpha(Color.WHITE, 0.22f),
                caption = context.getString(R.string.round_intro_team),
                captionColor = withAlpha(Color.WHITE, 0.8f),
                value = info.teamName, valueColor = Color.WHITE,
            )
        }
        layers += layer(RectF(55f, 444f, 335f, 512f), delay = 0.30) { canvas ->
            introRow(
                canvas, y = 444f,
                fill = RowFill.Gradient(0xFFF54A87.toInt(), 0xFFFA8538.toInt()), icon = RowIcon.EXPLAINS,
                badgeFill = withAlpha(Color.WHITE, 0.22f),
                caption = context.getString(R.string.round_intro_explains),
                captionColor = withAlpha(Color.WHITE, 0.85f),
                value = info.explainer, valueColor = Color.WHITE,
            )
        }
        layers += layer(RectF(55f, 520f, 335f, 588f), delay = 0.40) { canvas ->
            introRow(
                canvas, y = 520f, fill = RowFill.Solid(RoundCardColor.YELLOW), icon = RowIcon.GUESSES,
                badgeFill = 0xFFF58C0D.toInt(),
                caption = context.getString(R.string.round_intro_guesses),
                captionColor = withAlpha(RoundCardColor.INK, 0.62f),
                value = info.guesser, valueColor = RoundCardColor.INK,
            )
        }

        return RoundCard(backdrop, layers)
    }

    private sealed interface RowFill {
        class Solid(val color: Int) : RowFill
        class Gradient(val from: Int, val to: Int) : RowFill
    }

    /**
     * The three glyphs are drawn rather than loaded. iOS uses SF Symbols, which have no
     * counterpart here, and pulling in an icon pack for three shapes would cost more than
     * the shapes do.
     */
    private enum class RowIcon { TEAM, EXPLAINS, GUESSES }

    private fun introRow(
        canvas: Canvas,
        y: Float,
        fill: RowFill,
        icon: RowIcon,
        badgeFill: Int,
        caption: String,
        captionColor: Int,
        value: String,
        valueColor: Int,
    ) {
        val rect = scaled(RectF(55f, y, 335f, y + 68f))
        val radius = rect.height() / 2f

        // White border drawn as a fill, then the row colour inset — same reason as the
        // overlay's cards: a stroke lets the fill leak out along the anti-aliased edge.
        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                setShadowLayer(s(12f), 0f, s(8f), withAlpha(0xFF0D2659.toInt(), 0.18f))
            },
        )
        val inner = RectF(rect).apply { inset(s(2f), s(2f)) }
        val innerRadius = inner.height() / 2f
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            when (fill) {
                is RowFill.Solid -> color = fill.color
                is RowFill.Gradient -> shader = LinearGradient(
                    inner.left, inner.centerY(), inner.right, inner.centerY(),
                    fill.from, fill.to, Shader.TileMode.CLAMP,
                )
            }
        }
        canvas.drawRoundRect(inner, innerRadius, innerRadius, fillPaint)

        val badge = scaled(RectF(62f, y + 10f, 106f, y + 54f))
        canvas.drawOval(badge, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeFill })
        rowIcon(canvas, icon, RectF(badge).apply { inset(s(11f), s(11f)) })

        text(
            canvas, caption, scaled(RectF(60f, y + 12f, 330f, y + 26f)),
            paint(jakarta, 800, s(11f), captionColor).apply { letterSpacing = 0.9f / 11f }, center = true,
        )
        text(
            canvas, value, scaled(RectF(60f, y + 28f, 330f, y + 54f)),
            paint(baloo, 800, s(20f), valueColor), center = true, shrinkToFit = true,
        )
    }

    /** Simple geometry, sized to the badge — two heads, a horn, a bulb. */
    private fun rowIcon(canvas: Canvas, icon: RowIcon, rect: RectF) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val w = rect.width()
        val h = rect.height()
        when (icon) {
            RowIcon.TEAM -> {
                // Two overlapping figures: the one behind sits higher and smaller.
                canvas.drawCircle(rect.left + w * 0.32f, rect.top + h * 0.30f, w * 0.17f, paint)
                canvas.drawRoundRect(
                    RectF(rect.left + w * 0.10f, rect.top + h * 0.52f, rect.left + w * 0.54f, rect.bottom),
                    w * 0.20f, w * 0.20f, paint,
                )
                canvas.drawCircle(rect.left + w * 0.70f, rect.top + h * 0.26f, w * 0.15f, paint)
                canvas.drawRoundRect(
                    RectF(rect.left + w * 0.52f, rect.top + h * 0.46f, rect.right, rect.bottom - h * 0.06f),
                    w * 0.18f, w * 0.18f, paint,
                )
            }

            RowIcon.EXPLAINS -> {
                // A megaphone: horn opening to the right, with a handle behind it.
                val horn = Path().apply {
                    moveTo(rect.left + w * 0.08f, rect.top + h * 0.38f)
                    lineTo(rect.left + w * 0.62f, rect.top + h * 0.10f)
                    lineTo(rect.left + w * 0.62f, rect.bottom - h * 0.10f)
                    lineTo(rect.left + w * 0.08f, rect.bottom - h * 0.38f)
                    close()
                }
                canvas.drawPath(horn, paint)
                canvas.drawRoundRect(
                    RectF(rect.left + w * 0.66f, rect.top + h * 0.24f, rect.right, rect.bottom - h * 0.24f),
                    w * 0.10f, w * 0.10f, paint,
                )
            }

            RowIcon.GUESSES -> {
                // A bulb: glass on top, a stubby base under it.
                canvas.drawCircle(rect.centerX(), rect.top + h * 0.36f, w * 0.30f, paint)
                canvas.drawRoundRect(
                    RectF(rect.centerX() - w * 0.16f, rect.top + h * 0.62f,
                        rect.centerX() + w * 0.16f, rect.bottom),
                    w * 0.07f, w * 0.07f, paint,
                )
            }
        }
    }

    // MARK: - Outro

    /**
     * Reads top to bottom: title, wordmark, the two score cards, then the lists, whose rows
     * tick in one by one. The last row lands around 1.5s of the outro's 2s.
     */
    private fun buildOutro(): RoundCard {
        val backdrop = full { canvas ->
            val paint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(RoundCardColor.LIGHT_BLUE, RoundCardColor.BRAND_BLUE, RoundCardColor.DEEP_BLUE),
                    floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            chip(canvas, 21f, 40f, 34f, 10f, 12f, RoundCardColor.CONFETTI_PINK)
            chip(canvas, 328f, 26.7f, 30f, 9f, -14f, RoundCardColor.ORANGE)
            chip(canvas, 341f, 128f, 22f, 7f, 8f, RoundCardColor.CONFETTI_TEAL)
            chip(canvas, 14f, 210.8f, 30f, 9f, -10f, RoundCardColor.CONFETTI_PURPLE)
            chip(canvas, 345.7f, 268f, 26f, 8f, 14f, RoundCardColor.AMBER)
            chip(canvas, 8f, 546.5f, 20f, 6f, -16f, RoundCardColor.CONFETTI_TEAL)
            chip(canvas, 356.3f, 560f, 18f, 5f, 12f, RoundCardColor.CONFETTI_PINK)
            chip(canvas, 60f, 682.6f, 26f, 8f, -12f, RoundCardColor.CONFETTI_PURPLE)
            chip(canvas, 316.5f, 700f, 20f, 6f, 16f, RoundCardColor.AMBER)
        }

        val layers = mutableListOf<RoundCardLayer>()

        // The whole stack drops together, not just the heading: moving the title alone would
        // run it into the wordmark 8pt below. There is room to spare at the bottom.
        val drop = OUTRO_TOP_DROP

        val title = context.getString(R.string.round_outro_title).uppercase()
        val lines = title.split(" ", limit = 2)
        lines.forEachIndexed { index, line ->
            val rect = RectF(20f, 40f + drop + index * 46f, 370f, 40f + drop + index * 46f + 46f)
            layers += layer(rect, delay = 0.08 * index) { canvas ->
                shadowText(canvas, line, rect, paint(baloo, 800, s(42f), Color.WHITE), dy = 3f, blur = 8f, alpha = 0.32f)
            }
        }

        val alias = RectF(75f, 140f + drop, 315f, 192f + drop)
        layers += layer(alias, delay = 0.18) { canvas ->
            shadowText(
                canvas, "ALIAS", alias,
                paint(baloo, 800, s(48f), Color.WHITE).apply { letterSpacing = 1f / 48f },
                dy = 4f, blur = 6f, alpha = 0.35f,
            )
        }
        val words = RectF(75f, 184f + drop, 315f, 222f + drop)
        layers += layer(words, delay = 0.25) { canvas ->
            shadowText(
                canvas, "WORDS", words,
                paint(baloo, 800, s(32f), RoundCardColor.AMBER).apply { letterSpacing = 2f / 32f },
                dy = 3f, blur = 5f, alpha = 0.3f,
            )
        }

        layers += layer(RectF(25f, 246f + drop, 190f, 414f + drop), delay = 0.36) { canvas ->
            statCard(
                canvas, 25f, 246f + drop, RoundCardColor.GREEN, "✓",
                context.getString(R.string.ai_video_correct), info.correctWords.size,
            )
        }
        layers += layer(RectF(203f, 246f + drop, 368f, 414f + drop), delay = 0.44) { canvas ->
            statCard(
                canvas, 203f, 246f + drop, RoundCardColor.ORANGE, "✕",
                context.getString(R.string.round_outro_skipped), info.skippedWords.size,
            )
        }

        addWordList(
            layers, x = 25f, y = 436f + drop, accent = RoundCardColor.GREEN, glyph = "✓",
            title = context.getString(R.string.round_outro_correct_words), words = info.correctWords,
            cardDelay = 0.56, rowDelay = 0.72,
        )
        addWordList(
            layers, x = 203f, y = 439f + drop, accent = RoundCardColor.ORANGE, glyph = "✕",
            title = context.getString(R.string.round_outro_skipped_words), words = info.skippedWords,
            cardDelay = 0.62, rowDelay = 0.78,
        )

        return RoundCard(backdrop, layers)
    }

    /** The card first, then a layer per row so the list fills itself in. */
    private fun addWordList(
        layers: MutableList<RoundCardLayer>,
        x: Float,
        y: Float,
        accent: Int,
        glyph: String,
        title: String,
        words: List<String>,
        cardDelay: Double,
        rowDelay: Double,
    ) {
        val shown = words.take(MAX_WORD_ROWS)
        val listHeight = max(72f, 42f + max(shown.size, 1) * 21f + 8f)
        layers += layer(RectF(x, y, x + 165f, y + listHeight), delay = cardDelay) { canvas ->
            wordListCard(canvas, x, y, listHeight, accent, title)
        }
        shown.forEachIndexed { index, word ->
            val rowY = y + 42f + index * 21f
            // Rows sit inside the card and need no shadow room, so the padding is just
            // enough for the pop's overshoot.
            layers += layer(
                RectF(x + 12f, rowY - 1f, x + 154f, rowY + 18f),
                delay = rowDelay + 0.05 * index,
                motion = RoundCardLayer.Motion.RISE,
                padding = 6f,
            ) { canvas ->
                wordListRow(canvas, x, rowY, accent, glyph, word, divider = index < shown.size - 1)
            }
        }
    }

    private fun statCard(canvas: Canvas, x: Float, y: Float, fill: Int, glyph: String, caption: String, value: Int) {
        val rect = scaled(RectF(x, y, x + 165f, y + 168f))
        val radius = s(26f)
        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                setShadowLayer(s(18f), 0f, s(8f), withAlpha(RoundCardColor.SHADOW, 0.28f))
            },
        )
        val inner = RectF(rect).apply { inset(s(4f), s(4f)) }
        canvas.drawRoundRect(
            inner, radius - s(4f), radius - s(4f),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill },
        )

        val disc = scaled(RectF(x + 56.5f, y + 14f, x + 108.5f, y + 66f))
        canvas.drawOval(disc, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        text(canvas, glyph, disc, paint(jakarta, 800, s(26f), fill), center = true)

        text(
            canvas, caption, scaled(RectF(x, y + 76f, x + 165f, y + 96f)),
            paint(jakarta, 700, s(16f), Color.WHITE), center = true,
        )
        text(
            canvas, value.toString(), scaled(RectF(x, y + 94f, x + 165f, y + 160f)),
            paint(baloo, 800, s(58f), Color.WHITE), center = true,
        )
    }

    /** The empty card: background and heading, no rows — those animate in separately. */
    private fun wordListCard(canvas: Canvas, x: Float, y: Float, listHeight: Float, accent: Int, title: String) {
        val rect = scaled(RectF(x, y, x + 165f, y + listHeight))
        val radius = s(22f)
        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                setShadowLayer(s(16f), 0f, s(6f), withAlpha(RoundCardColor.SHADOW, 0.24f))
            },
        )
        text(
            canvas, title, scaled(RectF(x + 12f, y + 12f, x + 153f, y + 26f)),
            paint(jakarta, 800, s(10f), accent).apply { letterSpacing = 0.4f / 10f },
            center = false, shrinkToFit = true,
        )
        canvas.drawRect(
            scaled(RectF(x + 12f, y + 32f, x + 153f, y + 34f)),
            Paint().apply { color = withAlpha(accent, 0.45f) },
        )
    }

    private fun wordListRow(canvas: Canvas, x: Float, rowY: Float, accent: Int, glyph: String, word: String, divider: Boolean) {
        val icon = scaled(RectF(x + 12f, rowY, x + 27f, rowY + 15f))
        canvas.drawOval(icon, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
        text(canvas, glyph, icon, paint(jakarta, 800, s(9f), Color.WHITE), center = true)
        text(
            canvas, word, scaled(RectF(x + 33f, rowY - 1f, x + 154f, rowY + 16f)),
            paint(jakarta, 600, s(12f), RoundCardColor.INK), center = false, shrinkToFit = true,
        )
        if (divider) {
            canvas.drawRect(
                scaled(RectF(x + 33f, rowY + 17f, x + 154f, rowY + 18f)),
                Paint().apply { color = RoundCardColor.DIVIDER },
            )
        }
    }

    // MARK: - Primitives

    private fun chip(canvas: Canvas, x: Float, y: Float, side: Float, radius: Float, angle: Float, color: Int) {
        canvas.save()
        canvas.translate((x + side / 2f) * scale, (y + side / 2f) * scale)
        canvas.rotate(angle)
        val box = RectF(-s(side) / 2f, -s(side) / 2f, s(side) / 2f, s(side) / 2f)
        canvas.drawRoundRect(
            box, s(radius), s(radius),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                setShadowLayer(s(6f), 0f, s(3f), withAlpha(0xFF0A2E59.toInt(), 0.18f))
            },
        )
        canvas.restore()
    }

    private fun shadowText(canvas: Canvas, value: String, rect: RectF, paint: Paint, dy: Float, blur: Float, alpha: Float) {
        // A zero blur radius is not "no shadow" to Canvas — it is an error. The intro's
        // wordmark asks for a hard offset shadow, so the radius floors just above zero.
        paint.setShadowLayer(max(0.01f, s(blur)), 0f, s(dy), withAlpha(0xFF0A2E59.toInt(), alpha))
        text(canvas, value, scaled(rect), paint, center = true)
    }

    private fun s(value: Float) = value * scale

    private fun scaled(rect: RectF) =
        RectF(rect.left * scale, rect.top * scale, rect.right * scale, rect.bottom * scale)

    private fun withAlpha(color: Int, alpha: Float) =
        Color.argb((alpha * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    private fun paint(face: Typeface?, weight: Int, size: Float, color: Int) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = face
            fontVariationSettings = "'wght' $weight"
            textSize = size
            this.color = color
        }

    /** Largest rect of the source's aspect ratio that covers the whole card. */
    private fun aspectFill(sourceWidth: Float, sourceHeight: Float): RectF {
        if (sourceWidth <= 0f || sourceHeight <= 0f) return RectF(0f, 0f, width.toFloat(), height.toFloat())
        val factor = max(width / sourceWidth, height / sourceHeight)
        val filled = sourceWidth * factor to sourceHeight * factor
        return RectF(
            width / 2f - filled.first / 2f, height / 2f - filled.second / 2f,
            width / 2f + filled.first / 2f, height / 2f + filled.second / 2f,
        )
    }

    private fun text(canvas: Canvas, value: String, rect: RectF, paint: Paint, center: Boolean, shrinkToFit: Boolean = false) {
        // Names and team names are user-entered; shrink rather than overflow the pill.
        if (shrinkToFit) {
            while (paint.measureText(value) > rect.width() && paint.textSize > s(10f)) {
                paint.textSize -= s(1f)
            }
        }
        val metrics = paint.fontMetrics
        val baseline = rect.centerY() - (metrics.ascent + metrics.descent) / 2f
        val x = if (center) rect.centerX() - paint.measureText(value) / 2f else rect.left
        canvas.drawText(value, x, baseline, paint)
    }

    private fun full(body: (Canvas) -> Unit): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        body(Canvas(bitmap))
        return bitmap
    }

    /**
     * Renders one element into a bitmap of its own.
     *
     * The canvas is translated so the body can keep drawing at the same whole-card
     * coordinates every other helper uses — nothing has to know it is being drawn off to one
     * side. The frame is padded because drop shadows fall outside the element's box, and
     * cropping them would leave a visible hard edge.
     *
     * @param rect the element's box in canvas points.
     */
    private fun layer(
        rect: RectF,
        delay: Double,
        motion: RoundCardLayer.Motion = RoundCardLayer.Motion.POP,
        padding: Float = 30f,
        body: (Canvas) -> Unit,
    ): RoundCardLayer {
        val frame = scaled(rect).apply { inset(-s(padding), -s(padding)) }
        val bitmap = Bitmap.createBitmap(
            max(1, frame.width().toInt()), max(1, frame.height().toInt()), Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        canvas.translate(-frame.left, -frame.top)
        body(canvas)
        return RoundCardLayer(bitmap, frame, delay, motion)
    }

    fun release() {
        listOfNotNull(introCard, outroCard).forEach { card ->
            card.backdrop.recycle()
            card.layers.forEach { it.image.recycle() }
        }
        introCard = null
        outroCard = null
    }

    companion object {
        /** Nudges the outro's content down from the design's y positions. */
        const val OUTRO_TOP_DROP = 20f

        /** The list is capped so a long round can't overflow the card. */
        const val MAX_WORD_ROWS = 8
    }
}
