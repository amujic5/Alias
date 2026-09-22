package hr.azzi.socialgames.alias.arena

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * The level, known from launch rather than from the first visit to the AI section.
 *
 * The Android half of what iOS does with `ArenaProfileStore.shared`. There a profile store
 * can be a main-actor singleton that outlives every screen; here it is an `AndroidViewModel`
 * scoped to whoever owns it, so an app-wide read needs its own home.
 *
 * Why it has to exist at all: the deck screen is two taps from Home and gates on level,
 * and the avatar picker does the same. Both are reached long before anybody opens the
 * Arena, and a padlock that has to wait for Firestore to decide is a padlock that
 * flickers. A player who earned Draw & Guess at level five and reinstalled would find it
 * locked again.
 *
 * Two reads, no aggregation: the summary for the level, and the profile document for the
 * name and avatar. The six aggregation queries behind the standings are wanted only by
 * the screens that show them, and `ArenaProfileStore` fetches those when one opens.
 */
object ArenaIdentity {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var listener: FirebaseAuth.AuthStateListener? = null
    private var boundUid: String? = null

    /** Called once from the Application. */
    fun start(context: Context) {
        PlayerLevel.init(context)
        ArenaAnalytics.init(context)
        if (listener != null) return

        listener = FirebaseAuth.AuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid == null) {
                boundUid = null
                PlayerLevel.unbind()
                return@AuthStateListener
            }
            if (uid == boundUid) return@AuthStateListener
            boundUid = uid
            // Before the read, so nothing derived from the last account survives into
            // this one.
            PlayerLevel.bind(uid)
            scope.launch { sync() }
        }.also { FirebaseAuth.getInstance().addAuthStateListener(it) }
    }

    /**
     * Re-reads the level. Called on every foreground — it is the one place that catches a
     * level earned on another device.
     */
    fun refresh() {
        if (boundUid == null) return
        scope.launch { sync() }
    }

    private suspend fun sync() {
        val uid = boundUid ?: return
        val summary = runCatching {
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("arena").document("summary")
                .get().await()
        }.getOrNull() ?: return
        if (uid != boundUid) return
        PlayerLevel.store(ArenaSummary.from(summary.data).level)
    }
}
