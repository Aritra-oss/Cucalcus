package com.example.domain.math

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

enum class NumberFormatMode(val label: String) {
    INTERNATIONAL("International (1,234,567)"),
    INDIAN("Indian (12,34,567)"),
    SCIENTIFIC("Scientific (1.23e+6)")
}

object NumberFormatter {

    fun formatResult(
        value: Double,
        decimalPrecision: Int = 8,
        formatMode: NumberFormatMode = NumberFormatMode.INTERNATIONAL
    ): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return "Overflow"

        val cleanVal = if (abs(value) < 1e-14) 0.0 else value
        val precision = decimalPrecision.coerceIn(0, 12)

        if (formatMode == NumberFormatMode.SCIENTIFIC ||
            (cleanVal != 0.0 && (abs(cleanVal) >= 1e12 || abs(cleanVal) < 1e-9))
        ) {
            val pattern = if (precision == 0) "0E0" else "0." + "#".repeat(precision) + "E0"
            val df = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
            return df.format(cleanVal).replace("E", "e")
        }

        val bd = BigDecimal.valueOf(cleanVal)
            .setScale(precision, RoundingMode.HALF_UP)
            .stripTrailingZeros()

        val plain = bd.toPlainString()
        val parts = plain.split(".")
        val intPart = parts[0]
        val fracPart = if (parts.size > 1) parts[1] else null

        val isNegative = intPart.startsWith("-")
        val digits = if (isNegative) intPart.substring(1) else intPart

        val groupedInt = when (formatMode) {
            NumberFormatMode.INDIAN -> formatIndianGrouping(digits)
            else -> formatInternationalGrouping(digits)
        }

        return buildString {
            if (isNegative) append('-')
            append(groupedInt)
            if (!fracPart.isNullOrEmpty()) {
                append('.')
                append(fracPart)
            }
        }
    }

    /**
     * Formats numbers inside an active expression string with thousands separators
     * while preserving operators, functions, and trailing decimal points.
     */
    fun formatExpressionNumbers(
        expression: String,
        formatMode: NumberFormatMode
    ): String {
        if (expression.isEmpty() || formatMode == NumberFormatMode.SCIENTIFIC) return expression
        val regex = Regex("""\d+(\.\d*)?""")
        return regex.replace(expression) { match ->
            val token = match.value
            val dotIdx = token.indexOf('.')
            val intDigits = if (dotIdx >= 0) token.substring(0, dotIdx) else token
            val fracPart = if (dotIdx >= 0) token.substring(dotIdx) else ""
            val grouped = if (formatMode == NumberFormatMode.INDIAN) {
                formatIndianGrouping(intDigits)
            } else {
                formatInternationalGrouping(intDigits)
            }
            grouped + fracPart
        }
    }

    private fun formatInternationalGrouping(digits: String): String {
        if (digits.length <= 3) return digits
        val sb = StringBuilder()
        var count = 0
        for (i in digits.length - 1 downTo 0) {
            sb.append(digits[i])
            count++
            if (count % 3 == 0 && i != 0) {
                sb.append(',')
            }
        }
        return sb.reverse().toString()
    }

    private fun formatIndianGrouping(digits: String): String {
        if (digits.length <= 3) return digits
        val lastThree = digits.takeLast(3)
        val rest = digits.dropLast(3)
        val sb = StringBuilder()
        var count = 0
        for (i in rest.length - 1 downTo 0) {
            sb.append(rest[i])
            count++
            if (count % 2 == 0 && i != 0) {
                sb.append(',')
            }
        }
        return sb.reverse().toString() + "," + lastThree
    }
}
