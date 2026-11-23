package com.example.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.AccentPreset
import com.example.data.preferences.AppBackgroundPreset
import com.example.data.preferences.AppIconOption
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.ButtonShapeOption
import com.example.data.preferences.CucalcusSettings
import com.example.data.preferences.DisplayBoxAnimation
import com.example.data.preferences.ButtonAnimation
import com.example.data.preferences.FontOption
import com.example.data.preferences.HapticStrength
import com.example.data.preferences.KeypadArrangement
import com.example.data.preferences.SettingsRepository
import com.example.data.repository.HistoryRepository
import com.example.domain.math.AngleMode
import com.example.domain.math.NumberFormatMode
import com.example.platform.BackgroundImageManager
import com.example.platform.HapticManager
import com.example.platform.LauncherIconManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val hapticManager: HapticManager,
    private val launcherIconManager: LauncherIconManager,
    private val backgroundImageManager: BackgroundImageManager
) : ViewModel() {

    val settings: StateFlow<CucalcusSettings> = settingsRepository.settingsState

    val isHapticSupported: Boolean
        get() = hapticManager.isSupported

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(themeMode = mode) } }
    }

    fun setBackgroundPreset(preset: AppBackgroundPreset) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(backgroundPreset = preset) } }
    }

    fun importBackgroundImageFromUri(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val saved = backgroundImageManager.importFromGalleryUri(uri)
            saved.onSuccess { path ->
                settingsRepository.updateSettings { it.copy(backgroundImagePath = path) }
                onResult(true)
            }.onFailure {
                onResult(false)
            }
        }
    }

    fun clearBackgroundImage() {
        viewModelScope.launch {
            backgroundImageManager.clearBackgroundImage()
            settingsRepository.updateSettings { it.copy(backgroundImagePath = null) }
        }
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

    fun setDisplayBoxAnimation(animation: DisplayBoxAnimation) { viewModelScope.launch { settingsRepository.updateSettings { it.copy(displayBoxAnimation = animation) } } }
    fun setDisplayBoxAnimationsEnabled(enabled: Boolean) { viewModelScope.launch { settingsRepository.updateSettings { it.copy(displayBoxAnimationsEnabled = enabled) } } }
    fun setDisplayBoxAnimationPaused(paused: Boolean) { viewModelScope.launch { settingsRepository.updateSettings { it.copy(displayBoxAnimationPaused = paused) } } }
    fun setButtonAnimation(animation: ButtonAnimation) { viewModelScope.launch { settingsRepository.updateSettings { it.copy(buttonAnimation = animation) } } }
    fun setButtonAnimationsEnabled(enabled: Boolean) { viewModelScope.launch { settingsRepository.updateSettings { it.copy(buttonAnimationsEnabled = enabled) } } }
    fun setButtonAnimationPaused(paused: Boolean) { viewModelScope.launch { settingsRepository.updateSettings { it.copy(buttonAnimationPaused = paused) } } }

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
        val clamped = percent.coerceIn(8, 50)
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

    fun setBorderlessButtons(borderless: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(borderlessButtons = borderless) } }
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
        return applyAppIcon(AppIconOption.NORMAL_GREEN)
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
                settingsRepository.updateSettings { current ->
                    imported.copy(
                        backgroundImagePath = current.backgroundImagePath
                    )
                }
                launcherIconManager.applyLauncherIcon(imported.selectedIcon)
            }
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            backgroundImageManager.clearBackgroundImage()
            settingsRepository.resetAllSettings()
            launcherIconManager.applyLauncherIcon(AppIconOption.INDIA_FLAG)
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val historyRepository: HistoryRepository,
        private val hapticManager: HapticManager,
        private val launcherIconManager: LauncherIconManager,
        private val backgroundImageManager: BackgroundImageManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                settingsRepository,
                historyRepository,
                hapticManager,
                launcherIconManager,
                backgroundImageManager
            ) as T
        }
    }
}
