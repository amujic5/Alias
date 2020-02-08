package hr.azzi.socialgames.alias.Service

import com.google.firebase.firestore.FirebaseFirestore
import hr.azzi.socialgames.alias.Models.UserResult

class UserResultService {

    val db = FirebaseFirestore.getInstance()

    fun getUserResults(gameId: String, callback: (ArrayList<UserResult>, UserResult) -> Unit) {

        val userResults = ArrayList<UserResult>()
        var adminUserResult: UserResult = UserResult("admin", 0)

        db
            .collection("Games/$gameId/Score")
            .orderBy("score")
            .get()
            .addOnCompleteListener {
                val documents = it.result?.documents

                documents?.forEach {
                    val username = it.id
                    val score = (it["score"] as? Int) ?: 0
                    val admin = (it["admin"] as? Boolean) ?: false

                    val userResult = UserResult(username, score)
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
