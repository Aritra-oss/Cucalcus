package com.example.data.preferences

import com.example.domain.math.AngleMode
import com.example.domain.math.NumberFormatMode
import org.json.JSONObject

enum class AccentPreset(val label: String, val argbHex: Long) {
    CHAMPAGNE_GOLD("Champagne Gold", 0xFFD6B56DL),
    PLATINUM_SILVER("Platinum Silver", 0xFFC8CDD4L),
    ROYAL_PURPLE("Royal Purple", 0xFF9E7BFFL),
    EMERALD_GREEN("Emerald Green", 0xFF4FB286L),
    OCEAN_BLUE("Ocean Blue", 0xFF5A9CF8L),
    CRIMSON_RED("Crimson Red", 0xFFE05A5AL),
    ROSE_GOLD("Rose Gold", 0xFFD89A9EL),
    CUSTOM("Custom", 0xFFD6B56DL)
}

enum class AppThemeMode(val label: String) {
    OBSIDIAN_BLACK("Obsidian Black (Default)"),
    GRAPHITE_DARK("Graphite Charcoal"),
    SWISS_PAPER_LIGHT("Swiss Paper Light")
}

enum class FontOption(val label: String) {
    SANS_SERIF("Sans-serif (Inter)"),
    SERIF("Serif (Lora)"),
    MONOSPACE("Monospace (JetBrains)"),
    GEOMETRIC("Geometric (Space Grotesk)"),
    ELEGANT_DISPLAY("Display (Playfair)")
}

enum class ButtonShapeOption(val label: String) {
    SQUARE("Square"),
    SLIGHTLY_ROUNDED("Slightly Rounded"),
    ROUNDED("Rounded"),
    CIRCULAR("Circular")
}

enum class KeypadArrangement(val label: String) {
    STANDARD("Standard (Operators Right)"),
    LEFT_HANDED("Left-Handed (Operators Left)")
}

enum class ClickSoundStyle(val label: String) {
    SOFT_CLICK("Soft Click"),
    MECHANICAL_CLICK("Mechanical Click"),
    DIGITAL_CLICK("Digital Click"),
    CLASSIC_CALCULATOR("Classic Calculator")
}

enum class HapticStrength(val label: String) {
    GENTLE("Gentle"),
    MEDIUM("Medium"),
    CRISP("Crisp")
}

enum class IconCategory(val title: String) {
    MATHEMATICAL("Category A: Mathematical Collection"),
    IPL_INSPIRED("Category B: IPL-Inspired Collection")
}

enum class AppIconOption(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val category: IconCategory,
    val aliasClassName: String
) {
    QUADRANT(
        id = "quadrant",
        displayName = "Quadrant",
        subtitle = "Black & Champagne Gold",
        category = IconCategory.MATHEMATICAL,
        aliasClassName = "com.example.LauncherAliasQuadrant"
    ),
    INTERSECTION(
        id = "intersection",
        displayName = "Intersection",
        subtitle = "Interlocking Geometry",
        category = IconCategory.MATHEMATICAL,
        aliasClassName = "com.example.LauncherAliasIntersection"
    ),
    NEGATIVE_SPACE(
        id = "negative_space",
        displayName = "Negative Space",
        subtitle = "Minimalist Cutout Symbols",
        category = IconCategory.MATHEMATICAL,
        aliasClassName = "com.example.LauncherAliasNegativeSpace"
    ),
    PRECISION(
        id = "precision",
        displayName = "Precision",
        subtitle = "Platinum Silver Geometry",
        category = IconCategory.MATHEMATICAL,
        aliasClassName = "com.example.LauncherAliasPrecision"
    ),
    KKR(
        id = "kkr",
        displayName = "KKR Edition",
        subtitle = "Purple & Gold Knight",
        category = IconCategory.IPL_INSPIRED,
        aliasClassName = "com.example.LauncherAliasKkr"
    ),
    RCB(
        id = "rcb",
        displayName = "RCB Edition",
        subtitle = "Black, Red & Gold Lion",
        category = IconCategory.IPL_INSPIRED,
        aliasClassName = "com.example.LauncherAliasRcb"
    ),
    MI(
        id = "mi",
        displayName = "MI Edition",
        subtitle = "Deep Blue & Gold Energy",
        category = IconCategory.IPL_INSPIRED,
        aliasClassName = "com.example.LauncherAliasMi"
    ),
    CSK(
        id = "csk",
        displayName = "CSK Edition",
        subtitle = "Yellow & Navy Lion",
        category = IconCategory.IPL_INSPIRED,
        aliasClassName = "com.example.LauncherAliasCsk"
    )
}

