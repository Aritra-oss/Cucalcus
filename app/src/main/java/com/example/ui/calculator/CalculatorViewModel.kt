package com.example.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.CucalcusSettings
import com.example.data.preferences.SettingsRepository
import com.example.data.repository.HistoryRepository
import com.example.domain.math.AngleMode
import com.example.domain.math.EvalResult
import com.example.domain.math.ExpressionParser
import com.example.domain.math.NumberFormatMode
import com.example.domain.math.NumberFormatter
import com.example.platform.HapticManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalculatorUiState(
    val expression: String = "",
    val formattedExpression: String = "",
    val livePreview: String = "",
    val errorMessage: String? = null,
    val justEvaluated: Boolean = false,
    val isScientificExpanded: Boolean = false,
    val isInverseSci: Boolean = false,
    val angleMode: AngleMode = AngleMode.DEG,
    val memoryValue: Double = 0.0,
    val clipboardCandidate: String? = null
)

class CalculatorViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val hapticManager: HapticManager
) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorUiState())
    private var latestSettings = CucalcusSettings()

    val settings: StateFlow<CucalcusSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, CucalcusSettings())

    init {
        viewModelScope.launch {
            val initial = settingsRepository.settingsFlow.first()
            latestSettings = initial
            _state.update {
                it.copy(
                    isScientificExpanded = initial.defaultScientificExpanded,
                    angleMode = initial.angleMode,
                    memoryValue = initial.calculatorMemory
                )
            }
        }
    }

    val uiState: StateFlow<CalculatorUiState> = combine(_state, settings) { st, cfg ->
        latestSettings = cfg
        val formattedExpr = NumberFormatter.formatExpressionNumbers(st.expression, cfg.numberFormatMode)
        st.copy(
            formattedExpression = formattedExpr,
            memoryValue = cfg.calculatorMemory
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CalculatorUiState())

    private fun triggerHaptic() {
        val cfg = latestSettings
        hapticManager.performTick(cfg.hapticStrength, cfg.hapticEnabled)
    }

    fun onDigit(digit: String) {
        triggerHaptic()
        _state.update { current ->
            val base = if (current.justEvaluated) "" else current.expression
            if (digit == ".") {
                val lastNumber = base.takeLastWhile { it.isDigit() || it == '.' }
                if (lastNumber.contains('.')) return@update current
                val addition = if (lastNumber.isEmpty()) "0." else "."
                return@update recalculate(current.copy(expression = base + addition, justEvaluated = false, errorMessage = null))
            }
            val nextExpr = if ((digit == "0" || digit == "00" || digit == "000") && base == "0") {
                "0"
            } else if ((digit == "00" || digit == "000") && (base.isEmpty() || base.last() in listOf('+', '−', '×', '÷', '(', '^'))) {
                base + "0"
            } else {
                base + digit
            }
            recalculate(current.copy(expression = nextExpr, justEvaluated = false, errorMessage = null))
        }
    }

    fun onOperator(op: String) {
        triggerHaptic()
        _state.update { current ->
            var expr = current.expression
            val symbol = if (op == "-") "−" else op

            if (expr.isEmpty()) {
                return@update if (symbol == "−") {
                    recalculate(current.copy(expression = "−", justEvaluated = false, errorMessage = null))
                } else {
                    current
                }
            }

            // Allow unary minus right after '(' or '^', but block binary operators right after '('
            if (expr.last() == '(') {
                return@update if (symbol == "−") {
                    recalculate(current.copy(expression = expr + "−", justEvaluated = false, errorMessage = null))
                } else {
                    current
                }
            }

            // If expression ends with "(−", replacing with another binary operator is invalid
            if (expr.endsWith("(−") || expr == "−") {
                return@update current
            }

            // Replace trailing binary operator if user changes operator
            val binaryOps = listOf("+", "−", "-", "×", "÷", "^")
            if (expr.endsWith("mod")) {
                expr = expr.removeSuffix("mod")
            } else if (expr.last().toString() in binaryOps) {
                expr = expr.dropLast(1)
                if (expr.endsWith("(") && symbol != "−") {
                    return@update current
                }
            }
            recalculate(current.copy(expression = expr + symbol, justEvaluated = false, errorMessage = null))
        }
    }

    fun onPercent() {
        triggerHaptic()
        _state.update { current ->
            if (current.expression.isEmpty()) return@update current
            val last = current.expression.last()
            if (last.isDigit() || last == ')' || last == 'π' || last == 'e') {
                recalculate(current.copy(expression = current.expression + "%", justEvaluated = false, errorMessage = null))
            } else {
                current
            }
        }
    }

    fun onParenthesis() {
        triggerHaptic()
        _state.update { current ->
            val expr = if (current.justEvaluated) "" else current.expression
            val open = expr.count { it == '(' }
            val close = expr.count { it == ')' }
            val last = expr.lastOrNull()
            val token = if (open > close && last != null && (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '%' || last == '!')) {
                ")"
            } else {
                "("
            }
            recalculate(current.copy(expression = expr + token, justEvaluated = false, errorMessage = null))
        }
    }

    fun onOpenParen() {
        triggerHaptic()
        _state.update { current ->
            val expr = if (current.justEvaluated) "" else current.expression
            recalculate(current.copy(expression = expr + "(", justEvaluated = false, errorMessage = null))
        }
    }

    fun onCloseParen() {
        triggerHaptic()
        _state.update { current ->
            val open = current.expression.count { it == '(' }
            val close = current.expression.count { it == ')' }
            val last = current.expression.lastOrNull()
            if (open > close && last != null && last != '(' && last !in listOf('+', '−', '×', '÷', '^')) {
                recalculate(current.copy(expression = current.expression + ")", justEvaluated = false, errorMessage = null))
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
                return@update current.copy(expression = "−", justEvaluated = false, errorMessage = null)
            }
            if (expr == "−") {
                return@update current.copy(expression = "", livePreview = "", justEvaluated = false, errorMessage = null)
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
            recalculate(current.copy(expression = next, justEvaluated = false, errorMessage = null))
        }
    }

    fun onScientificFunction(fn: String) {
        triggerHaptic()
        _state.update { current ->
            val expr = current.expression
            val base = if (current.justEvaluated) "" else expr
            val endsWithValue = expr.isNotEmpty() &&
                (expr.last().isDigit() || expr.last() in listOf(')', 'π', 'e', '%', '!'))

            val next = when (fn) {
                "x²" -> if (endsWithValue) "($expr)^2" else return@update current
                "1/x" -> if (endsWithValue) "1÷($expr)" else "${base}1÷("
                "√" -> "$base√("
                "|x|" -> "${base}abs("
                "eˣ" -> "${base}exp("
                "10ˣ" -> "${base}10^("
                "x!" -> if (endsWithValue) "$expr!" else return@update current
                "π" -> "${base}π"
                "e" -> "${base}e"
                else -> "$base$fn("
            }
            recalculate(current.copy(expression = next, justEvaluated = false, errorMessage = null))
        }
    }

    fun onBackspace() {
        triggerHaptic()
        _state.update { current ->
            if (current.expression.isEmpty()) return@update current.copy(errorMessage = null, livePreview = "")
            val expr = current.expression
            val multiTokens = listOf("sin⁻¹(", "cos⁻¹(", "tan⁻¹(", "sqrt(", "asin(", "acos(", "atan(", "sin(", "cos(", "tan(", "log(", "abs(", "exp(", "ln(", "mod", "√(")
            val matched = multiTokens.firstOrNull { expr.endsWith(it) }
            val next = if (matched != null) expr.dropLast(matched.length) else expr.dropLast(1)
            recalculate(current.copy(expression = next, justEvaluated = false, errorMessage = null))
        }
    }

    fun onClear() {
        triggerHaptic()
        _state.update {
            it.copy(
                expression = "",
                livePreview = "",
                errorMessage = null,
                justEvaluated = false
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
            val base = if (current.justEvaluated) "" else current.expression
            val next = if (base.isNotEmpty() && (base.last().isDigit() || base.last() == '.')) {
                "$base×$formatted"
            } else {
                base + formatted
            }
            recalculate(current.copy(expression = next, justEvaluated = false, errorMessage = null))
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
            recalculate(current.copy(expression = cleanExpr, justEvaluated = false, errorMessage = null))
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
            recalculate(current.copy(expression = normalized, clipboardCandidate = null, justEvaluated = false, errorMessage = null))
        }
    }

    fun dismissClipboardCandidate() {
        _state.update { it.copy(clipboardCandidate = null) }
    }

    private fun recalculate(state: CalculatorUiState): CalculatorUiState {
        if (state.expression.isBlank()) {
            return state.copy(livePreview = "")
        }
        val preview = ExpressionParser.evaluateLivePreview(state.expression, state.angleMode)
        val previewText = when (preview) {
            is EvalResult.Success -> NumberFormatter.formatResult(
                value = preview.value,
                decimalPrecision = latestSettings.decimalPrecision,
                formatMode = latestSettings.numberFormatMode
            )
            else -> ""
        }
        return state.copy(livePreview = previewText)
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val historyRepository: HistoryRepository,
        private val hapticManager: HapticManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalculatorViewModel(settingsRepository, historyRepository, hapticManager) as T
        }
    }
}
