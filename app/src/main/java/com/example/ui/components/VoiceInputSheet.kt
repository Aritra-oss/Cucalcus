package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CucalcusTheme

/**
 * Animated listening indicator dialog displayed while speech capture is active.
 */
@Composable
fun VoiceListeningDialog(
    partialTranscript: String,
    onStopClick: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = CucalcusTheme.colors
    val reducedMotion = CucalcusTheme.reducedMotion

    val pulseScale = if (reducedMotion) {
        1f
    } else {
        rememberInfiniteTransition(label = "mic_pulse")
            .animateFloat(
                initialValue = 0.9f,
                targetValue = 1.18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(650, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "mic_ring_scale"
            ).value
    }

    BouncyDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Voice Input",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.primaryText
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("voice_listening_dialog"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.2f))
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Listening microphone",
                            tint = colors.onAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Text(
                    text = if (partialTranscript.isBlank()) {
                        "Speak an equation or educational question..."
                    } else {
                        partialTranscript
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (partialTranscript.isBlank()) colors.secondaryText else colors.primaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("CANCEL", color = colors.secondaryText)
            }
        },
        confirmButton = {
            TextButton(onClick = onStopClick) {
                Text("DONE", color = colors.accent, fontWeight = FontWeight.Bold)
            }
        }
    )
}
