package hr.azzi.socialgames.alias.ai

import java.io.InputStream

/**
 * Minimal cased BERT WordPiece tokenizer matching the bundled distilmBERT vocab,
 * so Kotlin tokenization matches the Python-trained model. Produces [CLS] … [SEP]
 * ids. No lowercasing / accent stripping (cased model). Port of iOS AIWordPieceTokenizer.
 */
class AIWordPieceTokenizer(vocabStream: InputStream, private val maxLen: Int = 64) {

    private val vocab: Map<String, Int>
    private val unkId: Int
    private val clsId: Int
    private val sepId: Int
    private val maxCharsPerWord = 100

    init {
        val map = HashMap<String, Int>()
        vocabStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            var i = 0
            for (line in lines) { map[line] = i; i++ }
        }
        vocab = map
        unkId = map["[UNK]"] ?: 0
        clsId = map["[CLS]"] ?: 0
        sepId = map["[SEP]"] ?: 0
    }

    /** Token ids wrapped with [CLS]/[SEP], truncated to maxLen. */
    fun encode(text: String): LongArray {
        val pieces = ArrayList<Int>()
        for (token in basicTokenize(text)) {
            pieces.addAll(wordPiece(token))
            if (pieces.size > maxLen - 2) break
        }
        val trimmed = if (pieces.size > maxLen - 2) pieces.subList(0, maxLen - 2) else pieces
        val out = LongArray(trimmed.size + 2)
        out[0] = clsId.toLong()
        for (i in trimmed.indices) out[i + 1] = trimmed[i].toLong()
        out[out.size - 1] = sepId.toLong()
        return out
    }

    /** Whitespace + punctuation splitting; case and accents preserved. */
    private fun basicTokenize(text: String): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        for (ch in text) {
            when {
                ch.isWhitespace() -> { if (cur.isNotEmpty()) { out.add(cur.toString()); cur.clear() } }
                !ch.isLetterOrDigit() -> {
                    if (cur.isNotEmpty()) { out.add(cur.toString()); cur.clear() }
                    out.add(ch.toString())
                }
                else -> cur.append(ch)
            }
        }
        if (cur.isNotEmpty()) out.add(cur.toString())
        return out
    }

    /** Greedy longest-match-first WordPiece; whole word → [UNK] on failure. */
    private fun wordPiece(token: String): List<Int> {
        val chars = token.toCharArray()
        if (chars.size > maxCharsPerWord) return listOf(unkId)
        val sub = ArrayList<Int>()
        var start = 0
        while (start < chars.size) {
            var end = chars.size
            var matched: Int? = null
            while (start < end) {
                var piece = String(chars, start, end - start)
                if (start > 0) piece = "##$piece"
                val id = vocab[piece]
                if (id != null) { matched = id; break }
                end -= 1
            }
            val id = matched ?: return listOf(unkId)
            sub.add(id)
            start = end
        }
        return sub
    }
}
