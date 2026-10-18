package hr.azzi.socialgames.alias.Models

import android.os.Parcelable
import hr.azzi.socialgames.alias.R
import hr.azzi.socialgames.alias.Service.DictionaryService
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

/**
 * @param points what guessing this word is worth. One everywhere except the scored decks,
 * where an easy word is 1 and a hard one 3.
 */
@Parcelize
class MarkedWord(var word: String, var isCorrect: Boolean, var points: Int = 1) : Parcelable {}


@Parcelize
class Game(private var _reverseExplaingAnsweringDirection: Boolean,
           private var _time: Int,
           private var _goalScore: Int,
           private var _teams: ArrayList<Team>,
           private var _words: ArrayList<String>,
           var _currentTeamIndex: Int,
           var currentTeamMarkedWords: ArrayList<MarkedWord> = ArrayList(),
           /** True on decks whose words carry their own value — see [newWord]. */
           private var _isScored: Boolean = false,
           /** One word a round, drawn rather than described. See PlayScreen. */
           private var _isDrawGuess: Boolean = false,
           /** After Dark: words carry a segment and a round climbs through them. See [newWord]. */
           private var _isSegmented: Boolean = false,
           /**
            * The deck as it was handed over. [_words] is dealt from and never refilled, so a
            * long game on a small deck — Junior is 235 words — empties it and the next draw
            * has nothing to pick from. Kept so play can carry on: repeating words late in a
            * marathon game is better than ending it.
            */
           private val _deck: ArrayList<String> = ArrayList(_words) ) : Parcelable {

    private val _playingTeams: List<Team>
        get() {
            return _teams.filter {
                !it.isKnockedOut
            }
        }

    // PUBLIC

    fun setTeamMarkedWords(markedWords: ArrayList<MarkedWord>) {
        currentTeamMarkedWords = markedWords
        currentTeam.deltaScoreRound = currentRoundScore
    }

    /** Whether this game's words carry their own value — the pill above the word. */
    val isScored: Boolean
        get() = _isScored

    /** One word a round, drawn rather than described. */
    val isDrawGuess: Boolean
        get() = _isDrawGuess

    /** Whether this game's words carry an After Dark segment — the climb above the buttons. */
    val isSegmented: Boolean
        get() = _isSegmented

    val currentRoundScore: Int
        get() {
            // A miss only costs a point on the decks that deduct for one. Every talking
            // deck does, and has to: the pile is deep, and without a cost the fastest
            // strategy is to skip everything hard. Draw & Guess has nothing to burn
            // through — the round is one word — so a miss is worth nothing rather than
            // worth less than nothing.
            val penalty = if (_isDrawGuess) 0 else currentSkipAnswers
            return currentCorrectPoints - penalty
        }

    /**
     * What the guessed words are worth. The same as their count on every deck but the
     * scored ones, where each word carries its own value.
     */
    val currentCorrectPoints: Int
        get() {
            return currentTeamMarkedWords.filter { it.isCorrect }.sumOf { it.points }
        }

    val currentCorrectAnswers: Int
        get() {
            return currentTeamMarkedWords.filter {it.isCorrect}.size
        }

    val currentSkipAnswers: Int
        get() {
            return currentTeamMarkedWords.filter {!it.isCorrect}.size
        }

    // PUBLIC GETTER

    val sortedTeams: List<Team>
        get() {

            return _teams.sortedWith(compareBy({ it.getScore()})).reversed()
        }

    val currentTeam: Team
        get(){
            return _teams[_currentTeamIndex]
        }

    val nextTeam: Team?
        get() {
            if (winnerTeam != null) {
                return null
            }

            return _teams[_nextTeamIndex ]
        }


    val nextExplainingPlayerName: String?
        get(){
            if (_reverseExplaingAnsweringDirection != _isLastTeamInRound()) {
                return nextTeam?.secondPlayer
            } else {
                return nextTeam?.firstPlayer
            }
        }

    val nextAnsweringPlayerName: String?
        get(){
            if (_reverseExplaingAnsweringDirection != _isLastTeamInRound()) {
                return nextTeam?.firstPlayer
            } else {
                return nextTeam?.secondPlayer
            }
        }

    val explainingPlayerName: String
    get() {
        if (_reverseExplaingAnsweringDirection) {
            return currentTeam.secondPlayer
        } else {
            return  currentTeam.firstPlayer
        }
    }

    val answeringPlayerName: String
        get() {
            if (!_reverseExplaingAnsweringDirection) {
                return currentTeam.secondPlayer
            } else {
                return  currentTeam.firstPlayer
            }
        }


    val winnerTeam: Team?
        get(){
            knockOutTeamsIfNeeded()
            if (_playingTeams.size == 1) {
                return _playingTeams.first()
            } else {
                return null
            }
    }

    val time: Int
        get() {
            return _time
        }

    /**
     * What the word now on screen is worth. Read when the word is marked, so it has to be
     * set as the word is handed out and left alone until the next one.
     */
    @IgnoredOnParcel
    var currentWordPoints: Int = 1
        private set

    /** Which After Dark segment the word now on screen belongs to; null on every other deck. */
    @IgnoredOnParcel
    var currentWordSegment: AfterDarkSegment? = null
        private set

    val newWord: String
        get() {
            if (_isSegmented) return newAfterDarkWord()
            currentWordSegment = null

            // Dealt the deck out — start it again rather than draw from nothing.
            if (_words.isEmpty()) _words.addAll(_deck)
            if (_words.isEmpty()) {
                currentWordPoints = 1
                return ""
            }

            // indices.random() rather than a hand-rolled bound: the previous
            // nextInt(0, size - 1) threw once a single word was left, and could never
            // deal the last word in the list.
            val randomIndex = _words.indices.random()
            val wordString = _words[randomIndex]
            _words.removeAt(randomIndex)

            if (!_isScored) {
                currentWordPoints = 1
                return wordString
            }
            val (word, points) = splitScored(wordString)
            currentWordPoints = points
            return word
        }

    // After Dark

    /**
     * The segment this round has climbed to.
     *
     * Counted from the round's own correct answers, so it is not state that can drift:
     * a round starts at SPARK because its list starts empty, and it never steps back
     * because a correct answer is never taken away mid-round. A skip costs a point but
     * not the climb. At CLIMAX it stays there.
     */
    val afterDarkLevel: AfterDarkSegment
        get() {
            val all = AfterDarkSegment.values()
            return all[minOf(currentCorrectAnswers / AFTER_DARK_HITS_PER_LEVEL, all.size - 1)]
        }

    /** Correct answers still needed to reach CLIMAX; zero once there. */
    val afterDarkHitsToClimax: Int
        get() {
            val needed = (AfterDarkSegment.values().size - 1) * AFTER_DARK_HITS_PER_LEVEL
            return maxOf(0, needed - currentCorrectAnswers)
        }

    /** The extra points the round has earned on top of one per word — what CLIMAX pays. */
    val currentBonusPoints: Int
        get() = currentTeamMarkedWords.filter { it.isCorrect }.sumOf { it.points - 1 }

    /**
     * Deals from the round's segment rather than from the whole deck.
     *
     * Drawn out of [_words] like every other deck, so what has been dealt travels with the
     * game between screens; a segment that has run dry is refilled from [_deck] alone, so a
     * long game deals the whole of CLIMAX before it repeats any of it.
     */
    private fun newAfterDarkWord(): String {
        val level = afterDarkLevel
        var pile = _words.indices.filter { AfterDarkSegment.split(_words[it]).second == level }
        if (pile.isEmpty()) {
            _words.addAll(_deck.filter { AfterDarkSegment.split(it).second == level })
            pile = _words.indices.filter { AfterDarkSegment.split(_words[it]).second == level }
        }
        // A deck file without segments has nothing to climb through. It still plays, one
        // point a word, rather than dealing nothing.
        if (pile.isEmpty()) {
            if (_words.isEmpty()) _words.addAll(_deck)
            if (_words.isEmpty()) {
                currentWordSegment = null
                currentWordPoints = 1
                return ""
            }
            val (word, segment) = AfterDarkSegment.split(_words.removeAt(_words.indices.random()))
            currentWordSegment = segment
            currentWordPoints = 1
            return word
        }
        val (word, segment) = AfterDarkSegment.split(_words.removeAt(pile.random()))
        currentWordSegment = segment
        currentWordPoints = segment?.points ?: 1
        return word
    }

    // Public functions

    fun resetGame() {
        _currentTeamIndex = 0
        _teams.forEach {
            it.resetTeam()
        }
    }

    fun currentTeamHasFinishedTheRound() {
        currentTeam.roundsPlayed += 1
        currentTeam.deltaScoreRound = currentRoundScore
    }

    fun newRound() {
        currentTeam.updateScore()
        currentTeamMarkedWords = ArrayList()
        knockOutTeamsIfNeeded()

        if (_isLastTeamInRound()) {
            _reverseExplaingAnsweringDirection = !_reverseExplaingAnsweringDirection
        }
        _increaseCurrentTeamIndex()
    }

    fun knockOutTeamsIfNeeded() {

        val shouldKnockOutTeams =
            _isLastTeamInRound()
                    &&
                    _teams.filter {
                        it.getScore() >= _goalScore
                    }.isNotEmpty()



        val bestScoreTeam = _teams.maxBy { it.getScore() }

        val maxCurrentScore: Int = bestScoreTeam?.getScore() ?: 0

        if (!shouldKnockOutTeams ) {
            if (_isLastTeamInRound()) {
                if (maxCurrentScore < _goalScore) {
                    _teams.forEach {
                        it.isKnockedOut = false
                    }
                } else {
                    _teams.forEach {
                        it.isKnockedOut = it.getScore()< maxCurrentScore
                    }
                }
            }

            return
        }

        _teams.forEach {
            it.isKnockedOut = it.getScore()< maxCurrentScore
        }
    }

    fun reviewWord(index: Int) {
        var markedWord = currentTeamMarkedWords[index]
        markedWord.isCorrect = !markedWord.isCorrect

        currentTeamMarkedWords[index] = markedWord
        currentTeam.deltaScoreRound = currentRoundScore
    }

    fun addMarkedWord(markedWord: MarkedWord) {
        currentTeamMarkedWords.add(markedWord)
        print(currentCorrectAnswers)
    }

    // Private

    private fun _increaseCurrentTeamIndex() {
        _currentTeamIndex = _nextTeamIndex
    }

    private val _nextTeamIndex: Int
    get(){
        var searchOffset = 0

        if (!_isLastTeamInRound()) {
            searchOffset = _currentTeamIndex + 1
        }

        val nextTeam = _teams
                                .drop(searchOffset)
                                .filter { !it.isKnockedOut }
                                .first()

        return _teams.indexOf(nextTeam)
    }

    private fun _isLastTeamInRound() : Boolean {
        val searchOffset = _currentTeamIndex + 1
        return _teams
            .drop(searchOffset)
            .filter { !it.isKnockedOut }
            .size == 0

    }


    companion object {
        /** How many correct answers move a round up one segment. */
        const val AFTER_DARK_HITS_PER_LEVEL = 1

        /**
         * Splits `"Ananas-2"` into the word and its value.
         *
         * The suffix is part of the deck file rather than a second field so the JSON stays
         * the shape every other deck has. Anything without a readable `-1`..`-3` tail is
         * taken whole and worth a point — a missing suffix should cost a word, not a round.
         */
        fun splitScored(entry: String): Pair<String, Int> {
            val dash = entry.lastIndexOf('-')
            if (dash < 0) return entry to 1
            val points = entry.substring(dash + 1).toIntOrNull()
            if (points == null || points !in 1..3) return entry to 1
            val word = entry.substring(0, dash).trim()
            return if (word.isEmpty()) entry to 1 else word to points
        }
    }

}

