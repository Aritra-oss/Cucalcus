package com.example.ui.settings

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SmartButton
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.AccentPreset
import com.example.data.preferences.AppBackgroundPreset
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.ButtonShapeOption
import com.example.data.preferences.CucalcusSettings
import com.example.data.preferences.DisplayBoxAnimation
import com.example.data.preferences.ButtonAnimation
import com.example.data.preferences.FontOption
import com.example.data.preferences.HapticStrength
import com.example.data.preferences.KeypadArrangement
import com.example.domain.math.AngleMode
import com.example.domain.math.NumberFormatMode
import com.example.ui.components.BouncyDialog
import com.example.ui.components.BouncySurface
import com.example.ui.components.CalculatorKeyButton
import com.example.ui.components.KeypadKeyKind
import com.example.ui.components.SettingsBodySkeleton
import com.example.ui.components.SwissScreenTopBar
import com.example.ui.components.SwissSectionHeader
import com.example.ui.components.bouncyPress
import com.example.ui.components.toComposeShape
import com.example.ui.theme.CucalcusFonts
import com.example.ui.theme.CucalcusTheme
import com.example.ui.theme.contrastOnColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private enum class SettingsFilter(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Outlined.GridView),
    THEME_BG("Theme & Bg", Icons.Outlined.Palette),
    TYPOGRAPHY("Typography", Icons.Outlined.FormatSize),
    BUTTONS("Buttons", Icons.Outlined.SmartButton),
    ICONS("App Icons", Icons.Outlined.Apps),
    CALCULATOR("Calculator", Icons.Outlined.Calculate),
    BACKUP("Backup", Icons.Outlined.Backup)
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onShowMessage: (String) -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val colors = CucalcusTheme.colors
    val animationPreviewTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "settings_animation_previews")
    val animationPreviewPhase by animationPreviewTransition.animateFloat(0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(3600, easing = androidx.compose.animation.core.LinearEasing)), label = "settings_preview_phase")
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var isPreparingSettings by remember { mutableStateOf(true) }

    var selectedFilter by remember { mutableStateOf(SettingsFilter.ALL) }
    var showCustomColorDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    val filterScrollState = rememberScrollState()
    val bodyScrollState = rememberScrollState()
    val reducedMotion = CucalcusTheme.reducedMotion

    LaunchedEffect(Unit) {
    delay(500)

    if (!BrandIconBitmapCache.isWarmedUp) {
        BrandIconBitmapCache.warmUp(context)
    }

    isPreparingSettings = false
    }
    

    LaunchedEffect(selectedFilter) {
        if (!reducedMotion && bodyScrollState.value > 0) {
            bodyScrollState.animateScrollTo(
                value = 0,
                animationSpec = spring(
                    dampingRatio = 0.88f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            bodyScrollState.scrollTo(0)
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackgroundImageFromUri(uri) { ok ->
                onShowMessage(
                    if (ok) "Gallery background applied with subtle dim & blur"
                    else "Could not load selected image"
                )
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        SwissScreenTopBar(
            title = "Settings & Studio",
            tag = "Personalisation"
        )

        // Studio Category Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(filterScrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsFilter.entries.forEachIndexed { idx, filter ->
                val isSelected = selectedFilter == filter
                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) colors.accent else colors.cardBg.copy(alpha = 0.85f),
                    label = "settings_filter_bg"
                )
                val pillContent by animateColorAsState(
                    targetValue = if (isSelected) colors.onAccent else colors.secondaryText,
                    label = "settings_filter_fg"
                )
                BouncySurface(
                    onClick = {
                        selectedFilter = filter
                        scope.launch {
                            val targetX = (idx * 160 - 120).coerceAtLeast(0)
                            filterScrollState.animateScrollTo(
                                targetX,
                                spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                    },
                    shape = RoundedCornerShape(50),
                    color = pillBg,
                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                    modifier = Modifier.testTag("settings_filter_${filter.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = filter.icon,
                            contentDescription = null,
                            tint = pillContent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = pillContent
                        )
                    }
                }
            }
        }

        val transitionMs = if (reducedMotion) 0 else 180

if (isPreparingSettings) {
    SettingsBodySkeleton(
        modifier = Modifier.weight(1f)
    )
} else {
    AnimatedContent(
            targetState = selectedFilter,
            transitionSpec = {
                fadeIn(tween(transitionMs)) togetherWith fadeOut(tween((transitionMs * 0.7f).toInt()))
            },
            label = "settings_filter_content",
            modifier = Modifier.weight(1f)
        ) { activeFilter ->
            val showAll = activeFilter == SettingsFilter.ALL
            Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(bodyScrollState)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.86f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                // =========================================================================
                // THEME, BACKGROUND & ACCENT (Sleek thin expandable bars with down arrow)
                // =========================================================================
                if (showAll || activeFilter == SettingsFilter.THEME_BG) {
                    SwissSectionHeader(
                        title = "Theme & Backdrop",
                        subtitle = "Tap any bar to expand colour options"
                    )

                    ThemePaletteGrid(
                        selectedTheme = settings.themeMode,
                        onSelectTheme = viewModel::updateThemeMode
                    )

                    BackgroundPaletteSection(
                        selectedBackground = settings.backgroundPreset,
                        onSelectBackground = viewModel::setBackgroundPreset
                    )

                    AccentAccordionSection(
                        accentPreset = settings.accentPreset,
                        customAccentArgb = settings.customAccentArgb,
                        onSelectPreset = viewModel::selectAccentPreset,
                        onOpenCustomPicker = { showCustomColorDialog = true }
                    )

                    SettingSubLabel("Gallery Background Picture")
                    GalleryBackgroundCard(
                        imagePath = settings.backgroundImagePath,
                        onPickFromGallery = {
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRemoveImage = {
                            viewModel.clearBackgroundImage()
                            onShowMessage("Gallery background removed")
                        }
                    )
                }

                // =========================================================================
                // TYPOGRAPHY
                // =========================================================================
                if (showAll || activeFilter == SettingsFilter.TYPOGRAPHY) {
                    SwissSectionHeader(
                        title = "Typography",
                        subtitle = "Display, keypad & interface typefaces with live scale preview"
                    )

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = colors.cardBg,
                        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "LIVE DISPLAY PREVIEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accent
                            )
                            Text(
                                text = "12,480.75 × π = 39,209.43",
                                fontFamily = CucalcusFonts.resolve(settings.displayFont),
                                fontSize = (22f * settings.fontScale).sp,
                                color = colors.primaryText
                            )
                            Text(
                                text = "Interface sample text at ${(settings.fontScale * 100).roundToInt()}% scale",
                                fontFamily = CucalcusFonts.resolve(settings.interfaceFont),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.secondaryText
                            )
                        }
                    }

                    SettingSubLabel("Calculator Display Font")
                    ChipSelectionRow(
                        items = FontOption.entries.map { it to it.label },
                        selected = settings.displayFont,
                        onSelect = viewModel::setDisplayFont
                    )

                    SettingSubLabel("Calculator Buttons Font")
                    ChipSelectionRow(
                        items = FontOption.entries.map { it to it.label },
                        selected = settings.buttonFont,
                        onSelect = viewModel::setButtonFont
                    )

                    SettingSubLabel("General Interface Font")
                    ChipSelectionRow(
                        items = FontOption.entries.map { it to it.label },
                        selected = settings.interfaceFont,
                        onSelect = viewModel::setInterfaceFont
                    )

                    SettingSliderRow(
                        label = "Font Size Scale",
                        valueText = "${(settings.fontScale * 100).roundToInt()}%",
                        value = settings.fontScale,
                        valueRange = 0.85f..1.25f,
                        onValueChange = viewModel::setFontScale
                    )
                }

                // =========================================================================
                // BUTTON STUDIO
                // =========================================================================
                if (showAll || activeFilter == SettingsFilter.BUTTONS) {
                    SwissSectionHeader(
                        title = "Button Studio",
                        subtitle = "Key geometry, roundness, borderless mode & custom colours"
                    )

                    ButtonLivePreviewRow(settings = settings)

                    ButtonShapeVisualSelector(
                        selectedShape = settings.buttonShape,
                        cornerRadiusPercent = settings.buttonCornerRadiusPercent,
                        onSelectShape = viewModel::setButtonShape
                    )

                    SettingSliderRow(
                        label = "Corner Roundness (Rounded → Circle)",
                        valueText = "${settings.buttonCornerRadiusPercent * 2}%",
                        value = settings.buttonCornerRadiusPercent.coerceIn(8, 50).toFloat(),
                        valueRange = 8f..50f,
                        onValueChange = { viewModel.setButtonCornerRadiusPercent(it.roundToInt()) }
                    )

                    SettingSliderRow(
                        label = "Button Label Size",
                        valueText = "${(settings.buttonSizeScale * 100).roundToInt()}%",
                        value = settings.buttonSizeScale,
                        valueRange = 0.85f..1.15f,
                        onValueChange = viewModel::setButtonSizeScale
                    )
                    SettingSliderRow(
                        label = "Button Spacing",
                        valueText = "${settings.buttonSpacingDp.roundToInt()} dp",
                        value = settings.buttonSpacingDp,
                        valueRange = 2f..12f,
                        onValueChange = viewModel::setButtonSpacing
                    )
                    SettingSliderRow(
                        label = "Button Opacity",
                        valueText = "${(settings.buttonOpacity * 100).roundToInt()}%",
                        value = settings.buttonOpacity,
                        valueRange = 0.4f..1.0f,
                        onValueChange = viewModel::setButtonOpacity
                    )

                    SettingsSwitchGroupCard {
                        SettingSwitchItem(
                            title = "Borderless Buttons",
                            subtitle = "Remove outlines from calculator & memory keys",
                            checked = settings.borderlessButtons,
                            onCheckedChange = viewModel::setBorderlessButtons
                        )
                        HorizontalDivider(thickness = 1.dp, color = colors.border.copy(alpha = 0.7f))
                        SettingSwitchItem(
                            title = "Custom Button Colours",
                            subtitle = "Override default dark key surface & text",
                            checked = settings.useCustomButtonColors,
                            onCheckedChange = viewModel::setUseCustomButtonColors
                        )
                    }

                    if (settings.useCustomButtonColors) {
                        SettingSubLabel("Button Background")
                        val bgPresets = listOf(
                            0xFF171717L to "Charcoal",
                            0xFF0E0E10L to "Obsidian",
                            0xFF23252AL to "Slate",
                            0xFF1F1B16L to "Espresso",
                            0xFF19222BL to "Midnight"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            bgPresets.forEach { (argb, name) ->
                                ColorDotChip(
                                    color = Color(argb),
                                    label = name,
                                    selected = settings.buttonBgArgb == argb,
                                    onClick = { viewModel.setButtonBgColor(argb) }
                                )
                            }
                        }
                        SettingSubLabel("Button Text Colour")
                        val textPresets = listOf(
                            0xFFF5F2EAL to "Ivory",
                            0xFFFFFFFFL to "White",
                            0xFFD6B56DL to "Gold",
                            0xFFB4BAC4L to "Silver"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            textPresets.forEach { (argb, name) ->
                                ColorDotChip(
                                    color = Color(argb),
                                    label = name,
                                    selected = settings.buttonTextArgb == argb,
                                    onClick = { viewModel.setButtonTextColor(argb) }
                                )
                            }
                        }
                    }
                }

                // =========================================================================
                // APP ICONS (8 CUSTOM + GALLERY ICON)
                // =========================================================================
                if (showAll || activeFilter == SettingsFilter.ICONS) {
                    AppIconSelectorSection(
                        activeIcon = settings.selectedIcon,
                        onApplyIcon = { option ->
                            val ok = viewModel.applyAppIcon(option)
                            onShowMessage(
                                if (ok) "Launcher icon switched to ${option.displayName}"
                                else "Saved ${option.displayName} preference"
                            )
                        },
                        onResetToDefault = {
                            viewModel.resetAppIconToDefault()
                            onShowMessage("Launcher icon reset to default")
                        },
                        onShowMessage = onShowMessage
                    )
                }

                // =========================================================================
                // CALCULATOR & TACTILE FEEDBACK
                // =========================================================================
                if (showAll || activeFilter == SettingsFilter.CALCULATOR) {
                    SwissSectionHeader(
                        title = "Calculator & Haptics",
                        subtitle = "Keypad layout, precision, number formatting & vibration"
                    )

                    SwissSectionHeader(title = "Display Box Animations", subtitle = "Ambient effects inside the calculator display")
                    SettingsSwitchGroupCard {
                        SettingSwitchItem(title = "Enable display animations", subtitle = "Turn on the selected display effect", checked = settings.displayBoxAnimationsEnabled, onCheckedChange = viewModel::setDisplayBoxAnimationsEnabled)
                        HorizontalDivider(thickness = 1.dp, color = colors.border.copy(alpha = 0.7f))
                        SettingSwitchItem(title = "Pause animation", subtitle = "Keep the effect visible without movement", checked = settings.displayBoxAnimationPaused, enabled = settings.displayBoxAnimationsEnabled, onCheckedChange = viewModel::setDisplayBoxAnimationPaused)
                    }
                    Text("Choose an effect · tap a preview to select", style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
                    DisplayBoxAnimation.entries.forEach { animation ->
                        AnimationPreviewChoice(animation.label, settings.displayBoxAnimation == animation, colors.accent, { viewModel.setDisplayBoxAnimation(animation) }, animationPreviewPhase)
                    }

                    Spacer(Modifier.height(14.dp))
                    SwissSectionHeader(title = "Button Animations", subtitle = "Subtle motion effects for calculator keys")
                    SettingsSwitchGroupCard {
                        SettingSwitchItem(title = "Enable button animations", subtitle = "Apply the selected effect to calculator keys", checked = settings.buttonAnimationsEnabled, onCheckedChange = viewModel::setButtonAnimationsEnabled)
                        HorizontalDivider(thickness = 1.dp, color = colors.border.copy(alpha = 0.7f))
                        SettingSwitchItem(title = "Pause animation", subtitle = "Keep the selected button effect visible without movement", checked = settings.buttonAnimationPaused, enabled = settings.buttonAnimationsEnabled, onCheckedChange = viewModel::setButtonAnimationPaused)
                    }
                    Text("Choose a button effect · tap a preview to select", style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
                    ButtonAnimation.entries.forEach { animation ->
                        ButtonAnimationPreviewChoice(animation.label, settings.buttonAnimation == animation, colors.accent, { viewModel.setButtonAnimation(animation) }, animationPreviewPhase)
                    }

                    SettingsSwitchGroupCard {
                        SettingSwitchItem(
                            title = "Double-Zero (00) Button",
                            subtitle = "Include 00 key in bottom keypad row",
                            checked = settings.showDoubleZero,
                            onCheckedChange = viewModel::setShowDoubleZero
                        )
                        HorizontalDivider(thickness = 1.dp, color = colors.border.copy(alpha = 0.7f))
                        SettingSwitchItem(
                            title = "Triple-Zero (000) Button",
                            subtitle = "Include 000 key in bottom keypad row",
                            checked = settings.showTripleZero,
                            onCheckedChange = viewModel::setShowTripleZero
                        )
                        HorizontalDivider(thickness = 1.dp, color = colors.border.copy(alpha = 0.7f))
                        SettingSwitchItem(
                            title = "Decimal (.) Button",
                            subtitle = "Show decimal separator key",
                            checked = settings.showDecimal,
                            onCheckedChange = viewModel::setShowDecimal
                        )
                        HorizontalDivider(thickness = 1.dp, color = colors.border.copy(alpha = 0.7f))
                        SettingSwitchItem(
                            title = "Percentage (%) Button",
                            subtitle = "Show percentage key on keypad",
                            checked = settings.showPercent,
                            onCheckedChange = viewModel::setShowPercent
                        )
                    }

                    SettingsSwitchGroupCard {
                        SettingSwitchItem(
                            title = "Scientific Mode Toggle",
                            subtitle = "Show SCI drawer button on calculator bar",
                            checked = settings.showScientificToggle,
                            onCheckedChange = viewModel::setShowScientificToggle
                        )
                        HorizontalDivider(thickness = 1.dp, color = colors.border.copy(alpha = 0.7f))
                        SettingSwitchItem(
                            title = "Expand Scientific on Launch",
                            subtitle = "Open scientific panel by default",
                            checked = settings.defaultScientificExpanded,
                            onCheckedChange = viewModel::setDefaultScientificExpanded
                        )
                        HorizontalDivider(thickness = 1.dp, color = colors.border.copy(alpha = 0.7f))
                        SettingSwitchItem(
                            title = "Clipboard Math Detection",
                            subtitle = "Offer to evaluate copied expressions",
                            checked = settings.clipboardDetectionEnabled,
                            onCheckedChange = viewModel::setClipboardDetection
                        )
                    }

                    SettingSubLabel("Keypad Arrangement")
                    ChipSelectionRow(
                        items = KeypadArrangement.entries.map { it to it.label },
                        selected = settings.keypadArrangement,
                        onSelect = viewModel::setKeypadArrangement
                    )

                    SettingSubLabel("Default Angle Unit")
                    ChipSelectionRow(
                        items = AngleMode.entries.map { it to if (it == AngleMode.DEG) "Degrees (DEG)" else "Radians (RAD)" },
                        selected = settings.angleMode,
                        onSelect = viewModel::setDefaultAngleMode
                    )

                    SettingSliderRow(
                        label = "Decimal Precision",
                        valueText = "${settings.decimalPrecision} digits",
                        value = settings.decimalPrecision.toFloat(),
                        valueRange = 0f..12f,
                        steps = 11,
                        onValueChange = { viewModel.setDecimalPrecision(it.roundToInt()) }
                    )

                    SettingSubLabel("Number Formatting")
                    ChipSelectionRow(
                        items = NumberFormatMode.entries.map { it to it.label },
                        selected = settings.numberFormatMode,
                        onSelect = viewModel::setNumberFormatMode
                    )

                    SettingsSwitchGroupCard {
                        SettingSwitchItem(
                            title = "Haptic Feedback",
                            subtitle = if (viewModel.isHapticSupported) "Tactile vibration on key press" else "Vibrator hardware not detected on device",
                            checked = settings.hapticEnabled,
                            onCheckedChange = viewModel::setHapticEnabled
                        )
                    }
                    if (settings.hapticEnabled) {
                        SettingSubLabel("Haptic Strength")
                        ChipSelectionRow(
                            items = HapticStrength.entries.map { it to it.label },
                            selected = settings.hapticStrength,
                            onSelect = viewModel::setHapticStrength
                        )
                    }
                }

                // =========================================================================
                // DATA, BACKUP & ABOUT
                // =========================================================================
                if (showAll || activeFilter == SettingsFilter.BACKUP) {
                    SwissSectionHeader(
                        title = "Data & Backup",
                        subtitle = "History export, personalisation backup & reset"
                    )

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = colors.cardBg,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val csv = viewModel.exportHistoryCsv()
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/csv"
                                                putExtra(Intent.EXTRA_TEXT, csv)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Export History CSV"))
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.weight(1f)
                                ) { Text("EXPORT CSV", color = colors.primaryText) }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val txt = viewModel.exportHistoryTxt()
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, txt)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Export History TXT"))
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.weight(1f)
                                ) { Text("EXPORT TXT", color = colors.primaryText) }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val json = viewModel.exportSettingsJson()
                                        clipboard.setText(AnnotatedString(json))
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Cucalcus Settings Backup")
                                            putExtra(Intent.EXTRA_TEXT, json)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Backup Settings JSON"))
                                        onShowMessage("Settings JSON copied & ready to share")
                                    },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.weight(1f)
                                ) { Text("BACKUP JSON", color = colors.accent) }

                                OutlinedButton(
                                    onClick = { showImportDialog = true },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.weight(1f)
                                ) { Text("IMPORT JSON", color = colors.primaryText) }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showClearHistoryConfirm = true },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, colors.error.copy(alpha = 0.6f)),
                                    modifier = Modifier.weight(1f)
                                ) { Text("CLEAR HISTORY", color = colors.error) }

                                Button(
                                    onClick = { showResetConfirm = true },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.error.copy(alpha = 0.18f),
                                        contentColor = colors.error
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reset_all_settings_button")
                                ) { Text("RESET ALL", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }

                    SwissSectionHeader(
                        title = "About Cucalcus",
                        subtitle = "Application & privacy details"
                    )
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = colors.cardBg,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AboutRow("Application", "Cucalcus")
                            HorizontalDivider(thickness = 1.dp, color = colors.border)
                            AboutRow("Version", "1.0.0 (Swiss Typographic Edition)")
                            HorizontalDivider(thickness = 1.dp, color = colors.border)
                            AboutRow("Engineering", "Safe Recursive-Descent Parser · Room + DataStore")
                            HorizontalDivider(thickness = 1.dp, color = colors.border)
                            Text(
                                text = "Privacy: All calculations, history logs, tools, and personalisation preferences remain strictly on your device. Internet access is used solely to fetch public currency exchange rates when requested.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.secondaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    }
    if (showCustomColorDialog) {
        CustomColorPickerDialog(
            initialArgb = settings.customAccentArgb,
            onDismiss = { showCustomColorDialog = false },
            onConfirm = { newArgb ->
                viewModel.setCustomAccentArgb(newArgb)
                showCustomColorDialog = false
                onShowMessage("Custom accent colour applied")
            }
        )
    }

    if (showImportDialog) {
        var jsonText by remember { mutableStateOf("") }
        BouncyDialog(
            onDismissRequest = { showImportDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = colors.cardBg,
            title = { Text("Import Settings Backup", color = colors.primaryText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste your exported Cucalcus JSON configuration below:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryText
                    )
                    OutlinedTextField(
                        value = jsonText,
                        onValueChange = { jsonText = it },
                        shape = RoundedCornerShape(16.dp),
                        minLines = 4,
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.primaryText,
                            unfocusedTextColor = colors.primaryText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importSettingsJson(jsonText).fold(
                            onSuccess = {
                                showImportDialog = false
                                onShowMessage("Settings imported successfully")
                            },
                            onFailure = {
                                onShowMessage("Invalid JSON backup format")
                            }
                        )
                    }
                ) { Text("IMPORT", color = colors.accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("CANCEL", color = colors.secondaryText)
                }
            }
        )
    }

    if (showClearHistoryConfirm) {
        BouncyDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = colors.cardBg,
            title = { Text("Clear Calculation History?", color = colors.primaryText) },
            text = { Text("All saved calculations will be permanently removed.", color = colors.secondaryText) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllHistory()
                    showClearHistoryConfirm = false
                    onShowMessage("History cleared")
                }) { Text("CLEAR", color = colors.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text("CANCEL", color = colors.secondaryText)
                }
            }
        )
    }

    if (showResetConfirm) {
        BouncyDialog(
            onDismissRequest = { showResetConfirm = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = colors.cardBg,
            title = { Text("Reset All Settings?", color = colors.primaryText) },
            text = {
                Text(
                    "Restores Obsidian Black theme, Champagne Gold accent, default typography, button layout, haptics, and Green Edition icon, and removes custom background & gallery icon pictures.",
                    color = colors.secondaryText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllSettings()
                        showResetConfirm = false
                        onShowMessage("All settings reset to defaults")
                    },
                    modifier = Modifier.testTag("confirm_reset_settings")
                ) { Text("RESET", color = colors.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("CANCEL", color = colors.secondaryText)
                }
            }
        )
    }
}

@Composable
private fun SettingsSwitchGroupCard(
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CucalcusTheme.colors
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.cardBg,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = CucalcusTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = colors.primaryText)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.secondaryText,
                uncheckedTrackColor = colors.elevatedSurface
            )
        )
    }
}

