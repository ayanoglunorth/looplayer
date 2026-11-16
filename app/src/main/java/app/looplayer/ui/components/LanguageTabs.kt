package app.looplayer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.ui.unit.sp
import app.looplayer.subtitle.Slot
import app.looplayer.ui.theme.IndexTabStyle
import app.looplayer.ui.theme.Palette

/**
 * The thumb index on the edge of the picture: one tab per loaded language.
 * The speaking tab is inverted (white, black text) and stands proud of the edge;
 * when it changes, it turns over like a page tab being flipped.
 */
@Composable
fun EdgeIndexTabs(
    marks: Map<Slot, String>,
    active: Slot,
    modifier: Modifier = Modifier,
    /** Expanded widths: the confirmation must still read at arm's length on a tablet. */
    large: Boolean = false,
    /** Off: the tab changes in place, no slide and no flip. */
    animate: Boolean = true,
) {
    val tabWidth = if (large) 76.dp else 52.dp
    val tabHeight = if (large) 60.dp else 44.dp
    val label = if (large) IndexTabStyle.copy(fontSize = 18.sp) else IndexTabStyle
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (large) 8.dp else 6.dp), horizontalAlignment = Alignment.End) {
        Slot.entries.forEach { slot ->
            val mark = marks[slot] ?: return@forEach
            val isActive = slot == active
            val reach by animateDpAsState(if (isActive) 0.dp else 10.dp, if (animate) tween(220, easing = FastOutSlowInEasing) else snap(), label = "reach")
            val flip = remember { Animatable(0f) }
            LaunchedEffect(isActive) {
                if (isActive && animate) {
                    flip.snapTo(90f)
                    flip.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
                }
            }
            val fill by animateColorAsState(if (isActive) Palette.Text else Palette.Raised, if (animate) tween(200) else snap(), label = "fill")
            val text by animateColorAsState(if (isActive) Palette.Ground else Palette.Muted, if (animate) tween(200) else snap(), label = "text")
            Box(
                Modifier
                    .offset(x = reach)
                    .graphicsLayer { rotationX = flip.value; cameraDistance = 12f * density }
                    .width(tabWidth)
                    .height(tabHeight)
                    .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                    .background(fill),
                contentAlignment = Alignment.Center,
            ) {
                Text(mark, style = label, color = text, modifier = Modifier.padding(end = 4.dp))
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
            .background(Palette.Surface.copy(alpha = 0.8f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Slot.entries.forEach { slot ->
            val mark = marks[slot] ?: return@forEach
            val isActive = slot == active
            val fill by animateColorAsState(if (isActive) Palette.Text else Color.Transparent, tween(180), label = "fill")
            val text by animateColorAsState(if (isActive) Palette.Ground else Palette.Muted, tween(180), label = "text")
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

/** A small static language tab for lists and sheets; filled when that language is the one speaking. */
@Composable
fun SlotMark(mark: String, slot: Slot, filled: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (filled) Palette.Text else Palette.Raised)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            mark,
            style = IndexTabStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
            color = if (filled) Palette.Ground else Palette.Muted,
        )
    }
}
