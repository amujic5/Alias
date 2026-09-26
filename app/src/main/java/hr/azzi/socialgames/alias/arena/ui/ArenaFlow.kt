package hr.azzi.socialgames.alias.arena.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import hr.azzi.socialgames.alias.ai.AIDeck
import hr.azzi.socialgames.alias.ai.AILanguage
import hr.azzi.socialgames.alias.ai.AIMode
import hr.azzi.socialgames.alias.ai.AIPracticeConfig
import hr.azzi.socialgames.alias.ai.AIPracticeResult
import hr.azzi.socialgames.alias.arena.ArenaAnalytics
import hr.azzi.socialgames.alias.arena.ArenaMode
import hr.azzi.socialgames.alias.arena.ArenaPhase
import hr.azzi.socialgames.alias.arena.ArenaProfileStore
import hr.azzi.socialgames.alias.arena.ArenaRound
import hr.azzi.socialgames.alias.arena.ArenaStore
import hr.azzi.socialgames.alias.arena.LevelRewards
import hr.azzi.socialgames.alias.arena.PlayerLevel
import hr.azzi.socialgames.alias.ui.screens.ai.AiExplainScreen
import hr.azzi.socialgames.alias.ui.screens.ai.AiPlayScreen
import kotlinx.coroutines.launch

/**
 * Plays one Arena round on the engine Practice already uses.
 *
 * Android port of iOS `ArenaRoundHost.swift`.
 *
 * The round is handed the words the server dealt and a deadline measured against the
 * server's clock, so what runs is the round that is already running — not a fresh sixty
 * seconds starting whenever this composable happened to appear.
 */
@Composable
private fun ArenaRoundHost(
    round: ArenaRound,
    mode: ArenaMode,
    deadlineMs: Long?,
    seconds: Int,
    onFinish: (guessed: List<Pair<String, Int>>, skipped: List<String>) -> Unit,
    onAbandon: () -> Unit,
) {
    val words = remember(round.id) { round.words.map { it.w } }

    /**
     * The clues the server dealt with this round, keyed the way the engine reads them.
     *
     * Not from a bundled deck: the server draws its thirty words from the merged pool of
     * every deck in the language, so no single deck has clues for all of them.
     */
    val clues = remember(round.id) {
        round.words.filter { it.clues.isNotEmpty() }.associate { it.w.lowercase() to it.clues }
    }

    val language = remember(round.lang) {
        AILanguage.entries.firstOrNull { it.fileCode == round.lang } ?: AILanguage.EN
    }

    // A deck the catalogue does not have to know about: the Arena's pool is every deck of
    // the language at once, and the engine only reads `deck.id` to look words up — which
    // it never does here, because the words are handed in.
    val config = remember(round.id, mode, seconds) {
        AIPracticeConfig(
            deck = AIDeck(id = "arena", nameRes = "arena_entry_title", imageRes = "", languages = listOf(language)),
            language = language,
            totalSeconds = seconds,
            mode = if (mode == ArenaMode.GUESS) AIMode.AI_EXPLAINS else AIMode.YOU_EXPLAIN,
        )
    }

    /**
     * Turns a finished round into a submission.
     *
     * The timings are the weak part and deliberately visible rather than hidden: the
     * engine records *which* words were guessed but not *when*, so every guessed word is
     * stamped with the full round length. The server uses time only to break ties between
     * players on the same net score, so the effect is a missing tiebreak, not a wrong one.
     */
    fun report(result: AIPracticeResult) {
        val ms = seconds * 1000
        // Each word once, and never in both lists. A player who gets through all thirty
        // is dealt them again — the engine loops rather than ending early — so the same
        // word can come back guessed twice, or skipped and then guessed. The server
        // refuses a submission that reports a word twice, and refuses the whole of it.
        val seen = mutableSetOf<String>()
        val guessed = result.correctWords.filter { seen.add(it) }
        val skipped = result.skippedWords.filter { seen.add(it) }
        onFinish(guessed.map { it to ms }, skipped)
    }

    if (mode == ArenaMode.GUESS) {
        AiExplainScreen(
            config = config,
            fixedWords = words,
            onChallengeFinish = { result, _ -> report(result) },
            clues = clues,
            deadlineMs = deadlineMs,
            // The engine already preloads when the round opens and presents just after
            // the whistle — exactly where an Arena round wants it. What it was doing was
            // reporting those impressions against the AI Practice unit.
            ads = hr.azzi.socialgames.alias.ai.AdInterstitial.arena,
            onClose = onAbandon,
        )
    } else {
        AiPlayScreen(
            config = config,
            fixedWords = words,
            onChallengeFinish = { result, _ -> report(result) },
            deadlineMs = deadlineMs,
            ads = hr.azzi.socialgames.alias.ai.AdInterstitial.arena,
            onClose = onAbandon,
        )
    }
}