@Composable
private fun ButtonLivePreviewRow(settings: CucalcusSettings) {
    val colors = CucalcusTheme.colors
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.secondaryBg,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(settings.buttonSpacingDp.dp)
        ) {
            CalculatorKeyButton(
                label = "7",
                keyKind = KeypadKeyKind.DIGIT,
                onClick = {},
                settingsOverride = settings,
                modifier = Modifier.weight(1f)
            )
            CalculatorKeyButton(
                label = "×",
                keyKind = KeypadKeyKind.OPERATOR,
                onClick = {},
                settingsOverride = settings,
                modifier = Modifier.weight(1f)
            )
            CalculatorKeyButton(
                label = "AC",
                keyKind = KeypadKeyKind.CLEAR,
                onClick = {},
                settingsOverride = settings,
                modifier = Modifier.weight(1f)
            )
            CalculatorKeyButton(
                label = "=",
                keyKind = KeypadKeyKind.EQUALS,
                onClick = {},
                settingsOverride = settings,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ButtonShapeVisualSelector(
    selectedShape: ButtonShapeOption,
    cornerRadiusPercent: Int,
    onSelectShape: (ButtonShapeOption) -> Unit
) {
    val colors = CucalcusTheme.colors
    val visibleOptions = ButtonShapeOption.entries.filter { it != ButtonShapeOption.CUSTOM_RADIUS }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        visibleOptions.chunked(4).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowOptions.forEach { option ->
                    val isSelected = selectedShape == option
                    val previewShape = option.toComposeShape(cornerRadiusPercent)
                    BouncySurface(
                        onClick = { onSelectShape(option) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) colors.accent.copy(alpha = 0.16f) else colors.cardBg,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) colors.accent else colors.border
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shape_option_${option.name}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(previewShape)
                                    .background(
                                        if (isSelected) colors.accent.copy(alpha = 0.24f) else colors.elevatedSurface
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isSelected) colors.accent else colors.secondaryText,
                                        shape = previewShape
                                    )
                            )
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp
                                ),
                                color = if (isSelected) colors.accent else colors.primaryText,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomColorPickerDialog(
    initialArgb: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val colors = CucalcusTheme.colors
    val init = Color(initialArgb)
    var r by remember { mutableFloatStateOf(init.red) }
    var g by remember { mutableFloatStateOf(init.green) }
    var b by remember { mutableFloatStateOf(init.blue) }
    val previewColor = Color(r, g, b, 1f)
    val hexCode = String.format("#%06X", 0xFFFFFF and previewColor.toArgb())

    BouncyDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = colors.cardBg,
        title = { Text("Custom Accent Colour", color = colors.primaryText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(previewColor)
                            .border(1.dp, colors.border, CircleShape)
                    )
                    Text(hexCode, style = MaterialTheme.typography.titleLarge, color = colors.primaryText)
                }
                Text("Red (${(r * 255).roundToInt()})", style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
                Slider(value = r, onValueChange = { r = it }, colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red))
                Text("Green (${(g * 255).roundToInt()})", style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
                Slider(value = g, onValueChange = { g = it }, colors = SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green))
                Text("Blue (${(b * 255).roundToInt()})", style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
                Slider(value = b, onValueChange = { b = it }, colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(previewColor.toArgb().toLong() and 0xFFFFFFFFL) }) {
                Text("APPLY", color = colors.accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = colors.secondaryText)
            }
        }
    )
}


