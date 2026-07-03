package hr.azzi.socialgames.alias.online

import com.google.firebase.firestore.Exclude
import java.util.Date
import kotlin.random.Random

/**
 * Realtime multiplayer ("Online Play") data model — the Android port of the iOS
 * `OnlineModels.swift`. A single Firestore document per room (`onlineRooms/{code}`)
 * holds the entire shared state; every player observes it and always knows which UI
 * to show. The frozen word order lives in a sibling doc (`onlineRooms/{code}/deck/words`).
 *
 * IMPORTANT: the Firestore schema must match iOS byte-for-byte so cross-platform
 * rooms work — same field names, `status`/`roundPhase` stored as the SAME lowercase
 * strings, dates as Firestore Timestamps. Data classes below hold ONLY stored fields
 * (all `var` + defaults → Firestore POJO (de)serialization); every derived value is an
 * extension so Firestore never serializes it.
 */

// MARK: Status / phase (stored as lowercase strings, matching iOS rawValues)

object OnlineStatus {
    const val LOBBY = "lobby"
    const val PLAYING = "playing"
    const val FINISHED = "finished"
    const val CANCELLED = "cancelled"
}

object OnlineRoundPhase {
    const val READY = "ready"
    const val ACTIVE = "active"
    const val REVIEW = "review"
    const val STANDINGS = "standings"
}

/** What UI a given player should see inside a round (never stored). */
enum class OnlineRole { EXPLAINER, GUESSER, SPECTATOR }

// MARK: Player / team / word

data class OnlinePlayer(
    var id: String = "",        // Firebase uid
    var name: String = "",
    var agoraUid: Int = 0,      // legacy (unused since the Stream migration)
    var teamId: Int = 0,
    var joinedAt: Date = Date(),
) {
    @get:Exclude val initial: String get() = (name.firstOrNull()?.uppercase() ?: "?")
}

data class OnlineTeam(
    var id: Int = 0,
    var name: String = "",      // "Team A", "Team B", …
    var colorIndex: Int = 0,    // index into the confetti palette
    var score: Int = 0,
    var roundDelta: Int = 0,    // points pending from the round under review
) {
    @get:Exclude val displayScore: Int get() = maxOf(0, score)
}

/** One marked word from a round (editable in the Edit Answers sheet). */
data class OnlineMarkedWord(
    var word: String = "",
    var correct: Boolean = false,
)

// MARK: Room

data class OnlineRoom(
    // identity / config
    var id: String = "",        // == code (doc id)
    var code: String = "",
    var hostId: String = "",
    var hostName: String = "",
    var status: String = OnlineStatus.LOBBY,

    var deckId: String = "",    // BoardGame serialized id, e.g. "game_alias_classic"
    var deckName: String = "",  // localized display name
    var language: String = "",  // display, e.g. "English"
    var languageCode: String = "", // "Eng"
    var maxPlayers: Int = 4,    // 4 / 6 / 8
    var goalScore: Int = 40,    // 40 / 60 / 80
    var roundSeconds: Int = 60, // round duration

    var createdAt: Date = Date(),
    var wordCount: Int = 0,     // size of the frozen deck (≤ 1000)

    // membership
    var players: List<OnlinePlayer> = emptyList(),
    var teams: List<OnlineTeam> = emptyList(),
    /** Every uid that was ever a member — lets them rejoin within 4h without a code. */
    var pastMemberIds: List<String> = emptyList(),

    // round state (meaningful while status == playing)
    var roundPhase: String = OnlineRoundPhase.READY,
    var roundIndex: Int = 0,
    var currentTeamIndex: Int = 0,
    var currentExplainerId: String? = null,
    var currentGuesserId: String? = null,
    var roundStartAt: Date? = null,
    var wordCursor: Int = 0,    // index of the word currently in play
    var roundResults: List<OnlineMarkedWord> = emptyList(),

    /** Teams knocked out in a goal playoff (not tied for the lead at a cycle boundary). */
    var eliminatedTeamIds: List<Int> = emptyList(),
    /** Set once at game end so the winner is unambiguous even if a leader's score dips. */
    var winnerTeamId: Int? = null,
)

// MARK: - Derived (extensions — NOT serialized by Firestore)

/** A room can be (re)joined only within 4h of creation and while alive. */
const val ONLINE_FRESHNESS_WINDOW_MS = 4L * 60 * 60 * 1000

