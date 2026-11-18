package com.example.ui.settings

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.data.preferences.FontOption
import com.example.data.preferences.HapticStrength
import com.example.data.preferences.KeypadArrangement
import com.example.domain.math.AngleMode
import com.example.domain.math.NumberFormatMode
import com.example.ui.components.CalculatorKeyButton
import com.example.ui.components.KeypadKeyKind
import com.example.ui.components.SwissScreenTopBar
import com.example.ui.components.SwissSectionHeader
import com.example.ui.components.toComposeShape
import com.example.ui.theme.CucalcusFonts
import com.example.ui.theme.CucalcusTheme
import com.example.ui.theme.contrastOnColor
import kotlinx.coroutines.Dispatchers
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
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf(SettingsFilter.ALL) }
    var showCustomColorDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

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
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) colors.accent else colors.cardBg.copy(alpha = 0.85f),
                    label = "settings_filter_bg"
                )
                val pillContent by animateColorAsState(
                    targetValue = if (isSelected) colors.onAccent else colors.secondaryText,
                    label = "settings_filter_fg"
                )
                Surface(
                    onClick = { selectedFilter = filter },
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

        AnimatedContent(
            targetState = selectedFilter,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "settings_filter_content",
            modifier = Modifier.weight(1f)
        ) { activeFilter ->
            val showAll = activeFilter == SettingsFilter.ALL
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // =========================================================================
                // THEME, BACKGROUND & ACCENT
                // =========================================================================
                if (showAll || activeFilter == SettingsFilter.THEME_BG) {
                    SwissSectionHeader(
                        title = "Theme & Backdrop",
                        subtitle = "20 themes, 20 whole-app colours, gallery backdrop & accents"
                    )

                    SettingSubLabel("App Theme (20 Colours)")
                    ThemePaletteGrid(
                        selectedTheme = settings.themeMode,
                        onSelectTheme = viewModel::updateThemeMode
                    )

                    SettingSubLabel("Whole-App Background (20 Colours)")
                    BackgroundPaletteSection(
                        selectedBackground = settings.backgroundPreset,
                        onSelectBackground = viewModel::setBackgroundPreset
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

                    SettingSubLabel("Accent Colour")
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = colors.cardBg,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AccentPreset.entries.filter { it != AccentPreset.CUSTOM }.forEach { preset ->
                                val isSelected = settings.accentPreset == preset
                                val swatchColor = Color(preset.argbHex)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { viewModel.selectAccentPreset(preset) }
                                        .padding(6.dp)
                                        .testTag("accent_${preset.name}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(swatchColor)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) colors.primaryText else colors.border,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = preset.label,
                                                tint = Color(0xFF0A0A0A),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = preset.label.split(" ").first(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) colors.accent else colors.secondaryText
                                    )
                                }
                            }
                            val isCustom = settings.accentPreset == AccentPreset.CUSTOM
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showCustomColorDialog = true }
                                    .padding(6.dp)
                                    .testTag("accent_CUSTOM")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(settings.customAccentArgb))
                                        .border(
                                            width = if (isCustom) 2.5.dp else 1.dp,
                                            color = if (isCustom) colors.primaryText else colors.accent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Palette,
                                        contentDescription = "Custom color picker",
                                        tint = Color(0xFF0A0A0A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = "Custom",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCustom) colors.accent else colors.secondaryText
                                )
                            }
                        }
                    }
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
        AlertDialog(
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
        AlertDialog(
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
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = colors.cardBg,
            title = { Text("Reset All Settings?", color = colors.primaryText) },
            text = {
                Text(
                    "Restores Obsidian Black theme, Champagne Gold accent, default typography, button layout, haptics, and Quadrant icon, and removes custom background & gallery icon pictures.",
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
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = CucalcusTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
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
                    Surface(
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

    AlertDialog(
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
            Surface(
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
private fun ThemePaletteGrid(
    selectedTheme: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit
) {
    val colors = CucalcusTheme.colors
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.cardBg,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE THEME",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryText
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.accent.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = selectedTheme.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            AppThemeMode.entries.chunked(5).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowItems.forEach { mode ->
                        val isSelected = selectedTheme == mode
                        val swatchColor = Color(mode.swatchArgb)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) colors.accent.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { onSelectTheme(mode) }
                                .padding(vertical = 6.dp, horizontal = 2.dp)
                                .testTag("theme_${mode.name}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) colors.accent else colors.border,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = mode.label,
                                        tint = contrastOnColor(swatchColor),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mode.label.split(" ").first(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isSelected) colors.accent else colors.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val isMatchTheme = selectedBackground == AppBackgroundPreset.THEME_DEFAULT
        Surface(
            onClick = { onSelectBackground(AppBackgroundPreset.THEME_DEFAULT) },
            shape = RoundedCornerShape(50),
            color = if (isMatchTheme) colors.accent.copy(alpha = 0.16f) else colors.cardBg,
            border = BorderStroke(1.dp, if (isMatchTheme) colors.accent else colors.border),
            modifier = Modifier.testTag("bg_THEME_DEFAULT")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isMatchTheme) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = "Match Active Theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isMatchTheme) colors.accent else colors.primaryText
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.cardBg,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BACKGROUND COLOUR",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryText
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.accent.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = selectedBackground.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                AppBackgroundPreset.colorChoices.chunked(5).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowItems.forEach { preset ->
                            val isSelected = selectedBackground == preset
                            val swatchColor = Color(preset.argbHex)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) colors.accent.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable { onSelectBackground(preset) }
                                    .padding(vertical = 6.dp, horizontal = 2.dp)
                                    .testTag("bg_${preset.name}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) colors.accent else colors.border,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = preset.label,
                                            tint = contrastOnColor(swatchColor),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = preset.label.split(" ").last(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isSelected) colors.accent else colors.secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
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