@Composable
private fun AnimationPreviewChoice(title: String, selected: Boolean, accent: Color, onClick: () -> Unit, phase: Float) {
    val colors = CucalcusTheme.colors
    BouncySurface(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp), color = if (selected) accent.copy(alpha = 0.10f) else colors.cardBg, border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else colors.border)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(width = 116.dp, height = 54.dp).clip(RoundedCornerShape(10.dp)).background(colors.secondaryBg)) {
                Canvas(Modifier.fillMaxSize()) {
                    val x = size.width * phase
                    when (title) {
                        "Flowing Wave", "Aurora Flow" -> for (i in 0..2) drawLine(accent.copy(alpha = 0.28f - i * 0.05f), Offset(0f, size.height * (0.35f + i * 0.18f)), Offset(size.width, size.height * (0.35f + i * 0.18f) + kotlin.math.sin(phase * 6.28f + i) * 7f), strokeWidth = 3.dp.toPx())
                        "Shimmer Sweep" -> drawRect(Brush.linearGradient(listOf(Color.Transparent, accent.copy(alpha = 0.35f), Color.Transparent), Offset(x * 1.4f - 30f, 0f), Offset(x * 1.4f + 30f, size.height)))
                        "Floating Orbs" -> for (i in 0..3) drawCircle(accent.copy(alpha = 0.25f), 5.dp.toPx(), Offset(size.width * (0.15f + i * 0.22f), size.height * (0.5f + kotlin.math.sin(phase * 6.28f + i) * 0.2f)))
                        "Moving Grid" -> for (i in 0..5) drawLine(accent.copy(alpha = 0.2f), Offset(i * 24f + x * 18f % 24f, 0f), Offset(i * 24f + x * 18f % 24f, size.height), 1.dp.toPx())
                        "Liquid Glow" -> drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent), Offset(x, size.height / 2), size.height), size.height, Offset(x, size.height / 2))
                        "Radial Rings" -> for (i in 0..3) {
                            val p = (phase + i * 0.25f) % 1f
                            drawCircle(accent.copy(alpha = 0.25f * (1f - p)), size.minDimension * p * 0.72f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx()))
                        }
                        "Nebula Drift" -> for (i in 0..5) {
                            val a = phase * 6.28f + i * 1.047f
                            val point = Offset(size.width * (0.5f + 0.32f * kotlin.math.cos(a)), size.height * (0.5f + 0.3f * kotlin.math.sin(a * 0.72f + i)))
                            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent), point, 12.dp.toPx()), 12.dp.toPx(), point)
                        }
                        "Scanline Flow" -> for (i in 0..7) {
                            val y = (i * 9f + phase * 18f) % size.height
                            drawLine(accent.copy(alpha = 0.28f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                        }
                        else -> for (i in 0..18) drawCircle(accent.copy(alpha = 0.25f), 1.5.dp.toPx(), Offset((i * 23f + x * 20f) % size.width, (i * 13f) % size.height))
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
                Text(if (selected) "Selected" else "Tap to select", style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
            }
            if (selected) Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = accent)
        }
    }
}

