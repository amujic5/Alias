package hr.azzi.socialgames.alias.ai

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

/** Firestore interface for AI challenges. Mirrors iOS AIChallengeRepository. */
object AIChallengeRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val challenges get() = db.collection("aiChallenges")

    suspend fun create(
        config: AIPracticeConfig,
        deckName: String,
        words: List<String>,
        result: AIPracticeResult,
        creatorId: String,
        creatorName: String,
    ): AIChallenge {
        val id = UUID.randomUUID().toString()
        val score = result.correctWords.size
        val ref = challenges.document(id)
        val data = hashMapOf(
            "creatorId" to creatorId,
            "creatorName" to creatorName,
            "mode" to config.mode.raw,
            "deckId" to config.deck.id,
            "deckName" to deckName,
            "language" to config.language.code,
            "totalSeconds" to config.totalSeconds,
            "words" to words,
            "creatorScore" to score,
            "createdAt" to FieldValue.serverTimestamp(),
            "players" to listOf(creatorId),
            "playerScores" to mapOf(creatorId to score),
        )
        val batch = db.batch()
        batch.set(ref, data)
        batch.set(ref.collection("plays").document(creatorId),
            playData(id, creatorId, creatorName, true, result, finished = true))
        batch.commit().await()
        return load(id) ?: throw IllegalStateException("create failed")
    }

    suspend fun load(id: String): AIChallenge? =
        runCatching { mapChallenge(challenges.document(id).get().await()) }.getOrNull()

    suspend fun join(challenge: AIChallenge, uid: String, name: String) {
        val ref = challenges.document(challenge.id)
        val batch = db.batch()
        batch.update(ref, "players", FieldValue.arrayUnion(uid))
        batch.set(
            ref.collection("plays").document(uid),
            hashMapOf(
                "challengeId" to challenge.id,
                "playerId" to uid,
                "playerName" to name,
                "isCreator" to false,
                "correctWords" to emptyList<String>(),
                "skippedWords" to emptyList<String>(),
                "playedAt" to FieldValue.serverTimestamp(),
                "finished" to false,
            ),
            SetOptions.merge(),
        )
        batch.commit().await()
    }

    suspend fun submit(challengeId: String, uid: String, name: String, isCreator: Boolean, result: AIPracticeResult) {
        val ref = challenges.document(challengeId)
        val batch = db.batch()
        batch.set(ref.collection("plays").document(uid),
            playData(challengeId, uid, name, isCreator, result, finished = true))
        batch.update(ref, mapOf(
            "players" to FieldValue.arrayUnion(uid),
            "playerScores.$uid" to result.correctWords.size,
        ))
        batch.commit().await()
    }

    suspend fun plays(challengeId: String): List<AIChallengePlay> {
        val snap = runCatching { challenges.document(challengeId).collection("plays").get().await() }.getOrNull()
            ?: return emptyList()
        return snap.documents.mapNotNull { mapPlay(it) }
    }

    suspend fun play(challengeId: String, uid: String): AIChallengePlay? =
        runCatching { mapPlay(challenges.document(challengeId).collection("plays").document(uid).get().await()) }.getOrNull()

    suspend fun recent(uid: String, limit: Long = 5): List<AIChallenge> {
        val snap = runCatching {
            challenges.whereArrayContains("players", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get().await()
        }.getOrNull() ?: return emptyList()
        return snap.documents.mapNotNull { mapChallenge(it) }
    }

    private fun playData(
        challengeId: String, uid: String, name: String, isCreator: Boolean,
        result: AIPracticeResult, finished: Boolean,
    ): HashMap<String, Any> = hashMapOf(
        "challengeId" to challengeId,
        "playerId" to uid,
        "playerName" to name,
        "isCreator" to isCreator,
        "correctWords" to result.correctWords,
        "skippedWords" to result.skippedWords,
        "playedAt" to FieldValue.serverTimestamp(),
        "finished" to finished,
    )

    @Suppress("UNCHECKED_CAST")
    private fun mapChallenge(doc: DocumentSnapshot): AIChallenge? {
        if (!doc.exists()) return null
        val scores = (doc.get("playerScores") as? Map<String, Any?>)?.mapValues { (it.value as? Number)?.toInt() ?: 0 }
            ?: emptyMap()
        return AIChallenge(
            id = doc.id,
            creatorId = doc.getString("creatorId") ?: "",
            creatorName = doc.getString("creatorName") ?: "",
            mode = doc.getString("mode") ?: "aiExplains",
            deckId = doc.getString("deckId") ?: "",
            deckName = doc.getString("deckName") ?: "",
            language = doc.getString("language") ?: "en",
            totalSeconds = (doc.getLong("totalSeconds") ?: 60).toInt(),
            words = (doc.get("words") as? List<String>) ?: emptyList(),
            creatorScore = (doc.getLong("creatorScore") ?: 0).toInt(),
            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
            players = (doc.get("players") as? List<String>) ?: emptyList(),
            playerScores = scores,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapPlay(doc: DocumentSnapshot): AIChallengePlay? {
        if (!doc.exists()) return null
        return AIChallengePlay(
            challengeId = doc.getString("challengeId") ?: "",
            playerId = doc.getString("playerId") ?: doc.id,
            playerName = doc.getString("playerName") ?: "",
            isCreator = doc.getBoolean("isCreator") ?: false,
            correctWords = (doc.get("correctWords") as? List<String>) ?: emptyList(),
            skippedWords = (doc.get("skippedWords") as? List<String>) ?: emptyList(),
            playedAt = doc.getTimestamp("playedAt")?.toDate()?.time ?: 0L,
            finished = doc.getBoolean("finished") ?: false,
        )
    }
}
