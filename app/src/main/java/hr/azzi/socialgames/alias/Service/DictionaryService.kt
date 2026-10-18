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

        /**
         * The deck the current game is being played on. Set beside [playingDictionary] when
         * the game starts, because Play only receives the Game itself — the deck never
         * travels that far — and the round-played event has to name it.
         */
        var playingBoardGame: BoardGame? = null
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

    /**
     * Decks whose words carry their own value, written as a `-1`/`-2`/`-3` suffix that
     * never reaches the screen. Everything else scores a flat point per word.
     */
    /**
     * One word is drawn on the phone and everyone else calls out guesses. The round is
     * that one word, so there is no skipping to the next — see PlayScreen.
     */
    val isDrawGuess: Boolean
        get() = id == BoardGameId.DRAW_GUESS

    /**
     * Whether a word that was not guessed costs the team a point.
     *
     * Every talking deck takes one back for a skip, and it has to: the pile is deep, and
     * without a cost the fastest strategy is to skip everything hard. Draw & Guess has
     * nothing to burn through — the round is one word, drawn once — so a miss is worth
     * nothing rather than worth less than nothing.
     */
    val deductsForMisses: Boolean
        get() = id != BoardGameId.DRAW_GUESS

    /**
     * The Play Store product that unlocks this deck.
     *
     * Every other paid deck is sold under its own id, so that is the default. Draw &
     * Guess and After Dark are the exceptions: their products were set up as
     * `draw_and_guess` and `after_dark` while the decks are `game_draw_guess` and
     * `game_alias_after_dark`, and a product id cannot be renamed once the store has seen
     * it. Better one mapping here than a rename rippling through the deck files.
     */
    val productId: String
        get() = when (id) {
            BoardGameId.DRAW_GUESS -> "draw_and_guess"
            BoardGameId.AFTER_DARK -> "after_dark"
            else -> id.serializedName()
        }

    /**
     * Whether the deck is called out as new on the Choose a Game grid. Listed by hand,
     * and meant to be: "new" is a marketing decision with an expiry date, and nothing in
     * the data can tell when it stops being true. Its counterpart, the PREMIUM badge, is
     * derived from `free` instead — that one is a fact.
     */
    val isNew: Boolean
        get() = id == BoardGameId.DRAW_GUESS || id == BoardGameId.EASY_MEDIUM_HARD ||
            id == BoardGameId.AFTER_DARK

    /** The goal score this deck is played to. One word a round scores far slower than a
     *  deck you can burn through, so it is counted in single figures. */
    val goalScoreRange: IntRange
        get() = if (id == BoardGameId.DRAW_GUESS) 5..20 else 40..120

    val isScored: Boolean
        get() = id == BoardGameId.EASY_MEDIUM_HARD

    /**
     * Decks whose words carry the segment they belong to, written as a `-SPARK`…`-CLIMAX`
     * suffix that never reaches the screen. See [hr.azzi.socialgames.alias.Models.AfterDarkSegment].
     */
    val isSegmented: Boolean
        get() = id == BoardGameId.AFTER_DARK

    /**
     * Decks that only make sense passing one phone around: the AI reads its words aloud
     * and Online Play freezes a list for strangers, and neither has anything to do with
     * a word being worth three points.
     *
     * After Dark is local for a different reason: it is an adult deck, meant for a table of
     * friends who chose it together — not for a room of strangers, nor for the AI.
     */
    val isLocalOnly: Boolean
        get() = id == BoardGameId.EASY_MEDIUM_HARD || id == BoardGameId.DRAW_GUESS ||
            id == BoardGameId.AFTER_DARK

    /** Nothing about a drawing round is worth filming, and the front camera would spend
     *  it looking at whoever is holding the phone. */
    val allowsDualCamera: Boolean
        get() = id != BoardGameId.DRAW_GUESS

    /**
     * Whether a locked deck can be opened for one session by watching a rewarded video.
     * Off for Easy Medium Hard: it is bought, not sampled.
     */
    val allowsFreeTry: Boolean
        get() = id != BoardGameId.EASY_MEDIUM_HARD

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
    WHOAMI,
    @SerializedName("easy_medium_hard")
    EASY_MEDIUM_HARD,
    @SerializedName("game_alias_esp")
    ESP,
    @SerializedName("game_alias_ita")
    ITA,
    @SerializedName("game_alias_fra")
    FRA,
    @SerializedName("game_draw_guess")
    DRAW_GUESS,
    @SerializedName("game_alias_after_dark")
    AFTER_DARK
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

    /** The country ("local-theme") deck for a given language short-code, if any.
     *  Every country deck belongs here: one that is missing falls through to null and
     *  then shows in every language, which is how the Italian and French decks first
     *  shipped. */
    private fun countryDeckLanguage(id: BoardGameId): String? = when (id) {
        BoardGameId.CRO -> "CRO"
        BoardGameId.SRB -> "SRB"
        BoardGameId.BOS -> "BOS"
        BoardGameId.SLO -> "SLO"
        BoardGameId.GER -> "GER"
        BoardGameId.UK  -> "ENG"
        BoardGameId.ESP -> "ESP"
        BoardGameId.ITA -> "ITA"
        BoardGameId.FRA -> "FRA"
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