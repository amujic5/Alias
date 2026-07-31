package hr.azzi.socialgames.alias.Service.RoundVideo

import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Size

/**
 * One camera's frames, as a GL texture.
 *
 * Camera frames arrive as `GL_TEXTURE_EXTERNAL_OES` — the buffer is handed over rather than
 * copied, which is the only reason two feeds can be composited inside a frame interval.
 * Nothing here ever touches pixels on the CPU.
 *
 * The turn to upright and the aspect-fill crop both happen in the sampling transform, folded
 * into one matrix, so they cost nothing. Asking the capture connection to rotate instead
 * would cost a pipeline pass — the same trade the iOS side documents.
 *
 * @param size the buffer the camera actually delivers. Not assumed: a device that does not
 *   support the size we would like hands back a different one, and every number below —
 *   which way is up, what to crop — is derived from it.
 */
class CameraTexture(private val sensorOrientation: Int, val size: Size) {

    val textureId: Int = createExternalTexture()
    val surfaceTexture = SurfaceTexture(textureId).apply {
        setDefaultBufferSize(size.width, size.height)
    }

    private val stMatrix = FloatArray(16)
    private val crop = FloatArray(16)
    private val flip = FloatArray(16)
    private val scratch = FloatArray(16)
    private val combined = FloatArray(16)

    /** True once a frame has actually landed — before that there is nothing to draw. */
    var hasFrame = false
        private set

    /** Latest frame's timestamp, in the capture clock's nanoseconds. */
    val timestampNanos: Long get() = surfaceTexture.timestamp

    fun updateTexImage() {
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(stMatrix)
        hasFrame = true
    }

    /**
     * Forget what was last delivered.
     *
     * For a camera that has gone: the texture still holds its final frame, and the
     * compositor draws whatever it holds — so without this the inset would sit there frozen
     * on the same face for the rest of the round, which looks worse than not being there.
     */
    fun forget() {
        hasFrame = false
    }

    /**
     * The sampling transform that fills [destination] with this camera's frame: upright,
     * aspect-filled, overflow centre-cropped.
     *
     * There is deliberately no rotation here. `SENSOR_ORIENTATION` says how far the sensor
     * sits from upright, and the obvious move is to turn the frame by it — but the camera
     * already sets that turn on the buffer, and `SurfaceTexture.getTransformMatrix` hands it
     * back to us. Applying it a second time is what put the picture on its side.
     *
     * Measured rather than assumed: the same frame drawn at all four turns beside the
     * platform camera app's own preview, on a Pixel emulator and an SM-A376B, and on both the
     * untouched one is the one that stands up — for the back camera at 90° and the front at
     * 270° alike. What the orientation is still good for is knowing which way round the
     * frame's sides are, which the crop below needs.
     *
     * @param destination in render pixels, top-left origin — the same space the layout is in.
     */
    fun samplingMatrix(destination: RectF): FloatArray {
        // The frame as it stands up: a camera whose sensor is a quarter turn from the
        // device's natural orientation delivers its long side across.
        val quarterTurn = sensorOrientation == 90 || sensorOrientation == 270
        val uprightWidth = if (quarterTurn) size.height else size.width
        val uprightHeight = if (quarterTurn) size.width else size.height

        val sourceAspect = uprightWidth.toFloat() / uprightHeight
        val targetAspect = destination.width() / destination.height()
        // Sampling a narrower region zooms in, which is what covering a taller target needs.
        val cropX = if (sourceAspect > targetAspect) targetAspect / sourceAspect else 1f
        val cropY = if (sourceAspect > targetAspect) 1f else sourceAspect / targetAspect

        // Crop about the centre, then hand GL's bottom-left origin to the transform.
        affine(crop, cropX, 0f, 0.5f - 0.5f * cropX, 0f, cropY, 0.5f - 0.5f * cropY)
        affine(flip, 1f, 0f, 0f, 0f, -1f, 1f)

        Matrix.multiplyMM(scratch, 0, flip, 0, crop, 0)
        Matrix.multiplyMM(combined, 0, stMatrix, 0, scratch, 0)
        return combined
    }

    /** A 2D affine map `(u, v) → (a·u + b·v + c, d·u + e·v + f)`, as a GL matrix. */
    private fun affine(out: FloatArray, a: Float, b: Float, c: Float, d: Float, e: Float, f: Float) {
        Matrix.setIdentityM(out, 0)
        out[0] = a; out[4] = b; out[12] = c
        out[1] = d; out[5] = e; out[13] = f
    }

    fun release() {
        surfaceTexture.release()
        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
    }

    private fun createExternalTexture(): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ids[0])
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR,
        )
        // Clamping matters: the aspect-fill crop can land a sample a hair outside 0..1, and
        // the default wrap would fetch it from the opposite edge.
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE,
        )
        return ids[0]
    }
}
