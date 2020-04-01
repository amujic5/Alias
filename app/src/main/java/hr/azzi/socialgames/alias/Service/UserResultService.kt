package hr.azzi.socialgames.alias.Service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hr.azzi.socialgames.alias.Models.UserResult

class UserResultService {

    val db = FirebaseFirestore.getInstance()

    fun getUserResults(gameId: String, callback: (ArrayList<UserResult>, UserResult) -> Unit) {

        val userResults = ArrayList<UserResult>()
        var adminUserResult: UserResult = UserResult("admin", 0, 0, false)

        db
            .collection("Games/$gameId/Score")
            .orderBy("score")
            .get()
            .addOnCompleteListener {
                val documents = it.result?.documents

                var position = 1
                var lastScore: Long = 0

                documents?.forEachIndexed { index, documentSnapshot ->
                    val username = documentSnapshot.id
                    val score = (documentSnapshot["score"] as? Long) ?: 0
                    val admin = (documentSnapshot["admin"] as? Boolean) ?: false

                    if (score < lastScore) {
                        position = index + 1
                    }

                    lastScore = score

                    val myUsername = FirebaseAuth.getInstance().currentUser?.displayName ?: ""
                    val isMe =  username == myUsername

                    if (isMe) {
                        saveResult(score.toInt(), gameId)
                    }

                    val userResult = UserResult(username, score.toInt(), position, isMe)
                    if (admin) {
                        adminUserResult = userResult
                    } else {
                        userResults.add(userResult)
                    }
                }

                callback(userResults, adminUserResult)
            }
    }

    fun saveResult(result: Int, gameId: String) {
        val myUdid = FirebaseAuth.getInstance().currentUser?.uid
        val username = FirebaseAuth.getInstance().currentUser?.displayName
        if (username == null) {
            return
        }
        if (myUdid == null) {
            return
        }

        val userRef= db.document("Users/" + myUdid)
        db.runTransaction {

            val userDocument= it.get(userRef)
            if (userDocument.data == null) {
                val scores = HashMap<String, Int>()
                scores.set(gameId, result)
                val map = hashMapOf<String, Any>("scores" to scores, "username" to username)
                it.set(userRef, map)
            } else {
                var scores = (userDocument.data?.get("scores") as? HashMap<String, Int>) ?: HashMap<String, Int>()
                scores.set(gameId, result)
                val map = hashMapOf<String, Any>("scores" to scores, "username" to username)
                it.update(userRef, map)
            }


        }
    }

    companion object {
        val instance = UserResultService()
    }

}
