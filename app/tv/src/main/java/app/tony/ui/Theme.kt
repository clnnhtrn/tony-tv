package app.tony.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import app.tony.R

/**
 * Tony's design tokens: v7-polished (design/mockups/v7-polished). Mock-ups are 1920×1080 px;
 * Android TV at 1080p is 960×540 dp, so every mock-up px value is divided by 2 here.
 */
object T {
    val Bg = Color(0xFF15161B)
    val Panel = Color(0xFF1C1D24)
    val Surface = Color(0xFF24252D)
    val Surface2 = Color(0xFF2A2B33)
    val Text = Color(0xFFF5F5F7)
    val Text2 = Color(0xFFC7C7CC)
    val Text3 = Color(0xFFAEAEB2)
    val Muted = Color(0xFF8E8E93)
    val Accent = Color(0xFFFF9F0A)
    val Hairline = Color.White.copy(alpha = 0.10f)
    val HairlineStrong = Color.White.copy(alpha = 0.16f)
    val ChipBg = Color(0xFF0C0C10).copy(alpha = 0.62f)
    val DarkBadge = Color(0xFF0B0C10).copy(alpha = 0.78f)

    val Margin = 52.dp          // 104 px
    val Top = 24.dp             // 48 px
    val Gap = 14.dp             // 28 px tile gap
    val RowGap = 24.dp
    val WideW = 200.dp; val WideH = 112.5.dp     // 400×225 px
    val StdW = 125.dp; val StdH = 70.dp          // 250×140 px
    val RadiusWide = 8.dp; val RadiusStd = 7.dp  // 16 / 14 px
    val FocusEdge = 1.5.dp                       // 3 px
    const val FocusScale = 1.05f
    const val Dim = 0.80f                        // unfocused stills at 80% brightness
}

private fun inter(w: Int) = Font(R.font.inter, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

val Inter = FontFamily(inter(400), inter(500), inter(600), inter(700), inter(800), inter(900))

fun ts(size: TextUnit, weight: Int = 500, color: Color = T.Text, tracking: TextUnit = 0.sp, lineHeight: TextUnit = TextUnit.Unspecified) =
    TextStyle(fontFamily = Inter, fontSize = size, fontWeight = FontWeight(weight), color = color, letterSpacing = tracking, lineHeight = lineHeight)

@Composable
fun TonyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = T.Bg, surface = T.Panel, onSurface = T.Text, primary = Color.White,
            onPrimary = T.Bg, border = T.Hairline, inverseSurface = Color.White,
        ),
        content = content,
    )
}

fun parseColor(hex: String?, fallback: Color = T.Surface): Color = try {
    if (hex == null || !hex.startsWith("#") || (hex.length != 7 && hex.length != 9)) fallback
    else Color(android.graphics.Color.parseColor(hex))
} catch (e: IllegalArgumentException) { fallback }
