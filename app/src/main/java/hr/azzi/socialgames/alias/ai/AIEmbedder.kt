package hr.azzi.socialgames.alias.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.io.File
import java.nio.LongBuffer

/** On-device sentence embedder (ONNX Runtime). Loads the int8 distilmBERT model
 *  from assets and returns a 512-d L2-normalized vector. */
object AIEmbedder {

    const val DIM = 512
    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var tokenizer: AIWordPieceTokenizer? = null

    val isAvailable: Boolean get() = session != null

    @Synchronized
    fun loadIfNeeded(context: Context) {
        if (session != null) return
        runCatching {
            val e = OrtEnvironment.getEnvironment()
            // Stream the 129MB model to a file, then memory-map it (no huge heap alloc → no OOM).
            val modelFile = File(context.filesDir, "ai_embed_int8.onnx")
            if (!modelFile.exists() || modelFile.length() == 0L) {
                context.assets.open("ai/ai_embed_int8.onnx").use { input ->
                    modelFile.outputStream().use { output -> input.copyTo(output, 1 shl 20) }
                }
            }
            val opts = OrtSession.SessionOptions().apply { setIntraOpNumThreads(2) }
            session = e.createSession(modelFile.absolutePath, opts)
            tokenizer = AIWordPieceTokenizer(context.assets.open("ai/ai_cro_vocab.txt"))
            env = e
        }.onFailure { android.util.Log.e("AIEmbed", "load failed", it) }
    }

    @Synchronized
    fun embed(text: String): FloatArray? {
        val e = env ?: return null
        val s = session ?: return null
        val tok = tokenizer ?: return null
        val ids = tok.encode(text)
        if (ids.isEmpty()) return null
        val shape = longArrayOf(1, ids.size.toLong())
        val mask = LongArray(ids.size) { 1L }
        var idsT: OnnxTensor? = null
        var maskT: OnnxTensor? = null
        var out: OrtSession.Result? = null
        return try {
            idsT = OnnxTensor.createTensor(e, LongBuffer.wrap(ids), shape)
            maskT = OnnxTensor.createTensor(e, LongBuffer.wrap(mask), shape)
            out = s.run(mapOf("input_ids" to idsT, "attention_mask" to maskT))
            @Suppress("UNCHECKED_CAST")
            (out[0].value as Array<FloatArray>)[0]
        } catch (ex: Exception) {
            null
        } finally {
            out?.close(); idsT?.close(); maskT?.close()
        }
    }
}
