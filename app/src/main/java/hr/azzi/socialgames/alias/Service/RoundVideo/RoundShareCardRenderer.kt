package hr.azzi.socialgames.alias.Service.RoundVideo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import hr.azzi.socialgames.alias.R
import kotlin.math.max

/** What sits between the wordmark and the score tiles. */
sealed interface RoundShareHeader {
    /** The local game: team name, who explained, who guessed. */
    data object Team : RoundShareHeader

    /**
     * AI play, where there is no team — the player's own mascot instead. A null image still
     * draws the disc in the mascot's tint, so one that failed to load leaves the layout
     * intact rather than a hole.
     */
    data class Mascot(val image: Bitmap?, val tint: Int) : RoundShareHeader

    /**
     * Draw & Guess, where the round produced one thing worth sharing. The word goes on top
     * — without it the picture is a riddle to everyone who wasn't there — and the drawing
     * fills the rest. A null image falls back to the score card rather than framing an
     * empty sheet of paper.
     */
    data class Drawing(val image: Bitmap?, val word: String) : RoundShareHeader
}

/**
 * The shareable round-result image: wordmark, the two score tiles and the word lists on the
 * brand gradient, headed either by the team card (Figma node 3333:1712, the local game) or by
 * the player's mascot (node 3340:645, AI play).
 *
 * A sibling of [RoundCardRenderer] — same design language, same Canvas approach — but a
 * single still bitmap instead of animatable layers, and the canvas grows with the word lists
 * so a long round isn't cropped.
 */
