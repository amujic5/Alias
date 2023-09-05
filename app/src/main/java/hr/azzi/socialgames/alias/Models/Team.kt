package hr.azzi.socialgames.alias.Models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Team(var firstPlayer: String,
                var secondPlayer: String,
                var teamName: String,
                private var _score: Int = 0,
                var playing: Boolean = true,
                var isKnockedOut: Boolean = false,
                var roundsPlayed: Int = 0,
                var deltaScoreRound: Int = 0
                ): Parcelable {

    fun getScore() : Int {
        return _score + deltaScoreRound
    }

    fun resetTeam() {
        isKnockedOut = false
        roundsPlayed = 0
        deltaScoreRound = 0
        _score = 0
    }

    fun updateScore() {
        _score += deltaScoreRound
        deltaScoreRound = 0
        if (_score < 0 ) {
            _score = 0
        }
    }

}
