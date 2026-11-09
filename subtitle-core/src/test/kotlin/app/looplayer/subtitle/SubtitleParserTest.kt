package app.looplayer.subtitle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubtitleParserTest {

    @Test
    fun `parses srt blocks with multi-line text and CRLF`() {
        val srt = "1\r\n00:00:01,000 --> 00:00:02,500\r\nMerhaba\r\ndünya\r\n\r\n" +
            "2\r\n00:01:00,250 --> 00:01:03,000\r\n<i>İkinci</i> satır\r\n"
        val cues = SubtitleParser.parse(srt, SubtitleFormat.SRT)
        assertEquals(
            listOf(
                Cue(1_000, 2_500, "Merhaba\ndünya"),
                Cue(60_250, 63_000, "İkinci satır"),
            ),
            cues,
        )
    }

    @Test
    fun `srt tolerates missing index lines, dot millis and extra blank lines`() {
        val srt = "\n\n00:00:05.000 --> 00:00:06.000\nHello\n\n\n\n7\n00:00:01,000 --> 00:00:02,000\nFirst\n"
        val cues = SubtitleParser.parse(srt, SubtitleFormat.SRT)
        assertEquals(listOf(Cue(1_000, 2_000, "First"), Cue(5_000, 6_000, "Hello")), cues)
    }

    @Test
    fun `parses webvtt with short timestamps, settings and notes`() {
        val vtt = """
            WEBVTT

            NOTE this is ignored

            intro
            00:01.000 --> 00:02.000 align:start line:90%
            <v Roger>Hi <b>there</b>

            01:00:00.000 --> 01:00:01.500
            Late &amp; great
        """.trimIndent()
        val cues = SubtitleParser.parse(vtt, SubtitleFormat.VTT)
        assertEquals(
            listOf(Cue(1_000, 2_000, "Hi there"), Cue(3_600_000, 3_601_500, "Late & great")),
            cues,
        )
    }

    @Test
    fun `parses ass dialogue lines using the format line and strips override tags`() {
        val ass = """
            [Script Info]
            Title: test

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Comment: 0,0:00:00.00,0:00:01.00,Default,,0,0,0,,ignored
            Dialogue: 0,0:00:01.50,0:00:03.00,Default,,0,0,0,,{\i1}Hello{\i0}, world\Nsecond line
        """.trimIndent()
        val cues = SubtitleParser.parse(ass, SubtitleFormat.ASS)
        assertEquals(listOf(Cue(1_500, 3_000, "Hello, world\nsecond line")), cues)
    }

    @Test
    fun `format is detected from file name`() {
        assertEquals(SubtitleFormat.SRT, SubtitleFormat.fromFileName("Film.TR.SRT"))
        assertEquals(SubtitleFormat.VTT, SubtitleFormat.fromFileName("a.vtt"))
        assertEquals(SubtitleFormat.ASS, SubtitleFormat.fromFileName("a.ssa"))
        assertEquals(SubtitleFormat.ASS, SubtitleFormat.fromFileName("a.ass"))
        assertNull(SubtitleFormat.fromFileName("a.mkv"))
    }

    @Test
    fun `format is sniffed from content when the name says nothing`() {
        assertEquals(SubtitleFormat.VTT, SubtitleFormat.sniff("﻿WEBVTT\n\n"))
        assertEquals(SubtitleFormat.ASS, SubtitleFormat.sniff("[Script Info]\n"))
        assertEquals(SubtitleFormat.SRT, SubtitleFormat.sniff("1\n00:00:01,000 --> 00:00:02,000\nx"))
    }
}
