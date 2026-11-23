package com.example.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.ButtonShapeOption
import com.example.data.preferences.ButtonAnimation
import com.example.data.preferences.CucalcusSettings
import com.example.ui.theme.CucalcusTheme
import com.example.ui.theme.LocalButtonFontFamily

enum class KeypadKeyKind {
    DIGIT,
    OPERATOR,
    FUNCTION,
    EQUALS,
    CLEAR
}

fun ButtonShapeOption.toComposeShape(cornerRadiusPercent: Int = 18): Shape = when (this) {
    ButtonShapeOption.SQUARE -> RoundedCornerShape(percent = 10)
    ButtonShapeOption.SLIGHTLY_ROUNDED -> RoundedCornerShape(percent = 18)
    ButtonShapeOption.ROUNDED -> RoundedCornerShape(percent = 32)
    ButtonShapeOption.SQUIRCLE -> RoundedCornerShape(percent = 42)
    ButtonShapeOption.CIRCULAR -> CircleShape
    ButtonShapeOption.CHAMFERED -> RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
    ButtonShapeOption.DIAGONAL_CUT -> RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 10.dp, bottomStart = 10.dp)
    ButtonShapeOption.LEAF -> RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp, topEnd = 12.dp, bottomStart = 12.dp)
    ButtonShapeOption.CUSTOM_RADIUS -> RoundedCornerShape(percent = cornerRadiusPercent.coerceIn(8, 50))
}

