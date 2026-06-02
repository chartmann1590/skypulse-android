package com.charles.skypulse.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.charles.skypulse.app.R

/** Tri-font strategy from DESIGN.md: Sora (headlines), Inter (body), JetBrains Mono (data). */
val Sora = FontFamily(
    Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_bold, FontWeight.Bold),
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

object SkyType {
    val DisplayLg = TextStyle(
        fontFamily = Sora, fontWeight = FontWeight.Bold,
        fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = (-0.02).em,
    )
    val HeadlineLg = TextStyle(
        fontFamily = Sora, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 40.sp,
    )
    val HeadlineLgMobile = TextStyle(
        fontFamily = Sora, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 32.sp,
    )
    val TitleMd = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp,
    )
    val BodyMd = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp,
    )
    val DataLg = TextStyle(
        fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium,
        fontSize = 18.sp, lineHeight = 24.sp,
    )
    val LabelSm = TextStyle(
        fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.05.em,
    )
}

/** Material3 Typography mapped onto the SkyPulse styles for default component theming. */
val SkyTypography = Typography(
    displayLarge = SkyType.DisplayLg,
    headlineLarge = SkyType.HeadlineLg,
    headlineMedium = SkyType.HeadlineLgMobile,
    titleLarge = SkyType.TitleMd,
    titleMedium = SkyType.TitleMd,
    bodyLarge = SkyType.BodyMd,
    bodyMedium = SkyType.BodyMd,
    labelLarge = SkyType.TitleMd,
    labelSmall = SkyType.LabelSm,
)
