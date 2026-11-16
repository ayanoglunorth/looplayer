package app.looplayer

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.looplayer.data.DeviceFolder
import app.looplayer.data.DeviceVideos
import app.looplayer.data.SiblingSubtitles
import app.looplayer.ui.theme.Ic
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.core.view.WindowCompat
import app.looplayer.data.AttachResult
import app.looplayer.data.Files
import app.looplayer.data.LibraryStore
import app.looplayer.data.SettingsStore
import app.looplayer.data.SubtitleSession
import app.looplayer.data.VideoEntry
import app.looplayer.subtitle.Slot
import app.looplayer.ui.library.LibraryScreen
import app.looplayer.ui.library.SetupSheet
import app.looplayer.ui.player.PlayerScreen
import app.looplayer.ui.settings.Group
import app.looplayer.ui.settings.IconRow
import app.looplayer.ui.settings.SettingsSheet
import app.looplayer.ui.settings.SheetFrame
import app.looplayer.ui.theme.LooplayerTheme
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    /** A video handed to us by another app ("Open with"), consumed once by the UI. */
    private var incoming by mutableStateOf<Uri?>(null)

    /** The in-app language choice wins over the system one; resources are picked here, once per activity. */
    override fun attachBaseContext(base: Context) {
        val chosen = SettingsStore.savedLanguage(base) ?: return super.attachBaseContext(base)
        val locale = Locale.forLanguageTag(chosen.tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incoming = intent.viewUri()
        val settings = SettingsStore.get(this)
        setContent {
            LooplayerTheme(dark = settings.value.dark ?: isSystemInDarkTheme()) {
                App(incoming, onIncomingHandled = { incoming = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.viewUri()?.let { incoming = it }
    }

    private fun Intent.viewUri(): Uri? = if (action == Intent.ACTION_VIEW) data else null
}

private const val MEDIA_STORE_VIDEO = "content://media/external/video/media/"

private enum class Screen { Library, Setup, Player }


@Composable
private fun App(incoming: Uri?, onIncomingHandled: () -> Unit) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val store = remember { LibraryStore.get(context) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var screen by remember { mutableStateOf(Screen.Library) }
    var session by remember { mutableStateOf<SubtitleSession?>(null) }
    var pendingSlot by remember { mutableStateOf(Slot.A) }
    var settingsOpen by remember { mutableStateOf(false) }
    var chooserOpen by remember { mutableStateOf(false) }
    val settings = remember { SettingsStore.get(context) }

    var folders by remember { mutableStateOf<List<DeviceFolder>>(emptyList()) }
    var hasAccess by remember { mutableStateOf(DeviceVideos.hasAccess(context)) }
    fun refreshFolders() {
        hasAccess = DeviceVideos.hasAccess(context)
        if (hasAccess) scope.launch { folders = DeviceVideos.load(context) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshFolders() }
    val askAccess = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshFolders() }

    // Bar icons follow the theme; the player is always dark.
    val view = LocalView.current
    val darkUi = (settings.value.dark ?: isSystemInDarkTheme()) || screen == Screen.Player
    SideEffect {
        val window = (context as? android.app.Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkUi
            isAppearanceLightNavigationBars = !darkUi
        }
    }

    fun open(entry: VideoEntry, then: Screen, persist: Boolean = true) {
        val s = SubtitleSession(
            resolver, store, entry, persist,
            discover = { SiblingSubtitles.find(context, Uri.parse(entry.uri)).map(Uri::fromFile) },
        )
        s.touch()
        session = s
        screen = then
        scope.launch { s.restore() }
    }

    fun openUri(uri: Uri, then: Screen) {
        val kept = Files.takePermission(resolver, uri)
        val key = uri.toString()
        val entry = store.find(key) ?: VideoEntry(uri = key, title = Files.titleFrom(Files.displayName(resolver, uri)))
        // A temporary "Open with" grant can't be reopened from the library later, so it isn't stored.
        open(entry, then, persist = kept || store.find(key) != null || key.startsWith(MEDIA_STORE_VIDEO))
    }

    LaunchedEffect(incoming) {
        if (incoming != null) {
            openUri(incoming, Screen.Player)
            onIncomingHandled()
        }
    }

    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) openUri(uri, Screen.Setup)
    }
    val pickGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) openUri(uri, Screen.Setup)
    }
    val pickSubtitle = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val s = session
        if (uri != null && s != null) {
            scope.launch {
                val message = when (s.attach(pendingSlot, uri)) {
                    AttachResult.OK -> null
                    AttachResult.EMPTY -> context.getString(R.string.subtitle_empty)
                    AttachResult.FAILED -> context.getString(R.string.subtitle_load_failed)
                }
                message?.let { snackbar.showSnackbar(it) }
            }
        }
    }
    val chooseSubtitle: (Slot) -> Unit = { slot ->
        pendingSlot = slot
        // Subtitle files come with unreliable MIME types (text/plain, application/x-subrip, octet-stream).
        pickSubtitle.launch(arrayOf("*/*"))
    }

    Box(Modifier.fillMaxSize()) {
        val current = session
        if (screen == Screen.Player && current != null) {
            // Keyed on the session: opening another video must build a fresh player, never reuse the old one.
            key(current) {
                LooplayerTheme(dark = true) {
                    PlayerScreen(
                        session = current,
                        onPickSubtitle = chooseSubtitle,
                        onExit = { screen = Screen.Library },
                        onChooseAnother = {
                            screen = Screen.Library
                            chooserOpen = true
                        },
                    )
                }
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
                    LibraryScreen(
                        entries = store.entries,
                        snackbar = snackbar,
                        onOpenSettings = { settingsOpen = true },
                        folders = folders,
                        hasAccess = hasAccess,
                        onRequestAccess = { askAccess.launch(DeviceVideos.permission) },
                        onOpenVideo = { chooserOpen = true },
                        onSelect = { entry ->
                            if (Files.canRead(resolver, entry.uri)) {
                                open(entry, Screen.Setup)
                            } else {
                                scope.launch { snackbar.showSnackbar(context.getString(R.string.file_unavailable)) }
                            }
                        },
                        onRemove = { entry ->
                            store.remove(entry.uri)
                            scope.launch {
                                val result = snackbar.showSnackbar(
                                    message = context.getString(R.string.removed_from_library),
                                    actionLabel = context.getString(R.string.undo),
                                    duration = SnackbarDuration.Short,
                                )
                                if (result == SnackbarResult.ActionPerformed) store.upsert(entry) else Files.releasePermissions(resolver, entry)
                            }
                        },
                    )
                }
            }
            if (screen == Screen.Setup && current != null) {
                SetupSheet(
                    session = current,
                    onPickSubtitle = chooseSubtitle,
                    onPlay = { screen = Screen.Player },
                    onDismiss = { screen = Screen.Library },
                )
            }
            if (chooserOpen) {
                OpenChooser(
                    onGallery = { chooserOpen = false; pickGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                    onFiles = { chooserOpen = false; pickVideo.launch(arrayOf("video/*")) },
                    onDismiss = { chooserOpen = false },
                )
            }
        }
        if (settingsOpen && screen != Screen.Player) SettingsSheet(settings, onDismiss = { settingsOpen = false })
        if (screen == Screen.Player) {
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** How to bring a video in: the gallery, or the file picker. */
@Composable
private fun OpenChooser(onGallery: () -> Unit, onFiles: () -> Unit, onDismiss: () -> Unit) {
    SheetFrame(null, onDismiss) {
        Group(null) {
            IconRow(Ic.Movie, stringResource(R.string.pick_gallery), null, onGallery)
            IconRow(Ic.Folder, stringResource(R.string.pick_files), null, onFiles)
        }
    }
}
