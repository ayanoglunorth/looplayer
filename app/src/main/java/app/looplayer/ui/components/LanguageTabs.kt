package app.looplayer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.looplayer.subtitle.Slot
import app.looplayer.ui.theme.IndexTabStyle
import app.looplayer.ui.theme.Inks

/**
 * The thumb index on the edge of the picture: one tab per loaded language.
 * The speaking tab is filled with its ink and stands proud of the edge;
 * when it changes, it turns over like a page tab being flipped.
 */
@Composable
fun EdgeIndexTabs(
    marks: Map<Slot, String>,
    active: Slot,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
        Slot.entries.forEach { slot ->
            val mark = marks[slot] ?: return@forEach
            val isActive = slot == active
            val ink = Inks.of(slot)
            val reach by animateDpAsState(if (isActive) 0.dp else 10.dp, tween(220, easing = FastOutSlowInEasing), label = "reach")
            val flip = remember { Animatable(0f) }
            LaunchedEffect(isActive) {
                if (isActive) {
                    flip.snapTo(90f)
                    flip.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
                }
            }
            val fill by animateColorAsState(if (isActive) ink.ink else Color(0xCC1C1A17), tween(200), label = "fill")
            val text by animateColorAsState(if (isActive) ink.onInk else ink.ink, tween(200), label = "text")
            Box(
                Modifier
                    .offset(x = reach)
                    .graphicsLayer { rotationX = flip.value; cameraDistance = 12f * density }
                    .width(52.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                    .background(fill),
                contentAlignment = Alignment.Center,
            ) {
                Text(mark, style = IndexTabStyle, color = text, modifier = Modifier.padding(end = 4.dp))
            }
        }
    }
}

/** The same two tabs laid side by side in the control bar; tapping either one switches language. */
@Composable
fun LanguageSwitch(
    marks: Map<Slot, String>,
    active: Slot,
    onSelect: (Slot) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x991C1A17))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Slot.entries.forEach { slot ->
            val mark = marks[slot] ?: return@forEach
            val isActive = slot == active
            val ink = Inks.of(slot)
            val fill by animateColorAsState(if (isActive) ink.ink else Color.Transparent, tween(180), label = "fill")
            val text by animateColorAsState(if (isActive) ink.onInk else ink.ink, tween(180), label = "text")
            Box(
                Modifier
                    .size(width = 52.dp, height = 42.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(fill)
                    .selectable(selected = isActive, role = Role.Tab, onClick = { onSelect(slot) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(mark, style = IndexTabStyle, color = text, textAlign = TextAlign.Center)
            }
        }
    }
}

/** A small static tab used in lists and sheets to show which ink belongs to which slot. */
@Composable
fun SlotMark(mark: String, slot: Slot, filled: Boolean, modifier: Modifier = Modifier) {
    val ink = Inks.of(slot)
    Box(
        modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (filled) ink.ink else ink.ink.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            mark,
            style = IndexTabStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
            color = if (filled) ink.onInk else ink.ink,
        )
    }
}
