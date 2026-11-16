package app.looplayer.data

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.MediaExtractorCompat
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.text.CueDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.Locale

/**
 * Pulls a subtitle stream out of a video container (mkv, mp4...) and saves it as an ordinary .srt,
 * so it can fill a language slot exactly like a subtitle file the user attached.
 */
@UnstableApi
object EmbeddedSubtitles {

    /**
     * Reads the whole stream, so it takes as long as the file takes to scan. [trackId] is the container's
     * track id (the player's `Format.id`); null or unmatched falls back to the first text stream.
     * Returns null when the stream holds no text cues (e.g. picture-based subtitles).
     */
    suspend fun extractToFile(
        context: Context,
        video: Uri,
        trackId: String?,
        language: String?,
        outDir: File,
        dataSource: DataSource.Factory = DefaultDataSource.Factory(context),
    ): File? = withContext(Dispatchers.IO) {
        val extractor = MediaExtractorCompat(DefaultExtractorsFactory(), dataSource)
        try {
            extractor.setDataSource(video, 0)
            val texts = (0 until extractor.trackCount).filter {
                extractor.getTrackFormat(it).getString("mime") == CUES_MIME
            }
            val track = texts.firstOrNull { extractor.getTrackFormat(it).getInteger("track-id", -1).toString() == trackId }
                ?: texts.firstOrNull() ?: return@withContext null
            extractor.selectTrack(track)

            val decoder = CueDecoder()
            val srt = StringBuilder()
            var count = 0
            fun add(startUs: Long, endUs: Long, text: String) {
                count++
                srt.append(count).append('\n')
                    .append(stamp(startUs)).append(" --> ").append(stamp(endUs)).append('\n')
                    .append(text).append("\n\n")
            }
            // mp4 text samples carry no duration: a cue lasts until the next sample (an empty one marks a gap).
            var open: Pair<Long, String>? = null
            var buffer = ByteBuffer.allocate(64 * 1024)
            while (true) {
                val size = extractor.sampleSize.toInt()
                if (size < 0) break
                if (size > buffer.capacity()) buffer = ByteBuffer.allocate(size)
                buffer.clear()
                val read = extractor.readSampleData(buffer, 0)
                if (read < 0) break
                val bytes = ByteArray(read).also { buffer.rewind(); buffer.get(it) }
                val start = extractor.sampleTime
                val timed = decoder.decode(start, bytes, 0, bytes.size)
                open?.let { (from, text) -> add(from, start, text); open = null }
                val text = timed.cues.mapNotNull { it.text?.toString()?.trim()?.takeIf(String::isNotEmpty) }.joinToString("\n")
                if (text.isNotEmpty()) {
                    if (timed.durationUs != C.TIME_UNSET) add(start, timed.endTimeUs, text) else open = start to text
                }
                extractor.advance()
            }
            open?.let { (from, text) -> add(from, from + LAST_CUE_US, text) }
            if (count == 0) return@withContext null

            outDir.mkdirs()
            val name = listOfNotNull("v${video.toString().hashCode().toUInt().toString(16)}", trackId, language).joinToString(".") + ".srt"
            File(outDir, name).apply { writeText(srt.toString()) }
        } finally {
            extractor.release()
        }
    }

    private const val CUES_MIME = "application/x-media3-cues"
    private const val LAST_CUE_US = 3_000_000L

    private fun stamp(us: Long): String {
        val ms = us.coerceAtLeast(0) / 1000
        return String.format(Locale.ROOT, "%02d:%02d:%02d,%03d", ms / 3_600_000, ms / 60_000 % 60, ms / 1000 % 60, ms % 1000)
    }
}
