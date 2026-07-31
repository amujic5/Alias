package hr.azzi.socialgames.alias.Service.RoundVideo

import android.content.Context
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * Writes the shareable round video *while the round is played*, straight from the two
 * cameras, instead of recording two files and compositing them afterwards.
 *
 * This is the Android half of iOS' `RoundLiveWriter`, and it is live for the same reason:
 * when the round ends there is nothing left to do but close the file, against the
 * twenty-odd seconds it takes to read two clips back and composite them.
 *
 * What it costs, stated plainly: there is a real-time deadline. A frame has 1/24 s to be
 * composited, and if it misses, the frame is dropped rather than the round stalling. There
 * is no second attempt — nothing is kept on disk to re-composite from. The round itself,
 * the scores and the game are never at risk; only the clip is.
 *
 * Everything touching GL happens on one thread, because an EGL context belongs to the
 * thread that made it. Both cameras' frame callbacks are delivered there too, which is what
 * makes the back-frame path free of locks: by the time a back frame is composited, the
 * front texture can only have been updated by an earlier callback on the same thread.
 */
class RoundRecorder(private val context: Context) {

    private enum class Stage { IDLE, RUNNING, FINISHING, FAILED }

    @Volatile
    private var stage = Stage.IDLE

    private lateinit var glThread: HandlerThread
    private lateinit var glHandler: Handler
    private lateinit var cameraThread: HandlerThread

    private var egl: EglCore? = null
    private var eglSurface: EGLSurface? = null
    private var muxer: RoundMuxer? = null
    private var encoder: RoundVideoEncoder? = null
    private var audio: RoundAudioEncoder? = null
    private var renderer: RoundFrameRenderer? = null
    private var back: CameraTexture? = null
    private var front: CameraTexture? = null
    private var session: DualCameraSession? = null
    private var output: File? = null
    private var overlay: RoundOverlayRenderer? = null
    private var cards: RoundCardRenderer? = null

    /**
     * True once the front camera has been given up on. The clip carries on with the back
     * camera alone; the caller reads this to stop labelling an inset that is not there.
     */
    @Volatile
    var singleCamera = false
        private set

    /** The play screen's live stage, if it is on screen. */
    private var previewSurface: EGLSurface? = null
    private var previewWidth = 0
    private var previewHeight = 0
    private val confetti = RoundConfetti()

    /** Correct answers already turned into a confetti burst. */
    private var scoresSeen = 0

    /** Round time of the last frame written; where the outro is hung from. */
    private var lastRoundTime = 0.0

    /**
     * What the overlay shows. Written from the round's own thread and read on the GL thread
     * one frame later; a whole state at a time, so a frame can never show a word from one
     * moment beside a score from another.
     */
    @Volatile
    private var state = RoundFrameRenderer.State(word = "", correct = 0, skipped = 0, remainingSeconds = 0)

    /** Called once if the front camera has to be dropped, so the screen can follow suit. */
    var onSingleCamera: (() -> Unit)? = null

    fun update(word: String, correct: Int, skipped: Int, remainingSeconds: Int) {
        state = RoundFrameRenderer.State(word, correct, skipped, remainingSeconds)
    }

    /** Capture-clock time of the first frame; everything is written relative to it. */
    private var startNanos = -1L
    /** When the next frame is owed, so the camera's rate is resampled down to ours. */
    private var nextFrameDue = 0L
    private var lastPresentation = -1L
    private var framesOffered = 0

    /**
     * Whether the round is actually being played. The cameras keep streaming across a
     * pause — reopening them would cost a second of gameplay and the exposure would have to
     * settle again — but nothing is written, and the paused time is subtracted from every
     * later frame's timestamp so the clip has no dead air in it.
     */
    @Volatile
    private var roundRunning = false
    private var pausedAt = -1L
    private var pausedTotal = 0L