@Composable
fun CalculatorKeyButton(
    label: String,
    keyKind: KeypadKeyKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accessibilityLabel: String = label,
    tagName: String = "key_$label",
    settingsOverride: CucalcusSettings? = null,
    fontSize: TextUnit? = null
) {
    val colors = CucalcusTheme.colors
    val settings = settingsOverride ?: CucalcusTheme.settings
    val reducedMotion = CucalcusTheme.reducedMotion
    val buttonFont = LocalButtonFontFamily.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = 0.58f,
            stiffness = 520f
        ),
        label = "key_scale"
    )
    val pressAlpha by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.88f else 1f,
        animationSpec = spring(stiffness = 700f),
        label = "key_press_alpha"
    )

    val shape = settings.buttonShape.toComposeShape(settings.buttonCornerRadiusPercent)
    val opacity = settings.buttonOpacity.coerceIn(0.4f, 1.0f)

    val baseBg: Color
    val baseText: Color
    val borderStroke: BorderStroke?

    when (keyKind) {
        KeypadKeyKind.EQUALS -> {
            baseBg = colors.accent.copy(alpha = opacity)
            baseText = colors.onAccent
            borderStroke = null
        }
        KeypadKeyKind.OPERATOR -> {
            baseBg = if (settings.useCustomButtonColors) {
                Color(settings.buttonBgArgb).copy(alpha = opacity)
            } else {
                colors.elevatedSurface.copy(alpha = opacity)
            }
            baseText = colors.accent
            borderStroke = BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f))
        }
        KeypadKeyKind.CLEAR -> {
            baseBg = if (settings.useCustomButtonColors) {
                Color(settings.buttonBgArgb).copy(alpha = opacity)
            } else {
                colors.secondaryBg.copy(alpha = opacity)
            }
            baseText = colors.error
            borderStroke = BorderStroke(1.dp, colors.border)
        }
        KeypadKeyKind.FUNCTION -> {
            baseBg = if (settings.useCustomButtonColors) {
                Color(settings.buttonBgArgb).copy(alpha = opacity)
            } else {
                colors.secondaryBg.copy(alpha = opacity)
            }
            baseText = colors.secondaryText
            borderStroke = BorderStroke(1.dp, colors.border)
        }
        KeypadKeyKind.DIGIT -> {
            baseBg = if (settings.useCustomButtonColors) {
                Color(settings.buttonBgArgb).copy(alpha = opacity)
            } else {
                colors.cardBg.copy(alpha = opacity)
            }
            baseText = if (settings.useCustomButtonColors) {
                Color(settings.buttonTextArgb)
            } else {
                colors.primaryText
            }
            borderStroke = BorderStroke(1.dp, colors.border)
        }
    }

    val defaultSp = when {
        label.length >= 4 -> 13f
        label.length == 3 -> 15f
        keyKind == KeypadKeyKind.OPERATOR || keyKind == KeypadKeyKind.EQUALS -> 22f
        else -> 20f
    }
    val resolvedFontSize = fontSize ?: (defaultSp * settings.fontScale * settings.buttonSizeScale).coerceIn(11f, 28f).sp

    val buttonPhase = if (settings.buttonAnimationsEnabled && !settings.buttonAnimationPaused) {
        val buttonTransition = rememberInfiniteTransition(label = "button_effect_transition")
        buttonTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), repeatMode = RepeatMode.Restart),
            label = "button_effect_phase"
        ).value
    } else if (settings.buttonAnimationsEnabled) 0.38f else 0f
    val resolvedBorder = if (settings.borderlessButtons) null else borderStroke

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = shape,
        color = baseBg,
        border = resolvedBorder,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = pressAlpha
            }
            .semantics {
                role = Role.Button
                contentDescription = accessibilityLabel
            }
            .testTag(tagName)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            if (settings.buttonAnimationsEnabled) {
                Canvas(Modifier.fillMaxSize()) {
                    val p = buttonPhase
                    when (settings.buttonAnimation) {
                        ButtonAnimation.SOFT_PULSE -> drawCircle(baseText.copy(alpha = 0.09f), size.minDimension * (0.28f + 0.07f * kotlin.math.sin(p * 6.28f)), center)
                        ButtonAnimation.SHIMMER -> drawRect(Brush.linearGradient(listOf(Color.Transparent, baseText.copy(alpha = 0.14f), Color.Transparent), start = Offset(size.width * (p - 0.35f), 0f), end = Offset(size.width * (p + 0.35f), size.height)))
                        ButtonAnimation.RIPPLE -> for (ring in 0..2) {
                            val radius = size.minDimension * ((p + ring * 0.33f) % 1f) * 0.70f
                            drawCircle(baseText.copy(alpha = 0.13f * (1f - radius / size.minDimension)), radius, style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx()))
                        }
                        ButtonAnimation.FLOATING_GLOW -> {
                            val x = size.width * (0.15f + 0.70f * p)
                            drawCircle(Brush.radialGradient(listOf(baseText.copy(alpha = 0.18f), Color.Transparent), center = Offset(x, size.height * 0.5f), radius = size.minDimension * 0.55f), size.minDimension * 0.55f, Offset(x, size.height * 0.5f))
                        }
                        ButtonAnimation.AURORA -> drawRect(Brush.linearGradient(listOf(Color.Transparent, baseText.copy(alpha = 0.12f), Color.Transparent), start = Offset(0f, size.height * p), end = Offset(size.width, size.height * (p + 0.45f))))
                        ButtonAnimation.MAGNETIC -> {
                            val x = size.width * (0.5f + 0.22f * kotlin.math.sin(p * 6.28f))
                            val y = size.height * (0.5f + 0.16f * kotlin.math.cos(p * 6.28f))
                            drawCircle(baseText.copy(alpha = 0.12f), size.minDimension * 0.22f, Offset(x, y))
                        }
                        ButtonAnimation.COLOR_BREATH -> {
                            val breath = 0.5f + 0.5f * kotlin.math.sin(p * 6.28f)
                            drawRect(baseText.copy(alpha = 0.035f + breath * 0.10f))
                            drawCircle(baseText.copy(alpha = 0.10f + breath * 0.08f), size.minDimension * (0.18f + breath * 0.08f), center)
                        }
                        ButtonAnimation.INNER_GLOW -> {
                            val glowX = size.width * (0.18f + 0.64f * p)
                            val glowY = size.height * (0.5f + 0.12f * kotlin.math.sin(p * 6.28f))
                            val glowRadius = size.minDimension * 0.62f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(baseText.copy(alpha = 0.24f), baseText.copy(alpha = 0.08f), Color.Transparent),
                                    center = Offset(glowX, glowY),
                                    radius = glowRadius
                                ),
                                radius = glowRadius,
                                center = Offset(glowX, glowY)
                            )
                        }
                        ButtonAnimation.SPARK_TRAIL -> {
                            for (spark in 0..6) {
                                val t = (p + spark * 0.13f) % 1f
                                val x = size.width * t
                                val y = size.height * (0.5f + 0.25f * kotlin.math.sin((t * 2f + spark) * 6.28f))
                                drawCircle(baseText.copy(alpha = 0.20f * (1f - t)), size.minDimension * (0.025f + (spark % 2) * 0.012f), Offset(x, y))
                            }
                        }
                    }
                }
            }
            Text(
                text = label,
                fontFamily = buttonFont,
                fontSize = resolvedFontSize,
                fontWeight = if (keyKind == KeypadKeyKind.EQUALS) FontWeight.SemiBold else FontWeight.Medium,
                color = baseText,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}
