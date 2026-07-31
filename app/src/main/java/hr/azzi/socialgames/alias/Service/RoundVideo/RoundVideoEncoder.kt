package hr.azzi.socialgames.alias.Service.RoundVideo

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface

/**
 * The H.264 encoder feeding the round's mp4.
 *
 * The compositor draws straight into [inputSurface], so a composited frame never comes back
 * to the CPU — it goes from the GL context to the encoder inside the driver. What is left
 * for this class is draining the encoder and handing samples to the muxer.
 *
 * This stands in for iOS' `AVAssetWriter`, and keeps its two properties that matter: the
 * encoder runs in real time and is never waited on, and a frame that cannot be encoded now
 * is dropped rather than queued. Nothing is buffered, so nothing can grow.
 */
class RoundVideoEncoder(private val muxer: RoundMuxer) {

    private val codec: MediaCodec
    val inputSurface: Surface

    private val bufferInfo = MediaCodec.BufferInfo()
    private var trackIndex = -1
    private var released = false

    val framesWritten: Int get() = if (trackIndex >= 0) muxer.samplesWritten(trackIndex) else 0

    init {
        val format = MediaFormat.createVideoFormat(
            RoundVideoSpec.MIME, RoundVideoSpec.RENDER_WIDTH, RoundVideoSpec.RENDER_HEIGHT,
        ).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
            )
            setInteger(MediaFormat.KEY_BIT_RATE, RoundVideoSpec.BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, RoundVideoSpec.FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, RoundVideoSpec.I_FRAME_INTERVAL)
        }

        codec = MediaCodec.createEncoderByType(RoundVideoSpec.MIME)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = codec.createInputSurface()
        codec.start()
    }

    /**
     * Moves whatever the encoder has finished into the file.
     *
     * Called after every frame rather than on a timer: the encoder's output buffers are a
     * fixed pool, and one that is never drained stops accepting input — which shows up as
     * the capture pipeline stalling, not as an error.
     */
    fun drain(endOfStream: Boolean) {
        if (released) return
        if (endOfStream) runCatching { codec.signalEndOfInputStream() }

        while (true) {
            val status = codec.dequeueOutputBuffer(bufferInfo, if (endOfStream) TIMEOUT_US else 0)
            when {
                status == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!endOfStream) return

                status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // Arrives exactly once, before any frame, and carries the codec-specific
                    // data the muxer needs.
                    trackIndex = muxer.addTrack(codec.outputFormat)
                }

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

    fun release() {
        if (released) return
        released = true
        runCatching { codec.stop() }
        runCatching { codec.release() }
        runCatching { inputSurface.release() }
    }

    private companion object {
        const val TIMEOUT_US = 10_000L
    }
}