    /**
     * Opens both cameras and starts writing. Returns false if the clip could not be started
     * at all — the caller plays the round regardless.
     */
    fun start(info: RoundCardInfo): Boolean {
        if (stage != Stage.IDLE) return false
        val pair = CameraPair.resolve(context) ?: run {
            Log.w(TAG, "no usable camera pair")
            return false
        }

        sweepOldClips()
        val file = File(context.cacheDir, "$FILE_PREFIX${UUID.randomUUID()}.mp4")

        // Drawn before the round rather than on the first frame: the chrome is two
        // full-frame bitmaps with shadows and text, and paying for it inside a frame
        // interval is how the first second of a round goes missing.
        val overlayRenderer = RoundOverlayRenderer(context)
        overlayRenderer.chromeBelow
        overlayRenderer.chromeAbove
        overlay = overlayRenderer

        // The intro card too — its backdrop and every animated layer. The outro cannot be
        // drawn yet: it lists the words, and the round has not been played.
        val cardRenderer = RoundCardRenderer(context, info)
        cardRenderer.intro
        cards = cardRenderer

        glThread = HandlerThread("round.video").apply { start() }
        glHandler = Handler(glThread.looper)
        cameraThread = HandlerThread("round.camera").apply { start() }

        // Whether the microphone works decides how many tracks the muxer waits for, so it has
        // to be settled before the video encoder can declare its own. A clip with no sound is
        // worth having; one that never starts because the microphone was busy is not.
        val silent = !hasMicrophonePermission()
        val roundMuxer = RoundMuxer(file, expectedTracks = if (silent) 1 else 2)
        muxer = roundMuxer
        if (!silent) {
            audio = RoundAudioEncoder(context, roundMuxer).takeIf { it.start() }
            if (audio == null) {
                Log.w(TAG, "microphone unavailable — the clip will be silent")
                // Nothing will ever declare the second track, and the muxer would wait for it
                // forever, so the clip has to be restarted as a one-track file.
                roundMuxer.finish()
                roundMuxer.delete()
                muxer = RoundMuxer(file, expectedTracks = 1)
            }
        }
        val activeMuxer = muxer ?: return false

        val opened = runOnGlThread {
            val videoEncoder = RoundVideoEncoder(activeMuxer)
            val core = EglCore()
            val surface = core.createWindowSurface(videoEncoder.inputSurface)
            core.makeCurrent(surface)

            val frameRenderer = RoundFrameRenderer().apply { prepare() }
            val backTexture = CameraTexture(pair.backOrientation, pair.backSize)
            val frontTexture = CameraTexture(pair.frontOrientation, pair.frontSize)

            // The back camera drives the clock: a back frame is what gets composited, and
            // the front texture is simply whatever arrived most recently. The front camera
            // may well run faster than the video is written at, and the extras are dropped.
            backTexture.surfaceTexture.setOnFrameAvailableListener({ onBackFrame() }, glHandler)
            frontTexture.surfaceTexture.setOnFrameAvailableListener({ onFrontFrame() }, glHandler)

            encoder = videoEncoder
            egl = core
            eglSurface = surface
            renderer = frameRenderer
            back = backTexture
            front = frontTexture
            output = file

            // The intro's hold is card only — it is opaque, so no camera frame is needed.
            // The dissolve is not written here: it happens over live footage, as round
            // frames arrive.
            val holdFrames = Math.round(RoundVideoSpec.INTRO_HOLD_S * RoundVideoSpec.FRAME_RATE).toInt()
            for (index in 0 until holdFrames) {
                val elapsed = index * RoundVideoSpec.FRAME_DURATION_NS
                writeFrame(RoundFrameRenderer.Phase.Intro(elapsed / 1e9), elapsed)
            }
        }
        if (!opened) {
            releaseAudio()
            muxer?.finish()
            muxer?.delete()
            muxer = null
            tearDown()
            return false
        }

        stage = Stage.RUNNING
        session = DualCameraSession(context, pair, Handler(cameraThread.looper)).also {
            it.open(
                back = Surface(back!!.surfaceTexture),
                front = Surface(front!!.surfaceTexture),
                onDegraded = { reason ->
                    // One camera is still a clip worth having. The inset is dropped and
                    // nothing else about the round changes.
                    Log.w(TAG, "single camera: $reason")
                    singleCamera = true
                    glHandler.post { front?.forget() }
                    onSingleCamera?.invoke()
                },
                onFailure = { reason ->
                    Log.w(TAG, "capture failed: $reason")
                    stage = Stage.FAILED
                },
            )
        }
        return true
    }

    /**
     * Starts and stops the writing, following the round's own play/pause. Frames are only
     * ever composited between a `true` and the next `false` — an activity that sits on the
     * play screen for twenty minutes with the round paused writes nothing.
     */
    fun setRoundRunning(running: Boolean) {
        if (running == roundRunning) return
        roundRunning = running
        audio?.setRoundRunning(running)
        if (running) {
            if (pausedAt > 0) {
                pausedTotal += System.nanoTime() - pausedAt
                pausedAt = -1L
            }
        } else {
            pausedAt = System.nanoTime()
        }
    }

