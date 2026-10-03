package cloud.g3h.nimbus.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import cloud.g3h.nimbus.R

// ---- Design tokens (spec §2; mockups are 1920×1080 px → dp = px ÷ 2) ----
val Ground = Color(0xFFF4F6F3)
val Surface = Color(0xFFFBFCFA)
val Line = Color(0xFFD9E3EC)
val Tint = Color(0xFFE1EDF8)
val Blue = Color(0xFF5B9BD5)
val BlueStrong = Color(0xFF3D7DBF)
val BlueSoft = Color(0xFF9CC2E6)
val Wave = Color(0xFFC9DCEE)
val Ink = Color(0xFF22384E)
val Ink2 = Color(0xFF586C80)
val WarnBg = Color(0xFFFBEFE5)
val WarnText = Color(0xFF9A4F1C)
val AvgLine = Color(0xFFE08A4C)

// Secondary surfaces seen in the mockups
val BarSoft = Color(0xFFCFE1F2)
val BarSoft2 = Color(0xFFBCD6EE)
val ChipVpnOn = Color(0xFFD5E6F6)
val ChipVpnOnText = Color(0xFF2E6AA8)
val DashedPending = Color(0xFFBFD0E0)
val HighlightRow = Color(0xFFEEF4FA)
val CardBorderActive = Color(0xFFBFD6EC)
val NeutralPill = Color(0xFFEEF1F4)
val WarnBorder = Color(0xFFE2C6AE)
val GridLine = Color(0xFFDCE6EF)
val ChartGrid = Color(0xFFE3EAF1)
val OrbitDots = Color(0xFFA9CBEA)
val PressOk = Color(0xFFDCEAF7)
val SubInk = Color(0xFF45627F)

val ChakraPetch = FontFamily(
    Font(R.font.chakrapetch_regular, FontWeight.Normal),
    Font(R.font.chakrapetch_medium, FontWeight.Medium),
    Font(R.font.chakrapetch_semibold, FontWeight.SemiBold),
    Font(R.font.chakrapetch_bold, FontWeight.Bold)
)

val Oxanium = FontFamily(
    Font(R.font.oxanium_medium, FontWeight.Medium),
    Font(R.font.oxanium_semibold, FontWeight.SemiBold),
    Font(R.font.oxanium_bold, FontWeight.Bold)
)

val NimbusTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = ChakraPetch,
        color = Ink
    )
)

/** Uppercase label style: ~0.18em letter-spacing, ink2. [sizeSp] in sp. */
fun LabelStyle(sizeSp: Float = 8f) = TextStyle(
    fontFamily = ChakraPetch,
    fontWeight = FontWeight.SemiBold,
    fontSize = sizeSp.sp,
    letterSpacing = (0.18f * sizeSp).sp,
    color = Ink2
)

/** Numeric style (speeds, ping, IP, timers). */
fun NumberStyle(sizeSp: Float, color: Color = Ink, weight: FontWeight = FontWeight.Bold) = TextStyle(
    fontFamily = Oxanium,
    fontSize = sizeSp.sp,
    fontWeight = weight,
    color = color
)
