package app.looplayer.ui

import android.net.Uri
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.media3.test.utils.robolectric.ShadowMediaCodecConfig
import app.looplayer.data.LibraryStore
import app.looplayer.data.SubtitleSession
import app.looplayer.data.VideoEntry
import app.looplayer.subtitle.Slot
import app.looplayer.ui.player.PlayerScreen
import app.looplayer.ui.theme.LooplayerTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.io.File

/**
 * The whole player screen playing a real H.264 file (generated with ffmpeg, 12 s, 320x180)
 * through ExoPlayer and Robolectric's shadow codecs: playback advances, subtitles follow the clock,
 * a double-tap swaps the language on screen, and a sideways drag moves the playhead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w851dp-h393dp-land")
class PlayerPlaybackTest {

    @get:Rule val rule = createComposeRule()
    @get:Rule val codecs: ShadowMediaCodecConfig = ShadowMediaCodecConfig.withAllDefaultSupportedCodecs()

    private val app get() = RuntimeEnvironment.getApplication()

    private fun copyResource(name: String): File =
        File(app.cacheDir, name).apply { PlayerPlaybackTest::class.java.classLoader!!.getResourceAsStream(name)!!.use { input -> outputStream().use { input.copyTo(it) } } }

    private fun srt(name: String, early: String, late: String) = Uri.fromFile(
        File(app.cacheDir, name).apply {
            writeText("1\n00:00:01,000 --> 00:00:05,000\n$early\n\n2\n00:00:08,000 --> 00:00:11,500\n$late\n")
        },
    )

    private fun screenTexts(): String = rule.onAllNodes(androidx.compose.ui.test.hasText("", substring = true), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .flatMap { node ->
            node.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.Text) { null }
                .orEmpty().map { t -> t.text }
        }
        .distinct().joinToString(" | ")

    private fun shown(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    /** Lets real playback time pass: Robolectric's clock moves only when the main looper is idled forward. */
    private fun playUntil(limitMs: Long, condition: () -> Boolean): Boolean {
        var waited = 0L
        while (waited < limitMs) {
            ShadowLooper.idleMainLooper(50, java.util.concurrent.TimeUnit.MILLISECONDS)
            rule.mainClock.advanceTimeBy(50)
            if (condition()) return true
            waited += 50
        }
        return condition()
    }

    @Test
    fun `real video plays, double tap swaps the language on screen, drag seeks`() {
        val video = copyResource("sample.mp4")
        val session = SubtitleSession(
            app.contentResolver, LibraryStore(app), VideoEntry(uri = Uri.fromFile(video).toString(), title = "Sample"),
        )
        runBlocking {
            session.attach(Slot.A, srt("sample.tr.srt", "Merhaba", "Sonra görüşürüz"))
            session.attach(Slot.B, srt("sample.en.srt", "Hello", "See you later"))
        }
        rule.mainClock.autoAdvance = false
        rule.setContent { LooplayerTheme { PlayerScreen(session, onPickSubtitle = {}, onExit = {}) } }

        // Playback must actually advance past 1 s for the first Turkish line to appear.
        assertTrue("first Turkish line never appeared: playback did not advance", playUntil(15_000) { shown("Merhaba") })

        // Hide the controls first so the double-tap lands on the bare picture, as when watching.
        playUntil(4_000) { !shown("Sample") }
        rule.onRoot().performTouchInput { doubleClick(center) }
        assertTrue("double tap did not switch to English", playUntil(2_000) { shown("Hello") && !shown("Merhaba") })

        // Playback is now around 5 s. On a 12 s video a full-width drag covers the whole 12 s, so a
        // drag across a quarter of the width jumps about +3 s: into the later cue (8-11.5 s).
        val log = StringBuilder()
        fun watch(limit: Long, condition: () -> Boolean) = playUntil(limit) {
            log.append(screenTexts()).append('\n')
            condition()
        }
        fun screenLog() = "\nscreen over time:\n" + log.lines().distinct().joinToString("\n")

        rule.onRoot().performTouchInput { swipeRight(startX = width * 0.30f, endX = width * 0.55f) }
        assertTrue("drag right did not seek forward into the later cue" + screenLog(), watch(1_500) { shown("See you later") })

        // Dragging back left returns into the first cue (1-5 s), still in English.
        rule.onRoot().performTouchInput { swipeLeft(startX = width * 0.70f, endX = width * 0.20f) }
        assertTrue("drag left did not seek back into the first cue" + screenLog(), watch(1_500) { shown("Hello") })
    }
}
