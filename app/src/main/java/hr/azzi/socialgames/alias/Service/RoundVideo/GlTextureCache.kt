package hr.azzi.socialgames.alias.Service.RoundVideo

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils

/**
 * Bitmaps that have been uploaded to the GPU, kept for as long as the clip is being written.
 *
 * Re-uploading a bitmap per frame is the mistake this exists to prevent: the chrome layers
 * are the size of the whole frame, and pushing several megabytes across the bus 24 times a
 * second would cost more than the compositing does. The overlay hands back the same bitmap
 * instance for the same value, and `Bitmap` does not override equality, so the map keys on
 * identity — which is exactly the question being asked: *have I already uploaded this one?*
 */
class GlTextureCache {

    private val textures = HashMap<Bitmap, Int>()

    fun textureFor(bitmap: Bitmap): Int = textures.getOrPut(bitmap) {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        ids[0]
    }

    fun release() {
        if (textures.isNotEmpty()) {
            GLES20.glDeleteTextures(textures.size, textures.values.toIntArray(), 0)
        }
        textures.clear()
    }
}
