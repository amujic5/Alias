package hr.azzi.socialgames.alias.Models

class UserResult(var username: String,
                 var score: Int,
                 var index: Int = 0) {

    var isMe: Boolean = false
}