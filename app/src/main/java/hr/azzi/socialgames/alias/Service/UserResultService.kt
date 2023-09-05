package hr.azzi.socialgames.alias.Service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hr.azzi.socialgames.alias.Models.UserResult

class UserResultService {

    val db = FirebaseFirestore.getInstance()

    companion object {
        val instance = UserResultService()
    }

}
