package app.looplayer.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.looplayer.R
import app.looplayer.data.SubtitleSession
import app.looplayer.data.tabMark
import app.looplayer.subtitle.SeekMath
import app.looplayer.subtitle.Slot
import app.looplayer.subtitle.TimeFormat
import app.looplayer.ui.components.EdgeIndexTabs
import app.looplayer.ui.components.LanguageSwitch
import app.looplayer.ui.components.SlotUi
import app.looplayer.ui.components.SubtitleSlots
import app.looplayer.ui.theme.Inter
import app.looplayer.ui.theme.Palette
import app.looplayer.ui.theme.TabularNumbers
import kotlinx.coroutines.delay

private val Speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
private const val CONTROLS_TIMEOUT_MS = 3_000L

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    session: SubtitleSession,
    onPickSubtitle: (Slot) -> Unit,
    onExit: () -> Unit,
    onChooseAnother: () -> Unit = onExit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }

    val player = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(session.entry.uri)))
                // Subtitles are drawn by this screen, never by the player.
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
                prepare()
                seekTo(session.entry.resumeMs)
                playWhenReady = true
            }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<PlaybackException?>(null) }
    var positionMs by remember { mutableLongStateOf(session.entry.resumeMs) }
    var durationMs by remember { mutableLongStateOf(session.entry.durationMs) }
    var speed by remember { mutableFloatStateOf(1f) }
    var zoom by remember { mutableStateOf(false) }
    var rotationLocked by remember { mutableStateOf(false) }
    var videoLandscape by remember { mutableStateOf(true) }

    var controlsVisible by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }  // bump to restart the hide timer
    var tabFlash by remember { mutableIntStateOf(0) }
    var tabsShown by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<Int?>(null) }
    var seekPreview by remember { mutableStateOf<Long?>(null) }
    var seekFrom by remember { mutableLongStateOf(0L) }
    var lastLiveSeek by remember { mutableLongStateOf(0L) }
    var sliderDrag by remember { mutableStateOf<Float?>(null) }
    var sheetOpen by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING || state == Player.STATE_IDLE
                if (player.duration > 0) durationMs = player.duration
                if (state == Player.STATE_ENDED) controlsVisible = true
            }
            override fun onPlayerError(e: PlaybackException) { error = e }
            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) {
                    videoLandscape = size.width * size.pixelWidthHeightRatio >= size.height
                    if (!rotationLocked) activity?.requestedOrientation = orientationFor(videoLandscape)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            session.savePosition(player.currentPosition, player.duration)
            player.removeListener(listener)
            player.release()
        }
    }

    // Full screen: hide system bars (a swipe from the edge shows them briefly), restore on exit.
    DisposableEffect(activity) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(isPlaying) {
        view.keepScreenOn = isPlaying
        onDispose { view.keepScreenOn = false }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                player.pause()
                controlsVisible = true
                session.savePosition(player.currentPosition, player.duration)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // The clock that drives subtitles and the scrubber.
    LaunchedEffect(player) {
        var sinceSave = 0L
        while (true) {
            positionMs = player.currentPosition
            if (player.duration > 0) durationMs = player.duration
            val step = if (player.isPlaying) 40L else 200L
            delay(step)
            sinceSave += step
            if (sinceSave >= 10_000) {
                sinceSave = 0
                session.savePosition(positionMs, durationMs)
            }
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, interaction, sheetOpen) {
        if (controlsVisible && isPlaying && !sheetOpen) {
            delay(CONTROLS_TIMEOUT_MS)
            controlsVisible = false
        }
    }
    LaunchedEffect(tabFlash) {
        if (tabFlash == 0) return@LaunchedEffect
        tabsShown = true
        delay(1_400)
        tabsShown = false
    }
    LaunchedEffect(hint) {
        if (hint != null) {
            delay(2_400)
            hint = null
        }
    }

    BackHandler { onExit() }

    val dual = session.dual
    val marks = Slot.entries.mapNotNull { s -> session.tracks[s]?.let { s to tabMark(session.entry.sub(s), s) } }.toMap()

    val switchLanguage = rememberUpdatedState {
        if (session.switch()) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            tabFlash++
        } else {
            hint = if (session.tracks.isEmpty()) R.string.no_subtitles else R.string.only_one_subtitle
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    subtitleView?.visibility = android.view.View.GONE
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    setKeepContentOnPlayerReset(true)
                    this.player = player
                }
            },
            update = {
                it.resizeMode = if (zoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Gesture surface: tap = controls, double-tap = language, sideways drag = seek.
        Box(
            Modifier
                .fillMaxSize()
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(context.getString(R.string.switch_language)) { switchLanguage.value(); true },
                    )
                }
                .playerGestures(
                    PlayerGestureHandler(
                        onTap = {
                            controlsVisible = !controlsVisible
                            interaction++
                        },
                        onDoubleTap = { switchLanguage.value() },
                        onSeekStart = {
                            seekFrom = player.currentPosition
                            seekPreview = seekFrom
                            player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                        },
                        onSeek = { drag, width ->
                            val target = SeekMath.target(seekFrom, drag, width, player.duration.coerceAtLeast(0))
                            seekPreview = target
                            // Show frames while dragging, but don't flood the decoder with seeks.
                            val now = System.currentTimeMillis()
                            if (now - lastLiveSeek > 120) {
                                lastLiveSeek = now
                                player.seekTo(target)
                            }
                        },
                        onSeekEnd = {
                            player.setSeekParameters(SeekParameters.EXACT)
                            seekPreview?.let { player.seekTo(it); positionMs = it }
                            seekPreview = null
                        },
                        onSeekCancel = {
                            player.setSeekParameters(SeekParameters.EXACT)
                            seekPreview = null
                        },
                    ),
                ),
        )

        // Subtitle line, lifted clear of the control bar while it is up.
        val lift by animateDpAsState(if (controlsVisible) 116.dp else 28.dp, tween(220), label = "lift")
        AnimatedContent(
            targetState = dual.active,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(90)) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = lift, start = 32.dp, end = 32.dp),
            label = "subtitle",
        ) { slot ->
            val text = if (session.visible) session.tracks[slot]?.textAt(positionMs) else null
            SubtitleLine(text)
        }

        // Thumb index on the right edge: appears with the controls or briefly after a switch.
        AnimatedVisibility(
            visible = tabsShown && marks.isNotEmpty(),
            enter = slideInHorizontally(tween(200)) { it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(160)) { it } + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            EdgeIndexTabs(marks = marks, active = dual.active, large = LocalConfiguration.current.screenWidthDp >= 840)
        }

        // Drag-to-seek readout.
        seekPreview?.let { target ->
            SeekReadout(target, seekFrom, durationMs, Modifier.align(Alignment.Center))
        }

        AnimatedVisibility(
            visible = controlsVisible && seekPreview == null && error == null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(140)),
        ) {
            Controls(
                title = session.entry.title,
                isPlaying = isPlaying,
                buffering = buffering,
                position = { sliderDrag?.toLong() ?: positionMs },
                durationMs = durationMs,
                speed = speed,
                zoom = zoom,
                marks = marks,
                active = dual.active,
                onBack = onExit,
                onPlayPause = {
                    if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                    if (player.isPlaying) player.pause() else player.play()
                    interaction++
                },
                onSkip = { delta ->
                    player.seekTo((player.currentPosition + delta).coerceIn(0, durationMs.coerceAtLeast(0)))
                    interaction++
                },
                onScrub = { sliderDrag = it; interaction++ },
                onScrubEnd = {
                    sliderDrag?.let { player.seekTo(it.toLong()); positionMs = it.toLong() }
                    sliderDrag = null
                },
                onSpeed = { speed = it; player.setPlaybackSpeed(it); interaction++ },
                onZoom = { zoom = !zoom; interaction++ },
                rotationLocked = rotationLocked,
                onRotationLock = {
                    rotationLocked = !rotationLocked
                    // Locked: hold whatever way the screen faces now. Unlocked: follow the video again.
                    activity?.requestedOrientation =
                        if (rotationLocked) ActivityInfo.SCREEN_ORIENTATION_LOCKED else orientationFor(videoLandscape)
                    interaction++
                },
                onSelectLanguage = { slot ->
                    if (slot != dual.active) {
                        session.select(slot)
                        tabFlash++
                    }
                    interaction++
                },
                onOpenSubtitles = { sheetOpen = true },
            )
        }

        if (buffering && !controlsVisible && error == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center).size(40.dp), color = Color.White, strokeWidth = 3.dp)
        }

        AnimatedVisibility(
            visible = hint != null,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 72.dp),
        ) {
            val last = remember { mutableIntStateOf(R.string.only_one_subtitle) }
            hint?.let { last.intValue = it }
            HintPill(
                stringResource(last.intValue),
                actionLabel = stringResource(R.string.add),
                onAction = { hint = null; sheetOpen = true },
            )
        }

        error?.let { PlaybackError(onBack = onExit, onChooseAnother = onChooseAnother, modifier = Modifier.fillMaxSize()) }
    }

    if (sheetOpen) PlayerSubtitleSheet(session, onPickSubtitle, onDismiss = { sheetOpen = false })
}

