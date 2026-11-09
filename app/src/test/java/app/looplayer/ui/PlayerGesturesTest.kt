package app.looplayer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import app.looplayer.subtitle.Cue
import app.looplayer.subtitle.DualSubtitles
import app.looplayer.subtitle.Slot
import app.looplayer.subtitle.SubtitleTrack
import app.looplayer.ui.player.Controls
import app.looplayer.ui.player.PlayerGestureHandler
import app.looplayer.ui.player.SubtitleLine
import app.looplayer.ui.player.playerGestures
import app.looplayer.ui.theme.Inks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerGesturesTest {

    @get:Rule
    val rule = createComposeRule()

    private var taps = 0
    private var doubleTaps = 0
    private var seekStarts = 0
    private var seekEnds = 0
    private var lastDrag = 0f

    private fun setSurface() {
        rule.setContent {
            Box(
                Modifier
                    .size(400.dp, 225.dp)
                    .testTag("surface")
                    .playerGestures(
                        PlayerGestureHandler(
                            onTap = { taps++ },
                            onDoubleTap = { doubleTaps++ },
                            onSeekStart = { seekStarts++ },
                            onSeek = { drag, _ -> lastDrag = drag },
                            onSeekEnd = { seekEnds++ },
                            onSeekCancel = {},
                        ),
                    ),
            )
        }
    }

    private fun settle() {
        rule.mainClock.advanceTimeBy(600)
        rule.waitForIdle()
    }

    @Test
    fun `double tap switches language and does not toggle controls`() {
        setSurface()
        rule.onNodeWithTag("surface").performTouchInput { doubleClick() }
        settle()
        assertEquals(1, doubleTaps)
        assertEquals(0, taps)
        assertEquals(0, seekStarts)
    }

    @Test
    fun `single tap toggles controls only`() {
        setSurface()
        rule.onNodeWithTag("surface").performTouchInput { click() }
        settle()
        assertEquals(1, taps)
        assertEquals(0, doubleTaps)
    }

    @Test
    fun `dragging right seeks forward, left seeks backward, and neither taps`() {
        setSurface()
        rule.onNodeWithTag("surface").performTouchInput { swipeRight() }
        settle()
        assertEquals(1, seekStarts)
        assertEquals(1, seekEnds)
        assertTrue("expected forward drag, was $lastDrag", lastDrag > 0f)

        rule.onNodeWithTag("surface").performTouchInput { swipeLeft() }
        settle()
        assertEquals(2, seekEnds)
        assertTrue("expected backward drag, was $lastDrag", lastDrag < 0f)
        assertEquals(0, taps)
        assertEquals(0, doubleTaps)
    }

    @Test
    fun `vertical swipe does not seek`() {
        setSurface()
        rule.onNodeWithTag("surface").performTouchInput { swipeUp() }
        settle()
        assertEquals(0, seekStarts)
        assertEquals(0, taps)
    }

    @Test
    @org.robolectric.annotation.Config(qualifiers = "w851dp-h393dp-land")
    fun `double tap still reaches the picture through the visible controls overlay`() {
        rule.setContent {
            Box(Modifier.size(851.dp, 393.dp)) {
                Box(
                    Modifier.matchParentSize().testTag("surface").playerGestures(
                        PlayerGestureHandler(
                            onTap = { taps++ }, onDoubleTap = { doubleTaps++ },
                            onSeekStart = { seekStarts++ }, onSeek = { _, _ -> }, onSeekEnd = { seekEnds++ }, onSeekCancel = {},
                        ),
                    ),
                )
                Controls(
                    title = "x", isPlaying = true, buffering = false, position = { 0 }, durationMs = 60_000,
                    speed = 1f, zoom = false, marks = mapOf(Slot.A to "TR", Slot.B to "EN"), active = Slot.A,
                    onBack = {}, onPlayPause = {}, onSkip = {}, onScrub = {}, onScrubEnd = {}, onSpeed = {},
                    onZoom = {}, onSelectLanguage = {}, onOpenSubtitles = {},
                )
            }
        }
        // An empty spot left of centre, between the top bar and the transport buttons.
        rule.onNodeWithTag("surface").performTouchInput { doubleClick(androidx.compose.ui.geometry.Offset(width * 0.2f, height * 0.3f)) }
        settle()
        assertEquals(1, doubleTaps)
        rule.onNodeWithTag("surface").performTouchInput { swipeRight(startX = width * 0.2f, endX = width * 0.6f) }
        settle()
        assertEquals(1, seekEnds)
    }

    @Test
    fun `double tap on the picture swaps the subtitle shown on screen`() {
        val tr = SubtitleTrack("Türkçe", "tr", listOf(Cue(0, 5_000, "Merhaba")))
        val en = SubtitleTrack("English", "en", listOf(Cue(0, 5_000, "Hello")))
        var dual by mutableStateOf(DualSubtitles().withTrack(Slot.A, tr).withTrack(Slot.B, en))
        rule.setContent {
            Box(
                Modifier
                    .size(400.dp, 225.dp)
                    .testTag("surface")
                    .playerGestures(
                        PlayerGestureHandler(
                            onTap = {}, onDoubleTap = { dual = dual.switched() },
                            onSeekStart = {}, onSeek = { _, _ -> }, onSeekEnd = {}, onSeekCancel = {},
                        ),
                    ),
            ) {
                SubtitleLine(dual.textAt(1_000), Inks.of(dual.active).subtitleTint)
            }
        }
        rule.onAllNodesWithText("Merhaba").onFirst().assertExists()

        rule.onNodeWithTag("surface").performTouchInput { doubleClick() }
        settle()
        rule.onAllNodesWithText("Hello").onFirst().assertExists()
        rule.onAllNodesWithText("Merhaba").assertCountEquals0()

        rule.onNodeWithTag("surface").performTouchInput { doubleClick() }
        settle()
        rule.onAllNodesWithText("Merhaba").onFirst().assertExists()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEquals0() =
        assertEquals(0, fetchSemanticsNodes().size)
}