@Composable
private fun ButtonAnimationPreviewChoice(title: String, selected: Boolean, accent: Color, onClick: () -> Unit, phase: Float) {
    val colors = CucalcusTheme.colors
    BouncySurface(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp), color = if (selected) accent.copy(alpha = 0.10f) else colors.cardBg, border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else colors.border)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(width = 116.dp, height = 54.dp).clip(RoundedCornerShape(10.dp)).background(colors.secondaryBg), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 64.dp, height = 34.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.16f))) {
                    Canvas(Modifier.fillMaxSize()) {
                        val p = phase
                        when (title) {
                            "Soft Pulse" -> drawCircle(accent.copy(alpha = 0.22f), size.minDimension * (0.24f + 0.08f * kotlin.math.sin(p * 6.28f)), center)
                            "Shimmer" -> drawRect(Brush.linearGradient(listOf(Color.Transparent, accent.copy(alpha = 0.55f), Color.Transparent), Offset(size.width * (p - 0.3f), 0f), Offset(size.width * (p + 0.3f), size.height)))
                            "Ripple" -> for (i in 0..2) drawCircle(accent.copy(alpha = 0.3f), size.minDimension * ((p + i * 0.33f) % 1f) * 0.5f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                            "Floating Glow", "Aurora Trail" -> drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.4f), Color.Transparent), Offset(size.width * p, size.height / 2), size.height), size.height, Offset(size.width * p, size.height / 2))
                            "Color Breath" -> drawCircle(accent.copy(alpha = 0.16f + 0.16f * kotlin.math.sin(p * 6.28f)), size.minDimension * (0.22f + 0.08f * kotlin.math.sin(p * 6.28f)), center)
                            "Inner Glow" -> {
                                val point = Offset(size.width * (0.18f + 0.64f * p), size.height * (0.5f + 0.12f * kotlin.math.sin(p * 6.28f)))
                                val radius = size.minDimension * 0.62f
                                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.42f), accent.copy(alpha = 0.12f), Color.Transparent), point, radius), radius, point)
                            }
                            "Spark Trail" -> for (i in 0..5) {
                                val t = (p + i * 0.14f) % 1f
                                drawCircle(accent.copy(alpha = 0.45f * (1f - t)), 2.dp.toPx(), Offset(size.width * t, size.height * (0.5f + 0.25f * kotlin.math.sin(t * 6.28f + i))))
                            }
                            else -> drawCircle(accent.copy(alpha = 0.35f), size.minDimension * 0.2f, Offset(size.width * (0.5f + 0.2f * kotlin.math.sin(p * 6.28f)), size.height / 2))
                        }
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
                Text(if (selected) "Selected" else "Tap to select", style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
            }
            if (selected) Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = accent)
        }
    }
}

