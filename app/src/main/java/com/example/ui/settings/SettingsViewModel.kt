package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.AccentPreset
import com.example.data.preferences.AppIconOption
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.ButtonShapeOption
import com.example.data.preferences.ClickSoundStyle
import com.example.data.preferences.CucalcusSettings
import com.example.data.preferences.FontOption
import com.example.data.preferences.HapticStrength
import com.example.data.preferences.KeypadArrangement
import com.example.data.preferences.SettingsRepository
import com.example.data.repository.HistoryRepository
import com.example.domain.math.AngleMode
import com.example.domain.math.NumberFormatMode
import com.example.platform.HapticManager
import com.example.platform.LauncherIconManager
import com.example.platform.SoundManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager,
    private val launcherIconManager: LauncherIconManager
) : ViewModel() {

    val settings: StateFlow<CucalcusSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, CucalcusSettings())

    val isHapticSupported: Boolean
        get() = hapticManager.isSupported

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(themeMode = mode) } }
    }

    fun selectAccentPreset(preset: AccentPreset) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(accentPreset = preset) } }
    }

    fun setCustomAccentArgb(argb: Long) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(accentPreset = AccentPreset.CUSTOM, customAccentArgb = argb)
            }
        }
    }

    fun setDisplayFont(font: FontOption) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(displayFont = font) } }
    }

    fun setButtonFont(font: FontOption) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(buttonFont = font) } }
    }

    fun setInterfaceFont(font: FontOption) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(interfaceFont = font) } }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(fontScale = scale.coerceIn(0.85f, 1.25f)) } }
    }

    fun setButtonShape(shape: ButtonShapeOption) {
        viewModelScope.launch {
            settingsRepository.updateSettings { current ->
                current.copy(
                    buttonShape = shape,
                    buttonCornerRadiusPercent = shape.defaultRadiusPercent ?: current.buttonCornerRadiusPercent
                )
            }
        }
    }

    fun setButtonCornerRadiusPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 50)
        val matchedPreset = ButtonShapeOption.entries.firstOrNull { it.defaultRadiusPercent == clamped }
        viewModelScope.launch {
            settingsRepository.updateSettings { current ->
                current.copy(
                    buttonShape = matchedPreset ?: ButtonShapeOption.CUSTOM_RADIUS,
                    buttonCornerRadiusPercent = clamped
                )
            }
        }
    }

    fun setButtonSizeScale(scale: Float) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(buttonSizeScale = scale.coerceIn(0.85f, 1.15f)) } }
    }

    fun setButtonSpacing(spacing: Float) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(buttonSpacingDp = spacing.coerceIn(2f, 12f)) } }
    }

    fun setButtonOpacity(opacity: Float) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(buttonOpacity = opacity.coerceIn(0.4f, 1.0f)) } }
    }

    fun setUseCustomButtonColors(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(useCustomButtonColors = enabled) } }
    }

    fun setButtonBgColor(argb: Long) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(useCustomButtonColors = true, buttonBgArgb = argb) } }
    }

    fun setButtonTextColor(argb: Long) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(useCustomButtonColors = true, buttonTextArgb = argb) } }
    }

    fun setShowDoubleZero(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(showDoubleZero = enabled) } }
    }

    fun setShowTripleZero(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(showTripleZero = enabled) } }
    }

    fun setShowDecimal(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(showDecimal = enabled) } }
    }

    fun setShowPercent(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(showPercent = enabled) } }
    }

    fun setShowScientificToggle(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(showScientificToggle = enabled) } }
    }

    fun setKeypadArrangement(arrangement: KeypadArrangement) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(keypadArrangement = arrangement) } }
    }

    fun setDefaultScientificExpanded(expanded: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(defaultScientificExpanded = expanded) } }
    }

    fun setDefaultAngleMode(mode: AngleMode) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(angleMode = mode) } }
    }

    fun setDecimalPrecision(precision: Int) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(decimalPrecision = precision.coerceIn(0, 12)) } }
    }

    fun setNumberFormatMode(mode: NumberFormatMode) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(numberFormatMode = mode) } }
    }

    fun setClipboardDetection(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(clipboardDetectionEnabled = enabled) } }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(soundEnabled = enabled) } }
    }

    fun setSoundVolume(volume: Float) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(soundVolume = volume.coerceIn(0f, 1f)) } }
    }

    fun setSoundStyle(style: ClickSoundStyle) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(soundStyle = style) } }
        soundManager.playClick(style, settings.value.soundVolume, enabled = true)
    }

    fun previewSound(style: ClickSoundStyle) {
        soundManager.playClick(style, settings.value.soundVolume, enabled = true)
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(hapticEnabled = enabled) } }
        if (enabled) hapticManager.performTick(settings.value.hapticStrength, true)
    }

    fun setHapticStrength(strength: HapticStrength) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(hapticStrength = strength) } }
        hapticManager.performTick(strength, true)
    }

    fun applyAppIcon(option: AppIconOption): Boolean {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(selectedIcon = option) }
        }
        return launcherIconManager.applyLauncherIcon(option)
    }

    fun resetAppIconToDefault(): Boolean {
        return applyAppIcon(AppIconOption.QUADRANT)
    }

    suspend fun exportHistoryCsv(): String = historyRepository.exportCsv()
    suspend fun exportHistoryTxt(): String = historyRepository.exportTxt()

    fun clearAllHistory() {
        viewModelScope.launch { historyRepository.clearAll() }
    }

    fun exportSettingsJson(): String = settings.value.toJsonString()

    fun importSettingsJson(rawJson: String): Result<Unit> {
        val parsed = CucalcusSettings.fromJsonString(rawJson)
        return parsed.map { imported ->
            viewModelScope.launch {
                settingsRepository.updateSettings { imported }
                launcherIconManager.applyLauncherIcon(imported.selectedIcon)
            }
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            settingsRepository.resetAllSettings()
            launcherIconManager.applyLauncherIcon(AppIconOption.QUADRANT)
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val historyRepository: HistoryRepository,
        private val soundManager: SoundManager,
        private val hapticManager: HapticManager,
        private val launcherIconManager: LauncherIconManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                settingsRepository,
                historyRepository,
                soundManager,
                hapticManager,
                launcherIconManager
            ) as T
        }
    }
}
