package app.looplayer.ui.player

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** Callbacks for the picture's gesture surface. */
class PlayerGestureHandler(
    val onTap: () -> Unit,
    val onDoubleTap: () -> Unit,
    val onSeekStart: () -> Unit,
    /** Total horizontal travel since the drag started, and the surface width, in px. */
    val onSeek: (dragPx: Float, widthPx: Float) -> Unit,
    val onSeekEnd: () -> Unit,
    val onSeekCancel: () -> Unit,
)

/**
 * Tap toggles the controls, double-tap switches subtitle language, a sideways drag seeks.
 * Double-tap is deliberately not ±10 s here: that job belongs to the drag and the skip buttons.
 */
@Composable
fun Modifier.playerGestures(handler: PlayerGestureHandler): Modifier {
    val h by rememberUpdatedState(handler)
    return this
        .pointerInput(Unit) {
            detectTapGestures(onTap = { h.onTap() }, onDoubleTap = { h.onDoubleTap() })
        }
        .pointerInput(Unit) {
            var drag = 0f
            detectHorizontalDragGestures(
                onDragStart = { drag = 0f; h.onSeekStart() },
                onHorizontalDrag = { change, dx ->
                    change.consume()
                    drag += dx
                    h.onSeek(drag, size.width.toFloat())
                },
                onDragEnd = { h.onSeekEnd() },
                onDragCancel = { h.onSeekCancel() },
            )
        }
}
