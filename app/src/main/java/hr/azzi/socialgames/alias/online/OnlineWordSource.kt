package hr.azzi.socialgames.alias.online

import android.content.Context
import hr.azzi.socialgames.alias.Service.BoardGame
import hr.azzi.socialgames.alias.Service.BoardGameId
import hr.azzi.socialgames.alias.Service.JSONService
import hr.azzi.socialgames.alias.Service.serializedName
import org.json.JSONObject
import java.nio.charset.Charset

/**
 * Loads the word list for a chosen deck + language from the bundled raw JSON decks
 * (same files the offline game uses, e.g. `game_alias_classic_eng`). The host freezes
 * a randomized subset once at room creation; every player then fetches that frozen list
 * from Firestore a single time. Android port of iOS `OnlineWordSource.swift`.
 */
object OnlineWordSource {

    /** Max frozen deck size uploaded per room. */
    const val MAX_WORDS = 1000

    /** Decks offered for Online Play — every board game except Taboo (poor fit online). */
    val decks: List<BoardGame>
        get() = JSONService.boardGames.filter { it.id != BoardGameId.TABOO && !it.isLocalOnly }

    /** Loaded deck words + display language name for a (deckId, code) pair, or null. */
    data class DeckWords(val language: String, val languageCode: String, val words: List<String>) {
        /** Language name in the app's language; `language` itself is an identifier, not display text. */
        fun displayName(context: Context): String {
            val id = context.resources.getIdentifier(
                "lang_" + languageCode.lowercase(), "string", context.packageName)
            return if (id != 0) context.getString(id) else language
        }
    }

    /** Languages a deck ships a non-empty word file for, as (code, displayName). */
    fun languages(context: Context, deck: BoardGame): List<Pair<String, String>> =
        deck.languages.mapNotNull { code ->
            val model = loadDeck(context, deck.id.serializedName(), code)
            if (model == null || model.words.isEmpty()) null else code to model.displayName(context)
        }

    /** Emoji flag for a games.json language short-code (case-insensitive). */
    fun flag(code: String): String = when (code.lowercase()) {
        "eng" -> "🇬🇧"
        "ger" -> "🇩🇪"
        "fra" -> "🇫🇷"
        "ita" -> "🇮🇹"
        "cro" -> "🇭🇷"
        "srb" -> "🇷🇸"
        "slo" -> "🇸🇮"
        "bos" -> "🇧🇦"
        "esp" -> "🇪🇸"
        "swe" -> "🇸🇪"
        "rus" -> "🇷🇺"
        "dut" -> "🇳🇱"
        else -> "🏳️"
    }

    /** Reads a single deck raw resource (`{deckId}_{code}`) → its words + language name. */
    fun loadDeck(context: Context, deckId: String, code: String): DeckWords? {
        val resource = "${deckId}_${code.lowercase()}"
        val id = context.resources.getIdentifier(resource, "raw", context.packageName)
        if (id == 0) return null
        return try {
            val json = context.resources.openRawResource(id).use { stream ->
                String(stream.readBytes(), Charset.forName("UTF-8"))
            }
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("words")
            val words = ArrayList<String>(arr.length())
            for (i in 0 until arr.length()) {
                runCatching { arr.getString(i) }.getOrNull()?.let { words.add(it) }
            }
            DeckWords(
                language = obj.optString("language", code),
                languageCode = obj.optString("languageCode", code),
                words = words,
            )
        } catch (e: Exception) {
            null
        }
    }

    /** The frozen, ordered words the host uploads: deduped, shuffled, capped at 1000. */
    fun frozenWords(context: Context, deckId: String, code: String): List<String> {
        val model = loadDeck(context, deckId, code) ?: return emptyList()
        return model.words.filter { it.isNotEmpty() }.distinct().shuffled().take(MAX_WORDS)
    }
}
