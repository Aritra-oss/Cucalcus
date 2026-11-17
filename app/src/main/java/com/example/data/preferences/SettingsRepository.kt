package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.math.AngleMode
import com.example.domain.math.NumberFormatMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cucalcus_prefs")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val CUSTOM_ACCENT_ARGB = longPreferencesKey("custom_accent_argb")

        val DISPLAY_FONT = stringPreferencesKey("display_font")
        val BUTTON_FONT = stringPreferencesKey("button_font")
        val INTERFACE_FONT = stringPreferencesKey("interface_font")
        val FONT_SCALE = floatPreferencesKey("font_scale")

        val BUTTON_SHAPE = stringPreferencesKey("button_shape")
        val BUTTON_CORNER_RADIUS_PERCENT = intPreferencesKey("button_corner_radius_percent")
        val BUTTON_SIZE_SCALE = floatPreferencesKey("button_size_scale")
        val BUTTON_SPACING_DP = floatPreferencesKey("button_spacing_dp")
        val BUTTON_OPACITY = floatPreferencesKey("button_opacity")
        val BUTTON_BG_ARGB = longPreferencesKey("button_bg_argb")
        val BUTTON_TEXT_ARGB = longPreferencesKey("button_text_argb")
        val USE_CUSTOM_BUTTON_COLORS = booleanPreferencesKey("use_custom_button_colors")

        val SHOW_DOUBLE_ZERO = booleanPreferencesKey("show_double_zero")
        val SHOW_TRIPLE_ZERO = booleanPreferencesKey("show_triple_zero")
        val SHOW_DECIMAL = booleanPreferencesKey("show_decimal")
        val SHOW_PERCENT = booleanPreferencesKey("show_percent")
        val SHOW_SCIENTIFIC_TOGGLE = booleanPreferencesKey("show_scientific_toggle")
        val KEYPAD_ARRANGEMENT = stringPreferencesKey("keypad_arrangement")

        val DEFAULT_SCIENTIFIC_EXPANDED = booleanPreferencesKey("default_scientific_expanded")
        val ANGLE_MODE = stringPreferencesKey("angle_mode")
        val DECIMAL_PRECISION = intPreferencesKey("decimal_precision")
        val NUMBER_FORMAT_MODE = stringPreferencesKey("number_format_mode")
        val CLIPBOARD_DETECTION = booleanPreferencesKey("clipboard_detection")

        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SOUND_VOLUME = floatPreferencesKey("sound_volume")
        val SOUND_STYLE = stringPreferencesKey("sound_style")

        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val HAPTIC_STRENGTH = stringPreferencesKey("haptic_strength")

        val SELECTED_ICON = stringPreferencesKey("selected_icon")
        val CALCULATOR_MEMORY = doublePreferencesKey("calculator_memory")

        // Currency Cache Keys
        val CURRENCY_CACHE_BASE = stringPreferencesKey("currency_cache_base")
        val CURRENCY_CACHE_RATES_JSON = stringPreferencesKey("currency_cache_rates_json")
        val CURRENCY_CACHE_FETCHED_AT = longPreferencesKey("currency_cache_fetched_at")
        val CURRENCY_CACHE_API_TIME = longPreferencesKey("currency_cache_api_time")
    }

    val settingsFlow: Flow<CucalcusSettings> = context.dataStore.data.map { prefs ->
        val d = CucalcusSettings()
        CucalcusSettings(
            themeMode = parseEnum(prefs[Keys.THEME_MODE], d.themeMode),
            accentPreset = parseEnum(prefs[Keys.ACCENT_PRESET], d.accentPreset),
            customAccentArgb = prefs[Keys.CUSTOM_ACCENT_ARGB] ?: d.customAccentArgb,
            displayFont = parseEnum(prefs[Keys.DISPLAY_FONT], d.displayFont),
            buttonFont = parseEnum(prefs[Keys.BUTTON_FONT], d.buttonFont),
            interfaceFont = parseEnum(prefs[Keys.INTERFACE_FONT], d.interfaceFont),
            fontScale = prefs[Keys.FONT_SCALE] ?: d.fontScale,
            buttonShape = parseEnum(prefs[Keys.BUTTON_SHAPE], d.buttonShape),
            buttonCornerRadiusPercent = (prefs[Keys.BUTTON_CORNER_RADIUS_PERCENT] ?: d.buttonCornerRadiusPercent).coerceIn(0, 50),
            buttonSizeScale = prefs[Keys.BUTTON_SIZE_SCALE] ?: d.buttonSizeScale,
            buttonSpacingDp = prefs[Keys.BUTTON_SPACING_DP] ?: d.buttonSpacingDp,
            buttonOpacity = prefs[Keys.BUTTON_OPACITY] ?: d.buttonOpacity,
            buttonBgArgb = prefs[Keys.BUTTON_BG_ARGB] ?: d.buttonBgArgb,
            buttonTextArgb = prefs[Keys.BUTTON_TEXT_ARGB] ?: d.buttonTextArgb,
            useCustomButtonColors = prefs[Keys.USE_CUSTOM_BUTTON_COLORS] ?: d.useCustomButtonColors,
            showDoubleZero = prefs[Keys.SHOW_DOUBLE_ZERO] ?: d.showDoubleZero,
            showTripleZero = prefs[Keys.SHOW_TRIPLE_ZERO] ?: d.showTripleZero,
            showDecimal = prefs[Keys.SHOW_DECIMAL] ?: d.showDecimal,
            showPercent = prefs[Keys.SHOW_PERCENT] ?: d.showPercent,
            showScientificToggle = prefs[Keys.SHOW_SCIENTIFIC_TOGGLE] ?: d.showScientificToggle,
            keypadArrangement = parseEnum(prefs[Keys.KEYPAD_ARRANGEMENT], d.keypadArrangement),
            defaultScientificExpanded = prefs[Keys.DEFAULT_SCIENTIFIC_EXPANDED] ?: d.defaultScientificExpanded,
            angleMode = parseEnum(prefs[Keys.ANGLE_MODE], d.angleMode),
            decimalPrecision = prefs[Keys.DECIMAL_PRECISION] ?: d.decimalPrecision,
            numberFormatMode = parseEnum(prefs[Keys.NUMBER_FORMAT_MODE], d.numberFormatMode),
            clipboardDetectionEnabled = prefs[Keys.CLIPBOARD_DETECTION] ?: d.clipboardDetectionEnabled,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: d.soundEnabled,
            soundVolume = prefs[Keys.SOUND_VOLUME] ?: d.soundVolume,
            soundStyle = parseEnum(prefs[Keys.SOUND_STYLE], d.soundStyle),
            hapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: d.hapticEnabled,
            hapticStrength = parseEnum(prefs[Keys.HAPTIC_STRENGTH], d.hapticStrength),
            selectedIcon = parseEnum(prefs[Keys.SELECTED_ICON], d.selectedIcon),
            calculatorMemory = prefs[Keys.CALCULATOR_MEMORY] ?: d.calculatorMemory
        )
    }

    suspend fun updateSettings(transform: (CucalcusSettings) -> CucalcusSettings) {
        context.dataStore.edit { prefs ->
            val current = mapPrefsToSettings(prefs)
            val updated = transform(current)
            writeSettingsToPrefs(prefs, updated)
        }
    }

    suspend fun updateMemory(value: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CALCULATOR_MEMORY] = value
        }
    }

    suspend fun resetAllSettings() {
        context.dataStore.edit { prefs ->
            val defaults = CucalcusSettings()
            writeSettingsToPrefs(prefs, defaults)
            prefs[Keys.CALCULATOR_MEMORY] = 0.0
        }
    }

    data class CachedCurrencyData(
        val baseCode: String,
        val ratesJson: String,
        val fetchedAtMs: Long,
        val apiTimestampSec: Long
    )

    val cachedCurrencyFlow: Flow<CachedCurrencyData?> = context.dataStore.data.map { prefs ->
        val base = prefs[Keys.CURRENCY_CACHE_BASE]
        val json = prefs[Keys.CURRENCY_CACHE_RATES_JSON]
        val fetchedAt = prefs[Keys.CURRENCY_CACHE_FETCHED_AT]
        val apiTime = prefs[Keys.CURRENCY_CACHE_API_TIME] ?: 0L
        if (base != null && json != null && fetchedAt != null) {
            CachedCurrencyData(base, json, fetchedAt, apiTime)
        } else {
            null
        }
    }

    suspend fun saveCurrencyCache(baseCode: String, ratesJson: String, fetchedAtMs: Long, apiTimestampSec: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENCY_CACHE_BASE] = baseCode
            prefs[Keys.CURRENCY_CACHE_RATES_JSON] = ratesJson
            prefs[Keys.CURRENCY_CACHE_FETCHED_AT] = fetchedAtMs
            prefs[Keys.CURRENCY_CACHE_API_TIME] = apiTimestampSec
        }
    }

    private fun mapPrefsToSettings(prefs: Preferences): CucalcusSettings {
        val d = CucalcusSettings()
        return CucalcusSettings(
            themeMode = parseEnum(prefs[Keys.THEME_MODE], d.themeMode),
            accentPreset = parseEnum(prefs[Keys.ACCENT_PRESET], d.accentPreset),
            customAccentArgb = prefs[Keys.CUSTOM_ACCENT_ARGB] ?: d.customAccentArgb,
            displayFont = parseEnum(prefs[Keys.DISPLAY_FONT], d.displayFont),
            buttonFont = parseEnum(prefs[Keys.BUTTON_FONT], d.buttonFont),
            interfaceFont = parseEnum(prefs[Keys.INTERFACE_FONT], d.interfaceFont),
            fontScale = prefs[Keys.FONT_SCALE] ?: d.fontScale,
            buttonShape = parseEnum(prefs[Keys.BUTTON_SHAPE], d.buttonShape),
            buttonCornerRadiusPercent = (prefs[Keys.BUTTON_CORNER_RADIUS_PERCENT] ?: d.buttonCornerRadiusPercent).coerceIn(0, 50),
            buttonSizeScale = prefs[Keys.BUTTON_SIZE_SCALE] ?: d.buttonSizeScale,
            buttonSpacingDp = prefs[Keys.BUTTON_SPACING_DP] ?: d.buttonSpacingDp,
            buttonOpacity = prefs[Keys.BUTTON_OPACITY] ?: d.buttonOpacity,
            buttonBgArgb = prefs[Keys.BUTTON_BG_ARGB] ?: d.buttonBgArgb,
            buttonTextArgb = prefs[Keys.BUTTON_TEXT_ARGB] ?: d.buttonTextArgb,
            useCustomButtonColors = prefs[Keys.USE_CUSTOM_BUTTON_COLORS] ?: d.useCustomButtonColors,
            showDoubleZero = prefs[Keys.SHOW_DOUBLE_ZERO] ?: d.showDoubleZero,
            showTripleZero = prefs[Keys.SHOW_TRIPLE_ZERO] ?: d.showTripleZero,
            showDecimal = prefs[Keys.SHOW_DECIMAL] ?: d.showDecimal,
            showPercent = prefs[Keys.SHOW_PERCENT] ?: d.showPercent,
            showScientificToggle = prefs[Keys.SHOW_SCIENTIFIC_TOGGLE] ?: d.showScientificToggle,
            keypadArrangement = parseEnum(prefs[Keys.KEYPAD_ARRANGEMENT], d.keypadArrangement),
            defaultScientificExpanded = prefs[Keys.DEFAULT_SCIENTIFIC_EXPANDED] ?: d.defaultScientificExpanded,
            angleMode = parseEnum(prefs[Keys.ANGLE_MODE], d.angleMode),
            decimalPrecision = prefs[Keys.DECIMAL_PRECISION] ?: d.decimalPrecision,
            numberFormatMode = parseEnum(prefs[Keys.NUMBER_FORMAT_MODE], d.numberFormatMode),
            clipboardDetectionEnabled = prefs[Keys.CLIPBOARD_DETECTION] ?: d.clipboardDetectionEnabled,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: d.soundEnabled,
            soundVolume = prefs[Keys.SOUND_VOLUME] ?: d.soundVolume,
            soundStyle = parseEnum(prefs[Keys.SOUND_STYLE], d.soundStyle),
            hapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: d.hapticEnabled,
            hapticStrength = parseEnum(prefs[Keys.HAPTIC_STRENGTH], d.hapticStrength),
            selectedIcon = parseEnum(prefs[Keys.SELECTED_ICON], d.selectedIcon),
            calculatorMemory = prefs[Keys.CALCULATOR_MEMORY] ?: d.calculatorMemory
        )
    }

    private fun writeSettingsToPrefs(prefs: androidx.datastore.preferences.core.MutablePreferences, s: CucalcusSettings) {
        prefs[Keys.THEME_MODE] = s.themeMode.name
        prefs[Keys.ACCENT_PRESET] = s.accentPreset.name
        prefs[Keys.CUSTOM_ACCENT_ARGB] = s.customAccentArgb
        prefs[Keys.DISPLAY_FONT] = s.displayFont.name
        prefs[Keys.BUTTON_FONT] = s.buttonFont.name
        prefs[Keys.INTERFACE_FONT] = s.interfaceFont.name
        prefs[Keys.FONT_SCALE] = s.fontScale
        prefs[Keys.BUTTON_SHAPE] = s.buttonShape.name
        prefs[Keys.BUTTON_CORNER_RADIUS_PERCENT] = s.buttonCornerRadiusPercent
        prefs[Keys.BUTTON_SIZE_SCALE] = s.buttonSizeScale
        prefs[Keys.BUTTON_SPACING_DP] = s.buttonSpacingDp
        prefs[Keys.BUTTON_OPACITY] = s.buttonOpacity
        prefs[Keys.BUTTON_BG_ARGB] = s.buttonBgArgb
        prefs[Keys.BUTTON_TEXT_ARGB] = s.buttonTextArgb
        prefs[Keys.USE_CUSTOM_BUTTON_COLORS] = s.useCustomButtonColors
        prefs[Keys.SHOW_DOUBLE_ZERO] = s.showDoubleZero
        prefs[Keys.SHOW_TRIPLE_ZERO] = s.showTripleZero
        prefs[Keys.SHOW_DECIMAL] = s.showDecimal
        prefs[Keys.SHOW_PERCENT] = s.showPercent
        prefs[Keys.SHOW_SCIENTIFIC_TOGGLE] = s.showScientificToggle
        prefs[Keys.KEYPAD_ARRANGEMENT] = s.keypadArrangement.name
        prefs[Keys.DEFAULT_SCIENTIFIC_EXPANDED] = s.defaultScientificExpanded
        prefs[Keys.ANGLE_MODE] = s.angleMode.name
        prefs[Keys.DECIMAL_PRECISION] = s.decimalPrecision
        prefs[Keys.NUMBER_FORMAT_MODE] = s.numberFormatMode.name
        prefs[Keys.CLIPBOARD_DETECTION] = s.clipboardDetectionEnabled
        prefs[Keys.SOUND_ENABLED] = s.soundEnabled
        prefs[Keys.SOUND_VOLUME] = s.soundVolume
        prefs[Keys.SOUND_STYLE] = s.soundStyle.name
        prefs[Keys.HAPTIC_ENABLED] = s.hapticEnabled
        prefs[Keys.HAPTIC_STRENGTH] = s.hapticStrength.name
        prefs[Keys.SELECTED_ICON] = s.selectedIcon.name
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String?, default: T): T {
        if (value == null) return default
        return enumValues<T>().firstOrNull { it.name == value } ?: default
    }
}
