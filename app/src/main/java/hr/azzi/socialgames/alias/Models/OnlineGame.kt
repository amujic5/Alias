package hr.azzi.socialgames.alias.Models

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.android.parcel.Parcelize


@Parcelize
class OnlineGame(var id: String?,
                 var dictionary: String,
                 var status: String,
                 var user: ArrayList<String>,
                 var word: String?,
                 var words: ArrayList<String>,
                 var admin: String?,
                 var date: Timestamp?
                 ) : Parcelable {

    fun isPlaying(): Boolean {
        return status == "playing"
    }

    fun isWaiting(): Boolean {
        return status == "waiting"
    }

    fun isFinished(): Boolean {
        return status == "finished"
    }

    companion object {
        fun gameWithDictionary(map: Map<String, Any>): OnlineGame {

            val id = map.get("id") as? String
            val dictionary = map.get("dictionary") as? String
            val status = map.get("status") as? String
            val user = (map.get("user") as? List<String>) ?: ArrayList()
            val word = map.get("word") as? String
            val words = (map.get("words") as? List<String>) ?: ArrayList()
            var admin = map.get("admin") as? String
            var date = map.get("date") as? Timestamp

            val userList = ArrayList(user)
            val wordsList = ArrayList(words)

            return OnlineGame(id,dictionary ?: "CRO", status ?: "done", userList, word, wordsList, admin, date)
        }
    }
}