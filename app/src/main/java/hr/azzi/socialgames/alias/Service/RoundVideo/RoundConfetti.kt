package hr.azzi.socialgames.alias.Service.RoundVideo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

/**
 * A burst of confetti from two poppers at the bottom corners, on every third correct answer.
 *
 * Nothing is simulated frame to frame: a particle's position, spin and fade are functions of
 * its burst time and its index, so any frame can be drawn on its own. That is what a writer
 * needs — a dropped frame must not leave the confetti in a different state than it would
 * have been, and there is no state to leave.
 */
class RoundConfetti {

    class Particle(
        val sprite: Int,
        /** Centre in render pixels, top-left origin. */
        val centerX: Float,
        val centerY: Float,
        val scale: Float,
        val rotation: Float,
        val alpha: Float,
    )

    private val scale = RoundOverlayLayout.scale
    private val moments = mutableListOf<Double>()

    /** Drawn once; the renderer uploads them as textures once too. */
    val sprites: List<Bitmap> by lazy { makeSprites() }

    /**
     * Call when a burst should go off — the caller counts correct answers and fires every
     * [BURST_EVERY]th one, so a run of quick answers reads as a few punctuating celebrations
     * rather than a continuous shower.
     */
    fun record(scoreAt: Double) {
        moments += scoreAt
    }

    /**
     * Every chip alive at [time], in round seconds.
     *
     * Newest burst first and capped: a fast run of correct answers would otherwise stack
     * three or four bursts and quietly triple the cost of every frame. Dropping the oldest,
     * already-fading chips is invisible.
     */
    fun particles(time: Double): List<Particle> {
        if (sprites.isEmpty() || moments.isEmpty()) return emptyList()
        val result = mutableListOf<Particle>()
        for (burst in moments.indices.reversed()) {
            val age = (time - moments[burst]).toFloat()
            if (age < 0f || age >= LIFE) continue
            for (index in 0 until PER_BURST) {
                val particle = particle(burst, index, age) ?: continue
                result += particle
                if (result.size >= MAX_ALIVE) return result
            }
        }
        return result
    }

    private fun particle(burst: Int, index: Int, age: Float): Particle? {
        val seed = burst * 977 + index
        // Chips alternate between the two poppers, so a burst is always balanced.
        val fromLeft = index % 2 == 0

        // Fired inwards and up, steeply enough to reach the top of the frame; gravity does
        // the rest.
        val degrees = (if (fromLeft) 52f else 82f) + random(seed, 1) * 46f
        val angle = degrees * Math.PI.toFloat() / 180f
        val speed = 900f + random(seed, 2) * 550f
        val gravity = 1400f

        val originX = if (fromLeft) 26f else RoundVideoSpec.CANVAS_WIDTH - 26f
        val originY = 770f
        // Sideways travel is damped like air drag, so chips fan out and then hang in frame.
        // Undamped they cross the whole width in a quarter second and the burst is over
        // before it reads.
        val drag = 1.7f
        val travel = (1f - exp(-drag * age)) / drag
        // A slow sway on top, so they flutter instead of tracing clean parabolas.
        val sway = sin(age * (3.5f + random(seed, 7) * 3f) + random(seed, 8) * 6f) * 14f
        val x = originX + cos(angle) * speed * travel + sway
        val y = originY - sin(angle) * speed * age + 0.5f * gravity * age * age

        // Chips that have left the frame cost nothing to skip. Above the top is not gone —
        // they come back down.
        if (y >= RoundVideoSpec.CANVAS_HEIGHT + 40f || x <= -50f ||
            x >= RoundVideoSpec.CANVAS_WIDTH + 50f
        ) {
            return null
        }

        val life = LIFE * (0.75f + random(seed, 3) * 0.25f)
        if (age >= life) return null
        val solid = life * 0.55f
        val alpha = if (age < solid) 1f else max(0f, 1f - (age - solid) / (life - solid))

        return Particle(
            sprite = abs(seed) % sprites.size,
            centerX = x * scale,
            centerY = y * scale,
            scale = 0.7f + random(seed, 4) * 0.65f,
            rotation = random(seed, 5) * 2f * Math.PI.toFloat() +
                (random(seed, 6) - 0.5f) * 16f * age,
            alpha = alpha,
        )
    }

    /** Deterministic, so the same frame always renders the same way. */
    private fun random(seed: Int, salt: Int): Float {
        var x = seed.toLong() * 6_364_136_223_846_793_005L + salt.toLong() * 1_442_695_040_888_963_407L
        x = x xor (x ushr 33)
        x *= 0xFF51AFD7ED558CCDuL.toLong()
        x = x xor (x ushr 33)
        x *= 0xC4CEB9FE1A85EC53uL.toLong()
        x = x xor (x ushr 33)
        // Unsigned remainder: the mix is a bit pattern, and a signed modulo would fold half
        // of it onto negative values and bias the result towards zero.
        return java.lang.Long.remainderUnsigned(x, 1000L).toFloat() / 1000f
    }

    /** Six chips: rounded rectangles and a couple of discs, in the card palette. */
    private fun makeSprites(): List<Bitmap> {
        val colors = listOf(
            RoundCardColor.CONFETTI_PINK, RoundCardColor.ORANGE, RoundCardColor.CONFETTI_TEAL,
            RoundCardColor.CONFETTI_PURPLE, RoundCardColor.AMBER, RoundCardColor.CONFETTI_GREEN,
        )
        return colors.mapIndexed { index, color ->
            val disc = index % 3 == 2
            val width = 18f * scale
            val height = (if (disc) 18f else 11f) * scale
            // A pixel of slack all round, so rotation doesn't clip the corners.
            val pad = 2f * scale
            val bitmap = Bitmap.createBitmap(
                (width + pad * 2).toInt(), (height + pad * 2).toInt(), Bitmap.Config.ARGB_8888,
            )
            val canvas = Canvas(bitmap)
            val rect = RectF(pad, pad, pad + width, pad + height)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
            if (disc) canvas.drawOval(rect, paint)
            else canvas.drawRoundRect(rect, 2f * scale, 2f * scale, paint)
            bitmap
        }
    }

    fun release() {
        sprites.forEach { it.recycle() }
        moments.clear()
    }

    companion object {
        /** Correct answers between bursts: one goes off on the 3rd, 6th, 9th and so on. */
        const val BURST_EVERY = 3

        /** Chips per burst, split between the two poppers. */
        private const val PER_BURST = 30

        /** How long a chip lives, before its own random shortening. */
        private const val LIFE = 1.8f

        /**
         * Ceiling on chips in one frame, however many bursts overlap. This is what keeps a
         * run of quick answers from pushing a frame past the time it has.
         */
        private const val MAX_ALIVE = 48
    }
}
