package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
        targetValue = if (isPressed && !reducedMotion) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "key_scale"
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
