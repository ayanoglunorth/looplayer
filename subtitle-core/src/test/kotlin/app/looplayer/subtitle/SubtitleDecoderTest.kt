package app.looplayer.subtitle

import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals

class SubtitleDecoderTest {

    @Test
    fun `utf8 with bom is decoded and bom dropped`() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "Şişli ğ".toByteArray(Charsets.UTF_8)
        assertEquals("Şişli ğ", SubtitleDecoder.decode(bytes))
    }

    @Test
    fun `plain utf8 is decoded`() {
        assertEquals("Işık ve gölge", SubtitleDecoder.decode("Işık ve gölge".toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun `utf16 little endian with bom is decoded`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "Çay".toByteArray(Charsets.UTF_16LE)
        assertEquals("Çay", SubtitleDecoder.decode(bytes))
    }

    @Test
    fun `legacy turkish windows-1254 files fall back correctly`() {
        val text = "Ağaçların gölgesinde İstanbul'u düşündü"
        val bytes = text.toByteArray(Charset.forName("windows-1254"))
        assertEquals(text, SubtitleDecoder.decode(bytes))
    }
}
