package app.looplayer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.looplayer.R
import app.looplayer.data.AppSettings
import app.looplayer.data.SettingsStore
import app.looplayer.data.SubtitleBackdrop
import app.looplayer.data.SubtitleColor
import app.looplayer.data.SubtitleFont
import app.looplayer.ui.player.SubtitleLine
import app.looplayer.ui.theme.color
import kotlin.math.roundToInt

/** Everything about how subtitles look and how the language switch behaves, with a live preview. */
@Composable
fun SubtitleStyleSheet(store: SettingsStore, onDismiss: () -> Unit) {
    val s = store.value
    SheetFrame(stringResource(R.string.section_subtitle_style), onDismiss) {
        Preview(s)

        Group(stringResource(R.string.group_text)) {
            OptionRow(stringResource(R.string.font)) {
                Segmented(SubtitleFont.entries, s.font, { fontLabel(it) }, scroll = true) { store.update { c -> c.copy(font = it) } }
            }
            SliderRow(
                stringResource(R.string.text_size), "${s.sizePercent}%", s.sizePercent.toFloat(),
                SettingsStore.MIN_SIZE.toFloat()..SettingsStore.MAX_SIZE.toFloat(), 0,
            ) { v -> store.update { c -> c.copy(sizePercent = (v / 10).roundToInt() * 10) } }
            ToggleRow(stringResource(R.string.bold), null, s.bold) { store.update { c -> c.copy(bold = it) } }
            OptionRow(stringResource(R.string.text_color)) {
                Segmented(SubtitleColor.entries, s.color, { colorLabel(it) }, scroll = true, dot = { it.color() }) {
                    store.update { c -> c.copy(color = it) }
                }
            }
            SliderRow(
                stringResource(R.string.text_opacity), "${s.textOpacity}%", s.textOpacity.toFloat(),
                SettingsStore.MIN_OPACITY.toFloat()..100f, 0,
            ) { v -> store.update { c -> c.copy(textOpacity = (v / 5).roundToInt() * 5) } }
        }

        Group(null) {
            OptionRow(stringResource(R.string.backdrop)) {
                Segmented(SubtitleBackdrop.entries, s.backdrop, { backdropLabel(it) }) { store.update { c -> c.copy(backdrop = it) } }
            }
            if (s.backdrop == SubtitleBackdrop.Box) {
                SliderRow(
                    stringResource(R.string.backdrop_opacity), "${s.backdropOpacity}%", s.backdropOpacity.toFloat(), 0f..100f, 0,
                ) { v -> store.update { c -> c.copy(backdropOpacity = (v / 10).roundToInt() * 10) } }
            }
        }

        Group(null) {
            SliderRow(
                stringResource(R.string.subtitle_position), "${s.position}%", s.position.toFloat(), 0f..SettingsStore.MAX_POSITION.toFloat(), 0,
            ) { v -> store.update { c -> c.copy(position = (v / 5).roundToInt() * 5) } }
        }

        Group(stringResource(R.string.group_switching)) { LanguageSwitchRows(store) }

        TextButton(
            onClick = {
                val d = AppSettings()
                store.update { c ->
                    c.copy(
                        font = d.font, sizePercent = d.sizePercent, bold = d.bold, color = d.color, backdrop = d.backdrop,
                        position = d.position, textOpacity = d.textOpacity, backdropOpacity = d.backdropOpacity,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.reset_defaults), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** The two switches for what a double-tap shows; shared by this sheet and the app settings. */
@Composable
internal fun LanguageSwitchRows(store: SettingsStore) {
    val s = store.value
    ToggleRow(stringResource(R.string.language_indicator), stringResource(R.string.language_indicator_body), s.languageIndicator) {
        store.update { c -> c.copy(languageIndicator = it) }
    }
    ToggleRow(stringResource(R.string.switch_animation), stringResource(R.string.switch_animation_body), s.switchAnimation) {
        store.update { c -> c.copy(switchAnimation = it) }
    }
}

/** The subtitle as it will look, over a mid-tone picture stand-in. */
@Composable
private fun Preview(s: AppSettings) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF5A6B7A), Color(0xFFB9A98E)))),
        contentAlignment = Alignment.Center,
    ) {
        SubtitleLine(stringResource(R.string.preview_line), s)
    }
}

@Composable
private fun fontLabel(f: SubtitleFont) = stringResource(
    when (f) {
        SubtitleFont.Inter -> R.string.font_inter
        SubtitleFont.Roboto -> R.string.font_roboto
        SubtitleFont.Atkinson -> R.string.font_atkinson
        SubtitleFont.OpenSans -> R.string.font_open_sans
        SubtitleFont.PtNarrow -> R.string.font_pt_narrow
        SubtitleFont.Inconsolata -> R.string.font_inconsolata
        SubtitleFont.Serif -> R.string.font_serif
    },
)

@Composable
private fun colorLabel(c: SubtitleColor) = stringResource(
    when (c) {
        SubtitleColor.White -> R.string.color_white
        SubtitleColor.Yellow -> R.string.color_yellow
        SubtitleColor.Cyan -> R.string.color_cyan
        SubtitleColor.Green -> R.string.color_green
    },
)

@Composable
private fun backdropLabel(b: SubtitleBackdrop) = stringResource(
    when (b) {
        SubtitleBackdrop.Outline -> R.string.backdrop_outline
        SubtitleBackdrop.Shadow -> R.string.backdrop_shadow
        SubtitleBackdrop.Box -> R.string.backdrop_box
    },
)
