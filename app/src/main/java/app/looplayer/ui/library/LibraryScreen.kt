package app.looplayer.ui.library

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.looplayer.R
import app.looplayer.data.Files
import app.looplayer.data.VideoEntry
import app.looplayer.data.tabMark
import app.looplayer.subtitle.Slot
import app.looplayer.subtitle.TimeFormat
import app.looplayer.ui.components.SlotMark
import app.looplayer.ui.theme.TabularNumbers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    entries: List<VideoEntry>,
    snackbar: SnackbarHostState,
    onOpenVideo: () -> Unit,
    onSelect: (VideoEntry) -> Unit,
    onRemove: (VideoEntry) -> Unit,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            // Same 720dp column as the list, so heading and rows share one left edge on tablets.
            Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.library_title)) },
                    scrollBehavior = scroll,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                    ),
                    modifier = Modifier.widthIn(max = ContentMaxWidth),
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenVideo,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.open_video)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (entries.isEmpty()) {
            EmptyLibrary(Modifier.padding(padding))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                modifier = Modifier.fillMaxSize().wrapContentWidth().widthIn(max = ContentMaxWidth),
            ) {
                items(entries, key = { it.uri }) { entry ->
                    VideoRow(entry, onClick = { onSelect(entry) }, onRemove = { onRemove(entry) }, modifier = Modifier.animateItem())
                    HorizontalDivider(Modifier.padding(start = 16.dp + ThumbWidth + 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

private val ThumbWidth = 128.dp
private val ContentMaxWidth = 720.dp

@Composable
private fun VideoRow(entry: VideoEntry, onClick: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(entry, Modifier.width(ThumbWidth))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                progressLine(entry),
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TabularNumbers),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val marks = Slot.entries.mapNotNull { s -> entry.sub(s)?.let { s to tabMark(it, s) } }
            if (marks.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    marks.forEach { (slot, mark) -> SlotMark(mark, slot, filled = slot == entry.activeSlot) }
                }
            }
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.remove_from_library))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.remove_from_library)) },
                    onClick = { menu = false; onRemove() },
                )
            }
        }
    }
}

@Composable
private fun progressLine(entry: VideoEntry): String {
    val duration = if (entry.durationMs > 0) TimeFormat.format(entry.durationMs) else null
    val p = entry.progress
    val state = when {
        p == null || entry.positionMs < 5_000 -> stringResource(R.string.not_started)
        p > 0.97f -> stringResource(R.string.finished)
        else -> stringResource(R.string.watched_percent, (p * 100).toInt())
    }
    return listOfNotNull(duration, state).joinToString(" · ")
}

@Composable
fun Thumbnail(entry: VideoEntry, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, entry.uri) {
        value = Files.thumbnail(context, entry.uri, entry.positionMs)
    }
    Box(
        modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val b = bitmap
        if (b != null) {
            Image(b.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Rounded.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
        val p = entry.progress
        if (p != null && entry.positionMs >= 5_000) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(p).background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.widthIn(max = 420.dp).animateContentSize()) {
            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.empty_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(96.dp))
        }
    }
}
