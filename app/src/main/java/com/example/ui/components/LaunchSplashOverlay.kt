package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CucalcusTheme
import com.example.ui.theme.LocalDisplayFontFamily
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Pure black splash screen displaying only animated white "CUCALCUS" lettering
 * (no icon, no subtitle, no extra graphic chrome).
 */
@Composable
fun LaunchSplashOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val displayFont = LocalDisplayFontFamily.current
    val reducedMotion = CucalcusTheme.reducedMotion
    val exitMs = if (reducedMotion) 0 else 220

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(0)),
        exit = fadeOut(tween(exitMs)),
        modifier = modifier
    ) {
        val letters = remember { "CUCALCUS".toList() }
        val progress = remember { Animatable(if (reducedMotion) 1f else 0f) }

        LaunchedEffect(reducedMotion) {
            if (!reducedMotion) {
                launch {
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
                    )
                }
            } else {
                progress.snapTo(1f)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("launch_splash_overlay"),
            contentAlignment = Alignment.Center
        ) {
            val spacingSp = if (reducedMotion) 6f else (10f - 4f * progress.value)
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacingSp.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .testTag("launch_splash_brand_text")
            ) {
                letters.forEachIndexed { index, ch ->
                    val thresholdStart = index * 0.08f
                    val letterT = if (reducedMotion) {
                        1f
                    } else {
                        ((progress.value - thresholdStart) / 0.36f).coerceIn(0f, 1f)
                    }
                    Text(
                        text = ch.toString(),
                        fontFamily = displayFont,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        modifier = Modifier.graphicsLayer {
                            alpha = letterT
                            translationY = ((1f - letterT) * 14f).roundToInt().toFloat()
                            val sc = 0.88f + 0.12f * letterT
                            scaleX = sc
                            scaleY = sc
                        }
                    )
                }
            }
        }
    }
}
