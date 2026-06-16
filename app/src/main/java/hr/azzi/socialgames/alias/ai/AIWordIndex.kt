package hr.azzi.socialgames.alias.ai

import android.content.Context

/** Closed-set candidate generator for "you explain → AI guesses": top-K nearest
 *  deck words (cosine) to the player's spoken description. Port of iOS AIWordIndex. */
object AIWordIndex {

    private val ready = HashMap<String, AIVectors.DeckVecs>()

    private fun key(deckId: String, language: AILanguage) = "${language.fileCode}|$deckId"

    fun isReady(deckId: String, language: AILanguage): Boolean = ready.containsKey(key(deckId, language))

    /** Loads precomputed vectors for the deck+language and the embedder. */
    fun prepare(context: Context, deckId: String, language: AILanguage): Boolean {
        val k = key(deckId, language)
        if (ready.containsKey(k)) return true
        val shipped = AIVectors.byDeck(context, language)[deckId] ?: return false
        if (shipped.words.isEmpty()) return false
        ready[k] = shipped
        AIEmbedder.loadIfNeeded(context)
        return true
    }

    fun deck(deckId: String, language: AILanguage): AIVectors.DeckVecs? = ready[key(deckId, language)]

    /** Cosine top-K of a normalized query vector against a deck (both normalized → dot = cosine). */
    fun topK(query: FloatArray, deck: AIVectors.DeckVecs, k: Int): List<String> {
        val scored = ArrayList<Pair<String, Float>>(deck.words.size)
        for (i in deck.words.indices) {
            val v = deck.vecs[i]
            val n = minOf(v.size, query.size)
            var dot = 0f
            for (j in 0 until n) dot += v[j] * query[j]
            scored.add(deck.words[i] to dot)
        }
        scored.sortByDescending { it.second }
        return scored.take(k).map { it.first }
    }
}
