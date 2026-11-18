package com.example.ui.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.math.NumberFormatMode
import com.example.domain.math.NumberFormatter
import com.example.domain.tools.ToolId
import com.example.domain.tools.ToolsEngine
import com.example.ui.theme.CucalcusTheme
import com.example.ui.theme.LocalDisplayFontFamily
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

@Composable
fun ToolWorkspaceContent(toolId: ToolId) {
    when (toolId) {
        ToolId.GST -> GstToolView()
        ToolId.DISCOUNT -> DiscountToolView()
        ToolId.PROFIT -> ProfitToolView()
        ToolId.LOSS -> LossToolView()
        ToolId.SIMPLE_INTEREST -> InterestToolView(isCompound = false)
        ToolId.COMPOUND_INTEREST -> InterestToolView(isCompound = true)
        ToolId.EMI -> EmiToolView()
        ToolId.TIP -> TipToolView()
        ToolId.PERCENTAGE -> PercentageToolView()
        ToolId.FRACTION -> FractionToolView()
        ToolId.RATIO -> RatioToolView()
        ToolId.AVERAGE -> AverageToolView()
        ToolId.PRIME -> PrimeToolView()
        ToolId.LCM -> LcmHcfToolView(isLcm = true)
        ToolId.HCF -> LcmHcfToolView(isLcm = false)
        ToolId.BINARY -> RadixToolView(initialRadix = 2)
        ToolId.DECIMAL -> RadixToolView(initialRadix = 10)
        ToolId.OCTAL -> RadixToolView(initialRadix = 8)
        ToolId.HEXADECIMAL -> RadixToolView(initialRadix = 16)
        ToolId.AGE -> AgeToolView()
        ToolId.DATE_DIFF -> DateDiffToolView(showWorkdaysEmphasis = false)
        ToolId.DAYS_BETWEEN -> DateDiffToolView(showWorkdaysEmphasis = true)
        ToolId.TIME_DURATION -> TimeDurationToolView()
    }
}

@Composable
private fun ToolField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    placeholder: String = ""
) {
    val colors = CucalcusTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = colors.secondaryText) },
        placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder, color = colors.secondaryText.copy(alpha = 0.6f)) } } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.border,
            focusedTextColor = colors.primaryText,
            unfocusedTextColor = colors.primaryText,
            cursorColor = colors.accent
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ToolResultPanel(
    title: String,
    rows: List<Pair<String, String>>,
    error: String? = null
) {
    val colors = CucalcusTheme.colors
    val displayFont = LocalDisplayFontFamily.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBg,
        border = BorderStroke(1.dp, if (error != null) colors.error.copy(alpha = 0.6f) else colors.accent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (error != null) colors.error else colors.accent
            )
            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodyLarge, color = colors.error)
            } else {
                rows.forEachIndexed { idx, (k, v) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(k, style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText)
                        Text(
                            text = v,
                            fontFamily = if (idx == 0) displayFont else MaterialTheme.typography.bodyLarge.fontFamily,
                            style = if (idx == 0) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primaryText
                        )
                    }
                    if (idx < rows.lastIndex) {
                        HorizontalDivider(thickness = 1.dp, color = colors.border)
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionChipRow(
    options: List<Pair<String, Boolean>>,
    onSelect: (Int) -> Unit
) {
    val colors = CucalcusTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { idx, (label, selected) ->
            Surface(
                onClick = { onSelect(idx) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) colors.accent.copy(alpha = 0.16f) else colors.cardBg,
                border = BorderStroke(1.dp, if (selected) colors.accent else colors.border),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) colors.accent else colors.secondaryText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                )
            }
        }
    }
}

private fun fmt(v: Double, decimals: Int = 2): String =
    NumberFormatter.formatResult(v, decimals, NumberFormatMode.INTERNATIONAL)

