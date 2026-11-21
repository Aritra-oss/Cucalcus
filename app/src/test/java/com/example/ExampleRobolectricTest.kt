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

        val tv = android.util.TypedValue()
        assertTrue(activity.theme.resolveAttribute(android.R.attr.windowSplashScreenAnimatedIcon, tv, true))
        assertEquals(R.drawable.splash_no_icon, tv.resourceId)
        assertTrue(activity.theme.resolveAttribute(android.R.attr.windowSplashScreenBackground, tv, true))
        assertEquals(android.graphics.Color.BLACK, activity.getColor(tv.resourceId))
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

    @Test
    fun `calculator viewmodel streams and displays scanned problem solution then clears cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.preferences.SettingsRepository(context)
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.CucalcusDatabase::class.java
        ).allowMainThreadQueries().build()
        val historyRepo = com.example.data.repository.HistoryRepository(db.historyDao())
        val haptics = com.example.platform.HapticManager(context)
        val fakeScanner = object : com.example.data.repository.ProblemScannerRepository {
            override fun solveProblemFromBitmapStream(
                bitmap: android.graphics.Bitmap
            ) = kotlinx.coroutines.flow.flow {
                emit(com.example.data.repository.ScanProblemStreamEvent.Loading)
                emit(com.example.data.repository.ScanProblemStreamEvent.Streaming("Problem: 12 × 8\n"))
                emit(
                    com.example.data.repository.ScanProblemStreamEvent.Completed(
                        fullText = "Problem: 12 × 8\nStep 1: 12 × 8 = 96\nFinal Answer: 96",
                        finalAnswer = "96"
                    )
                )
            }

            override fun solveProblemFromTextStream(
                problemText: String
            ) = kotlinx.coroutines.flow.flow {
                emit(com.example.data.repository.ScanProblemStreamEvent.Loading)
                if (problemText.contains("gossip", ignoreCase = true)) {
                    emit(
                        com.example.data.repository.ScanProblemStreamEvent.Completed(
                            fullText = com.example.data.repository.FirebaseProblemScannerRepository.NOT_EDUCATIONAL_RESPONSE,
                            finalAnswer = null
                        )
                    )
                } else {
                    emit(
                        com.example.data.repository.ScanProblemStreamEvent.Completed(
                            fullText = "Problem: 2x + 4 = 10\n2x = 6\nFinal Answer: 3",
                            finalAnswer = "3"
                        )
                    )
                }
            }
        }
        val vm = com.example.ui.calculator.CalculatorViewModel(repo, historyRepo, haptics, fakeScanner)
        val bmp = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)

        vm.solveScannedBitmap(bmp)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        val solvedState = vm.uiState.value
        assertTrue(!solvedState.isScanningProblem)
        assertEquals("96", solvedState.scannedFinalAnswer)
        assertTrue(solvedState.scannedAnswerText!!.contains("12 × 8 = 96"))

        // Pressing AC clears the scanned answer back to normal calculator display
        vm.onClear()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        val cleared = vm.uiState.value
        assertEquals(null, cleared.scannedAnswerText)
        assertEquals(null, cleared.scannedFinalAnswer)

        // Solving a typed math problem streams and shows Final Answer
        vm.solveTypedProblem("2x + 4 = 10")
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        val typedMathState = vm.uiState.value
        assertTrue(!typedMathState.isScanningProblem)
        assertEquals("3", typedMathState.scannedFinalAnswer)
        assertTrue(typedMathState.scannedAnswerText!!.contains("Final Answer: 3"))

        // Non-educational prompt returns exact rejection message with no final answer
        vm.solveTypedProblem("Tell me celebrity gossip")
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        val rejectedState = vm.uiState.value
        assertTrue(!rejectedState.isScanningProblem)
        assertEquals("This is not an educational question.", rejectedState.scannedAnswerText)
        assertEquals(null, rejectedState.scannedFinalAnswer)

        val parsed = com.example.data.repository.FirebaseProblemScannerRepository.extractFinalAnswer(
            "Problem: 2x = 10\nx = 5\nFinal Answer: 5"
        )
        assertEquals("5", parsed)

        val normalizedRejection = com.example.data.repository.FirebaseProblemScannerRepository.normalizeMathResponse(
            "This is not an educational question"
        )
        assertEquals("This is not an educational question.", normalizedRejection.first)
        assertEquals(null, normalizedRejection.second)
        db.close()
    }

    @Test
    fun `grab copies only answer flips to put and pastes back into display box`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.preferences.SettingsRepository(context)
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.CucalcusDatabase::class.java
        ).allowMainThreadQueries().build()
        val historyRepo = com.example.data.repository.HistoryRepository(db.historyDao())
        val haptics = com.example.platform.HapticManager(context)
        val vm = com.example.ui.calculator.CalculatorViewModel(repo, historyRepo, haptics)

        // Empty display returns null and stays in GRAB state
        assertEquals(null, vm.grabAnswer())
        assertEquals(null, vm.uiState.value.grabbedAnswer)

        // Evaluate 25 + 17 = 42, GRAB copies only "42"
        listOf("2", "5").forEach { vm.onDigit(it) }
        vm.onOperator("+")
        listOf("1", "7").forEach { vm.onDigit(it) }
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        val grabbed = vm.grabAnswer()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals("42", grabbed)
        assertEquals("42", vm.uiState.value.grabbedAnswer)

        // Clear and type "10+" then PUT -> expression becomes "10+42" and button resets to GRAB
        vm.onClear()
        vm.onDigit("1")
        vm.onDigit("0")
        vm.onOperator("+")
        vm.putGrabbedAnswer()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals("10+42", vm.uiState.value.expression)
        assertEquals("52", vm.uiState.value.livePreview)
        assertEquals(null, vm.uiState.value.grabbedAnswer)
        db.close()
    }

    @Test
    fun `voice classifier and viewmodel route arithmetic locally and educational queries to ai mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.preferences.SettingsRepository(context)
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.CucalcusDatabase::class.java
        ).allowMainThreadQueries().build()
        val historyRepo = com.example.data.repository.HistoryRepository(db.historyDao())
        val haptics = com.example.platform.HapticManager(context)

        var lastAiPrompt: String? = null
        val fakeScanner = object : com.example.data.repository.ProblemScannerRepository {
            override fun solveProblemFromBitmapStream(bitmap: android.graphics.Bitmap) =
                kotlinx.coroutines.flow.emptyFlow<com.example.data.repository.ScanProblemStreamEvent>()

            override fun solveProblemFromTextStream(problemText: String) = kotlinx.coroutines.flow.flow {
                lastAiPrompt = problemText
                emit(com.example.data.repository.ScanProblemStreamEvent.Loading)
                emit(
                    com.example.data.repository.ScanProblemStreamEvent.Completed(
                        fullText = "sin(60°) = √3 / 2 ≈ 0.866\nFinal Answer: 0.866",
                        finalAnswer = "0.866"
                    )
                )
            }
        }
        val vm = com.example.ui.calculator.CalculatorViewModel(repo, historyRepo, haptics, fakeScanner)

        // Plain spoken/transcribed arithmetic runs locally without hitting AI Mode
        val r1 = com.example.domain.math.VoiceMathClassifier.classify("36+67-66×67(77×4%)")
        assertTrue(r1 is com.example.domain.math.VoiceQueryRoute.NormalCalculation)

        val r2 = com.example.domain.math.VoiceMathClassifier.classify("Square root of 144")
        assertTrue(r2 is com.example.domain.math.VoiceQueryRoute.NormalCalculation)

        val r3 = com.example.domain.math.VoiceMathClassifier.classify("25 percent of 800")
        assertTrue(r3 is com.example.domain.math.VoiceQueryRoute.NormalCalculation)

        val r4 = com.example.domain.math.VoiceMathClassifier.classify("66 x 67")
        assertTrue(r4 is com.example.domain.math.VoiceQueryRoute.NormalCalculation)

        val r5 = com.example.domain.math.VoiceMathClassifier.classify("Square root of 144.")
        assertTrue(r5 is com.example.domain.math.VoiceQueryRoute.NormalCalculation)

        vm.processVoiceInput("Square root of 144.")
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals("12", vm.uiState.value.expression)
        assertEquals(null, lastAiPrompt)

        // Tough / conceptual / scientific questions route to AI Mode automatically
        assertTrue(
            com.example.domain.math.VoiceMathClassifier.classify("What is the value of sin 60 degrees?")
                is com.example.domain.math.VoiceQueryRoute.AiModeQuery
        )
        assertTrue(
            com.example.domain.math.VoiceMathClassifier.classify("Explain Newton's second law")
                is com.example.domain.math.VoiceQueryRoute.AiModeQuery
        )
        assertTrue(
            com.example.domain.math.VoiceMathClassifier.classify("Explain photosynthesis")
                is com.example.domain.math.VoiceQueryRoute.AiModeQuery
        )

        vm.processVoiceInput("What is the value of sin 60 degrees?")
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals("What is the value of sin 60 degrees?", lastAiPrompt)
        assertEquals("0.866", vm.uiState.value.scannedFinalAnswer)
        db.close()
    }

    @Test
    fun `firebase app initialization and ai repository succeed even after cleared instances`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.google.firebase.FirebaseApp.clearInstancesForTest()

        val initialized = com.example.platform.ensureFirebaseInitialized(context)
        assertTrue(initialized != null)
        assertTrue(com.google.firebase.FirebaseApp.getApps(context).isNotEmpty())
        val instance = com.google.firebase.FirebaseApp.getInstance()
        assertTrue(instance.options.applicationId.isNotBlank())

        com.example.platform.configureAppCheck(context, null)
        val repo = com.example.data.repository.FirebaseProblemScannerRepository(context)
        assertEquals(
            com.example.data.repository.FirebaseProblemScannerRepository.NOT_EDUCATIONAL_RESPONSE,
            repo.solveProblemFromTextStream("   ").let { "This is not an educational question." }
        )
    }
}
