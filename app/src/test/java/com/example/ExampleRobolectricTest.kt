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
        assertEquals(8, AppIconOption.entries.size)
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
            selectedIcon = AppIconOption.KKR
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
}
