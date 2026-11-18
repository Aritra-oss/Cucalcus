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

enum class AppThemeMode(
    val label: String,
    val swatchArgb: Long,
    val isDark: Boolean
) {
    OBSIDIAN_BLACK("Obsidian Black", 0xFF080808L, true),
    GRAPHITE_DARK("Graphite Charcoal", 0xFF14161BL, true),
    SWISS_PAPER_LIGHT("Swiss Paper", 0xFFF6F3ECL, false),
    MIDNIGHT_NAVY("Midnight Navy", 0xFF0A111FL, true),
    DEEP_EMERALD("Deep Emerald", 0xFF081A14L, true),
    BURGUNDY_NOIR("Burgundy Noir", 0xFF1A0A10L, true),
    ROYAL_AMETHYST("Royal Amethyst", 0xFF130C22L, true),
    NORDIC_SLATE("Nordic Slate", 0xFF101924L, true),
    ESPRESSO_ROAST("Espresso Roast", 0xFF18110DL, true),
    FOREST_MOSS("Forest Moss", 0xFF0E190FL, true),
    CARBON_INDIGO("Carbon Indigo", 0xFF0B0E21L, true),
    COPPER_CHARCOAL("Copper Ash", 0xFF19120FL, true),
    TEAL_ABYSS("Teal Abyss", 0xFF081B1EL, true),
    PLUM_VELVET("Plum Velvet", 0xFF1C0E1AL, true),
    OLIVE_SLATE("Olive Slate", 0xFF13170EL, true),
    CRIMSON_SHADOW("Crimson Shadow", 0xFF1D0B0CL, true),
    WARM_SAND_LIGHT("Warm Sand", 0xFFEFE6D5L, false),
    ICE_MIST_LIGHT("Ice Mist", 0xFFEBF2F7L, false),
    SAGE_PAPER_LIGHT("Sage Paper", 0xFFE8F0E9L, false),
    ROSE_QUARTZ_LIGHT("Rose Quartz", 0xFFF5ECEEL, false)
}

enum class AppBackgroundPreset(
    val label: String,
    val argbHex: Long,
    val isDark: Boolean
) {
    THEME_DEFAULT("Match Theme", 0x00000000L, true),
    OBSIDIAN("Obsidian", 0xFF080808L, true),
    CHARCOAL("Charcoal", 0xFF141518L, true),
    ONYX_BLUE("Onyx Blue", 0xFF0B111DL, true),
    MIDNIGHT("Midnight", 0xFF0D1729L, true),
    FOREST("Forest", 0xFF0A160EL, true),
    EMERALD("Emerald", 0xFF071B16L, true),
    TEAL("Deep Teal", 0xFF081D22L, true),
    BURGUNDY("Burgundy", 0xFF1C0910L, true),
    CRIMSON("Crimson", 0xFF210A0CL, true),
    PLUM("Plum", 0xFF1C0C1BL, true),
    AMETHYST("Amethyst", 0xFF140C24L, true),
    INDIGO("Indigo", 0xFF0D1026L, true),
    ESPRESSO("Espresso", 0xFF1A120DL, true),
    BRONZE("Bronze", 0xFF1E160EL, true),
    OLIVE("Olive", 0xFF15180DL, true),
    SLATE("Steel Slate", 0xFF161D26L, true),
    IVORY("Swiss Ivory", 0xFFF6F3ECL, false),
    SAND("Warm Sand", 0xFFEFE5D2L, false),
    ICE("Ice Mist", 0xFFEAF1F7L, false),
    SAGE("Soft Sage", 0xFFE7EFE8L, false);

    companion object {
        val colorChoices: List<AppBackgroundPreset>
            get() = entries.filter { it != THEME_DEFAULT }
    }
}

enum class FontOption(val label: String) {
    SANS_SERIF("Sans-serif (Inter)"),
    SERIF("Serif (Lora)"),
    MONOSPACE("Monospace (JetBrains)"),
    GEOMETRIC("Geometric (Space Grotesk)"),
    ELEGANT_DISPLAY("Display (Playfair)")
}

enum class ButtonShapeOption(val label: String, val defaultRadiusPercent: Int? = null) {
    SQUARE("Soft Square", 10),
    SLIGHTLY_ROUNDED("Rounded Tile", 18),
    ROUNDED("Smooth", 32),
    SQUIRCLE("Squircle", 42),
    CIRCULAR("Full Circle", 50),
    CHAMFERED("Soft Arch"),
    DIAGONAL_CUT("Swiss Curve"),
    LEAF("Leaf"),
    CUSTOM_RADIUS("Custom Radius")
}

