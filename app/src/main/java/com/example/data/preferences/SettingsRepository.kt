package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cucalcus_prefs")

class SettingsRepository(private val context: Context) {

    private val appContext: Context = context.applicationContext ?: context
    private val bootPrefs = appContext.getSharedPreferences(BOOT_PREFS_NAME, Context.MODE_PRIVATE)
    private val aiQuotaPrefs = appContext.getSharedPreferences(AI_QUOTA_PREFS_NAME, Context.MODE_PRIVATE)
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BACKGROUND_PRESET = stringPreferencesKey("background_preset")
        val BACKGROUND_IMAGE_PATH = stringPreferencesKey("background_image_path")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val CUSTOM_ACCENT_ARGB = longPreferencesKey("custom_accent_argb")

        val DISPLAY_FONT = stringPreferencesKey("display_font")
        val BUTTON_FONT = stringPreferencesKey("button_font")
        val INTERFACE_FONT = stringPreferencesKey("interface_font")
        val FONT_SCALE = floatPreferencesKey("font_scale")

        val BUTTON_SHAPE = stringPreferencesKey("button_shape")
        val BUTTON_CORNER_RADIUS_PERCENT = intPreferencesKey("button_corner_radius_percent")
        val BORDERLESS_BUTTONS = booleanPreferencesKey("borderless_buttons")
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

    private val _settingsState = MutableStateFlow(loadInitialSettingsSync())
    val settingsState: StateFlow<CucalcusSettings> = _settingsState.asStateFlow()

    val currentSettings: CucalcusSettings
        get() = _settingsState.value

    val settingsFlow: Flow<CucalcusSettings> = settingsState

    init {
        repoScope.launch {
            appContext.dataStore.data
                .map { prefs -> mapPrefsToSettings(prefs) }
                .collect { mapped ->
                    _settingsState.value = mapped
                    saveBootSnapshot(mapped)
                }
        }
    }

    private fun loadInitialSettingsSync(): CucalcusSettings {
        val cachedJson = bootPrefs.getString(KEY_BOOT_JSON, null)
        if (!cachedJson.isNullOrBlank()) {
            val parsed = CucalcusSettings.fromJsonString(cachedJson).getOrNull()
            if (parsed != null) {
                val bgPath = bootPrefs.getString(KEY_BOOT_BG_PATH, null)?.takeIf { it.isNotBlank() }
                val memory = if (bootPrefs.contains(KEY_BOOT_MEMORY_BITS)) {
                    Double.fromBits(bootPrefs.getLong(KEY_BOOT_MEMORY_BITS, 0L))
                } else {
                    0.0
                }
                return parsed.copy(
                    backgroundImagePath = bgPath,
                    calculatorMemory = memory
                )
            }
        }
        // First run after upgrade or clean install: read DataStore once synchronously and mirror it
        val initial = runCatching {
            runBlocking(Dispatchers.IO) {
                mapPrefsToSettings(appContext.dataStore.data.first())
            }
        }.getOrElse { CucalcusSettings() }
        saveBootSnapshot(initial)
        return initial
    }

    private fun saveBootSnapshot(settings: CucalcusSettings) {
        val editor = bootPrefs.edit()
            .putString(KEY_BOOT_JSON, settings.toJsonString())
            .putLong(KEY_BOOT_MEMORY_BITS, settings.calculatorMemory.toBits())
        if (settings.backgroundImagePath.isNullOrBlank()) {
            editor.remove(KEY_BOOT_BG_PATH)
        } else {
            editor.putString(KEY_BOOT_BG_PATH, settings.backgroundImagePath)
        }
        editor.apply()
    }

    suspend fun updateSettings(transform: (CucalcusSettings) -> CucalcusSettings) {
        val immediate = transform(_settingsState.value)
        _settingsState.value = immediate
        saveBootSnapshot(immediate)

        var updatedSnapshot: CucalcusSettings? = null
        appContext.dataStore.edit { prefs ->
            val current = mapPrefsToSettings(prefs)
            val updated = transform(current)
            writeSettingsToPrefs(prefs, updated)
            updatedSnapshot = updated
        }
        updatedSnapshot?.let { persisted ->
            _settingsState.value = persisted
            saveBootSnapshot(persisted)
        }
    }

    suspend fun updateMemory(value: Double) {
        val immediate = _settingsState.value.copy(calculatorMemory = value)
        _settingsState.value = immediate
        saveBootSnapshot(immediate)

        appContext.dataStore.edit { prefs ->
            prefs[Keys.CALCULATOR_MEMORY] = value
        }
    }

    suspend fun addToMemory(delta: Double) {
        appContext.dataStore.edit { prefs ->
            val current = prefs[Keys.CALCULATOR_MEMORY] ?: 0.0
            val next = current + delta
            prefs[Keys.CALCULATOR_MEMORY] = next
            val updated = mapPrefsToSettings(prefs)
            _settingsState.value = updated
            saveBootSnapshot(updated)
        }
    }

    suspend fun resetAllSettings() {
        val defaults = CucalcusSettings()
        _settingsState.value = defaults
        saveBootSnapshot(defaults)

        appContext.dataStore.edit { prefs ->
            writeSettingsToPrefs(prefs, defaults)
            prefs[Keys.CALCULATOR_MEMORY] = 0.0
        }
    }

