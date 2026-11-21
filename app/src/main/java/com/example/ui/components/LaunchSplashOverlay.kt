package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.settings.drawableRes
import com.example.ui.theme.CucalcusTheme
import com.example.ui.theme.LocalDisplayFontFamily

/**
 * Theme-matched, icon-aware branded launch splash overlay rendered on Frame 0
 * so cold app launch never shows a blank black screen or flashes a default theme.
 */
@Composable
fun LaunchSplashOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = CucalcusTheme.colors
    val settings = CucalcusTheme.settings
    val displayFont = LocalDisplayFontFamily.current
    val reducedMotion = CucalcusTheme.reducedMotion

    val exitMs = if (reducedMotion) 0 else 220

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(0)),
        exit = fadeOut(tween(exitMs)) + scaleOut(targetScale = 1.03f, animationSpec = tween(exitMs)),
        modifier = modifier
    ) {
        var emblemEntered by remember { mutableStateOf(reducedMotion) }
        LaunchedEffect(Unit) { emblemEntered = true }

        val emblemScale by animateFloatAsState(
            targetValue = if (emblemEntered) 1f else 0.90f,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
            label = "splash_emblem_scale"
        )

        val sweepFraction = if (reducedMotion) {
            0.55f
        } else {
            val transition = rememberInfiniteTransition(label = "splash_bar_clock")
            val anim by transition.animateFloat(
                initialValue = -0.35f,
                targetValue = 1.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 850, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "splash_bar_sweep"
            )
            anim
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.mainBg)
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.accent.copy(alpha = if (colors.isDark) 0.14f else 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.44f),
                            radius = size.minDimension * 0.58f
                        )
                    )
                }
                .testTag("launch_splash_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.graphicsLayer {
                        scaleX = emblemScale
                        scaleY = emblemScale
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.10f))
                    )
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = colors.cardBg,
                        border = BorderStroke(1.5.dp, colors.accent.copy(alpha = 0.45f)),
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(92.dp)
                    ) {
                        Image(
                            painter = painterResource(id = settings.selectedIcon.drawableRes()),
                            contentDescription = "Cucalcus App Emblem",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(25.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "CUCALCUS",
                    fontFamily = displayFont,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    ),
                    color = colors.primaryText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Precision Scientific & Converter Suite",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.secondaryText
                )

                Spacer(modifier = Modifier.height(26.dp))

                Box(
                    modifier = Modifier
                        .width(124.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.border.copy(alpha = 0.55f))
                        .drawBehind {
                            val w = size.width
                            val startX = (sweepFraction - 0.35f) * w
                            val endX = (sweepFraction + 0.35f) * w
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        colors.accent,
                                        Color.Transparent
                                    ),
                                    startX = startX,
                                    endX = endX
                                )
                            )
                        }
                )
            }
        }
    }
}
