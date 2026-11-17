package com.example.domain.tools

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

enum class ToolCategory(val title: String) {
    FINANCIAL("Financial"),
    MATHEMATICAL("Mathematical"),
    DATE_TIME("Date & Time")
}

enum class ToolId(
    val title: String,
    val subtitle: String,
    val category: ToolCategory
) {
    // Financial
    GST("GST Calculator", "Add or remove GST with CGST/SGST split", ToolCategory.FINANCIAL),
    DISCOUNT("Discount Calculator", "Final price & savings after discount", ToolCategory.FINANCIAL),
    PROFIT("Profit Calculator", "Profit amount, margin & markup %", ToolCategory.FINANCIAL),
    LOSS("Loss Calculator", "Loss amount & loss percentage", ToolCategory.FINANCIAL),
    SIMPLE_INTEREST("Simple Interest", "Principal, rate & tenure interest", ToolCategory.FINANCIAL),
    COMPOUND_INTEREST("Compound Interest", "Compounding frequency growth", ToolCategory.FINANCIAL),
    EMI("EMI Calculator", "Monthly loan instalment & interest", ToolCategory.FINANCIAL),
    TIP("Tip Calculator", "Tip amount & per-person split", ToolCategory.FINANCIAL),
    PERCENTAGE("Percentage Calculator", "X% of Y, % change & ratio %", ToolCategory.FINANCIAL),

    // Mathematical
    FRACTION("Fraction Calculator", "Simplify, add, subtract, multiply, divide", ToolCategory.MATHEMATICAL),
    RATIO("Ratio Calculator", "Simplify ratios & solve proportions", ToolCategory.MATHEMATICAL),
    AVERAGE("Average Calculator", "Mean, median, sum, min & max", ToolCategory.MATHEMATICAL),
    PRIME("Prime Number Checker", "Primality test & prime factorization", ToolCategory.MATHEMATICAL),
    LCM("LCM Calculator", "Least common multiple of integers", ToolCategory.MATHEMATICAL),
    HCF("HCF / GCD Calculator", "Highest common factor of integers", ToolCategory.MATHEMATICAL),
    BINARY("Binary Converter", "Base-2 to Dec, Oct, Hex", ToolCategory.MATHEMATICAL),
    DECIMAL("Decimal Converter", "Base-10 to Bin, Oct, Hex", ToolCategory.MATHEMATICAL),
    OCTAL("Octal Converter", "Base-8 to Bin, Dec, Hex", ToolCategory.MATHEMATICAL),
    HEXADECIMAL("Hexadecimal Converter", "Base-16 to Bin, Dec, Oct", ToolCategory.MATHEMATICAL),

    // Date & Time
    AGE("Age Calculator", "Exact years, months, days & next birthday", ToolCategory.DATE_TIME),
    DATE_DIFF("Date Difference", "Exact calendar difference (Y/M/D)", ToolCategory.DATE_TIME),
    DAYS_BETWEEN("Days Between Dates", "Total days, weeks & workdays", ToolCategory.DATE_TIME),
    TIME_DURATION("Time Duration", "Elapsed hours, minutes & seconds", ToolCategory.DATE_TIME)
}

object ToolsEngine {

    // --- Financial ---
    data class GstResult(val netAmount: Double, val gstAmount: Double, val cgst: Double, val sgst: Double, val totalAmount: Double)
    fun calculateGst(amount: Double, ratePercent: Double, isInclusive: Boolean): GstResult {
        return if (isInclusive) {
            val net = amount / (1.0 + ratePercent / 100.0)
            val gst = amount - net
            GstResult(net, gst, gst / 2.0, gst / 2.0, amount)
        } else {
            val gst = amount * ratePercent / 100.0
            GstResult(amount, gst, gst / 2.0, gst / 2.0, amount + gst)
        }
    }

    data class DiscountResult(val savings: Double, val finalPrice: Double)
    fun calculateDiscount(originalPrice: Double, discountPercent: Double, extraTaxPercent: Double = 0.0): DiscountResult {
        val savings = originalPrice * (discountPercent / 100.0)
        val afterDiscount = originalPrice - savings
        val finalPrice = afterDiscount * (1.0 + extraTaxPercent / 100.0)
        return DiscountResult(savings, finalPrice)
    }

    data class ProfitResult(val profit: Double, val markupPercent: Double, val marginPercent: Double)
    fun calculateProfit(costPrice: Double, sellingPrice: Double): ProfitResult {
        val profit = sellingPrice - costPrice
        val markup = if (costPrice != 0.0) (profit / costPrice) * 100.0 else 0.0
        val margin = if (sellingPrice != 0.0) (profit / sellingPrice) * 100.0 else 0.0
        return ProfitResult(profit, markup, margin)
    }

