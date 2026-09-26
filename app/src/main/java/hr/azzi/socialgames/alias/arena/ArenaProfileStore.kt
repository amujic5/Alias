package hr.azzi.socialgames.alias.arena

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import hr.azzi.socialgames.alias.ai.AuthService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * XP, level, streak — everything the server keeps about how a player is doing.
 *
 * Android port of the `ArenaSummary` half of iOS `ArenaProfile.swift`.
 */
data class ArenaSummary(
    /** Never decreases. Rewards turning up, which is a different question from being good. */
    val xp: Int = 0,
    val level: Int = 1,
    val roundsPlayed: Int = 0,
    /** Best `net` — guessed minus skipped — ever recorded. */
    val bestNet: Int = 0,
    /** Consecutive days with at least one round. */
    val streakDays: Int = 0,
) {
    /** Bounds taken from the level the *server* assigned, not one recomputed here. */
    val xpIntoLevel: Int get() = maxOf(0, xp - levelFloor(level))
    val xpForLevel: Int get() = maxOf(1, levelFloor(level + 1) - levelFloor(level))
    val levelProgress: Float get() = minOf(1f, xpIntoLevel.toFloat() / xpForLevel.toFloat())

    companion object {
        val EMPTY = ArenaSummary()

        /**
         * XP at which [level] begins.
         *
         * Mirrors `levelForXp` on the server, which is `floor((xp / 1000) ^ (1 / 1.4)) + 1`
         * — so the boundaries are `1000 · (level − 1) ^ 1.4` and they widen as they go:
         * 1 000, 2 639, 4 656, 6 964, and 3 445 XP to cross level ten.
         *
         * Duplicated rather than sent down, because it is a formula rather than a value:
         * the server would have to publish two numbers per player per read to say the
         * same thing. If the curve is ever retuned, all three clients move together.
         */
        fun levelFloor(level: Int): Int =
            if (level <= 1) 0 else ((level - 1).toDouble().pow(1.4) * 1000).roundToInt()

        fun from(data: Map<String, Any?>?): ArenaSummary {
            val d = data ?: return EMPTY
            return ArenaSummary(
                xp = (d["xp"] as? Number)?.toInt() ?: 0,
                level = (d["level"] as? Number)?.toInt() ?: 1,
                roundsPlayed = (d["roundsPlayed"] as? Number)?.toInt() ?: 0,
                bestNet = (d["bestNet"] as? Number)?.toInt() ?: 0,
                streakDays = (d["streakDays"] as? Number)?.toInt() ?: 0,
            )
        }
    }
}

/** A page of a season board, and where to continue from. */
data class ArenaStandingsPage(
    val rows: List<ArenaStandingRow>,
    val next: DocumentSnapshot?,
)

/**
 * The player's own side of the Arena: who they are, and where they stand.
 *
 * Android port of iOS `ArenaProfileStore.swift`. Split across three documents, because
 * they have three different owners:
 *
 * * `users/{uid}` — name, country, avatar. The player writes these.
 * * `users/{uid}/arena/summary` — XP, level, rounds, best, streak. Function-owned.
 * * `leaderboards/{period}/…/{uid}` — the standing itself, also function-owned.
 *
 * The ranks are read rather than computed. "Where am I on the daily board" is a count of
 * everyone above you, and asking a phone to do that would mean downloading the board; the
 * entry document carries `score`, so the answer is one aggregation query.
 */
class ArenaProfileStore(app: Application) : AndroidViewModel(app) {

    private val db = FirebaseFirestore.getInstance()
    private val appCtx get() = getApplication<Application>()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _avatar = MutableStateFlow<String?>(null)
    val avatar: StateFlow<String?> = _avatar.asStateFlow()

    private val _country = MutableStateFlow<String?>(null)
    val country: StateFlow<String?> = _country.asStateFlow()

    private val _summary = MutableStateFlow(ArenaSummary.EMPTY)
    val summary: StateFlow<ArenaSummary> = _summary.asStateFlow()

    /**
     * True while the three standings are being read for a mode not seen yet. Only then:
     * switching back to a mode already on this screen is instant, and a spinner that
     * flashed for it would suggest work that is not happening.
     */
    private val _loadingRanks = MutableStateFlow(false)
    val loadingRanks: StateFlow<Boolean> = _loadingRanks.asStateFlow()

