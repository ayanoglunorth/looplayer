package app.looplayer.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.provider.Settings
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
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
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.text.CueGroup
import androidx.media3.common.Tracks
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.looplayer.R
import androidx.compose.runtime.rememberCoroutineScope
import app.looplayer.data.AttachResult
import app.looplayer.data.EmbeddedSubtitles
import kotlinx.coroutines.launch
import java.io.File
import app.looplayer.data.AppSettings
import app.looplayer.data.OrientationMode
import app.looplayer.data.SettingsStore
import app.looplayer.data.SubtitleBackdrop
import app.looplayer.data.SubtitleSession
import app.looplayer.data.languageName
import app.looplayer.data.tabMark
import app.looplayer.subtitle.SeekMath
import app.looplayer.subtitle.Slot
import app.looplayer.subtitle.TimeFormat
import app.looplayer.ui.components.EdgeIndexTabs
import app.looplayer.ui.components.LanguageSwitch
import app.looplayer.ui.components.SlotUi
import app.looplayer.ui.components.SubtitleSlots
import app.looplayer.ui.settings.Group
import app.looplayer.ui.settings.NavRow
import app.looplayer.ui.settings.Segmented
import app.looplayer.ui.settings.SettingsSheet
import app.looplayer.ui.settings.SheetFrame
import app.looplayer.ui.settings.SubtitleStyleSheet
import app.looplayer.ui.theme.family
import app.looplayer.ui.theme.Ic
import app.looplayer.ui.theme.color
import app.looplayer.ui.theme.Palette
import app.looplayer.ui.theme.TabularNumbers
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val Speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f, 4f, 6f, 8f)

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    session: SubtitleSession,
    onPickSubtitle: (Slot) -> Unit,
    onExit: () -> Unit,
    onChooseAnother: () -> Unit = onExit,
    settingsStore: SettingsStore = SettingsStore.get(LocalContext.current),
) {
    val context = LocalContext.current
    val settings = settingsStore.value
    val settingsNow = rememberUpdatedState(settings)
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
    var moreOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var styleOpen by remember { mutableStateOf(false) }
    val anySheet = sheetOpen || moreOpen || settingsOpen || styleOpen
    var audioChoices by remember { mutableStateOf(emptyList<AudioChoice>()) }
    // Subtitle tracks muxed into the video; at most one is shown, in place of the attached files.
    var embeddedChoices by remember { mutableStateOf(emptyList<AudioChoice>()) }
    var embeddedLine by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var sleepMinutes by remember { mutableIntStateOf(0) }
    var loopA by remember { mutableStateOf<Long?>(null) }
    var loopB by remember { mutableStateOf<Long?>(null) }
    var locked by remember { mutableStateOf(false) }
    var unlockShown by remember { mutableStateOf(false) }
    var boosting by remember { mutableStateOf(false) }
    var adjust by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
    var adjustActive by remember { mutableStateOf(false) }
    var adjustBrightness by remember { mutableStateOf(true) }
    var adjustFrom by remember { mutableFloatStateOf(0f) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING || state == Player.STATE_IDLE
                if (player.duration > 0) durationMs = player.duration
                if (state == Player.STATE_ENDED) controlsVisible = true
            }
            override fun onPlayerError(e: PlaybackException) { error = e }
            override fun onTracksChanged(tracks: Tracks) {
                audioChoices = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }.mapIndexed { i, g ->
                    val f = g.getTrackFormat(0)
                    val label = f.label ?: languageName(f.language) ?: context.getString(R.string.audio_n, i + 1)
                    AudioChoice(label, g, g.isSelected)
                }
                embeddedChoices = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT && it.isSupported }.mapIndexed { i, g ->
                    val f = g.getTrackFormat(0)
                    val label = f.label ?: languageName(f.language) ?: context.getString(R.string.embedded_n, i + 1)
                    AudioChoice(label, g, g.isSelected)
                }
            }
            override fun onCues(cueGroup: CueGroup) {
                embeddedLine = cueGroup.cues.mapNotNull { it.text?.toString() }.joinToString("\n").ifBlank { null }
            }
            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) {
                    videoLandscape = size.width * size.pixelWidthHeightRatio >= size.height
                    if (!rotationLocked) activity?.requestedOrientation = orientationFor(videoLandscape, settingsNow.value.orientation)
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
            window?.let { w -> w.attributes = w.attributes.also { it.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } }
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // In the small window the controls would cover the picture; hide them when it opens.
    DisposableEffect(activity) {
        val host = activity as? androidx.core.app.OnPictureInPictureModeChangedProvider
        val listener = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { if (it.isInPictureInPictureMode) controlsVisible = false }
        host?.addOnPictureInPictureModeChangedListener(listener)
        onDispose { host?.removeOnPictureInPictureModeChangedListener(listener) }
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
            val a = loopA
            val b = loopB
            if (a != null && b != null && positionMs >= b) {
                player.seekTo(a)
                positionMs = a
            }
            val step = if (player.isPlaying) 40L else 200L
            delay(step)
            sinceSave += step
            if (sinceSave >= 10_000) {
                sinceSave = 0
                session.savePosition(positionMs, durationMs)
            }
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, interaction, anySheet, settings.controlsTimeoutSec) {
        if (controlsVisible && isPlaying && !anySheet) {
            delay(settings.controlsTimeoutSec * 1_000L)
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

    LaunchedEffect(sleepMinutes) {
        if (sleepMinutes > 0) {
            delay(sleepMinutes * 60_000L)
            player.pause()
            sleepMinutes = 0
            controlsVisible = true
            hint = R.string.sleep_done
        }
    }
    LaunchedEffect(unlockShown) {
        if (unlockShown) {
            delay(2_500)
            unlockShown = false
        }
    }
    LaunchedEffect(adjustActive) {
        if (!adjustActive) {
            delay(700)
            adjust = null
        }
    }

    BackHandler { if (locked) unlockShown = true else onExit() }

    val dual = session.dual
    val marks = Slot.entries.mapNotNull { s -> session.tracks[s]?.let { s to tabMark(session.entry.sub(s), s) } }.toMap()

    val embedded = embeddedChoices.firstOrNull { it.selected }
    val selectEmbedded: (AudioChoice?) -> Unit = { choice ->
        val b = player.trackSelectionParameters.buildUpon()
        player.trackSelectionParameters = if (choice == null) {
            b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).clearOverridesOfType(C.TRACK_TYPE_TEXT).build()
        } else {
            b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(choice.group.mediaTrackGroup, 0)).build()
        }
        if (choice == null) embeddedLine = null
    }

    // Copies a built-in subtitle stream into a language slot, so it joins the double-tap language switch.
    suspend fun import(choice: AudioChoice, slot: Slot): Boolean {
        val file = runCatching {
            EmbeddedSubtitles.extractToFile(
                context, Uri.parse(session.entry.uri), choice.trackId, choice.language,
                File(context.filesDir, "embedded"),
            )
        }.getOrNull()
        return file != null && session.attach(slot, Uri.fromFile(file)) == AttachResult.OK
    }
    val importEmbedded: (AudioChoice, Slot) -> Unit = { choice, slot ->
        if (!importing) {
            importing = true
            scope.launch {
                val ok = import(choice, slot)
                if (ok) selectEmbedded(null)
                hint = if (ok) R.string.embedded_imported else R.string.embedded_import_failed
                importing = false
            }
        }
    }

    // A video that brings its own subtitles and has none attached starts with the first two in the language slots.
    var autoImported by remember { mutableStateOf(false) }
    LaunchedEffect(embeddedChoices.size) {
        if (autoImported || embeddedChoices.isEmpty()) return@LaunchedEffect
        delay(1_500) // let the library entry and folder subtitles load first
        if (session.tracks.isNotEmpty() || session.entry.subA != null || session.entry.subB != null) return@LaunchedEffect
        autoImported = true
        importing = true
        embeddedChoices.take(2).zip(Slot.entries).forEach { (choice, slot) -> import(choice, slot) }
        importing = false
    }

    val switchLanguage = rememberUpdatedState {
        if (embedded != null && session.tracks.isNotEmpty()) {
            // Double tap leaves the built-in track and goes back to the attached languages.
            selectEmbedded(null)
            tabFlash++
        } else if (session.switch()) {
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
                    if (locked) PlayerGestureHandler(
                        onTap = { unlockShown = !unlockShown },
                        onDoubleTap = {},
                        onSeekStart = {},
                        onSeek = { _, _ -> },
                        onSeekEnd = {},
                        onSeekCancel = {},
                    ) else PlayerGestureHandler(
                        onHoldStart = {
                            if (settings.holdBoost && isPlaying) {
                                boosting = true
                                player.setPlaybackSpeed(settings.holdSpeed)
                            }
                        },
                        onHoldEnd = {
                            if (boosting) {
                                boosting = false
                                player.setPlaybackSpeed(speed)
                            }
                        },
                        onAdjustStart = { fx ->
                            adjustActive = settings.swipeAdjust
                            if (adjustActive) {
                                adjustBrightness = fx < 0.5f
                                adjustFrom = if (adjustBrightness) {
                                    val b = activity?.window?.attributes?.screenBrightness ?: -1f
                                    if (b >= 0f) b else Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
                                } else {
                                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
                                        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                }
                            }
                        },
                        onAdjust = { drag, height ->
                            if (adjustActive) {
                                val v = (adjustFrom - drag / height * 1.2f).coerceIn(0f, 1f)
                                if (adjustBrightness) {
                                    activity?.window?.let { w -> w.attributes = w.attributes.also { it.screenBrightness = v.coerceAtLeast(0.02f) } }
                                } else {
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC, (v * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).roundToInt(), 0,
                                    )
                                }
                                adjust = adjustBrightness to (v * 100).roundToInt()
                            }
                        },
                        onAdjustEnd = { adjustActive = false },
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
        val extraLift = LocalConfiguration.current.screenHeightDp.dp * (settings.position / 100f)
        val lift by animateDpAsState(if (controlsVisible) 116.dp else 28.dp, tween(220), label = "lift")
        AnimatedContent(
            targetState = dual.active,
            transitionSpec = {
                if (settings.switchAnimation) fadeIn(tween(160)) togetherWith fadeOut(tween(90))
                else EnterTransition.None togetherWith ExitTransition.None
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Above the controls' scrim, so the line keeps full contrast while the controls are up.
                .zIndex(1f)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = lift + extraLift, start = 32.dp, end = 32.dp),
            label = "subtitle",
        ) { slot ->
            val text = when {
                embedded != null -> embeddedLine
                session.visible -> session.tracks[slot]?.textAt(positionMs)
                else -> null
            }
            SubtitleLine(text, settings)
        }

        // Thumb index on the right edge: appears with the controls or briefly after a switch.
        AnimatedVisibility(
            visible = tabsShown && marks.isNotEmpty() && settings.languageIndicator,
            enter = if (settings.switchAnimation) slideInHorizontally(tween(200)) { it } + fadeIn(tween(200)) else EnterTransition.None,
            exit = if (settings.switchAnimation) slideOutHorizontally(tween(160)) { it } + fadeOut(tween(160)) else ExitTransition.None,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            EdgeIndexTabs(marks = marks, active = dual.active, large = LocalConfiguration.current.screenWidthDp >= 840, animate = settings.switchAnimation)
        }

        // Drag-to-seek readout.
        seekPreview?.let { target ->
            SeekReadout(target, seekFrom, durationMs, Modifier.align(Alignment.Center))
        }

        AnimatedVisibility(
            visible = controlsVisible && !locked && seekPreview == null && error == null,
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
                skipSeconds = settings.skipSeconds,
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
                        if (rotationLocked) ActivityInfo.SCREEN_ORIENTATION_LOCKED else orientationFor(videoLandscape, settings.orientation)
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
                onPrevCue = dual.activeTrack?.let { t ->
                    {
                        t.previousCueStart(player.currentPosition)?.let { player.seekTo(it); positionMs = it }
                        interaction++
                    }
                },
                onNextCue = dual.activeTrack?.let { t ->
                    {
                        t.nextCueStart(player.currentPosition)?.let { player.seekTo(it); positionMs = it }
                        interaction++
                    }
                },
                onMore = { moreOpen = true },
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
            val needsSubtitle = last.intValue == R.string.only_one_subtitle || last.intValue == R.string.no_subtitles
            HintPill(
                stringResource(last.intValue),
                actionLabel = if (needsSubtitle) stringResource(R.string.add) else null,
                onAction = { hint = null; sheetOpen = true },
            )
        }

        if (boosting) {
            HintPill(
                stringResource(R.string.boost_speed, settings.holdSpeed.toString().removeSuffix(".0")),
                Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 24.dp),
            )
        }
        adjust?.let { (brightness, pct) ->
            HintPill(
                stringResource(if (brightness) R.string.brightness_pct else R.string.volume_pct, pct),
                Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 24.dp),
            )
        }

        AnimatedVisibility(
            visible = locked && unlockShown,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.CenterStart).windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 16.dp),
        ) {
            IconButton(
                onClick = { locked = false; unlockShown = false; controlsVisible = true; interaction++ },
                modifier = Modifier.size(56.dp).clip(CircleShape),
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.18f)),
            ) {
                Icon(painterResource(Ic.Lock), contentDescription = stringResource(R.string.unlock_touch), tint = Color.White)
            }
        }

        error?.let { PlaybackError(onBack = onExit, onChooseAnother = onChooseAnother, modifier = Modifier.fillMaxSize()) }
    }

    if (sheetOpen) {
        PlayerSubtitleSheet(
            session, onPickSubtitle, onDismiss = { sheetOpen = false },
            embedded = embeddedChoices, onEmbedded = selectEmbedded, onImport = importEmbedded, importing = importing,
            onOpenStyle = { sheetOpen = false; styleOpen = true },
        )
    }
    if (moreOpen) {
        MoreSheet(
            audio = audioChoices,
            onAudio = { choice ->
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setOverrideForType(TrackSelectionOverride(choice.group.mediaTrackGroup, 0)).build()
            },
            sleepMinutes = sleepMinutes,
            onSleep = { sleepMinutes = it },
            loopA = loopA,
            loopB = loopB,
            onLoop = {
                val now = player.currentPosition
                val a = loopA
                when {
                    a == null -> { loopA = now; hint = R.string.loop_a_set }
                    loopB == null && now > a + 500 -> { loopB = now; hint = R.string.loop_on }
                    loopB == null -> { loopA = null; hint = R.string.loop_b_error }
                    else -> { loopA = null; loopB = null; hint = R.string.loop_off }
                }
                moreOpen = false
            },
            onPip = {
                moreOpen = false
                activity?.enterPictureInPictureMode(
                    android.app.PictureInPictureParams.Builder().setAspectRatio(android.util.Rational(16, 9)).build(),
                )
            },
            onLock = { moreOpen = false; locked = true; controlsVisible = false },
            onSettings = { moreOpen = false; settingsOpen = true },
            onDismiss = { moreOpen = false },
        )
    }
    if (settingsOpen) SettingsSheet(settingsStore, onDismiss = { settingsOpen = false })
    if (styleOpen) SubtitleStyleSheet(settingsStore, onDismiss = { styleOpen = false })
}

