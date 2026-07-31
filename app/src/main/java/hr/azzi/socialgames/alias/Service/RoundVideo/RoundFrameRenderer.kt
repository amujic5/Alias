package hr.azzi.socialgames.alias.Service.RoundVideo

import android.graphics.Bitmap
import android.graphics.RectF
import android.opengl.GLES11Ext
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Builds one frame of the round video: back camera full-bleed, front camera in the rounded
 * PiP window. The overlay, the cards and the confetti go on top of this in later passes.
 *
 * Ported from the iOS `RoundFrameRenderer`, with Core Image's graph replaced by a handful
 * of GL draws. The shape is the same and for the same reason: every expensive thing is
 * built once, and a frame is only ever composites over things that already exist.
 */
class RoundFrameRenderer {

    /** What the overlay shows at a given moment. */
    data class State(
        val word: String,
        val correct: Int,
        val skipped: Int,
        val remainingSeconds: Int,
    )

    /** Which card, if any, covers this frame. */
    sealed interface Phase {
        /** The intro card alone. No camera frame is needed — it is opaque. */
        class Intro(val elapsed: Double) : Phase

        /** The round, with the intro card dissolving over it while [fade] runs 0 → 1. */
        class Round(val time: Double, val fade: Float) : Phase

        class Outro(val elapsed: Double) : Phase
    }

    private var program = 0
    private var positionHandle = 0
    private var quadHandle = 0
    private var texMatrixHandle = 0
    private var textureHandle = 0
    private var halfSizeHandle = 0
    private var radiusHandle = 0
    private var borderHandle = 0

    /** The overlay's own program: plain 2D textures, no external sampler, no transform. */
    private var overlayProgram = 0
    private var overlayPositionHandle = 0
    private var overlayQuadHandle = 0
    private var overlayTextureHandle = 0
    private var overlayAlphaHandle = 0

    private val textures = GlTextureCache()

    /**
     * What is currently being drawn into. The clip and the on-screen preview are different
     * sizes, and every quad is placed in the target's own pixels, so the conversion to clip
     * space has to know which one it is working in.
     */
    private var targetWidth = RoundVideoSpec.RENDER_WIDTH.toFloat()
    private var targetHeight = RoundVideoSpec.RENDER_HEIGHT.toFloat()

