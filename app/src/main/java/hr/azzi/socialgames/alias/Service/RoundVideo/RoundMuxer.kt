package hr.azzi.socialgames.alias.Service.RoundVideo

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * The mp4 both encoders write into.
 *
 * `MediaMuxer` will not accept a track once it has been started, and will not start without
 * one — so with two encoders producing formats at their own pace, somebody has to hold the
 * door open until both have arrived. That is this class' whole job.
 *
 * Samples that show up before the last track is declared are dropped rather than queued. In
 * practice that is a few milliseconds of audio at the very start of the intro card, where
 * there is nothing to hear; buffering them would mean holding encoder output buffers back,
 * and an encoder whose buffers are not returned stops accepting input.
 */
class RoundMuxer(private val output: File, private val expectedTracks: Int) {

    private val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private val lock = Any()

    private var declared = 0
    private var started = false
    private var released = false

    /** Samples actually written, per track — the honest measure of whether anything landed. */
    private val written = mutableMapOf<Int, Int>()

    fun addTrack(format: MediaFormat): Int = synchronized(lock) {
        check(!started) { "track added after the muxer started" }
        val index = muxer.addTrack(format)
        declared++
        if (declared == expectedTracks) {
            muxer.start()
            started = true
        }
        index
    }

    fun writeSample(track: Int, buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        synchronized(lock) {
            if (!started || released) return
            if (info.size <= 0) return
            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) return
            buffer.position(info.offset)
            buffer.limit(info.offset + info.size)
            runCatching { muxer.writeSampleData(track, buffer, info) }
                .onSuccess { written[track] = (written[track] ?: 0) + 1 }
                .onFailure { Log.w(TAG, "writeSampleData failed", it) }
        }
    }

    fun samplesWritten(track: Int): Int = synchronized(lock) { written[track] ?: 0 }

    /** True if anything playable was produced. */
    fun finish(): Boolean = synchronized(lock) {
        if (released) return false
        released = true
        val wrote = started && written.values.sum() > 0
        runCatching { if (started) muxer.stop() }
        runCatching { muxer.release() }
        wrote
    }

    fun delete() {
        output.delete()
    }

    private companion object {
        const val TAG = "RoundVideo"
    }
}
