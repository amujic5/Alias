package hr.azzi.socialgames.alias.Online.Models

enum class MessageType(val text: String) {
    CORRECT("correct"),
    WRONG("wrong"),
    EXPLAIN("explain");

    companion object  {
        fun initFrom(text: String): MessageType {
            if (text == "correct") {
                return CORRECT
            }
            if (text == "wrong") {
                return WRONG
            }
            return EXPLAIN
        }
    }

}