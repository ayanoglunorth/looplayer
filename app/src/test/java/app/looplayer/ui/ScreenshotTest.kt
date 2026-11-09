package app.looplayer.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import app.looplayer.R
import app.looplayer.data.LibraryStore
import app.looplayer.data.SubtitleRef
import app.looplayer.data.SubtitleSession
import app.looplayer.data.VideoEntry
import app.looplayer.subtitle.Cue
import app.looplayer.subtitle.Slot
import app.looplayer.subtitle.SubtitleTrack
import app.looplayer.ui.components.EdgeIndexTabs
import app.looplayer.ui.components.SlotUi
import app.looplayer.ui.components.SubtitleSlots
import app.looplayer.ui.library.LibraryScreen
import app.looplayer.ui.library.SetupSheet
import app.looplayer.ui.player.Controls
import app.looplayer.ui.player.HintPill
import app.looplayer.ui.player.PlaybackError
import app.looplayer.ui.player.PlayerSubtitleSheet
import app.looplayer.ui.player.SeekReadout
import app.looplayer.ui.player.SubtitleLine
import app.looplayer.ui.theme.LooplayerTheme
import com.github.takahirom.roborazzi.captureScreenRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

private const val PHONE = "w393dp-h851dp-xxhdpi"
private const val PHONE_LAND = "w851dp-h393dp-land-xxhdpi"
private const val TABLET = "w800dp-h1280dp-xhdpi"
private const val TABLET_LAND = "w1280dp-h800dp-land-xhdpi"

