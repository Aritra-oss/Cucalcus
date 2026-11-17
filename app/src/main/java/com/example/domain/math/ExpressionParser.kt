package com.example.domain.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class AngleMode {
    DEG, RAD
}

enum class MathErrorType {
    DIVISION_BY_ZERO,
    DOMAIN_ERROR,
    SYNTAX_ERROR,
    OVERFLOW
}

sealed interface EvalResult {
    data class Success(val value: Double) : EvalResult
    data class Error(val type: MathErrorType, val message: String) : EvalResult
}

/**
 * Safe recursive-descent mathematical expression parser and evaluator.
 * Supports standard precedence, implicit multiplication, scientific functions,
 * factorial, modulo, calculator-style percentage, and DEG/RAD modes.
 */
object ExpressionParser {

    fun evaluate(rawExpression: String, angleMode: AngleMode = AngleMode.DEG): EvalResult {
        val cleaned = rawExpression.trim()
        if (cleaned.isEmpty()) {
            return EvalResult.Error(MathErrorType.SYNTAX_ERROR, "Empty expression")
        }

        return try {
            val tokens = tokenize(cleaned)
            if (tokens.isEmpty()) {
                return EvalResult.Error(MathErrorType.SYNTAX_ERROR, "Invalid expression")
            }
            val parser = Parser(tokens, angleMode)
            val result = parser.parse()
            if (result.isNaN()) {
                EvalResult.Error(MathErrorType.DOMAIN_ERROR, "Domain error")
            } else if (result.isInfinite()) {
                EvalResult.Error(MathErrorType.OVERFLOW, "Value out of range")
            } else {
                EvalResult.Success(cleanFloatingPointNoise(result))
            }
        } catch (e: MathEvalException) {
            EvalResult.Error(e.type, e.message ?: "Calculation error")
        } catch (_: Exception) {
            EvalResult.Error(MathErrorType.SYNTAX_ERROR, "Invalid expression")
        }
    }

    /**
     * Auto-closes unclosed trailing parentheses for live preview evaluation.
     */
    fun evaluateLivePreview(rawExpression: String, angleMode: AngleMode = AngleMode.DEG): EvalResult? {
        val trimmed = rawExpression.trim()
        if (trimmed.isEmpty()) return null

        // Strip trailing binary operators so live preview evaluates the valid prefix
        var candidate = trimmed
        while (candidate.isNotEmpty() && candidate.last() in listOf('+', '-', '−', '×', '*', '÷', '/', '^', '(')) {
            candidate = candidate.dropLast(1).trimEnd()
        }
        if (candidate.endsWith("mod")) {
            candidate = candidate.removeSuffix("mod").trimEnd()
        }
        if (candidate.isEmpty()) return null

        val openCount = candidate.count { it == '(' }
        val closeCount = candidate.count { it == ')' }
        val balanced = if (openCount > closeCount) {
            candidate + ")".repeat(openCount - closeCount)
        } else {
            candidate
        }

        return evaluate(balanced, angleMode)
    }

    private fun cleanFloatingPointNoise(value: Double): Double {
        if (abs(value) < 1e-14) return 0.0
        val nearest = value.roundToLong().toDouble()
        if (abs(value - nearest) < 1e-12 && abs(nearest) < 1e12) {
            return nearest
        }
        return value
    }

    private sealed interface Token {
        data class Number(val value: Double) : Token
        data class Constant(val symbol: String) : Token
        data class Function(val name: String) : Token
        data class Operator(val symbol: String) : Token
        data object LeftParen : Token
        data object RightParen : Token
        data object Percent : Token
        data object Factorial : Token
    }

    private class MathEvalException(val type: MathErrorType, message: String) : RuntimeException(message)

