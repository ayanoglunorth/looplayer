package app.looplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.looplayer.data.AttachResult
import app.looplayer.data.Files
import app.looplayer.data.LibraryStore
import app.looplayer.data.SubtitleSession
import app.looplayer.data.VideoEntry
import app.looplayer.subtitle.Slot
import app.looplayer.ui.library.LibraryScreen
import app.looplayer.ui.library.SetupSheet
import app.looplayer.ui.player.PlayerScreen
import app.looplayer.ui.theme.LooplayerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** A video handed to us by another app ("Open with"), consumed once by the UI. */
    private var incoming by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incoming = intent.viewUri()
        setContent {
            LooplayerTheme { App(incoming, onIncomingHandled = { incoming = null }) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.viewUri()?.let { incoming = it }
    }

    private fun Intent.viewUri(): Uri? = if (action == Intent.ACTION_VIEW) data else null
}

private enum class Screen { Library, Setup, Player }

@Composable
private fun App(incoming: Uri?, onIncomingHandled: () -> Unit) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val store = remember { LibraryStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var screen by remember { mutableStateOf(Screen.Library) }
    var session by remember { mutableStateOf<SubtitleSession?>(null) }
    var pendingSlot by remember { mutableStateOf(Slot.A) }

    fun open(entry: VideoEntry, then: Screen) {
        val s = SubtitleSession(resolver, store, entry)
        s.touch()
        session = s
        screen = then
        scope.launch { s.restore() }
    }

    fun openUri(uri: Uri, then: Screen) {
        Files.takePermission(resolver, uri)
        val key = uri.toString()
        val entry = store.find(key) ?: VideoEntry(uri = key, title = Files.titleFrom(Files.displayName(resolver, uri)))
        open(entry, then)
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
            PlayerScreen(
                session = current,
                onPickSubtitle = chooseSubtitle,
                onExit = { screen = Screen.Library },
            )
        } else {
            LibraryScreen(
                entries = store.entries,
                snackbar = snackbar,
                onOpenVideo = { pickVideo.launch(arrayOf("video/*")) },
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
                        if (result == SnackbarResult.ActionPerformed) store.upsert(entry)
                    }
                },
            )
            if (screen == Screen.Setup && current != null) {
                SetupSheet(
                    session = current,
                    onPickSubtitle = chooseSubtitle,
                    onPlay = { screen = Screen.Player },
                    onDismiss = { screen = Screen.Library },
                )
            }
        }
        if (screen == Screen.Player) {
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}
