package hr.azzi.socialgames.alias.Models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class OnlineGame(var id: String?,
                 var dictionary: String,
                 var status: String,
                 var user: ArrayList<String>,
                 var word: String?,
                 var words: ArrayList<String>
                 ) : Parcelable {


    companion object {
        fun gameWithDictionary(map: Map<String, Any>): OnlineGame {

            val id = map.get("id") as? String
            val dictionary = map.get("dictionary") as? String
            val status = map.get("status") as? String
            val user = (map.get("user") as? List<String>) ?: ArrayList()
            val word = map.get("word") as? String
            val words = (map.get("words") as? List<String>) ?: ArrayList()

            val userList = ArrayList(user)
            val wordsList = ArrayList(words)

            return OnlineGame(id,dictionary ?: "CRO", status ?: "done", userList, word, wordsList)
        }
    }
}