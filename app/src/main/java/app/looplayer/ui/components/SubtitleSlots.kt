package app.looplayer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.toggleable
import app.looplayer.R
import app.looplayer.data.SubtitleRef
import app.looplayer.data.languageName
import app.looplayer.data.tabMark
import app.looplayer.subtitle.Slot
import app.looplayer.subtitle.SubtitleTrack
import app.looplayer.ui.theme.Palette
import app.looplayer.ui.theme.Ic
import app.looplayer.ui.theme.TabularNumbers
import java.text.NumberFormat
import java.util.Locale

/** What the slot list needs to know about one slot. */
data class SlotUi(val ref: SubtitleRef?, val track: SubtitleTrack?, val loading: Boolean)

/**
 * The two subtitle editions of a video, shown as two dictionary entries:
 * slot mark, language as the headword, file and line count beneath.
 */
@Composable
fun SubtitleSlots(
    slots: Map<Slot, SlotUi>,
    active: Slot,
    visible: Boolean,
    onPick: (Slot) -> Unit,
    onRemove: (Slot) -> Unit,
    onOffset: (Slot, Long) -> Unit,
    onVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Slot.entries.forEachIndexed { i, slot ->
            if (i > 0) Spacer(Modifier.height(8.dp))
            SlotRow(slot, slots[slot] ?: SlotUi(null, null, false), isActive = slot == active, onPick, onRemove, onOffset)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = visible, role = Role.Switch, onValueChange = onVisibleChange)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.show_subtitles), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = visible, onCheckedChange = null)
        }
        Text(
            stringResource(R.string.switch_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SlotRow(
    slot: Slot,
    ui: SlotUi,
    isActive: Boolean,
    onPick: (Slot) -> Unit,
    onRemove: (Slot) -> Unit,
    onOffset: (Slot, Long) -> Unit,
) {
    val ref = ui.ref
    val placeholder = stringResource(if (slot == Slot.A) R.string.slot_first else R.string.slot_second)
    Column(Modifier.padding(vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SlotMark(tabMark(ref, slot), slot, filled = ref != null && isActive, modifier = Modifier.width(44.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = ref?.let { languageName(it.language) ?: it.fileName.substringBeforeLast('.') } ?: placeholder,
                    style = if (ref != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    color = if (ref != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when {
                ui.loading -> CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp, color = Palette.Text)
                ref == null -> FilledTonalButton(
                    onClick = { onPick(slot) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Palette.Raised,
                        contentColor = Palette.Text,
                    ),
                ) {
                    Icon(painterResource(Ic.Add), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add))
                }
                else -> {
                    TextButton(onClick = { onPick(slot) }) { Text(stringResource(R.string.replace)) }
                    IconButton(onClick = { onRemove(slot) }) {
                        Icon(painterResource(Ic.Close), contentDescription = stringResource(R.string.remove))
                    }
                }
            }
        }
        if (ref != null) {
            // Its own full-width line, so the file name (what tells two editions apart) survives large text.
            val count = ui.track?.cues?.size
            Text(
                text = listOfNotNull(ref.fileName, count?.let { stringResource(R.string.line_count, it) }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TabularNumbers),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 58.dp, top = 2.dp),
            )
        }
        if (ref != null && ui.track != null) {
            Row(Modifier.padding(start = 58.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.sync_offset, formatSeconds(ref.offsetMs)),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularNumbers),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onOffset(slot, -250) }) {
                    Icon(painterResource(Ic.Remove), contentDescription = stringResource(R.string.earlier))
                }
                IconButton(onClick = { onOffset(slot, 250) }) {
                    Icon(painterResource(Ic.Add), contentDescription = stringResource(R.string.later))
                }
            }
        }
    }
}

private fun formatSeconds(ms: Long): String {
    val f = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }
    val sign = if (ms > 0) "+" else if (ms < 0) "−" else ""
    return sign + f.format(kotlin.math.abs(ms) / 1000.0)
}
