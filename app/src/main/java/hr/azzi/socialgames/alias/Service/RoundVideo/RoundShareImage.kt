package hr.azzi.socialgames.alias.Service.RoundVideo

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Drawing and sharing the round-result card.
 *
 * The file is rewritten on every share rather than kept per round: the sheet reads it while
 * the chooser is up and nothing needs it afterwards, so one name in the cache is enough.
 */
private const val SHARE_DIR = "share"
const val ROUND_SHARE_FILE = "round-result.png"

/**
 * Renders the card and writes it out. Off the main thread — this is a 1080pt-wide bitmap.
 *
 * [fileName] is what the share sheet names the attachment, so it is worth keeping readable.
 */
suspend fun writeRoundShareImage(
    context: Context,
    info: RoundCardInfo,
    header: RoundShareHeader = RoundShareHeader.Team,
    fileName: String = ROUND_SHARE_FILE,
): File? =
    withContext(Dispatchers.Default) {
        runCatching {
            val bitmap = RoundShareCardRenderer(context, info, header).render()
            val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
            val file = File(dir, fileName)
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            file
        }.onFailure { Log.w("RoundShare", "cannot draw the round card", it) }.getOrNull()
    }

/** Hands the rendered card to the share sheet. */
fun shareRoundImage(context: Context, file: File) {
    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.onFailure {
        // A path the provider has not been told about throws here, and swallowing it is how
        // a share button ends up doing nothing at all.
        Log.w("RoundShare", "cannot share ${file.path}", it)
    }.getOrNull() ?: return

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        // The extra grants the app the user picks; the clip data grants the share sheet
        // itself, which is a separate app and is what draws the preview thumbnail.
        clipData = ClipData.newUri(context.contentResolver, "image", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