val OnlineRoom.isFresh: Boolean get() = System.currentTimeMillis() - createdAt.time < ONLINE_FRESHNESS_WINDOW_MS
val OnlineRoom.isAlive: Boolean get() = status == OnlineStatus.LOBBY || status == OnlineStatus.PLAYING
val OnlineRoom.isFull: Boolean get() = players.size >= maxPlayers
val OnlineRoom.teamCount: Int get() = maxPlayers / 2

fun OnlineRoom.player(uid: String?): OnlinePlayer? = players.firstOrNull { it.id == uid }
fun OnlineRoom.nameOf(uid: String?): String = player(uid)?.name ?: "—"
fun OnlineRoom.teamOf(uid: String?): OnlineTeam? {
    val tid = player(uid)?.teamId ?: return null
    return teams.firstOrNull { it.id == tid }
}
val OnlineRoom.currentTeam: OnlineTeam? get() = teams.firstOrNull { it.id == currentTeamIndex }
fun OnlineRoom.isHost(uid: String?): Boolean = uid != null && uid == hostId
fun OnlineRoom.hasMember(uid: String): Boolean = players.any { it.id == uid } || pastMemberIds.contains(uid)

/** The in-round role for a player (only meaningful while playing). */
fun OnlineRoom.role(uid: String?): OnlineRole = when {
    uid != null && uid == currentExplainerId -> OnlineRole.EXPLAINER
    uid != null && uid == currentGuesserId -> OnlineRole.GUESSER
    else -> OnlineRole.SPECTATOR
}

fun OnlineRoom.isEliminated(team: OnlineTeam): Boolean = eliminatedTeamIds.contains(team.id)
/** Teams still in contention (not eliminated) — the ones that take turns. */
val OnlineRoom.activeTeams: List<OnlineTeam> get() = teams.filter { !eliminatedTeamIds.contains(it.id) }

val OnlineRoom.sortedTeams: List<OnlineTeam>
    get() = teams.sortedWith(Comparator { a, b ->
        val ae = isEliminated(a); val be = isEliminated(b)
        if (ae != be) return@Comparator if (ae) 1 else -1   // eliminated ranks last
        if (a.displayScore != b.displayScore) b.displayScore - a.displayScore else a.id - b.id
    })

/**
 * A team's score for the standings view. While reviewing (before Finish Round
 * commits points), the active team's pending round delta is shown projected on top
 * of its stored score, so the ranking already reflects this round.
 */
fun OnlineRoom.standingScore(team: OnlineTeam, projected: Boolean): Int {
    if (!projected) return team.displayScore
    val pending = if (team.id == currentTeamIndex) roundDelta else 0
    return maxOf(0, team.score + pending)
}
fun OnlineRoom.standings(projected: Boolean): List<OnlineTeam> =
    teams.sortedWith(Comparator { a, b ->
        val ae = isEliminated(a); val be = isEliminated(b)
        if (ae != be) return@Comparator if (ae) 1 else -1
        val sa = standingScore(a, projected); val sb = standingScore(b, projected)
        if (sa != sb) sb - sa else a.id - b.id
    })

val OnlineRoom.leadingTeam: OnlineTeam? get() = sortedTeams.firstOrNull()

/** The decided winner (frozen at game end), falling back to the current leader. */
val OnlineRoom.winner: OnlineTeam?
    get() {
        val id = winnerTeamId
        if (id != null) teams.firstOrNull { it.id == id }?.let { return it }
        return leadingTeam
    }

val OnlineRoom.correctThisRound: Int get() = roundResults.count { it.correct }
val OnlineRoom.skippedThisRound: Int get() = roundResults.count { !it.correct }
/** Net points the active team earns from the current round (skips subtract). */
val OnlineRoom.roundDelta: Int get() = correctThisRound - skippedThisRound

/** Seconds left in the active round, derived from the server start stamp. */
fun OnlineRoom.secondsRemaining(now: Long = System.currentTimeMillis()): Int {
    val start = roundStartAt ?: return roundSeconds
    return maxOf(0, roundSeconds - ((now - start.time) / 1000).toInt())
}

// MARK: - Helpers

object OnlineRoomCode {
    /** 6-char A–Z/2–9 code (no ambiguous 0/O/1/I), e.g. "A7K2QX". */
    private val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    fun generate(): String = (0 until 6).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
}

object OnlineTeams {
    val letters = listOf("A", "B", "C", "D")
    /** Builds [count] teams; [nameFor] localizes the team name in the host's language. */
    fun make(count: Int, nameFor: (letter: String) -> String): List<OnlineTeam> =
        (0 until count).map { i ->
            OnlineTeam(id = i, name = nameFor(letters[i % letters.size]), colorIndex = i % 6)
        }
}