@Composable
private fun GstToolView() {
    var amount by remember { mutableStateOf("1000") }
    var rate by remember { mutableStateOf("18") }
    var isInclusive by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OptionChipRow(
            options = listOf("Exclusive (+GST)" to !isInclusive, "Inclusive (Remove)" to isInclusive),
            onSelect = { isInclusive = it == 1 }
        )
        ToolField("Amount", amount, { amount = it })
        ToolField("GST Rate (%)", rate, { rate = it })
        val a = amount.toDoubleOrNull()
        val r = rate.toDoubleOrNull()
        if (a != null && r != null && a >= 0 && r >= 0) {
            val res = ToolsEngine.calculateGst(a, r, isInclusive)
            ToolResultPanel(
                "GST Breakdown",
                listOf(
                    "Total Amount" to fmt(res.totalAmount),
                    "Net Amount" to fmt(res.netAmount),
                    "Total GST ($r%)" to fmt(res.gstAmount),
                    "CGST (${r / 2}%)" to fmt(res.cgst),
                    "SGST (${r / 2}%)" to fmt(res.sgst)
                )
            )
        } else {
            ToolResultPanel("GST Breakdown", emptyList(), "Enter valid non-negative numbers")
        }
    }
}

@Composable
private fun DiscountToolView() {
    var price by remember { mutableStateOf("2500") }
    var discount by remember { mutableStateOf("20") }
    var tax by remember { mutableStateOf("0") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolField("Original Price", price, { price = it })
        ToolField("Discount (%)", discount, { discount = it })
        ToolField("Additional Tax (%)", tax, { tax = it })
        val p = price.toDoubleOrNull()
        val d = discount.toDoubleOrNull()
        val t = tax.toDoubleOrNull() ?: 0.0
        if (p != null && d != null && p >= 0 && d in 0.0..100.0) {
            val res = ToolsEngine.calculateDiscount(p, d, t)
            ToolResultPanel(
                "Discount Summary",
                listOf(
                    "Final Price" to fmt(res.finalPrice),
                    "You Save" to fmt(res.savings)
                )
            )
        } else {
            ToolResultPanel("Discount Summary", emptyList(), "Enter valid price and discount (0–100%)")
        }
    }
}

@Composable
private fun ProfitToolView() {
    var cost by remember { mutableStateOf("800") }
    var sell by remember { mutableStateOf("1000") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolField("Cost Price", cost, { cost = it })
        ToolField("Selling Price", sell, { sell = it })
        val c = cost.toDoubleOrNull()
        val s = sell.toDoubleOrNull()
        if (c != null && s != null && c > 0) {
            val res = ToolsEngine.calculateProfit(c, s)
            ToolResultPanel(
                "Profit Analysis",
                listOf(
                    "Profit Amount" to fmt(res.profit),
                    "Markup (%)" to "${fmt(res.markupPercent)}%",
                    "Profit Margin (%)" to "${fmt(res.marginPercent)}%"
                )
            )
        } else {
            ToolResultPanel("Profit Analysis", emptyList(), "Enter valid positive cost price")
        }
    }
}

@Composable
private fun LossToolView() {
    var cost by remember { mutableStateOf("1000") }
    var sell by remember { mutableStateOf("850") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolField("Cost Price", cost, { cost = it })
        ToolField("Selling Price", sell, { sell = it })
        val c = cost.toDoubleOrNull()
        val s = sell.toDoubleOrNull()
        if (c != null && s != null && c > 0) {
            val res = ToolsEngine.calculateLoss(c, s)
            ToolResultPanel(
                "Loss Analysis",
                listOf(
                    "Loss Amount" to fmt(res.loss),
                    "Loss Percentage" to "${fmt(res.lossPercent)}%"
                )
            )
        } else {
            ToolResultPanel("Loss Analysis", emptyList(), "Enter valid positive cost price")
        }
    }
}