    /**
     * Points the on-screen stage at the compositor, or takes it away.
     *
     * Both cameras already stream into this class, and the play screen needs to show them —
     * so it borrows the same textures rather than opening its own preview streams. That is
     * not only cheaper: a concurrent-camera configuration only guarantees so many streams,
     * and asking for two more is how a session that configures on one device fails on the
     * next.
     */
    fun setPreviewSurface(surface: Surface?, width: Int, height: Int) {
        if (stage == Stage.IDLE || !::glHandler.isInitialized) return
        glHandler.post {
            previewSurface?.let { existing ->
                // Whatever comes next needs the encoder's surface current, not a dead one.
                eglSurface?.let { egl?.makeCurrent(it) }
                egl?.releaseSurface(existing)
            }
            previewSurface = null
            previewWidth = width
            previewHeight = height
            if (surface != null && surface.isValid) {
                previewSurface = runCatching { egl?.createWindowSurface(surface) }
                    .onFailure { Log.w(TAG, "preview surface rejected", it) }
                    .getOrNull()
            }
        }
    }

    /**
     * Draws the stage. Runs on every camera frame, round or not — the players line the shot
     * up before pressing play, and a black rectangle until the round starts would be useless
     * for exactly the moment the preview exists to serve.
     */
    private fun drawPreview() {
        val surface = previewSurface ?: return
        val core = egl ?: return
        val backTexture = back ?: return
        val frontTexture = front ?: return
        val frameRenderer = renderer ?: return

        core.makeCurrent(surface)
        frameRenderer.renderPreview(backTexture, frontTexture, previewWidth, previewHeight)
        core.swapBuffers(surface)
        // Back to the encoder, so the next composite lands in the file and not on screen.
        eglSurface?.let { core.makeCurrent(it) }
    }

    private fun onFrontFrame() {
        if (stage != Stage.RUNNING) return
        runCatching { front?.updateTexImage() }
    }

    /**
     * Composites and writes one frame. Runs on the GL thread, so a slow frame makes the
     * next camera frame wait — and `SurfaceTexture` keeps only the newest, so what is
     * behind gets dropped rather than queued.
     */
    private fun onBackFrame() {
        val texture = back ?: return
        if (stage != Stage.RUNNING) return
        try {
            texture.updateTexImage()
        } catch (t: Throwable) {
            Log.w(TAG, "updateTexImage failed", t)
            return
        }
        drawPreview()
        // Consuming the frame regardless is what keeps the camera's queue from backing up
        // while the round is paused; what the pause changes is that nothing is written.
        if (!roundRunning) return
        framesOffered++

        if (startNanos < 0) {
            startNanos = texture.timestampNanos
            // The round is written after the intro's hold, not from zero.
            nextFrameDue = RoundVideoSpec.INTRO_HOLD_NS
        }
        val roundTime = texture.timestampNanos - startNanos - pausedTotal
        val presentation = RoundVideoSpec.INTRO_HOLD_NS + roundTime
        // The camera runs at its own rate — 30 fps, typically — and the video is written at
        // 24. Resampling against a running deadline rather than against the last frame's
        // timestamp is what makes the difference: comparing to the last frame either lets
        // everything through (a 33 ms gap clears a 21 ms bar) or throws away every other
        // frame (a 33 ms gap never clears a 42 ms one). A deadline that advances by exactly
        // one frame interval keeps 4 frames in every 5, which is 24 out of 30.
        if (presentation < nextFrameDue) return

        val seconds = roundTime / 1e9
        recordScores(state.correct, seconds)
        val fade = min(1.0, max(0.0, seconds / RoundVideoSpec.INTRO_FADE_S)).toFloat()
        writeFrame(RoundFrameRenderer.Phase.Round(seconds, fade), presentation)
        lastRoundTime = seconds

        nextFrameDue += RoundVideoSpec.FRAME_DURATION_NS
        // Resync rather than try to catch up: after a stall, owing a burst of frames would
        // write them all at once and play back as a jump.
        if (nextFrameDue <= presentation) nextFrameDue = presentation + RoundVideoSpec.FRAME_DURATION_NS
    }

    /**
     * A burst on every third correct answer — 3, 6, 9. Derived from the counter rather than
     * wired up separately, so it can't drift from what the overlay shows.
     */
    private fun recordScores(correct: Int, roundTime: Double) {
        while (scoresSeen < correct) {
            scoresSeen++
            if (scoresSeen % RoundConfetti.BURST_EVERY == 0) confetti.record(roundTime)
        }
    }

    /** Composites one frame and hands it to the encoder. Runs on the GL thread. */
    private fun writeFrame(phase: RoundFrameRenderer.Phase, presentation: Long) {
        val core = egl ?: return
        val surface = eglSurface ?: return
        val backTexture = back ?: return
        val frontTexture = front ?: return
        val frameRenderer = renderer ?: return
        val overlayRenderer = overlay ?: return
        val cardRenderer = cards

        frameRenderer.render(
            phase = phase,
            back = backTexture,
            front = frontTexture,
            overlay = overlayRenderer,
            confetti = confetti,
            intro = cardRenderer?.intro,
            outro = if (phase is RoundFrameRenderer.Phase.Outro) cardRenderer?.outro else null,
            state = state,
        )
        core.setPresentationTime(surface, presentation)
        core.swapBuffers(surface)
        encoder?.drain(endOfStream = false)
        lastPresentation = presentation
    }

