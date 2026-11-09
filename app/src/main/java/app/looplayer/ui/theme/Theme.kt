package app.looplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import app.looplayer.subtitle.Slot

// Night ink: the app is used where films are watched, in dim rooms, and its main
// screen is a picture on black. The library is the doorway into that, so it is dark too.
private val NightInk = Color(0xFF0F0E0D)
private val Paper = Color(0xFFEDE7DD)
private val PaperMuted = Color(0xFFB5AC9F)

/** Each subtitle slot owns one ink, used identically everywhere that slot appears. */
@Immutable
data class LanguageInk(val ink: Color, val onInk: Color, val subtitleTint: Color)

object Inks {
    val A = LanguageInk(ink = Color(0xFFF0B43C), onInk = Color(0xFF2A1D00), subtitleTint = Color(0xFFFFF3DC))
    val B = LanguageInk(ink = Color(0xFF8DB4FF), onInk = Color(0xFF00194A), subtitleTint = Color(0xFFE6EEFF))

    fun of(slot: Slot) = if (slot == Slot.A) A else B
}

private val Colors = darkColorScheme(
    primary = Inks.A.ink,
    onPrimary = Inks.A.onInk,
    primaryContainer = Color(0xFF3A2C0C),
    onPrimaryContainer = Color(0xFFFFDDA0),
    secondary = Inks.B.ink,
    onSecondary = Inks.B.onInk,
    secondaryContainer = Color(0xFF14284A),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = Inks.B.ink,
    background = NightInk,
    onBackground = Paper,
    surface = NightInk,
    onSurface = Paper,
    surfaceVariant = Color(0xFF2A2621),
    onSurfaceVariant = PaperMuted,
    surfaceContainerLowest = Color(0xFF0A0908),
    surfaceContainerLow = Color(0xFF171513),
    surfaceContainer = Color(0xFF1C1A17),
    surfaceContainerHigh = Color(0xFF24211D),
    surfaceContainerHighest = Color(0xFF2D2A25),
    inverseSurface = Paper,
    inverseOnSurface = Color(0xFF1C1A17),
    outline = Color(0xFF6E665B),
    outlineVariant = Color(0xFF34302A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    scrim = Color.Black,
)

/** Headwords: a dictionary sets its entries in a bold serif. Everything you operate stays Roboto. */
private val Headword = FontFamily.Serif

private val base = Typography()
private val Type = base.copy(
    displaySmall = base.displaySmall.copy(fontFamily = Headword, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontFamily = Headword, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headlineSmall = base.headlineSmall.copy(fontFamily = Headword, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = Headword, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = Headword, fontWeight = FontWeight.SemiBold),
)

/** Language codes on tabs: tracked bold caps, like a thumb index. */
val IndexTabStyle = TextStyle(fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.2.sp)

/** Every time readout uses tabular figures so digits don't jitter while playing. */
val TabularNumbers = "tnum"

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun LooplayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Type, shapes = AppShapes, content = content)
}
