package app.looplayer.subtitle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DualSubtitlesTest {

    private val turkish = SubtitleTrack(
        label = "Türkçe",
        language = "tr",
        cues = listOf(Cue(1_000, 3_000, "Merhaba"), Cue(2_500, 4_000, "Nasılsın?"), Cue(10_000, 11_000, "Son")),
    )
    private val english = SubtitleTrack(
        label = "English",
        language = "en",
        cues = listOf(Cue(1_000, 3_000, "Hello")),
    )

    @Test
    fun `track returns every cue active at a position joined in order`() {
        assertNull(turkish.textAt(999))
        assertEquals("Merhaba", turkish.textAt(1_000))
        assertEquals("Merhaba\nNasılsın?", turkish.textAt(2_700))
        assertEquals("Nasılsın?", turkish.textAt(3_000))
        assertNull(turkish.textAt(4_000))
        assertEquals("Son", turkish.textAt(10_500))
    }

    @Test
    fun `track applies a sync offset`() {
        val delayed = turkish.copy(offsetMs = 500)
        assertNull(delayed.textAt(1_200))
        assertEquals("Merhaba", delayed.textAt(1_500))
    }

    @Test
    fun `empty state shows nothing and cannot switch`() {
        val state = DualSubtitles()
        assertNull(state.activeTrack)
        assertFalse(state.canSwitch)
        assertEquals(state, state.switched())
    }

    @Test
    fun `first loaded track becomes active and second one does not steal focus`() {
        val state = DualSubtitles().withTrack(Slot.A, turkish).withTrack(Slot.B, english)
        assertEquals(Slot.A, state.active)
        assertEquals("Merhaba", state.textAt(1_500))
        assertTrue(state.canSwitch)
    }

    @Test
    fun `loading only the second slot makes it active`() {
        val state = DualSubtitles().withTrack(Slot.B, english)
        assertEquals(Slot.B, state.active)
        assertEquals("Hello", state.textAt(1_500))
    }

    @Test
    fun `switching flips between the two loaded languages`() {
        val state = DualSubtitles().withTrack(Slot.A, turkish).withTrack(Slot.B, english)
        val flipped = state.switched()
        assertEquals(Slot.B, flipped.active)
        assertEquals("Hello", flipped.textAt(1_500))
        assertEquals(Slot.A, flipped.switched().active)
    }

    @Test
    fun `removing the active track moves focus to the remaining one`() {
        val state = DualSubtitles().withTrack(Slot.A, turkish).withTrack(Slot.B, english).switched()
        val removed = state.withTrack(Slot.B, null)
        assertEquals(Slot.A, removed.active)
        assertFalse(removed.canSwitch)
    }

    @Test
    fun `subtitles can be hidden and shown again without losing the active slot`() {
        val state = DualSubtitles().withTrack(Slot.A, turkish).withTrack(Slot.B, english).switched()
        val hidden = state.copy(visible = false)
        assertNull(hidden.textAt(1_500))
        assertEquals("Hello", hidden.copy(visible = true).textAt(1_500))
    }
}