@Composable
private fun InterestToolView(isCompound: Boolean) {
    var principal by remember { mutableStateOf("10000") }
    var rate by remember { mutableStateOf("7.5") }
    var years by remember { mutableStateOf("5") }
    var freqIdx by remember { mutableStateOf(2) } // 1=Annual, 2=Semi, 4=Quarterly, 12=Monthly
    val freqs = listOf("Yearly" to 1, "Quarterly" to 4, "Monthly" to 12)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolField("Principal Amount", principal, { principal = it })
        ToolField("Annual Rate (%)", rate, { rate = it })
        ToolField("Time Period (Years)", years, { years = it })
        if (isCompound) {
            OptionChipRow(
                options = freqs.mapIndexed { idx, pair -> pair.first to (freqIdx == idx) },
                onSelect = { freqIdx = it }
            )
        }
        val p = principal.toDoubleOrNull()
        val r = rate.toDoubleOrNull()
        val y = years.toDoubleOrNull()
        if (p != null && r != null && y != null && p >= 0 && r >= 0 && y >= 0) {
            val res = if (isCompound) {
                ToolsEngine.calculateCompoundInterest(p, r, y, freqs[freqIdx].second)
            } else {
                ToolsEngine.calculateSimpleInterest(p, r, y)
            }
            ToolResultPanel(
                if (isCompound) "Compound Interest" else "Simple Interest",
                listOf(
                    "Total Maturity" to fmt(res.totalAmount),
                    "Interest Earned" to fmt(res.interest),
                    "Principal" to fmt(p)
                )
            )
        } else {
            ToolResultPanel("Interest", emptyList(), "Enter valid non-negative numbers")
        }
    }
}

@Composable
private fun EmiToolView() {
    var principal by remember { mutableStateOf("500000") }
    var rate by remember { mutableStateOf("8.5") }
    var months by remember { mutableStateOf("60") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolField("Loan Principal", principal, { principal = it })
        ToolField("Annual Interest Rate (%)", rate, { rate = it })
        ToolField("Tenure (Months)", months, { months = it }, KeyboardType.Number)
        val p = principal.toDoubleOrNull()
        val r = rate.toDoubleOrNull()
        val m = months.toIntOrNull()
        if (p != null && r != null && m != null && p > 0 && r >= 0 && m > 0) {
            val res = ToolsEngine.calculateEmi(p, r, m)
            ToolResultPanel(
                "EMI Schedule Summary",
                listOf(
                    "Monthly EMI" to fmt(res.monthlyEmi),
                    "Total Interest" to fmt(res.totalInterest),
                    "Total Payable" to fmt(res.totalPayment)
                )
            )
        } else {
            ToolResultPanel("EMI Summary", emptyList(), "Enter positive loan amount and months")
        }
    }
}

@Composable
private fun TipToolView() {
    var bill by remember { mutableStateOf("120") }
    var tip by remember { mutableStateOf("15") }
    var people by remember { mutableStateOf("2") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolField("Bill Amount", bill, { bill = it })
        ToolField("Tip (%)", tip, { tip = it })
        ToolField("Split Between (People)", people, { people = it }, KeyboardType.Number)
        val b = bill.toDoubleOrNull()
        val t = tip.toDoubleOrNull()
        val n = people.toIntOrNull()
        if (b != null && t != null && n != null && b >= 0 && t >= 0 && n >= 1) {
            val res = ToolsEngine.calculateTip(b, t, n)
            ToolResultPanel(
                "Tip & Split",
                listOf(
                    "Per Person" to fmt(res.perPerson),
                    "Total Bill" to fmt(res.totalBill),
                    "Tip Amount" to fmt(res.tipAmount)
                )
            )
        } else {
            ToolResultPanel("Tip & Split", emptyList(), "Enter valid bill, tip %, and at least 1 person")
        }
    }
}