    private val vertices: FloatBuffer = ByteBuffer.allocateDirect(4 * 2 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val quadCoords: FloatBuffer = ByteBuffer.allocateDirect(4 * 2 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            // Top-left origin, matching the layout's space.
            put(floatArrayOf(0f, 0f, 0f, 1f, 1f, 0f, 1f, 1f))
            position(0)
        }

    fun prepare() {
        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        quadHandle = GLES20.glGetAttribLocation(program, "aQuadCoord")
        texMatrixHandle = GLES20.glGetUniformLocation(program, "uTexMatrix")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        halfSizeHandle = GLES20.glGetUniformLocation(program, "uHalfSize")
        radiusHandle = GLES20.glGetUniformLocation(program, "uRadius")
        borderHandle = GLES20.glGetUniformLocation(program, "uBorder")

        overlayProgram = buildProgram(VERTEX_SHADER, OVERLAY_FRAGMENT_SHADER)
        overlayPositionHandle = GLES20.glGetAttribLocation(overlayProgram, "aPosition")
        overlayQuadHandle = GLES20.glGetAttribLocation(overlayProgram, "aQuadCoord")
        overlayTextureHandle = GLES20.glGetUniformLocation(overlayProgram, "uTexture")
        overlayAlphaHandle = GLES20.glGetUniformLocation(overlayProgram, "uAlpha")

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    /**
     * Draws one composited frame. The back feed covers the frame by itself, so there is no
     * clear-then-paint — the flat colour is only there for the frames before the back
     * camera has delivered anything.
     */
    fun render(
        phase: Phase,
        back: CameraTexture,
        front: CameraTexture,
        overlay: RoundOverlayRenderer,
        confetti: RoundConfetti,
        intro: RoundCard?,
        outro: RoundCard?,
        state: State,
    ) {
        targetWidth = RoundVideoSpec.RENDER_WIDTH.toFloat()
        targetHeight = RoundVideoSpec.RENDER_HEIGHT.toFloat()
        GLES20.glViewport(0, 0, RoundVideoSpec.RENDER_WIDTH, RoundVideoSpec.RENDER_HEIGHT)

        when (phase) {
            is Phase.Intro -> {
                if (intro != null) card(intro, phase.elapsed, exit = 0f, cardAlpha = 1f)
            }

            is Phase.Outro -> {
                if (outro != null) card(outro, phase.elapsed, exit = 0f, cardAlpha = 1f)
            }

            is Phase.Round -> {
                roundFrame(back, front, overlay, confetti, state, phase.time)
                // The card dissolves over live footage rather than into black, so the round
                // is already underneath by the time it starts clearing.
                if (phase.fade < 1f && intro != null) {
                    card(
                        intro,
                        elapsed = RoundVideoSpec.INTRO_HOLD_S + phase.fade * RoundVideoSpec.INTRO_FADE_S,
                        exit = phase.fade,
                        cardAlpha = 1f - phase.fade,
                    )
                }
            }
        }
    }

    /**
     * The same two feeds, drawn for the screen instead of for the file.
     *
     * The preview is not the clip scaled down: the play screen's stage is a different shape
     * from a 9:19.5 video, and it carries none of the clip's overlay — the labels, the word
     * plate and the timer are Compose, drawn over this. What is shared is the pipeline, which
     * is the point: the previews cost two more draws on frames the compositor is already
     * handling, rather than two more camera streams the concurrent configuration might not
     * survive.
     */
    fun renderPreview(back: CameraTexture, front: CameraTexture, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        targetWidth = width.toFloat()
        targetHeight = height.toFloat()
        GLES20.glViewport(0, 0, width, height)

        val full = RectF(0f, 0f, width.toFloat(), height.toFloat())
        GLES20.glUseProgram(program)
        if (back.hasFrame) {
            draw(back, full, radius = 0f)
        } else {
            GLES20.glClearColor(0.176f, 0.216f, 0.267f, 1f) // the stage's own grey
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        }

        if (front.hasFrame) {
            // The design's PiP, as fractions of its 358×514 stage, so it lands in the same
            // place whatever the screen is.
            val window = RectF(
                PREVIEW_PIP_X * width, PREVIEW_PIP_Y * height,
                (PREVIEW_PIP_X + PREVIEW_PIP_WIDTH) * width,
                (PREVIEW_PIP_Y + PREVIEW_PIP_HEIGHT) * height,
            )
            draw(front, window, radius = PREVIEW_PIP_RADIUS * width, border = PREVIEW_PIP_BORDER * width)
        }
    }

    private fun roundFrame(
        back: CameraTexture,
        front: CameraTexture,
        overlay: RoundOverlayRenderer,
        confetti: RoundConfetti,
        state: State,
        time: Double,
    ) {
        val full = RectF(
            0f, 0f,
            RoundVideoSpec.RENDER_WIDTH.toFloat(), RoundVideoSpec.RENDER_HEIGHT.toFloat(),
        )
        GLES20.glUseProgram(program)
        if (back.hasFrame) {
            draw(back, full, radius = 0f)
        } else {
            GLES20.glClearColor(0.055f, 0.09f, 0.149f, 1f) // RoundOverlayColor.SHADOW
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        }

        // The card the PiP feed sits on has to be under the feed, so the chrome is split in
        // two rather than drawn as one layer.
        stamp(overlay.chromeBelow, full)

        if (front.hasFrame) {
            GLES20.glUseProgram(program)
            val window = RoundOverlayLayout.scaled(RoundOverlayLayout.pipFeed)
            draw(front, window, radius = RoundOverlayLayout.PIP_FEED_RADIUS * RoundOverlayLayout.scale)
        }

        stamp(overlay.chromeAbove, full)

        // Only these four change during a round, and each is cached per value.
        stamp(overlay.timer(state.remainingSeconds), overlay.rectFor(RoundOverlayRenderer.Patch.TIMER))
        stamp(overlay.correct(state.correct), overlay.rectFor(RoundOverlayRenderer.Patch.CORRECT))
        stamp(overlay.missed(state.skipped), overlay.rectFor(RoundOverlayRenderer.Patch.MISSED))
        if (state.word.isNotEmpty()) {
            stamp(overlay.word(state.word), overlay.rectFor(RoundOverlayRenderer.Patch.WORD))
        }

        // In front of everything — it's a celebration, not part of the furniture.
        for (particle in confetti.particles(time)) {
            val sprite = confetti.sprites.getOrNull(particle.sprite) ?: continue
            stamp(
                sprite,
                centerX = particle.centerX,
                centerY = particle.centerY,
                width = sprite.width * particle.scale,
                height = sprite.height * particle.scale,
                rotation = particle.rotation,
                alpha = particle.alpha,
            )
        }
    }

    /**
     * Backdrop plus every layer at its position for [elapsed].
     *
     * @param exit how far the card is through its dissolve. Layers pull away slightly and
     *   clear out ahead of the backdrop, so the round underneath is revealed rather than the
     *   card simply going translucent.
     * @param cardAlpha the whole card's opacity. Applied per element rather than by
     *   flattening the card first: an offscreen pass per frame would cost more than the
     *   difference is worth over a 0.6 s dissolve.
     */
    private fun card(card: RoundCard, elapsed: Double, exit: Float, cardAlpha: Float) {
        stamp(
            card.backdrop,
            RectF(0f, 0f, RoundVideoSpec.RENDER_WIDTH.toFloat(), RoundVideoSpec.RENDER_HEIGHT.toFloat()),
            alpha = cardAlpha,
        )

        card.layers.forEachIndexed { index, layer ->
            val progress = clamp01(((elapsed - layer.delay) / layer.motion.duration).toFloat())

            // Leaving, staggered in the order the layers arrived and finished before the
            // backdrop is: the card lifts off rather than merely going translucent.
            val exitDelay = min(0.30f, 0.06f * index)
            val leaving = clamp01((exit - exitDelay) / max(0.001f, 0.85f - exitDelay))

            // Fades in over the first 40% of the move, so nothing appears full-strength at
            // its smallest.
            var alpha = clamp01(progress * 2.5f) * (1f - easeInQuad(leaving))
            if (alpha <= 0.004f) return@forEachIndexed

            var scale = 1f
            // Positive sits the layer lower on screen.
            var offset = 0f
            when (layer.motion) {
                RoundCardLayer.Motion.POP -> scale = 0.62f + 0.38f * easeOutBack(progress)
                RoundCardLayer.Motion.RISE ->
                    offset = (1f - easeOutCubic(progress)) * RoundVideoSpec.RENDER_HEIGHT * 0.018f
            }
            offset -= easeInCubic(leaving) * RoundVideoSpec.RENDER_HEIGHT * 0.24f
            alpha = min(1f, alpha) * cardAlpha

            stamp(
                layer.image,
                centerX = layer.frame.centerX(),
                centerY = layer.frame.centerY() + offset,
                width = layer.frame.width() * scale,
                height = layer.frame.height() * scale,
                rotation = 0f,
                alpha = alpha,
            )
        }
    }

    /** Draws a bitmap at its place in the layout. */
    private fun stamp(bitmap: Bitmap, destination: RectF, alpha: Float = 1f) {
        drawBitmap(bitmap, alpha) { putQuad(destination) }
    }

    /** Draws a bitmap turned and scaled about its own centre — card layers and confetti. */
    private fun stamp(
        bitmap: Bitmap,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotation: Float,
        alpha: Float,
    ) {
        drawBitmap(bitmap, alpha) { putQuad(centerX, centerY, width, height, rotation) }
    }

    private inline fun drawBitmap(bitmap: Bitmap, alpha: Float, geometry: () -> Unit) {
        GLES20.glUseProgram(overlayProgram)
        geometry()

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures.textureFor(bitmap))
        GLES20.glUniform1i(overlayTextureHandle, 0)
        GLES20.glUniform1f(overlayAlphaHandle, alpha)

        GLES20.glEnableVertexAttribArray(overlayPositionHandle)
        GLES20.glVertexAttribPointer(overlayPositionHandle, 2, GLES20.GL_FLOAT, false, 0, vertices)
        GLES20.glEnableVertexAttribArray(overlayQuadHandle)
        GLES20.glVertexAttribPointer(overlayQuadHandle, 2, GLES20.GL_FLOAT, false, 0, quadCoords)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(overlayPositionHandle)
        GLES20.glDisableVertexAttribArray(overlayQuadHandle)
    }

    private fun draw(camera: CameraTexture, destination: RectF, radius: Float, border: Float = 0f) {
        putQuad(destination)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, camera.textureId)
        GLES20.glUniform1i(textureHandle, 0)
        GLES20.glUniformMatrix4fv(texMatrixHandle, 1, false, camera.samplingMatrix(destination), 0)
        GLES20.glUniform2f(halfSizeHandle, destination.width() / 2f, destination.height() / 2f)
        GLES20.glUniform1f(radiusHandle, radius)
        GLES20.glUniform1f(borderHandle, border)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertices)
        GLES20.glEnableVertexAttribArray(quadHandle)
        GLES20.glVertexAttribPointer(quadHandle, 2, GLES20.GL_FLOAT, false, 0, quadCoords)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(quadHandle)
    }

    /** Layout rect (top-left origin, pixels) → the clip-space quad GL draws. */
    private fun putQuad(rect: RectF) {
        val width = targetWidth
        val height = targetHeight
        val left = rect.left / width * 2f - 1f
        val right = rect.right / width * 2f - 1f
        val top = 1f - rect.top / height * 2f
        val bottom = 1f - rect.bottom / height * 2f
        vertices.position(0)
        vertices.put(floatArrayOf(left, top, left, bottom, right, top, right, bottom))
        vertices.position(0)
    }

    /**
     * The same quad, turned and scaled about its centre. The corners are worked out in pixel
     * space and converted once, rather than handed to the vertex shader as a matrix: there
     * are four of them, and it keeps the shader shared with the axis-aligned path.
     */
    private fun putQuad(centerX: Float, centerY: Float, width: Float, height: Float, rotation: Float) {
        val renderWidth = targetWidth
        val renderHeight = targetHeight
        val cos = cos(rotation)
        val sin = sin(rotation)
        val halfWidth = width / 2f
        val halfHeight = height / 2f

        // Top-left, bottom-left, top-right, bottom-right — the strip's order.
        val corners = floatArrayOf(
            -halfWidth, -halfHeight,
            -halfWidth, halfHeight,
            halfWidth, -halfHeight,
            halfWidth, halfHeight,
        )
        vertices.position(0)
        var index = 0
        while (index < corners.size) {
            val x = centerX + corners[index] * cos - corners[index + 1] * sin
            val y = centerY + corners[index] * sin + corners[index + 1] * cos
            vertices.put(x / renderWidth * 2f - 1f)
            vertices.put(1f - y / renderHeight * 2f)
            index += 2
        }
        vertices.position(0)
    }

    private fun clamp01(value: Float) = min(1f, max(0f, value))

    /** Overshoots past its target before settling — the spring in the pop. */
    private fun easeOutBack(progress: Float): Float {
        val overshoot = 1.70158f
        val q = progress - 1f
        return 1f + (overshoot + 1f) * q * q * q + overshoot * q * q
    }

    private fun easeOutCubic(progress: Float) = 1f - (1f - progress).let { it * it * it }

    /** Slow to start, so a layer drifts before it flies off. */
    private fun easeInCubic(progress: Float) = progress * progress * progress

    private fun easeInQuad(progress: Float) = progress * progress

    fun release() {
        textures.release()
        if (program != 0) GLES20.glDeleteProgram(program)
        if (overlayProgram != 0) GLES20.glDeleteProgram(overlayProgram)
        program = 0
        overlayProgram = 0
    }

    private fun buildProgram(vertexSource: String, fragmentSource: String): Int {
        val vertex = compile(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragment = compile(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val id = GLES20.glCreateProgram()
        GLES20.glAttachShader(id, vertex)
        GLES20.glAttachShader(id, fragment)
        GLES20.glLinkProgram(id)
        val status = IntArray(1)
        GLES20.glGetProgramiv(id, GLES20.GL_LINK_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) { "link failed: ${GLES20.glGetProgramInfoLog(id)}" }
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        return id
    }

    private fun compile(type: Int, source: String): Int {
        val id = GLES20.glCreateShader(type)
        GLES20.glShaderSource(id, source)
        GLES20.glCompileShader(id)
        val status = IntArray(1)
        GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) { "compile failed: ${GLES20.glGetShaderInfoLog(id)}" }
        return id
    }

    companion object {
        /** The preview PiP, from Figma "11b Play - Playing · Camera Mode": 14,13,117×167 in 358×514. */
        const val PREVIEW_PIP_X = 14f / 358f
        const val PREVIEW_PIP_Y = 13f / 514f
        const val PREVIEW_PIP_WIDTH = 117f / 358f
        const val PREVIEW_PIP_HEIGHT = 167f / 514f
        private const val PREVIEW_PIP_RADIUS = 16f / 358f
        private const val PREVIEW_PIP_BORDER = 3f / 358f

        private const val VERTEX_SHADER = """
            uniform mat4 uTexMatrix;
            attribute vec4 aPosition;
            attribute vec4 aQuadCoord;
            varying vec2 vTexCoord;
            varying vec2 vQuad;
            void main() {
                gl_Position = aPosition;
                vTexCoord = (uTexMatrix * aQuadCoord).xy;
                vQuad = aQuadCoord.xy;
            }
        """

        /**
         * The rounded corner is a signed distance to the window's rounded box, rather than a
         * mask texture — one less texture unit and one less upload, and it antialiases for
         * free. Guarded on a positive radius: at zero the distance is zero across the whole
         * quad, which would smoothstep the full-bleed pass to half opacity.
         */
        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES uTexture;
            uniform vec2 uHalfSize;
            uniform float uRadius;
            uniform float uBorder;
            varying vec2 vTexCoord;
            varying vec2 vQuad;
            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                if (uRadius > 0.0) {
                    vec2 fromCenter = abs(vQuad - 0.5) * 2.0 * uHalfSize;
                    vec2 outside = max(fromCenter - (uHalfSize - uRadius), vec2(0.0));
                    float distance = length(outside) - uRadius;
                    // The border is the outer ring of the same shape, so it follows the
                    // corners exactly rather than being a second rounded rect behind.
                    if (uBorder > 0.0) {
                        color = mix(color, vec4(1.0), smoothstep(-uBorder - 1.0, -uBorder, distance));
                    }
                    color *= 1.0 - smoothstep(-1.0, 1.0, distance);
                }
                gl_FragColor = color;
            }
        """

        /**
         * The overlay's fragment stage. It samples the quad coordinate directly and ignores
         * `vTexCoord` — the bitmaps are drawn in the layout's own space, so there is nothing
         * to rotate or crop, and the vertex shader's transform is left unset on purpose.
         */
        private const val OVERLAY_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexture;
            uniform float uAlpha;
            varying vec2 vTexCoord;
            varying vec2 vQuad;
            void main() {
                // Bitmaps arrive premultiplied, so scaling the whole colour — not just its
                // alpha — is what keeps a faded layer premultiplied too.
                gl_FragColor = texture2D(uTexture, vQuad) * uAlpha;
            }
        """
    }
}
