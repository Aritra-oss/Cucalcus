package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.example.data.preferences.AppThemeMode

object CucalcusPalette {
    val MainBg = Color(0xFF080808)
    val SecondaryBg = Color(0xFF111111)
    val CardBg = Color(0xFF171717)
    val ElevatedSurface = Color(0xFF202020)
    val PrimaryText = Color(0xFFF5F2EA)
    val SecondaryText = Color(0xFF929292)
    val ChampagneGold = Color(0xFFD6B56D)
    val SubtleBorder = Color(0xFF262626)

    // Light typography baseline
    val PaperText = Color(0xFF141414)
    val PaperSecondaryText = Color(0xFF5D5A52)

    val ErrorRed = Color(0xFFE05A5A)
    val SuccessGreen = Color(0xFF4FB286)
    val WarningAmber = Color(0xFFD6B56D)
}

fun contrastOnColor(color: Color): Color {
    return if (color.luminance() > 0.42f) Color(0xFF0A0A0A) else Color(0xFFF5F2EA)
}

data class SurfacePalette(
    val mainBg: Color,
    val secondaryBg: Color,
    val cardBg: Color,
    val elevatedSurface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val border: Color,
    val isDark: Boolean
)

fun surfacePaletteForTheme(mode: AppThemeMode): SurfacePalette {
    val main = Color(mode.swatchArgb)
    return deriveSurfacePalette(main, mode.isDark)
}

fun deriveSurfacePalette(mainBg: Color, isDark: Boolean): SurfacePalette {
    return if (isDark) {
        val secondary = lerp(mainBg, Color.White, 0.045f)
        val card = lerp(mainBg, Color.White, 0.08f)
        val elevated = lerp(mainBg, Color.White, 0.13f)
        val border = lerp(mainBg, Color.White, 0.17f)
        SurfacePalette(
            mainBg = mainBg,
            secondaryBg = secondary,
            cardBg = card,
            elevatedSurface = elevated,
            primaryText = CucalcusPalette.PrimaryText,
            secondaryText = Color(0xFF9D9B95),
            border = border,
            isDark = true
        )
    } else {
        val secondary = lerp(mainBg, Color.Black, 0.045f)
        val card = lerp(mainBg, Color.White, 0.72f)
        val elevated = lerp(mainBg, Color.Black, 0.085f)
        val border = lerp(mainBg, Color.Black, 0.15f)
        SurfacePalette(
            mainBg = mainBg,
            secondaryBg = secondary,
            cardBg = card,
            elevatedSurface = elevated,
            primaryText = CucalcusPalette.PaperText,
            secondaryText = CucalcusPalette.PaperSecondaryText,
            border = border,
            isDark = false
        )
    }
}
