package hr.azzi.socialgames.alias.ai

import android.content.Context
import org.json.JSONObject

/** AI decks available on Android (those with bundled ai_<deck>_<lang>.json clue files + artwork). */
object AIDeckCatalog {

    data class Entry(val word: String, val clues: List<String>)

    val decks: List<AIDeck> = listOf(
        AIDeck("game_alias_classic", "alias_classic", "game_classic",
            listOf(AILanguage.EN, AILanguage.FR, AILanguage.DE, AILanguage.IT, AILanguage.HR, AILanguage.SR)),
        AIDeck("game_alias_cro", "game_alias_cro", "game_alias_cro", listOf(AILanguage.HR)),
        AIDeck("game_alias_srb", "game_alias_srb", "serbia", listOf(AILanguage.SR)),
        AIDeck("game_alias_football", "alias_football", "game_football",
            listOf(AILanguage.HR, AILanguage.EN, AILanguage.DE, AILanguage.SR)),
        AIDeck("game_alias_junior", "game_alias_junior", "game_alias_junior",
            listOf(AILanguage.HR, AILanguage.EN, AILanguage.FR, AILanguage.DE, AILanguage.IT, AILanguage.SR)),
        AIDeck("game_alias_uk", "game_alias_uk", "game_alias_uk", listOf(AILanguage.EN)),
    )

    fun deck(id: String): AIDeck? = decks.firstOrNull { it.id == id }

    fun entries(context: Context, deckId: String, language: AILanguage): List<Entry> {
        val resName = "ai_${deckId}_${language.fileCode}"
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
        if (resId == 0) return emptyList()
        return try {
            val json = context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
            val arr = JSONObject(json).getJSONArray("words")
            val out = ArrayList<Entry>(arr.length())
            for (i in 0 until arr.length()) {
                when (val item = arr.get(i)) {
                    is JSONObject -> {
                        val word = item.optString("word")
                        val cluesArr = item.optJSONArray("clues")
                        val clues = if (cluesArr != null) (0 until cluesArr.length()).map { cluesArr.getString(it) } else emptyList()
                        if (word.isNotBlank()) out.add(Entry(word, clues))
                    }
                    is String -> if (item.isNotBlank()) out.add(Entry(item, emptyList()))
                }
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun words(context: Context, deckId: String, language: AILanguage): List<String> =
        entries(context, deckId, language).map { it.word }

    fun clueMap(context: Context, deckId: String, language: AILanguage): Map<String, List<String>> =
        entries(context, deckId, language).associate { it.word.lowercase() to it.clues }
}