    private val _daily = MutableStateFlow(ArenaStanding.EMPTY)
    val daily: StateFlow<ArenaStanding> = _daily.asStateFlow()
    private val _weekly = MutableStateFlow(ArenaStanding.EMPTY)
    val weekly: StateFlow<ArenaStanding> = _weekly.asStateFlow()
    private val _monthly = MutableStateFlow(ArenaStanding.EMPTY)
    val monthly: StateFlow<ArenaStanding> = _monthly.asStateFlow()

    /**
     * Which of the two boards is being *looked at* — not which one is being played.
     *
     * The server has always kept them apart: a board id is `{period}_{mode}`, so guessing
     * and explaining have had separate standings since the first round. Remembered on its
     * own, so a player who came to compare explainers is still looking at explainers when
     * they come back.
     */
    private val _mode = MutableStateFlow(savedBoardMode())
    val mode: StateFlow<ArenaMode> = _mode.asStateFlow()

    private var boundUid: String? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null

    /** Whether the bound account's profile has been read yet. */
    private var loaded = false

    /** The [scoredRounds] this store's numbers already include. */
    private var seenScoredRounds = scoredRounds

    /**
     * The three standings of one mode, kept for as long as the screen is open.
     *
     * Cleared by [refresh] — by a read that has just replaced these numbers, not by the
     * screen opening — so flipping between the two modes costs nothing, on this visit or
     * the next one.
     */
    private val cachedStandings = mutableMapOf<ArenaMode, Triple<ArenaStanding, ArenaStanding, ArenaStanding>>()

    // MARK: Binding

    /**
     * Follows whoever is signed in.
     *
     * Driven by the auth listener rather than read once, because the profile is usually
     * started *before* there is a session: a first-time player is signed in anonymously
     * by `ArenaStore.enter()` a moment later, and a one-shot read would bind to nothing
     * and stay empty.
     */
    fun start() {
        if (authListener != null) return
        authListener = FirebaseAuth.AuthStateListener { auth ->
            bind(auth.currentUser?.uid)
        }.also { FirebaseAuth.getInstance().addAuthStateListener(it) }
    }

    override fun onCleared() {
        super.onCleared()
        authListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
    }

    private fun bind(uid: String?) {
        if (uid == null) {
            reset()
            return
        }
        if (uid == boundUid) return
        boundUid = uid
        loaded = false
        // Before the read, so nothing derived from the last account survives into this
        // one — see `PlayerLevel.bind`.
        PlayerLevel.bind(uid)

        // Identity only, not the standings. This runs at launch for everybody, and what
        // it owes the rest of the app is the level, the name and the avatar; the six
        // aggregation queries behind the standings are wanted only by the screens that
        // show them.
        viewModelScope.launch { syncIdentity() }
    }

    private fun reset() {
        boundUid = null
        loaded = false
        PlayerLevel.unbind()
        _name.value = ""
        _avatar.value = null
        _country.value = null
        _summary.value = ArenaSummary.EMPTY
        _daily.value = ArenaStanding.EMPTY
        _weekly.value = ArenaStanding.EMPTY
        _monthly.value = ArenaStanding.EMPTY
        cachedStandings.clear()
    }

    // MARK: Reading

    /**
     * Who you are and what level you are: the profile document and the summary.
     *
     * Two reads, no aggregation. This is what runs at launch and on every foreground,
     * because it is what the rest of the app needs to draw itself — the level for the
     * padlocks, and the name and avatar for every screen that shows a face.
     *
     * The avatar in particular is why this reads the profile document and not the summary
     * alone. The picture is a bundled drawable, so once the id is here it draws instantly;
     * without the id every screen falls back to the default and the player is shown
     * somebody else's face.
     */
    suspend fun syncIdentity(): Boolean {
        val uid = boundUid ?: return false
        val user = db.collection("users").document(uid)

        val profileDoc = runCatching { user.get().await() }.getOrNull()
        val summaryDoc = runCatching {
            user.collection("arena").document("summary").get().await()
        }.getOrNull()
        if (uid != boundUid) return false

        // A guest whose seed write never landed still gets their name, because it is
        // derived from the uid rather than stored.
        val storedName = profileDoc?.getString("name")
        _name.value = if (storedName.isNullOrEmpty()) ArenaProfile.guestName(uid) else storedName
        _avatar.value = profileDoc?.getString("avatar")
        _country.value = profileDoc?.getString("country")
        summaryDoc?.let { _summary.value = ArenaSummary.from(it.data) }

        // Mirrored for the screens that gate on level but have no session to ask with.
        PlayerLevel.store(_summary.value.level)
        return true
    }

