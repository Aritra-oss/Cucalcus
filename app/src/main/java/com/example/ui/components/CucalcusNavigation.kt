package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CucalcusTheme
import kotlin.math.cos
import kotlin.math.sin

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
    val isMenuExpanded: Boolean = false,
    val onToggleMenu: () -> Unit = {},
    val onCloseMenu: () -> Unit = {},
    val onNavigate: (CucalcusRoute) -> Unit = {},
    val onScanProblem: () -> Unit = {},
    val onSubmitTypedProblem: (String) -> Unit = {}
)

val LocalCucalcusNavActions = staticCompositionLocalOf { CucalcusNavActions() }

/**
 * Animated 3-line hamburger icon that smoothly morphs into a symmetric Cross (X) when expanded.
 */
@Composable
fun AnimatedMenuToCloseIcon(
    expanded: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val reducedMotion = CucalcusTheme.reducedMotion
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMedium)
        },
        label = "menu_to_close_progress"
    )

    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.1.dp.toPx()
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val halfBar = w * 0.40f
        val offsetFromCenter = h * 0.28f

        val angleRad = (Math.PI / 4.0) * progress
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        val dx = halfBar * cosA
        val dyTop = halfBar * sinA
        val topCenterY = cy - offsetFromCenter * (1f - progress)
        val bottomCenterY = cy + offsetFromCenter * (1f - progress)

        drawLine(
            color = tint,
            start = Offset(cx - dx, topCenterY - dyTop),
            end = Offset(cx + dx, topCenterY + dyTop),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        val midAlpha = (1f - progress * 1.6f).coerceIn(0f, 1f)
        if (midAlpha > 0.01f) {
            val midHalf = halfBar * (1f - progress)
            drawLine(
                color = tint.copy(alpha = midAlpha),
                start = Offset(cx - midHalf, cy),
                end = Offset(cx + midHalf, cy),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }

        drawLine(
            color = tint,
            start = Offset(cx - dx, bottomCenterY + dyTop),
            end = Offset(cx + dx, bottomCenterY - dyTop),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Top-right 3-line / Cross toggle button. Opens/closes the inline squeezing menu card
 * rather than launching a floating popup window.
 */
@Composable
fun CucalcusMoreMenuButton(
    modifier: Modifier = Modifier,
    navActions: CucalcusNavActions = LocalCucalcusNavActions.current
) {
    val colors = CucalcusTheme.colors
    val expanded = navActions.isMenuExpanded

    Box(modifier = modifier.padding(end = 4.dp)) {
        BouncySurface(
            onClick = navActions.onToggleMenu,
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
                AnimatedMenuToCloseIcon(
                    expanded = expanded,
                    tint = if (expanded) colors.accent else colors.primaryText
                )
            }
        }
    }
}

/**
 * Inline collapsible card placed between the top bar and content (display box / screen body).
 * Displays 2 compact rows by default:
 *   Row 1: Calculator | History | Converter
 *   Row 2: Tools      | Settings | AI Mode
 * Tapping AI Mode expands the card to show "Scan a problem" & "Type a problem",
 * and tapping "Type a problem" expands an inline text input right inside the card.
 */
@Composable
fun InlineCucalcusMenuCard(
    visible: Boolean,
    navActions: CucalcusNavActions,
    modifier: Modifier = Modifier
) {
    val colors = CucalcusTheme.colors
    val reducedMotion = CucalcusTheme.reducedMotion

    var isAiModeExpanded by remember { mutableStateOf(false) }
    var isTypeProblemInputVisible by remember { mutableStateOf(false) }
    var typedProblemText by remember { mutableStateOf("") }
    val inputFocusRequester = remember { FocusRequester() }

    // Reset submenu state when the card is opened, not while it is closing.
    // Changing nested AnimatedVisibility targets during the parent's exit makes Compose
    // wait for the child collapse first, producing a visibly sequential close animation.
    LaunchedEffect(visible) {
        if (visible) {
            isAiModeExpanded = false
            isTypeProblemInputVisible = false
            typedProblemText = ""
        }
    }

    LaunchedEffect(isTypeProblemInputVisible) {
        if (isTypeProblemInputVisible) {
            runCatching { inputFocusRequester.requestFocus() }
        }
    }

    val submitTyped = {
        val trimmed = typedProblemText.trim()
        if (trimmed.isNotEmpty()) {
            typedProblemText = ""
            isTypeProblemInputVisible = false
            isAiModeExpanded = false
            navActions.onCloseMenu()
            navActions.onSubmitTypedProblem(trimmed)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = if (reducedMotion) {
            fadeIn(tween(0))
        } else {
            expandVertically(
                animationSpec = spring(
                    dampingRatio = 0.88f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                expandFrom = Alignment.Top
            ) + fadeIn(
                animationSpec = spring(
                    dampingRatio = 0.90f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + scaleIn(
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                initialScale = 0.965f,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
            )
        },
        exit = if (reducedMotion) {
            fadeOut(tween(0))
        } else {
            shrinkVertically(
                animationSpec = spring(
                    dampingRatio = 0.90f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                shrinkTowards = Alignment.Top
            ) + fadeOut(
                animationSpec = spring(
                    dampingRatio = 0.92f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + scaleOut(
                animationSpec = spring(
                    dampingRatio = 0.90f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                targetScale = 0.985f,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
            )
        },
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.elevatedSurface,
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .testTag("inline_menu_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Calculator, History, Converter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        CucalcusRoute.CALCULATOR,
                        CucalcusRoute.HISTORY,
                        CucalcusRoute.CONVERTER
                    ).forEach { dest ->
                        MenuDestinationChip(
                            label = dest.label,
                            icon = if (navActions.currentRoute == dest.route) dest.selectedIcon else dest.unselectedIcon,
                            isSelected = navActions.currentRoute == dest.route,
                            onClick = {
                                navActions.onCloseMenu()
                                navActions.onNavigate(dest)
                            },
                            testTag = "menu_nav_${dest.route}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Row 2: Tools, Settings, AI Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        CucalcusRoute.TOOLS,
                        CucalcusRoute.SETTINGS
                    ).forEach { dest ->
                        MenuDestinationChip(
                            label = dest.label,
                            icon = if (navActions.currentRoute == dest.route) dest.selectedIcon else dest.unselectedIcon,
                            isSelected = navActions.currentRoute == dest.route,
                            onClick = {
                                navActions.onCloseMenu()
                                navActions.onNavigate(dest)
                            },
                            testTag = "menu_nav_${dest.route}",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    MenuDestinationChip(
                        label = "AI Mode",
                        icon = if (isAiModeExpanded) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                        isSelected = isAiModeExpanded,
                        isAccentStyle = true,
                        onClick = {
                            isAiModeExpanded = !isAiModeExpanded
                            if (!isAiModeExpanded) {
                                isTypeProblemInputVisible = false
                            }
                        },
                        testTag = "menu_ai_mode_button",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Expanded AI Mode options: Scan a problem | Type a problem
                AnimatedVisibility(
                    visible = isAiModeExpanded,
                    enter = if (reducedMotion) {
                        fadeIn(tween(0))
                    } else {
                        expandVertically(
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            expandFrom = Alignment.Top
                        ) + fadeIn(tween(180))
                    },
                    exit = if (reducedMotion) {
                        fadeOut(tween(0))
                    } else {
                        shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = 0.92f,
                                stiffness = Spring.StiffnessMedium
                            ),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut(tween(130))
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.accent.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.42f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("menu_ai_mode_section")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BouncySurface(
                                    onClick = {
                                        isAiModeExpanded = false
                                        isTypeProblemInputVisible = false
                                        navActions.onCloseMenu()
                                        navActions.onScanProblem()
                                    },
                                    shape = RoundedCornerShape(13.dp),
                                    color = colors.accent.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.65f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("menu_scan_problem_button")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DocumentScanner,
                                            contentDescription = "Scan a problem",
                                            tint = colors.accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Scan a problem",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            ),
                                            color = colors.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                BouncySurface(
                                    onClick = {
                                        isTypeProblemInputVisible = !isTypeProblemInputVisible
                                    },
                                    shape = RoundedCornerShape(13.dp),
                                    color = if (isTypeProblemInputVisible) {
                                        colors.accent.copy(alpha = 0.22f)
                                    } else {
                                        colors.cardBg
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isTypeProblemInputVisible) colors.accent else colors.accent.copy(alpha = 0.55f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("menu_type_problem_button")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.EditNote,
                                            contentDescription = "Type a problem",
                                            tint = colors.accent,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Type a problem",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            ),
                                            color = colors.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Inline Type-a-problem input box shown when "Type a problem" is clicked
                            AnimatedVisibility(
                                visible = isTypeProblemInputVisible,
                                enter = if (reducedMotion) {
                                    fadeIn(tween(0))
                                } else {
                                    expandVertically(
                                        animationSpec = spring(
                                            dampingRatio = 0.86f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) + fadeIn(tween(150))
                                },
                                exit = if (reducedMotion) {
                                    fadeOut(tween(0))
                                } else {
                                    shrinkVertically(
                                        animationSpec = spring(
                                            dampingRatio = 0.92f,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    ) + fadeOut(tween(120))
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = typedProblemText,
                                        onValueChange = { typedProblemText = it },
                                        placeholder = {
                                            Text(
                                                text = "Enter a math problem...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.secondaryText.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                        keyboardActions = KeyboardActions(onSend = { submitTyped() }),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.accent,
                                            unfocusedBorderColor = colors.border,
                                            focusedTextColor = colors.primaryText,
                                            unfocusedTextColor = colors.primaryText,
                                            cursorColor = colors.accent,
                                            focusedContainerColor = colors.secondaryBg,
                                            unfocusedContainerColor = colors.secondaryBg
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusRequester(inputFocusRequester)
                                            .testTag("menu_type_problem_input")
                                    )

                                    Button(
                                        onClick = submitTyped,
                                        enabled = typedProblemText.isNotBlank(),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.accent,
                                            contentColor = colors.onAccent,
                                            disabledContainerColor = colors.cardBg,
                                            disabledContentColor = colors.secondaryText.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier
                                            .heightIn(min = 50.dp)
                                            .testTag("menu_type_problem_submit_button")
                                    ) {
                                        Text(
                                            text = "SOLVE",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            )
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
}

@Composable
private fun MenuDestinationChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    isAccentStyle: Boolean = false
) {
    val colors = CucalcusTheme.colors
    val tintColor = when {
        isSelected -> colors.accent
        isAccentStyle -> colors.accent
        else -> colors.secondaryText
    }
    BouncySurface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = when {
            isSelected -> colors.accent.copy(alpha = 0.16f)
            isAccentStyle -> colors.accent.copy(alpha = 0.08f)
            else -> colors.cardBg
        },
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isSelected -> colors.accent
                isAccentStyle -> colors.accent.copy(alpha = 0.45f)
                else -> colors.border
            }
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected || isAccentStyle) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = if (isSelected) colors.accent else colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