@Composable
private fun PercentageToolView() {
    var x by remember { mutableStateOf("25") }
    var y by remember { mutableStateOf("200") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolField("Value X", x, { x = it })
        ToolField("Value Y", y, { y = it })
        val xv = x.toDoubleOrNull()
        val yv = y.toDoubleOrNull()
        if (xv != null && yv != null) {
            val res = ToolsEngine.calculatePercentages(xv, yv)
            ToolResultPanel(
                "Percentage Metrics",
                listOf(
                    "$xv% of $yv" to fmt(res.percentOfY, 4),
                    "$xv is what % of $yv" to "${fmt(res.xIsWhatPercentOfY, 4)}%",
                    "% Change ($xv → $yv)" to "${fmt(res.percentChange, 4)}%"
                )
            )
        } else {
            ToolResultPanel("Percentage Metrics", emptyList(), "Enter valid numbers for X and Y")
        }
    }
}

@Composable
private fun FractionToolView() {
    var n1 by remember { mutableStateOf("1") }
    var d1 by remember { mutableStateOf("2") }
    var n2 by remember { mutableStateOf("1") }
    var d2 by remember { mutableStateOf("3") }
    var opIdx by remember { mutableStateOf(0) }
    val ops = ToolsEngine.FractionOp.entries
    val labels = listOf("+", "−", "×", "÷")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ToolField("Num 1", n1, { n1 = it }, KeyboardType.Ascii)
                ToolField("Den 1", d1, { d1 = it }, KeyboardType.Ascii)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ToolField("Num 2", n2, { n2 = it }, KeyboardType.Ascii)
                ToolField("Den 2", d2, { d2 = it }, KeyboardType.Ascii)
            }
        }
        OptionChipRow(
            options = labels.mapIndexed { idx, s -> s to (opIdx == idx) },
            onSelect = { opIdx = it }
        )
        val a = n1.trim().toLongOrNull()
        val b = d1.trim().toLongOrNull()
        val c = n2.trim().toLongOrNull()
        val d = d2.trim().toLongOrNull()
        if (a != null && b != null && c != null && d != null) {
            ToolsEngine.calculateFraction(a, b, c, d, ops[opIdx]).fold(
                onSuccess = { frac ->
                    ToolResultPanel(
                        "Simplified Fraction",
                        listOf(
                            "Fraction" to frac.formatted(),
                            "Decimal" to fmt(frac.decimal, 6)
                        )
                    )
                },
                onFailure = { ToolResultPanel("Fraction", emptyList(), it.message) }
            )
        } else {
            ToolResultPanel("Fraction", emptyList(), "Enter valid integer numerators and denominators")
        }
    }
}

@Composable
private fun RatioToolView() {
    var a by remember { mutableStateOf("16") }
    var b by remember { mutableStateOf("9") }
    var c by remember { mutableStateOf("1920") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) { ToolField("A", a, { a = it }, KeyboardType.Ascii) }
            Column(modifier = Modifier.weight(1f)) { ToolField("B", b, { b = it }, KeyboardType.Ascii) }
        }
        ToolField("Proportion C (A:B = C:D, optional)", c, { c = it })
        val av = a.toLongOrNull()
        val bv = b.toLongOrNull()
        val cv = c.toDoubleOrNull()
        if (av != null && bv != null && (av != 0L || bv != 0L)) {
            val res = ToolsEngine.calculateRatio(av, bv, cv)
            val rows = buildList {
                add("Simplified Ratio" to "${res.simplifiedA} : ${res.simplifiedB}")
                if (res.solvedD != null) add("Solved D" to fmt(res.solvedD, 4))
            }
            ToolResultPanel("Ratio & Proportion", rows)
        } else {
            ToolResultPanel("Ratio", emptyList(), "Enter valid integers for A and B")
        }
    }
}