/** The subtitles sheet opened from the player's top bar. */
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerSubtitleSheet(session: SubtitleSession, onPickSubtitle: (Slot) -> Unit, onDismiss: () -> Unit) {
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
            Text(stringResource(R.string.subtitles), style = MaterialTheme.typography.titleLarge)
            SubtitleSlots(
                slots = Slot.entries.associateWith { SlotUi(session.entry.sub(it), session.tracks[it], it in session.loading) },
                active = session.dual.active,
                visible = session.visible,
                onPick = onPickSubtitle,
                onRemove = session::remove,
                onOffset = session::shift,
                onVisibleChange = { session.visible = it },
            )
        }
    }
}

/** Subtitle text: white with a dark outline that holds on any picture. */
@Composable
internal fun SubtitleLine(text: String?, tint: Color = Palette.Subtitle) {
    if (text.isNullOrBlank()) return
    BoxWithConstraints {
        val size = when {
            maxWidth > 1000.dp -> 30.sp
            maxWidth > 600.dp -> 24.sp
            else -> 19.sp
        }
        val base = TextStyle(
            fontFamily = Inter,
            fontSize = size,
            lineHeight = size * 1.25f,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                style = base.copy(
                    color = Color.Black.copy(alpha = 0.9f),
                    drawStyle = Stroke(width = 5f, join = StrokeJoin.Round),
                ),
            )
            Text(
                text,
                style = base.copy(
                    color = tint,
                    shadow = Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 6f),
                ),
            )
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Controls(
    title: String,
    isPlaying: Boolean,
    buffering: Boolean,
    position: () -> Long,
    durationMs: Long,
    speed: Float,
    zoom: Boolean,
    rotationLocked: Boolean,
    marks: Map<Slot, String>,
    active: Slot,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSkip: (Long) -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    onSpeed: (Float) -> Unit,
    onZoom: () -> Unit,
    onRotationLock: () -> Unit,
    onSelectLanguage: (Slot) -> Unit,
    onOpenSubtitles: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        // Scrims exist for legibility of the controls over bright footage.
        Box(
            Modifier.fillMaxWidth().height(140.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))),
        )
        Box(
            Modifier.fillMaxWidth().height(200.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))),
        )

        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 8.dp)) {
            // Top: back, title, speed, subtitles.
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                SpeedMenu(speed, onSpeed)
                IconButton(onClick = onOpenSubtitles) {
                    Icon(Icons.Rounded.Subtitles, contentDescription = stringResource(R.string.subtitles), tint = Color.White)
                }
            }

            // Centre: skip back, play/pause, skip forward. Visible equivalents of the drag gesture.
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
                verticalAlignment = BiasAlignment.Vertical(-0.45f),
            ) {
                IconButton(onClick = { onSkip(-10_000) }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.Replay10, contentDescription = TimeFormat.formatDelta(-10_000), tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Box(contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(76.dp).clip(CircleShape),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.14f)),
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                            tint = Color.White,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                    if (buffering) CircularProgressIndicator(Modifier.size(76.dp), color = Color.White.copy(alpha = 0.6f), strokeWidth = 2.dp)
                }
                IconButton(onClick = { onSkip(10_000) }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.Forward10, contentDescription = TimeFormat.formatDelta(10_000), tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }

            // Bottom: scrubber with times, then the language switch and picture fit.
            ScrubberRow(position, durationMs, onScrub, onScrubEnd)
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 0.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (marks.isNotEmpty()) {
                    LanguageSwitch(marks = marks, active = active, onSelect = onSelectLanguage)
                } else {
                    TextButton(onClick = onOpenSubtitles) {
                        Text(stringResource(R.string.add_subtitle), color = Color.White)
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRotationLock) {
                    Icon(
                        if (rotationLocked) Icons.Rounded.ScreenLockRotation else Icons.Rounded.ScreenRotation,
                        contentDescription = stringResource(if (rotationLocked) R.string.unlock_rotation else R.string.lock_rotation),
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onZoom) {
                    Icon(
                        if (zoom) Icons.Rounded.FitScreen else Icons.Rounded.ZoomOutMap,
                        contentDescription = stringResource(if (zoom) R.string.fit_picture else R.string.fill_picture),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedMenu(speed: Float, onSpeed: (Float) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = stringResource(R.string.speed)
    Box {
        TextButton(onClick = { open = true }) {
            Text(
                speedLabel(speed),
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TabularNumbers),
                color = Color.White,
                modifier = Modifier.semantics { contentDescription = "$label ${speedLabel(speed)}" },
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Speeds.forEach { s ->
                DropdownMenuItem(
                    text = {
                        Text(
                            speedLabel(s),
                            fontWeight = if (s == speed) FontWeight.Bold else FontWeight.Normal,
                            color = if (s == speed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = { open = false; onSpeed(s) },
                )
            }
        }
    }
}

private fun speedLabel(s: Float): String =
    (if (s % 1f == 0f) s.toInt().toString() else s.toString().trimEnd('0')) + "×"

@Composable
internal fun PlaybackError(onBack: () -> Unit, onChooseAnother: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.background(Palette.Ground).windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 420.dp).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.playback_error),
                style = MaterialTheme.typography.titleLarge,
                color = Palette.Text,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.playback_error_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onChooseAnother) { Text(stringResource(R.string.choose_another)) }
            TextButton(onClick = onBack) { Text(stringResource(R.string.back), color = Palette.Muted) }
        }
    }
}

private fun orientationFor(landscape: Boolean) =
    if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

private fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** The drag-to-seek readout: the signed jump, and where it lands. */
@Composable
internal fun SeekReadout(targetMs: Long, fromMs: Long, durationMs: Long, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface.copy(alpha = 0.88f))
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            TimeFormat.formatDelta(targetMs - fromMs),
            style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = TabularNumbers),
            color = Color.White,
        )
        Text(
            "${TimeFormat.format(targetMs)} / ${TimeFormat.format(durationMs)}",
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularNumbers),
            color = Color.White.copy(alpha = 0.75f),
        )
    }
}

