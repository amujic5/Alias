package hr.azzi.socialgames.alias.Models

class UserLeader(var username: String?,
                 var totalScore: Int,
                 var avgScore: Float,
                 var scores: HashMap<String, Long>?,
                 var index: Int,
                 var isMe: Boolean) {
}