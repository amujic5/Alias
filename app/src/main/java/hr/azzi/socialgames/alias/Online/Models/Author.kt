package hr.azzi.socialgames.alias.Online.Models

import com.stfalcon.chatkit.commons.models.IUser

class Author(var userId: String, var username: String,  var useravatar: String?): IUser {
    override fun getAvatar(): String {
        return ""
    }

    override fun getName(): String {
        return username
    }

    override fun getId(): String {
        return userId
    }
}