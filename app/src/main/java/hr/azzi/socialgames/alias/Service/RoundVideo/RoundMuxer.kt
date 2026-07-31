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
 * Samples that show up before the last track is declared are held, not dropped. They used to
 * be dropped, on the theory that it was a few milliseconds of nothing — it was not. The audio
 * track is declared first and then feeds the intro card's silence into a muxer that has not
 * started, so the whole intro's worth of audio went in the bin, and the finished file had a
 * sound track that began three seconds after the picture did. A player syncing video to audio
 * then skipped the intro to catch up, which is how a card that is demonstrably in the file
 * never appeared on screen.
 *
 * Held by copy rather than by keeping the encoder's buffer: an encoder whose output buffers
 * are not returned stops accepting input, and that would stall the round rather than the
 * intro. It is a few tens of kilobytes and it lasts until the second track arrives.
 */
class RoundMuxer(private val output: File, private val expectedTracks: Int) {

    private val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private val lock = Any()

    private var declared = 0
    private var started = false
    private var released = false

    /** Samples actually written, per track — the honest measure of whether anything landed. */
    private val written = mutableMapOf<Int, Int>()

    /** What arrived before the muxer could take it. Emptied the moment it can. */
    private class Pending(val track: Int, val bytes: ByteArray, val info: MediaCodec.BufferInfo)

    private val pending = mutableListOf<Pending>()

    fun addTrack(format: MediaFormat): Int = synchronized(lock) {
        check(!started) { "track added after the muxer started" }
        val index = muxer.addTrack(format)
        declared++
        if (declared == expectedTracks) {
            muxer.start()
            started = true
            flushPending()
        }
        index
    }

    /**
     * In arrival order, which is each track's own order — that is all `MediaMuxer` asks for,
     * since timestamps must only rise within a track and not across them.
     */
    private fun flushPending() {
        pending.forEach { held ->
            runCatching { muxer.writeSampleData(held.track, ByteBuffer.wrap(held.bytes), held.info) }
                .onSuccess { written[held.track] = (written[held.track] ?: 0) + 1 }
                .onFailure { Log.w(TAG, "held sample rejected", it) }
        }
        pending.clear()
    }

    fun writeSample(track: Int, buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        synchronized(lock) {
            if (released) return
            if (info.size <= 0) return
            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) return
            buffer.position(info.offset)
            buffer.limit(info.offset + info.size)

            if (!started) {
                if (pending.size < MAX_PENDING) {
                    val bytes = ByteArray(info.size)
                    buffer.get(bytes)
                    val copy = MediaCodec.BufferInfo().apply {
                        set(0, info.size, info.presentationTimeUs, info.flags)
                    }
                    pending += Pending(track, bytes, copy)
                }
                return
            }

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

        /**
         * A ceiling on what is held, so a second track that never arrives cannot grow this
         * without limit. Generous against what it is for: the intro's audio is around a
         * hundred AAC frames.
         */
        const val MAX_PENDING = 2_000
    }
}
