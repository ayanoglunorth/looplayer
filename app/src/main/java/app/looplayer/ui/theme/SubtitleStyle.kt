package app.looplayer.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.looplayer.R
import app.looplayer.data.SubtitleColor
import app.looplayer.data.SubtitleFont

/** A variable font file exposed at the weights the subtitle line uses. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variable(res: Int) = FontFamily(
    listOf(FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold).map { w ->
        Font(res, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
    },
)

private val Roboto = variable(R.font.roboto_var)
private val Atkinson = variable(R.font.atkinson_next)
private val OpenSans = variable(R.font.open_sans_var)
private val Inconsolata = variable(R.font.inconsolata_var)
private val PtNarrow = FontFamily(
    Font(R.font.pt_sans_narrow_regular, FontWeight.Normal),
    Font(R.font.pt_sans_narrow_regular, FontWeight.SemiBold),
    Font(R.font.pt_sans_narrow_bold, FontWeight.Bold),
)

/**
 * Subtitle faces, all SIL Open Font License and checked for Turkish letters: Roboto (YouTube captions),
 * Atkinson Hyperlegible Next (made for low-vision readers), Open Sans, PT Sans Narrow, Inconsolata
 * (a free stand-in for Consolas), plus the device serif.
 */
fun SubtitleFont.family(): FontFamily = when (this) {
    SubtitleFont.Inter -> Inter
    SubtitleFont.Roboto -> Roboto
    SubtitleFont.Atkinson -> Atkinson
    SubtitleFont.OpenSans -> OpenSans
    SubtitleFont.PtNarrow -> PtNarrow
    SubtitleFont.Inconsolata -> Inconsolata
    SubtitleFont.Serif -> FontFamily.Serif
}

fun SubtitleColor.color() = Color(argb)
