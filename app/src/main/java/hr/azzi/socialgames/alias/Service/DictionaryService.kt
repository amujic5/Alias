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
        var playingDictionary: DictionaryModel? = null
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
    val info: String,
    @SerializedName("free")
    val free: Boolean = true
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
        parcel.readString()!!,
        parcel.readByte() != 0.toByte()
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
        parcel.writeByte(if (free) 1 else 0)
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
    @SerializedName("game_alias_srb")
    SRB,
    @SerializedName("game_alias_bos")
    BOS,
    @SerializedName("game_taboo")
    TABOO,
    @SerializedName("game_alias_junior")
    JUNIOR,
    @SerializedName("game_alias_slo")
    SLO,
    @SerializedName("alias_ger")
    GER,
    @SerializedName("game_alias_uk")
    UK,
    @SerializedName("alias_nba")
    NBA,
    @SerializedName("alias_movies")
    MOVIES,
    @SerializedName("game_alias_who_am_i")
    WHOAMI
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

    /** The country ("local-theme") deck for a given language short-code, if any. */
    private fun countryDeckLanguage(id: BoardGameId): String? = when (id) {
        BoardGameId.CRO -> "CRO"
        BoardGameId.SRB -> "SRB"
        BoardGameId.BOS -> "BOS"
        BoardGameId.SLO -> "SLO"
        BoardGameId.GER -> "GER"
        BoardGameId.UK  -> "ENG"
        else -> null
    }

    fun loadBoardGamesFromFile(context: Context) {
        val inputStream = context.resources.openRawResource(rawId("games"))
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = bufferedReader.use { it.readText() }
        val gson = Gson()
        val boardGames = gson.fromJson(jsonString, BoardGames::class.java)

        // Rebuild from scratch so this is safe to call again after a language change.
        this.boardGames.clear()
        this.boardGames.addAll(boardGames.games)

        // The locally-themed "country" decks (cro/srb/bos/slo/ger/uk) only show when
        // their language is the one selected in the app — mirrors iOS.
        val selected = LanguageManager.deckLanguageCode
        this.boardGames.removeIf { game ->
            countryDeckLanguage(game.id)?.let { it != selected } ?: false
        }

        // Junior (picture cards) and Taboo (forbidden-words card) are both supported in
        // local play now — see PlayScreen. No board games are filtered by type here.
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