/** The subtitles sheet opened from the player's top bar. */
@Composable
internal fun PlayerSubtitleSheet(
    session: SubtitleSession,
    onPickSubtitle: (Slot) -> Unit,
    onDismiss: () -> Unit,
    onOpenStyle: (() -> Unit)? = null,
    embedded: List<AudioChoice> = emptyList(),
    onEmbedded: (AudioChoice?) -> Unit = {},
    onImport: (AudioChoice, Slot) -> Unit = { _, _ -> },
    importing: Boolean = false,
) {
    SheetFrame(stringResource(R.string.subtitles), onDismiss) {
        Group(null) {
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
        if (embedded.isNotEmpty()) {
            Group(stringResource(R.string.embedded_subtitles)) {
                Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Segmented(listOf<AudioChoice?>(null) + embedded, embedded.firstOrNull { it.selected }, { it?.label ?: stringResource(R.string.embedded_off) }, scroll = true) {
                        onEmbedded(it)
                    }
                    Text(
                        stringResource(if (importing) R.string.embedded_importing else R.string.embedded_import_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                embedded.forEach { choice ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(choice.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Slot.entries.forEach { slot ->
                            TextButton(onClick = { onImport(choice, slot) }, enabled = !importing) {
                                Text(stringResource(if (slot == Slot.A) R.string.embedded_to_first else R.string.embedded_to_second))
                            }
                        }
                    }
                }
            }
        }
        if (onOpenStyle != null) {
            Group(null) { NavRow(stringResource(R.string.section_subtitle_style), stringResource(R.string.subtitle_style_body), onOpenStyle) }
        }
    }
}

/** Subtitle text drawn in the user's chosen font, size, colour and backdrop (default: white with a dark outline). */
@Composable
internal fun SubtitleLine(text: String?, settings: AppSettings = AppSettings()) {
    if (text.isNullOrBlank()) return
    BoxWithConstraints {
        val size = when {
            maxWidth > 1000.dp -> 30.sp
            maxWidth > 600.dp -> 24.sp
            else -> 19.sp
        } * (settings.sizePercent / 100f)
        val base = TextStyle(
            fontFamily = settings.font.family(),
            fontSize = size,
            lineHeight = size * 1.25f,
            fontWeight = if (settings.bold) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        val tint = settings.color.color().copy(alpha = settings.textOpacity / 100f)
        when (settings.backdrop) {
            SubtitleBackdrop.Outline -> Box(contentAlignment = Alignment.Center) {
                Text(
                    text,
                    style = base.copy(
                        color = Color.Black.copy(alpha = 0.9f),
                        drawStyle = Stroke(width = 5f, join = StrokeJoin.Round),
                    ),
                )
                Text(text, style = base.copy(color = tint, shadow = Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 6f)))
            }
            SubtitleBackdrop.Shadow -> Text(
                text,
                style = base.copy(color = tint, shadow = Shadow(Color.Black.copy(alpha = 0.9f), offset = Offset(2f, 3f), blurRadius = 8f)),
            )
            SubtitleBackdrop.Box -> Text(
                text,
                style = base.copy(color = tint),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = settings.backdropOpacity / 100f))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
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
    onPrevCue: (() -> Unit)? = null,
    onNextCue: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    skipSeconds: Int = 10,
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
                    Icon(painterResource(Ic.Back), contentDescription = stringResource(R.string.back), tint = Color.White)
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
                    Icon(painterResource(Ic.Subtitles), contentDescription = stringResource(R.string.subtitles), tint = Color.White)
                }
                if (onMore != null) {
                    IconButton(onClick = onMore) {
                        Icon(painterResource(Ic.More), contentDescription = stringResource(R.string.more_options), tint = Color.White)
                    }
                }
            }

            // Centre: skip back, play/pause, skip forward. Visible equivalents of the drag gesture.
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
                verticalAlignment = BiasAlignment.Vertical(-0.45f),
            ) {
                IconButton(onClick = { onSkip(-skipSeconds * 1_000L) }, modifier = Modifier.size(56.dp)) {
                    SkipIcon(forward = false, seconds = skipSeconds)
                }
                Box(contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(76.dp).clip(CircleShape),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.14f)),
                    ) {
                        Icon(
                            if (isPlaying) painterResource(Ic.Pause) else painterResource(Ic.Play),
                            contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                            tint = Color.White,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                    if (buffering) CircularProgressIndicator(Modifier.size(76.dp), color = Color.White.copy(alpha = 0.6f), strokeWidth = 2.dp)
                }
                IconButton(onClick = { onSkip(skipSeconds * 1_000L) }, modifier = Modifier.size(56.dp)) {
                    SkipIcon(forward = true, seconds = skipSeconds)
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
                if (marks.isNotEmpty() && onPrevCue != null && onNextCue != null) {
                    IconButton(onClick = onPrevCue) {
                        Icon(painterResource(Ic.PreviousCue), contentDescription = stringResource(R.string.prev_cue), tint = Color.White)
                    }
                    IconButton(onClick = onNextCue) {
                        Icon(painterResource(Ic.NextCue), contentDescription = stringResource(R.string.next_cue), tint = Color.White)
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRotationLock) {
                    Icon(
                        if (rotationLocked) painterResource(Ic.RotationLocked) else painterResource(Ic.RotationFree),
                        contentDescription = stringResource(if (rotationLocked) R.string.unlock_rotation else R.string.lock_rotation),
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onZoom) {
                    Icon(
                        if (zoom) painterResource(Ic.Fit) else painterResource(Ic.Fill),
                        contentDescription = stringResource(if (zoom) R.string.fit_picture else R.string.fill_picture),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

/** A turning arrow with "10" inside: Lucide has no numbered replay icon, so the number is set in Inter. */
@Composable
private fun SkipIcon(forward: Boolean, seconds: Int) {
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        Icon(
            painterResource(if (forward) Ic.SkipForward else Ic.SkipBack),
            contentDescription = TimeFormat.formatDelta(if (forward) seconds * 1_000L else -seconds * 1_000L),
            tint = Color.White,
            modifier = Modifier.size(32.dp),
        )
        Text("$seconds", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = Color.White)
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

private fun orientationFor(landscape: Boolean, mode: OrientationMode) = when {
    mode == OrientationMode.Sensor -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
    landscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
}

internal fun Context.findActivity(): Activity? {
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
            .clip(RoundedCornerShape(16.dp))
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
