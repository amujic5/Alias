package hr.azzi.socialgames.alias.Models

import android.content.Context
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class DictionaryModel(var language: String,
                      var languageCode: String,
                      var words: List<String>,
                      var imageURLString: String?): Parcelable {

    /**
     * The language name to show the player, in the app's language.
     *
     * Derived from [languageCode] rather than [language]: the raw values in the deck files are
     * inconsistent (country names like "Germany", the Slovene word "Slovenski", "Croatia" next to
     * "Croatian") and they are English regardless of the app language. [language] itself stays
     * untouched — it is an identifier, used for analytics and written into Online Play rooms.
     * Falls back to the raw value for any code without a translation.
     */
    fun displayName(context: Context): String {
        val id = context.resources.getIdentifier(
            "lang_" + languageCode.lowercase(), "string", context.packageName)
        return if (id != 0) context.getString(id) else language
    }

    companion object {
        fun dictionaryFromJSON(json: Map<String, Any>): DictionaryModel {

            val language = json["language"] as String
            val languageCode = json["languageCode"] as String
            val wordsAPI = json["words"] as List<String>
            val wordsSet = wordsAPI.toSet()
            val words = wordsSet.toList()
            val imageURLString = json["imageURLString"]?.toString()

            return DictionaryModel(language, languageCode, words, imageURLString)
        }

    }
}