package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.AccentPreset
import com.example.data.preferences.AppBackgroundPreset
import com.example.data.preferences.AppIconOption
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.ButtonShapeOption
import com.example.data.preferences.CucalcusSettings
import com.example.data.preferences.FontOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name and counts from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Cucalcus", appName)
        assertEquals(15, AppIconOption.entries.size)
        assertEquals(20, AppThemeMode.entries.size)
        assertEquals(20, AppBackgroundPreset.colorChoices.size)
    }

    @Test
    fun `settings json backup roundtrip preserves personalisation`() {
        val original = CucalcusSettings(
            themeMode = AppThemeMode.MIDNIGHT_NAVY,
            backgroundPreset = AppBackgroundPreset.EMERALD,
            accentPreset = AccentPreset.ROYAL_PURPLE,
            displayFont = FontOption.ELEGANT_DISPLAY,
            buttonShape = ButtonShapeOption.CHAMFERED,
            buttonCornerRadiusPercent = 38,
            borderlessButtons = true,
            showDoubleZero = true,
            showTripleZero = true,
            decimalPrecision = 6,
            selectedIcon = AppIconOption.IPL_KKR
        )
        val json = original.toJsonString()
        val restored = CucalcusSettings.fromJsonString(json).getOrThrow()
        assertEquals(original.themeMode, restored.themeMode)
        assertEquals(original.backgroundPreset, restored.backgroundPreset)
        assertEquals(original.accentPreset, restored.accentPreset)
        assertEquals(original.displayFont, restored.displayFont)
        assertEquals(original.buttonShape, restored.buttonShape)
        assertEquals(original.buttonCornerRadiusPercent, restored.buttonCornerRadiusPercent)
        assertEquals(original.borderlessButtons, restored.borderlessButtons)
        assertEquals(original.showDoubleZero, restored.showDoubleZero)
        assertEquals(original.showTripleZero, restored.showTripleZero)
        assertEquals(original.decimalPrecision, restored.decimalPrecision)
        assertEquals(original.selectedIcon, restored.selectedIcon)
    }

    @Test
    fun `legacy backup json with removed sound fields imports cleanly`() {
        val legacyJson = """{"themeMode":"OBSIDIAN_BLACK","soundEnabled":true,"soundVolume":0.8,"soundStyle":"SOFT_CLICK","borderlessButtons":true}"""
        val restored = CucalcusSettings.fromJsonString(legacyJson).getOrThrow()
        assertEquals(AppThemeMode.OBSIDIAN_BLACK, restored.themeMode)
        assertTrue(restored.borderlessButtons)
    }

    @Test
    fun `button corner radius always stays rounded above zero`() {
        val zeroRadiusJson = """{"buttonCornerRadiusPercent":0}"""
        val restored = CucalcusSettings.fromJsonString(zeroRadiusJson).getOrThrow()
        assertEquals(8, restored.buttonCornerRadiusPercent)
        assertTrue(
            ButtonShapeOption.entries.all {
                (it.defaultRadiusPercent ?: 8) >= 8
            }
        )
    }

    @Test
    fun `main activity launches cleanly`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertTrue(!activity.isFinishing)
    }

    @Test
    fun `all 15 app icon options match manifest aliases and default is normal green`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val pkg = context.packageName

        assertEquals(AppIconOption.NORMAL_GREEN, CucalcusSettings().selectedIcon)
        assertEquals(15, AppIconOption.entries.map { it.id }.toSet().size)
        assertEquals(15, AppIconOption.entries.map { it.aliasClassName }.toSet().size)

        for (option in AppIconOption.entries) {
            val component = android.content.ComponentName(pkg, option.aliasClassName)
            val info = pm.getActivityInfo(
                component,
                android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS
            )
            assertTrue("Missing alias ${option.aliasClassName}", info.name == option.aliasClassName)
            val drawableId = context.resources.getIdentifier("ic_brand_${option.id}", "drawable", pkg)
            assertTrue("Missing ic_brand_${option.id}", drawableId != 0)
            val adaptiveId = context.resources.getIdentifier("ic_adaptive_${option.id}", "drawable", pkg)
            assertTrue("Missing ic_adaptive_${option.id}", adaptiveId != 0)
        }
    }

    @Test
    fun `saved theme and accent load synchronously on cold reopen without default flash`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo1 = com.example.data.preferences.SettingsRepository(context)
        repo1.updateSettings {
            it.copy(
                themeMode = AppThemeMode.MIDNIGHT_NAVY,
                backgroundPreset = AppBackgroundPreset.EMERALD,
                accentPreset = AccentPreset.OCEAN_BLUE,
                selectedIcon = AppIconOption.IPL_MI
            )
        }

        // Simulate process restart by instantiating a fresh SettingsRepository
        val repo2 = com.example.data.preferences.SettingsRepository(context)
        val loadedSync = repo2.currentSettings
        assertEquals(AppThemeMode.MIDNIGHT_NAVY, loadedSync.themeMode)
        assertEquals(AppBackgroundPreset.EMERALD, loadedSync.backgroundPreset)
        assertEquals(AccentPreset.OCEAN_BLUE, loadedSync.accentPreset)
        assertEquals(AppIconOption.IPL_MI, loadedSync.selectedIcon)
        assertEquals(loadedSync, repo2.settingsState.value)

        // Restore clean defaults so other tests remain isolated
        repo2.resetAllSettings()
    }

    @Test
    fun `calculator display allows mid-expression cursor insertion, backspace, and cursor lock`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.preferences.SettingsRepository(context)
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.CucalcusDatabase::class.java
        ).allowMainThreadQueries().build()
        val historyRepo = com.example.data.repository.HistoryRepository(db.historyDao())
        val haptics = com.example.platform.HapticManager(context)
        val vm = com.example.ui.calculator.CalculatorViewModel(repo, historyRepo, haptics)

        fun state(): com.example.ui.calculator.CalculatorUiState {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            return vm.uiState.value
        }

        // Type "1234"
        listOf("1", "2", "3", "4").forEach { vm.onDigit(it) }
        assertEquals("1234", state().expression)
        assertEquals(4, state().cursorPosition)

        // Move cursor to middle (after "12") and delete '2'
        vm.onCursorPositionChange(2)
        assertEquals(2, state().cursorPosition)
        vm.onBackspace()
        assertEquals("134", state().expression)
        assertEquals(1, state().cursorPosition)

        // Insert '9' in the middle
        vm.onDigit("9")
        assertEquals("1934", state().expression)
        assertEquals(2, state().cursorPosition)

        // Lock cursor via lock toggle -> attempted cursor moves are ignored
        vm.toggleCursorLock()
        assertTrue(state().isCursorLocked)
        vm.onCursorPositionChange(4)
        assertEquals(2, state().cursorPosition)

        // Unlock cursor -> cursor movement works again
        vm.toggleCursorLock()
        assertTrue(!state().isCursorLocked)
        vm.onCursorPositionChange(4)
        assertEquals(4, state().cursorPosition)

        // Multi-token delete in middle: insert "+sin(30)", move cursor after "sin(", backspace deletes entire "sin("
        vm.onOperator("+")
        vm.onScientificFunction("sin")
        vm.onDigit("3")
        vm.onDigit("0")
        vm.onCloseParen()
        assertEquals("1934+sin(30)", state().expression)
        vm.onCursorPositionChange("1934+sin(".length)
        vm.onBackspace()
        assertEquals("1934+30)", state().expression)
        assertEquals("1934+".length, state().cursorPosition)
        db.close()
    }

    @Test
    fun `expression comma visual transformation keeps cursor offset mapping accurate`() {
        val transformation = com.example.ui.calculator.ExpressionFormatTransformation(
            com.example.domain.math.NumberFormatMode.INTERNATIONAL
        )
        val raw = androidx.compose.ui.text.AnnotatedString("1234567+8901")
        val transformed = transformation.filter(raw)
        assertEquals("1,234,567+8,901", transformed.text.text)
        assertEquals(0, transformed.offsetMapping.originalToTransformed(0))
        assertEquals(raw.length, transformed.offsetMapping.transformedToOriginal(transformed.text.length))

        for (i in 0..raw.length) {
            val transIdx = transformed.offsetMapping.originalToTransformed(i)
            assertTrue(transIdx in 0..transformed.text.length)
            val roundTrip = transformed.offsetMapping.transformedToOriginal(transIdx)
            assertEquals(i, roundTrip)
        }
    }
}
