package com.example

import com.example.domain.converter.UnitCategory
import com.example.domain.converter.UnitConverterEngine
import com.example.domain.math.AngleMode
import com.example.domain.math.EvalResult
import com.example.domain.math.ExpressionParser
import com.example.domain.math.MathErrorType
import com.example.domain.math.NumberFormatMode
import com.example.domain.math.NumberFormatter
import com.example.domain.tools.ToolsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.E
import kotlin.math.PI

class ExampleUnitTest {

    @Test
    fun `expression parser precedence and parentheses`() {
        val r1 = ExpressionParser.evaluate("2+3×4") as EvalResult.Success
        assertEquals(14.0, r1.value, 1e-9)

        val r2 = ExpressionParser.evaluate("(2+3)×4^2") as EvalResult.Success
        assertEquals(80.0, r2.value, 1e-9)

        val r3 = ExpressionParser.evaluate("2^3^2") as EvalResult.Success
        assertEquals(512.0, r3.value, 1e-9) // Right-associative
    }

    @Test
    fun `expression parser euler e vs scientific notation and adjacent constants`() {
        // Lowercase e is always Euler's constant even when followed by +3 or -1
        val eulerAdd = ExpressionParser.evaluate("2e+3") as EvalResult.Success
        assertEquals(2.0 * E + 3.0, eulerAdd.value, 1e-9)

        val eulerSub = ExpressionParser.evaluate("2e−1") as EvalResult.Success
        assertEquals(2.0 * E - 1.0, eulerSub.value, 1e-9)

        // Adjacent constants & functions multiply implicitly without token collision
        val piEuler = ExpressionParser.evaluate("πe") as EvalResult.Success
        assertEquals(PI * E, piEuler.value, 1e-9)

        val eulerPi = ExpressionParser.evaluate("eπ") as EvalResult.Success
        assertEquals(E * PI, eulerPi.value, 1e-9)

        val sciLiteral = ExpressionParser.evaluate("1.5E+3") as EvalResult.Success
        assertEquals(1500.0, sciLiteral.value, 1e-9)

        // Very small non-zero results are preserved rather than clamped to 0.0
        val tiny = ExpressionParser.evaluate("10^−16") as EvalResult.Success
        assertEquals(1e-16, tiny.value, 1e-22)
    }

    @Test
    fun `expression parser scientific and percentage and errors`() {
        val sin30 = ExpressionParser.evaluate("sin(30)", AngleMode.DEG) as EvalResult.Success
        assertEquals(0.5, sin30.value, 1e-9)

        val sin180 = ExpressionParser.evaluate("sin(180)", AngleMode.DEG) as EvalResult.Success
        assertEquals(0.0, sin180.value, 1e-12)

        val cosPi = ExpressionParser.evaluate("cos(π)", AngleMode.RAD) as EvalResult.Success
        assertEquals(-1.0, cosPi.value, 1e-9)

        val fact = ExpressionParser.evaluate("5!") as EvalResult.Success
        assertEquals(120.0, fact.value, 1e-9)

        val pct = ExpressionParser.evaluate("200+15%") as EvalResult.Success
        assertEquals(230.0, pct.value, 1e-9)

        val divZero = ExpressionParser.evaluate("10÷0") as EvalResult.Error
        assertEquals(MathErrorType.DIVISION_BY_ZERO, divZero.type)

        val domainErr = ExpressionParser.evaluate("√(−9)") as EvalResult.Error
        assertEquals(MathErrorType.DOMAIN_ERROR, domainErr.type)
    }

    @Test
    fun `number formatter grouping modes and negative zero handling`() {
        assertEquals("1,234,567.89", NumberFormatter.formatResult(1234567.89, 2, NumberFormatMode.INTERNATIONAL))
        assertEquals("12,34,567.89", NumberFormatter.formatResult(1234567.89, 2, NumberFormatMode.INDIAN))
        assertTrue(NumberFormatter.formatResult(1234567.0, 2, NumberFormatMode.SCIENTIFIC).contains("e"))
        assertEquals("0", NumberFormatter.formatResult(-0.0, 4, NumberFormatMode.INTERNATIONAL))
        assertEquals("0", NumberFormatter.formatResult(-0.0001, 2, NumberFormatMode.INTERNATIONAL))
        assertEquals("1,000+1.5E1000", NumberFormatter.formatExpressionNumbers("1000+1.5E1000", NumberFormatMode.INTERNATIONAL))
    }

    @Test
    fun `unit converter covers all 15 categories accurately`() {
        assertEquals(15, UnitCategory.entries.size)
        for (cat in UnitCategory.entries) {
            val units = UnitConverterEngine.unitsByCategory[cat]!!
            assertTrue("Category $cat should have >= 2 units", units.size >= 2)
        }
        val c = UnitConverterEngine.findUnitById("c")!!
        val f = UnitConverterEngine.findUnitById("f")!!
        assertEquals(212.0, UnitConverterEngine.convert(100.0, c, f), 1e-9)
        assertEquals(-40.0, UnitConverterEngine.convert(-40.0, c, f), 1e-9)

        val kmh = UnitConverterEngine.findUnitById("kmh")!!
        val mps = UnitConverterEngine.findUnitById("mps")!!
        assertEquals(10.0, UnitConverterEngine.convert(36.0, kmh, mps), 1e-9)

        val kml = UnitConverterEngine.findUnitById("kml")!!
        val l100 = UnitConverterEngine.findUnitById("l100km")!!
        assertEquals(5.0, UnitConverterEngine.convert(20.0, kml, l100), 1e-9)

        val kib = UnitConverterEngine.findUnitById("kib")!!
        val bIec = UnitConverterEngine.findUnitById("b_iec")!!
        assertEquals(1024.0, UnitConverterEngine.convert(1.0, kib, bIec), 1e-9)
    }

    @Test
    fun `tools engine computes financial math and date calculations`() {
        val gst = ToolsEngine.calculateGst(1000.0, 18.0, isInclusive = false)
        assertEquals(1180.0, gst.totalAmount, 1e-9)
        assertEquals(90.0, gst.cgst, 1e-9)

        val emi = ToolsEngine.calculateEmi(120000.0, 0.0, 12)
        assertEquals(10000.0, emi.monthlyEmi, 1e-9)

        val frac = ToolsEngine.calculateFraction(1, 2, 1, 3, ToolsEngine.FractionOp.ADD).getOrThrow()
        assertEquals("5 / 6", frac.formatted())

        assertTrue(ToolsEngine.checkPrime(97).isPrime)
        assertFalse(ToolsEngine.checkPrime(98).isPrime)
        assertEquals(12L, ToolsEngine.gcdList(listOf(24, 36, 60)))
        assertEquals(360L, ToolsEngine.lcmList(listOf(24, 36, 60)))

        val radix = ToolsEngine.convertRadix("2A", 16).getOrThrow()
        assertEquals("42", radix.decimal)
        assertEquals("101010", radix.binary)

        val diff = ToolsEngine.calculateDateDifference(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 8))
        assertEquals(7L, diff.totalDays)

        val dur = ToolsEngine.calculateTimeDuration(LocalTime.of(23, 0), LocalTime.of(1, 30))
        assertEquals(2L, dur.hours)
        assertEquals(30L, dur.minutes)
    }
}
