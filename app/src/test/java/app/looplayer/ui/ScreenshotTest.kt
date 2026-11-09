package app.looplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.looplayer.data.SubtitleRef
import app.looplayer.data.VideoEntry
import app.looplayer.subtitle.Cue
import app.looplayer.subtitle.Slot
import app.looplayer.subtitle.SubtitleTrack
import app.looplayer.ui.components.EdgeIndexTabs
import app.looplayer.ui.components.SlotUi
import app.looplayer.ui.components.SubtitleSlots
import app.looplayer.ui.library.LibraryScreen
import app.looplayer.ui.player.Controls
import app.looplayer.ui.player.SubtitleLine
import app.looplayer.ui.theme.Inks
import app.looplayer.ui.theme.LooplayerTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real composables on the JVM so the design can be reviewed without a device.
 * Video frames are a synthetic stand-in gradient; titles and subtitle lines are sample content.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    private val now = 1_762_700_000_000L
    private val samples = listOf(
        VideoEntry(
            uri = "content://sample/1", title = "Kış Uykusu", durationMs = 11_760_000, positionMs = 4_100_000, lastOpened = now,
            subA = SubtitleRef("content://s/1a", "Kis.Uykusu.tr.srt", "tr"),
            subB = SubtitleRef("content://s/1b", "Winter.Sleep.en.srt", "en"), activeSlot = Slot.A,
        ),
        VideoEntry(
            uri = "content://sample/2", title = "Before Sunrise", durationMs = 6_060_000, positionMs = 6_000_000, lastOpened = now - 1,
            subA = SubtitleRef("content://s/2a", "Before.Sunrise.en.srt", "en"),
            subB = SubtitleRef("content://s/2b", "Before.Sunrise.tr.srt", "tr"), activeSlot = Slot.B,
        ),
        VideoEntry(
            uri = "content://sample/3", title = "Amélie", durationMs = 7_320_000, positionMs = 0, lastOpened = now - 2,
            subA = SubtitleRef("content://s/3a", "Amelie.fr.srt", "fr"),
        ),
        VideoEntry(uri = "content://sample/4", title = "Ev videosu 2019", durationMs = 412_000, lastOpened = now - 3),
    )

    @Test
    @Config(qualifiers = "w393dp-h851dp-xxhdpi")
    fun library_empty() = captureRoboImage("src/test/screenshots/library_empty.png") {
        LooplayerTheme { LibraryScreen(emptyList(), SnackbarHostState(), {}, {}, {}) }
    }

    @Test
    @Config(qualifiers = "w393dp-h851dp-xxhdpi")
    fun library_list() = captureRoboImage("src/test/screenshots/library_list.png") {
        LooplayerTheme { LibraryScreen(samples, SnackbarHostState(), {}, {}, {}) }
    }

    @Test
    @Config(qualifiers = "w393dp-h700dp-xxhdpi")
    fun subtitle_slots() = captureRoboImage("src/test/screenshots/subtitle_slots.png") {
        LooplayerTheme {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                SubtitleSlots(
                    slots = mapOf(
                        Slot.A to SlotUi(
                            SubtitleRef("u", "Kis.Uykusu.2014.tr.srt", "tr", offsetMs = 500),
                            SubtitleTrack("Türkçe", "tr", List(1284) { Cue(it * 1000L, it * 1000L + 900, "x") }),
                            loading = false,
                        ),
                        Slot.B to SlotUi(null, null, loading = false),
                    ),
                    active = Slot.A,
                    visible = true,
                    onPick = {}, onRemove = {}, onOffset = { _, _ -> }, onVisibleChange = {},
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w851dp-h393dp-land-xxhdpi")
    fun player_controls() = captureRoboImage("src/test/screenshots/player_controls.png") {
        LooplayerTheme {
            FakeFrame {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 132.dp)) {
                        SubtitleLine("Bunu sana daha önce de söylemiştim,\nama dinlemedin.", Inks.A.subtitleTint)
                    }
                    Controls(
                        title = "Kış Uykusu", isPlaying = true, buffering = false,
                        positionMs = 4_100_000, durationMs = 11_760_000, speed = 1f, zoom = false,
                        marks = mapOf(Slot.A to "TR", Slot.B to "EN"), active = Slot.A,
                        onBack = {}, onPlayPause = {}, onSkip = {}, onScrub = {}, onScrubEnd = {},
                        onSpeed = {}, onZoom = {}, onSelectLanguage = {}, onOpenSubtitles = {},
                    )
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "w851dp-h393dp-land-xxhdpi")
    fun player_after_switch() = captureRoboImage("src/test/screenshots/player_after_switch.png") {
        LooplayerTheme {
            FakeFrame {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)) {
                        SubtitleLine("I told you this before,\nbut you didn't listen.", Inks.B.subtitleTint)
                    }
                    EdgeIndexTabs(mapOf(Slot.A to "TR", Slot.B to "EN"), Slot.B, Modifier.align(Alignment.CenterEnd))
                }
            }
        }
    }

    /** Synthetic stand-in for a video frame: a dusk landscape gradient. */
    @Composable
    private fun FakeFrame(content: @Composable () -> Unit) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFF1B2433), 0.55f to Color(0xFF6B5A4E), 0.62f to Color(0xFF3A3029), 1f to Color(0xFF14110F),
                    ),
                ),
        ) { content() }
    }
}
