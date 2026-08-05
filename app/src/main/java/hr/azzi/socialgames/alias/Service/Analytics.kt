package hr.azzi.socialgames.alias.Service

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * The round-video events, and their names in one place.
 *
 * Names are constants rather than literals at the call sites for the obvious reason: a
 * mistyped event is not an error, it is a second event that quietly collects nothing, and
 * nobody finds out until a week of data is missing from the dashboard.
 *
 * Every name is prefixed `android_` so the two platforms can be told apart in a shared
 * project, and each stays inside Firebase's forty-character limit.
 */
object Analytics {

    /** A round started, and it is being filmed. */
    const val RECORDING_ROUND_ENABLED = "android_recording_round_enabled"

    /** A round started without filming. */
    const val RECORDING_ROUND_DISABLED = "android_recording_round_disabled"

    /** A clip came out of a round. The suffixed pair says with how many cameras. */
    const val REPLAY_CREATION_SUCCESS = "android_replay_creation_success"
    const val REPLAY_CREATION_SUCCESS_DUAL = "android_replay_creation_success_dual"
    const val REPLAY_CREATION_SUCCESS_SINGLE = "android_replay_creation_success_single"

    /** A round was filmed and produced nothing usable. */
    const val REPLAY_CREATION_FAIL = "android_replay_creation_fail"
    const val REPLAY_CREATION_FAIL_DUAL = "android_replay_creation_fail_dual"
    const val REPLAY_CREATION_FAIL_SINGLE = "android_replay_creation_fail_single"

    /** Watched from the results screen, straight after the round. */
    const val REPLAY_PLAY = "android_replay_play"

    /** Shared from the player. */
    const val REPLAY_SHARE = "android_replay_share"

    /** The round's result was shared as an image from the results screen. */
    const val ROUND_RESULT_SHARED = "android_round_result_shared"

    /** The saved rounds were opened from settings. */
    const val REPLAY_LIST_VISITED = "android_replay_list_visited"

    /** Acted on from the saved list rather than from the round that made it. */
    const val REPLAY_LIST_ITEM_PLAY = "android_replay_list_item_play"
    const val REPLAY_LIST_ITEM_SHARE = "android_replay_list_item_share"
    const val REPLAY_LIST_ITEM_DELETE = "android_replay_list_item_delete"

    fun log(context: Context, event: String) {
        runCatching { FirebaseAnalytics.getInstance(context).logEvent(event, null) }
    }

    /**
     * The outcome of a round's recording: the plain event and the one that says whether both
     * cameras were used, so a total does not have to be summed from the two halves.
     */
    fun logReplayCreation(context: Context, succeeded: Boolean, dualCamera: Boolean) {
        log(context, if (succeeded) REPLAY_CREATION_SUCCESS else REPLAY_CREATION_FAIL)
        log(
            context,
            when {
                succeeded && dualCamera -> REPLAY_CREATION_SUCCESS_DUAL
                succeeded -> REPLAY_CREATION_SUCCESS_SINGLE
                dualCamera -> REPLAY_CREATION_FAIL_DUAL
                else -> REPLAY_CREATION_FAIL_SINGLE
            },
        )
    }
}