/**
 * How far into the night an After Dark word is, from the least sexual to the most.
 *
 * It says nothing about how hard the word is to guess — a segment is about what the word
 * means, so a table can tell how spicy the deck is getting, not how many points it is worth.
 * Written into the deck file as a suffix (`zadnja runda-SPARK`), the same way the scored
 * deck carries its `-1`…`-3`, so the JSON keeps the shape every other deck has.
 */
enum class AfterDarkSegment(val raw: String, val titleRes: Int) {
    /** Going out, nightlife, small vices, parties and social blunders. Nothing sexual. */
    SPARK("SPARK", R.string.after_dark_segment_spark),
    /** Attraction, flirting, dating, apps, relationships and their complications. */
    ATTRACTION("ATTRACTION", R.string.after_dark_segment_attraction),
    /** Kissing, touching, innuendo, lingerie, erotic shows and intimate mishaps. */
    EXCITEMENT("EXCITEMENT", R.string.after_dark_segment_excitement),
    /** Sex itself: acts, the body, toys, protection, preferences and fantasies. */
    CLIMAX("CLIMAX", R.string.after_dark_segment_climax);

    /**
     * What a correct answer is worth. CLIMAX pays double — it is the top of the climb and
     * the round stays there, so it is where the points are.
     */
    val points: Int
        get() = if (this == CLIMAX) 2 else 1

    companion object {
        /**
         * Splits `"zadnja runda-SPARK"` into the word, ready for the screen, and its segment.
         *
         * The deck file is written in lower case, so the first letter is raised here to match
         * every other deck. Split on the last dash, since a word may have one of its own
         * (`g-točka`). Anything without a readable tail is taken whole and has no segment —
         * a missing suffix should cost the label, not the word.
         */
        fun split(entry: String): Pair<String, AfterDarkSegment?> {
            val dash = entry.lastIndexOf('-')
            val segment = if (dash < 0) null else values().firstOrNull { it.raw == entry.substring(dash + 1) }
            if (segment == null) return capitalizingFirst(entry) to null
            val word = entry.substring(0, dash).trim()
            return if (word.isEmpty()) capitalizingFirst(entry) to null else capitalizingFirst(word) to segment
        }

        private fun capitalizingFirst(word: String): String =
            word.replaceFirstChar { it.uppercase() }
    }
}
