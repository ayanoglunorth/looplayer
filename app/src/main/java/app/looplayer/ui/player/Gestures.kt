package app.looplayer.ui.player

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
    /** Press-and-hold: starts after the long-press delay, ends on release. */
    val onHoldStart: () -> Unit = {},
    val onHoldEnd: () -> Unit = {},
    /** Vertical slide: start with the touch x as a 0..1 fraction of the width, then the total travel and surface height in px. */
    val onAdjustStart: (xFraction: Float) -> Unit = {},
    val onAdjust: (dragPx: Float, heightPx: Float) -> Unit = { _, _ -> },
    val onAdjustEnd: () -> Unit = {},
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
            var holding = false
            detectTapGestures(
                onPress = {
                    holding = false
                    tryAwaitRelease()
                    if (holding) { holding = false; h.onHoldEnd() }
                },
                onLongPress = { holding = true; h.onHoldStart() },
                onTap = { h.onTap() },
                onDoubleTap = { h.onDoubleTap() },
            )
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
        // Vertical slides adjust brightness (left) or volume (right); they must not end as a tap either way.
        .pointerInput(Unit) {
            var drag = 0f
            detectVerticalDragGestures(
                onDragStart = { at -> drag = 0f; h.onAdjustStart(at.x / size.width) },
                onVerticalDrag = { change, dy ->
                    change.consume()
                    drag += dy
                    h.onAdjust(drag, size.height.toFloat())
                },
                onDragEnd = { h.onAdjustEnd() },
                onDragCancel = { h.onAdjustEnd() },
            )
        }
}
