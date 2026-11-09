package app.looplayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.looplayer.R

/**
 * True-black flat: the ground is the black around a film, surfaces rise by lightness alone
 * (no borders, no shadows), the primary action is an inverted white pill, and the single
 * accent hue is spent only on progress.
 */
object Palette {
    val Ground = Color(0xFF000000)
    val Surface = Color(0xFF111111)
    val Raised = Color(0xFF2A2A2A)
    val Border = Color(0xFF1A1A1A)
    val Control = Color(0xFF444444)
    val Text = Color(0xFFFFFFFF)
    val Muted = Color(0xFFB4B4B4)
    val Faint = Color(0xFF949494)
    /** The one hue: how far you are into something. */
    val Progress = Color(0xFF3EA6FF)
    val Negative = Color(0xFFE5484D)

    /** Subtitles are white, the convention every viewer already reads fastest. */
    val Subtitle = Color(0xFFFFFFFF)
}

private val Colors = darkColorScheme(
    primary = Palette.Text,
    onPrimary = Palette.Ground,
    primaryContainer = Palette.Raised,
    onPrimaryContainer = Palette.Text,
    secondary = Palette.Muted,
    onSecondary = Palette.Ground,
    secondaryContainer = Palette.Raised,
    onSecondaryContainer = Palette.Text,
    tertiary = Palette.Progress,
    background = Palette.Ground,
    onBackground = Palette.Text,
    surface = Palette.Ground,
    onSurface = Palette.Text,
    surfaceVariant = Palette.Raised,
    onSurfaceVariant = Palette.Muted,
    surfaceContainerLowest = Palette.Ground,
    surfaceContainerLow = Palette.Surface,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Palette.Raised,
    inverseSurface = Palette.Text,
    inverseOnSurface = Palette.Ground,
    outline = Palette.Control,
    outlineVariant = Palette.Border,
    error = Palette.Negative,
    onError = Palette.Text,
    scrim = Color.Black,
)

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
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun LooplayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Type, shapes = AppShapes, content = content)
}
