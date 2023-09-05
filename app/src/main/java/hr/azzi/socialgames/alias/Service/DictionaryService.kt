package hr.azzi.socialgames.alias.Service

import android.content.Context
import hr.azzi.socialgames.alias.Models.DictionaryModel
import hr.azzi.socialgames.alias.R
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.Charset


class DictionaryService {

    companion object {
        val instance = DictionaryService()
        var playingDictionary: DictionaryModel? = null
    }

    fun getDictionaries(context: Context):  MutableList<DictionaryModel> {

        var localDictionaries: MutableList<DictionaryModel> = mutableListOf()

        val jsonArray = JSONObject(loadJSONFromAsset(context)).getJSONArray("dictionaries")

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)

            val imageUrlString = item.getString("imageURLString")
            val language = item.getString("language")
            val languageCode = item.getString("languageCode")

            var words: MutableList<String> = mutableListOf()

            val wordsJsonArray = item.getJSONArray("words")
            for (i in 0 until wordsJsonArray.length()) {
                val word = wordsJsonArray.getString(i)
                words.add(word)
            }

            val dictionary = DictionaryModel(language, languageCode, words, imageUrlString)
            localDictionaries.add(dictionary)

        }

        return localDictionaries
    }

    fun loadJSONFromAsset(context: Context): String {
        var json: String? = null
        try {

            val `is` = context.resources.openRawResource(R.raw.full_json)

            val size = `is`.available()

            val buffer = ByteArray(size)

            `is`.read(buffer)

            `is`.close()

            json = String(buffer, Charset.forName("UTF-8"))


        } catch (ex: IOException) {
            ex.printStackTrace()
            return ""
        }

        return json
    }

}