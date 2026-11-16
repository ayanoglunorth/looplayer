package app.looplayer.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import app.looplayer.subtitle.LanguageGuess
import app.looplayer.subtitle.SubtitleFormat
import java.io.File

/**
 * Finds subtitle files that sit beside the video ("Film.mkv" → "Film.en.srt", "Film.tr.srt").
 * Needs real file access, so it works for the app's own downloads always and for everything
 * else once "All files access" is granted; without it the scan simply finds nothing.
 */
object SiblingSubtitles {

    /** True when the app may read arbitrary folders (or the OS predates scoped storage). */
    fun hasFolderAccess(): Boolean = Build.VERSION.SDK_INT < 30 || runCatching { Environment.isExternalStorageManager() }.getOrDefault(false)

    /** Up to two subtitle files next to the video, known languages first. */
    fun find(context: Context, videoUri: Uri): List<File> {
        val video = videoFile(context, videoUri) ?: return emptyList()
        val base = video.nameWithoutExtension
        val found = video.parentFile?.listFiles { f ->
            f.isFile && SubtitleFormat.fromFileName(f.name) != null && f.name.startsWith(base, ignoreCase = true)
        }.orEmpty()
        return found
            .sortedWith(compareBy({ LanguageGuess.fromFileName(it.name) == null }, { it.name.lowercase() }))
            .distinctBy { LanguageGuess.fromFileName(it.name) ?: it.name }
            .take(2)
    }

    private fun videoFile(context: Context, uri: Uri): File? = runCatching {
        when {
            uri.scheme == "file" -> File(requireNotNull(uri.path))
            uri.scheme != "content" -> null
            DocumentsContract.isDocumentUri(context, uri) -> fromDocumentId(DocumentsContract.getDocumentId(uri))
            else -> context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0)?.let(::File) else null
            }
        }
    }.getOrNull()?.takeIf { it.exists() }

    // "primary:Movies/a.mkv" → /storage/emulated/0/Movies/a.mkv; "raw:/storage/..." is already a path.
    private fun fromDocumentId(id: String): File? {
        val (volume, path) = id.split(':', limit = 2).takeIf { it.size == 2 } ?: return null
        return when (volume) {
            "primary" -> File(Environment.getExternalStorageDirectory(), path)
            "raw" -> File(path)
            else -> File("/storage/$volume/$path")
        }
    }
}
