package hr.azzi.socialgames.alias.ai

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Precomputed per-deck word vectors loaded from assets ai/ai_<fileCode>_vectors.bin.
 *  Binary layout (little-endian): u32 'ACV1', u32 dim, u32 numDecks,
 *  per deck: u16 idLen, id, u32 count, per word: u16 wordLen, word, dim×f32. */
object AIVectors {

    class DeckVecs(val words: List<String>, val vecs: Array<FloatArray>)

    private val cache = HashMap<String, Map<String, DeckVecs>>()

    @Synchronized
    fun byDeck(context: Context, language: AILanguage): Map<String, DeckVecs> {
        cache[language.fileCode]?.let { return it }
        val loaded = load(context, "ai/ai_${language.fileCode}_vectors.bin")
        cache[language.fileCode] = loaded
        return loaded
    }

    private fun load(context: Context, assetPath: String): Map<String, DeckVecs> {
        return try {
            val bytes = context.assets.open(assetPath).use { it.readBytes() }
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = buf.int
            if (magic != 0x41435631) return emptyMap()
            val dim = buf.int
            val nDecks = buf.int
            val out = HashMap<String, DeckVecs>()
            repeat(nDecks) {
                val idLen = buf.short.toInt() and 0xFFFF
                val idBytes = ByteArray(idLen); buf.get(idBytes)
                val deckId = String(idBytes, Charsets.UTF_8)
                val count = buf.int
                val words = ArrayList<String>(count)
                val vecs = ArrayList<FloatArray>(count)
                repeat(count) {
                    val wl = buf.short.toInt() and 0xFFFF
                    val wb = ByteArray(wl); buf.get(wb)
                    words.add(String(wb, Charsets.UTF_8))
                    val v = FloatArray(dim)
                    for (i in 0 until dim) v[i] = buf.float
                    vecs.add(v)
                }
                out[deckId] = DeckVecs(words, vecs.toTypedArray())
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
