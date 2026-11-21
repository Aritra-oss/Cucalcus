package com.example.ui.calculator

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.CucalcusSettings
import com.example.data.preferences.KeypadArrangement
import com.example.domain.math.AngleMode
import com.example.domain.math.NumberFormatMode
import com.example.domain.math.NumberFormatter
import com.example.ui.components.BouncySurface
import com.example.ui.components.CalculatorKeyButton
import com.example.ui.components.ClipboardMathBanner
import com.example.ui.components.CucalcusMoreMenuButton
import com.example.ui.components.KeypadKeyKind
import com.example.ui.components.bouncyPress
import com.example.ui.theme.CucalcusTheme
import com.example.ui.theme.LocalDisplayFontFamily
import kotlinx.coroutines.awaitCancellation
import kotlin.math.abs

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onShowMessage: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val colors = CucalcusTheme.colors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        val clip = clipboard.getText()?.text
        viewModel.checkClipboardCandidate(clip)
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val isLandscape = maxWidth > maxHeight && maxWidth > 540.dp
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.44f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    CalculatorTopControls(
                        state = state,
                        settings = settings,
                        onToggleAngle = viewModel::toggleAngleMode,
                        onToggleSci = viewModel::toggleScientific,
                        onCopy = {
                            val text = state.livePreview.ifBlank { state.formattedExpression }
                            if (text.isNotBlank()) {
                                clipboard.setText(AnnotatedString(text))
                                onShowMessage("Copied to clipboard")
                            }
                        },
                        onShare = {
                            shareCalculation(context, state)
                        }
                    )
                    ClipboardMathBanner(
                        expression = state.clipboardCandidate,
                        onEvaluate = viewModel::useClipboardCandidate,
                        onDismiss = viewModel::dismissClipboardCandidate
                    )
                    CalculatorDisplayPanel(
                        state = state,
                        settings = settings,
                        onCursorPositionChange = viewModel::onCursorPositionChange,
                        onToggleCursorLock = {
                            viewModel.toggleCursorLock()
                            val msg = if (!state.isCursorLocked) "Display cursor locked" else "Display cursor unlocked"
                            onShowMessage(msg)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MemoryControlRow(
                        memoryValue = state.memoryValue,
                        borderless = settings.borderlessButtons,
                        onClear = viewModel::onMemoryClear,
                        onRecall = viewModel::onMemoryRecall,
                        onAdd = viewModel::onMemoryAdd,
                        onSubtract = viewModel::onMemorySubtract,
                        onBackspace = viewModel::onBackspace
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(0.56f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    ScientificPanel(
                        visible = state.isScientificExpanded,
                        isInverse = state.isInverseSci,
                        spacing = settings.buttonSpacingDp.dp,
                        onToggleInverse = viewModel::toggleInverseSci,
                        onFunction = viewModel::onScientificFunction,
                        onOperator = viewModel::onOperator,
                        onOpenParen = viewModel::onOpenParen,
                        onCloseParen = viewModel::onCloseParen,
                        onToggleSign = viewModel::onToggleSign
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StandardKeypadGrid(
                        settings = settings,
                        onDigit = viewModel::onDigit,
                        onOperator = viewModel::onOperator,
                        onPercent = viewModel::onPercent,
                        onParen = viewModel::onParenthesis,
                        onToggleSign = viewModel::onToggleSign,
                        onClear = viewModel::onClear,
                        onEquals = viewModel::onEquals,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp)
            ) {
                CalculatorTopControls(
                    state = state,
                    settings = settings,
                    onToggleAngle = viewModel::toggleAngleMode,
                    onToggleSci = viewModel::toggleScientific,
                    onCopy = {
                        val text = state.livePreview.ifBlank { state.formattedExpression }
                        if (text.isNotBlank()) {
                            clipboard.setText(AnnotatedString(text))
                            onShowMessage("Copied to clipboard")
                        }
                    },
                    onShare = {
                        shareCalculation(context, state)
                    }
                )
                ClipboardMathBanner(
                    expression = state.clipboardCandidate,
                    onEvaluate = viewModel::useClipboardCandidate,
                    onDismiss = viewModel::dismissClipboardCandidate
                )
                CalculatorDisplayPanel(
                    state = state,
                    settings = settings,
                    onCursorPositionChange = viewModel::onCursorPositionChange,
                    onToggleCursorLock = {
                        viewModel.toggleCursorLock()
                        val msg = if (!state.isCursorLocked) "Display cursor locked" else "Display cursor unlocked"
                        onShowMessage(msg)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.34f)
                )
                MemoryControlRow(
                    memoryValue = state.memoryValue,
                    borderless = settings.borderlessButtons,
                    onClear = viewModel::onMemoryClear,
                    onRecall = viewModel::onMemoryRecall,
                    onAdd = viewModel::onMemoryAdd,
                    onSubtract = viewModel::onMemorySubtract,
                    onBackspace = viewModel::onBackspace
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.66f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    ScientificPanel(
                        visible = state.isScientificExpanded,
                        isInverse = state.isInverseSci,
                        spacing = settings.buttonSpacingDp.dp,
                        onToggleInverse = viewModel::toggleInverseSci,
                        onFunction = viewModel::onScientificFunction,
                        onOperator = viewModel::onOperator,
                        onOpenParen = viewModel::onOpenParen,
                        onCloseParen = viewModel::onCloseParen,
                        onToggleSign = viewModel::onToggleSign
                    )
                    StandardKeypadGrid(
                        settings = settings,
                        onDigit = viewModel::onDigit,
                        onOperator = viewModel::onOperator,
                        onPercent = viewModel::onPercent,
                        onParen = viewModel::onParenthesis,
                        onToggleSign = viewModel::onToggleSign,
                        onClear = viewModel::onClear,
                        onEquals = viewModel::onEquals,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun shareCalculation(context: android.content.Context, state: CalculatorUiState) {
    val text = when {
        state.formattedExpression.isNotBlank() && state.livePreview.isNotBlank() ->
            "${state.formattedExpression} = ${state.livePreview}\n— Computed with Cucalcus"
        state.formattedExpression.isNotBlank() ->
            "${state.formattedExpression}\n— Computed with Cucalcus"
        else -> return
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Calculation"))
}

@Composable
private fun CalculatorTopControls(
    state: CalculatorUiState,
    settings: CucalcusSettings,
    onToggleAngle: () -> Unit,
    onToggleSci: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val colors = CucalcusTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 22.dp, top = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "CUCALCUS",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            BouncySurface(
                onClick = onToggleAngle,
                shape = RoundedCornerShape(50),
                color = colors.cardBg,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.testTag("angle_mode_toggle")
            ) {
                Text(
                    text = state.angleMode.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.accent,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (settings.showScientificToggle) {
                BouncySurface(
                    onClick = onToggleSci,
                    shape = RoundedCornerShape(50),
                    color = if (state.isScientificExpanded) colors.accent.copy(alpha = 0.16f) else colors.cardBg,
                    border = BorderStroke(
                        1.dp,
                        if (state.isScientificExpanded) colors.accent else colors.border
                    ),
                    modifier = Modifier.testTag("scientific_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Science,
                            contentDescription = "Toggle scientific keypad",
                            tint = if (state.isScientificExpanded) colors.accent else colors.secondaryText,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "SCI",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (state.isScientificExpanded) colors.accent else colors.secondaryText
                        )
                    }
                }
            }
            val copyInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onCopy,
                interactionSource = copyInteraction,
                modifier = Modifier
                    .size(40.dp)
                    .bouncyPress(copyInteraction, pressedScale = 0.88f)
                    .testTag("copy_result_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy result",
                    tint = colors.secondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
            val shareInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onShare,
                interactionSource = shareInteraction,
                modifier = Modifier
                    .size(40.dp)
                    .bouncyPress(shareInteraction, pressedScale = 0.88f)
                    .testTag("share_calc_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share calculation",
                    tint = colors.secondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
            CucalcusMoreMenuButton()
        }
    }
}

internal class ExpressionFormatTransformation(
    private val formatMode: NumberFormatMode
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val formatted = NumberFormatter.formatExpressionNumbers(raw, formatMode)
        if (formatted == raw) {
            return TransformedText(AnnotatedString(formatted), OffsetMapping.Identity)
        }
        val origToTrans = IntArray(raw.length + 1)
        val transToOrig = IntArray(formatted.length + 1)
        var ri = 0
        var fi = 0
        while (ri < raw.length && fi < formatted.length) {
            if (formatted[fi] == ',' && raw[ri] != ',') {
                transToOrig[fi] = ri
                fi++
            } else {
                origToTrans[ri] = fi
                transToOrig[fi] = ri
                ri++
                fi++
            }
        }
        while (fi < formatted.length) {
            transToOrig[fi] = raw.length
            fi++
        }
        origToTrans[raw.length] = formatted.length
        transToOrig[formatted.length] = raw.length

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                origToTrans[offset.coerceIn(0, raw.length)]

            override fun transformedToOriginal(offset: Int): Int =
                transToOrig[offset.coerceIn(0, formatted.length)]
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CalculatorDisplayPanel(
    state: CalculatorUiState,
    settings: CucalcusSettings,
    onCursorPositionChange: (Int) -> Unit,
    onToggleCursorLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CucalcusTheme.colors
    val displayFont = LocalDisplayFontFamily.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.isCursorLocked) {
        if (state.isCursorLocked) {
            focusManager.clearFocus()
        }
    }

    val displayText = state.formattedExpression.ifEmpty { "0" }
    val dynamicSp = when {
        displayText.length > 22 -> 24f
        displayText.length > 16 -> 30f
        displayText.length > 11 -> 38f
        else -> 48f
    } * settings.fontScale

    val hasMemory = abs(state.memoryValue) > 1e-14
    val formatTag = when (settings.numberFormatMode) {
        NumberFormatMode.INTERNATIONAL -> "INT"
        NumberFormatMode.INDIAN -> "IND"
        NumberFormatMode.SCIENTIFIC -> "SCI"
    }
    val visualTransformation = remember(settings.numberFormatMode) {
        ExpressionFormatTransformation(settings.numberFormatMode)
    }
    val suppressImeInterceptor = remember {
        PlatformTextInputInterceptor { _, _ -> awaitCancellation() }
    }
    val clampedCursor = state.cursorPosition.coerceIn(0, state.expression.length)
    val textFieldValue = TextFieldValue(
        text = state.expression,
        selection = TextRange(clampedCursor)
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.secondaryBg,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$formatTag · ${settings.decimalPrecision}D",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = colors.secondaryText.copy(alpha = 0.8f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasMemory) {
                        val formattedMem = NumberFormatter.formatResult(
                            state.memoryValue,
                            settings.decimalPrecision.coerceAtMost(4),
                            NumberFormatMode.INTERNATIONAL
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.accent.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "M $formattedMem",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Corner cursor lock button
                    BouncySurface(
                        onClick = onToggleCursorLock,
                        shape = RoundedCornerShape(50),
                        color = if (state.isCursorLocked) colors.accent.copy(alpha = 0.18f) else colors.cardBg,
                        border = BorderStroke(
                            1.dp,
                            if (state.isCursorLocked) colors.accent else colors.border
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("display_cursor_lock_button")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isCursorLocked) Icons.Filled.Lock else Icons.Outlined.LockOpen,
                                contentDescription = if (state.isCursorLocked) "Unlock display cursor" else "Lock display cursor",
                                tint = if (state.isCursorLocked) colors.accent else colors.secondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                InterceptPlatformTextInput(interceptor = suppressImeInterceptor) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { updated ->
                            if (!state.isCursorLocked && updated.text == state.expression) {
                                onCursorPositionChange(updated.selection.end)
                            }
                        },
                        enabled = !state.isCursorLocked,
                        singleLine = true,
                        visualTransformation = visualTransformation,
                        cursorBrush = if (state.isCursorLocked) SolidColor(colors.accent.copy(alpha = 0f)) else SolidColor(colors.accent),
                        textStyle = TextStyle(
                            fontFamily = displayFont,
                            fontSize = dynamicSp.sp,
                            fontWeight = FontWeight.Light,
                            color = if (state.expression.isEmpty()) colors.secondaryText.copy(alpha = 0.55f) else colors.primaryText,
                            textAlign = TextAlign.End
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calculator_display"),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (state.expression.isEmpty()) {
                                    Text(
                                        text = "0",
                                        fontFamily = displayFont,
                                        fontSize = dynamicSp.sp,
                                        fontWeight = FontWeight.Light,
                                        color = colors.secondaryText.copy(alpha = 0.55f),
                                        maxLines = 1,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                AnimatedContent(
                    targetState = state.errorMessage ?: state.livePreview,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "preview_anim"
                ) { text ->
                    val isError = state.errorMessage != null
                    Text(
                        text = if (isError) text else if (text.isNotEmpty()) "= $text" else " ",
                        fontFamily = displayFont,
                        fontSize = (if (isError) 15f else 22f * settings.fontScale).sp,
                        fontWeight = if (isError) FontWeight.Medium else FontWeight.Normal,
                        color = if (isError) colors.error else colors.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calculator_preview")
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryControlRow(
    memoryValue: Double,
    borderless: Boolean,
    onClear: () -> Unit,
    onRecall: () -> Unit,
    onAdd: () -> Unit,
    onSubtract: () -> Unit,
    onBackspace: () -> Unit
) {
    val colors = CucalcusTheme.colors
    val hasMemory = abs(memoryValue) > 1e-14
    val keyBorder = if (borderless) null else BorderStroke(1.dp, colors.border)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "MC" to onClear,
                "MR" to onRecall,
                "M+" to onAdd,
                "M−" to onSubtract
            ).forEach { (label, action) ->
                BouncySurface(
                    onClick = action,
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardBg,
                    border = keyBorder,
                    modifier = Modifier.testTag("mem_$label")
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (label in listOf("MC", "MR") && !hasMemory) {
                            colors.secondaryText.copy(alpha = 0.45f)
                        } else {
                            colors.primaryText
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        BouncySurface(
            onClick = onBackspace,
            shape = RoundedCornerShape(14.dp),
            color = colors.elevatedSurface,
            border = keyBorder,
            modifier = Modifier.testTag("key_backspace")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = "Backspace",
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "DEL",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.primaryText
                )
            }
        }
    }
}

@Composable
private fun ScientificPanel(
    visible: Boolean,
    isInverse: Boolean,
    spacing: Dp,
    onToggleInverse: () -> Unit,
    onFunction: (String) -> Unit,
    onOperator: (String) -> Unit,
    onOpenParen: () -> Unit,
    onCloseParen: () -> Unit,
    onToggleSign: () -> Unit
) {
    val reducedMotion = CucalcusTheme.reducedMotion
    AnimatedVisibility(
        visible = visible,
        enter = if (reducedMotion) fadeIn() else fadeIn() + expandVertically(),
        exit = if (reducedMotion) fadeOut() else fadeOut() + shrinkVertically()
    ) {
        val row1 = if (isInverse) {
            listOf("2nd", "sin⁻¹", "cos⁻¹", "tan⁻¹", "eˣ", "10ˣ")
        } else {
            listOf("2nd", "sin", "cos", "tan", "ln", "log")
        }
        val row2 = listOf("√", "x²", "^", "1/x", "x!", "mod")
        val row3 = listOf("π", "e", "|x|", "(", ")", "+/−")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            listOf(row1, row2, row3).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    row.forEach { key ->
                        val kind = when {
                            key == "2nd" && isInverse -> KeypadKeyKind.OPERATOR
                            key in listOf("^", "mod") -> KeypadKeyKind.OPERATOR
                            else -> KeypadKeyKind.FUNCTION
                        }
                        CalculatorKeyButton(
                            label = key,
                            keyKind = kind,
                            onClick = {
                                when (key) {
                                    "2nd" -> onToggleInverse()
                                    "^", "mod" -> onOperator(key)
                                    "(" -> onOpenParen()
                                    ")" -> onCloseParen()
                                    "+/−" -> onToggleSign()
                                    else -> onFunction(key)
                                }
                            },
                            fontSize = 12.sp,
                            tagName = "sci_$key",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StandardKeypadGrid(
    settings: CucalcusSettings,
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onPercent: () -> Unit,
    onParen: () -> Unit,
    onToggleSign: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = settings.buttonSpacingDp.dp
    val leftHanded = settings.keypadArrangement == KeypadArrangement.LEFT_HANDED

    val topRowKeys = buildList {
        add("AC")
        add("( )")
        if (settings.showPercent) add("%") else add("+/−")
        add("÷")
    }

    val digitRows = listOf(
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+")
    )

    // Dynamically adapt bottom row based on 00, 000, and decimal settings without conflicts
    val bottomRowKeys = buildList {
        if (!settings.showDoubleZero && !settings.showTripleZero) {
            add("+/−")
            add("0")
            if (settings.showDecimal) add(".")
            add("=")
        } else {
            add("0")
            if (settings.showDoubleZero) add("00")
            if (settings.showTripleZero) add("000")
            if (settings.showDecimal) add(".")
            add("=")
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // Row 1: AC, (), %, ÷ aligned cleanly on 4-column Swiss grid
        val orderedTop = if (leftHanded) listOf(topRowKeys.last()) + topRowKeys.dropLast(1) else topRowKeys
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            orderedTop.forEach { key ->
                val kind = when (key) {
                    "AC" -> KeypadKeyKind.CLEAR
                    "÷" -> KeypadKeyKind.OPERATOR
                    else -> KeypadKeyKind.FUNCTION
                }
                CalculatorKeyButton(
                    label = key,
                    keyKind = kind,
                    onClick = {
                        when (key) {
                            "AC" -> onClear()
                            "( )" -> onParen()
                            "%" -> onPercent()
                            "+/−" -> onToggleSign()
                            else -> onOperator(key)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }

        // Rows 2-4: Digits + Operators
        digitRows.forEach { row ->
            val ordered = if (leftHanded) listOf(row.last()) + row.dropLast(1) else row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                ordered.forEach { key ->
                    val isOp = key in listOf("×", "−", "+")
                    CalculatorKeyButton(
                        label = key,
                        keyKind = if (isOp) KeypadKeyKind.OPERATOR else KeypadKeyKind.DIGIT,
                        onClick = {
                            if (isOp) onOperator(key) else onDigit(key)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }

        // Row 5: Adaptive bottom row
        val orderedBottom = if (leftHanded) listOf(bottomRowKeys.last()) + bottomRowKeys.dropLast(1) else bottomRowKeys
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            orderedBottom.forEach { key ->
                val kind = when (key) {
                    "=" -> KeypadKeyKind.EQUALS
                    "+/−" -> KeypadKeyKind.FUNCTION
                    else -> KeypadKeyKind.DIGIT
                }
                CalculatorKeyButton(
                    label = key,
                    keyKind = kind,
                    onClick = {
                        when (key) {
                            "=" -> onEquals()
                            "+/−" -> onToggleSign()
                            else -> onDigit(key)
                        }
                    },
                    modifier = Modifier
                        .weight(if (key == "0" && orderedBottom.size == 3) 1.5f else 1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}