@Composable
private fun AverageToolView() {
    var input by remember { mutableStateOf("12, 18, 24, 30, 42") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolField("Numbers (comma or space separated)", input, { input = it }, KeyboardType.Text)
        val numbers = input.split(Regex("[,\\s]+")).mapNotNull { it.trim().toDoubleOrNull() }
        val res = ToolsEngine.calculateAverage(numbers)
        if (res != null) {
            ToolResultPanel(
                "Statistical Summary",
                listOf(
                    "Mean (Average)" to fmt(res.mean, 4),
                    "Median" to fmt(res.median, 4),
                    "Sum" to fmt(res.sum, 4),
                    "Count" to "${res.count}",
                    "Range" to "${fmt(res.min, 2)} – ${fmt(res.max, 2)}"
                )
            )
        } else {
            ToolResultPanel("Statistical Summary", emptyList(), "Enter at least one valid number")
        }
    }
}

@Composable
private fun PrimeToolView() {
    var input by remember { mutableStateOf("97") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolField("Integer to Check", input, { input = it }, KeyboardType.Number)
        val n = input.toLongOrNull()
        if (n != null && n >= 0 && n <= 1_000_000_000_000L) {
            val res = ToolsEngine.checkPrime(n)
            ToolResultPanel(
                "Primality & Factors",
                listOf(
                    "Status" to if (res.isPrime) "$n is PRIME" else "$n is NOT prime",
                    "Prime Factors" to if (res.factors.isEmpty()) "None" else res.factors.joinToString(" × ")
                )
            )
        } else {
            ToolResultPanel("Primality", emptyList(), "Enter a positive integer up to 10¹²")
        }
    }
}

@Composable
private fun LcmHcfToolView(isLcm: Boolean) {
    var input by remember { mutableStateOf("24, 36, 60") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolField("Integers (comma or space separated)", input, { input = it }, KeyboardType.Text)
        val nums = input.split(Regex("[,\\s]+")).mapNotNull { it.trim().toLongOrNull() }.filter { it != 0L }
        if (nums.size >= 2) {
            val lcm = ToolsEngine.lcmList(nums)
            val gcd = ToolsEngine.gcdList(nums)
            ToolResultPanel(
                if (isLcm) "Least Common Multiple" else "Highest Common Factor",
                if (isLcm) {
                    listOf("LCM" to "${lcm ?: 0}", "HCF / GCD" to "${gcd ?: 0}")
                } else {
                    listOf("HCF / GCD" to "${gcd ?: 0}", "LCM" to "${lcm ?: 0}")
                }
            )
        } else {
            ToolResultPanel("Result", emptyList(), "Enter at least two non-zero integers")
        }
    }
}

@Composable
private fun RadixToolView(initialRadix: Int) {
    var radix by remember(initialRadix) { mutableStateOf(initialRadix) }
    var input by remember(initialRadix) {
        mutableStateOf(
            when (initialRadix) {
                2 -> "101010"
                8 -> "52"
                16 -> "2A"
                else -> "42"
            }
        )
    }
    val bases = listOf("BIN" to 2, "OCT" to 8, "DEC" to 10, "HEX" to 16)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OptionChipRow(
            options = bases.map { it.first to (radix == it.second) },
            onSelect = { idx -> radix = bases[idx].second }
        )
        ToolField("Input (Base-$radix)", input, { input = it }, KeyboardType.Ascii)
        ToolsEngine.convertRadix(input, radix).fold(
            onSuccess = { res ->
                ToolResultPanel(
                    "Number Base Equivalents",
                    listOf(
                        "Decimal (10)" to res.decimal,
                        "Binary (2)" to res.binary,
                        "Octal (8)" to res.octal,
                        "Hexadecimal (16)" to res.hexadecimal
                    )
                )
            },
            onFailure = { ToolResultPanel("Base Converter", emptyList(), it.message) }
        )
    }
}

