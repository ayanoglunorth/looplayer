package app.looplayer.data

import android.net.Uri
import app.looplayer.subtitle.Slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SiblingSubtitlesTest {

    private val app get() = RuntimeEnvironment.getApplication()

    @Test
    fun `finds subtitles that share the video name and ignores other files`() {
        val dir = File(app.cacheDir, "siblings").apply { deleteRecursively(); mkdirs() }
        val video = File(dir, "Film.mkv").apply { writeText("x") }
        listOf("Film.en.srt", "Film.tr.srt", "Film.de.srt", "Other.srt", "Film.txt").forEach { File(dir, it).writeText("x") }

        val found = SiblingSubtitles.find(app, Uri.fromFile(video)).map { it.name }

        assertEquals(2, found.size)
        assertTrue(found.all { it.startsWith("Film.") && it.endsWith(".srt") })
    }

    @Test
    fun `a session with no subtitles of its own attaches the ones beside the video`() = runBlocking {
        app.getSharedPreferences("library", 0).edit().clear().commit()
        val dir = File(app.cacheDir, "siblings-session").apply { deleteRecursively(); mkdirs() }
        val video = File(dir, "Film.mkv").apply { writeText("x") }
        File(dir, "Film.tr.srt").writeText("1\n00:00:01,000 --> 00:00:03,000\nMerhaba\n")
        File(dir, "Film.en.srt").writeText("1\n00:00:01,000 --> 00:00:03,000\nHello\n")

        val session = SubtitleSession(
            app.contentResolver, LibraryStore(app), VideoEntry(uri = Uri.fromFile(video).toString(), title = "Film"),
            discover = { SiblingSubtitles.find(app, Uri.fromFile(video)).map(Uri::fromFile) },
        )
        session.restore()

        assertEquals(setOf(Slot.A, Slot.B), session.tracks.keys)
    }
}