    data class LossResult(val loss: Double, val lossPercent: Double)
    fun calculateLoss(costPrice: Double, sellingPrice: Double): LossResult {
        val loss = costPrice - sellingPrice
        val lossPct = if (costPrice != 0.0) (loss / costPrice) * 100.0 else 0.0
        return LossResult(loss, lossPct)
    }

    data class InterestResult(val interest: Double, val totalAmount: Double)
    fun calculateSimpleInterest(principal: Double, annualRatePercent: Double, years: Double): InterestResult {
        val interest = principal * annualRatePercent * years / 100.0
        return InterestResult(interest, principal + interest)
    }

    fun calculateCompoundInterest(
        principal: Double,
        annualRatePercent: Double,
        years: Double,
        compoundsPerYear: Int
    ): InterestResult {
        val n = compoundsPerYear.coerceAtLeast(1).toDouble()
        val r = annualRatePercent / 100.0
        val total = principal * (1.0 + r / n).pow(n * years)
        return InterestResult(total - principal, total)
    }

    data class EmiResult(val monthlyEmi: Double, val totalInterest: Double, val totalPayment: Double)
    fun calculateEmi(principal: Double, annualRatePercent: Double, months: Int): EmiResult {
        if (months <= 0) return EmiResult(0.0, 0.0, 0.0)
        val monthlyRate = annualRatePercent / 12.0 / 100.0
        val emi = if (monthlyRate == 0.0) {
            principal / months
        } else {
            val factor = (1.0 + monthlyRate).pow(months)
            principal * monthlyRate * factor / (factor - 1.0)
        }
        val totalPayment = emi * months
        return EmiResult(emi, totalPayment - principal, totalPayment)
    }

    data class TipResult(val tipAmount: Double, val totalBill: Double, val perPerson: Double)
    fun calculateTip(billAmount: Double, tipPercent: Double, splitCount: Int): TipResult {
        val people = splitCount.coerceAtLeast(1)
        val tip = billAmount * tipPercent / 100.0
        val total = billAmount + tip
        return TipResult(tip, total, total / people)
    }

    data class PercentageResult(val percentOfY: Double, val xIsWhatPercentOfY: Double, val percentChange: Double)
    fun calculatePercentages(x: Double, y: Double): PercentageResult {
        val pOfY = (x / 100.0) * y
        val whatPct = if (y != 0.0) (x / y) * 100.0 else 0.0
        val pctChange = if (x != 0.0) ((y - x) / x) * 100.0 else 0.0
        return PercentageResult(pOfY, whatPct, pctChange)
    }

    // --- Mathematical ---
    data class Fraction(val numerator: Long, val denominator: Long) {
        val decimal: Double get() = numerator.toDouble() / denominator.toDouble()
        fun formatted(): String = if (denominator == 1L) "$numerator" else "$numerator / $denominator"
    }

    enum class FractionOp { ADD, SUB, MUL, DIV }

    fun calculateFraction(n1: Long, d1: Long, n2: Long, d2: Long, op: FractionOp): Result<Fraction> {
        if (d1 == 0L || d2 == 0L) return Result.failure(IllegalArgumentException("Denominator cannot be zero"))
        val (rawNum, rawDen) = when (op) {
            FractionOp.ADD -> (n1 * d2 + n2 * d1) to (d1 * d2)
            FractionOp.SUB -> (n1 * d2 - n2 * d1) to (d1 * d2)
            FractionOp.MUL -> (n1 * n2) to (d1 * d2)
            FractionOp.DIV -> {
                if (n2 == 0L) return Result.failure(IllegalArgumentException("Cannot divide by zero fraction"))
                (n1 * d2) to (d1 * n2)
            }
        }
        return Result.success(simplifyFraction(rawNum, rawDen))
    }

    fun simplifyFraction(num: Long, den: Long): Fraction {
        if (den == 0L) return Fraction(num, 1L)
        if (num == 0L) return Fraction(0L, 1L)
        val sign = if ((num < 0) xor (den < 0)) -1L else 1L
        val a = abs(num)
        val b = abs(den)
        val g = gcd(a, b)
        return Fraction(sign * (a / g), b / g)
    }

    data class RatioResult(val simplifiedA: Long, val simplifiedB: Long, val solvedD: Double?)
    fun calculateRatio(a: Long, b: Long, c: Double? = null): RatioResult {
        val g = if (a == 0L && b == 0L) 1L else gcd(abs(a), abs(b)).coerceAtLeast(1L)
        val solvedD = if (c != null && a != 0L) (b.toDouble() / a.toDouble()) * c else null
        return RatioResult(a / g, b / g, solvedD)
    }