data class CucalcusSettings(
    val themeMode: AppThemeMode = AppThemeMode.OBSIDIAN_BLACK,
    val accentPreset: AccentPreset = AccentPreset.CHAMPAGNE_GOLD,
    val customAccentArgb: Long = 0xFFD6B56DL,

    val displayFont: FontOption = FontOption.GEOMETRIC,
    val buttonFont: FontOption = FontOption.SANS_SERIF,
    val interfaceFont: FontOption = FontOption.SANS_SERIF,
    val fontScale: Float = 1.0f,

    val buttonShape: ButtonShapeOption = ButtonShapeOption.SLIGHTLY_ROUNDED,
    val buttonSizeScale: Float = 1.0f,
    val buttonSpacingDp: Float = 6f,
    val buttonOpacity: Float = 1.0f,
    val buttonBgArgb: Long = 0xFF171717L,
    val buttonTextArgb: Long = 0xFFF5F2EAL,
    val useCustomButtonColors: Boolean = false,

    val showDoubleZero: Boolean = false,
    val showTripleZero: Boolean = false,
    val showDecimal: Boolean = true,
    val showPercent: Boolean = true,
    val showScientificToggle: Boolean = true,
    val keypadArrangement: KeypadArrangement = KeypadArrangement.STANDARD,

    val defaultScientificExpanded: Boolean = false,
    val angleMode: AngleMode = AngleMode.DEG,
    val decimalPrecision: Int = 8,
    val numberFormatMode: NumberFormatMode = NumberFormatMode.INTERNATIONAL,
    val clipboardDetectionEnabled: Boolean = true,

    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.7f,
    val soundStyle: ClickSoundStyle = ClickSoundStyle.SOFT_CLICK,

    val hapticEnabled: Boolean = true,
    val hapticStrength: HapticStrength = HapticStrength.MEDIUM,

    val selectedIcon: AppIconOption = AppIconOption.QUADRANT,
    val calculatorMemory: Double = 0.0
) {
    val activeAccentArgb: Long
        get() = if (accentPreset == AccentPreset.CUSTOM) customAccentArgb else accentPreset.argbHex

    fun toJsonString(): String {
        val json = JSONObject()
        json.put("themeMode", themeMode.name)
        json.put("accentPreset", accentPreset.name)
        json.put("customAccentArgb", customAccentArgb)
        json.put("displayFont", displayFont.name)
        json.put("buttonFont", buttonFont.name)
        json.put("interfaceFont", interfaceFont.name)
        json.put("fontScale", fontScale.toDouble())
        json.put("buttonShape", buttonShape.name)
        json.put("buttonSizeScale", buttonSizeScale.toDouble())
        json.put("buttonSpacingDp", buttonSpacingDp.toDouble())
        json.put("buttonOpacity", buttonOpacity.toDouble())
        json.put("buttonBgArgb", buttonBgArgb)
        json.put("buttonTextArgb", buttonTextArgb)
        json.put("useCustomButtonColors", useCustomButtonColors)
        json.put("showDoubleZero", showDoubleZero)
        json.put("showTripleZero", showTripleZero)
        json.put("showDecimal", showDecimal)
        json.put("showPercent", showPercent)
        json.put("showScientificToggle", showScientificToggle)
        json.put("keypadArrangement", keypadArrangement.name)
        json.put("defaultScientificExpanded", defaultScientificExpanded)
        json.put("angleMode", angleMode.name)
        json.put("decimalPrecision", decimalPrecision)
        json.put("numberFormatMode", numberFormatMode.name)
        json.put("clipboardDetectionEnabled", clipboardDetectionEnabled)
        json.put("soundEnabled", soundEnabled)
        json.put("soundVolume", soundVolume.toDouble())
        json.put("soundStyle", soundStyle.name)
        json.put("hapticEnabled", hapticEnabled)
        json.put("hapticStrength", hapticStrength.name)
        json.put("selectedIcon", selectedIcon.name)
        return json.toString(2)
    }

    companion object {
        fun fromJsonString(raw: String): Result<CucalcusSettings> {
            return try {
                val j = JSONObject(raw)
                val defaults = CucalcusSettings()
                Result.success(
                    CucalcusSettings(
                        themeMode = enumOrDefault(j.optString("themeMode"), defaults.themeMode),
                        accentPreset = enumOrDefault(j.optString("accentPreset"), defaults.accentPreset),
                        customAccentArgb = j.optLong("customAccentArgb", defaults.customAccentArgb),
                        displayFont = enumOrDefault(j.optString("displayFont"), defaults.displayFont),
                        buttonFont = enumOrDefault(j.optString("buttonFont"), defaults.buttonFont),
                        interfaceFont = enumOrDefault(j.optString("interfaceFont"), defaults.interfaceFont),
                        fontScale = j.optDouble("fontScale", defaults.fontScale.toDouble()).toFloat().coerceIn(0.85f, 1.25f),
                        buttonShape = enumOrDefault(j.optString("buttonShape"), defaults.buttonShape),
                        buttonSizeScale = j.optDouble("buttonSizeScale", defaults.buttonSizeScale.toDouble()).toFloat().coerceIn(0.85f, 1.15f),
                        buttonSpacingDp = j.optDouble("buttonSpacingDp", defaults.buttonSpacingDp.toDouble()).toFloat().coerceIn(2f, 12f),
                        buttonOpacity = j.optDouble("buttonOpacity", defaults.buttonOpacity.toDouble()).toFloat().coerceIn(0.4f, 1.0f),
                        buttonBgArgb = j.optLong("buttonBgArgb", defaults.buttonBgArgb),
                        buttonTextArgb = j.optLong("buttonTextArgb", defaults.buttonTextArgb),
                        useCustomButtonColors = j.optBoolean("useCustomButtonColors", defaults.useCustomButtonColors),
                        showDoubleZero = j.optBoolean("showDoubleZero", defaults.showDoubleZero),
                        showTripleZero = j.optBoolean("showTripleZero", defaults.showTripleZero),
                        showDecimal = j.optBoolean("showDecimal", defaults.showDecimal),
                        showPercent = j.optBoolean("showPercent", defaults.showPercent),
                        showScientificToggle = j.optBoolean("showScientificToggle", defaults.showScientificToggle),
                        keypadArrangement = enumOrDefault(j.optString("keypadArrangement"), defaults.keypadArrangement),
                        defaultScientificExpanded = j.optBoolean("defaultScientificExpanded", defaults.defaultScientificExpanded),
                        angleMode = enumOrDefault(j.optString("angleMode"), defaults.angleMode),
                        decimalPrecision = j.optInt("decimalPrecision", defaults.decimalPrecision).coerceIn(0, 12),
                        numberFormatMode = enumOrDefault(j.optString("numberFormatMode"), defaults.numberFormatMode),
                        clipboardDetectionEnabled = j.optBoolean("clipboardDetectionEnabled", defaults.clipboardDetectionEnabled),
                        soundEnabled = j.optBoolean("soundEnabled", defaults.soundEnabled),
                        soundVolume = j.optDouble("soundVolume", defaults.soundVolume.toDouble()).toFloat().coerceIn(0f, 1f),
                        soundStyle = enumOrDefault(j.optString("soundStyle"), defaults.soundStyle),
                        hapticEnabled = j.optBoolean("hapticEnabled", defaults.hapticEnabled),
                        hapticStrength = enumOrDefault(j.optString("hapticStrength"), defaults.hapticStrength),
                        selectedIcon = enumOrDefault(j.optString("selectedIcon"), defaults.selectedIcon)
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T {
            return enumValues<T>().firstOrNull { it.name == name } ?: default
        }
    }
}
