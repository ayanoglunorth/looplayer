package app.looplayer.data

import android.net.Uri
import app.looplayer.subtitle.Slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.nio.charset.Charset

/** The real data path under the player: files on disk → parsed tracks → switching → saved library. */
@RunWith(RobolectricTestRunner::class)
class SubtitleSessionTest {

    private val app get() = RuntimeEnvironment.getApplication()
    private lateinit var dir: File

    @Before
    fun setUp() {
        dir = File(app.cacheDir, "session-test").apply { deleteRecursively(); mkdirs() }
        app.getSharedPreferences("library", 0).edit().clear().commit()
    }

    private fun file(name: String, text: String, charset: Charset = Charsets.UTF_8) =
        Uri.fromFile(File(dir, name).apply { writeBytes(text.toByteArray(charset)) })

    private val trSrt = "1\n00:00:01,000 --> 00:00:03,000\nAğaçların gölgesinde\n"
    private val enSrt = "1\n00:00:01,000 --> 00:00:03,000\nIn the shade of the trees\n"

    private fun newSession(store: LibraryStore = LibraryStore(app)) =
        SubtitleSession(app.contentResolver, store, VideoEntry(uri = "content://video/1", title = "Film"))

    @Test
    fun `two loaded subtitles switch, and switching back restores the first`() = runBlocking {
        val s = newSession()
        assertEquals(AttachResult.OK, s.attach(Slot.A, file("film.tr.srt", trSrt, Charset.forName("windows-1254"))))
        assertEquals(AttachResult.OK, s.attach(Slot.B, file("film.en.srt", enSrt)))

        assertEquals("tr", s.entry.subA?.language)
        assertEquals("en", s.entry.subB?.language)
        assertEquals("Ağaçların gölgesinde", s.dual.textAt(2_000))

        assertTrue(s.switch())
        assertEquals("In the shade of the trees", s.dual.textAt(2_000))
        assertTrue(s.switch())
        assertEquals("Ağaçların gölgesinde", s.dual.textAt(2_000))
    }

    @Test
    fun `with one subtitle, switching reports there is nothing to switch to`() = runBlocking {
        val s = newSession()
        s.attach(Slot.A, file("film.tr.srt", trSrt))
        assertFalse(s.switch())
        assertEquals("Ağaçların gölgesinde", s.dual.textAt(2_000))
    }

    @Test
    fun `sync offset shifts the active subtitle in time`() = runBlocking {
        val s = newSession()
        s.attach(Slot.A, file("film.tr.srt", trSrt))
        s.shift(Slot.A, 1_000)
        assertNull(s.dual.textAt(1_500))
        assertEquals("Ağaçların gölgesinde", s.dual.textAt(2_500))
        assertEquals(1_000L, s.entry.subA?.offsetMs)
    }

    @Test
    fun `removing the speaking subtitle hands over to the other one`() = runBlocking {
        val s = newSession()
        s.attach(Slot.A, file("film.tr.srt", trSrt))
        s.attach(Slot.B, file("film.en.srt", enSrt))
        s.remove(Slot.A)
        assertEquals(Slot.B, s.dual.active)
        assertEquals("In the shade of the trees", s.dual.textAt(2_000))
    }

    @Test
    fun `a file with no timed lines and an unreadable file are reported, not attached`() = runBlocking {
        val s = newSession()
        assertEquals(AttachResult.EMPTY, s.attach(Slot.A, file("notes.srt", "just some text\n")))
        assertEquals(AttachResult.FAILED, s.attach(Slot.A, Uri.fromFile(File(dir, "missing.srt"))))
        assertNull(s.entry.subA)
    }

    @Test
    fun `subtitles, active language and position survive a restart`() = runBlocking {
        val first = newSession()
        first.attach(Slot.A, file("film.tr.srt", trSrt))
        first.attach(Slot.B, file("film.en.srt", enSrt))
        first.switch()
        first.savePosition(42_000, 600_000)

        // A fresh store reads what the first one wrote, as after the app is restarted.
        val store = LibraryStore(app)
        val saved = store.find("content://video/1")!!
        assertEquals(42_000L, saved.positionMs)
        assertEquals(Slot.B, saved.activeSlot)

        val again = SubtitleSession(app.contentResolver, store, saved)
        again.restore()
        assertEquals("In the shade of the trees", again.dual.textAt(2_000))
        assertEquals(42_000L, again.entry.resumeMs)
    }

    @Test
    fun `a temporary open-with session never writes to the library`() = runBlocking {
        val store = LibraryStore(app)
        val s = SubtitleSession(app.contentResolver, store, VideoEntry(uri = "content://tmp/9", title = "Tmp"), persist = false)
        s.attach(Slot.A, file("film.tr.srt", trSrt))
        s.savePosition(10_000, 60_000)
        assertNull(LibraryStore(app).find("content://tmp/9"))
    }
}
