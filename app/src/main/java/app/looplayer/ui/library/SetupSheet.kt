package app.looplayer.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.looplayer.R
import app.looplayer.data.SubtitleSession
import app.looplayer.subtitle.Slot
import app.looplayer.subtitle.TimeFormat
import app.looplayer.ui.components.SlotUi
import app.looplayer.ui.components.SubtitleSlots
import app.looplayer.ui.theme.TabularNumbers
import app.looplayer.ui.theme.Ic

/** Before playing: the video, its two subtitle editions, and Play. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupSheet(
    session: SubtitleSession,
    onPickSubtitle: (Slot) -> Unit,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            val entry = session.entry
            Row(verticalAlignment = Alignment.CenterVertically) {
                Thumbnail(entry, Modifier.width(112.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (entry.durationMs > 0) {
                        Text(
                            TimeFormat.format(entry.durationMs),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularNumbers),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.subtitles),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SubtitleSlots(
                slots = Slot.entries.associateWith { SlotUi(entry.sub(it), session.tracks[it], it in session.loading) },
                active = session.dual.active,
                visible = session.visible,
                onPick = onPickSubtitle,
                onRemove = session::remove,
                onOffset = session::shift,
                onVisibleChange = { session.visible = it },
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(painterResource(Ic.Play), contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                val resume = entry.resumeMs
                Text(
                    if (resume > 0) stringResource(R.string.resume_from, TimeFormat.format(resume)) else stringResource(R.string.play),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}
