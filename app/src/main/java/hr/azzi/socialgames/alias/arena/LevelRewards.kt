package hr.azzi.socialgames.alias.arena

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import hr.azzi.socialgames.alias.Service.BoardGame
import hr.azzi.socialgames.alias.Service.BoardGameId
import hr.azzi.socialgames.alias.Service.NoAds

/**
 * The reward ladder.
 *
 * Android port of iOS `LevelRewards.swift`.
 *
 * Nothing here is stored. A level is a number, every reward names the level it arrives
 * at, and "do I have it" is that comparison — so there is no unlock list to write, no
 * state to migrate, no way for the two to disagree, and no cheat surface beyond the XP
 * itself.
 *
 * One reward per level, never two: avatars on the levels between, a deck on every fifth,
 * and the ads switched off at fifty.
 */
object LevelRewards {

    /**
     * Every fifth level is a deck level, and a deck level gives nothing else.
     *
     * The rule, not a coincidence in a table: a player who works out that decks come on
     * the fives has understood the whole ladder, and the two lists below are derived from
     * it rather than typed out, so they cannot drift apart from it or from each other.
     */
    const val DECK_STEP = 5

    /**
     * The level that switches the ads off for good.
     *
     * On the fives like every other big reward, and far enough out to be a trophy rather
     * than a discount. The purchase remains the way anybody actually gets there.
     */
    const val NO_ADS_LEVEL = 50

    /**
     * How far ahead the grid reveals what is coming. Beyond this the cards show a
     * silhouette — everything visible is a reason to keep going; a wall of named rewards
     * twenty levels out is a reason to work out how long that will take and stop.
     */
    const val REVEAL_HORIZON = 6

    /**
     * Decks, in the order they are given away.
     *
     * Draw & Guess first because it is the one most worth trying, Movies & TV last
     * because it has the broadest appeal. Buying still works at any time; the ladder is a
     * second door to the same deck, not a replacement for the first.
     */
    val deckOrder: List<BoardGameId> = listOf(
        BoardGameId.DRAW_GUESS,
        BoardGameId.NBA,
        BoardGameId.WHOAMI,
        BoardGameId.EASY_MEDIUM_HARD,
        BoardGameId.MOVIES,
    )

    /**
     * Avatars, in catalogue order.
     *
     * The first eight of the catalogue are not here: they are free from the first launch,
     * so a new player picks from eight rather than being handed one and shown twenty-six
     * padlocks.
     */
    val avatarOrder: List<String> = listOf(
        "human_tessa", "human_finn", "red_panda_kiko", "panda_nori",
        "dragon_ember", "axolotl_bubbles", "corgi_bolt", "triceratops_pebble",
        "astronaut_nova", "wizard_hex", "parrot_rio", "ninja_shade",
        "gamer_pixel", "skater_dash", "ghost_boo", "mushroom_sprout",
        "human_jax", "human_zoe", "human_rex",
    )

    /** Deck levels: 5, 10, 15, 20, 25. */
    val decks: List<Pair<BoardGameId, Int>> =
        deckOrder.mapIndexed { i, id -> id to (i + 1) * DECK_STEP }

    /** Avatar levels: every level from 2 upwards that is not a deck level, in order. */
    val avatars: List<Pair<String, Int>> = buildList {
        var level = 2
        for (id in avatarOrder) {
            while (level % DECK_STEP == 0) level++   // a deck level gives only a deck
            add(id to level)
            level++
        }
    }

    /** The last level that hands anything over. Past it the ladder is finished. */
    val lastRewardedLevel: Int
        get() = maxOf(NO_ADS_LEVEL, decks.maxOf { it.second }, avatars.maxOf { it.second })

    // MARK: Lookups

    /** The level a deck is handed over at, or null for one the ladder never gives. */
    fun levelFor(deck: BoardGameId): Int? = decks.firstOrNull { it.first == deck }?.second

    /** The level an avatar is handed over at. Zero means it was never locked. */
    fun levelForAvatar(id: String): Int = avatars.firstOrNull { it.first == id }?.second ?: 0

    /** Every reward, in the order it arrives. */
    val ladder: List<Pair<Int, LevelReward>> by lazy {
        (decks.map { it.second to LevelReward.Deck(it.first) } +
            avatars.map { it.second to LevelReward.Avatar(it.first) } +
            listOf(NO_ADS_LEVEL to LevelReward.NoAds))
            .sortedWith(compareBy({ it.first }, { if (it.second is LevelReward.Deck) 0 else 1 }))
    }

