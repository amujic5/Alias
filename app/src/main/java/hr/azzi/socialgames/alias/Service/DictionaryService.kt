package hr.azzi.socialgames.alias.Service

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import hr.azzi.socialgames.alias.Models.DictionaryModel
import hr.azzi.socialgames.alias.R
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
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

data class BoardGames(
    @SerializedName("games")
    val games: List<BoardGame>
)

data class BoardGame(
    @SerializedName("id")
    val id: BoardGameId,
    @SerializedName("name")
    val name: String,
    @SerializedName("image")
    val image: String,
    @SerializedName("languages")
    val languages: List<String>,
    @SerializedName("age")
    val age: String,
    @SerializedName("time")
    val time: String,
    @SerializedName("players")
    val players: String,
    @SerializedName("info")
    val info: String
) : Parcelable {
    val isTaboo: Boolean
        get() = id == BoardGameId.TABOO

    val isAliasJunior: Boolean
        get() = id == BoardGameId.JUNIOR

    constructor(parcel: Parcel) : this(
        parcel.readSerializable() as BoardGameId,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.createStringArrayList()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(id)
        parcel.writeString(name)
        parcel.writeString(image)
        parcel.writeStringList(languages)
        parcel.writeString(age)
        parcel.writeString(time)
        parcel.writeString(players)
        parcel.writeString(info)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BoardGame> {
        override fun createFromParcel(parcel: Parcel): BoardGame {
            return BoardGame(parcel)
        }

        override fun newArray(size: Int): Array<BoardGame?> {
            return arrayOfNulls(size)
        }
    }
}
enum class BoardGameId {
    @SerializedName("game_alias_classic")
    CLASSIC,
    @SerializedName("game_alias_football")
    FOOTBALL,
    @SerializedName("game_alias_cro")
    CRO,
    @SerializedName("game_taboo")
    TABOO,
    @SerializedName("game_alias_junior")
    JUNIOR,
    @SerializedName("game_alias_slo")
    SLO,
    @SerializedName("game_alias_uk")
    UK
}

fun BoardGameId.serializedName(): String {
    val field = this::class.java.getField(this.name)
    val serializedAnnotation = field.getAnnotation(SerializedName::class.java)
    return serializedAnnotation?.value ?: this.name
}

object JSONService {
    val boardGames: MutableList<BoardGame> = mutableListOf()
    fun rawId(file: String): Int {
        if (file == "games") {
            return R.raw.games
        } else if (file == "game_alias_classic_bos") {

        }
        return 0
    }

    fun loadBoardGamesFromFile(context: Context) {


        val inputStream = context.resources.openRawResource(rawId("games"))
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = bufferedReader.use { it.readText() }
        val gson = Gson()
        var boardGames = gson.fromJson(jsonString, BoardGames::class.java)
        // TODO: filter based on locale
//        val userLocale = Locale.getDefault()
//        val regionCode = userLocale.country
//        val languageCode = userLocale.language
//
//        // remove alias cro if needed
//        if (regionCode.toLowerCase() != "hr" && languageCode.toLowerCase() != "hr") {
//            boardGames.games.removeAll { it.id == BoardGameId.CRO }
//        }
//
//        // remove alias slo if needed
//        if (regionCode.toLowerCase() != "si" && languageCode.toLowerCase() != "sl") {
//            boardGames.games.removeAll { it.id == BoardGameId.SLO }
//        }


        this.boardGames.addAll(boardGames.games)
        this.boardGames.removeIf {
            it.id == BoardGameId.TABOO || it.id == BoardGameId.JUNIOR
        }
    }

    fun getDictionaries(context: Context, boardGame: BoardGame):  MutableList<DictionaryModel> {

        var localDictionaries: MutableList<DictionaryModel> = mutableListOf()

        val names = boardGame.languages.map { boardGame.id.serializedName() + "_" + it }.map { it.lowercase() }

        val jsonArray = names.map {
            val id = context.resources.getIdentifier(it, "raw", context.packageName)
            JSONObject(loadJSONFromAsset(context, id))
        }

        for (i in 0 until jsonArray.size) {
            val item = jsonArray[i]

            val imageUrlString = item.getString("imageURLString")
            val language = item.getString("language")
            val languageCode = item.getString("languageCode")

            var words: MutableList<String> = mutableListOf()

            val wordsJsonArray = item.getJSONArray("words")
            for (i in 0 until wordsJsonArray.length()) {
                try {
                    val word = wordsJsonArray.getString(i)
                    words.add(word)
                } catch (error: JSONException){

                }
            }

            val dictionary = DictionaryModel(language, languageCode, words, imageUrlString)
            localDictionaries.add(dictionary)
        }

        return localDictionaries
    }

    fun loadJSONFromAsset(context: Context, id: Int): String {
        var json: String? = null
        try {

            val `is` = context.resources.openRawResource(id)

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