package hr.azzi.socialgames.alias.ui.screens.ai

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Loop
import app.rive.runtime.kotlin.core.Rive
import hr.azzi.socialgames.alias.R

/**
 * The animated "AI Party Bot" mascot (Rive), ported from iOS AliasBot.
 *
 * We drive the mascot by playing its named animations DIRECTLY (no state machine,
 * no data binding) — robust and independent of the file's state-machine wiring.
 *   loops:     idle_loop / talking_loop / listening_loop / thinking_loop
 *   one-shots: correct_once / wrong_once / celebration_once /
 *              timeout_celebration_once / timeout_fail_once  (play once → mood)
 * All mascots share the "avatar" artboard. For now the file is hardcoded to
 * aliasbot.riv; swap [BOT_RES] to use catbot / wordmonster / aliasowl.
 */
private val BOT_RES = R.raw.aliasbot

/** Loads Rive's native library once. The androidx.startup auto-init isn't always
 *  triggered (App Startup provider can be absent/removed), which crashes with
 *  UnsatisfiedLinkError when the RiveAnimationView is created — so init explicitly. */
private var riveInitialized = false
private fun ensureRiveInitialized(context: Context) {
    if (riveInitialized) return
    Rive.init(context.applicationContext)
    riveInitialized = true
}

/** Looping "mood" states — value is the Rive animation name. */
enum class BotMood(val anim: String) {
    Idle("idle_loop"),
    Talking("talking_loop"),
    Listening("listening_loop"),
    Thinking("thinking_loop"),
}

/** One-shot reactions — animation name + how long it runs before returning to the mood. */
enum class BotReaction(val anim: String, val durationMs: Long) {
    Correct("correct_once", 900),
    Wrong("wrong_once", 900),
    Celebration("celebration_once", 1800),
    TimeoutCelebration("timeout_celebration_once", 1800),
    TimeoutFail("timeout_fail_once", 1500),
}

/** Thin wrapper over the Rive view. Mirrors iOS AliasBot: play a mood loop, or a
 *  one-shot reaction that returns to the current mood when it finishes. */
class AliasBot(private val view: RiveAnimationView) {
    private var current = BotMood.Idle
    private var reacting = false
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    /** Set the looping mood. No-op if unchanged or while a one-shot reaction plays. */
    fun setMood(mood: BotMood) {
        val changed = mood != current
        current = mood
        if (reacting || !changed) return
        playLoop(mood)
    }

    /** Play a one-shot reaction, then return to the current mood loop. */
    fun react(reaction: BotReaction) {
        reacting = true
        pending?.let { handler.removeCallbacks(it) }
        safePlay(reaction.anim, Loop.ONESHOT)
        val run = Runnable { reacting = false; playLoop(current) }
        pending = run
        handler.postDelayed(run, reaction.durationMs)
    }

    private fun playLoop(mood: BotMood) {
        // Loop.AUTO respects each timeline's authored loop / ping-pong, so it
        // repeats seamlessly (same as iOS .autoLoop) with no jump mid-cycle.
        safePlay(mood.anim, Loop.AUTO)
    }

    private fun safePlay(name: String, loop: Loop) {
        try {
            view.stop()
            view.play(name, loop = loop)
        } catch (_: Throwable) { /* view not ready / animation missing — ignore */ }
    }
}

/** Holds the live [AliasBot] plus the current mood, so a screen can push mood
 *  changes and fire reactions. Create with [rememberBotController]. */
class BotController {
    internal var bot: AliasBot? = null
    var mood: BotMood by mutableStateOf(BotMood.Idle)
    fun react(reaction: BotReaction) { bot?.react(reaction) }
}

@Composable
fun rememberBotController(): BotController = remember { BotController() }

/**
 * Renders the mascot (hardcoded aliasbot.riv / artboard "avatar").
 * Drive it by setting [controller].mood and calling [controller].react(...).
 */
@Composable
fun BotAvatar(controller: BotController, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ensureRiveInitialized(ctx)
            RiveAnimationView(ctx).apply {
                setRiveResource(
                    AiAvatar.current(ctx).rawRes,
                    artboardName = "avatar",
                    animationName = BotMood.Idle.anim,
                    autoplay = true,
                    fit = Fit.CONTAIN,
                )
                controller.bot = AliasBot(this).also { it.setMood(controller.mood) }
            }
        },
        onRelease = { view ->
            controller.bot = null
            view.stop()
        },
    )
    // Push mood changes to the (already created) bot view.
    LaunchedEffect(controller.mood) {
        controller.bot?.setMood(controller.mood)
    }
}

/** A static idle thumbnail of a given mascot `.riv` (artboard "avatar"), for the
 *  avatar picker / setup pill. */
@Composable
fun AvatarThumb(rawRes: Int, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ensureRiveInitialized(ctx)
            RiveAnimationView(ctx).apply {
                setRiveResource(
                    rawRes,
                    artboardName = "avatar",
                    animationName = BotMood.Idle.anim,
                    autoplay = true,
                    fit = Fit.CONTAIN,
                )
            }
        },
        onRelease = { it.stop() },
    )
}
