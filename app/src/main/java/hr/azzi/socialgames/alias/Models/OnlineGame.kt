package hr.azzi.socialgames.alias.Models

class OnlineGame(var dictionary: String,
                 var status: String,
                 var user: ArrayList<String>,
                 var word: String?,
                 var words: ArrayList<String>
                 ) {


    companion object {
        fun gameWithDictionary(map: Map<String, Any>): OnlineGame {

            val dictionary = map.get("dictionary") as? String
            val status = map.get("status") as? String
            val user = (map.get("user") as? List<String>) ?: ArrayList()
            val word = map.get("word") as? String
            val words = (map.get("words") as? List<String>) ?: ArrayList()

            val userList = ArrayList(user)
            val wordsList = ArrayList(words)

            return OnlineGame(dictionary ?: "CRO", status ?: "done", userList, word, wordsList)
        }
    }
}