    /** Re-reads everything, including the standings. After a round, and nowhere else. */
    suspend fun refresh() {
        seenScoredRounds = scoredRounds
        if (!syncIdentity()) return
        loaded = true
        cachedStandings.clear()
        showRanks()
    }

    /**
     * Reads the profile the first time somebody asks, and not again.
     *
     * Opening a screen is not news. Both events that can move these numbers refresh on
     * their own: the auth listener when an account appears, and the flow the moment a
     * board lands with this player on it.
     */
    suspend fun refreshIfNeeded() {
        if (loaded) return
        refresh()
    }

    /**
     * Re-reads if a round has been scored since this store last did.
     *
     * The Arena and the AI hub are two activities with a store each, and only the Arena's
     * is there when the board lands. The hub's sits in the back stack holding the XP from
     * before the round, and the profile opened from it would show that — so the hub calls
     * this when it comes back to the front.
     */
    suspend fun catchUpIfScored() {
        if (seenScoredRounds == scoredRounds) return
        if (loaded) refresh() else { seenScoredRounds = scoredRounds; syncIdentity() }
    }

    fun setMode(next: ArenaMode) {
        if (next == _mode.value) return
        _mode.value = next
        appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_BOARD_MODE, next.raw).apply()
        viewModelScope.launch { showRanks() }
    }

    /** Shows the current mode's standings: from memory if they are there, otherwise read. */
    private suspend fun showRanks() {
        cachedStandings[_mode.value]?.let { (d, w, m) ->
            _daily.value = d; _weekly.value = w; _monthly.value = m
            return
        }
        _loadingRanks.value = true
        try { refreshRanks() } finally { _loadingRanks.value = false }
    }

    private suspend fun refreshRanks() = viewModelScope.launch {
        val uid = boundUid ?: return@launch
        val requested = _mode.value
        val results = listOf(
            async { standing(uid, "daily", boardId(ArenaPeriod.DAILY, requested)) },
            async { standing(uid, "weekly", boardId(ArenaPeriod.WEEKLY, requested)) },
            async { standing(uid, "monthly", boardId(ArenaPeriod.MONTHLY, requested)) },
        ).awaitAll()

        // The switcher may have moved again while these were in flight.
        if (_mode.value != requested) return@launch

        _daily.value = withDelta(results[0], "daily")
        _weekly.value = withDelta(results[1], "weekly")
        _monthly.value = withDelta(results[2], "monthly")
        cachedStandings[requested] = Triple(_daily.value, _weekly.value, _monthly.value)
    }.join()

    /**
     * One place on a board, and how crowded that board is — both worked out server-side.
     *
     * `count()` is an aggregation query: Firestore returns the number, not the documents,
     * so a player ranked eight hundredth costs the same as one ranked second.
     */
    private suspend fun standing(uid: String, period: String, board: String): ArenaStanding {
        val collection = db.collection("leaderboards").document(period).collection(board)

        val players = runCatching {
            collection.count().get(AggregateSource.SERVER).await().count.toInt()
        }.getOrDefault(0)

        val mine = runCatching { collection.document(uid).get().await() }.getOrNull()
        val score = (mine?.get("score") as? Number)?.toInt()
            ?: return ArenaStanding(rank = null, players = players, score = null, delta = null)

        val above = runCatching {
            collection.whereGreaterThan("score", score)
                .count().get(AggregateSource.SERVER).await().count.toInt()
        }.getOrNull()

        return ArenaStanding(rank = above?.plus(1), players = players, score = score, delta = null)
    }

    /**
     * Fills in "↑ 6 places".
     *
     * There is no server-side history to compare against — recording every player's rank
     * after every round would mean an aggregation query per player per round, which is a
     * large bill for a decoration. So the comparison is against what this device last saw,
     * and it only moves when the player has actually played since: without that check the
     * pill would flicker as other people's rounds pushed the number around, and claim
     * credit for it.
     */
    private fun withDelta(s: ArenaStanding, period: String): ArenaStanding {
        val rank = s.rank ?: return s
        val rounds = _summary.value.roundsPlayed
        if (rounds == 0) return s

        val prefs = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "arena_standing_${period}_${_mode.value.raw}"
        val wasRank = prefs.getInt("${key}_rank", -1)
        val wasRounds = prefs.getInt("${key}_rounds", -1)
        val storedDelta = prefs.getInt("${key}_delta", 0)

        if (wasRank < 0 || wasRounds < 0) {
            prefs.edit().putInt("${key}_rank", rank).putInt("${key}_rounds", rounds)
                .putInt("${key}_delta", 0).apply()
            return s
        }
        // Same number of rounds means nothing this player did moved them.
        if (wasRounds == rounds) {
            return s.copy(delta = storedDelta.takeIf { it != 0 })
        }

        val delta = wasRank - rank      // positive: moved up the board
        prefs.edit().putInt("${key}_rank", rank).putInt("${key}_rounds", rounds)
            .putInt("${key}_delta", delta).apply()
        return s.copy(delta = delta.takeIf { it != 0 })
    }

    // MARK: Standings

    /**
     * Where the player themself stands, as a row.
     *
     * Read separately from the top of the board, because somebody outside it still has a
     * place and still wants to see it — the screen pins this row rather than leaving them
     * absent from their own leaderboard.
     */
    suspend fun myStanding(period: ArenaPeriod): ArenaStandingRow? {
        val uid = boundUid ?: return null
        val collection = db.collection("leaderboards").document(period.raw)
            .collection(boardId(period, _mode.value))

        val mine = runCatching { collection.document(uid).get().await() }.getOrNull() ?: return null
        val score = (mine.get("score") as? Number)?.toInt() ?: return null
        val above = runCatching {
            collection.whereGreaterThan("score", score)
                .count().get(AggregateSource.SERVER).await().count.toInt()
        }.getOrNull() ?: return null

        return ArenaStandingRow(
            id = uid,
            name = mine.getString("name") ?: "",
            avatar = mine.getString("avatar") ?: AliasAvatar.DEFAULT.id,
            country = mine.getString("country") ?: "",
            score = score,
            rank = above + 1,
            isMe = true,
        )
    }

    /**
     * The top of a board, in order.
     *
     * Ordered server-side on `score`, and the rows are numbered the way the rank query
     * counts, so the list and the number on the profile cannot disagree.
     *
     * One `orderBy` and no more. Adding the id explicitly to break ties reads as the
     * careful thing to do and is unusable here: ordering a field descending and the id
     * ascending needs a composite index, and these collections are named after the day or
     * week they cover — a new index every morning. Firestore already orders by the id
     * implicitly, which is what `startAfter(document)` uses.
     */
    suspend fun standings(
        period: ArenaPeriod,
        limit: Long = 20,
        after: DocumentSnapshot? = null,
        rankOffset: Int = 0,
        previous: ArenaStandingRow? = null,
    ): ArenaStandingsPage {
        val me = boundUid
        var query: Query = db.collection("leaderboards").document(period.raw)
            .collection(boardId(period, _mode.value))
            .orderBy("score", Query.Direction.DESCENDING)
        if (after != null) query = query.startAfter(after)

        val snap = runCatching { query.limit(limit).get().await() }.getOrNull()
            ?: return ArenaStandingsPage(emptyList(), null)

        // Competition rank, not position in the list.
        //
        // These are the two different questions a leaderboard can answer. The profile
        // counts how many players are strictly ahead of you and adds one, so eleven people
        // tied on the same score are all 24th. Numbering rows 1, 2, 3 down the page would
        // read the same eleven as 24 through 34, and tell the last of them they were 35th
        // on a screen that had just said 24th.
        var lastScore = previous?.score
        var lastRank = previous?.rank
        val rows = snap.documents.mapIndexed { index, doc ->
            val score = (doc.get("score") as? Number)?.toInt() ?: 0
            val rank = if (score == lastScore && lastRank != null) lastRank!! else rankOffset + index + 1
            lastScore = score
            lastRank = rank
            ArenaStandingRow(
                id = doc.id,
                name = doc.getString("name") ?: "",
                avatar = doc.getString("avatar") ?: AliasAvatar.DEFAULT.id,
                country = doc.getString("country") ?: "",
                score = score,
                rank = rank,
                isMe = doc.id == me,
            )
        }

        // A short page is the end of the board. Asking again would cost a read to learn
        // nothing.
        val next = if (snap.documents.size.toLong() == limit) snap.documents.lastOrNull() else null
        return ArenaStandingsPage(rows, next)
    }

    /** Writes the chosen avatar, showing it immediately; a failed write puts the old one back. */
    suspend fun setAvatar(id: String) {
        val uid = boundUid ?: return
        val previous = _avatar.value
        _avatar.value = id
        runCatching {
            db.collection("users").document(uid)
                .set(mapOf("avatar" to id), com.google.firebase.firestore.SetOptions.merge()).await()
        }.onFailure { _avatar.value = previous }
    }

    companion object {
        private const val PREFS = "alias_arena"
        private const val KEY_BOARD_MODE = "arena_board_mode"

        /** The app's profile, started at launch and alive for as long as the app is. */
        @Volatile
        var shared: ArenaProfileStore? = null
            private set

        fun install(store: ArenaProfileStore) { shared = store }

        /** Rounds scored with this player on the board, in this process. See [catchUpIfScored]. */
        @Volatile
        private var scoredRounds = 0

        /** The board has landed with this player on it: their XP and ranks have moved. */
        fun roundScored() { scoredRounds++ }

        /**
         * The ids the scoring function writes to. Duplicated here rather than fetched,
         * because they are derived from the date: a client that computed them differently
         * would read an empty collection and show an empty leaderboard rather than fail.
         */
        fun boardId(period: ArenaPeriod, mode: ArenaMode): String = when (period) {
            ArenaPeriod.DAILY -> "${utcDay()}_${mode.raw}"
            ArenaPeriod.WEEKLY -> "${utcWeek()}_${mode.raw}"
            ArenaPeriod.MONTHLY -> "${utcMonth()}_${mode.raw}"
        }

        /** UTC, so a board does not roll over at a different moment for each country. */
        private fun utcDay(date: Date = Date()): String = utcFormat("yyyyMMdd", date)

        private fun utcMonth(date: Date = Date()): String = utcFormat("yyyyMM", date)

        private fun utcFormat(pattern: String, date: Date): String =
            SimpleDateFormat(pattern, Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(date)

        /** `2026-W38`, as `weekKey()` writes it. */
        private fun utcWeek(date: Date = Date()): String {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
                time = date
                firstDayOfWeek = Calendar.MONDAY      // ISO-8601, like the server
                minimalDaysInFirstWeek = 4
            }
            val week = cal.get(Calendar.WEEK_OF_YEAR)
            // Late December can belong to week 1 of the next year, and early January to
            // week 52 of the last — the year has to follow the week, not the date.
            val month = cal.get(Calendar.MONTH)
            var year = cal.get(Calendar.YEAR)
            if (month == Calendar.DECEMBER && week == 1) year += 1
            if (month == Calendar.JANUARY && week >= 52) year -= 1
            return String.format(Locale.US, "%04d-W%02d", year, week)
        }

        private fun savedBoardMode(): ArenaMode = ArenaMode.GUESS
    }
}

/** Names and countries for players who have not chosen one. */
object ArenaProfile {

    /**
     * A stable name for a guest, derived from the uid: "Guest4821".
     *
     * So a guest whose seed write never landed still has something to be called, and is
     * called the same thing on every screen and every board. The same djb2 hash as iOS
     * `ArenaProfile.guestName(for:)`, so one uid is one name on both platforms.
     */
    fun guestName(uid: String): String {
        var hash = 5381uL
        for (byte in uid.toByteArray(Charsets.UTF_8)) hash = hash * 33uL + byte.toUByte().toULong()
        return String.format(Locale.US, "Guest%04d", (hash % 10_000uL).toLong())
    }

    /** Whether a name was generated for a guest rather than chosen by its owner. */
    fun isGuestName(name: String): Boolean = GUEST_NAME.matches(name)

    private val GUEST_NAME = Regex("^Guest[0-9]{4}$")
}
