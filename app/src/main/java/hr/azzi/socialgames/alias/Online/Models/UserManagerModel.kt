package hr.azzi.socialgames.alias.Online.Models

import com.google.firebase.auth.FirebaseAuth

class UserManagerModel {

    companion object {

        var _username: String? = null

        fun username(): String {
            return _username ?: "Guest"
        }
    }
}