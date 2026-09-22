package hr.azzi.socialgames.alias.arena

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Server time, held as an offset from this device's own clock.
 *
 * Android port of iOS `ArenaClock.swift`.
 *
 * Every countdown in the Arena is measured against the server, never against the phone.
 * A device clock can be minutes out — set by hand, stale after a flight, or simply wrong
 * — and two players seeing different countdowns for the same round would each believe
 * the other cheated.
 *
 * Deliberately not how Online Play works: there the round is anchored to the wall clock
 * of whichever client pressed start, which is survivable in a four-person room and is not
 * survivable when hundreds of strangers are ranked against each other.
 */
object ArenaClock {

    private const val TAG = "ArenaClock"
    private const val REGION = "europe-west1"
    private const val RETRY_MS = 5_000L

    @Volatile
    private var offsetMs: Long? = null

    /**
     * When the last attempt was made, so a failing sync is retried at a sane rate rather
     * than four times a second.
     */
    @Volatile
    private var lastAttemptMs: Long = 0

    /** One sync at a time. The tick calls this from every wake-up. */
    private val lock = Mutex()

    val isSynced: Boolean get() = offsetMs != null

    /**
     * Fetches the server time once. Repeated calls are cheap and keep the last good value
     * if the network is unavailable — a slightly stale offset beats no Arena at all.
     */
    suspend fun sync() {
        lock.withLock {
            lastAttemptMs = System.currentTimeMillis()
            runCatching {
                val result = FirebaseFunctions.getInstance(REGION)
                    .getHttpsCallable("arenaClock").call().await()
                @Suppress("UNCHECKED_CAST")
                val data = result.data as? Map<String, Any?>
                val serverMs = (data?.get("nowMs") as? Number)?.toLong() ?: return@runCatching
                val deviceMs = System.currentTimeMillis()
                offsetMs = serverMs - deviceMs
                Log.d(TAG, "clock synced, offset ${serverMs - deviceMs}ms")
            }.onFailure {
                Log.w(TAG, "clock sync failed: ${it.message}")
                // Keep whatever we had. A first-run failure leaves `nowMs()` on device
                // time, which is wrong by whatever the clock is wrong by — but the round
                // document still bounds everything, so the worst case is a countdown that
                // is visibly off rather than a player scoring against the wrong round.
            }
        }
    }

    /**
     * Tries again for as long as the offset is unknown.
     *
     * Without this a single failed call — the Arena entered on a dead connection, say —
     * leaves every countdown running on the device's own clock for the rest of the
     * session, which is exactly the case this type exists to prevent.
     */
    suspend fun syncIfNeeded() {
        if (offsetMs != null) return
        if (System.currentTimeMillis() - lastAttemptMs <= RETRY_MS) return
        sync()
    }

    /**
     * Server epoch milliseconds: the device clock plus the offset measured at sync.
     *
     * A player who moves the clock mid-round moves this with it, which is the one hole
     * left — and it is a hole that cannot help them, because the server re-checks the
     * window before it accepts a submission.
     */
    fun nowMs(): Long = System.currentTimeMillis() + (offsetMs ?: 0L)
}
