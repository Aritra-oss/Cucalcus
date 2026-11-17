package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

object CucalcusPalette {
    val MainBg = Color(0xFF080808)
    val SecondaryBg = Color(0xFF111111)
    val CardBg = Color(0xFF171717)
    val ElevatedSurface = Color(0xFF202020)
    val PrimaryText = Color(0xFFF5F2EA)
    val SecondaryText = Color(0xFF929292)
    val ChampagneGold = Color(0xFFD6B56D)
    val SubtleBorder = Color(0xFF262626)

    // Graphite Dark option
    val GraphiteBg = Color(0xFF101114)
    val GraphiteSecondary = Color(0xFF17191E)
    val GraphiteCard = Color(0xFF1E2026)
    val GraphiteElevated = Color(0xFF282B33)
    val GraphiteBorder = Color(0xFF31343D)

    // Swiss Paper Light option
    val PaperBg = Color(0xFFF6F3EC)
    val PaperSecondary = Color(0xFFEDE9DF)
    val PaperCard = Color(0xFFFFFFFF)
    val PaperElevated = Color(0xFFE5E0D4)
    val PaperText = Color(0xFF141414)
    val PaperSecondaryText = Color(0xFF636059)
    val PaperBorder = Color(0xFFD8D2C5)

    val ErrorRed = Color(0xFFE05A5A)
    val SuccessGreen = Color(0xFF4FB286)
    val WarningAmber = Color(0xFFD6B56D)
}

fun contrastOnColor(color: Color): Color {
    return if (color.luminance() > 0.42f) Color(0xFF0A0A0A) else Color(0xFFF5F2EA)
}