@Composable
private fun AgeToolView() {
    var birth by remember { mutableStateOf("1998-05-14") }
    var asOf by remember { mutableStateOf(LocalDate.now().toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolField("Birth Date (YYYY-MM-DD)", birth, { birth = it }, KeyboardType.Ascii, "1998-05-14")
        ToolField("Target Date (YYYY-MM-DD)", asOf, { asOf = it }, KeyboardType.Ascii, "2026-11-17")
        val parsed = try {
            LocalDate.parse(birth.trim()) to LocalDate.parse(asOf.trim())
        } catch (_: DateTimeParseException) {
            null
        }
        if (parsed != null) {
            ToolsEngine.calculateAge(parsed.first, parsed.second).fold(
                onSuccess = { res ->
                    ToolResultPanel(
                        "Exact Age",
                        listOf(
                            "Age" to "${res.years}y ${res.months}m ${res.days}d",
                            "Total Days Lived" to NumberFormatter.formatResult(res.totalDays.toDouble(), 0),
                            "Next Birthday In" to "${res.daysUntilNextBirthday} days"
                        )
                    )
                },
                onFailure = { ToolResultPanel("Age", emptyList(), it.message) }
            )
        } else {
            ToolResultPanel("Age", emptyList(), "Use YYYY-MM-DD format (e.g. 1998-05-14)")
        }
    }
}

@Composable
private fun DateDiffToolView(showWorkdaysEmphasis: Boolean) {
    var start by remember { mutableStateOf(LocalDate.now().minusMonths(3).toString()) }
    var end by remember { mutableStateOf(LocalDate.now().toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolField("Start Date (YYYY-MM-DD)", start, { start = it }, KeyboardType.Ascii)
        ToolField("End Date (YYYY-MM-DD)", end, { end = it }, KeyboardType.Ascii)
        val parsed = try {
            LocalDate.parse(start.trim()) to LocalDate.parse(end.trim())
        } catch (_: DateTimeParseException) {
            null
        }
        if (parsed != null) {
            val res = ToolsEngine.calculateDateDifference(parsed.first, parsed.second)
            val rows = if (showWorkdaysEmphasis) {
                listOf(
                    "Total Days" to "${res.totalDays} days",
                    "Weeks" to fmt(res.weeks, 2),
                    "Weekdays (Mon–Fri)" to "${res.weekdays} days",
                    "Calendar Span" to "${res.years}y ${res.months}m ${res.days}d"
                )
            } else {
                listOf(
                    "Difference" to "${res.years}y ${res.months}m ${res.days}d",
                    "Total Days" to "${res.totalDays} days",
                    "Weeks" to fmt(res.weeks, 2),
                    "Weekdays" to "${res.weekdays} days"
                )
            }
            ToolResultPanel(if (showWorkdaysEmphasis) "Days Between Dates" else "Date Difference", rows)
        } else {
            ToolResultPanel("Date Span", emptyList(), "Use YYYY-MM-DD format")
        }
    }
}

@Composable
private fun TimeDurationToolView() {
    var start by remember { mutableStateOf("09:30:00") }
    var end by remember { mutableStateOf("17:45:30") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolField("Start Time (HH:MM or HH:MM:SS)", start, { start = it }, KeyboardType.Ascii)
        ToolField("End Time (HH:MM or HH:MM:SS)", end, { end = it }, KeyboardType.Ascii)
        val parsed = try {
            LocalTime.parse(start.trim()) to LocalTime.parse(end.trim())
        } catch (_: DateTimeParseException) {
            null
        }
        if (parsed != null) {
            val res = ToolsEngine.calculateTimeDuration(parsed.first, parsed.second)
            ToolResultPanel(
                "Elapsed Duration",
                listOf(
                    "Duration" to "${res.hours}h ${res.minutes}m ${res.seconds}s",
                    "Total Minutes" to "${res.totalMinutes} min",
                    "Total Seconds" to "${res.totalSeconds} sec"
                )
            )
        } else {
            ToolResultPanel("Elapsed Duration", emptyList(), "Use 24h HH:MM or HH:MM:SS format")
        }
    }
}