    private fun tokenize(input: String): List<Token> {
        val normalized = input
            .replace("−", "-")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("√", "sqrt")
            .replace("π", "pi")
            .replace("sin⁻¹", "asin")
            .replace("cos⁻¹", "acos")
            .replace("tan⁻¹", "atan")
            .replace(",", "")
            .replace(" ", "")

        val rawTokens = mutableListOf<Token>()
        var i = 0
        while (i < normalized.length) {
            val ch = normalized[i]
            when {
                ch.isDigit() || ch == '.' -> {
                    val start = i
                    var hasDot = (ch == '.')
                    i++
                    while (i < normalized.length) {
                        val c = normalized[i]
                        if (c.isDigit()) {
                            i++
                        } else if (c == '.' && !hasDot) {
                            hasDot = true
                            i++
                        } else if ((c == 'E' || c == 'e') && i + 1 < normalized.length &&
                            (normalized[i + 1].isDigit() || normalized[i + 1] == '+' || normalized[i + 1] == '-')
                        ) {
                            // Scientific notation number literal like 1.5E-4 or 2e5
                            i++
                            if (i < normalized.length && (normalized[i] == '+' || normalized[i] == '-')) {
                                i++
                            }
                            if (i >= normalized.length || !normalized[i].isDigit()) {
                                throw MathEvalException(MathErrorType.SYNTAX_ERROR, "Invalid exponent")
                            }
                            while (i < normalized.length && normalized[i].isDigit()) {
                                i++
                            }
                            break
                        } else {
                            break
                        }
                    }
                    val numStr = normalized.substring(start, i)
                    val num = numStr.toDoubleOrNull()
                        ?: throw MathEvalException(MathErrorType.SYNTAX_ERROR, "Invalid number")
                    rawTokens.add(Token.Number(num))
                }
                ch.isLetter() -> {
                    val start = i
                    while (i < normalized.length && (normalized[i].isLetter() || normalized[i].isDigit())) {
                        // Stop before splitting 'e' followed by something? Actually functions are alphabetic, constants are pi, e
                        i++
                    }
                    val word = normalized.substring(start, i).lowercase()
                    when (word) {
                        "pi" -> rawTokens.add(Token.Constant("pi"))
                        "e" -> rawTokens.add(Token.Constant("e"))
                        "mod" -> rawTokens.add(Token.Operator("mod"))
                        "sin", "cos", "tan", "asin", "acos", "atan",
                        "log", "ln", "sqrt", "abs", "exp", "sq", "inv" -> rawTokens.add(Token.Function(word))
                        else -> throw MathEvalException(MathErrorType.SYNTAX_ERROR, "Unknown token: $word")
                    }
                }
                ch == '(' -> {
                    rawTokens.add(Token.LeftParen)
                    i++
                }
                ch == ')' -> {
                    rawTokens.add(Token.RightParen)
                    i++
                }
                ch == '%' -> {
                    rawTokens.add(Token.Percent)
                    i++
                }
                ch == '!' -> {
                    rawTokens.add(Token.Factorial)
                    i++
                }
                ch in listOf('+', '-', '*', '/', '^') -> {
                    rawTokens.add(Token.Operator(ch.toString()))
                    i++
                }
                else -> throw MathEvalException(MathErrorType.SYNTAX_ERROR, "Unexpected character: $ch")
            }
        }

        // Insert implicit multiplication where appropriate:
        // e.g. 2 pi -> 2 * pi, 2 (3) -> 2 * (3), ) ( -> ) * (, 2 sin(30) -> 2 * sin(30), pi 2 -> pi * 2, ) 2 -> ) * 2
        val result = mutableListOf<Token>()
        for (idx in rawTokens.indices) {
            val curr = rawTokens[idx]
            if (result.isNotEmpty()) {
                val prev = result.last()
                val prevEndsValue = prev is Token.Number || prev is Token.Constant ||
                    prev is Token.RightParen || prev is Token.Percent || prev is Token.Factorial
                val currStartsValue = curr is Token.Number || curr is Token.Constant ||
                    curr is Token.Function || curr is Token.LeftParen
                if (prevEndsValue && currStartsValue) {
                    result.add(Token.Operator("*"))
                }
            }
            result.add(curr)
        }
        return result
    }

