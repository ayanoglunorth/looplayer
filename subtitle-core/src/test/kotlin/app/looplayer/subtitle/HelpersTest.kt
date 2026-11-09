package app.looplayer.subtitle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HelpersTest {

    @Test
    fun `language is guessed from common file name patterns`() {
        assertEquals("tr", LanguageGuess.fromFileName("The.Movie.2024.tr.srt"))
        assertEquals("en", LanguageGuess.fromFileName("the_movie_eng.srt"))
        assertEquals("tr", LanguageGuess.fromFileName("Film - Turkish.srt"))
        assertEquals("de", LanguageGuess.fromFileName("Film.GER.forced.srt"))
        assertEquals("pt", LanguageGuess.fromFileName("film.pt-BR.vtt"))
        assertNull(LanguageGuess.fromFileName("subtitle.srt"))
    }

    @Test
    fun `time is formatted with hours only when needed`() {
        assertEquals("0:00", TimeFormat.format(0))
        assertEquals("0:09", TimeFormat.format(9_999))
        assertEquals("12:05", TimeFormat.format(725_000))
        assertEquals("1:02:03", TimeFormat.format(3_723_000))
        assertEquals("0:00", TimeFormat.format(-5))
    }

    @Test
    fun `signed delta is formatted for the seek preview`() {
        assertEquals("+0:24", TimeFormat.formatDelta(24_400))
        assertEquals("−1:05", TimeFormat.formatDelta(-65_000))
    }

    @Test
    fun `horizontal drag maps to a seek target proportional to width and clamped`() {
        // A full-width drag covers 90 s on long videos.
        assertEquals(45_000L, SeekMath.target(startMs = 0, dragPx = 500f, widthPx = 1000f, durationMs = 3_600_000))
        // Short videos: a full-width drag never exceeds the whole duration.
        assertEquals(30_000L, SeekMath.target(startMs = 0, dragPx = 1000f, widthPx = 1000f, durationMs = 30_000))
        assertEquals(0L, SeekMath.target(startMs = 10_000, dragPx = -1000f, widthPx = 1000f, durationMs = 3_600_000))
        assertEquals(3_600_000L, SeekMath.target(startMs = 3_590_000, dragPx = 1000f, widthPx = 1000f, durationMs = 3_600_000))
    }
}