/**
 * Renders the real composables on the JVM (Robolectric + Roborazzi) so the design can be reviewed
 * without a device. Video frames are a synthetic gradient stand-in; titles and subtitle lines are
 * sample content. Subtitle files for the sheets are real SRT files parsed by the app.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.setContent { LooplayerTheme { content() } }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/$name.png")
    }

    // ---- Library -------------------------------------------------------------------------

    @Test @Config(qualifiers = PHONE) fun phone_library_empty() = shot("phone-library_empty") { emptyLibrary() }
    @Test @Config(qualifiers = PHONE) fun phone_library_list() = shot("phone-library_list") { library() }
    @Test @Config(qualifiers = TABLET) fun tablet_library_empty() = shot("tablet-library_empty") { emptyLibrary() }
    @Test @Config(qualifiers = TABLET) fun tablet_library_list() = shot("tablet-library_list") { library() }
    @Test @Config(qualifiers = PHONE, fontScale = 1.3f) fun phone_library_empty_fs130() = shot("phone-library_empty-fs130") { emptyLibrary() }
    @Test @Config(qualifiers = PHONE, fontScale = 1.3f) fun phone_library_list_fs130() = shot("phone-library_list-fs130") { library() }
    @Test @Config(qualifiers = TABLET, fontScale = 1.3f) fun tablet_library_list_fs130() = shot("tablet-library_list-fs130") { library() }

    // ---- Subtitle slots and sheets ---------------------------------------------------------

    @Test @Config(qualifiers = PHONE) fun phone_subtitle_slots() = shot("phone-subtitle_slots") { slots() }
    @Test @Config(qualifiers = TABLET) fun tablet_subtitle_slots() = shot("tablet-subtitle_slots") { slots() }
    @Test @Config(qualifiers = PHONE, fontScale = 1.3f) fun phone_subtitle_slots_fs130() = shot("phone-subtitle_slots-fs130") { slots() }

    @Test @Config(qualifiers = PHONE)
    fun phone_setup_sheet() {
        val session = sessionWithRealSubtitles()
        shot("phone-setup_sheet") { library(); SetupSheet(session, {}, {}, {}) }
    }

    @Test @Config(qualifiers = PHONE, fontScale = 1.3f)
    fun phone_setup_sheet_fs130() {
        val session = sessionWithRealSubtitles()
        shot("phone-setup_sheet-fs130") { library(); SetupSheet(session, {}, {}, {}) }
    }

    @Test @Config(qualifiers = PHONE_LAND)
    fun phone_player_subtitle_sheet() {
        val session = sessionWithRealSubtitles()
        shot("phone-player_subtitle_sheet") { FakeFrame { }; PlayerSubtitleSheet(session, {}, {}) }
    }

    // ---- Player --------------------------------------------------------------------------

    @Test @Config(qualifiers = PHONE_LAND) fun phone_player_controls() = shot("phone-player_controls") { playerControls() }
    @Test @Config(qualifiers = TABLET_LAND) fun tablet_player_controls() = shot("tablet-player_controls") { playerControls() }
    @Test @Config(qualifiers = PHONE_LAND, fontScale = 1.3f) fun phone_player_controls_fs130() = shot("phone-player_controls-fs130") { playerControls() }
    @Test @Config(qualifiers = TABLET_LAND, fontScale = 1.3f) fun tablet_player_controls_fs130() = shot("tablet-player_controls-fs130") { playerControls() }

    @Test @Config(qualifiers = PHONE_LAND) fun phone_player_after_switch() = shot("phone-player_after_switch") { afterSwitch() }
    @Test @Config(qualifiers = TABLET_LAND) fun tablet_player_after_switch() = shot("tablet-player_after_switch") { afterSwitch() }
    @Test @Config(qualifiers = PHONE_LAND, fontScale = 1.3f) fun phone_player_after_switch_fs130() = shot("phone-player_after_switch-fs130") { afterSwitch() }

    @Test @Config(qualifiers = PHONE_LAND)
    fun phone_player_seek_readout() = shot("phone-player_seek_readout") {
        FakeFrame {
            SeekReadout(targetMs = 4_124_000, fromMs = 4_100_000, durationMs = 11_760_000, modifier = Modifier.align(Alignment.Center))
        }
    }

    @Test @Config(qualifiers = PHONE_LAND)
    fun phone_player_single_subtitle_hint() = shot("phone-player_single_subtitle_hint") {
        FakeFrame {
            Box(Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 72.dp)) {
                HintPill(RuntimeEnvironment.getApplication().getString(R.string.only_one_subtitle), actionLabel = RuntimeEnvironment.getApplication().getString(R.string.add))
            }
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)) {
                SubtitleLine("Bunu sana daha önce de söylemiştim.")
            }
        }
    }

    @Test @Config(qualifiers = PHONE_LAND)
    fun phone_player_error() = shot("phone-player_error") {
        Box(Modifier.fillMaxSize().background(Color.Black)) { PlaybackError(onBack = {}, onChooseAnother = {}, modifier = Modifier.fillMaxSize()) }
    }

    // ---- Scenes ----------------------------------------------------------------------------

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

    @Composable private fun emptyLibrary() = LibraryScreen(emptyList(), SnackbarHostState(), {}, {}, {})

    @Composable private fun library() = LibraryScreen(samples, SnackbarHostState(), {}, {}, {})

    @Composable
    private fun slots() = Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxSize()) {
        // Same width cap the modal sheet applies on large screens.
        Box(contentAlignment = Alignment.TopCenter) {
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
                modifier = Modifier.widthIn(max = 640.dp).padding(24.dp),
            )
        }
    }

    @Composable
    private fun playerControls() = FakeFrame {
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 116.dp)) {
            SubtitleLine("Bunu sana daha önce de söylemiştim,\nama dinlemedin.")
        }
        Controls(
            title = "Kış Uykusu", isPlaying = true, buffering = false,
            position = { 4_100_000 }, durationMs = 11_760_000, speed = 1f, zoom = false,
            marks = mapOf(Slot.A to "TR", Slot.B to "EN"), active = Slot.A,
            onBack = {}, onPlayPause = {}, onSkip = {}, onScrub = {}, onScrubEnd = {},
            onSpeed = {}, onZoom = {}, rotationLocked = false, onRotationLock = {}, onSelectLanguage = {}, onOpenSubtitles = {},
        )
    }

    @Composable
    private fun afterSwitch() = FakeFrame {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)) {
                SubtitleLine("I told you this before,\nbut you didn't listen.")
            }
            EdgeIndexTabs(mapOf(Slot.A to "TR", Slot.B to "EN"), Slot.B, Modifier.align(Alignment.CenterEnd), large = LocalConfiguration.current.screenWidthDp >= 840)
        }
    }

    /** Synthetic stand-in for a video frame: a dusk landscape gradient. */
    @Composable
    private fun FakeFrame(content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
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

    /** A session whose two slots hold real SRT files, parsed by the app's own loader. */
    private fun sessionWithRealSubtitles(): SubtitleSession {
        val app = RuntimeEnvironment.getApplication()
        val dir = File(app.cacheDir, "subs").apply { mkdirs() }
        fun srt(name: String, line: String) = File(dir, name).apply {
            writeText((1..1284).joinToString("\n\n") { i ->
                val s = i * 2_000L
                "$i\n${clock(s)} --> ${clock(s + 1_500)}\n$line $i"
            })
        }
        val tr = srt("Kis.Uykusu.2014.tr.srt", "Satır")
        val en = srt("Winter.Sleep.2014.en.srt", "Line")
        val entry = VideoEntry(uri = "content://sample/1", title = "Kış Uykusu", durationMs = 11_760_000, positionMs = 4_100_000)
        val session = SubtitleSession(app.contentResolver, LibraryStore(app), entry)
        runBlocking {
            session.attach(Slot.A, Uri.fromFile(tr))
            session.attach(Slot.B, Uri.fromFile(en))
        }
        return session
    }

    private fun clock(ms: Long) = "%02d:%02d:%02d,%03d".format(ms / 3_600_000, ms / 60_000 % 60, ms / 1000 % 60, ms % 1000)
}
