package app.looplayer.data

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.FileDataSource
import app.looplayer.subtitle.LanguageGuess
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/** Real containers made with ffmpeg: two soft subtitle streams (tur, eng) beside a tiny H.264 video. */
@UnstableApi
@RunWith(RobolectricTestRunner::class)
class EmbeddedSubtitlesTest {
    private val app get() = RuntimeEnvironment.getApplication()

    private fun extract(resource: String, trackId: String?, lang: String?): File? = runBlocking {
        val bytes = this@EmbeddedSubtitlesTest.javaClass.classLoader!!.getResourceAsStream(resource)!!.readBytes()
        val video = File(app.cacheDir, resource).apply { writeBytes(bytes) }
        EmbeddedSubtitles.extractToFile(
            app, Uri.parse(video.toURI().toString()), trackId, lang, File(app.cacheDir, "out-$resource"), FileDataSource.Factory(),
        )
    }

    @Test
    fun `mkv streams come out as timed srt, picked by track id`() {
        val tr = extract("two.mkv", "2", "tr")
        val en = extract("two.mkv", "3", "en")

        assertNotNull(tr); assertNotNull(en)
        assertTrue(tr!!.readText().contains("00:00:00,500 --> 00:00:01,500\nMerhaba dunya"))
        assertTrue(en!!.readText().contains("00:00:02,000 --> 00:00:03,200\nSecond line"))
        assertEquals("tr", LanguageGuess.fromFileName(tr.name))
    }

    @Test
    fun `mp4 text streams work too`() {
        val en = extract("two.mp4", "3", "en")

        assertNotNull(en)
        assertTrue(en!!.readText().contains("Hello world"))
    }
}
