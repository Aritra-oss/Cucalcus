package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CucalcusTheme

enum class CucalcusRoute(
    val route: String,
    val label: String,
    val subtitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CALCULATOR("calculator", "Calculator", "Standard & Scientific", Icons.Filled.Calculate, Icons.Outlined.Calculate),
    HISTORY("history", "History", "Saved Archive & Exports", Icons.Filled.History, Icons.Outlined.History),
    CONVERTER("converter", "Converter", "15 Unit Systems & Currency", Icons.Filled.SwapHoriz, Icons.Outlined.SwapHoriz),
    TOOLS("tools", "Tools", "23 Math, Finance & Date Tools", Icons.Filled.Build, Icons.Outlined.Build),
    SETTINGS("settings", "Settings", "Themes, Icons & Studio", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Immutable
data class CucalcusNavActions(
    val currentRoute: String = CucalcusRoute.CALCULATOR.route,
    val onNavigate: (CucalcusRoute) -> Unit = {}
)

val LocalCucalcusNavActions = staticCompositionLocalOf { CucalcusNavActions() }

/**
 * 3-line top-right "More" menu button replacing the bottom navigation bar.
 * Opens a smooth bouncy popup menu cleanly inset from the screen edge.
 */
@Composable
fun CucalcusMoreMenuButton(
    modifier: Modifier = Modifier,
    navActions: CucalcusNavActions = LocalCucalcusNavActions.current
) {
    val colors = CucalcusTheme.colors
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.padding(end = 4.dp)) {
        BouncySurface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = if (expanded) colors.accent.copy(alpha = 0.16f) else colors.cardBg,
            border = BorderStroke(
                width = 1.dp,
                color = if (expanded) colors.accent else colors.border
            ),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("top_more_menu_button")
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Open sections menu",
                    tint = if (expanded) colors.accent else colors.primaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        BouncyPopupMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            transformOrigin = TransformOrigin(0.92f, 0.04f),
            shape = RoundedCornerShape(22.dp),
            containerColor = colors.elevatedSurface,
            borderColor = colors.border,
            modifier = Modifier
                .widthIn(min = 248.dp, max = 290.dp)
                .padding(top = 48.dp)
                .testTag("top_more_dropdown")
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "SECTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.8.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.accent,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                HorizontalDivider(thickness = 0.8.dp, color = colors.border.copy(alpha = 0.7f))

                CucalcusRoute.entries.forEach { dest ->
                    val isSelected = navActions.currentRoute == dest.route
                    val itemInteraction = remember { MutableInteractionSource() }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .bouncyPress(itemInteraction, pressedScale = 0.96f)
                            .background(
                                if (isSelected) colors.accent.copy(alpha = 0.10f) else Color.Transparent
                            )
                            .clickable(interactionSource = itemInteraction, indication = null) {
                                expanded = false
                                navActions.onNavigate(dest)
                            }
                            .padding(horizontal = 10.dp, vertical = 9.dp)
                            .testTag("menu_nav_${dest.route}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) colors.accent.copy(alpha = 0.16f)
                                    else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = null,
                                tint = if (isSelected) colors.accent else colors.secondaryText,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dest.label,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) colors.accent else colors.primaryText
                            )
                            Text(
                                text = dest.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondaryText
                            )
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                            )
                        }
                    }
                }
            }
        }
    }
}
