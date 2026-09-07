package hr.azzi.socialgames.alias.Service.RoundVideo

import com.google.firebase.storage.FirebaseStorage
import hr.azzi.socialgames.alias.Service.SocialRegion
import com.google.firebase.storage.StorageMetadata
import java.io.File

/**
 * Uploads a replay the player has explicitly approved.
 *
 * Nothing here runs without that approval: the round's video stays on the device until
 * somebody taps through the confirmation dialog, which is the only reason it is acceptable
 * to send a recording of two people's faces anywhere at all.
 *
 * There is no sign-in. The Storage rule grants this write by path, not by identity — a
 * party game has no account behind it, and creating one purely to satisfy a rule would
 * leave a stray user record on every device that ever sent a clip. App Check still gates
 * the request, so the path is reachable by the app and not by whoever guesses it.
 */
/**
 * What a screen needs in order to offer "share with Alias".
 *
 * The saved-replays list keeps a file name and a team, not the deck or the language, so a
 * clip opened from there cannot say which audience it belongs to. Rather than hide the
 * button, it lands in a `history` folder — whoever reviews the bucket can tell those apart
 * from the ones that arrived with their round still known.
 */
data class AliasSubmission(
    val deckId: String,
    val languageCode: String,
    /** Path segment under `replays/`. */
    val folder: String,
) {
    companion object {
        fun round(deckId: String, languageCode: String, region: SocialRegion) =
            AliasSubmission(deckId, languageCode, region.id)

        fun history(languageCode: String) =
            AliasSubmission(deckId = "history", languageCode = languageCode, folder = "history")
    }
}

object ReplayUploader {

    /**
     * `deck_language_timestamp.mp4`, e.g. `game_alias_classic_CRO_1757183045.mp4`.
     *
     * The three parts are what anyone sorting through the bucket actually needs: which deck
     * it came from, which audience it belongs to, and when it arrived. Seconds since the
     * epoch rather than a formatted date — it sorts lexically, which a localised date does
     * not.
     */
    fun fileName(deckId: String, languageCode: String, nowMillis: Long = System.currentTimeMillis()): String =
        "${deckId}_${languageCode.uppercase()}_${nowMillis / 1000}.mp4"

    /** `replays/{folder}/{file}` — one path segment for the folder, which is what the rule matches on. */
    fun storagePath(folder: String, fileName: String): String = "replays/$folder/$fileName"

    /**
     * Replays already sent during this run of the app.
     *
     * The same video is reachable from two places — the results card and the player — and
     * without this, sending from both would put two copies in the bucket under different
     * timestamps. Session-scoped on purpose: it guards against a double tap, not against
     * somebody deliberately re-sending after a relaunch.
     */
    private val sentFileNames = mutableSetOf<String>()

    fun hasSent(path: String): Boolean = File(path).name in sentFileNames

    /**
     * Uploads [file] and reports back on the caller's thread.
     *
     * The transfer keeps going while the player walks away from the screen — that is the
     * point of doing it out of sight — but it lives with the process. If Android kills the
     * app mid-upload the clip simply is not sent; nothing is queued for later, because a
     * video the player has moved on from is not worth a background worker.
     */
    fun upload(
        file: File,
        deckId: String,
        languageCode: String,
        folder: String,
        onResult: (Result<String>) -> Unit,
    ) {
        if (!file.exists()) {
            onResult(Result.failure(IllegalStateException("Missing local file ${file.name}")))
            return
        }

        val path = storagePath(folder, fileName(deckId, languageCode))
        val metadata = StorageMetadata.Builder().setContentType("video/mp4").build()

        FirebaseStorage.getInstance().reference.child(path)
            .putFile(android.net.Uri.fromFile(file), metadata)
            .addOnSuccessListener {
                sentFileNames += file.name
                onResult(Result.success(path))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }
}
