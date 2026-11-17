package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.AccentPreset
import com.example.data.preferences.AppIconOption
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
    fun `read app name and icon count from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Cucalcus", appName)
        assertEquals(8, AppIconOption.entries.size)
    }

    @Test
    fun `settings json backup roundtrip preserves personalisation`() {
        val original = CucalcusSettings(
            accentPreset = AccentPreset.ROYAL_PURPLE,
            displayFont = FontOption.ELEGANT_DISPLAY,
            buttonShape = com.example.data.preferences.ButtonShapeOption.CHAMFERED,
            buttonCornerRadiusPercent = 38,
            showDoubleZero = true,
            showTripleZero = true,
            decimalPrecision = 6,
            selectedIcon = AppIconOption.KKR
        )
        val json = original.toJsonString()
        val restored = CucalcusSettings.fromJsonString(json).getOrThrow()
        assertEquals(original.accentPreset, restored.accentPreset)
        assertEquals(original.displayFont, restored.displayFont)
        assertEquals(original.buttonShape, restored.buttonShape)
        assertEquals(original.buttonCornerRadiusPercent, restored.buttonCornerRadiusPercent)
        assertEquals(original.showDoubleZero, restored.showDoubleZero)
        assertEquals(original.showTripleZero, restored.showTripleZero)
        assertEquals(original.decimalPrecision, restored.decimalPrecision)
        assertEquals(original.selectedIcon, restored.selectedIcon)
    }

    @Test
    fun `main activity launches cleanly`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertTrue(!activity.isFinishing)
    }
}
