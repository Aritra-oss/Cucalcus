package com.example.ui.settings

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Palette
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
import androidx.compose.ui.graphics.toArgb
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
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.ButtonShapeOption
import com.example.data.preferences.ClickSoundStyle
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

    var showCustomColorDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.mainBg)
    ) {
        SwissScreenTopBar(
            title = "Settings & Studio",
            tag = "Personalisation"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // =========================================================================
            // GROUP 1: APPEARANCE
            // =========================================================================
            SwissSectionHeader(
                title = "Appearance",
                subtitle = "Accent colour, theme, typography, buttons & app icon"
            )

            // Theme mode
            SettingSubLabel("Surface Theme")
            ChipSelectionRow(
                items = AppThemeMode.entries.map { it to it.label },
                selected = settings.themeMode,
                onSelect = viewModel::updateThemeMode
            )

            // Accent presets + custom picker
            SettingSubLabel("Accent Colour")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccentPreset.entries.filter { it != AccentPreset.CUSTOM }.forEach { preset ->
                    val isSelected = settings.accentPreset == preset
                    val swatchColor = Color(preset.argbHex)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { viewModel.selectAccentPreset(preset) }
                            .padding(4.dp)
                            .testTag("accent_${preset.name}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preset.label.split(" ").first(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) colors.accent else colors.secondaryText
                        )
                    }
                }
                // Custom Color Picker Circle
                val isCustom = settings.accentPreset == AccentPreset.CUSTOM
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { showCustomColorDialog = true }
                        .padding(4.dp)
                        .testTag("accent_CUSTOM")
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Custom",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCustom) colors.accent else colors.secondaryText
                    )
                }
            }

            // Font Customisation + Live Preview
            SettingSubLabel("Typography & Live Preview")
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = colors.cardBg,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "DISPLAY PREVIEW",
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

            // Button Shape & Appearance + Real-time Preview
            SettingSubLabel("Button Shape & Real-Time Preview")
            ButtonLivePreviewRow(settings = settings)

            ButtonShapeVisualSelector(
                selectedShape = settings.buttonShape,
                cornerRadiusPercent = settings.buttonCornerRadiusPercent,
                onSelectShape = viewModel::setButtonShape
            )

            SettingSliderRow(
                label = "Corner Roundness (Square → Circle)",
                valueText = "${settings.buttonCornerRadiusPercent * 2}%",
                value = settings.buttonCornerRadiusPercent.toFloat(),
                valueRange = 0f..50f,
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

            SettingSwitchRow(
                title = "Custom Button Colours",
                subtitle = "Override default dark key surface & text",
                checked = settings.useCustomButtonColors,
                onCheckedChange = viewModel::setUseCustomButtonColors
            )

            if (settings.useCustomButtonColors) {
                SettingSubLabel("Button Background")
                val bgPresets = listOf(
                    0xFF171717L to "Charcoal",
                    0xFF0E0E10L to "Obsidian",
                    0xFF23252AL to "Slate",
                    0xFF1F1B16L to "Espresso",
                    0xFF19222BL to "Midnight"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            // 8 App Icons Section
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
                    onShowMessage("Launcher icon reset to Quadrant")
                }
            )

            // =========================================================================
            // GROUP 2: CALCULATOR
            // =========================================================================
            SwissSectionHeader(
                title = "Calculator",
                subtitle = "Keypad configuration, precision & number formatting"
            )

            SettingSwitchRow(
                title = "Double-Zero (00) Button",
                subtitle = "Include 00 key in bottom keypad row",
                checked = settings.showDoubleZero,
                onCheckedChange = viewModel::setShowDoubleZero
            )
            SettingSwitchRow(
                title = "Triple-Zero (000) Button",
                subtitle = "Include 000 key in bottom keypad row",
                checked = settings.showTripleZero,
                onCheckedChange = viewModel::setShowTripleZero
            )
            SettingSwitchRow(
                title = "Decimal (.) Button",
                subtitle = "Show decimal separator key",
                checked = settings.showDecimal,
                onCheckedChange = viewModel::setShowDecimal
            )
            SettingSwitchRow(
                title = "Percentage (%) Button",
                subtitle = "Show percentage key on keypad",
                checked = settings.showPercent,
                onCheckedChange = viewModel::setShowPercent
            )
            SettingSwitchRow(
                title = "Scientific Mode Toggle",
                subtitle = "Show SCI drawer button on calculator bar",
                checked = settings.showScientificToggle,
                onCheckedChange = viewModel::setShowScientificToggle
            )
            SettingSwitchRow(
                title = "Expand Scientific on Launch",
                subtitle = "Open scientific panel by default",
                checked = settings.defaultScientificExpanded,
                onCheckedChange = viewModel::setDefaultScientificExpanded
            )
            SettingSwitchRow(
                title = "Clipboard Math Detection",
                subtitle = "Offer to evaluate copied expressions",
                checked = settings.clipboardDetectionEnabled,
                onCheckedChange = viewModel::setClipboardDetection
            )

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

            // =========================================================================
            // GROUP 3: SOUND AND FEEDBACK
            // =========================================================================
            SwissSectionHeader(
                title = "Sound & Feedback",
                subtitle = "Synthesised tactile clicks & vibration"
            )

            SettingSwitchRow(
                title = "Click Sounds",
                subtitle = "Play subtle acoustic feedback on keypad presses",
                checked = settings.soundEnabled,
                onCheckedChange = viewModel::setSoundEnabled
            )
            SettingSliderRow(
                label = "Click Volume",
                valueText = "${(settings.soundVolume * 100).roundToInt()}%",
                value = settings.soundVolume,
                valueRange = 0.1f..1.0f,
                onValueChange = viewModel::setSoundVolume
            )

            SettingSubLabel("Click Sound Style (Tap Preview)")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ClickSoundStyle.entries.forEach { style ->
                    val selected = settings.soundStyle == style
                    Surface(
                        onClick = { viewModel.setSoundStyle(style) },
                        shape = RoundedCornerShape(4.dp),
                        color = if (selected) colors.accent.copy(alpha = 0.14f) else colors.cardBg,
                        border = BorderStroke(1.dp, if (selected) colors.accent else colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) colors.accent else colors.primaryText
                            )
                            Surface(
                                onClick = { viewModel.previewSound(style) },
                                shape = RoundedCornerShape(3.dp),
                                color = colors.elevatedSurface,
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "Preview", tint = colors.accent, modifier = Modifier.size(15.dp))
                                    Text("Preview", style = MaterialTheme.typography.labelSmall, color = colors.primaryText)
                                }
                            }
                        }
                    }
                }
            }

            SettingSwitchRow(
                title = "Haptic Feedback",
                subtitle = if (viewModel.isHapticSupported) "Tactile vibration on key press" else "Vibrator hardware not detected on device",
                checked = settings.hapticEnabled,
                onCheckedChange = viewModel::setHapticEnabled
            )
            if (settings.hapticEnabled) {
                SettingSubLabel("Haptic Strength")
                ChipSelectionRow(
                    items = HapticStrength.entries.map { it to it.label },
                    selected = settings.hapticStrength,
                    onSelect = viewModel::setHapticStrength
                )
            }

            // =========================================================================
            // GROUP 4: DATA & BACKUP
            // =========================================================================
            SwissSectionHeader(
                title = "Data & Backup",
                subtitle = "History export, personalisation backup & reset"
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    shape = RoundedCornerShape(4.dp),
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
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.weight(1f)
                ) { Text("EXPORT TXT", color = colors.primaryText) }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.weight(1f)
                ) { Text("BACKUP SETTINGS", color = colors.accent) }

                OutlinedButton(
                    onClick = { showImportDialog = true },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.weight(1f)
                ) { Text("IMPORT SETTINGS", color = colors.primaryText) }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showClearHistoryConfirm = true },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, colors.error.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f)
                ) { Text("CLEAR HISTORY", color = colors.error) }

                Button(
                    onClick = { showResetConfirm = true },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error.copy(alpha = 0.18f),
                        contentColor = colors.error
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reset_all_settings_button")
                ) { Text("RESET SETTINGS", fontWeight = FontWeight.Bold) }
            }

            // =========================================================================
            // GROUP 5: ABOUT
            // =========================================================================
            SwissSectionHeader(
                title = "About Cucalcus",
                subtitle = "Application & privacy details"
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = colors.cardBg,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Spacer(modifier = Modifier.height(24.dp))
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
            containerColor = colors.cardBg,
            title = { Text("Import Settings Backup", color = colors.primaryText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Paste your exported Cucalcus JSON configuration below:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryText
                    )
                    OutlinedTextField(
                        value = jsonText,
                        onValueChange = { jsonText = it },
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
            containerColor = colors.cardBg,
            title = { Text("Reset All Settings?", color = colors.primaryText) },
            text = {
                Text(
                    "Restores Obsidian Black theme, Champagne Gold accent, default typography, button layout, sound, haptics, and Quadrant icon.",
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
private fun ButtonLivePreviewRow(settings: CucalcusSettings) {
    val colors = CucalcusTheme.colors
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = colors.secondaryBg,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .height(62.dp),
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
                        shape = RoundedCornerShape(4.dp),
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
                                    .size(28.dp)
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
                shape = RoundedCornerShape(4.dp),
                color = if (isSel) colors.accent.copy(alpha = 0.16f) else colors.cardBg,
                border = BorderStroke(1.dp, if (isSel) colors.accent else colors.border)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSel) colors.accent else colors.secondaryText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = CucalcusTheme.colors
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = colors.cardBg,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.primaryText)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText)
            }
            Spacer(modifier = Modifier.width(8.dp))
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
        shape = RoundedCornerShape(4.dp),
        color = colors.cardBg,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.primaryText)
                Text(valueText, style = MaterialTheme.typography.labelLarge, color = colors.accent)
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
        shape = RoundedCornerShape(4.dp),
        color = if (selected) colors.accent.copy(alpha = 0.14f) else colors.cardBg,
        border = BorderStroke(1.dp, if (selected) colors.accent else colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
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
