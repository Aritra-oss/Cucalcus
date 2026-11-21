package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.ui.theme.CucalcusTheme
import kotlinx.coroutines.launch

/**
 * GPU-only bouncy scale press modifier (no relayout overhead).
 */
fun Modifier.bouncyPress(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.93f
): Modifier = composed {
    val reducedMotion = CucalcusTheme.reducedMotion
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.86f,
            stiffness = Spring.StiffnessHigh
        ),
        label = "bouncy_press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Clickable Surface with smooth spring press-bounce animation.
 */
@Composable
fun BouncySurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    color: Color = CucalcusTheme.colors.cardBg,
    border: BorderStroke? = null,
    pressedScale: Float = 0.94f,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = shape,
        color = color,
        border = border,
        modifier = modifier.bouncyPress(
            interactionSource = interactionSource,
            enabled = enabled,
            pressedScale = pressedScale
        ),
        content = content
    )
}

/**
 * Anchored popup card that plays a crisp spring-bounce animation on open and smooth shrink on close.
 */
@Composable
fun BouncyPopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopEnd,
    transformOrigin: TransformOrigin = TransformOrigin(0.92f, 0.04f),
    shape: Shape = RoundedCornerShape(22.dp),
    containerColor: Color = CucalcusTheme.colors.elevatedSurface,
    borderColor: Color = CucalcusTheme.colors.border,
    content: @Composable ColumnScope.() -> Unit
) {
    val reducedMotion = CucalcusTheme.reducedMotion
    val visibilityState = remember { MutableTransitionState(false) }

    LaunchedEffect(expanded) {
        visibilityState.targetState = expanded
    }

    // Keep the popup in the composition while it shrinks away; removing Popup immediately
    // on a close tap would cut off the exit animation and can look like a stuck/stacked menu.
    if (visibilityState.currentState || visibilityState.targetState) {
        Popup(
            alignment = alignment,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            AnimatedVisibility(
                visibleState = visibilityState,
                enter = if (reducedMotion) {
                    fadeIn(tween(0))
                } else {
                    fadeIn(tween(110, easing = FastOutSlowInEasing)) +
                        scaleIn(
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = Spring.StiffnessMedium
                            ),
                            initialScale = 0.92f,
                            transformOrigin = transformOrigin
                        )
                },
                exit = if (reducedMotion) {
                    fadeOut(tween(0))
                } else {
                    fadeOut(tween(90, easing = FastOutSlowInEasing)) +
                        scaleOut(
                            animationSpec = tween(105, easing = FastOutSlowInEasing),
                            targetScale = 0.96f,
                            transformOrigin = transformOrigin
                        )
                }
            ) {
                Surface(
                    shape = shape,
                    color = containerColor,
                    border = BorderStroke(1.dp, borderColor),
                    shadowElevation = 10.dp,
                    modifier = modifier
                ) {
                    Column(content = content)
                }
            }
        }
    }
}

/**
 * Modal dialog with a flicker-free, silky spring entrance animation.
 */
@Composable
fun BouncyDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    containerColor: Color = CucalcusTheme.colors.cardBg,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable RowScope.() -> Unit,
    dismissButton: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = CucalcusTheme.colors
    val reducedMotion = CucalcusTheme.reducedMotion
    val scale = remember { Animatable(if (reducedMotion) 1f else 0.90f) }
    val alpha = remember { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            launch { alpha.animateTo(1f, tween(210, easing = FastOutSlowInEasing)) }
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.80f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Disable platform window animation so Compose's graphicsLayer owns 100% of dialog open motion cleanly.
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        SideEffect {
            dialogWindowProvider?.window?.setWindowAnimations(0)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = shape,
                color = containerColor,
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 14.dp,
                modifier = modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                    }
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    CompositionLocalProvider(LocalContentColor provides colors.primaryText) {
                        androidx.compose.material3.ProvideTextStyle(
                            value = MaterialTheme.typography.headlineSmall
                        ) {
                            title()
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    CompositionLocalProvider(LocalContentColor provides colors.secondaryText) {
                        androidx.compose.material3.ProvideTextStyle(
                            value = MaterialTheme.typography.bodyMedium
                        ) {
                            text()
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissButton != null) {
                            dismissButton()
                        }
                        confirmButton()
                    }
                }
            }
        }
    }
}