    data class AverageResult(val count: Int, val sum: Double, val mean: Double, val median: Double, val min: Double, val max: Double)
    fun calculateAverage(numbers: List<Double>): AverageResult? {
        if (numbers.isEmpty()) return null
        val sorted = numbers.sorted()
        val sum = numbers.sum()
        val mean = sum / numbers.size
        val median = if (sorted.size % 2 == 1) {
            sorted[sorted.size / 2]
        } else {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }
        return AverageResult(numbers.size, sum, mean, median, sorted.first(), sorted.last())
    }

    data class PrimeResult(val number: Long, val isPrime: Boolean, val factors: List<Long>)
    fun checkPrime(n: Long): PrimeResult {
        if (n < 2) return PrimeResult(n, false, emptyList())
        val factors = mutableListOf<Long>()
        var rem = n
        var d = 2L
        while (d * d <= rem) {
            while (rem % d == 0L) {
                factors.add(d)
                rem /= d
            }
            d += if (d == 2L) 1L else 2L
        }
        if (rem > 1L) factors.add(rem)
        return PrimeResult(n, factors.size == 1 && factors[0] == n, factors)
    }

    fun gcd(a: Long, b: Long): Long {
        var x = abs(a)
        var y = abs(b)
        while (y != 0L) {
            val t = x % y
            x = y
            y = t
        }
        return x
    }

    fun lcm(a: Long, b: Long): Long {
        if (a == 0L || b == 0L) return 0L
        return abs(a / gcd(a, b)) * abs(b)
    }

    fun gcdList(values: List<Long>): Long? = if (values.isEmpty()) null else values.reduce { acc, v -> gcd(acc, v) }
    fun lcmList(values: List<Long>): Long? = if (values.isEmpty()) null else values.reduce { acc, v -> lcm(acc, v) }

    data class RadixResult(val binary: String, val octal: String, val decimal: String, val hexadecimal: String)
    fun convertRadix(input: String, sourceRadix: Int): Result<RadixResult> {
        val clean = input.trim().uppercase()
        if (clean.isEmpty()) return Result.failure(IllegalArgumentException("Enter a value"))
        return try {
            val value = clean.toLong(sourceRadix)
            Result.success(
                RadixResult(
                    binary = value.toString(2),
                    octal = value.toString(8),
                    decimal = value.toString(10),
                    hexadecimal = value.toString(16).uppercase()
                )
            )
        } catch (_: NumberFormatException) {
            Result.failure(IllegalArgumentException("Invalid base-$sourceRadix number"))
        }
    }

    // --- Date & Time ---
    data class AgeResult(val years: Int, val months: Int, val days: Int, val totalDays: Long, val daysUntilNextBirthday: Long)
    fun calculateAge(birthDate: LocalDate, asOfDate: LocalDate = LocalDate.now()): Result<AgeResult> {
        if (birthDate.isAfter(asOfDate)) {
            return Result.failure(IllegalArgumentException("Birth date cannot be in the future"))
        }
        val period = Period.between(birthDate, asOfDate)
        val totalDays = ChronoUnit.DAYS.between(birthDate, asOfDate)
        var nextBirthday = birthDate.withYear(asOfDate.year)
        if (!nextBirthday.isAfter(asOfDate)) {
            nextBirthday = nextBirthday.plusYears(1)
        }
        val daysUntil = ChronoUnit.DAYS.between(asOfDate, nextBirthday)
        return Result.success(AgeResult(period.years, period.months, period.days, totalDays, daysUntil))
    }

    data class DateDiffResult(val years: Int, val months: Int, val days: Int, val totalDays: Long, val weeks: Double, val weekdays: Long)
    fun calculateDateDifference(start: LocalDate, end: LocalDate): DateDiffResult {
        val (from, to) = if (start.isAfter(end)) end to start else start to end
        val period = Period.between(from, to)
        val totalDays = ChronoUnit.DAYS.between(from, to)
        var weekdays = 0L
        var cursor = from
        // Efficient weekday count for reasonable spans, fallback formula for huge spans
        if (totalDays <= 50_000L) {
            while (cursor.isBefore(to)) {
                val dow = cursor.dayOfWeek.value
                if (dow in 1..5) weekdays++
                cursor = cursor.plusDays(1)
            }
        } else {
            weekdays = (totalDays * 5L) / 7L
        }
        return DateDiffResult(period.years, period.months, period.days, totalDays, totalDays / 7.0, weekdays)
    }

    data class TimeDurationResult(val hours: Long, val minutes: Long, val seconds: Long, val totalMinutes: Long, val totalSeconds: Long)
    fun calculateTimeDuration(start: LocalTime, end: LocalTime, wrapMidnight: Boolean = true): TimeDurationResult {
        var duration = Duration.between(start, end)
        if (duration.isNegative && wrapMidnight) {
            duration = duration.plusHours(24)
        }
        val totalSec = abs(duration.seconds)
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return TimeDurationResult(hours, minutes, seconds, totalSec / 60, totalSec)
    }
}