    /** What arrives exactly at [level]. */
    fun rewardsAt(level: Int): List<LevelReward> =
        ladder.filter { it.first == level }.map { it.second }

    /** The next level that gives anything, counting from [level] upwards. */
    fun nextRewardLevel(after: Int): Int? =
        ladder.firstOrNull { it.first > after }?.first
}

/**
 * A single thing the ladder hands over.
 *
 * The ladder itself is three lists because the kinds are looked up by different keys;
 * this is the merged view of them, which is what both reward screens iterate.
 */
sealed interface LevelReward {
    data class Deck(val id: BoardGameId) : LevelReward
    data class Avatar(val id: String) : LevelReward

    /**
     * Banners and interstitials, gone. The rewarded videos stay — those are a trade the
     * player opts into, not something done to them.
     */
    data object NoAds : LevelReward

    val key: String
        get() = when (this) {
            is Deck -> "deck-${id.name}"
            is Avatar -> "avatar-$id"
            NoAds -> "no-ads"
        }

    val isDeck: Boolean get() = this is Deck

    /**
     * Shown by name however far off it is.
     *
     * The mystery silhouettes exist so the far end of the ladder stays interesting. This
     * one *is* the far end — hiding the single biggest thing on offer behind a question
     * mark would be hiding the reason to keep climbing.
     */
    val isAlwaysRevealed: Boolean get() = this is NoAds
}

/**
 * The player's Arena level, readable without a network call.
 *
 * The level lives on the server and reaches the app through `ArenaProfileStore`, which
 * needs a session and a read. The deck screen and the avatar picker have neither — they
 * are reached from Home, often before anybody has signed in even anonymously — and a
 * padlock that has to wait for Firestore to decide is a padlock that flickers.
 *
 * So the number is mirrored here every time the profile is read. This is a cache of one
 * integer, not a record of what is unlocked: the unlocks are still derived from it every
 * time they are asked for.
 */
object PlayerLevel {

    private const val PREFS = "alias_player_level"
    private const val KEY_LEVEL = "player_arena_level"
    private const val KEY_SEEN = "player_level_seen"
    private const val KEY_CELEBRATED = "player_celebrated_level"
    private const val KEY_LOGGED = "player_level_logged"
    private const val KEY_UID = "player_level_uid"

    /** The levels worth an analytics event. Deliberately two. */
    private val milestones = listOf(2, 5)

    private lateinit var prefs: SharedPreferences

    /** Listeners that redraw when the level lands — the deck screen, mainly. */
    private val listeners = mutableListOf<() -> Unit>()

    /** Called once from the Application. Everything below is a no-op until it is. */
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private val ready: Boolean get() = ::prefs.isInitialized

    /**
     * Level 1 until the Arena has said otherwise, which is what a player who has never
     * opened it actually is.
     */
    val current: Int
        get() = if (ready) maxOf(1, prefs.getInt(KEY_LEVEL, 1)) else 1

    /**
     * Posted when the level changes, so screens already drawn can redraw.
     *
     * The level arrives asynchronously — one read at launch — and the screens that gate
     * on it may have laid themselves out long before it landed. Without this a player who
     * opens the app and goes straight to New Game sees the padlocks from the last
     * session, and a reinstalled player sees all of them.
     */
    fun addListener(block: () -> Unit) { listeners += block }
    fun removeListener(block: () -> Unit) { listeners -= block }
    private fun notifyChanged() = listeners.toList().forEach { it() }

