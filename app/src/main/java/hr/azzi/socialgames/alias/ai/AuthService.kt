package hr.azzi.socialgames.alias.ai

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/** Firebase auth + username + stats. Google sign-in goes through Credential Manager. */
object AuthService {

    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    val uid: String? get() = auth.currentUser?.uid
    val isSignedIn: Boolean get() = auth.currentUser != null
    val displayName: String? get() = auth.currentUser?.displayName

    /** Outcome of a Google sign-in attempt, so the UI can react appropriately. */
    sealed interface SignInResult {
        object Success : SignInResult
        /** User dismissed the picker — stay silent. */
        object Cancelled : SignInResult
        /** No Google account on the device — prompt the user to add one. */
        object NoAccount : SignInResult
        data class Error(val cause: Throwable) : SignInResult
    }

    /**
     * Launches the Google account picker via Credential Manager and signs into Firebase
     * with the returned ID token. Never throws — failures are mapped to [SignInResult].
     */
    suspend fun signInWithGoogle(context: Context, serverClientId: String): SignInResult {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
        val response = try {
            CredentialManager.create(context).getCredential(context, request)
        } catch (e: GetCredentialCancellationException) {
            return SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            return SignInResult.NoAccount
        } catch (e: GetCredentialException) {
            return SignInResult.Error(e)
        }
        return try {
            val cred = response.credential
            check(cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                "Unexpected credential type: ${cred.type}"
            }
            val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            SignInResult.Success
        } catch (e: Exception) {
            SignInResult.Error(e)
        }
    }

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