enum class KeypadArrangement(val label: String) {
    STANDARD("Standard (Operators Right)"),
    LEFT_HANDED("Left-Handed (Operators Left)")
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
        subtitle = "Monochrome Glass Grid",
        category = IconCategory.MATHEMATICAL,
        aliasClassName = "com.example.LauncherAliasQuadrant"
    ),
    INTERSECTION(
        id = "intersection",
        displayName = "Orbit Silver",
        subtitle = "Platinum Swirl & Glass",
        category = IconCategory.MATHEMATICAL,
        aliasClassName = "com.example.LauncherAliasIntersection"
    ),
    NEGATIVE_SPACE(
        id = "negative_space",
        displayName = "Prism Grid",
        subtitle = "Blue, Violet & Amber",
        category = IconCategory.MATHEMATICAL,
        aliasClassName = "com.example.LauncherAliasNegativeSpace"
    ),
    PRECISION(
        id = "precision",
        displayName = "Emerald Crest",
        subtitle = "Emerald & Cream Orbit",
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
        subtitle = "Crimson & Gold Lion",
        category = IconCategory.IPL_INSPIRED,
        aliasClassName = "com.example.LauncherAliasRcb"
    ),
    MI(
        id = "mi",
        displayName = "MI Edition",
        subtitle = "Royal Blue & Gold",
        category = IconCategory.IPL_INSPIRED,
        aliasClassName = "com.example.LauncherAliasMi"
    ),
    CSK(
        id = "csk",
        displayName = "CSK Edition",
        subtitle = "Gold & Blue Lion",
        category = IconCategory.IPL_INSPIRED,
        aliasClassName = "com.example.LauncherAliasCsk"
    )
}

data class CucalcusSettings(
    val themeMode: AppThemeMode = AppThemeMode.OBSIDIAN_BLACK,
    val backgroundPreset: AppBackgroundPreset = AppBackgroundPreset.THEME_DEFAULT,
    val backgroundImagePath: String? = null,
    val accentPreset: AccentPreset = AccentPreset.CHAMPAGNE_GOLD,
    val customAccentArgb: Long = 0xFFD6B56DL,

    val displayFont: FontOption = FontOption.GEOMETRIC,
    val buttonFont: FontOption = FontOption.SANS_SERIF,
    val interfaceFont: FontOption = FontOption.SANS_SERIF,
    val fontScale: Float = 1.0f,

    val buttonShape: ButtonShapeOption = ButtonShapeOption.SLIGHTLY_ROUNDED,
    val buttonCornerRadiusPercent: Int = 18, // 8..50 always softly rounded
    val borderlessButtons: Boolean = false,
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

    val hapticEnabled: Boolean = true,
    val hapticStrength: HapticStrength = HapticStrength.MEDIUM,

    val selectedIcon: AppIconOption = AppIconOption.QUADRANT,
    val customIconImagePath: String? = null,
    val calculatorMemory: Double = 0.0
) {
    val activeAccentArgb: Long
        get() = if (accentPreset == AccentPreset.CUSTOM) customAccentArgb else accentPreset.argbHex

    fun toJsonString(): String {
        val json = JSONObject()
        json.put("themeMode", themeMode.name)
        json.put("backgroundPreset", backgroundPreset.name)
        json.put("accentPreset", accentPreset.name)
        json.put("customAccentArgb", customAccentArgb)
        json.put("displayFont", displayFont.name)
        json.put("buttonFont", buttonFont.name)
        json.put("interfaceFont", interfaceFont.name)
        json.put("fontScale", fontScale.toDouble())
        json.put("buttonShape", buttonShape.name)
        json.put("buttonCornerRadiusPercent", buttonCornerRadiusPercent)
        json.put("borderlessButtons", borderlessButtons)
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
                        backgroundPreset = enumOrDefault(j.optString("backgroundPreset"), defaults.backgroundPreset),
                        accentPreset = enumOrDefault(j.optString("accentPreset"), defaults.accentPreset),
                        customAccentArgb = j.optLong("customAccentArgb", defaults.customAccentArgb),
                        displayFont = enumOrDefault(j.optString("displayFont"), defaults.displayFont),
                        buttonFont = enumOrDefault(j.optString("buttonFont"), defaults.buttonFont),
                        interfaceFont = enumOrDefault(j.optString("interfaceFont"), defaults.interfaceFont),
                        fontScale = j.optDouble("fontScale", defaults.fontScale.toDouble()).toFloat().coerceIn(0.85f, 1.25f),
                        buttonShape = enumOrDefault(j.optString("buttonShape"), defaults.buttonShape),
                        buttonCornerRadiusPercent = j.optInt("buttonCornerRadiusPercent", defaults.buttonCornerRadiusPercent).coerceIn(8, 50),
                        borderlessButtons = j.optBoolean("borderlessButtons", defaults.borderlessButtons),
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
