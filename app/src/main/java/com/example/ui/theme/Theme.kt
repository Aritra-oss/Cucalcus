package com.example.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AppBackgroundPreset
import com.example.data.preferences.CucalcusSettings
import java.io.File

@Immutable
data class CucalcusColors(
    val mainBg: Color,
    val secondaryBg: Color,
    val cardBg: Color,
    val elevatedSurface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accent: Color,
    val onAccent: Color,
    val border: Color,
    val error: Color,
    val success: Color,
    val isDark: Boolean,
    val hasBackgroundImage: Boolean = false
)

val LocalCucalcusColors = staticCompositionLocalOf {
    CucalcusColors(
        mainBg = CucalcusPalette.MainBg,
        secondaryBg = CucalcusPalette.SecondaryBg,
        cardBg = CucalcusPalette.CardBg,
        elevatedSurface = CucalcusPalette.ElevatedSurface,
        primaryText = CucalcusPalette.PrimaryText,
        secondaryText = CucalcusPalette.SecondaryText,
        accent = CucalcusPalette.ChampagneGold,
        onAccent = Color(0xFF0A0A0A),
        border = CucalcusPalette.SubtleBorder,
        error = CucalcusPalette.ErrorRed,
        success = CucalcusPalette.SuccessGreen,
        isDark = true,
        hasBackgroundImage = false
    )
}

val LocalCucalcusSettings = staticCompositionLocalOf { CucalcusSettings() }
val LocalReducedMotion = staticCompositionLocalOf { false }

val LocalDisplayFontFamily = staticCompositionLocalOf<FontFamily> { CucalcusFonts.SpaceGrotesk }
val LocalButtonFontFamily = staticCompositionLocalOf<FontFamily> { CucalcusFonts.Inter }

object CucalcusTheme {
    val colors: CucalcusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCucalcusColors.current

    val settings: CucalcusSettings
        @Composable
        @ReadOnlyComposable
        get() = LocalCucalcusSettings.current

    val reducedMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReducedMotion.current
}

fun isReducedMotionEnabled(context: Context): Boolean {
    return try {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        scale == 0f
    } catch (_: Throwable) {
        false
    }
}

private val SwissShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CucalcusTheme(
    settings: CucalcusSettings = CucalcusSettings(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    val animDuration = if (reducedMotion) 0 else 240

    val hasActiveBgImage = remember(settings.backgroundImagePath) {
        val path = settings.backgroundImagePath
        !path.isNullOrBlank() && File(path).exists()
    }

    val targetAccent = Color(settings.activeAccentArgb)
    val themePalette = surfacePaletteForTheme(settings.themeMode)

    val mainBgColor = when {
        hasActiveBgImage -> themePalette.mainBg.copy(alpha = 0.85f)
        settings.backgroundPreset != AppBackgroundPreset.THEME_DEFAULT -> Color(settings.backgroundPreset.argbHex)
        else -> themePalette.mainBg
    }

    val animatedColors = CucalcusColors(
        mainBg = mainBgColor,
        secondaryBg = themePalette.secondaryBg,
        cardBg = themePalette.cardBg,
        elevatedSurface = themePalette.elevatedSurface,
        primaryText = themePalette.primaryText,
        secondaryText = themePalette.secondaryText,
        accent = targetAccent,
        onAccent = contrastOnColor(targetAccent),
        border = themePalette.border,
        error = CucalcusPalette.ErrorRed,
        success = CucalcusPalette.SuccessGreen,
        isDark = themePalette.isDark,
        hasBackgroundImage = hasActiveBgImage
    )

    val m3Scheme = if (animatedColors.isDark) {
        darkColorScheme(
            primary = animatedColors.accent,
            onPrimary = animatedColors.onAccent,
            secondary = animatedColors.accent,
            onSecondary = animatedColors.onAccent,
            background = animatedColors.mainBg,
            onBackground = animatedColors.primaryText,
            surface = animatedColors.cardBg,
            onSurface = animatedColors.primaryText,
            surfaceVariant = animatedColors.elevatedSurface,
            onSurfaceVariant = animatedColors.secondaryText,
            outline = animatedColors.border,
            error = animatedColors.error
        )
    } else {
        lightColorScheme(
            primary = animatedColors.accent,
            onPrimary = animatedColors.onAccent,
            secondary = animatedColors.accent,
            onSecondary = animatedColors.onAccent,
            background = animatedColors.mainBg,
            onBackground = animatedColors.primaryText,
            surface = animatedColors.cardBg,
            onSurface = animatedColors.primaryText,
            surfaceVariant = animatedColors.elevatedSurface,
            onSurfaceVariant = animatedColors.secondaryText,
            outline = animatedColors.border,
            error = animatedColors.error
        )
    }

    val typography = remember(settings.interfaceFont, settings.fontScale) {
        buildCucalcusTypography(settings.interfaceFont, settings.fontScale)
    }
    val displayFontFamily = remember(settings.displayFont) {
        CucalcusFonts.resolve(settings.displayFont)
    }
    val buttonFontFamily = remember(settings.buttonFont) {
        CucalcusFonts.resolve(settings.buttonFont)
    }

    CompositionLocalProvider(
        LocalCucalcusColors provides animatedColors,
        LocalCucalcusSettings provides settings,
        LocalReducedMotion provides reducedMotion,
        LocalDisplayFontFamily provides displayFontFamily,
        LocalButtonFontFamily provides buttonFontFamily
    ) {
        MaterialTheme(
            colorScheme = m3Scheme,
            typography = typography,
            shapes = SwissShapes,
            content = content
        )
    }
}
