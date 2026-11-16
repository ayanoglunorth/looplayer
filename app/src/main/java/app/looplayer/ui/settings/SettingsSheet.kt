package app.looplayer.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.looplayer.R
import app.looplayer.data.AppLanguage
import app.looplayer.data.OrientationMode
import app.looplayer.data.SettingsStore
import app.looplayer.data.SiblingSubtitles
import app.looplayer.ui.player.findActivity

private val SkipOptions = listOf(5, 10, 15, 30)
private val HoldSpeeds = listOf(1.5f, 2f, 3f)
private val Timeouts = listOf(3, 5, 8)

/** App-wide settings. Subtitle looks live one level down, so this list stays short enough to scan. */
@Composable
fun SettingsSheet(store: SettingsStore, onDismiss: () -> Unit) {
    val s = store.value
    var styleOpen by remember { mutableStateOf(false) }
    SheetFrame(stringResource(R.string.settings), onDismiss) {
        Group(stringResource(R.string.section_appearance)) {
            ToggleRow(stringResource(R.string.dark_theme), stringResource(R.string.dark_theme_body), s.dark ?: isSystemInDarkTheme()) {
                store.update { c -> c.copy(dark = it) }
            }
            OptionRow(stringResource(R.string.language)) {
                val shown = s.language ?: if (LocalConfiguration.current.locales[0].language == "en") AppLanguage.English else AppLanguage.Turkish
                val activity = LocalContext.current.findActivity()
                Segmented(AppLanguage.entries, shown, { stringResource(if (it == AppLanguage.Turkish) R.string.language_turkish else R.string.language_english) }) { pick ->
                    if (pick != shown) {
                        store.update { c -> c.copy(language = pick) }
                        // Resources are chosen when the activity is created, so it restarts in the new language.
                        activity?.recreate()
                    }
                }
            }
        }

        Group(stringResource(R.string.subtitles)) {
            NavRow(stringResource(R.string.section_subtitle_style), stringResource(R.string.subtitle_style_body)) { styleOpen = true }
            LanguageSwitchRows(store)
        }

        Group(stringResource(R.string.section_player)) {
            ToggleRow(stringResource(R.string.swipe_adjust), stringResource(R.string.swipe_adjust_body), s.swipeAdjust) {
                store.update { c -> c.copy(swipeAdjust = it) }
            }
            ToggleRow(stringResource(R.string.hold_boost), stringResource(R.string.hold_boost_body), s.holdBoost) {
                store.update { c -> c.copy(holdBoost = it) }
            }
            if (s.holdBoost) {
                OptionRow(stringResource(R.string.hold_speed)) {
                    Segmented(HoldSpeeds, s.holdSpeed, { stringResource(R.string.speed_x, it.toString().removeSuffix(".0")) }) {
                        store.update { c -> c.copy(holdSpeed = it) }
                    }
                }
            }
            OptionRow(stringResource(R.string.skip_interval)) {
                Segmented(SkipOptions, s.skipSeconds, { stringResource(R.string.seconds_short, it) }) { store.update { c -> c.copy(skipSeconds = it) } }
            }
            OptionRow(stringResource(R.string.controls_timeout)) {
                Segmented(Timeouts, s.controlsTimeoutSec, { stringResource(R.string.seconds_short, it) }) { store.update { c -> c.copy(controlsTimeoutSec = it) } }
            }
            OptionRow(stringResource(R.string.orientation)) {
                Segmented(OrientationMode.entries, s.orientation, { stringResource(if (it == OrientationMode.Video) R.string.orientation_video else R.string.orientation_sensor) }) {
                    store.update { c -> c.copy(orientation = it) }
                }
            }
        }

        Group(stringResource(R.string.section_system)) { SystemRows() }
    }
    if (styleOpen) SubtitleStyleSheet(store, onDismiss = { styleOpen = false })
}

/** Things only Android itself can grant: being the default player, and reading folders for sibling subtitles. */
@Composable
private fun SystemRows() {
    val context = LocalContext.current
    var folderAccess by remember { mutableStateOf(SiblingSubtitles.hasFolderAccess()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { folderAccess = SiblingSubtitles.hasFolderAccess() }

    ActionRow(R.string.default_player, R.string.default_player_body, R.string.open_settings) {
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        runCatching { context.startActivity(intent) }.onFailure {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
    }
    ActionRow(R.string.folder_subtitles, R.string.folder_subtitles_body, if (folderAccess) R.string.folder_subtitles_on else R.string.grant, enabled = !folderAccess) {
        val pkg = Uri.parse("package:${context.packageName}")
        runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, pkg)) }
            .onFailure { context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
    }
}

@Composable
private fun ActionRow(title: Int, body: Int, action: Int, enabled: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onClick, enabled = enabled) { Text(stringResource(action)) }
    }
}
