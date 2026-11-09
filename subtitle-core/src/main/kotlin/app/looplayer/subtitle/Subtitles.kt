package app.looplayer.subtitle

/** One timed line of subtitle text. Times are in milliseconds. */
data class Cue(val startMs: Long, val endMs: Long, val text: String)

enum class SubtitleFormat {
    SRT, VTT, ASS;

    companion object {
        fun fromFileName(name: String): SubtitleFormat? =
            when (name.substringAfterLast('.', "").lowercase()) {
                "srt" -> SRT
                "vtt" -> VTT
                "ass", "ssa" -> ASS
                else -> null
            }

        /** Best guess from the text itself; SRT is the fallback. */
        fun sniff(text: String): SubtitleFormat {
            val head = text.trimStart('﻿', ' ', '\n', '\r', '\t').take(64)
            return when {
                head.startsWith("WEBVTT") -> VTT
                head.startsWith("[Script Info]", ignoreCase = true) -> ASS
                else -> SRT
            }
        }
    }
}

/** A loaded subtitle file. [offsetMs] shifts every cue later (positive) or earlier (negative). */
data class SubtitleTrack(
    val label: String,
    val language: String?,
    val cues: List<Cue>,
    val offsetMs: Long = 0,
) {
    private val sorted: List<Cue> by lazy { cues.sortedBy { it.startMs } }

    /** maxEndUpTo[i] = latest end among cues 0..i, so long cues under many short ones are never missed. */
    private val maxEndUpTo: LongArray by lazy {
        var max = Long.MIN_VALUE
        LongArray(sorted.size) { i -> max = maxOf(max, sorted[i].endMs); max }
    }

    /** Text of every cue showing at [positionMs], or null when nothing is on screen. */
    fun textAt(positionMs: Long): String? {
        val t = positionMs - offsetMs
        // Last cue that has started by t; everything active starts at or before it.
        var lo = 0
        var hi = sorted.size - 1
        var last = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (sorted[mid].startMs <= t) {
                last = mid; lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        if (last < 0) return null
        val active = ArrayList<String>(2)
        // Walk back only while some earlier cue could still be running (its prefix-max end is past t).
        var i = last
        while (i >= 0 && maxEndUpTo[i] > t) {
            val cue = sorted[i]
            if (t < cue.endMs) active.add(cue.text)
            i--
        }
        return if (active.isEmpty()) null else active.asReversed().joinToString("\n")
    }
}

enum class Slot {
    A, B;

    val other: Slot get() = if (this == A) B else A
}

/** Two subtitle slots and which one is speaking. */
data class DualSubtitles(
    val a: SubtitleTrack? = null,
    val b: SubtitleTrack? = null,
    val active: Slot = Slot.A,
    val visible: Boolean = true,
) {
    operator fun get(slot: Slot): SubtitleTrack? = if (slot == Slot.A) a else b

    val activeTrack: SubtitleTrack? get() = this[active]

    val canSwitch: Boolean get() = a != null && b != null

    fun withTrack(slot: Slot, track: SubtitleTrack?): DualSubtitles {
        val next = if (slot == Slot.A) copy(a = track) else copy(b = track)
        return when {
            next[next.active] != null -> next
            next[next.active.other] != null -> next.copy(active = next.active.other)
            else -> next.copy(active = Slot.A)
        }
    }

    fun switched(): DualSubtitles = if (canSwitch) copy(active = active.other) else this

    fun textAt(positionMs: Long): String? = if (visible) activeTrack?.textAt(positionMs) else null
}