@Composable
private fun SettingSubLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = CucalcusTheme.colors.primaryText
    )
}

@Composable
private fun <T> ChipSelectionRow(
    items: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    val colors = CucalcusTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (value, label) ->
            val isSel = value == selected
            BouncySurface(
                onClick = { onSelect(value) },
                shape = RoundedCornerShape(50),
                color = if (isSel) colors.accent.copy(alpha = 0.16f) else colors.cardBg,
                border = BorderStroke(1.dp, if (isSel) colors.accent else colors.border)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSel) colors.accent else colors.secondaryText,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    val colors = CucalcusTheme.colors
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.cardBg,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.primaryText)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.accent.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.elevatedSurface
                )
            )
        }
    }
}

@Composable
private fun ColorDotChip(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = CucalcusTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) colors.accent.copy(alpha = 0.16f) else colors.cardBg,
        border = BorderStroke(1.dp, if (selected) colors.accent else colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, colors.border, CircleShape)
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.primaryText)
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    val colors = CucalcusTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.primaryText, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ThinExpandableColorAccordion(
    title: String,
    activeLabel: String,
    activeSwatchColor: Color,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    headerTestTag: String,
    expandedContent: @Composable ColumnScope.() -> Unit
) {
    val colors = CucalcusTheme.colors
    val reducedMotion = CucalcusTheme.reducedMotion
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "accordion_arrow"
    )
    val headerInteraction = remember { MutableInteractionSource() }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardBg,
        border = BorderStroke(1.dp, if (expanded) colors.accent.copy(alpha = 0.55f) else colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = if (reducedMotion) tween(0) else spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyPress(headerInteraction, pressedScale = 0.98f)
                    .clickable(interactionSource = headerInteraction, indication = null) {
                        onToggleExpand()
                    }
                    .padding(horizontal = 14.dp, vertical = 11.dp)
                    .testTag(headerTestTag)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(activeSwatchColor)
                        .border(1.5.dp, colors.border, CircleShape)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primaryText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.accent.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = activeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = colors.secondaryText,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = arrowRotation }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = if (reducedMotion) fadeIn(tween(0)) else fadeIn(tween(140)) + expandVertically(
                    spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = if (reducedMotion) fadeOut(tween(0)) else fadeOut(tween(100)) + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(thickness = 0.8.dp, color = colors.border.copy(alpha = 0.7f))
                    expandedContent()
                }
            }
        }
    }
}

