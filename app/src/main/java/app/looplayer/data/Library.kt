package app.looplayer.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.LruCache
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import app.looplayer.subtitle.LanguageGuess
import app.looplayer.subtitle.Slot
import app.looplayer.subtitle.SubtitleDecoder
import app.looplayer.subtitle.SubtitleFormat
import app.looplayer.subtitle.SubtitleParser
import app.looplayer.subtitle.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** A subtitle file attached to a video slot. */
data class SubtitleRef(val uri: String, val fileName: String, val language: String?, val offsetMs: Long = 0)

/** One video the user has opened, with the two subtitle editions attached to it. */
data class VideoEntry(
    val uri: String,
    val title: String,
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    val lastOpened: Long = System.currentTimeMillis(),
    val subA: SubtitleRef? = null,
    val subB: SubtitleRef? = null,
    val activeSlot: Slot = Slot.A,
) {
    fun sub(slot: Slot) = if (slot == Slot.A) subA else subB
    fun withSub(slot: Slot, ref: SubtitleRef?) = if (slot == Slot.A) copy(subA = ref) else copy(subB = ref)

    /** 0..1 progress, or null when the duration is unknown. */
    val progress: Float?
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else null

    /** Where to start: the saved spot, unless it is too close to either end to be useful. */
    val resumeMs: Long
        get() = if (positionMs > 5_000 && (durationMs == 0L || positionMs < durationMs - 10_000)) positionMs else 0
}

/** Recently opened videos, persisted as JSON in SharedPreferences. Small, local, no database needed. */
class LibraryStore(context: Context) {
    private val prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)
    val entries: SnapshotStateList<VideoEntry> = mutableStateListOf<VideoEntry>().apply { addAll(load()) }

    fun find(uri: String): VideoEntry? = entries.firstOrNull { it.uri == uri }

    fun upsert(entry: VideoEntry) {
        val i = entries.indexOfFirst { it.uri == entry.uri }
        if (i >= 0) entries[i] = entry else entries.add(0, entry)
        entries.sortByDescending { it.lastOpened }
        save()
    }

    fun remove(uri: String) {
        entries.removeAll { it.uri == uri }
        save()
    }

    private fun save() {
        val arr = JSONArray()
        entries.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun load(): List<VideoEntry> = runCatching {
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        (0 until arr.length()).map { arr.getJSONObject(it).toEntry() }.sortedByDescending { it.lastOpened }
    }.getOrDefault(emptyList())

    private fun VideoEntry.toJson() = JSONObject().apply {
        put("uri", uri); put("title", title); put("duration", durationMs); put("position", positionMs)
        put("opened", lastOpened); put("active", activeSlot.name)
        subA?.let { put("subA", it.toJson()) }
        subB?.let { put("subB", it.toJson()) }
    }

    private fun SubtitleRef.toJson() = JSONObject().apply {
        put("uri", uri); put("name", fileName); put("offset", offsetMs)
        language?.let { put("lang", it) }
    }

    private fun JSONObject.toEntry() = VideoEntry(
        uri = getString("uri"),
        title = getString("title"),
        durationMs = optLong("duration"),
        positionMs = optLong("position"),
        lastOpened = optLong("opened"),
        subA = optJSONObject("subA")?.toRef(),
        subB = optJSONObject("subB")?.toRef(),
        activeSlot = runCatching { Slot.valueOf(optString("active", "A")) }.getOrDefault(Slot.A),
    )

    private fun JSONObject.toRef() = SubtitleRef(
        uri = getString("uri"),
        fileName = getString("name"),
        language = if (has("lang")) getString("lang") else null,
        offsetMs = optLong("offset"),
    )

    companion object {
        private const val KEY = "entries"

        @Volatile private var instance: LibraryStore? = null

        /** One shared instance, so downloads that finish in the background show up in the open library. */
        fun get(context: Context): LibraryStore =
            instance ?: synchronized(this) { instance ?: LibraryStore(context.applicationContext).also { instance = it } }
    }
}

/** File access through the Storage Access Framework: names, persisted grants, subtitle reading. */
object Files {

    /** Keeps read access across restarts; false when the grant was only temporary (e.g. "Open with"). */
    fun takePermission(resolver: ContentResolver, uri: Uri): Boolean =
        runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }.isSuccess

    /** Gives back persisted grants for a removed entry; Android caps how many an app may hold. */
    fun releasePermissions(resolver: ContentResolver, entry: VideoEntry) {
        listOfNotNull(entry.uri, entry.subA?.uri, entry.subB?.uri).forEach { u ->
            runCatching { resolver.releasePersistableUriPermission(Uri.parse(u), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }

    fun displayName(resolver: ContentResolver, uri: Uri): String {
        val fromProvider = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
        return fromProvider ?: (uri.lastPathSegment ?: uri.toString()).substringAfterLast('/').substringAfterLast('\\')
    }

    fun titleFrom(fileName: String) = fileName.substringBeforeLast('.').replace('.', ' ').replace('_', ' ').trim()

    fun subtitleRef(resolver: ContentResolver, uri: Uri): SubtitleRef {
        val name = displayName(resolver, uri)
        return SubtitleRef(uri.toString(), name, LanguageGuess.fromFileName(name))
    }

    /** Reads and parses a subtitle; throws when the file can't be read or holds no cues. */
    suspend fun loadTrack(resolver: ContentResolver, ref: SubtitleRef): SubtitleTrack = withContext(Dispatchers.IO) {
        val bytes = resolver.openInputStream(Uri.parse(ref.uri))?.use { it.readBytes() }
            ?: error("unreadable")
        val text = SubtitleDecoder.decode(bytes)
        val format = SubtitleFormat.fromFileName(ref.fileName) ?: SubtitleFormat.sniff(text)
        val cues = SubtitleParser.parse(text, format)
        if (cues.isEmpty()) throw EmptySubtitleException()
        SubtitleTrack(label = languageName(ref.language) ?: titleFrom(ref.fileName), language = ref.language, cues = cues, offsetMs = ref.offsetMs)
    }

    fun canRead(resolver: ContentResolver, uri: String): Boolean =
        runCatching { resolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false }.getOrDefault(false)

    private val thumbs = LruCache<String, Bitmap>(64)

    suspend fun thumbnail(context: Context, uri: String, atMs: Long): Bitmap? = withContext(Dispatchers.IO) {
        thumbs.get(uri)?.let { return@withContext it }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(uri))
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            val at = if (atMs > 0) atMs else duration / 10
            val frame = if (Build.VERSION.SDK_INT >= 27) {
                retriever.getScaledFrameAtTime(at * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 480, 270)
            } else {
                retriever.getFrameAtTime(at * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } ?: retriever.frameAtTime
            frame?.also { thumbs.put(uri, it) }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}

class EmptySubtitleException : Exception()

/** "tr" → "Türkçe" in the device language. */
fun languageName(code: String?): String? = code?.let {
    Locale.forLanguageTag(it).getDisplayLanguage(Locale.getDefault())
        .replaceFirstChar { c -> c.titlecase(Locale.getDefault()) }
        .takeIf { name -> name.isNotBlank() && name != it }
}

/** Tab mark for a slot: the language code, or the slot number when the language is unknown. */
fun tabMark(ref: SubtitleRef?, slot: Slot): String = ref?.language?.uppercase() ?: if (slot == Slot.A) "1" else "2"
