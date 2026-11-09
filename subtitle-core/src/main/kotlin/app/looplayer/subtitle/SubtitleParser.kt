package app.looplayer.subtitle

object SubtitleParser {

    fun parse(text: String, format: SubtitleFormat = SubtitleFormat.sniff(text)): List<Cue> {
        val normalized = text.removePrefix("﻿").replace("\r\n", "\n").replace('\r', '\n')
        val cues = when (format) {
            SubtitleFormat.SRT, SubtitleFormat.VTT -> parseTimedBlocks(normalized)
            SubtitleFormat.ASS -> parseAss(normalized)
        }
        return cues.filter { it.text.isNotBlank() && it.endMs > it.startMs }.sortedBy { it.startMs }
    }

    // SRT and WebVTT share the "start --> end" line followed by text lines up to a blank line.
    private val arrow = Regex("""^\s*(\S+)\s*-->\s*(\S+)""")

    private fun parseTimedBlocks(text: String): List<Cue> {
        val lines = text.split('\n')
        val out = ArrayList<Cue>()
        var i = 0
        while (i < lines.size) {
            val match = arrow.find(lines[i])
            val start = match?.let { parseClock(it.groupValues[1]) }
            val end = match?.let { parseClock(it.groupValues[2]) }
            if (start == null || end == null) {
                i++; continue
            }
            i++
            val body = StringBuilder()
            // A cue ends at a blank line, or where a sloppy file starts the next cue without one.
            while (i < lines.size && lines[i].isNotBlank() && !startsCue(lines, i)) {
                if (body.isNotEmpty()) body.append('\n')
                body.append(lines[i].trim())
                i++
            }
            out += Cue(start, end, cleanMarkup(body.toString()))
        }
        return out
    }

    private fun startsCue(lines: List<String>, i: Int): Boolean =
        arrow.containsMatchIn(lines[i]) ||
            (lines[i].trim().all { it.isDigit() } && i + 1 < lines.size && arrow.containsMatchIn(lines[i + 1]))

    /** Accepts hh:mm:ss,mmm / hh:mm:ss.mmm / mm:ss.mmm (VTT short form). */
    private fun parseClock(raw: String): Long? {
        val parts = raw.replace(',', '.').split(':')
        if (parts.size !in 2..3) return null
        val secParts = parts.last().split('.')
        val h = if (parts.size == 3) parts[0].toLongOrNull() ?: return null else 0
        val m = parts[parts.size - 2].toLongOrNull() ?: return null
        val s = secParts[0].toLongOrNull() ?: return null
        val frac = secParts.getOrNull(1)?.takeIf { it.isNotEmpty() }
        val ms = frac?.let { (it + "00").take(3).toLongOrNull() ?: return null } ?: 0
        return ((h * 60 + m) * 60 + s) * 1000 + ms
    }

    // Only markup subtitle formats actually use: i/b/u/s, font, ruby, and WebVTT class, voice, lang and timestamp tags.
    private val tag = Regex("""</?(?:[ibus]|font|ruby|rt|c|v|lang)(?:[.\s][^>]*)?>|<\d{1,2}:\d{2}(?::\d{2})?\.\d{3}>""", RegexOption.IGNORE_CASE)
    private val assOverride = Regex("""\{[^}]*\}""")

    private fun cleanMarkup(s: String): String =
        tag.replace(assOverride.replace(s, ""), "")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&nbsp;", " ").replace("&quot;", "\"")
            .lines().joinToString("\n") { it.trim() }
            .trim()

    private fun parseAss(text: String): List<Cue> {
        var fields = listOf("Layer", "Start", "End", "Style", "Name", "MarginL", "MarginR", "MarginV", "Effect", "Text")
        var inEvents = false
        val out = ArrayList<Cue>()
        for (line in text.split('\n')) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[")) {
                inEvents = trimmed.equals("[Events]", ignoreCase = true)
                continue
            }
            if (!inEvents) continue
            when {
                trimmed.startsWith("Format:", ignoreCase = true) ->
                    fields = trimmed.substringAfter(':').split(',').map { it.trim() }
                trimmed.startsWith("Dialogue:", ignoreCase = true) -> {
                    // Text is the last field and may itself contain commas.
                    val values = trimmed.substringAfter(':').trimStart().split(',', limit = fields.size)
                    val startAt = fields.indexOf("Start")
                    val endAt = fields.indexOf("End")
                    val textAt = fields.indexOf("Text")
                    if (values.size < fields.size || startAt < 0 || endAt < 0 || textAt < 0) continue
                    val start = parseAssClock(values[startAt]) ?: continue
                    val end = parseAssClock(values[endAt]) ?: continue
                    val raw = values[textAt]
                        .replace("\\N", "\n").replace("\\n", "\n").replace("\\h", " ")
                    out += Cue(start, end, cleanMarkup(raw))
                }
            }
        }
        return out
    }

    /** h:mm:ss.cc (centiseconds). */
    private fun parseAssClock(raw: String): Long? = parseClock(raw.trim())
}
