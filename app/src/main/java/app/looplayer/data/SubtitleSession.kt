package app.looplayer.data

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.looplayer.subtitle.DualSubtitles
import app.looplayer.subtitle.Slot
import app.looplayer.subtitle.SubtitleTrack

enum class AttachResult { OK, EMPTY, FAILED }

/**
 * The open video and its two subtitle editions, shared by the setup sheet and the player
 * so subtitles are parsed once. Every change is written back to the library.
 */
class SubtitleSession(
    private val resolver: ContentResolver,
    private val store: LibraryStore,
    initial: VideoEntry,
) {
    var entry by mutableStateOf(initial)
        private set
    var tracks by mutableStateOf(emptyMap<Slot, SubtitleTrack>())
        private set
    var loading by mutableStateOf(emptySet<Slot>())
        private set
    var visible by mutableStateOf(true)

    val dual: DualSubtitles
        get() = DualSubtitles(tracks[Slot.A], tracks[Slot.B], entry.activeSlot, visible)

    /** Re-reads subtitles saved with the entry; a file that vanished is quietly detached. */
    suspend fun restore() {
        for (slot in Slot.entries) {
            val ref = entry.sub(slot) ?: continue
            if (tracks[slot] != null) continue
            if (load(slot, ref) != AttachResult.OK) update(entry.withSub(slot, null))
        }
        normalizeActive()
    }

    suspend fun attach(slot: Slot, uri: Uri): AttachResult {
        Files.takePermission(resolver, uri)
        val ref = Files.subtitleRef(resolver, uri)
        val result = load(slot, ref)
        if (result == AttachResult.OK) {
            // A freshly attached edition only takes focus when nothing else is speaking.
            val before = dual
            val after = DualSubtitles(before.a, before.b, before.active).withTrack(slot, tracks[slot])
            update(entry.withSub(slot, ref).copy(activeSlot = after.active))
        }
        return result
    }

    fun remove(slot: Slot) {
        tracks = tracks - slot
        update(entry.withSub(slot, null))
        normalizeActive()
    }

    fun shift(slot: Slot, deltaMs: Long) {
        val ref = entry.sub(slot) ?: return
        val track = tracks[slot] ?: return
        val offset = (ref.offsetMs + deltaMs).coerceIn(-60_000, 60_000)
        tracks = tracks + (slot to track.copy(offsetMs = offset))
        update(entry.withSub(slot, ref.copy(offsetMs = offset)))
    }

    /** Flips to the other language. Returns false when there is no second language to flip to. */
    fun switch(): Boolean {
        val d = dual
        if (!d.canSwitch) return false
        update(entry.copy(activeSlot = d.active.other))
        return true
    }

    fun select(slot: Slot) {
        if (tracks[slot] != null) update(entry.copy(activeSlot = slot))
    }

    fun savePosition(positionMs: Long, durationMs: Long) {
        update(entry.copy(positionMs = positionMs, durationMs = if (durationMs > 0) durationMs else entry.durationMs))
    }

    fun touch() = update(entry.copy(lastOpened = System.currentTimeMillis()))

    private suspend fun load(slot: Slot, ref: SubtitleRef): AttachResult {
        loading = loading + slot
        return try {
            tracks = tracks + (slot to Files.loadTrack(resolver, ref))
            AttachResult.OK
        } catch (_: EmptySubtitleException) {
            AttachResult.EMPTY
        } catch (_: Exception) {
            AttachResult.FAILED
        } finally {
            loading = loading - slot
        }
    }

    private fun normalizeActive() {
        val d = dual
        val fixed = when {
            d[d.active] != null -> d.active
            d[d.active.other] != null -> d.active.other
            else -> d.active
        }
        if (fixed != entry.activeSlot) update(entry.copy(activeSlot = fixed))
    }

    private fun update(next: VideoEntry) {
        entry = next
        store.upsert(next)
    }
}