    private class Parser(
        private val tokens: List<Token>,
        private val angleMode: AngleMode
    ) {
        private var pos = 0

        fun parse(): Double {
            val value = parseAddSub()
            if (pos < tokens.size) {
                throw MathEvalException(MathErrorType.SYNTAX_ERROR, "Unexpected trailing expression")
            }
            return value
        }

        private fun peek(): Token? = tokens.getOrNull(pos)
        private fun peekNext(): Token? = tokens.getOrNull(pos + 1)

        // Handles + and -, including calculator-style percentage: A + B% = A + (A * B / 100)
        private fun parseAddSub(): Double {
            var left = parseMulDivMod()
            while (true) {
                val tok = peek()
                if (tok is Token.Operator && (tok.symbol == "+" || tok.symbol == "-")) {
                    pos++
                    // Check if RHS is a standalone percentage term (e.g., 100 + 10%)
                    val rightTermStart = pos
                    val right = parseMulDivModWithPercentFlag()
                    left = if (right.isStandalonePercent) {
                        val delta = left * (right.rawValue / 100.0)
                        if (tok.symbol == "+") left + delta else left - delta
                    } else {
                        if (tok.symbol == "+") left + right.evaluatedValue else left - right.evaluatedValue
                    }
                    @Suppress("UNUSED_VARIABLE")
                    val unused = rightTermStart
                } else {
                    break
                }
            }
            return left
        }

        private data class TermResult(val evaluatedValue: Double, val rawValue: Double, val isStandalonePercent: Boolean)

        private fun parseMulDivModWithPercentFlag(): TermResult {
            // Check if this multiplicative chain is a single postfix-% primary (no *, /, mod)
            val startPos = pos
            val valWithoutDiv = parseMulDivMod()
            // Inspect if tokens[startPos..pos-1] ended with Token.Percent and had no * / mod at top level
            val consumed = tokens.subList(startPos, pos)
            if (consumed.isNotEmpty() && consumed.last() is Token.Percent) {
                // Re-verify it had no binary * / mod at depth 0
                var depth = 0
                var hasTopMulDiv = false
                for (t in consumed) {
                    when (t) {
                        is Token.LeftParen -> depth++
                        is Token.RightParen -> depth--
                        is Token.Operator -> if (depth == 0 && t.symbol in listOf("*", "/", "mod")) hasTopMulDiv = true
                        else -> {}
                    }
                }
                if (!hasTopMulDiv) {
                    return TermResult(
                        evaluatedValue = valWithoutDiv,
                        rawValue = valWithoutDiv * 100.0,
                        isStandalonePercent = true
                    )
                }
            }
            return TermResult(valWithoutDiv, valWithoutDiv, false)
        }

        private fun parseMulDivMod(): Double {
            var left = parseUnary()
            while (true) {
                val tok = peek()
                if (tok is Token.Operator && tok.symbol in listOf("*", "/", "mod")) {
                    pos++
                    val right = parseUnary()
                    left = when (tok.symbol) {
                        "*" -> left * right
                        "/" -> {
                            if (abs(right) == 0.0) {
                                throw MathEvalException(MathErrorType.DIVISION_BY_ZERO, "Cannot divide by zero")
                            }
                            left / right
                        }
                        "mod" -> {
                            if (abs(right) == 0.0) {
                                throw MathEvalException(MathErrorType.DIVISION_BY_ZERO, "Cannot divide by zero")
                            }
                            left % right
                        }
                        else -> left
                    }
                } else {
                    break
                }
            }
            return left
        }

        private fun parseUnary(): Double {
            val tok = peek()
            if (tok is Token.Operator && tok.symbol == "-") {
                pos++
                return -parseUnary()
            }
            if (tok is Token.Operator && tok.symbol == "+") {
                pos++
                return parseUnary()
            }
            return parsePower()
        }

        // Right-associative ^
        private fun parsePower(): Double {
            val base = parsePostfix()
            val tok = peek()
            if (tok is Token.Operator && tok.symbol == "^") {
                pos++
                val exponent = parseUnary()
                if (base < 0 && exponent != floorVal(exponent)) {
                    throw MathEvalException(MathErrorType.DOMAIN_ERROR, "Complex result not supported")
                }
                if (base == 0.0 && exponent < 0) {
                    throw MathEvalException(MathErrorType.DIVISION_BY_ZERO, "Cannot divide by zero")
                }
                val res = base.pow(exponent)
                if (res.isInfinite() || res.isNaN()) {
                    throw MathEvalException(MathErrorType.OVERFLOW, "Value out of range")
                }
                return res
            }
            return base
        }

        private fun floorVal(x: Double): Double = kotlin.math.floor(x)

        private fun parsePostfix(): Double {
            var value = parsePrimary()
            while (true) {
                when (peek()) {
                    is Token.Percent -> {
                        pos++
                        value /= 100.0
                    }
                    is Token.Factorial -> {
                        pos++
                        value = computeFactorial(value)
                    }
                    else -> break
                }
            }
            return value
        }

        private fun computeFactorial(value: Double): Double {
            if (value < 0 || abs(value - value.roundToLong()) > 1e-9) {
                throw MathEvalException(MathErrorType.DOMAIN_ERROR, "Factorial requires a non-negative integer")
            }
            val n = value.roundToLong().toInt()
            if (n > 170) {
                throw MathEvalException(MathErrorType.OVERFLOW, "Factorial overflow")
            }
            var acc = 1.0
            for (k in 2..n) {
                acc *= k
            }
            return acc
        }

        private fun parsePrimary(): Double {
            return when (val tok = peek()) {
                is Token.Number -> {
                    pos++
                    tok.value
                }
                is Token.Constant -> {
                    pos++
                    when (tok.symbol) {
                        "pi" -> PI
                        "e" -> Math.E
                        else -> throw MathEvalException(MathErrorType.SYNTAX_ERROR, "Unknown constant")
                    }
                }
                is Token.LeftParen -> {
                    pos++
                    val value = parseAddSub()
                    if (peek() !is Token.RightParen) {
                        throw MathEvalException(MathErrorType.SYNTAX_ERROR, "Missing closing parenthesis")
                    }
                    pos++
                    value
                }
                is Token.Function -> {
                    pos++
                    val arg = if (peek() is Token.LeftParen) {
                        pos++
                        val inside = parseAddSub()
                        if (peek() !is Token.RightParen) {
                            throw MathEvalException(MathErrorType.SYNTAX_ERROR, "Missing closing parenthesis")
                        }
                        pos++
                        inside
                    } else {
                        parseUnary()
                    }
                    applyFunction(tok.name, arg)
                }
                null -> throw MathEvalException(MathErrorType.SYNTAX_ERROR, "Unexpected end of expression")
                else -> throw MathEvalException(MathErrorType.SYNTAX_ERROR, "Invalid expression")
            }
        }

        private fun toRadians(x: Double): Double = if (angleMode == AngleMode.DEG) Math.toRadians(x) else x
        private fun fromRadians(rad: Double): Double = if (angleMode == AngleMode.DEG) Math.toDegrees(rad) else rad

        private fun applyFunction(name: String, arg: Double): Double {
            return when (name) {
                "sin" -> sin(toRadians(arg))
                "cos" -> cos(toRadians(arg))
                "tan" -> {
                    val rad = toRadians(arg)
                    val c = cos(rad)
                    if (abs(c) < 1e-12) {
                        throw MathEvalException(MathErrorType.DOMAIN_ERROR, "Tangent undefined at 90°")
                    }
                    tan(rad)
                }
                "asin" -> {
                    if (arg < -1.0 || arg > 1.0) {
                        throw MathEvalException(MathErrorType.DOMAIN_ERROR, "sin⁻¹ domain is [-1, 1]")
                    }
                    fromRadians(asin(arg))
                }
                "acos" -> {
                    if (arg < -1.0 || arg > 1.0) {
                        throw MathEvalException(MathErrorType.DOMAIN_ERROR, "cos⁻¹ domain is [-1, 1]")
                    }
                    fromRadians(acos(arg))
                }
                "atan" -> fromRadians(atan(arg))
                "log" -> {
                    if (arg <= 0.0) {
                        throw MathEvalException(MathErrorType.DOMAIN_ERROR, "Logarithm requires positive input")
                    }
                    log10(arg)
                }
                "ln" -> {
                    if (arg <= 0.0) {
                        throw MathEvalException(MathErrorType.DOMAIN_ERROR, "Natural log requires positive input")
                    }
                    ln(arg)
                }
                "sqrt" -> {
                    if (arg < 0.0) {
                        throw MathEvalException(MathErrorType.DOMAIN_ERROR, "Square root of negative number")
                    }
                    sqrt(arg)
                }
                "abs" -> abs(arg)
                "exp" -> {
                    val res = exp(arg)
                    if (res.isInfinite()) throw MathEvalException(MathErrorType.OVERFLOW, "Value out of range")
                    res
                }
                "sq" -> arg * arg
                "inv" -> {
                    if (abs(arg) == 0.0) {
                        throw MathEvalException(MathErrorType.DIVISION_BY_ZERO, "Cannot divide by zero")
                    }
                    1.0 / arg
                }
                else -> throw MathEvalException(MathErrorType.SYNTAX_ERROR, "Unknown function: $name")
            }
        }
    }
}
