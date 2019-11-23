package hr.azzi.socialgames.alias.Models

import android.os.Parcelable
import hr.azzi.socialgames.alias.Service.DictionaryService
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import kotlin.random.Random

@Parcelize
class MarkedWord(var word: String, var isCorrect: Boolean) : Parcelable {}


@Parcelize
class Game(private var _reverseExplaingAnsweringDirection: Boolean,
           private var _time: Int,
           private var _goalScore: Int,
           private var _teams: ArrayList<Team>,
           private var _words: ArrayList<String>,
           private var _currentTeamIndex: Int,
           var currentTeamMarkedWords: ArrayList<MarkedWord> = ArrayList() ) : Parcelable {

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

    val currentRoundScore: Int
        get() {
            return currentCorrectAnswers - currentSkipAnswers
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

    val newWord: String
        get() {

            val randomIndex = randomValue(0, _words.size - 1)
            val wordString = _words[randomIndex]
            _words.removeAt(randomIndex)

            return wordString
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

    private fun randomValue(min: Int, max: Int) : Int {
        return Random.nextInt(min, max)
    }

}

