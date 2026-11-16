package app.looplayer.ui.library

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.runtime.saveable.rememberSaveable
import app.looplayer.data.DeviceFolder
import app.looplayer.data.DeviceVideo
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
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
import app.looplayer.ui.theme.Palette
import app.looplayer.ui.theme.Ic
import app.looplayer.ui.theme.TabularNumbers

private const val OPENED_FOLDER = -1L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    entries: List<VideoEntry>,
    snackbar: SnackbarHostState,
    onOpenVideo: () -> Unit,
    onSelect: (VideoEntry) -> Unit,
    onRemove: (VideoEntry) -> Unit,
    onOpenSettings: () -> Unit = {},
    folders: List<DeviceFolder> = emptyList(),
    hasAccess: Boolean = true,
    onRequestAccess: () -> Unit = {},
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var openFolder by rememberSaveable { mutableStateOf<Long?>(null) }

    val stored = entries.associateBy { it.uri }
    val onDevice = folders.flatMapTo(HashSet()) { f -> f.videos.map { it.uri } }
    // Files opened from elsewhere (the picker, a download, "Open with") aren't in the media index.
    val opened = entries.filter { it.uri !in onDevice }
    val openedName = stringResource(R.string.opened_files)
    val shelf = buildList {
        if (opened.isNotEmpty()) add(DeviceFolder(OPENED_FOLDER, openedName, opened.map { DeviceVideo(it.uri, it.title, it.durationMs, 0, 0) }))
        addAll(folders)
    }
    val folder = shelf.firstOrNull { it.id == openFolder }
    BackHandler(enabled = folder != null) { openFolder = null }

    val continuing = entries.filter { it.resumeMs > 0 && (it.progress ?: 0f) < 0.97f }.take(12)

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            // Same 720dp column as the list, so heading and rows share one left edge on tablets.
            Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
                LargeTopAppBar(
                    title = { Text(folder?.name ?: stringResource(R.string.library_title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        if (folder != null) {
                            IconButton(onClick = { openFolder = null }) {
                                Icon(painterResource(Ic.Back), contentDescription = stringResource(R.string.folder_back))
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(painterResource(Ic.Settings), contentDescription = stringResource(R.string.settings))
                        }
                    },
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
                icon = { Icon(painterResource(Ic.Add), contentDescription = null) },
                text = { Text(stringResource(R.string.open_video)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            modifier = Modifier.fillMaxSize().wrapContentWidth().widthIn(max = ContentMaxWidth),
        ) {
            if (folder != null) {
                items(folder.videos, key = { it.uri }) { video ->
                    val entry = stored[video.uri] ?: VideoEntry(video.uri, video.title, video.durationMs)
                    VideoRow(
                        entry, onClick = { onSelect(entry) },
                        onRemove = if (video.uri in stored) ({ onRemove(entry) }) else null,
                        modifier = Modifier.animateItem(),
                    )
                }
            } else {
                if (!hasAccess) item(key = "access") { AccessCard(onRequestAccess) }
                if (continuing.isNotEmpty()) {
                    item(key = "continue-head") { SectionLabel(R.string.library_continue) }
                    item(key = "continue") {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(continuing, key = { it.uri }) { entry -> ContinueCard(entry) { onSelect(entry) } }
                        }
                    }
                }
                if (shelf.isNotEmpty()) item(key = "folders-head") { SectionLabel(R.string.library_folders) }
                items(shelf, key = { "f${it.id}" }) { f -> FolderRow(f) { openFolder = f.id } }
                if (shelf.isEmpty()) item(key = "empty") { EmptyLibrary() }
            }
        }
    }
}

private val ThumbWidth = 128.dp
private val ContentMaxWidth = 720.dp

@Composable
private fun SectionLabel(res: Int) = Text(
    stringResource(res),
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
)

/** Asks once for permission to list the device's videos; without it the library still works from the file picker. */
@Composable
private fun AccessCard(onGrant: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(stringResource(R.string.device_access_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.device_access_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onGrant, shape = RoundedCornerShape(50)) { Text(stringResource(R.string.grant)) }
    }
}

@Composable
private fun ContinueCard(entry: VideoEntry, onClick: () -> Unit) {
    Column(Modifier.width(196.dp).clip(MaterialTheme.shapes.small).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Thumbnail(entry, Modifier.fillMaxWidth())
        Column(Modifier.padding(horizontal = 2.dp)) {
            Text(entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (entry.durationMs > 0) {
                Text(
                    stringResource(R.string.time_left, TimeFormat.format(entry.durationMs - entry.positionMs)),
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TabularNumbers),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FolderRow(folder: DeviceFolder, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) { Icon(painterResource(Ic.Folder), contentDescription = null) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                pluralStringResource(R.plurals.video_count, folder.videos.size, folder.videos.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VideoRow(entry: VideoEntry, onClick: () -> Unit, onRemove: (() -> Unit)?, modifier: Modifier = Modifier) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = if (onRemove != null) 4.dp else 16.dp),
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
        if (onRemove != null) {
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(painterResource(Ic.More), contentDescription = stringResource(R.string.remove_from_library))
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
            Icon(painterResource(Ic.Movie), contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
        val p = entry.progress
        if (p != null && entry.positionMs >= 5_000) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(p).background(Palette.Progress))
            }
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 48.dp)) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.widthIn(max = 420.dp).animateContentSize()) {
            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.empty_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
