package app.looplayer.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Tracks
import app.looplayer.R
import app.looplayer.subtitle.TimeFormat
import app.looplayer.ui.settings.Group
import app.looplayer.ui.settings.IconRow
import app.looplayer.ui.settings.OptionRow
import app.looplayer.ui.settings.Segmented
import app.looplayer.ui.settings.SheetFrame
import app.looplayer.ui.theme.Ic

/** One selectable audio track of the open video. */
class AudioChoice(val label: String, val group: Tracks.Group, val selected: Boolean) {
    /** The container's own id for this stream, used to find it again when extracting subtitles. */
    val trackId: String? get() = group.getTrackFormat(0).id
    val language: String? get() = group.getTrackFormat(0).language?.takeIf { it != "und" }
}

private val SleepOptions = listOf(0, 15, 30, 45, 60)

/** Less-used playback tools, kept off the control bar: audio track, sleep timer, A–B repeat, lock, settings. */
@Composable
internal fun MoreSheet(
    audio: List<AudioChoice>,
    onAudio: (AudioChoice) -> Unit,
    sleepMinutes: Int,
    onSleep: (Int) -> Unit,
    loopA: Long?,
    loopB: Long?,
    onLoop: () -> Unit,
    onPip: () -> Unit,
    onLock: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    SheetFrame(null, onDismiss) {
        if (audio.size > 1) {
            Group(null) {
                OptionRow(stringResource(R.string.audio_track)) {
                    Segmented(audio, audio.firstOrNull { it.selected }, { it.label }, scroll = true) { onAudio(it) }
                }
            }
        }
        Group(null) {
            OptionRow(stringResource(R.string.sleep_timer)) {
                Segmented(SleepOptions, sleepMinutes, { m -> if (m == 0) stringResource(R.string.sleep_off) else stringResource(R.string.sleep_minutes, m) }) { onSleep(it) }
            }
        }

        val loopTitle = when {
            loopA == null -> R.string.loop_set_a
            loopB == null -> R.string.loop_set_b
            else -> R.string.loop_clear
        }
        val loopBody = when {
            loopA == null -> null
            loopB == null -> "A ${TimeFormat.format(loopA)}"
            else -> "A ${TimeFormat.format(loopA)} – B ${TimeFormat.format(loopB)}"
        }
        Group(null) {
            IconRow(Ic.Repeat, stringResource(loopTitle), loopBody ?: stringResource(R.string.ab_loop), onLoop)
            IconRow(Ic.Fit, stringResource(R.string.picture_in_picture), null, onPip)
            IconRow(Ic.Lock, stringResource(R.string.lock_touch), null, onLock)
            IconRow(Ic.Settings, stringResource(R.string.settings), null, onSettings)
        }
    }
}
