package app.looplayer.subtitle

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

object SubtitleDecoder {
    /**
     * Subtitle files arrive in whatever encoding their author's PC used. BOMs win;
     * otherwise strict UTF-8; otherwise windows-1254, which is windows-1252 plus the
     * Turkish letters, so it also reads ordinary Western European files.
     */
    fun decode(bytes: ByteArray): String {
        fun startsWith(vararg b: Int) = bytes.size >= b.size && b.indices.all { bytes[it] == b[it].toByte() }
        return when {
            startsWith(0xEF, 0xBB, 0xBF) -> String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
            startsWith(0xFF, 0xFE) -> String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
            startsWith(0xFE, 0xFF) -> String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
            else -> strictUtf8(bytes) ?: String(bytes, Charset.forName("windows-1254"))
        }
    }

    private fun strictUtf8(bytes: ByteArray): String? = try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()
    } catch (_: CharacterCodingException) {
        null
    }
}

object LanguageGuess {
    private val codes = mapOf(
        "tr" to "tr", "tur" to "tr", "turkish" to "tr", "turkce" to "tr", "türkçe" to "tr",
        "en" to "en", "eng" to "en", "english" to "en", "ingilizce" to "en",
        "de" to "de", "ger" to "de", "deu" to "de", "german" to "de", "almanca" to "de",
        "fr" to "fr", "fre" to "fr", "fra" to "fr", "french" to "fr", "fransizca" to "fr",
        "es" to "es", "spa" to "es", "spanish" to "es",
        "it" to "it", "ita" to "it", "italian" to "it",
        "pt" to "pt", "por" to "pt", "portuguese" to "pt",
        "ru" to "ru", "rus" to "ru", "russian" to "ru",
        "ar" to "ar", "ara" to "ar", "arabic" to "ar",
        "ja" to "ja", "jpn" to "ja", "japanese" to "ja",
        "ko" to "ko", "kor" to "ko", "korean" to "ko",
        "zh" to "zh", "chi" to "zh", "zho" to "zh", "chinese" to "zh",
        "nl" to "nl", "dut" to "nl", "dutch" to "nl",
        "pl" to "pl", "pol" to "pl", "polish" to "pl",
    )

    /** Looks for a language token in the name, preferring the ones nearest the extension. */
    fun fromFileName(name: String): String? {
        val tokens = name.substringBeforeLast('.').lowercase()
            .split('.', '_', '-', ' ', '[', ']', '(', ')')
            .filter { it.isNotEmpty() }
        // Ignore the first token: it is the title, and "It" or "Ran" must not become a language.
        return tokens.drop(1).asReversed().firstNotNullOfOrNull { codes[it] }
    }
}

object TimeFormat {
    fun format(ms: Long): String {
        val total = (ms.coerceAtLeast(0) / 1000)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    fun formatDelta(ms: Long): String = (if (ms < 0) "−" else "+") + format(kotlin.math.abs(ms))
}

object SeekMath {
    /** How much time a drag across the full width covers on a long video. */
    const val FULL_WIDTH_MS = 90_000L

    fun target(startMs: Long, dragPx: Float, widthPx: Float, durationMs: Long): Long {
        if (widthPx <= 0f || durationMs <= 0) return startMs.coerceAtLeast(0)
        val span = minOf(FULL_WIDTH_MS, durationMs)
        val delta = (dragPx / widthPx * span).toLong()
        return (startMs + delta).coerceIn(0, durationMs)
    }
}