@Composable
private fun CompactColorPill(
    label: String,
    swatchColor: Color,
    isSelected: Boolean,
    testTagName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val colors = CucalcusTheme.colors
    BouncySurface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (isSelected) colors.accent.copy(alpha = 0.16f) else colors.secondaryBg.copy(alpha = 0.85f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) colors.accent else colors.border
        ),
        pressedScale = 0.94f,
        modifier = modifier.testTag(testTagName)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(swatchColor)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) colors.primaryText else colors.border,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = contrastOnColor(swatchColor),
                        modifier = Modifier.size(10.dp)
                    )
                } else if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = label,
                        tint = contrastOnColor(swatchColor),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) colors.accent else colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ThemePaletteGrid(
    selectedTheme: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ThinExpandableColorAccordion(
        title = "App Theme",
        activeLabel = selectedTheme.label,
        activeSwatchColor = Color(selectedTheme.swatchArgb),
        expanded = expanded,
        onToggleExpand = { expanded = !expanded },
        headerTestTag = "theme_accordion_toggle"
    ) {
        AppThemeMode.entries.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { mode ->
                    CompactColorPill(
                        label = mode.label.split(" ").first(),
                        swatchColor = Color(mode.swatchArgb),
                        isSelected = selectedTheme == mode,
                        testTagName = "theme_${mode.name}",
                        onClick = { onSelectTheme(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BackgroundPaletteSection(
    selectedBackground: AppBackgroundPreset,
    onSelectBackground: (AppBackgroundPreset) -> Unit
) {
    val colors = CucalcusTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val previewColor = if (selectedBackground == AppBackgroundPreset.THEME_DEFAULT) {
        colors.mainBg
    } else {
        Color(selectedBackground.argbHex)
    }

    ThinExpandableColorAccordion(
        title = "App Background",
        activeLabel = selectedBackground.label,
        activeSwatchColor = previewColor,
        expanded = expanded,
        onToggleExpand = { expanded = !expanded },
        headerTestTag = "bg_accordion_toggle"
    ) {
        val isMatchTheme = selectedBackground == AppBackgroundPreset.THEME_DEFAULT
        BouncySurface(
            onClick = { onSelectBackground(AppBackgroundPreset.THEME_DEFAULT) },
            shape = RoundedCornerShape(50),
            color = if (isMatchTheme) colors.accent.copy(alpha = 0.16f) else colors.secondaryBg,
            border = BorderStroke(1.dp, if (isMatchTheme) colors.accent else colors.border),
            modifier = Modifier.testTag("bg_THEME_DEFAULT")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isMatchTheme) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "Match Active Theme",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isMatchTheme) colors.accent else colors.primaryText
                )
            }
        }

        AppBackgroundPreset.colorChoices.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { preset ->
                    CompactColorPill(
                        label = preset.label.split(" ").last(),
                        swatchColor = Color(preset.argbHex),
                        isSelected = selectedBackground == preset,
                        testTagName = "bg_${preset.name}",
                        onClick = { onSelectBackground(preset) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AccentAccordionSection(
    accentPreset: AccentPreset,
    customAccentArgb: Long,
    onSelectPreset: (AccentPreset) -> Unit,
    onOpenCustomPicker: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val activeColor = if (accentPreset == AccentPreset.CUSTOM) {
        Color(customAccentArgb)
    } else {
        Color(accentPreset.argbHex)
    }

    ThinExpandableColorAccordion(
        title = "Accent Colour",
        activeLabel = accentPreset.label,
        activeSwatchColor = activeColor,
        expanded = expanded,
        onToggleExpand = { expanded = !expanded },
        headerTestTag = "accent_accordion_toggle"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccentPreset.entries.filter { it != AccentPreset.CUSTOM }.forEach { preset ->
                CompactColorPill(
                    label = preset.label.split(" ").first(),
                    swatchColor = Color(preset.argbHex),
                    isSelected = accentPreset == preset,
                    testTagName = "accent_${preset.name}",
                    onClick = { onSelectPreset(preset) }
                )
            }
            CompactColorPill(
                label = "Custom",
                swatchColor = Color(customAccentArgb),
                isSelected = accentPreset == AccentPreset.CUSTOM,
                testTagName = "accent_CUSTOM",
                onClick = onOpenCustomPicker,
                leadingIcon = Icons.Outlined.Palette
            )
        }
    }
}

@Composable
private fun GalleryBackgroundCard(
    imagePath: String?,
    onPickFromGallery: () -> Unit,
    onRemoveImage: () -> Unit
) {
    val colors = CucalcusTheme.colors
    val hasImage = remember(imagePath) { !imagePath.isNullOrBlank() && File(imagePath).exists() }
    var thumbBitmap by remember(imagePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imagePath, hasImage) {
        thumbBitmap = if (hasImage && imagePath != null) {
            withContext(Dispatchers.IO) {
                try {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeFile(imagePath, opts)
                } catch (_: Exception) {
                    null
                }
            }
        } else {
            null
        }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.cardBg,
        border = BorderStroke(1.dp, if (hasImage) colors.accent.copy(alpha = 0.6f) else colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val activeThumb = thumbBitmap
                if (hasImage && activeThumb != null) {
                    Image(
                        bitmap = activeThumb.asImageBitmap(),
                        contentDescription = "Selected gallery background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, colors.accent, RoundedCornerShape(14.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.elevatedSurface)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasImage) "Custom Picture Active" else "No Picture Selected",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primaryText
                    )
                    Text(
                        text = "Applies across the entire app with a light blur and subtle dim for clear legibility.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryText
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPickFromGallery,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_gallery_bg_button")
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (hasImage) "CHANGE PICTURE" else "CHOOSE FROM GALLERY", fontWeight = FontWeight.Bold)
                }
                if (hasImage) {
                    OutlinedButton(
                        onClick = onRemoveImage,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, colors.error.copy(alpha = 0.6f)),
                        modifier = Modifier.testTag("remove_gallery_bg_button")
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove picture", tint = colors.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("REMOVE", color = colors.error)
                    }
                }
            }
        }
    }
}
