package com.example.ui.tools

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.tools.ToolCategory
import com.example.domain.tools.ToolId
import com.example.ui.components.SwissScreenTopBar
import com.example.ui.components.SwissSectionHeader
import com.example.ui.components.ToolsBodySkeleton
import com.example.ui.theme.CucalcusTheme
import kotlinx.coroutines.delay

@Composable
fun ToolsScreen(viewModel: ToolsViewModel) {
    val activeTool by viewModel.selectedTool.collectAsStateWithLifecycle()
    val recentTools by viewModel.recentTools.collectAsStateWithLifecycle()
    val colors = CucalcusTheme.colors

    if (activeTool != null) {
        val tool = activeTool!!
        BackHandler { viewModel.closeTool() }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SwissScreenTopBar(
                title = tool.title,
                tag = tool.category.title
            ) {
                IconButton(
                    onClick = viewModel::closeTool,
                    modifier = Modifier.testTag("tool_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back to tools",
                        tint = colors.accent
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                ToolWorkspaceContent(toolId = tool)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    } else {
        var isDirectoryReady by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(160)
            isDirectoryReady = true
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SwissScreenTopBar(
                title = "Mathematical Tools",
                tag = "23 Precision Utilities"
            )

            AnimatedContent(
                targetState = isDirectoryReady,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "tools_skeleton_transition",
                modifier = Modifier.weight(1f)
            ) { ready ->
                if (!ready) {
                    ToolsBodySkeleton()
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                if (recentTools.isNotEmpty()) {
                    SwissSectionHeader(
                        title = "Quick Tools",
                        subtitle = "Recently used utilities"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentTools.forEach { tool ->
                            Surface(
                                onClick = { viewModel.openTool(tool) },
                                shape = RoundedCornerShape(50),
                                color = colors.accent.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("quick_tool_${tool.name}")
                            ) {
                                Text(
                                    text = tool.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.accent,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                ToolCategory.entries.forEach { category ->
                    val tools = ToolId.entries.filter { it.category == category }
                    SwissSectionHeader(
                        title = category.title,
                        subtitle = "${tools.size} calculators"
                    )
                    tools.forEach { tool ->
                        key(tool.name) {
                            ToolDirectoryCard(
                                tool = tool,
                                onClick = { viewModel.openTool(tool) }
                            )
                        }
                    }
                }
            }
        }
    }
}
}
}

@Composable
private fun ToolDirectoryCard(
    tool: ToolId,
    onClick: () -> Unit
) {
    val colors = CucalcusTheme.colors
    val icon = when (tool.category) {
        ToolCategory.FINANCIAL -> Icons.Outlined.AccountBalance
        ToolCategory.MATHEMATICAL -> Icons.Outlined.Calculate
        ToolCategory.DATE_TIME -> Icons.Outlined.Schedule
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBg,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tool_card_${tool.name}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tool.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