class RoundShareCardRenderer(
    private val context: Context,
    private val info: RoundCardInfo,
    private val header: RoundShareHeader = RoundShareHeader.Team,
) {

    /** Design width; every coordinate below is in these points and scaled on the way out. */
    private val canvasWidth = 390f
    private val designHeight = 844f

    /** Exported width in pixels — big enough for stories and messaging apps. */
    private val outputWidth = 1080f

    private val scale = outputWidth / canvasWidth

    private val baloo: Typeface? by lazy { ResourcesCompat.getFont(context, R.font.baloo2) }
    private val jakarta: Typeface? by lazy { ResourcesCompat.getFont(context, R.font.plusjakartasans) }

    // MARK: - Layout

    private val aliasY = 58f
    private val wordsY = 116f
    private val wordmarkSize = 62f
    private val teamCardRect = RectF(24f, 190f, 366f, 322f)

    /** The mascot disc occupies the same band as the team card, centred. */
    private val mascotRect = RectF(129f, 190f, 261f, 322f)
    private val statY = 336f
    private val statWidth = 165f
    private val statHeight = 120f
    private val listY = 470f
    private val listWidth = 165f

    /** Distance from a list's top to its first row icon. */
    private val listHeader = 47f
    private val rowPitch = 28f
    private val listPadding = 5f

    private val correctRows = rows(info.correctWords)
    private val skippedRows = rows(info.skippedWords)

    /**
     * Beyond [MAX_ROWS] a list stops listing and ends with a "+N" row: an image that grows
     * without limit stops being shareable.
     */
    private fun rows(words: List<RoundCardWord>): List<RoundCardWord> =
        if (words.size <= MAX_ROWS) words
        // The overflow row counts words, not points, so it carries no value of its own.
        else words.take(MAX_ROWS - 1) + RoundCardWord("+${words.size - (MAX_ROWS - 1)}")

    private fun listHeight(rowCount: Int) = listHeader + max(rowCount, 1) * rowPitch + listPadding

    /** Tall enough for the longer of the two lists, never shorter than the design. */
    private val canvasHeight: Float
        get() = max(
            designHeight,
            listY + max(listHeight(correctRows.size), listHeight(skippedRows.size)) + 46f,
        )

    // MARK: - Draw & Guess

    /**
     * The word, then the drawing on white paper. Everything the talking card says with
     * tiles and lists, this round says with one picture.
     */
    private fun drawnRound(canvas: Canvas, drawing: Bitmap, word: String) {
        shadowText(canvas, word.uppercase(), RectF(20f, 188f, 370f, 254f), 48f)

        val paper = RectF(24f, 268f, 366f, 768f)
        val scaledPaper = scaled(paper)
        val radius = 30f * scale
        canvas.drawRoundRect(
            scaledPaper, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                setShadowLayer(28f * scale, 0f, 10f * scale, withAlpha(Color.BLACK, 0.22f))
            },
        )

        // Aspect-fit inside the paper. The drawing was made on a canvas of a different
        // shape from this one, and stretching it would bend every line drawn straight.
        val inset = 18f * scale
        val box = RectF(
            scaledPaper.left + inset, scaledPaper.top + inset,
            scaledPaper.right - inset, scaledPaper.bottom - inset,
        )
        val ratio = minOf(box.width() / drawing.width, box.height() / drawing.height)
        val w = drawing.width * ratio
        val hgt = drawing.height * ratio
        val dst = RectF(
            box.centerX() - w / 2f, box.centerY() - hgt / 2f,
            box.centerX() + w / 2f, box.centerY() + hgt / 2f,
        )
        canvas.drawBitmap(drawing, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    // MARK: - Render

    fun render(): Bitmap {
        val height = canvasHeight
        val bitmap = Bitmap.createBitmap(
            outputWidth.toInt(), (height * scale).toInt(), Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)

        background(canvas, height)

        shadowText(canvas, "ALIAS", RectF(20f, aliasY, 370f, aliasY + 60f), wordmarkSize)
        shadowText(canvas, "WORDS", RectF(20f, wordsY, 370f, wordsY + 60f), wordmarkSize)

        val h = header
        if (h is RoundShareHeader.Drawing && h.image != null) {
            drawnRound(canvas, h.image, h.word)
            return bitmap
        }
        when (h) {
            is RoundShareHeader.Team -> teamCard(canvas)
            is RoundShareHeader.Mascot -> mascotDisc(canvas, h.image, h.tint)
            // A drawing round with nothing drawn: fall through to the score card.
            is RoundShareHeader.Drawing -> teamCard(canvas)
        }

        statCard(
            canvas, 24f, RoundCardColor.GREEN, "✓",
            context.getString(R.string.ai_video_correct).uppercase(), info.correctTotal,
        )
        statCard(
            canvas, 201f, RoundCardColor.ORANGE, "✕",
            context.getString(R.string.round_outro_skipped).uppercase(), info.skippedWords.size,
        )

        wordList(
            canvas, 25f, RoundCardColor.GREEN, "✓",
            context.getString(R.string.round_outro_correct_words), correctRows,
        )
        wordList(
            canvas, 203f, RoundCardColor.ORANGE, "✕",
            context.getString(R.string.round_outro_skipped_words), skippedRows,
            mutedPoints = true,
        )

        return bitmap
    }

    // MARK: - Background

    private fun background(canvas: Canvas, height: Float) {
        val paint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height * scale,
                intArrayOf(RoundCardColor.LIGHT_BLUE, RoundCardColor.BRAND_BLUE, RoundCardColor.DEEP_BLUE),
                floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, outputWidth, height * scale, paint)

        // Confetti. The top set keeps the design's y; the bottom set hangs off the bottom
        // edge so it stays in the corners however tall the lists made the card.
        chip(canvas, 20.9f, 40f, 34f, 10f, 12f, RoundCardColor.CONFETTI_PINK)
        chip(canvas, 328f, 26.7f, 30f, 9f, -14f, RoundCardColor.ORANGE)
        chip(canvas, 340.9f, 128f, 22f, 7f, 8f, RoundCardColor.CONFETTI_TEAL)
        chip(canvas, 345.7f, 268f, 26f, 8f, 14f, RoundCardColor.AMBER)
        chip(canvas, 0f, 464.4f, 18f, 5f, -18f, RoundCardColor.CONFETTI_PINK)
        chip(canvas, 372f, 468.5f, 14f, 4f, -6f, RoundCardColor.CONFETTI_PURPLE)

        // Offsets measured up from the design's 844pt bottom edge.
        chip(canvas, 0f, height - 297.5f, 20f, 6f, -16f, RoundCardColor.CONFETTI_TEAL)
        chip(canvas, 364.3f, height - 284f, 18f, 5f, 12f, RoundCardColor.CONFETTI_PINK)
        chip(canvas, 240f, height - 169.2f, 30f, 9f, -10f, RoundCardColor.CONFETTI_PURPLE)
        chip(canvas, 329.2f, height - 114f, 16f, 5f, 10f, RoundCardColor.ORANGE)
        chip(canvas, 206.5f, height - 97f, 14f, 4f, 6f, RoundCardColor.CONFETTI_GREEN)
        chip(canvas, 60f, height - 57.4f, 26f, 8f, -12f, RoundCardColor.CONFETTI_PURPLE)
        chip(canvas, 294.5f, height - 52f, 20f, 6f, 16f, RoundCardColor.AMBER)
    }

    // MARK: - Team card

    private fun teamCard(canvas: Canvas) {
        val card = teamCardRect
        val rect = scaled(card)
        val radius = s(30f)
        val inner = RectF(rect).apply { inset(s(3f), s(3f)) }
        val innerRadius = radius - s(3f)

        // The card is a white ring around a frosted interior. Filling the whole shape white
        // and then laying 16% white on top would just give white again, so the interior is
        // clipped away for the border pass — which also keeps the drop shadow from washing
        // out the middle.
        canvas.save()
        canvas.clipOutPath(Path().apply { addRoundRect(inner, innerRadius, innerRadius, Path.Direction.CW) })
        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                setShadowLayer(s(22f), 0f, s(10f), withAlpha(RoundCardColor.SHADOW, 0.28f))
            },
        )
        canvas.restore()

        canvas.drawRoundRect(
            inner, innerRadius, innerRadius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(Color.WHITE, 0.16f) },
        )

        // Team row.
        val badge = RectF(card.left + 17f, card.top + 12f, card.left + 61f, card.top + 56f)
        canvas.drawOval(scaled(badge), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(Color.WHITE, 0.22f) })
        roleIcon(canvas, RoleIcon.TEAM, scaled(badge).apply { inset(s(11f), s(11f)) })
        text(
            canvas, info.teamName,
            scaled(RectF(card.left + 75f, card.top + 12f, card.left + 317f, card.top + 56f)),
            paint(baloo, 800, s(30f), Color.WHITE), center = false, shrinkToFit = true,
        )

        canvas.drawRect(
            scaled(RectF(card.left + 17f, card.top + 66f, card.left + 319f, card.top + 68f)),
            Paint().apply { color = withAlpha(Color.WHITE, 0.32f) },
        )
        canvas.drawRect(
            scaled(RectF(card.left + 167f, card.top + 78f, card.left + 169f, card.top + 120f)),
            Paint().apply { color = withAlpha(Color.WHITE, 0.22f) },
        )

        role(
            canvas, badgeX = card.left + 19f, textX = card.left + 71f, y = card.top,
            fill = RoundCardColor.CONFETTI_PINK, icon = RoleIcon.EXPLAINS,
            caption = context.getString(R.string.round_intro_explains) + ":",
            captionColor = RoundCardColor.CONFETTI_PINK, value = info.explainer,
        )
        role(
            canvas, badgeX = card.left + 171f, textX = card.left + 223f, y = card.top,
            fill = RoundCardColor.YELLOW, icon = RoleIcon.GUESSES,
            caption = context.getString(R.string.round_intro_guesses) + ":",
            captionColor = RoundCardColor.YELLOW, value = info.guesser,
        )
    }

    private fun role(
        canvas: Canvas,
        badgeX: Float,
        textX: Float,
        y: Float,
        fill: Int,
        icon: RoleIcon,
        caption: String,
        captionColor: Int,
        value: String,
    ) {
        val badge = RectF(badgeX, y + 78f, badgeX + 42f, y + 120f)
        canvas.drawOval(scaled(badge), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill })
        roleIcon(canvas, icon, scaled(badge).apply { inset(s(11f), s(11f)) })
        text(
            canvas, caption, scaled(RectF(textX, y + 79f, textX + 96f, y + 93f)),
            paint(jakarta, 800, s(11f), captionColor).apply { letterSpacing = 0.9f / 11f },
            center = false, shrinkToFit = true,
        )
        text(
            canvas, value, scaled(RectF(textX, y + 94f, textX + 96f, y + 120f)),
            paint(jakarta, 800, s(20f), Color.WHITE), center = false, shrinkToFit = true,
        )
    }

    // MARK: - Mascot disc

    /**
     * A pale disc with a white rim, the mascot fitted inside it and clipped to the circle so
     * a tall artwork can't spill over the rim.
     */
    private fun mascotDisc(canvas: Canvas, image: Bitmap?, tint: Int) {
        val rect = scaled(mascotRect)
        canvas.drawOval(
            rect,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                setShadowLayer(s(20f), 0f, s(9f), withAlpha(RoundCardColor.SHADOW, 0.28f))
            },
        )
        val inner = RectF(rect).apply { inset(s(4f), s(4f)) }
        canvas.drawOval(inner, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint })

        if (image == null || image.width <= 0 || image.height <= 0) return
        // The mascots are taller than they are wide and nearly fill the disc, so the artwork
        // is fitted to the inner circle's box rather than inscribed in it.
        val box = RectF(inner).apply { inset(s(6f), s(3f)) }
        val factor = minOf(box.width() / image.width, box.height() / image.height)
        val w = image.width * factor
        val h = image.height * factor
        val target = RectF(
            inner.centerX() - w / 2f, inner.centerY() - h / 2f,
            inner.centerX() + w / 2f, inner.centerY() + h / 2f,
        )
        canvas.save()
        canvas.clipPath(Path().apply { addOval(inner, Path.Direction.CW) })
        canvas.drawBitmap(image, null, target, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    }

    // MARK: - Stat tiles

    private fun statCard(canvas: Canvas, x: Float, fill: Int, glyph: String, caption: String, value: Int) {
        val card = RectF(x, statY, x + statWidth, statY + statHeight)
        val rect = scaled(card)
        val radius = s(26f)

        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                setShadowLayer(s(18f), 0f, s(8f), withAlpha(RoundCardColor.SHADOW, 0.28f))
            },
        )
        val inner = RectF(rect).apply { inset(s(4f), s(4f)) }
        canvas.drawRoundRect(inner, radius - s(4f), radius - s(4f), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill })

        val icon = scaled(RectF(card.left + 10f, card.top + 10f, card.left + 48f, card.top + 48f))
        canvas.drawOval(icon, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        text(canvas, glyph, icon, paint(jakarta, 800, s(20f), fill), center = true)

        text(
            canvas, caption,
            scaled(RectF(card.left + 54f, card.top + 10f, card.left + 155f, card.top + 48f)),
            paint(jakarta, 800, s(12f), Color.WHITE).apply { letterSpacing = 0.4f / 12f },
            center = false, shrinkToFit = true,
        )

        dashed(
            canvas, card.left + 12f, card.top + 56f, 133f,
            withAlpha(Color.WHITE, 0.55f), 2f, floatArrayOf(3f, 4f),
        )

        text(
            canvas, value.toString(),
            scaled(RectF(card.left, card.top + 56f, card.left + card.width(), card.top + 116f)),
            paint(baloo, 800, s(56f), Color.WHITE).apply {
                setShadowLayer(s(6f), 0f, s(4f), withAlpha(0xFF0A2E59.toInt(), 0.32f))
            },
            center = true,
        )
    }

    // MARK: - Word lists

    private fun wordList(
        canvas: Canvas, x: Float, accent: Int, glyph: String, title: String,
        words: List<RoundCardWord>, mutedPoints: Boolean = false,
    ) {
        val card = RectF(x, listY, x + listWidth, listY + listHeight(words.size))
        val rect = scaled(card)
        val radius = s(22f)

        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                setShadowLayer(s(16f), 0f, s(6f), withAlpha(RoundCardColor.SHADOW, 0.24f))
            },
        )

        text(
            canvas, title, scaled(RectF(card.left + 14f, card.top + 12f, card.left + 154f, card.top + 28f)),
            paint(jakarta, 800, s(11.5f), accent).apply { letterSpacing = 0.3f / 11.5f },
            center = false, shrinkToFit = true,
        )
        canvas.drawRect(
            scaled(RectF(card.left + 14f, card.top + 34f, card.left + 151f, card.top + 36f)),
            Paint().apply { color = withAlpha(accent, 0.5f) },
        )

        words.forEachIndexed { index, word ->
            val iconY = card.top + listHeader + index * rowPitch
            val icon = scaled(RectF(card.left + 14f, iconY, card.left + 33f, iconY + 19f))
            canvas.drawOval(icon, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
            text(canvas, glyph, icon, paint(jakarta, 800, s(11f), Color.WHITE), center = true)
            // A valued word gives up its right end to the chip; the rest keep the full width.
            val textRight = if (word.points == null) 153f else 129f
            text(
                canvas, word.text,
                scaled(RectF(card.left + 41f, iconY - 1f, card.left + textRight, iconY + 20f)),
                paint(jakarta, 600, s(13.5f), RoundCardColor.INK), center = false, shrinkToFit = true,
            )
            word.points?.let {
                pointsChip(canvas, RectF(card.left + 133f, iconY - 1f, card.left + 153f, iconY + 20f),
                    it, mutedPoints)
            }
            dashed(canvas, card.left + 41f, iconY + 23f, 112f, RoundCardColor.DIVIDER, 1.5f, floatArrayOf(2f, 3f))
        }
    }

    /**
     * What the word was worth, in the tier's colour on a soft tint of itself — the same badge
     * the Edit Answers list shows next to the word. `muted` is the skipped list: the value
     * still reads, but faded, because none of it was earned.
     */
    private fun pointsChip(canvas: Canvas, rect: RectF, points: Int, muted: Boolean) {
        val tint = RoundCardColor.tier(points)
        val box = scaled(rect)
        canvas.drawRoundRect(box, box.height() / 2f, box.height() / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tint
            alpha = if (muted) 26 else 41
        })
        text(canvas, "$points", box, paint(jakarta, 800, s(12.5f), tint).apply {
            if (muted) alpha = 140
        }, center = true)
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

    private fun dashed(canvas: Canvas, x: Float, y: Float, length: Float, color: Int, width: Float, pattern: FloatArray) {
        canvas.drawLine(
            x * scale, y * scale, (x + length) * scale, y * scale,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = s(width)
                pathEffect = DashPathEffect(pattern.map { s(it) }.toFloatArray(), 0f)
            },
        )
    }

    /**
     * The three glyphs are drawn rather than loaded, same as the video's intro rows: iOS uses
     * SF Symbols, which have no counterpart here, and an icon pack for three shapes would
     * cost more than the shapes do.
     */
    private enum class RoleIcon { TEAM, EXPLAINS, GUESSES }

    /** Simple geometry, sized to the badge — two heads, a horn, a bulb. */
    private fun roleIcon(canvas: Canvas, icon: RoleIcon, rect: RectF) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val w = rect.width()
        val h = rect.height()
        when (icon) {
            RoleIcon.TEAM -> {
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

            RoleIcon.EXPLAINS -> {
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

            RoleIcon.GUESSES -> {
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

    private fun shadowText(canvas: Canvas, value: String, rect: RectF, size: Float) {
        // A zero blur radius is not "no shadow" to Canvas — it is an error, so the hard
        // offset the wordmark asks for floors just above zero.
        val paint = paint(baloo, 800, s(size), Color.WHITE).apply {
            setShadowLayer(0.01f, 0f, s(6f), withAlpha(0xFF0A2E59.toInt(), 0.35f))
        }
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

    private fun text(canvas: Canvas, value: String, rect: RectF, paint: Paint, center: Boolean, shrinkToFit: Boolean = false) {
        // Names and words are user-entered; shrink rather than overflow the card.
        if (shrinkToFit) {
            while (paint.measureText(value) > rect.width() && paint.textSize > s(9f)) {
                paint.textSize -= s(1f)
            }
        }
        val metrics = paint.fontMetrics
        val baseline = rect.centerY() - (metrics.ascent + metrics.descent) / 2f
        val x = if (center) rect.centerX() - paint.measureText(value) / 2f else rect.left
        canvas.drawText(value, x, baseline, paint)
    }

    companion object {
        /** The lists are capped so a long round can't grow the image out of usefulness. */
        const val MAX_ROWS = 16
    }
}
