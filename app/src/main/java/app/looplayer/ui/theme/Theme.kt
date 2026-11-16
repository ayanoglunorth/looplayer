package app.looplayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.looplayer.R

/** One set of colours; the theme provides the dark or the light one. */
class PaletteColors(
    val ground: Color, val surface: Color, val raised: Color, val border: Color, val control: Color,
    val text: Color, val muted: Color, val faint: Color, val progress: Color, val negative: Color,
)

private val DarkPalette = PaletteColors(
    ground = Color(0xFF16130F), surface = Color(0xFF1F1B16), raised = Color(0xFF2E2923), border = Color(0xFF26211B),
    control = Color(0xFF4A4339), text = Color(0xFFF4EEE4), muted = Color(0xFFBDB4A5), faint = Color(0xFF9A9183),
    progress = Color(0xFFF0A24B), negative = Color(0xFFE5645F),
)

/** Warm paper: the same hues as the dark ground, inverted, with the accent deepened to keep contrast on white. */
private val LightPalette = PaletteColors(
    ground = Color(0xFFFAF7F2), surface = Color(0xFFF1ECE3), raised = Color(0xFFE6DFD3), border = Color(0xFFE9E3D8),
    control = Color(0xFFCBC2B2), text = Color(0xFF1F1B16), muted = Color(0xFF665E51), faint = Color(0xFF8A8171),
    progress = Color(0xFFC2700C), negative = Color(0xFFC0392F),
)

private val LocalPalette = staticCompositionLocalOf { DarkPalette }

/**
 * The warm flat palette. Surfaces rise by lightness alone, the primary action is the text colour as a pill,
 * and the single amber accent is spent only on progress. Read inside composition: it follows the theme.
 */
object Palette {
    val Ground: Color @Composable get() = LocalPalette.current.ground
    val Surface: Color @Composable get() = LocalPalette.current.surface
    val Raised: Color @Composable get() = LocalPalette.current.raised
    val Border: Color @Composable get() = LocalPalette.current.border
    val Control: Color @Composable get() = LocalPalette.current.control
    val Text: Color @Composable get() = LocalPalette.current.text
    val Muted: Color @Composable get() = LocalPalette.current.muted
    val Faint: Color @Composable get() = LocalPalette.current.faint
    /** The one hue: how far you are into something. */
    val Progress: Color @Composable get() = LocalPalette.current.progress
    val Negative: Color @Composable get() = LocalPalette.current.negative

    /** Subtitles are white, the convention every viewer already reads fastest. */
    val Subtitle = Color(0xFFFFFFFF)
}

private fun schemeFor(p: PaletteColors, dark: Boolean): ColorScheme {
    val high = if (dark) Color(0xFF28231D) else Color(0xFFECE6DA)
    val scrim = if (dark) Color(0xFF0B0907) else Color(0xFF1F1B16)
    return (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = p.text, onPrimary = p.ground,
        primaryContainer = p.raised, onPrimaryContainer = p.text,
        secondary = p.muted, onSecondary = p.ground,
        secondaryContainer = p.raised, onSecondaryContainer = p.text,
        tertiary = p.progress,
        background = p.ground, onBackground = p.text,
        surface = p.ground, onSurface = p.text,
        surfaceVariant = p.raised, onSurfaceVariant = p.muted,
        surfaceContainerLowest = p.ground, surfaceContainerLow = p.surface, surfaceContainer = p.surface,
        surfaceContainerHigh = high, surfaceContainerHighest = p.raised,
        inverseSurface = p.text, inverseOnSurface = p.ground,
        outline = p.control, outlineVariant = p.border,
        error = p.negative, onError = p.text,
        scrim = scrim,
    )
}

/** Inter: a screen-first workhorse with full Turkish coverage and tabular figures. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun TextStyle.inter(weight: FontWeight, tracking: Float = 0f) =
    copy(fontFamily = Inter, fontWeight = weight, letterSpacing = tracking.sp)

private val base = Typography()
private val Type = Typography(
    displayLarge = base.displayLarge.inter(FontWeight.SemiBold, -1.2f),
    displayMedium = base.displayMedium.inter(FontWeight.SemiBold, -1f),
    displaySmall = base.displaySmall.inter(FontWeight.SemiBold, -0.8f),
    headlineLarge = base.headlineLarge.inter(FontWeight.SemiBold, -0.6f),
    headlineMedium = base.headlineMedium.inter(FontWeight.SemiBold, -0.5f),
    headlineSmall = base.headlineSmall.inter(FontWeight.SemiBold, -0.3f),
    titleLarge = base.titleLarge.inter(FontWeight.SemiBold, -0.2f),
    titleMedium = base.titleMedium.inter(FontWeight.SemiBold, -0.1f),
    titleSmall = base.titleSmall.inter(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.inter(FontWeight.Normal),
    bodyMedium = base.bodyMedium.inter(FontWeight.Normal),
    bodySmall = base.bodySmall.inter(FontWeight.Normal),
    labelLarge = base.labelLarge.inter(FontWeight.Medium),
    labelMedium = base.labelMedium.inter(FontWeight.Medium),
    labelSmall = base.labelSmall.inter(FontWeight.Medium),
)

/** Language codes on tabs. */
val IndexTabStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.4.sp)

/** Every time readout uses tabular figures so digits don't jitter while playing. */
const val TabularNumbers = "tnum"

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun LooplayerTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = schemeFor(palette, dark), typography = Type, shapes = AppShapes, content = content)
    }
}
