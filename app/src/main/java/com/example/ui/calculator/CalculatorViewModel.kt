package com.example.ui.calculator

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.CucalcusSettings
import com.example.data.preferences.SettingsRepository
import com.example.data.repository.FirebaseProblemScannerRepository
import com.example.data.repository.HistoryRepository
import com.example.data.repository.ProblemScannerRepository
import com.example.data.repository.ScanProblemStreamEvent
import com.example.domain.math.AngleMode
import com.example.domain.math.EvalResult
import com.example.domain.math.ExpressionParser
import com.example.domain.math.NumberFormatMode
import com.example.domain.math.NumberFormatter
import com.example.domain.math.VoiceMathClassifier
import com.example.domain.math.VoiceQueryRoute
import com.example.platform.HapticManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalculatorUiState(
    val expression: String = "",
    val cursorPosition: Int = 0,
    val isCursorLocked: Boolean = false,
    val formattedExpression: String = "",
    val livePreview: String = "",
    val errorMessage: String? = null,
    val justEvaluated: Boolean = false,
    val isScientificExpanded: Boolean = false,
    val isInverseSci: Boolean = false,
    val angleMode: AngleMode = AngleMode.DEG,
    val memoryValue: Double = 0.0,
    val clipboardCandidate: String? = null,
    val isScanningProblem: Boolean = false,
    val scannedAnswerText: String? = null,
    val scannedFinalAnswer: String? = null,
    val scanErrorMessage: String? = null,
    val grabbedAnswer: String? = null,
    val aiQuestionsRemaining: Int = 10
)

class CalculatorViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val hapticManager: HapticManager,
    private val problemScannerRepository: ProblemScannerRepository = FirebaseProblemScannerRepository()
) : ViewModel() {

    private var scanJob: Job? = null
    private var activeAiQuestion: String = ""
    private val initialSettings = settingsRepository.currentSettings
    private val _state = MutableStateFlow(
        CalculatorUiState(
            isScientificExpanded = initialSettings.defaultScientificExpanded,
            angleMode = initialSettings.angleMode,
            memoryValue = initialSettings.calculatorMemory,
            aiQuestionsRemaining = settingsRepository.aiQuestionsRemainingToday()
        )
    )
    private var latestSettings = initialSettings

    val settings: StateFlow<CucalcusSettings> = settingsRepository.settingsState

    val uiState: StateFlow<CalculatorUiState> = combine(_state, settings) { st, cfg ->
        latestSettings = cfg
        val formattedExpr = NumberFormatter.formatExpressionNumbers(st.expression, cfg.numberFormatMode)
        st.copy(
            cursorPosition = st.cursorPosition.coerceIn(0, st.expression.length),
            formattedExpression = formattedExpr,
            memoryValue = cfg.calculatorMemory
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        CalculatorUiState(
            isScientificExpanded = initialSettings.defaultScientificExpanded,
            angleMode = initialSettings.angleMode,
            memoryValue = initialSettings.calculatorMemory
        )
    )

    private fun triggerHaptic() {
        val cfg = latestSettings
        hapticManager.performTick(cfg.hapticStrength, cfg.hapticEnabled)
    }

    fun toggleCursorLock() {
        triggerHaptic()
        _state.update { current ->
            current.copy(
                isCursorLocked = !current.isCursorLocked,
                cursorPosition = current.cursorPosition.coerceIn(0, current.expression.length)
            )
        }
    }

    fun onCursorPositionChange(newPosition: Int) {
        _state.update { current ->
            if (current.isCursorLocked) return@update current
            val clamped = newPosition.coerceIn(0, current.expression.length)
            if (clamped == current.cursorPosition && !current.justEvaluated) return@update current
            current.copy(
                cursorPosition = clamped,
                justEvaluated = false
            )
        }
    }

    private fun insertAtCursor(base: String, cursor: Int, token: String): Pair<String, Int> {
        val pos = cursor.coerceIn(0, base.length)
        val next = base.substring(0, pos) + token + base.substring(pos)
        return next to (pos + token.length)
    }

    fun onDigit(digit: String) {
        triggerHaptic()
        scanJob?.cancel()
        _state.update { rawCurrent ->
            val current = rawCurrent.withoutScanOverlay()
            val freshStart = current.justEvaluated
            val base = if (freshStart) "" else current.expression
            val pos = if (freshStart) 0 else current.cursorPosition.coerceIn(0, base.length)

            if (digit == ".") {
                val leftSegment = base.substring(0, pos).takeLastWhile { it.isDigit() || it == '.' }
                val rightSegment = base.substring(pos).takeWhile { it.isDigit() || it == '.' }
                if (leftSegment.contains('.') || rightSegment.contains('.')) {
                    return@update current
                }
                val hasAdjacentDigitBefore = pos > 0 && base[pos - 1].isDigit()
                val hasAdjacentDigitAfter = pos < base.length && base[pos].isDigit()
                val addition = if (!hasAdjacentDigitBefore && !hasAdjacentDigitAfter) "0." else "."
                val (nextExpr, nextCursor) = insertAtCursor(base, pos, addition)
                return@update recalculate(
                    current.copy(
                        expression = nextExpr,
                        cursorPosition = nextCursor,
                        justEvaluated = false,
                        errorMessage = null
                    )
                )
            }

            if ((digit == "0" || digit == "00" || digit == "000") && base == "0" && pos == 1) {
                return@update recalculate(
                    current.copy(
                        expression = "0",
                        cursorPosition = 1,
                        justEvaluated = false,
                        errorMessage = null
                    )
                )
            }

            val prevChar = if (pos > 0) base[pos - 1] else null
            val tokenToInsert = if ((digit == "00" || digit == "000") &&
                (prevChar == null || prevChar in listOf('+', '−', '×', '÷', '(', '^'))
            ) {
                "0"
            } else {
                digit
            }
            val (nextExpr, nextCursor) = insertAtCursor(base, pos, tokenToInsert)
            recalculate(
                current.copy(
                    expression = nextExpr,
                    cursorPosition = nextCursor,
                    justEvaluated = false,
                    errorMessage = null
                )
            )
        }
    }

    fun onOperator(op: String) {
        triggerHaptic()
        _state.update { current ->
            val expr = current.expression
            val pos = current.cursorPosition.coerceIn(0, expr.length)
            var before = expr.substring(0, pos)
            val after = expr.substring(pos)
            val symbol = if (op == "-") "−" else op

            if (before.isEmpty()) {
                return@update if (symbol == "−" && !after.startsWith("−")) {
                    val (nextExpr, nextCursor) = insertAtCursor(expr, pos, "−")
                    recalculate(
                        current.copy(
                            expression = nextExpr,
                            cursorPosition = nextCursor,
                            justEvaluated = false,
                            errorMessage = null
                        )
                    )
                } else {
                    current
                }
            }

            // Allow unary minus right after '(', block binary operators right after '('
            if (before.last() == '(') {
                return@update if (symbol == "−") {
                    val (nextExpr, nextCursor) = insertAtCursor(expr, pos, "−")
                    recalculate(
                        current.copy(
                            expression = nextExpr,
                            cursorPosition = nextCursor,
                            justEvaluated = false,
                            errorMessage = null
                        )
                    )
                } else {
                    current
                }
            }

            // Cannot replace leading unary minus or "(−" with a binary operator
            if (before.endsWith("(−") || before == "−") {
                return@update current
            }

            // Replace operator immediately before the cursor
            val binaryOps = listOf("+", "−", "-", "×", "÷", "^")
            if (before.endsWith("mod")) {
                before = before.removeSuffix("mod")
            } else if (before.last().toString() in binaryOps) {
                before = before.dropLast(1)
                if (before.endsWith("(") && symbol != "−") {
                    return@update current
                }
            }
            val nextExpr = before + symbol + after
            val nextCursor = before.length + symbol.length
            recalculate(
                current.copy(
                    expression = nextExpr,
                    cursorPosition = nextCursor,
                    justEvaluated = false,
                    errorMessage = null
                )
            )
        }
    }

    fun onPercent() {
        triggerHaptic()
        _state.update { current ->
            val pos = current.cursorPosition.coerceIn(0, current.expression.length)
            if (pos == 0) return@update current
            val prev = current.expression[pos - 1]
            if (prev.isDigit() || prev == ')' || prev == 'π' || prev == 'e') {
                val (nextExpr, nextCursor) = insertAtCursor(current.expression, pos, "%")
                recalculate(
                    current.copy(
                        expression = nextExpr,
                        cursorPosition = nextCursor,
                        justEvaluated = false,
                        errorMessage = null
                    )
                )
            } else {
                current
            }
        }
    }

    fun onParenthesis() {
        triggerHaptic()
        _state.update { current ->
            val freshStart = current.justEvaluated
            val expr = if (freshStart) "" else current.expression
            val pos = if (freshStart) 0 else current.cursorPosition.coerceIn(0, expr.length)
            val before = expr.substring(0, pos)
            val open = before.count { it == '(' }
            val close = before.count { it == ')' }
            val last = before.lastOrNull()
            val token = if (open > close && last != null && (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '%' || last == '!')) {
                ")"
            } else {
                "("
            }
            val (nextExpr, nextCursor) = insertAtCursor(expr, pos, token)
            recalculate(
                current.copy(
                    expression = nextExpr,
                    cursorPosition = nextCursor,
                    justEvaluated = false,
                    errorMessage = null
                )
            )
        }
    }

    fun onOpenParen() {
        triggerHaptic()
        _state.update { current ->
            val freshStart = current.justEvaluated
            val expr = if (freshStart) "" else current.expression
            val pos = if (freshStart) 0 else current.cursorPosition.coerceIn(0, expr.length)
            val (nextExpr, nextCursor) = insertAtCursor(expr, pos, "(")
            recalculate(
                current.copy(
                    expression = nextExpr,
                    cursorPosition = nextCursor,
                    justEvaluated = false,
                    errorMessage = null
                )
            )
        }
    }

    fun onCloseParen() {
        triggerHaptic()
        _state.update { current ->
            val pos = current.cursorPosition.coerceIn(0, current.expression.length)
            val before = current.expression.substring(0, pos)
            val open = before.count { it == '(' }
            val close = before.count { it == ')' }
            val last = before.lastOrNull()
            if (open > close && last != null && last != '(' && last !in listOf('+', '−', '×', '÷', '^')) {
                val (nextExpr, nextCursor) = insertAtCursor(current.expression, pos, ")")
                recalculate(
                    current.copy(
                        expression = nextExpr,
                        cursorPosition = nextCursor,
                        justEvaluated = false,
                        errorMessage = null
                    )
                )
            } else {
                current
            }
        }
    }

    fun onToggleSign() {
        triggerHaptic()
        _state.update { current ->
            val expr = current.expression
            if (expr.isEmpty()) {
                return@update current.copy(
                    expression = "−",
                    cursorPosition = 1,
                    justEvaluated = false,
                    errorMessage = null
                )
            }
            if (expr == "−") {
                return@update current.copy(
                    expression = "",
                    cursorPosition = 0,
                    livePreview = "",
                    justEvaluated = false,
                    errorMessage = null
                )
            }
            val simpleLiteral = Regex("""^\d+(\.\d+)?([eE][+−-]?\d+)?$""")
            val isOuterNegatedGroup = expr.startsWith("−(") && expr.endsWith(")") && run {
                var depth = 0
                var closedEarly = false
                for (i in 1 until expr.lastIndex) {
                    when (expr[i]) {
                        '(' -> depth++
                        ')' -> {
                            depth--
                            if (depth == 0) {
                                closedEarly = true
                                break
                            }
                        }
                    }
                }
                !closedEarly && depth == 1
            }

            val next = when {
                expr.startsWith("−") && simpleLiteral.matches(expr.drop(1)) -> expr.drop(1)
                simpleLiteral.matches(expr) -> "−$expr"
                isOuterNegatedGroup -> expr.removePrefix("−(").dropLast(1)
                else -> "−($expr)"
            }
            recalculate(
                current.copy(
                    expression = next,
                    cursorPosition = next.length,
                    justEvaluated = false,
                    errorMessage = null
                )
            )
        }
    }

    fun onScientificFunction(fn: String) {
        triggerHaptic()
        _state.update { current ->
            val expr = current.expression
            val freshStart = current.justEvaluated
            val base = if (freshStart) "" else expr
            val pos = if (freshStart) 0 else current.cursorPosition.coerceIn(0, expr.length)

            val prevChar = if (pos > 0) expr[pos - 1] else null
            val cursorFollowsValue = prevChar != null && (prevChar.isDigit() || prevChar in listOf(')', 'π', 'e', '%', '!'))

            if (fn == "x²") {
                return@update if (pos == expr.length && expr.isNotEmpty() && cursorFollowsValue) {
                    val next = "($expr)^2"
                    recalculate(current.copy(expression = next, cursorPosition = next.length, justEvaluated = false, errorMessage = null))
                } else if (cursorFollowsValue) {
                    val (next, nextCursor) = insertAtCursor(expr, pos, "^2")
                    recalculate(current.copy(expression = next, cursorPosition = nextCursor, justEvaluated = false, errorMessage = null))
                } else {
                    current
                }
            }
            if (fn == "x!") {
                return@update if (cursorFollowsValue) {
                    val (next, nextCursor) = insertAtCursor(expr, pos, "!")
                    recalculate(current.copy(expression = next, cursorPosition = nextCursor, justEvaluated = false, errorMessage = null))
                } else {
                    current
                }
            }
            if (fn == "1/x") {
                return@update if (pos == expr.length && expr.isNotEmpty() && cursorFollowsValue) {
                    val next = "1÷($expr)"
                    recalculate(current.copy(expression = next, cursorPosition = next.length, justEvaluated = false, errorMessage = null))
                } else {
                    val (next, nextCursor) = insertAtCursor(base, pos, "1÷(")
                    recalculate(current.copy(expression = next, cursorPosition = nextCursor, justEvaluated = false, errorMessage = null))
                }
            }

            val token = when (fn) {
                "√" -> "√("
                "|x|" -> "abs("
                "eˣ" -> "exp("
                "10ˣ" -> "10^("
                "π" -> "π"
                "e" -> "e"
                else -> "$fn("
            }
            val (next, nextCursor) = insertAtCursor(base, pos, token)
            recalculate(
                current.copy(
                    expression = next,
                    cursorPosition = nextCursor,
                    justEvaluated = false,
                    errorMessage = null
                )
            )
        }
    }

    fun onBackspace() {
        triggerHaptic()
        _state.update { current ->
            if (current.expression.isEmpty()) {
                return@update current.copy(cursorPosition = 0, errorMessage = null, livePreview = "")
            }
            val expr = current.expression
            val pos = current.cursorPosition.coerceIn(0, expr.length)
            if (pos == 0) {
                return@update current.copy(errorMessage = null)
            }
            val before = expr.substring(0, pos)
            val after = expr.substring(pos)
            val multiTokens = listOf("sin⁻¹(", "cos⁻¹(", "tan⁻¹(", "sqrt(", "asin(", "acos(", "atan(", "sin(", "cos(", "tan(", "log(", "abs(", "exp(", "ln(", "mod", "√(")
            val matched = multiTokens.firstOrNull { before.endsWith(it) }
            val delLen = matched?.length ?: 1
            val next = before.dropLast(delLen) + after
            val nextCursor = (pos - delLen).coerceAtLeast(0)
            recalculate(
                current.copy(
                    expression = next,
                    cursorPosition = nextCursor,
                    justEvaluated = false,
                    errorMessage = null
                )
            )
        }
    }

    fun onClear() {
        triggerHaptic()
        scanJob?.cancel()
        _state.update {
            it.copy(
                expression = "",
                cursorPosition = 0,
                livePreview = "",
                errorMessage = null,
                justEvaluated = false,
                isScanningProblem = false,
                scannedAnswerText = null,
                scannedFinalAnswer = null,
                scanErrorMessage = null
            )
        }
    }

    fun onEquals() {
        triggerHaptic()
        val current = _state.value
        val raw = current.expression.trim()
        if (raw.isEmpty()) return

        val openCount = raw.count { it == '(' }
        val closeCount = raw.count { it == ')' }
        val balanced = if (openCount > closeCount) raw + ")".repeat(openCount - closeCount) else raw

        when (val result = ExpressionParser.evaluate(balanced, current.angleMode)) {
            is EvalResult.Success -> {
                val formatted = NumberFormatter.formatResult(
                    value = result.value,
                    decimalPrecision = latestSettings.decimalPrecision,
                    formatMode = latestSettings.numberFormatMode
                )
                viewModelScope.launch {
                    historyRepository.saveCalculation(
                        expression = NumberFormatter.formatExpressionNumbers(balanced, NumberFormatMode.INTERNATIONAL),
                        formattedResult = formatted,
                        rawResult = result.value,
                        angleMode = current.angleMode.name
                    )
                }
                val editableNumber = NumberFormatter.toEditableExpressionNumber(
                    value = result.value,
                    decimalPrecision = latestSettings.decimalPrecision
                )

                _state.update {
                    it.copy(
                        expression = editableNumber,
                        cursorPosition = editableNumber.length,
                        livePreview = "",
                        errorMessage = null,
                        justEvaluated = true
                    )
                }
            }
            is EvalResult.Error -> {
                _state.update {
                    it.copy(
                        errorMessage = result.message,
                        livePreview = ""
                    )
                }
            }
        }
    }

    fun toggleScientific() {
        triggerHaptic()
        _state.update { it.copy(isScientificExpanded = !it.isScientificExpanded) }
    }

    fun toggleInverseSci() {
        triggerHaptic()
        _state.update { it.copy(isInverseSci = !it.isInverseSci) }
    }

    fun toggleAngleMode() {
        triggerHaptic()
        _state.update { current ->
            val next = if (current.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
            recalculate(current.copy(angleMode = next))
        }
    }

    // Memory functions
    fun onMemoryClear() {
        triggerHaptic()
        viewModelScope.launch { settingsRepository.updateMemory(0.0) }
    }

    fun onMemoryRecall() {
        triggerHaptic()
        val mem = latestSettings.calculatorMemory
        val formatted = NumberFormatter.toEditableExpressionNumber(mem, latestSettings.decimalPrecision)
        _state.update { current ->
            val freshStart = current.justEvaluated
            val base = if (freshStart) "" else current.expression
            val pos = if (freshStart) 0 else current.cursorPosition.coerceIn(0, base.length)
            val prev = if (pos > 0) base[pos - 1] else null
            val token = if (prev != null && (prev.isDigit() || prev == '.')) "×$formatted" else formatted
            val (nextExpr, nextCursor) = insertAtCursor(base, pos, token)
            recalculate(
                current.copy(
                    expression = nextExpr,
                    cursorPosition = nextCursor,
                    justEvaluated = false,
                    errorMessage = null
                )
            )
        }
    }

    fun onMemoryAdd() {
        triggerHaptic()
        val value = currentEvaluatedValue() ?: return
        viewModelScope.launch {
            settingsRepository.addToMemory(value)
        }
    }

    fun onMemorySubtract() {
        triggerHaptic()
        val value = currentEvaluatedValue() ?: return
        viewModelScope.launch {
            settingsRepository.addToMemory(-value)
        }
    }

    private fun currentEvaluatedValue(): Double? {
        val current = _state.value
        if (current.expression.isEmpty()) return null
        val eval = ExpressionParser.evaluateLivePreview(current.expression, current.angleMode)
        return (eval as? EvalResult.Success)?.value
    }

    fun loadExpressionFromHistory(expression: String, @Suppress("UNUSED_PARAMETER") result: String) {
        _state.update { current ->
            val cleanExpr = expression.replace(",", "").replace("-", "−")
            recalculate(
                current.copy(
                    expression = cleanExpr,
                    cursorPosition = cleanExpr.length,
                    justEvaluated = false,
                    errorMessage = null
                )
            )
        }
    }

    fun checkClipboardCandidate(text: String?) {
        if (!latestSettings.clipboardDetectionEnabled || text.isNullOrBlank()) {
            _state.update { it.copy(clipboardCandidate = null) }
            return
        }
        val trimmed = text.trim()
        val hasMathOps = trimmed.any { it in listOf('+', '-', '*', '/', '×', '÷', '^', '%') } ||
            trimmed.contains("sin") || trimmed.contains("cos") || trimmed.contains("sqrt")
        val valid = trimmed.length in 3..80 && hasMathOps &&
            ExpressionParser.evaluateLivePreview(trimmed, _state.value.angleMode) is EvalResult.Success
        if (valid && trimmed != _state.value.expression) {
            _state.update { it.copy(clipboardCandidate = trimmed) }
        }
    }

    fun useClipboardCandidate(expr: String) {
        triggerHaptic()
        _state.update { current ->
            val normalized = expr.replace("*", "×").replace("/", "÷").replace("-", "−").replace(",", "")
            recalculate(
                current.copy(
                    expression = normalized,
                    cursorPosition = normalized.length,
                    clipboardCandidate = null,
                    justEvaluated = false,
                    errorMessage = null
                )
            )
        }
    }

    fun dismissClipboardCandidate() {
        _state.update { it.copy(clipboardCandidate = null) }
    }

    fun grabAnswer(): String? {
        triggerHaptic()
        val current = _state.value
        val answer = when {
            !current.scannedFinalAnswer.isNullOrBlank() ->
                current.scannedFinalAnswer.trim().removePrefix("=").trim()
            current.livePreview.isNotBlank() ->
                current.livePreview.trim()
            current.justEvaluated && current.expression.isNotBlank() ->
                current.formattedExpression.ifBlank { current.expression }.trim()
            current.expression.isNotBlank() -> {
                val res = ExpressionParser.evaluate(current.expression, current.angleMode)
                if (res is EvalResult.Success) {
                    NumberFormatter.formatResult(
                        value = res.value,
                        decimalPrecision = latestSettings.decimalPrecision,
                        formatMode = latestSettings.numberFormatMode
                    )
                } else {
                    null
                }
            }
            else -> null
        } ?: return null

        _state.update { it.copy(grabbedAnswer = answer) }
        return answer
    }

    fun putGrabbedAnswer() {
        triggerHaptic()
        _state.update { rawCurrent ->
            val rawGrabbed = rawCurrent.grabbedAnswer ?: return@update rawCurrent
            val pasteToken = rawGrabbed.replace(",", "").replace("-", "−").trim()
            val clean = rawCurrent.withoutScanOverlay()
            val base = if (clean.justEvaluated) "" else clean.expression
            val pos = if (clean.justEvaluated) 0 else clean.cursorPosition.coerceIn(0, base.length)
            val (nextExpr, nextCursor) = insertAtCursor(base, pos, pasteToken)
            recalculate(
                clean.copy(
                    expression = nextExpr,
                    cursorPosition = nextCursor,
                    justEvaluated = false,
                    errorMessage = null,
                    grabbedAnswer = null
                )
            )
        }
    }

    fun processVoiceInput(spokenText: String) {
        triggerHaptic()
        val currentAngle = _state.value.angleMode
        when (val route = VoiceMathClassifier.classify(spokenText, currentAngle)) {
            is VoiceQueryRoute.NormalCalculation -> {
                val balanced = route.normalizedExpression
                val eval = ExpressionParser.evaluate(balanced, currentAngle)
                if (eval is EvalResult.Success) {
                    val formatted = NumberFormatter.formatResult(
                        value = eval.value,
                        decimalPrecision = latestSettings.decimalPrecision,
                        formatMode = latestSettings.numberFormatMode
                    )
                    viewModelScope.launch {
                        historyRepository.saveCalculation(
                            expression = NumberFormatter.formatExpressionNumbers(balanced, NumberFormatMode.INTERNATIONAL),
                            formattedResult = formatted,
                            rawResult = eval.value,
                            angleMode = currentAngle.name
                        )
                    }
                    val editable = NumberFormatter.toEditableExpressionNumber(
                        value = eval.value,
                        decimalPrecision = latestSettings.decimalPrecision
                    )
                    scanJob?.cancel()
                    _state.update {
                        it.withoutScanOverlay().copy(
                            expression = editable,
                            cursorPosition = editable.length,
                            livePreview = "",
                            errorMessage = null,
                            justEvaluated = true
                        )
                    }
                } else {
                    solveTypedProblem(spokenText)
                }
            }
            is VoiceQueryRoute.AiModeQuery -> {
                solveTypedProblem(route.spokenPrompt)
            }
        }
    }

    fun solveScannedBitmap(bitmap: Bitmap) {
        if (!consumeAiQuestionOrShowLimit()) return
        activeAiQuestion = "Scanned image problem"
        collectAiStream(problemScannerRepository.solveProblemFromBitmapStream(bitmap))
    }

    fun solveTypedProblem(problemText: String) {
        if (problemText.isBlank()) return
        if (!consumeAiQuestionOrShowLimit()) return
        activeAiQuestion = problemText.trim()
        collectAiStream(problemScannerRepository.solveProblemFromTextStream(problemText))
    }

    private fun consumeAiQuestionOrShowLimit(): Boolean {
        val remaining = settingsRepository.consumeAiQuestion()
        _state.update { it.copy(aiQuestionsRemaining = remaining.coerceAtLeast(0)) }
        if (remaining >= 0) return true
        _state.update {
            it.copy(
                isScanningProblem = false,
                scannedAnswerText = null,
                scannedFinalAnswer = null,
                scanErrorMessage = "Daily AI limit reached. Your 10 questions will reset tomorrow."
            )
        }
        return false
    }

    private fun collectAiStream(flow: kotlinx.coroutines.flow.Flow<ScanProblemStreamEvent>) {
        triggerHaptic()
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            flow.collect { event ->
                when (event) {
                    is ScanProblemStreamEvent.Loading -> {
                        _state.update {
                            it.copy(
                                isScanningProblem = true,
                                scannedAnswerText = "",
                                scannedFinalAnswer = null,
                                scanErrorMessage = null,
                                errorMessage = null
                            )
                        }
                    }
                    is ScanProblemStreamEvent.Streaming -> {
                        _state.update {
                            it.copy(
                                isScanningProblem = true,
                                scannedAnswerText = event.partialText,
                                scanErrorMessage = null
                            )
                        }
                    }
                    is ScanProblemStreamEvent.Completed -> {
                        _state.update {
                            it.copy(
                                isScanningProblem = false,
                                scannedAnswerText = event.fullText,
                                scannedFinalAnswer = event.finalAnswer,
                                scanErrorMessage = null
                            )
                        }
                        val question = activeAiQuestion
                        if (question.isNotBlank() && event.fullText.isNotBlank()) {
                            historyRepository.saveAiConversation(question, event.fullText)
                            activeAiQuestion = ""
                        }
                    }
                    is ScanProblemStreamEvent.Error -> {
                        _state.update {
                            it.copy(
                                isScanningProblem = false,
                                scannedAnswerText = null,
                                scannedFinalAnswer = null,
                                scanErrorMessage = event.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun setScanError(message: String) {
        scanJob?.cancel()
        _state.update {
            it.copy(
                isScanningProblem = false,
                scannedAnswerText = null,
                scannedFinalAnswer = null,
                scanErrorMessage = message
            )
        }
    }

    fun clearScanResult() {
        triggerHaptic()
        scanJob?.cancel()
        _state.update { it.withoutScanOverlay() }
    }

    private fun CalculatorUiState.withoutScanOverlay(): CalculatorUiState =
        if (isScanningProblem || scannedAnswerText != null || scannedFinalAnswer != null || scanErrorMessage != null) {
            copy(
                isScanningProblem = false,
                scannedAnswerText = null,
                scannedFinalAnswer = null,
                scanErrorMessage = null
            )
        } else {
            this
        }

    private fun recalculate(state: CalculatorUiState): CalculatorUiState {
        val cleanState = state.withoutScanOverlay()
        if (cleanState.expression.isBlank()) {
            return cleanState.copy(livePreview = "")
        }
        val preview = ExpressionParser.evaluateLivePreview(cleanState.expression, cleanState.angleMode)
        val previewText = when (preview) {
            is EvalResult.Success -> NumberFormatter.formatResult(
                value = preview.value,
                decimalPrecision = latestSettings.decimalPrecision,
                formatMode = latestSettings.numberFormatMode
            )
            else -> ""
        }
        return cleanState.copy(livePreview = previewText)
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val historyRepository: HistoryRepository,
        private val hapticManager: HapticManager,
        private val problemScannerRepository: ProblemScannerRepository = FirebaseProblemScannerRepository()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalculatorViewModel(
                settingsRepository = settingsRepository,
                historyRepository = historyRepository,
                hapticManager = hapticManager,
                problemScannerRepository = problemScannerRepository
            ) as T
        }
    }
}
