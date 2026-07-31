package hr.azzi.socialgames.alias.Service.RoundVideo

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** One saved round. */
data class RoundMediaItem(
    val id: String,
    val teamName: String,
    /** The player who explained. */
    val explainer: String,
    /** The player who guessed. */
    val guesser: String,
    val correctCount: Int,
    val skippedCount: Int,
    val recordedAt: Long,
    /**
     * File name, deliberately not a path.
     *
     * The index lives inside the folder it describes, so the folder is known by the time a
     * row is read — writing the prefix into every row would be the same fact twice, and two
     * copies of a fact can disagree. They would, too: an app moved to adopted storage has
     * its data under `/mnt/expand/<uuid>/…` instead of `/data/user/0/…`, and every absolute
     * path recorded before the move would point at nothing.
     */
    val fileName: String,
)

enum class RoundMediaSort { NEWEST, OLDEST, MOST_CORRECT, MOST_SKIPPED }

/**
 * Where a round's video lives once the round is over, and what is known about it.
 *
 * Clips used to be written to the cache directory and swept an hour later. They are now
 * kept: moved into a folder of their own and recorded in an index, so the app can list them,
 * sort them, and hand them back long after the game has finished.
 *
 * Backed by a JSON index rather than Room. One row per round is a couple of hundred bytes
 * and the sorts are `sortedWith`, which does not earn an entity, a DAO, a database and a
 * migration every time the row grows a field. The whole store sits behind this one object,
 * so swapping the backing out later touches nothing else.
 */
object RoundMediaLibrary {

    private const val TAG = "RoundVideo"
    private const val FOLDER = "RoundVideos"
    private const val INDEX = "index.json"

    private val lock = Any()

    /**
     * `files/RoundVideos`, not the cache: the system may reclaim the cache whenever it likes
     * and these cannot be regenerated.
     */
    fun folder(context: Context): File =
        File(context.filesDir, FOLDER).apply { if (!exists()) mkdirs() }

    fun fileFor(context: Context, item: RoundMediaItem) = File(folder(context), item.fileName)

    fun items(context: Context, sort: RoundMediaSort = RoundMediaSort.NEWEST): List<RoundMediaItem> {
        val all = synchronized(lock) { loadIndex(context) }
        // Rows whose file has gone — cleared by the system, or lost with a restore — are
        // dropped from what callers see rather than shown as entries that open nothing.
        return all
            .filter { fileFor(context, it).exists() }
            .sortedWith(comparatorFor(sort))
    }

    fun count(context: Context) = items(context).size

    /** Total bytes the saved clips take up — worth showing, since nothing deletes them. */
    fun totalBytes(context: Context) = items(context).sumOf { fileFor(context, it).length() }

    /**
     * Moves a finished recording into the library and records it.
     *
     * A move within the same volume is a rename, so this is cheap — and it is a move rather
     * than a copy so a failure cannot leave two copies of fifty megabytes.
     *
     * @param video where the writer left the file, which is the cache directory.
     * @return the item, or null if the file could not be taken over.
     */
    fun save(
        context: Context,
        video: File,
        teamName: String,
        explainer: String,
        guesser: String,
        correctCount: Int,
        skippedCount: Int,
        recordedAt: Long = System.currentTimeMillis(),
    ): RoundMediaItem? {
        val id = UUID.randomUUID().toString()
        val item = RoundMediaItem(
            id = id,
            teamName = teamName,
            explainer = explainer,
            guesser = guesser,
            correctCount = correctCount,
            skippedCount = skippedCount,
            recordedAt = recordedAt,
            fileName = "round_$id.mp4",
        )

        synchronized(lock) {
            val destination = File(folder(context), item.fileName)
            val moved = runCatching { video.renameTo(destination) }.getOrDefault(false) ||
                // A rename across volumes fails; copying is the fallback, not the plan.
                runCatching { video.copyTo(destination, overwrite = true); video.delete(); true }
                    .getOrDefault(false)
            if (!moved) {
                Log.w(TAG, "could not move the recording into the library")
                return null
            }
            writeIndex(context, loadIndex(context) + item)
        }
        return item
    }

    fun delete(context: Context, item: RoundMediaItem) {
        synchronized(lock) {
            runCatching { fileFor(context, item).delete() }
            writeIndex(context, loadIndex(context).filterNot { it.id == item.id })
        }
    }

    /** Ties break on date, so equal scores still come out in a stable, sensible order. */
    private fun comparatorFor(sort: RoundMediaSort): Comparator<RoundMediaItem> = when (sort) {
        RoundMediaSort.NEWEST -> compareByDescending { it.recordedAt }
        RoundMediaSort.OLDEST -> compareBy { it.recordedAt }
        RoundMediaSort.MOST_CORRECT ->
            compareByDescending<RoundMediaItem> { it.correctCount }.thenByDescending { it.recordedAt }
        RoundMediaSort.MOST_SKIPPED ->
            compareByDescending<RoundMediaItem> { it.skippedCount }.thenByDescending { it.recordedAt }
    }

    // MARK: - Index

    private fun indexFile(context: Context) = File(folder(context), INDEX)

    private fun loadIndex(context: Context): List<RoundMediaItem> {
        val file = indexFile(context)
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { index ->
                val row = array.getJSONObject(index)
                RoundMediaItem(
                    id = row.getString("id"),
                    teamName = row.getString("teamName"),
                    explainer = row.getString("explainer"),
                    guesser = row.getString("guesser"),
                    correctCount = row.getInt("correctCount"),
                    skippedCount = row.getInt("skippedCount"),
                    recordedAt = row.getLong("recordedAt"),
                    fileName = row.getString("fileName"),
                )
            }
        }.onFailure { Log.w(TAG, "index unreadable", it) }.getOrDefault(emptyList())
    }

    private fun writeIndex(context: Context, items: List<RoundMediaItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("teamName", item.teamName)
                    put("explainer", item.explainer)
                    put("guesser", item.guesser)
                    put("correctCount", item.correctCount)
                    put("skippedCount", item.skippedCount)
                    put("recordedAt", item.recordedAt)
                    put("fileName", item.fileName)
                },
            )
        }
        // Written beside and renamed over: a half-written index would lose every round, not
        // just the one being added.
        runCatching {
            val temporary = File(folder(context), "$INDEX.tmp")
            temporary.writeText(array.toString())
            temporary.renameTo(indexFile(context))
        }.onFailure { Log.w(TAG, "index could not be written", it) }
    }
}
