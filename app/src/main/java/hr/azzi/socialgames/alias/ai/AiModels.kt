package hr.azzi.socialgames.alias.ai

/** Languages the AI feature supports. fileCode maps to the ai_<deck>_<fileCode>.json assets. */
enum class AILanguage(val code: String, val locale: String, val aiName: String, val fileCode: String) {
    EN("en", "en-US", "English", "eng"),
    FR("fr", "fr-FR", "French", "fra"),
    DE("de", "de-DE", "German", "ger"),
    IT("it", "it-IT", "Italian", "ita"),
    HR("hr", "hr-HR", "Croatian", "cro");

    val display: String get() = code.uppercase()

    companion object {
        fun from(code: String?): AILanguage = entries.firstOrNull { it.code == code } ?: EN
    }
}

enum class AIMode(val raw: String) {
    YOU_EXPLAIN("youExplain"),
    AI_EXPLAINS("aiExplains");

    companion object {
        fun from(raw: String?): AIMode = entries.firstOrNull { it.raw == raw } ?: AI_EXPLAINS
    }
}

/** A playable AI deck. nameRes/imageRes are resource NAMES resolved at runtime via getIdentifier. */
data class AIDeck(
    val id: String,
    val nameRes: String,
    val imageRes: String,
    val languages: List<AILanguage>,
)

data class AIPracticeConfig(
    val deck: AIDeck,
    val language: AILanguage,
    val totalSeconds: Int = 60,
    val mode: AIMode = AIMode.AI_EXPLAINS,
)

data class AIPracticeResult(
    val deckName: String,
    val language: String,
    val correctWords: List<String>,
    val skippedWords: List<String>,
    val totalSeconds: Int,
) {
    val correct: Int get() = correctWords.size
    val skipped: Int get() = skippedWords.size
    val played: Int get() = correct + skipped
    val accuracy: Double get() = if (played == 0) 0.0 else correct.toDouble() / played
}

enum class AIChallengeOutcome { WIN, DRAW, LOST, CREATOR }

data class AIChallenge(
    val id: String,
    val creatorId: String,
    val creatorName: String,
    val mode: String,
    val deckId: String,
    val deckName: String,
    val language: String,
    val totalSeconds: Int,
    val words: List<String>,
    val creatorScore: Int,
    val createdAt: Long,
    val players: List<String>,
    val playerScores: Map<String, Int>,
) {
    val shareUrl: String get() = "https://aliaswords.com/challenge/$id"
    val aiLanguage: AILanguage get() = AILanguage.from(language)
    val aiMode: AIMode get() = AIMode.from(mode)

    fun hasJoined(uid: String): Boolean = players.contains(uid)
    fun didNotFinish(uid: String): Boolean = players.contains(uid) && !playerScores.containsKey(uid)
    fun score(uid: String): Int = playerScores[uid] ?: 0
    fun outcome(uid: String): AIChallengeOutcome = when {
        uid == creatorId -> AIChallengeOutcome.CREATOR
        score(uid) > creatorScore -> AIChallengeOutcome.WIN
        score(uid) == creatorScore -> AIChallengeOutcome.DRAW
        else -> AIChallengeOutcome.LOST
    }
}

data class AIChallengePlay(
    val challengeId: String,
    val playerId: String,
    val playerName: String,
    val isCreator: Boolean,
    val correctWords: List<String>,
    val skippedWords: List<String>,
    val playedAt: Long,
    val finished: Boolean,
) {
    val score: Int get() = correctWords.size
    val total: Int get() = correctWords.size + skippedWords.size
}

data class AIUserStats(val wins: Int = 0, val losses: Int = 0, val draws: Int = 0)
