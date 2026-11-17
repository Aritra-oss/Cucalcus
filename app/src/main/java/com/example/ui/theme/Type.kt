package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.FontOption

object CucalcusFonts {
    val Inter = FontFamily(Font(R.font.inter, FontWeight.Normal))
    val Lora = FontFamily(Font(R.font.lora, FontWeight.Normal))
    val JetBrainsMono = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))
    val SpaceGrotesk = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))
    val PlayfairDisplay = FontFamily(Font(R.font.playfair_display, FontWeight.Normal))

    fun resolve(option: FontOption): FontFamily = when (option) {
        FontOption.SANS_SERIF -> Inter
        FontOption.SERIF -> Lora
        FontOption.MONOSPACE -> JetBrainsMono
        FontOption.GEOMETRIC -> SpaceGrotesk
        FontOption.ELEGANT_DISPLAY -> PlayfairDisplay
    }
}

fun buildCucalcusTypography(interfaceFont: FontOption, scale: Float): Typography {
    val family = CucalcusFonts.resolve(interfaceFont)
    val s = scale.coerceIn(0.85f, 1.25f)
    return Typography(
        displayLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Light,
            fontSize = (44f * s).sp,
            lineHeight = (50f * s).sp,
            letterSpacing = (-1.0).sp
        ),
        displayMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (32f * s).sp,
            lineHeight = (38f * s).sp,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = (22f * s).sp,
            lineHeight = (28f * s).sp,
            letterSpacing = (-0.2).sp
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (18f * s).sp,
            lineHeight = (24f * s).sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (15f * s).sp,
            lineHeight = (20f * s).sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * s).sp,
            lineHeight = (22f * s).sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = (13f * s).sp,
            lineHeight = (18f * s).sp,
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (13f * s).sp,
            lineHeight = (18f * s).sp,
            letterSpacing = 0.4.sp
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (11f * s).sp,
            lineHeight = (15f * s).sp,
            letterSpacing = 0.8.sp
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = (10f * s).sp,
            lineHeight = (14f * s).sp,
            letterSpacing = 1.0.sp
        )
    )
}
