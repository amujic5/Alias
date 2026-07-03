package hr.azzi.socialgames.alias.online

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/** Error kinds surfaced to the UI (mapped to localized strings in the ViewModel). */
enum class OnlineError { NOT_FOUND, EXPIRED, CLOSED, FULL, NOT_SIGNED_IN, CODE_TAKEN }
class OnlineException(val error: OnlineError) : Exception(error.name)

/**
 * Firestore-backed room store — Android port of iOS `OnlineRoomRepository.swift`.
 * The whole shared game state is ONE document (`onlineRooms/{code}`) that every client
 * observes; a sibling `deck/words` doc holds the frozen word order. All transitions run
 * inside a single-doc transaction, so there are no lost-update races between the host,
 * the explainer and joining players.
 */
class OnlineRoomRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val rooms get() = db.collection("onlineRooms")
    private fun roomRef(code: String): DocumentReference = rooms.document(code)
    private fun wordsRef(code: String): DocumentReference = roomRef(code).collection("deck").document("words")

    // MARK: Create / join

    /** Atomic create-if-absent: never overwrites a still-live room on a code collision. */
    suspend fun createRoom(room: OnlineRoom, words: List<String>) {
        val rRef = roomRef(room.code)
        val wRef = wordsRef(room.code)
        val taken = db.runTransaction { txn ->
            val snap = txn.get(rRef)
            if (snap.exists()) {
                // Only a still-live room (alive + <4h) blocks the code; finished/cancelled/
                // expired rooms can be overwritten (nobody can rejoin them). Undecodable → live.
                val existing = runCatching { snap.toObject(OnlineRoom::class.java) }.getOrNull()
                val live = existing?.let { it.isAlive && it.isFresh } ?: true
                if (live) return@runTransaction true
            }
            txn.set(rRef, room)
            txn.set(wRef, mapOf("words" to words))
            false
        }.await()
        if (taken == true) throw OnlineException(OnlineError.CODE_TAKEN)
    }

    /** Joins (or rejoins) [code]. Enforces freshness, liveness and capacity. */
    suspend fun joinRoom(code: String, uid: String, name: String, agoraUid: Long): OnlineRoom =
        mutate(code) { room ->
            if (!room.isAlive) throw OnlineException(OnlineError.CLOSED)
            if (!room.isFresh) throw OnlineException(OnlineError.EXPIRED)
            val rejoining = room.players.any { it.id == uid } || room.pastMemberIds.contains(uid)
            if (!rejoining && room.isFull) throw OnlineException(OnlineError.FULL)

            val existing = room.players.firstOrNull { it.id == uid }
            if (existing != null) {
                existing.name = name          // refresh name, keep team
                existing.agoraUid = agoraUid
            } else {
                val team = openTeamId(room)
                room.players = room.players + OnlinePlayer(id = uid, name = name, agoraUid = agoraUid, teamId = team)
            }
            if (!room.pastMemberIds.contains(uid)) room.pastMemberIds = room.pastMemberIds + uid
        }

    suspend fun canRejoin(code: String, uid: String): Boolean {
        val room = runCatching { roomRef(code).get().await().toObject(OnlineRoom::class.java) }.getOrNull() ?: return false
        return room.isAlive && room.isFresh && room.hasMember(uid)
    }

    /** Rooms [uid] was a member of that are still joinable (alive + <4h), newest first. */
    suspend fun myRooms(uid: String): List<OnlineRoom> {
        val cutoff = Timestamp(Date(System.currentTimeMillis() - ONLINE_FRESHNESS_WINDOW_MS))
        val snap = runCatching {
            rooms.whereArrayContains("pastMemberIds", uid)
                .whereGreaterThan("createdAt", cutoff)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
        }.getOrNull() ?: return emptyList()
        return snap.documents.mapNotNull { runCatching { it.toObject(OnlineRoom::class.java) }.getOrNull() }
            .filter { it.isAlive }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun fetchWords(code: String): List<String> {
        val snap = wordsRef(code).get().await()
        return (snap.get("words") as? List<String>) ?: emptyList()
    }

    /** Live updates for the room doc as a cold Flow (null when it can't be decoded). */
    fun observe(code: String): Flow<OnlineRoom?> = callbackFlow {
        val reg = roomRef(code).addSnapshotListener { snap, _ ->
            trySend(runCatching { snap?.toObject(OnlineRoom::class.java) }.getOrNull())
        }
        awaitClose { reg.remove() }
    }

    // MARK: Mutations

    suspend fun assignTeam(code: String, uid: String, teamId: Int) = mutateUnit(code) { room ->
        if (room.status != OnlineStatus.LOBBY || teamId < 0 || teamId >= room.teamCount) return@mutateUnit
        val pi = room.players.indexOfFirst { it.id == uid }
        if (pi < 0) return@mutateUnit
        val from = room.players[pi].teamId
        if (from == teamId) return@mutateUnit
        // If the target team is already full (2), swap the longest-seated member back.
        val targetMembers = room.players.indices
            .filter { room.players[it].teamId == teamId && room.players[it].id != uid }
            .sortedBy { room.players[it].joinedAt.time }
        if (targetMembers.size >= 2) room.players[targetMembers.first()].teamId = from
        room.players[pi].teamId = teamId
    }

    suspend fun shuffleTeams(code: String) = mutateUnit(code) { room ->
        if (room.status != OnlineStatus.LOBBY) return@mutateUnit
        room.players.map { it.id }.shuffled().forEachIndexed { i, uid ->
            room.players.firstOrNull { it.id == uid }?.let { it.teamId = i % room.teamCount }
        }
    }

    suspend fun startGame(code: String) = mutateUnit(code) { room ->
        if (room.status != OnlineStatus.LOBBY || room.players.size < 2) return@mutateUnit
        room.status = OnlineStatus.PLAYING
        room.roundIndex = 0
        room.currentTeamIndex = 0
        assignRoles(room)
        room.roundPhase = OnlineRoundPhase.READY
        room.roundResults = emptyList()
        room.roundStartAt = null
    }

    suspend fun startRound(code: String) = mutateUnit(code) { room ->
        if (room.status != OnlineStatus.PLAYING || room.roundPhase != OnlineRoundPhase.READY) return@mutateUnit
        room.roundPhase = OnlineRoundPhase.ACTIVE
        room.roundStartAt = Date()
        room.roundResults = emptyList()
    }

    suspend fun setRoundResults(code: String, results: List<OnlineMarkedWord>, wordCursor: Int) = mutateUnit(code) { room ->
        if (room.status != OnlineStatus.PLAYING) return@mutateUnit
        room.roundResults = results
        room.wordCursor = wordCursor
    }

    suspend fun endRound(code: String) = mutateUnit(code) { room ->
        if (room.status != OnlineStatus.PLAYING || room.roundPhase != OnlineRoundPhase.ACTIVE) return@mutateUnit
        room.roundPhase = OnlineRoundPhase.REVIEW
        room.roundStartAt = null
    }

    suspend fun saveAnswers(code: String) = mutateUnit(code) { room ->
        if (room.status != OnlineStatus.PLAYING || room.roundPhase != OnlineRoundPhase.REVIEW) return@mutateUnit
        room.roundPhase = OnlineRoundPhase.STANDINGS
    }

    suspend fun editAnswers(code: String) = mutateUnit(code) { room ->
        if (room.status != OnlineStatus.PLAYING || room.roundPhase != OnlineRoundPhase.STANDINGS) return@mutateUnit
        room.roundPhase = OnlineRoundPhase.REVIEW
    }

    suspend fun finishRound(code: String) = mutateUnit(code) { room ->
        // Points are committed here (not on Save), so re-editing from standings never
        // double-counts — the delta only lands once, on Finish Round.
        if (room.status != OnlineStatus.PLAYING || room.roundPhase != OnlineRoundPhase.STANDINGS) return@mutateUnit
        val delta = room.roundDelta
        room.teams.firstOrNull { it.id == room.currentTeamIndex }?.let { it.score = maxOf(0, it.score + delta) }
        room.roundResults = emptyList()
        room.roundStartAt = null

        // Fair finish: decide only at the end of a FULL cycle so every team in contention
        // has played an equal number of turns. currentTeamIndex is the team that just
        // played, so the cycle is complete when it's the last active team in the rotation.
        val active = room.activeTeams
        val reachedGoal = active.any { it.displayScore >= room.goalScore }
        val cycleComplete = room.currentTeamIndex == (active.maxOfOrNull { it.id } ?: room.currentTeamIndex)
        if (reachedGoal && cycleComplete) {
            val top = active.maxOfOrNull { it.displayScore } ?: 0
            val leaders = active.filter { it.displayScore == top }
            if (leaders.size == 1) {                 // clear winner → game over
                room.winnerTeamId = leaders[0].id
                room.status = OnlineStatus.FINISHED
                return@mutateUnit
            }
            // Tie for the top → everyone not tied for the lead is eliminated; the tied
            // leaders replay a full cycle until one pulls ahead.
            val leaderIds = leaders.map { it.id }.toSet()
            active.filter { it.id !in leaderIds }.forEach { room.eliminatedTeamIds = room.eliminatedTeamIds + it.id }
        }
        room.roundIndex += 1
        room.currentTeamIndex = nextActiveTeamId(room.currentTeamIndex, room)
        assignRoles(room)
        room.roundPhase = OnlineRoundPhase.READY
    }

    suspend fun cancel(code: String) = mutateUnit(code) { it.status = OnlineStatus.CANCELLED }

    suspend fun leave(code: String, uid: String) = mutateUnit(code) { room ->
        // Only prune while still in the lobby; mid-game seats are kept so a dropped
        // player can reconnect into the same team/role.
        if (room.status != OnlineStatus.LOBBY) return@mutateUnit
        room.players = room.players.filter { it.id != uid }
    }

    // MARK: - Transaction helper

    private suspend fun mutateUnit(code: String, body: (OnlineRoom) -> Unit) {
        mutate(code, body)
    }

    private suspend fun mutate(code: String, body: (OnlineRoom) -> Unit): OnlineRoom {
        val ref = roomRef(code)
        return db.runTransaction { txn ->
            val snap = txn.get(ref)
            val room = snap.toObject(OnlineRoom::class.java) ?: throw OnlineException(OnlineError.NOT_FOUND)
            body(room)
            txn.set(ref, room)
            room
        }.await()
    }

    // MARK: - Pure helpers

    /** Next team id (cyclically) still in contention — skips eliminated teams / empty slots. */
    private fun nextActiveTeamId(current: Int, room: OnlineRoom): Int {
        val count = room.teamCount
        if (count <= 0) return current
        for (step in 1..count) {
            val candidate = (current + step) % count
            if (candidate !in room.eliminatedTeamIds && room.teams.any { it.id == candidate }) return candidate
        }
        return current
    }

    /** The team id a new player should fill (fewest members, then lowest id). */
    private fun openTeamId(room: OnlineRoom): Int {
        val counts = IntArray(room.teamCount)
        room.players.forEach { if (it.teamId in counts.indices) counts[it.teamId]++ }
        var best = 0
        for (i in counts.indices) if (counts[i] < counts[best]) best = i
        return best
    }

    /** Sets explainer/guesser for currentTeamIndex, alternating which partner explains each cycle. */
    private fun assignRoles(room: OnlineRoom) {
        val members = room.players.filter { it.teamId == room.currentTeamIndex }.sortedBy { it.joinedAt.time }
        if (members.isEmpty()) { room.currentExplainerId = null; room.currentGuesserId = null; return }
        val cycle = if (room.teamCount > 0) (room.roundIndex / room.teamCount) % 2 else 0
        val explainer = members[cycle % members.size]
        val guesser = members.firstOrNull { it.id != explainer.id }
        room.currentExplainerId = explainer.id
        // Fall back to the explainer when their team has no partner (under-filled team).
        room.currentGuesserId = guesser?.id ?: explainer.id
    }
}
