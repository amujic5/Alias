package hr.azzi.socialgames.alias.ai

import java.text.Normalizer
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Forgiving spoken-answer matching: case/diacritic-insensitive, with a small
 * edit-distance + stem tolerance (handles Croatian inflections).
 * Port of iOS AnswerMatcher.
 */
object AnswerMatcher {

    fun normalize(s: String): String {
        val stripped = Normalizer.normalize(cyrillicToLatin(s), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
        return stripped
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .trim()
    }

    private val CYRILLIC_TO_LATIN = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'ђ' to "đ",
        'е' to "e", 'ж' to "ž", 'з' to "z", 'и' to "i", 'ј' to "j", 'к' to "k",
        'л' to "l", 'љ' to "lj", 'м' to "m", 'н' to "n", 'њ' to "nj", 'о' to "o",
        'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'ћ' to "ć", 'у' to "u",
        'ф' to "f", 'х' to "h", 'ц' to "c", 'ч' to "č", 'џ' to "dž", 'ш' to "š",
    )

    /**
     * Serbian Cyrillic → Latin (orthographic). Google's recognizer returns Latin
     * on most setups but Cyrillic on some; transliterating here means spoken
     * answers match the Latin decks either way. No-op for non-Cyrillic input.
     */
    fun cyrillicToLatin(s: String): String {
        if (s.none { it in 'Ѐ'..'ӿ' }) return s
        val sb = StringBuilder(s.length + 4)
        for (ch in s) sb.append(CYRILLIC_TO_LATIN[ch.lowercaseChar()] ?: ch.toString())
        return sb.toString()
    }

    /** True if the transcript contains an acceptable answer. */
    fun matches(transcript: String, accepted: List<String>): Boolean {
        val normTranscript = normalize(transcript)
        if (normTranscript.isEmpty()) return false
        val spokenWords = normTranscript.split(" ").filter { it.isNotEmpty() }

        for (raw in accepted) {
            val target = normalize(raw)
            if (target.isEmpty()) continue

            if (target.contains(" ")) {
                if (normTranscript.contains(target)) return true
                continue
            }
            for (word in spokenWords) {
                if (word == target) return true
                if (stemEqual(word, target)) return true
                if (levenshtein(word, target) <= tolerance(target)) return true
            }
        }
        return false
    }

    private fun tolerance(target: String): Int = max(1, target.length / 5)

    /** Shared leading stem of >=70% of the shorter word (inflection tolerance). */
    private fun stemEqual(a: String, b: String): Boolean {
        val n = min(a.length, b.length)
        if (n < 4) return false
        val stem = ceil(n * 0.7).toInt()
        return a.take(stem) == b.take(stem)
    }

    fun levenshtein(a: String, b: String): Int {
        val s = a.toCharArray()
        val t = b.toCharArray()
        if (s.isEmpty()) return t.size
        if (t.isEmpty()) return s.size
        var prev = IntArray(t.size + 1) { it }
        var cur = IntArray(t.size + 1)
        for (i in 1..s.size) {
            cur[0] = i
            for (j in 1..t.size) {
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1
                cur[j] = min(min(prev[j] + 1, cur[j - 1] + 1), prev[j - 1] + cost)
            }
            val tmp = prev; prev = cur; cur = tmp
        }
        return prev[t.size]
    }
}
