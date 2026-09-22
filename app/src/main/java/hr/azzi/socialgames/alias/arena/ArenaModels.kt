package hr.azzi.socialgames.alias.arena

/**
 * The shapes the Arena screens render, decoded from what the server writes.
 *
 * Android port of iOS `ArenaModels.swift`. The field names are the server's, not
 * Kotlin's, because the same Firestore documents feed both apps — `w` is a word and
 * `net` is guessed-minus-skipped on either platform, and renaming them here would only
 * move the translation somewhere less obvious.
 */

enum class ArenaMode(val raw: String) {
    /** The clue is written for you; you name the word. */
    GUESS("guess"),

    /** You give the clues; the AI does the naming. */
    CLUE("clue");

    companion object {
        fun from(raw: String?): ArenaMode = entries.firstOrNull { it.raw == raw } ?: GUESS
    }
}

enum class ArenaPeriod(val raw: String) {
    DAILY("daily"), WEEKLY("weekly"), MONTHLY("monthly")
}

data class ArenaWord(val w: String, val clues: List<String>)

/**
 * One round of one language.
 *
 * `words` is ordered, and every player is served it in exactly that order — that is what
 * makes a round comparable at all. The timings are frozen into the document at creation,
 * so a change to the live config cannot move a round that is already being played.
 */
data class ArenaRound(
    val id: String,                 // "{lang}_{index}"
    val lang: String,
    val index: Int,
    val startsAtMs: Long,
    val playSec: Int,
    val submitGraceSec: Int,
    val resultsAtSec: Int,
    /** A player may still enter while at least this many seconds of play remain. */
    val lateJoinMinRemainingSec: Int,
    val nextStartsAtMs: Long,
    /**
     * How many bots the round committed to when it was created. The lobby quotes it,
     * which is why it is decided up front instead of at scoring time.
     */
    val plannedBots: Int,
    /**
     * Filled in separately, once the round is about to start.
     *
     * The round document carries no words: they live at `arenaRounds/{id}/deal/words`,
     * which the rules keep shut until ten seconds before `startsAtMs`. Rounds are
     * published a few cycles ahead so the lobby has something to count down to, and with
     * the words on that document the next three rounds were readable before anybody
     * played them.
     */
    val words: List<ArenaWord> = emptyList(),
) {
    val endsAtMs: Long get() = startsAtMs + playSec * 1000L
    val closesAtMs: Long get() = endsAtMs + submitGraceSec * 1000L

    /**
     * When the client reads the finished board: after submissions close and the server's
     * scoring window has passed.
     */
    val boardFetchAtMs: Long get() = closesAtMs + ArenaStore.BOARD_FETCH_DELAY_MS

    companion object {
        fun from(id: String, data: Map<String, Any?>?): ArenaRound? {
            val d = data ?: return null
            val lang = d["lang"] as? String ?: return null
            val startsAtMs = (d["startsAtMs"] as? Number)?.toLong() ?: return null
            val nextStartsAtMs = (d["nextStartsAtMs"] as? Number)?.toLong() ?: return null
            return ArenaRound(
                id = id,
                lang = lang,
                index = (d["index"] as? Number)?.toInt() ?: 0,
                startsAtMs = startsAtMs,
                playSec = (d["playSec"] as? Number)?.toInt() ?: 60,
                submitGraceSec = (d["submitGraceSec"] as? Number)?.toInt() ?: 10,
                resultsAtSec = (d["resultsAtSec"] as? Number)?.toInt() ?: 15,
                lateJoinMinRemainingSec = (d["lateJoinMinRemainingSec"] as? Number)?.toInt()
                    ?: ArenaStore.DEFAULT_LATE_JOIN_MIN_REMAINING_SEC,
                nextStartsAtMs = nextStartsAtMs,
                plannedBots = (d["plannedBots"] as? Number)?.toInt() ?: 0,
            )
        }
    }
}

data class ArenaBoardEntry(
    val id: String,             // a uid, or a bot id — the board does not distinguish
    val name: String,
    val avatar: String,         // AliasAvatar catalogue id
    val country: String,
    val net: Int,
    val rank: Int,
    val isMe: Boolean,
) {
    companion object {
        fun from(data: Map<String, Any?>, me: String?): ArenaBoardEntry? {
            val id = data["id"] as? String ?: return null
            return ArenaBoardEntry(
                id = id,
                name = data["name"] as? String ?: "",
                avatar = data["avatar"] as? String ?: AliasAvatar.DEFAULT.id,
                country = data["country"] as? String ?: "",
                net = (data["net"] as? Number)?.toInt() ?: 0,
                rank = (data["rank"] as? Number)?.toInt() ?: 0,
                isMe = id == me,
            )
        }
    }
}

/**
 * Where the round is right now, as the screens need to see it.
 *
 * Derived from the round's own timings rather than from a server flag: the phase changes
 * every second and a document write per tick would be absurd.
 */
sealed interface ArenaPhase {
    data object Idle : ArenaPhase
    data class Lobby(val secondsToStart: Int) : ArenaPhase
    data class Playing(val secondsLeft: Int) : ArenaPhase
    data class Results(val secondsToNext: Int) : ArenaPhase
}

/** One row of a season board. */
data class ArenaStandingRow(
    val id: String,
    val name: String,
    val avatar: String,
    val country: String,
    val score: Int,
    val rank: Int,
    val isMe: Boolean,
)

/** Where one player stands on one board. */
data class ArenaStanding(
    val rank: Int?,
    val players: Int,
    val score: Int?,
    val delta: Int?,
) {
    /**
     * Top N per cent, rounded up and never zero — "top 0%" is not a thing, and somebody
     * first out of five hundred is top 1%, not top 0.2%.
     */
    val topPercent: Int?
        get() {
            val r = rank ?: return null
            if (players <= 0) return null
            return maxOf(1, Math.ceil(r.toDouble() / players * 100).toInt())
        }

    companion object {
        val EMPTY = ArenaStanding(rank = null, players = 0, score = null, delta = null)
    }
}
