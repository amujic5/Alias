package hr.azzi.socialgames.alias.Service

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

                    val userResult = UserResult(username, score.toInt(), position,false)
                    if (admin) {
                        adminUserResult = userResult
                    } else {
                        userResults.add(userResult)
                    }
                }

                callback(userResults, adminUserResult)
            }
    }

    companion object {
        val instance = UserResultService()
    }

}
