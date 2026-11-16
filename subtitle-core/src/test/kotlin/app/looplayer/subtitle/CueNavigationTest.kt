package app.looplayer.subtitle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CueNavigationTest {
    private val track = SubtitleTrack(
        "x", "en",
        listOf(Cue(1_000, 3_000, "a"), Cue(5_000, 7_000, "b"), Cue(9_000, 10_000, "c")),
    )

    @Test fun `previous goes to start of current line`() = assertEquals(5_000L, track.previousCueStart(6_500))

    @Test fun `previous skips to earlier line when just after a start`() = assertEquals(1_000L, track.previousCueStart(5_300))

    @Test fun `previous is null before the first line`() = assertNull(track.previousCueStart(1_200))

    @Test fun `next finds the following line`() = assertEquals(9_000L, track.nextCueStart(6_500))

    @Test fun `next is null after the last line`() = assertNull(track.nextCueStart(9_500))

    @Test fun `offset shifts navigation targets`() {
        val shifted = track.copy(offsetMs = 500)
        assertEquals(5_500L, shifted.previousCueStart(7_000))
        assertEquals(1_500L, shifted.nextCueStart(0))
    }
}