    fun store(level: Int) {
        if (!ready) return

        // The first level this device is ever told is not a level-up.
        //
        // A reinstall, or a sign-in on a second phone, arrives already at level seven —
        // and the naive "it went up, log it" would report that as reaching two and five
        // in the same second. Whatever is already true on arrival is history: recorded,
        // not announced.
        val firstEver = !prefs.getBoolean(KEY_SEEN, false)
        if (firstEver) {
            prefs.edit {
                putBoolean(KEY_SEEN, true)
                putStringSet(KEY_LOGGED, (logged + milestones.filter { it <= level }).map(Int::toString).toSet())
                // Nothing already true is owed a celebration either.
                putInt(KEY_CELEBRATED, maxOf(celebrated, level))
            }
        }

        val previous = current
        if (level == previous) return

        val crossedNoAds = previous < LevelRewards.NO_ADS_LEVEL && level >= LevelRewards.NO_ADS_LEVEL
        prefs.edit { putInt(KEY_LEVEL, level) }
        notifyChanged()
        if (crossedNoAds) NoAds.notifyChanged()

        // Logged from here rather than from the celebration screen: this is the one place
        // a level changes, and the celebration can be deferred — it waits out a round in
        // progress — or never shown at all.
        val done = logged.toMutableSet()
        for (m in milestones) {
            if (m !in done && previous < m && level >= m) {
                ArenaAnalytics.levelReached(m)
                done += m
            }
        }
        if (done != logged) {
            prefs.edit { putStringSet(KEY_LOGGED, done.map(Int::toString).toSet()) }
        }
    }

    private val logged: Set<Int>
        get() = if (ready) {
            prefs.getStringSet(KEY_LOGGED, emptySet()).orEmpty().mapNotNull(String::toIntOrNull).toSet()
        } else emptySet()

    // MARK: Account

    /**
     * Ties everything derived from the level to one account.
     *
     * The level, what has been celebrated and what has been logged all describe a
     * *player*, and keeping them per device is what let a guest who reached level two and
     * then signed into an account sitting at fifteen keep the guest's bookkeeping: the
     * level jumped thirteen at once with nothing marked as seen, and the flow would put
     * up a full-screen celebration for every rewarded level in between.
     *
     * Compared against the **stored** uid rather than anything held in memory. A fresh
     * process has no bound account yet, so "different from what I have" is true on every
     * launch — clearing on that would re-prime each time, and a milestone would never be
     * reported because every launch would mark it as already past.
     */
    fun bind(uid: String) {
        if (!ready || prefs.getString(KEY_UID, null) == uid) return
        prefs.edit {
            putString(KEY_UID, uid)
            remove(KEY_LEVEL); remove(KEY_SEEN); remove(KEY_CELEBRATED); remove(KEY_LOGGED)
        }
        // The padlocks were drawn against the previous account's level.
        notifyChanged()
    }

    /**
     * Forgets the account on sign-out.
     *
     * Without this the decks the last player earned stay unlocked on Home until somebody
     * signs in again — an entitlement outliving the account that held it.
     */
    fun unbind() {
        if (!ready || prefs.getString(KEY_UID, null) == null) return
        prefs.edit {
            remove(KEY_LEVEL); remove(KEY_SEEN); remove(KEY_CELEBRATED)
            remove(KEY_LOGGED); remove(KEY_UID)
        }
        notifyChanged()
    }

    // MARK: Celebrating

    /**
     * The highest level already celebrated for the bound account.
     *
     * The one thing here that genuinely has to be remembered. Everything else is derived
     * from the level, but "have I already shown this" is not a property of the level —
     * without it the screen would reappear on every launch, and with a naive "celebrate
     * on change" it would never appear at all for somebody who levelled up while the app
     * was closed.
     */
    private val celebrated: Int
        get() = if (ready) prefs.getInt(KEY_CELEBRATED, 0) else 0

    /**
     * The next level owed a celebration, or null when none is.
     *
     * Only levels that actually gave something: congratulating somebody on receiving
     * nothing teaches them to dismiss the next one without reading it. Levels passed
     * while the app was closed are still owed, and are shown one at a time.
     */
    fun pendingCelebration(): Int? {
        val from = maxOf(celebrated, 1)
        if (current <= from) return null
        return ((from + 1)..current).firstOrNull { LevelRewards.rewardsAt(it).isNotEmpty() }
    }

    /** Marks [level] — and everything below it, rewarded or not — as done with. */
    fun markCelebrated(level: Int) {
        if (!ready) return
        prefs.edit { putInt(KEY_CELEBRATED, maxOf(celebrated, level)) }
    }
}

// MARK: - Applying the ladder

/** The level this deck is given away at, if it is on the ladder. */
val BoardGame.unlockLevel: Int? get() = LevelRewards.levelFor(id)

/**
 * Whether the ladder has handed this deck over.
 *
 * Not the whole answer to "can I play this" — buying still works, and most decks are free
 * outright. See the call sites, which or the three together.
 */
val BoardGame.isUnlockedByLevel: Boolean
    get() = unlockLevel?.let { PlayerLevel.current >= it } ?: false
