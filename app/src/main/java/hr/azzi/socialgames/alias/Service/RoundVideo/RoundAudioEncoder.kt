package hr.azzi.socialgames.alias.Service.RoundVideo

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

/**
 * The round's sound: the microphone, for as long as the round is being played.
 *
 * Audio gets its own thread on purpose — the same decision iOS documents. Sharing the video's
 * would make every microphone buffer queue up behind a composite, and audio has no frame to
 * drop: the gap would simply be audible.
 *
 * Timestamps come from the samples themselves rather than from a clock. Counting samples is
 * gap-free by construction, and since nothing is read while the round is paused, the pause
 * disappears from the audio timeline exactly as it does from the video's.
 */
class RoundAudioEncoder(private val context: Context, private val muxer: RoundMuxer) {

    private val codec: MediaCodec = MediaCodec.createEncoderByType(MIME)
    private val bufferInfo = MediaCodec.BufferInfo()
    private var trackIndex = -1

    private var record: AudioRecord? = null
    private var worker: Thread? = null

    @Volatile
    private var capturing = false

    @Volatile
    private var roundRunning = false

    /** Frames handed to the encoder so far; the audio clock. */
    private var framesEncoded = 0L

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        val format = MediaFormat.createAudioFormat(MIME, SAMPLE_RATE, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, READ_FRAMES * 2)
        }
        return try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            val minimum = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            )
            check(minimum > 0) { "no usable microphone buffer size" }
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minimum, READ_FRAMES * 2 * 4),
            )
            check(recorder.state == AudioRecord.STATE_INITIALIZED) { "microphone would not initialise" }
            recorder.startRecording()

            // The intro card's silence, fed up front. It is not only correct — the card has
            // nothing to say — it is also what declares the audio track. The muxer will not
            // start until every track is in, and until it starts it drops what it is given:
            // waiting for the round to declare this one would throw away the whole intro.
            feed(ShortArray((RoundVideoSpec.INTRO_HOLD_S * SAMPLE_RATE).toInt()))
            drain(endOfStream = false)

            record = recorder
            capturing = true
            worker = thread(name = "round.audio") { pump(recorder) }
            true
        } catch (t: Throwable) {
            Log.w(TAG, "audio could not start; the clip will be silent", t)
            release()
            false
        }
    }

    fun setRoundRunning(running: Boolean) {
        roundRunning = running
    }

    /**
     * Reads the microphone for as long as the clip is being written.
     *
     * The read happens even while the round is paused — [AudioRecord] keeps filling its
     * ring buffer whether or not anyone collects it, and letting it overrun would leave stale
     * audio to be read when the round resumes. What the pause changes is that nothing is
     * encoded.
     */
    private fun pump(recorder: AudioRecord) {
        val buffer = ByteBuffer.allocateDirect(READ_FRAMES * 2).order(ByteOrder.nativeOrder())
        while (capturing) {
            buffer.clear()
            val read = recorder.read(buffer, buffer.capacity())
            if (read <= 0) continue
            if (!roundRunning) continue
            buffer.position(0)
            buffer.limit(read)
            encode(buffer, read / 2)
            drain(endOfStream = false)
        }
    }

    /**
     * Hands PCM to the encoder, stamped from the running sample count.
     *
     * When the encoder has no input buffer free, the samples are dropped — but the clock is
     * advanced anyway. The alternative is worse than it looks: leaving the count where it is
     * shifts every later sample earlier, so a moment's congestion does not cost a blip, it
     * costs permanent drift, and by the end of a round the sound is ahead of the picture. One
     * drained retry first, since a full encoder is usually just one undrained buffer away
     * from having room.
     */
    private fun encode(pcm: ByteBuffer, frames: Int) {
        var index = codec.dequeueInputBuffer(TIMEOUT_US)
        if (index < 0) {
            drain(endOfStream = false)
            index = codec.dequeueInputBuffer(TIMEOUT_US)
        }
        if (index < 0) {
            framesEncoded += frames
            return
        }
        val input = codec.getInputBuffer(index) ?: return
        input.clear()
        val count = minOf(pcm.remaining(), input.remaining())
        val slice = pcm.slice().apply { limit(count) }
        input.put(slice)
        codec.queueInputBuffer(index, 0, count, presentationUs(framesEncoded), 0)
        framesEncoded += frames
    }

    private fun drain(endOfStream: Boolean) {
        while (true) {
            val status = codec.dequeueOutputBuffer(bufferInfo, if (endOfStream) TIMEOUT_US else 0)
            when {
                status == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!endOfStream) return

                status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ->
                    trackIndex = muxer.addTrack(codec.outputFormat)

                status >= 0 -> {
                    val buffer = codec.getOutputBuffer(status)
                    if (buffer != null && trackIndex >= 0) {
                        muxer.writeSample(trackIndex, buffer, bufferInfo)
                    }
                    codec.releaseOutputBuffer(status, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }

    /**
     * Stops the microphone and closes the track.
     *
     * Nothing is laid over the outro. The whistle used to be decoded and appended here, which
     * put it in the clip twice: the round's own end sound plays through the speaker while the
     * round is still running, so the microphone has already recorded it. iOS adds it because
     * its writer stops before the sound plays; this one does not.
     */
    fun finish() {
        capturing = false
        runCatching { worker?.join(2_000) }
        worker = null
        runCatching { record?.stop() }
        runCatching { record?.release() }
        record = null

        runCatching {
            val index = codec.dequeueInputBuffer(TIMEOUT_US)
            if (index >= 0) {
                codec.queueInputBuffer(
                    index, 0, 0, presentationUs(framesEncoded),
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                )
            }
            drain(endOfStream = true)
        }.onFailure { Log.w(TAG, "audio end-of-stream failed", it) }

        release()
    }

    private fun feed(samples: ShortArray) {
        var offset = 0
        while (offset < samples.size) {
            val chunk = minOf(READ_FRAMES, samples.size - offset)
            val buffer = ByteBuffer.allocateDirect(chunk * 2).order(ByteOrder.nativeOrder())
            buffer.asShortBuffer().put(samples, offset, chunk)
            buffer.position(0)
            buffer.limit(chunk * 2)
            encode(buffer, chunk)
            drain(endOfStream = false)
            offset += chunk
        }
    }

    fun release() {
        capturing = false
        runCatching { record?.stop() }
        runCatching { record?.release() }
        record = null
        runCatching { codec.stop() }
        runCatching { codec.release() }
    }

    /**
     * The audio timeline starts at zero, like the video's — the intro's offset is carried by
     * the silence fed at the front rather than by an offset here, so the two cannot drift.
     */
    private fun presentationUs(frames: Long) = frames * 1_000_000L / SAMPLE_RATE

    private companion object {
        const val TAG = "RoundVideo"
        const val MIME = MediaFormat.MIMETYPE_AUDIO_AAC
        const val SAMPLE_RATE = 44_100
        const val BIT_RATE = 96_000

        /** ~23 ms per read: small enough that a pause lands promptly, large enough to be cheap. */
        const val READ_FRAMES = 1024
        const val TIMEOUT_US = 10_000L
    }
}