    /**
     * Writes the outro card, closes the file and hands over the clip, or null if nothing
     * usable came out.
     *
     * @param info the outro's contents — the same card as the intro, but it can only be
     *   drawn now, because it lists the words the round produced.
     */
    fun stop(info: RoundCardInfo?, onDone: (File?) -> Unit) {
        if (stage == Stage.IDLE) {
            onDone(null)
            return
        }
        val failed = stage == Stage.FAILED
        stage = Stage.FINISHING
        session?.close()

        var file: File? = null
        runOnGlThread {
            if (!failed && info != null && lastPresentation >= 0) {
                writeOutro(info)
            }
            if (failed) {
                encoder?.release()
            } else {
                encoder?.drain(endOfStream = true)
                encoder?.release()
            }
            val written = encoder?.framesWritten ?: 0
            val dropped = framesOffered - written
            Log.w(
                TAG,
                "$written frames written, $dropped skipped" +
                    if (framesOffered > 0) " (%.1f%%)".format(dropped * 100f / framesOffered) else "",
            )
            releaseGl()
        }

        // After the video: its own thread is joined inside, and the muxer is only closed
        // once both tracks are done with it.
        audio?.finish()
        audio = null

        val produced = muxer?.finish() ?: false
        file = if (failed || !produced) {
            muxer?.delete()
            null
        } else {
            output
        }
        muxer = null

        tearDown()
        onDone(file)
    }

    /**
     * The outro card, hung off the last round frame so it follows the footage without a gap.
     *
     * The card renderer is rebuilt here rather than reused: the intro's copy was fixed
     * before the round, and this one needs the words the round actually produced.
     */
    private fun writeOutro(info: RoundCardInfo) {
        val previous = cards
        cards = RoundCardRenderer(context, info).also { it.outro }
        previous?.release()

        val start = max(lastPresentation + RoundVideoSpec.FRAME_DURATION_NS, RoundVideoSpec.INTRO_HOLD_NS)
        val frames = Math.round(RoundVideoSpec.OUTRO_S * RoundVideoSpec.FRAME_RATE).toInt()
        for (index in 0 until frames) {
            val elapsed = index * RoundVideoSpec.FRAME_DURATION_NS
            writeFrame(RoundFrameRenderer.Phase.Outro(elapsed / 1e9), start + elapsed)
        }
    }

    private fun releaseGl() {
        renderer?.release()
        overlay?.release()
        overlay = null
        cards?.release()
        cards = null
        confetti.release()
        back?.release()
        front?.release()
        previewSurface?.let { egl?.releaseSurface(it) }
        previewSurface = null
        eglSurface?.let { egl?.releaseSurface(it) }
        egl?.release()
        renderer = null
        back = null
        front = null
        eglSurface = null
        egl = null
        encoder = null
    }

    /** The camera permission is the gate; the microphone only decides whether there is sound. */
    private fun releaseAudio() {
        audio?.release()
        audio = null
    }

    private fun tearDown() {
        if (::glThread.isInitialized) glThread.quitSafely()
        if (::cameraThread.isInitialized) cameraThread.quitSafely()
        session = null
        stage = Stage.IDLE
    }

    private fun hasMicrophonePermission() =
        context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    /** Runs on the GL thread and waits, so setup failures surface where they happen. */
    private fun runOnGlThread(block: () -> Unit): Boolean {
        var ok = true
        val done = Object()
        var finished = false
        glHandler.post {
            try {
                block()
            } catch (t: Throwable) {
                Log.w(TAG, "GL work failed", t)
                ok = false
            }
            synchronized(done) {
                finished = true
                done.notifyAll()
            }
        }
        synchronized(done) {
            while (!finished) {
                runCatching { done.wait(SETUP_TIMEOUT_MS) }
                if (!finished) return false
            }
        }
        return ok
    }

    /**
     * Deletes strays left in the cache directory — a round abandoned partway, or one whose
     * move into the library failed. At tens of megabytes apiece those are worth clearing.
     */
    private fun sweepOldClips() {
        val cutoff = System.currentTimeMillis() - KEEP_FINISHED_MS
        context.cacheDir.listFiles()
            ?.filter { it.name.startsWith(FILE_PREFIX) && it.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }

    private companion object {
        const val TAG = "RoundVideo"
        const val FILE_PREFIX = "alias_round_"
        const val KEEP_FINISHED_MS = 60 * 60 * 1000L
        const val SETUP_TIMEOUT_MS = 15_000L
    }
}