/** A short notice over the picture, e.g. when a double-tap has no second language to flip to. */
@Composable
internal fun HintPill(text: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: () -> Unit = {}) {
    // Dark like the seek readout: a notice over a film must never be the brightest thing on screen.
    Row(
        modifier
            .widthIn(max = 480.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.Surface.copy(alpha = 0.88f))
            .heightIn(min = 48.dp)
            .padding(start = 16.dp, end = if (actionLabel != null) 4.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Palette.Text, modifier = Modifier.weight(1f, fill = false))
        if (actionLabel != null) {
            TextButton(onClick = onAction) { Text(actionLabel, color = Palette.Text, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** Its own composable so the 25-per-second clock only recomposes this row, not the whole overlay. */
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScrubberRow(position: () -> Long, durationMs: Long, onScrub: (Float) -> Unit, onScrubEnd: () -> Unit) {
    val positionMs = position()
    val seekLabel = stringResource(R.string.seek_position)
    val scrubColors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color.White.copy(alpha = 0.28f),
    )
    val timeStyle = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TabularNumbers, color = Color.White)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
        Text(TimeFormat.format(positionMs), style = timeStyle, modifier = Modifier.widthIn(min = 52.dp))
        Slider(
            value = positionMs.toFloat().coerceIn(0f, durationMs.coerceAtLeast(1).toFloat()),
            onValueChange = onScrub,
            onValueChangeFinished = onScrubEnd,
            valueRange = 0f..durationMs.coerceAtLeast(1).toFloat(),
            colors = scrubColors,
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    colors = scrubColors,
                    modifier = Modifier.height(4.dp),
                    thumbTrackGapSize = 0.dp,
                    drawStopIndicator = null,
                )
            },
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                .semantics { contentDescription = seekLabel },
        )
        Text(TimeFormat.format(durationMs), style = timeStyle, textAlign = TextAlign.End, modifier = Modifier.widthIn(min = 52.dp))
    }}
