package hr.azzi.socialgames.alias.Models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class DictionaryModel(var language: String,
                      var languageCode: String,
                      var words: List<String>,
                      var imageURLString: String?): Parcelable {

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