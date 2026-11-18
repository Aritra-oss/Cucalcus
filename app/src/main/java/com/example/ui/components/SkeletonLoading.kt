package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CucalcusTheme

/**
 * Shared shimmer progress clock hoisted once per skeleton container.
 * Uses drawBehind so the GPU redraws the gradient without triggering Compose recomposition.
 */
@Composable
fun rememberSkeletonShimmerProgress(): State<Float> {
    if (CucalcusTheme.reducedMotion) {
        return remember { mutableFloatStateOf(0.5f) }
    }
    val transition = rememberInfiniteTransition(label = "skeleton_shimmer_clock")
    return transition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeleton_shimmer_progress"
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    shimmerProgress: State<Float>? = null
) {
    val progress = shimmerProgress ?: rememberSkeletonShimmerProgress()
    val colors = CucalcusTheme.colors
    val baseColor = if (colors.isDark) {
        colors.cardBg.copy(alpha = 0.78f)
    } else {
        colors.border.copy(alpha = 0.50f)
    }
    val highlightColor = if (colors.isDark) {
        colors.accent.copy(alpha = 0.22f)
    } else {
        colors.accent.copy(alpha = 0.16f)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                val p = progress.value
                val w = size.width.coerceAtLeast(1f)
                val h = size.height.coerceAtLeast(1f)
                val startX = (p - 0.45f) * w
                val endX = (p + 0.45f) * w
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(baseColor, highlightColor, baseColor),
                        start = Offset(startX, 0f),
                        end = Offset(endX, h)
                    )
                )
            }
    )
}

@Composable
fun SkeletonTopBar(
    titleWidth: Dp = 160.dp,
    tagWidth: Dp = 80.dp,
    shimmerProgress: State<Float> = rememberSkeletonShimmerProgress()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                SkeletonBox(
                    modifier = Modifier.size(tagWidth, 12.dp),
                    shape = RoundedCornerShape(6.dp),
                    shimmerProgress = shimmerProgress
                )
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBox(
                    modifier = Modifier.size(titleWidth, 26.dp),
                    shape = RoundedCornerShape(8.dp),
                    shimmerProgress = shimmerProgress
                )
            }
            SkeletonBox(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                shimmerProgress = shimmerProgress
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            shape = RoundedCornerShape(50),
            shimmerProgress = shimmerProgress
        )
    }
}

@Composable
fun SkeletonPillRow(
    itemCount: Int = 5,
    shimmerProgress: State<Float> = rememberSkeletonShimmerProgress()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val widths = listOf(64.dp, 88.dp, 82.dp, 74.dp, 92.dp, 78.dp)
        for (i in 0 until itemCount) {
            val w = widths[i % widths.size]
            SkeletonBox(
                modifier = Modifier.size(w, 36.dp),
                shape = RoundedCornerShape(50),
                shimmerProgress = shimmerProgress
            )
        }
    }
}

/**
 * Lightweight skeleton body for Settings & Studio while content composes and icon bitmaps warm up.
 */
@Composable
fun SettingsBodySkeleton(
    modifier: Modifier = Modifier
) {
    val colors = CucalcusTheme.colors
    val progress = rememberSkeletonShimmerProgress()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("settings_skeleton_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header skeleton
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SkeletonBox(
                modifier = Modifier.size(150.dp, 14.dp),
                shape = RoundedCornerShape(6.dp),
                shimmerProgress = progress
            )
            SkeletonBox(
                modifier = Modifier.size(220.dp, 12.dp),
                shape = RoundedCornerShape(6.dp),
                shimmerProgress = progress
            )
        }

        // Theme / Color swatch grid card skeleton
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.cardBg.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, colors.border.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (r in 0 until 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (c in 0 until 4) {
                            SkeletonBox(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                shimmerProgress = progress
                            )
                        }
                    }
                }
            }
        }

        // Live Icon / Backdrop Preview Card Skeleton
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.cardBg.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, colors.border.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SkeletonBox(
                    modifier = Modifier.size(68.dp),
                    shape = RoundedCornerShape(18.dp),
                    shimmerProgress = progress
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkeletonBox(
                        modifier = Modifier.size(110.dp, 12.dp),
                        shape = RoundedCornerShape(6.dp),
                        shimmerProgress = progress
                    )
                    SkeletonBox(
                        modifier = Modifier.size(165.dp, 18.dp),
                        shape = RoundedCornerShape(8.dp),
                        shimmerProgress = progress
                    )
                    SkeletonBox(
                        modifier = Modifier.size(130.dp, 13.dp),
                        shape = RoundedCornerShape(6.dp),
                        shimmerProgress = progress
                    )
                }
            }
        }

        // 2x2 Grid of Icon/Option Cards Skeleton
        for (r in 0 until 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (c in 0 until 2) {
                    SkeletonBox(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp),
                        shape = RoundedCornerShape(20.dp),
                        shimmerProgress = progress
                    )
                }
            }
        }

        // Sliders & Toggles Skeleton
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            shape = RoundedCornerShape(20.dp),
            shimmerProgress = progress
        )
    }
}

/**
 * Lightweight skeleton body for Converter while unit/currency workspace loads.
 */
@Composable
fun ConverterBodySkeleton(
    modifier: Modifier = Modifier
) {
    val progress = rememberSkeletonShimmerProgress()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("converter_skeleton_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SkeletonPillRow(itemCount = 5, shimmerProgress = progress)

        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp),
            shape = RoundedCornerShape(22.dp),
            shimmerProgress = progress
        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SkeletonBox(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                shimmerProgress = progress
            )
        }

        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp),
            shape = RoundedCornerShape(22.dp),
            shimmerProgress = progress
        )

        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(18.dp),
            shimmerProgress = progress
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (i in 0 until 4) {
                SkeletonBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    shimmerProgress = progress
                )
            }
        }
    }
}

/**
 * Lightweight skeleton body for Mathematical Tools directory.
 */
@Composable
fun ToolsBodySkeleton(
    modifier: Modifier = Modifier
) {
    val progress = rememberSkeletonShimmerProgress()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("tools_skeleton_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkeletonPillRow(itemCount = 4, shimmerProgress = progress)
        SkeletonBox(
            modifier = Modifier.size(160.dp, 16.dp),
            shape = RoundedCornerShape(8.dp),
            shimmerProgress = progress
        )
        for (i in 0 until 5) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp),
                shape = RoundedCornerShape(20.dp),
                shimmerProgress = progress
            )
        }
    }
}

/**
 * Lightweight skeleton body for Calculation History archive.
 */
@Composable
fun HistoryBodySkeleton(
    modifier: Modifier = Modifier
) {
    val progress = rememberSkeletonShimmerProgress()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("history_skeleton_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(20.dp),
            shimmerProgress = progress
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                shape = RoundedCornerShape(50),
                shimmerProgress = progress
            )
            SkeletonBox(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                shape = RoundedCornerShape(50),
                shimmerProgress = progress
            )
        }
        for (i in 0 until 5) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp),
                shape = RoundedCornerShape(20.dp),
                shimmerProgress = progress
            )
        }
    }
}