/**
 * The Arena's root: wait, play, read the board, wait again.
 *
 * Android port of iOS `ArenaFlowView.swift`.
 *
 * The server does not tell a client what screen to show. It publishes a round — when it
 * starts, how long it runs, when the next one starts — and every client works out for
 * itself where in that cycle it is. So this has no navigation of its own: it watches the
 * phase and swaps the screen under the player.
 */
@Composable
fun ArenaFlowScreen(
    store: ArenaStore,
    profile: ArenaProfileStore,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val phase by store.phase.collectAsState()
    val round by store.round.collectAsState()
    val mode by store.mode.collectAsState()
    val lang by store.lang.collectAsState()
    val board by store.board.collectAsState()
    val boardLoaded by store.boardLoaded.collectAsState()
    val secondsToResults by store.secondsToResults.collectAsState()
    val playersInLobby by store.playersInLobby.collectAsState()
    val canJoinNow by store.canJoinNow.collectAsState()
    val forceJoinSeconds by store.forceJoinSeconds.collectAsState()
    val playDeadlineMs by store.playDeadlineMs.collectAsState()
    val summary by profile.summary.collectAsState()

    /**
     * The round currently being played locally. Keyed by id rather than by a boolean so a
     * round that ends while the player is still on the play screen cannot be confused
     * with the next one starting.
     */
    var playingRoundId by remember { mutableStateOf<String?>(null) }
    var playSeconds by remember { mutableIntStateOfCompat(0) }

    /**
     * The round the player is finished with, whether they played it out or walked away.
     * Without it the flow deals them straight back in: the phase changes four times a
     * second, and the round is still running and still joinable.
     */
    var finishedRoundId by remember { mutableStateOf<String?>(null) }

    /** The round whose board the player has dismissed with "Stay for next round". */
    var dismissedResultsId by remember { mutableStateOf<String?>(null) }

    /** What the player just did, kept until the board for that round arrives. */
    var finished by remember { mutableStateOf<FinishedRound?>(null) }

    /** The level whose celebration is on screen, if one is. */
    var celebrating by remember { mutableStateOf<Int?>(null) }

    fun startRound(seconds: Int) {
        val r = round ?: return
        if (playingRoundId != null || r.id == finishedRoundId) return
        playSeconds = seconds
        playingRoundId = r.id
        ArenaAnalytics.roundStarted(mode, lang)
    }

    fun startRoundIfDue() {
        val p = phase
        if (canJoinNow && p is ArenaPhase.Playing) startRound(p.secondsLeft)
    }

    // The phase says "playing" the moment the round opens, but the words arrive a network
    // hop later — and until they do the round cannot be entered. Watching only the phase
    // meant waiting for the next tick after they landed, which is the pause where the
    // lobby sat there saying "round in progress".
    LaunchedEffect(phase, canJoinNow) { startRoundIfDue() }

    // The player's XP, streak and ranks only move when they are on the board, so that is
    // the one moment to re-read them.
    LaunchedEffect(boardLoaded) {
        if (boardLoaded && board.any { it.isMe }) {
            ArenaProfileStore.roundScored()
            profile.refresh()
        }
    }

    // XP only moves in the Arena, so this is the one screen a level-up can happen behind.
    LaunchedEffect(summary.level) {
        if (playingRoundId == null && celebrating == null) {
            celebrating = PlayerLevel.pendingCelebration()
        }
    }

    LaunchedEffect(Unit) {
        ArenaAnalytics.lobby(lang)
        store.enter()
    }

    /** Whether to show the board instead of the lobby. */
    val showingResults = phase is ArenaPhase.Results &&
        round != null && finished?.roundId == round!!.id && dismissedResultsId != round!!.id

    val playing = playingRoundId
    val currentRound = round

    when {
        playing != null && currentRound != null && currentRound.id == playing -> {
            ArenaRoundHost(
                round = currentRound,
                mode = mode,
                deadlineMs = playDeadlineMs,
                seconds = playSeconds,
                onFinish = { guessed, skipped ->
                    finishedRoundId = playing
                    playingRoundId = null
                    // Kept in the round's own order rather than guessed-then-skipped, so
                    // the list reads back the way it was played.
                    val touched = guessed.map { it.first }.toSet() + skipped.toSet()
                    val got = guessed.map { it.first }.toSet()
                    finished = FinishedRound(
                        roundId = playing,
                        words = currentRound.words.map { it.w }.filter { it in touched }
                            .map { ArenaReviewWord(it, it in got) },
                        xpBefore = summary.xp,
                    )
                    ArenaAnalytics.roundFinished(mode, lang)
                    // Pushes the idle nudge out two days. Recorded on a submission, not
                    // on entering the lobby: the reminder is about playing.
                    hr.azzi.socialgames.alias.notifications.AliasNotifications
                        .recordArenaPlayed(context)
                    scope.launch { store.submit(playing, guessed, skipped) }
                },
                // Leaving the play screen early submits nothing, because there is nothing
                // to submit: the engine reports a result when it finishes and not before.
                onAbandon = {
                    finishedRoundId = playing
                    playingRoundId = null
                },
            )
        }

        finished != null && finished!!.roundId == round?.id && !boardLoaded -> {
            LaunchedEffect(finished!!.roundId) { ArenaAnalytics.results() }
            ArenaRoundReviewScreen(
                mode = mode,
                words = finished!!.words,
                secondsToResults = secondsToResults,
                onStay = { dismissedResultsId = round?.id; finished = null },
                onLeave = { store.leave(); onClose() },
            )
        }

        showingResults -> {
            LaunchedEffect(round?.id) { ArenaAnalytics.roundLeaderboard() }
            ArenaResultsScreen(
                board = board,
                boardLoaded = boardLoaded,
                secondsToNext = (phase as? ArenaPhase.Results)?.secondsToNext ?: 0,
                secondsToResults = secondsToResults,
                waitTotal = round?.let { ((it.boardFetchAtMs - it.endsAtMs) / 1000).toInt() } ?: 1,
                // Read as a difference rather than reported: the scorer writes the new
                // total onto the summary and the client held the old one.
                xpGained = finished?.let { (summary.xp - it.xpBefore).takeIf { d -> d > 0 } },
                onStay = { dismissedResultsId = round?.id; finished = null },
                onLeave = { store.leave(); onClose() },
            )
        }

        else -> {
            val language = remember(lang) {
                AILanguage.entries.firstOrNull { it.fileCode == lang } ?: AILanguage.EN
            }
            ArenaLobbyScreen(
                phase = phase,
                round = round,
                mode = mode,
                languageLabel = language.display,
                languageFlag = language.emoji,
                playersInLobby = playersInLobby,
                board = board,
                forceJoinSeconds = forceJoinSeconds,
                onJoinNow = { startRound(it) },
                onBack = { store.leave(); onClose() },
            )
        }
    }

    celebrating?.let { level ->
        ArenaLevelUpDialog(
            level = level,
            rewards = LevelRewards.rewardsAt(level),
            onDone = {
                PlayerLevel.markCelebrated(level)
                // Another rewarded level may have been passed in the same jump.
                celebrating = PlayerLevel.pendingCelebration()
            },
        )
    }
}

/**
 * What the player just did, kept until the board for that round arrives.
 *
 * The review screen is drawn from this rather than from a read: the client knows what it
 * submitted, and the server has not scored it yet — that is the whole reason the screen
 * exists.
 */
private data class FinishedRound(
    val roundId: String,
    val words: List<ArenaReviewWord>,
    val xpBefore: Int,
)

/** `mutableIntStateOf` under a name that does not collide with the import list above. */
private fun mutableIntStateOfCompat(value: Int) = androidx.compose.runtime.mutableIntStateOf(value)