    /**
     * Local AI allowance: ten submitted questions per device installation per
     * calendar day. Clearing app data or uninstalling resets this preference.
     */
    @Synchronized
    fun aiQuestionsRemainingToday(): Int {
        val today = currentAiQuotaDay()
        val savedDay = aiQuotaPrefs.getInt(KEY_AI_QUOTA_DAY, -1)
        if (savedDay != today) {
            aiQuotaPrefs.edit()
                .putInt(KEY_AI_QUOTA_DAY, today)
                .putInt(KEY_AI_QUOTA_USED, 0)
                .apply()
            return AI_DAILY_QUESTION_LIMIT
        }
        return (AI_DAILY_QUESTION_LIMIT - aiQuotaPrefs.getInt(KEY_AI_QUOTA_USED, 0))
            .coerceIn(0, AI_DAILY_QUESTION_LIMIT)
    }

    /** Atomically consumes one question, returning the remaining count. */
    @Synchronized
    fun consumeAiQuestion(): Int {
        val remaining = aiQuestionsRemainingToday()
        if (remaining <= 0) return -1
        val used = AI_DAILY_QUESTION_LIMIT - remaining + 1
        aiQuotaPrefs.edit()
            .putInt(KEY_AI_QUOTA_DAY, currentAiQuotaDay())
            .putInt(KEY_AI_QUOTA_USED, used)
            .commit()
        return AI_DAILY_QUESTION_LIMIT - used
    }

    private fun currentAiQuotaDay(): Int {
        val calendar = java.util.Calendar.getInstance()
        return calendar.get(java.util.Calendar.YEAR) * 1000 +
            calendar.get(java.util.Calendar.DAY_OF_YEAR)
    }

    companion object {
        const val AI_DAILY_QUESTION_LIMIT = 10
        private const val KEY_AI_QUOTA_DAY = "ai_quota_day"
        private const val KEY_AI_QUOTA_USED = "ai_quota_used"
        private const val BOOT_PREFS_NAME = "cucalcus_boot_settings"
        private const val AI_QUOTA_PREFS_NAME = "ai_daily_quota"
        private const val KEY_BOOT_JSON = "boot_json"
        private const val KEY_BOOT_BG_PATH = "boot_bg_path"
        private const val KEY_BOOT_MEMORY_BITS = "boot_memory_bits"
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
            backgroundPreset = parseEnum(prefs[Keys.BACKGROUND_PRESET], d.backgroundPreset),
            backgroundImagePath = prefs[Keys.BACKGROUND_IMAGE_PATH]?.takeIf { it.isNotBlank() },
            accentPreset = parseEnum(prefs[Keys.ACCENT_PRESET], d.accentPreset),
            customAccentArgb = prefs[Keys.CUSTOM_ACCENT_ARGB] ?: d.customAccentArgb,
            displayFont = parseEnum(prefs[Keys.DISPLAY_FONT], d.displayFont),
            buttonFont = parseEnum(prefs[Keys.BUTTON_FONT], d.buttonFont),
            interfaceFont = parseEnum(prefs[Keys.INTERFACE_FONT], d.interfaceFont),
            fontScale = prefs[Keys.FONT_SCALE] ?: d.fontScale,
            buttonShape = parseEnum(prefs[Keys.BUTTON_SHAPE], d.buttonShape),
            buttonCornerRadiusPercent = (prefs[Keys.BUTTON_CORNER_RADIUS_PERCENT] ?: d.buttonCornerRadiusPercent).coerceIn(8, 50),
            borderlessButtons = prefs[Keys.BORDERLESS_BUTTONS] ?: d.borderlessButtons,
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
            hapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: d.hapticEnabled,
            hapticStrength = parseEnum(prefs[Keys.HAPTIC_STRENGTH], d.hapticStrength),
            selectedIcon = parseEnum(prefs[Keys.SELECTED_ICON], d.selectedIcon),
            calculatorMemory = prefs[Keys.CALCULATOR_MEMORY] ?: d.calculatorMemory
        )
    }

    private fun writeSettingsToPrefs(prefs: MutablePreferences, s: CucalcusSettings) {
        prefs[Keys.THEME_MODE] = s.themeMode.name
        prefs[Keys.BACKGROUND_PRESET] = s.backgroundPreset.name
        if (s.backgroundImagePath.isNullOrBlank()) {
            prefs.remove(Keys.BACKGROUND_IMAGE_PATH)
        } else {
            prefs[Keys.BACKGROUND_IMAGE_PATH] = s.backgroundImagePath
        }
        prefs[Keys.ACCENT_PRESET] = s.accentPreset.name
        prefs[Keys.CUSTOM_ACCENT_ARGB] = s.customAccentArgb
        prefs[Keys.DISPLAY_FONT] = s.displayFont.name
        prefs[Keys.BUTTON_FONT] = s.buttonFont.name
        prefs[Keys.INTERFACE_FONT] = s.interfaceFont.name
        prefs[Keys.FONT_SCALE] = s.fontScale
        prefs[Keys.BUTTON_SHAPE] = s.buttonShape.name
        prefs[Keys.BUTTON_CORNER_RADIUS_PERCENT] = s.buttonCornerRadiusPercent
        prefs[Keys.BORDERLESS_BUTTONS] = s.borderlessButtons
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
        prefs[Keys.HAPTIC_ENABLED] = s.hapticEnabled
        prefs[Keys.HAPTIC_STRENGTH] = s.hapticStrength.name
        prefs[Keys.SELECTED_ICON] = s.selectedIcon.name
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String?, default: T): T {
        if (value == null) return default
        return enumValues<T>().firstOrNull { it.name == value } ?: default
    }
}
