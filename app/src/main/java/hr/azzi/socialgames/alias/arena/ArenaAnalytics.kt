package hr.azzi.socialgames.alias.arena

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * What the Arena reports.
 *
 * Android port of iOS `ArenaAnalytics.swift`. Every name is prefixed `android_` where the
 * iOS one says `ios_`, matching `AIAnalytics` — the two platforms answer the same
 * questions and have to be separable in the console.
 *
 * The funnel the Arena actually has is: reach the lobby → be dealt into a round → play it
 * out → see your result → see where that put you. Each of those is one event, and the two
 * that come in pairs are split by mode rather than carrying it as a parameter, because a
 * drop-off that only happens in one of the two modes is the interesting kind and a
 * parameter buries it.
 */
object ArenaAnalytics {

    private var appContext: Context? = null

    /** Called once from the Application, so the events that have no screen can still log. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun log(name: String, params: Bundle? = null) {
        val ctx = appContext ?: return
        FirebaseAnalytics.getInstance(ctx).logEvent(name, params)
    }

    private fun lang(language: String) = Bundle().apply { putString("language", language) }

    /**
     * The player reached the lobby — mode chosen, Arena entered, waiting for a round.
     * The top of the funnel: logged before any round exists, so the drop-off to
     * `…_started` is visible.
     *
     * The misspelling is deliberate: it matches the event iOS already reports, and a
     * dashboard that has to union two spellings is worse than one typo.
     */
    fun lobby(language: String) = log("android_alias_arena_loby", lang(language))

    /**
     * The Arena promo sheet was put on screen.
     *
     * The denominator for the two below: every showing resolves into exactly one of them,
     * so `shown - go_play - maybe` is the count that is still open.
     */
    fun promoShown() = log("android_alias_promo_shown")

    /**
     * The promo was declined.
     *
     * Every way out counts, not only the button it is named after: "Maybe later", the ×,
     * and a tap outside are one answer given three ways, and splitting them would make
     * the funnel not add up.
     */
    fun promoDismissed() = log("android_alias_promo_maybe_button")

    /** The promo converted — "Enter the Arena". */
    fun promoEntered() = log("android_alias_promo_go_play_button")

    /**
     * A round was entered, automatically or through "join now".
     *
     * [ArenaMode.GUESS] is the mode where the AI gives the clues and the player answers,
     * so it is the one that reports as `ai_explain`.
     */
    fun roundStarted(mode: ArenaMode, language: String) = log(
        if (mode == ArenaMode.GUESS) "android_alias_arena_ai_explain_started"
        else "android_alias_arena_you_explain_started",
        lang(language),
    )

    /**
     * A round was played to the whistle and submitted.
     *
     * Not logged for a round the player walked out of: that submits nothing and puts them
     * on no board, and counting it as finished would make the pair say the round was
     * completed when it was abandoned.
     */
    fun roundFinished(mode: ArenaMode, language: String) = log(
        if (mode == ArenaMode.GUESS) "android_alias_arena_ai_explain_finished"
        else "android_alias_arena_you_explain_finished",
        lang(language),
    )

    /**
     * The player's own result for the round they just played — correct, missed, net —
     * shown while the server is still scoring.
     */
    fun results() = log("android_alias_arena_results")

    /**
     * That round's board, once it has been scored. Distinct from [leaderboardShown]: this
     * is one round, that is the season.
     */
    fun roundLeaderboard() = log("android_alias_arena_leaderboard")

    /** The profile screen was shown — from the Arena or from the AI hub. */
    fun profileShown() = log("android_profile_shown")

    /** The season standings were shown — daily, weekly and monthly are one screen. */
    fun leaderboardShown() = log("android_leaderboard_shown")

    /**
     * The player reached a level worth counting.
     *
     * Only two: level 2 says the reward ladder started working at all, and level 5 says
     * they stayed long enough to earn a deck. Everything past that is a number nobody
     * would act on, and an event per level would bury the two that matter.
     */
    fun levelReached(level: Int) = log("android_level_$level")
}
