package hr.azzi.socialgames.alias.ai

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/** Firebase auth + username + stats. Sign-in itself is launched via FirebaseUI in the UI layer. */
object AuthService {

    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    val uid: String? get() = auth.currentUser?.uid
    val isSignedIn: Boolean get() = auth.currentUser != null
    val displayName: String? get() = auth.currentUser?.displayName

    /** A freshly created Firebase user has no username yet. */
    suspend fun needsUsername(): Boolean {
        val id = uid ?: return false
        val name = loadName(id)
        return name.isNullOrBlank() || name == "You"
    }

    suspend fun loadName(id: String): String? = runCatching {
        db.collection("users").document(id).get().await().getString("name")
    }.getOrNull()

    suspend fun isUsernameAvailable(handle: String): Boolean {
        val lower = handle.trim().lowercase()
        if (lower.isEmpty()) return false
        val snap = runCatching { db.collection("usernames").document(lower).get().await() }.getOrNull() ?: return false
        return !snap.exists() || snap.getString("uid") == uid
    }

    /** Valid handle: 3-15 alphanumeric chars. */
    fun isValidHandle(handle: String): Boolean = Regex("^[A-Za-z0-9]{3,15}$").matches(handle.trim())

    /** Claims the username atomically. Throws if taken by someone else. */
    suspend fun setUsername(handle: String) {
        val id = uid ?: throw IllegalStateException("not signed in")
        val trimmed = handle.trim()
        val lower = trimmed.lowercase()
        val unameRef = db.collection("usernames").document(lower)
        val userRef = db.collection("users").document(id)
        db.runTransaction { tx ->
            val snap = tx.get(unameRef)
            if (snap.exists() && snap.getString("uid") != id) throw IllegalStateException("username_taken")
            tx.set(unameRef, mapOf("uid" to id, "handle" to trimmed))
            tx.set(
                userRef,
                mapOf("name" to trimmed, "usernameLower" to lower, "updatedAt" to FieldValue.serverTimestamp()),
                SetOptions.merge(),
            )
        }.await()
        runCatching { auth.currentUser?.updateProfile(userProfileChangeRequest { displayName = trimmed })?.await() }
    }

    suspend fun userStats(id: String): AIUserStats {
        val snap = runCatching { db.collection("users").document(id).get().await() }.getOrNull()
            ?: return AIUserStats()
        return AIUserStats(
            wins = (snap.getLong("aiWins") ?: 0).toInt(),
            losses = (snap.getLong("aiLosses") ?: 0).toInt(),
            draws = (snap.getLong("aiDraws") ?: 0).toInt(),
        )
    }

    suspend fun currentName(): String =
        uid?.let { loadName(it) } ?: displayName ?: "You"

    fun signOut() = auth.signOut()
}
