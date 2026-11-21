package com.example.domain.math

sealed interface VoiceQueryRoute {
    data class NormalCalculation(val normalizedExpression: String) : VoiceQueryRoute
    data class AiModeQuery(val spokenPrompt: String) : VoiceQueryRoute
}

/**
 * Classifies spoken queries into either local calculator evaluation
 * or educational AI Mode for conceptual/scientific/advanced questions.
 */
object VoiceMathClassifier {

    private val CONCEPTUAL_TRIGGERS = listOf(
        "what is", "what's", "whats", "value of", "explain", "define",
        "why", "how", "who", "when", "where", "prove", "theorem",
        "formula", "derivative", "integral", "limit", "matrix", "vector",
        "equation", "sine", "cosine", "tangent", "sin", "cos", "tan",
        "log", "ln", "degree", "degrees", "radian", "radians",
        "physics", "chemistry", "biology", "science", "history",
        "geography", "photosynthesis", "gravity", "newton", "quadratic",
        "atom", "molecule", "energy", "velocity", "acceleration",
        "area", "volume", "perimeter", "solve"
    )

    private val DIRECT_CALC_PREFIX = Regex(
        pattern = """^(?:calculate|compute|evaluate)\s+""",
        option = RegexOption.IGNORE_CASE
    )

    fun classify(rawSpokenText: String, angleMode: AngleMode = AngleMode.DEG): VoiceQueryRoute {
        val trimmed = rawSpokenText.trim()
        if (trimmed.isEmpty()) {
            return VoiceQueryRoute.AiModeQuery(trimmed)
        }

        val lower = trimmed.lowercase()
        val isConceptualOrTrig = trimmed.contains('°') ||
            trimmed.contains('?') ||
            trimmed.contains('=') ||
            CONCEPTUAL_TRIGGERS.any { kw ->
                Regex("""\b${Regex.escape(kw)}\b""").containsMatchIn(lower) ||
                    (kw in listOf("sin", "cos", "tan", "log", "ln") && lower.contains(kw))
            }

        if (isConceptualOrTrig) {
            return VoiceQueryRoute.AiModeQuery(trimmed)
        }

        val candidate = trimmed.replace(DIRECT_CALC_PREFIX, "").trim()
        val normalized = normalizeSpokenArithmetic(candidate)

        val alphaRemainder = normalized
            .replace("sqrt", "", ignoreCase = true)
            .replace("mod", "", ignoreCase = true)
        val hasDisallowedWords = alphaRemainder.any { it.isLetter() && it != 'π' && it != 'e' }

        val evalOk = normalized.isNotBlank() &&
            !hasDisallowedWords &&
            ExpressionParser.evaluate(normalized, angleMode) is EvalResult.Success

        return if (evalOk) {
            VoiceQueryRoute.NormalCalculation(normalized)
        } else {
            VoiceQueryRoute.AiModeQuery(trimmed)
        }
    }

    fun normalizeSpokenArithmetic(input: String): String {
        var text = " ${input.trim()} "
        // Handle "square root of N" -> √(N)
        text = Regex("""\bsquare\s+root\s+of\s+([0-9]+(?:\.[0-9]+)?|\([^)]+\))""", RegexOption.IGNORE_CASE)
            .replace(text) { " √(${it.groupValues[1]}) " }
        // Handle "N percent of M" -> (N% * M)
        text = Regex("""([0-9]+(?:\.[0-9]+)?)\s*(?:percent|%)\s+of\s+""", RegexOption.IGNORE_CASE)
            .replace(text) { "(${it.groupValues[1]}/100)×" }

        val replacements = listOf(
            Regex("""\bmultiplied\s+by\b""", RegexOption.IGNORE_CASE) to " × ",
            Regex("""\bmultiply\s+by\b""", RegexOption.IGNORE_CASE) to " × ",
            Regex("""\bmultiply\b""", RegexOption.IGNORE_CASE) to " × ",
            Regex("""\btimes\b""", RegexOption.IGNORE_CASE) to " × ",
            Regex("""\binto\b""", RegexOption.IGNORE_CASE) to " × ",
            Regex("""\bdivided\s+by\b""", RegexOption.IGNORE_CASE) to " ÷ ",
            Regex("""\bdivide\s+by\b""", RegexOption.IGNORE_CASE) to " ÷ ",
            Regex("""\bdivide\b""", RegexOption.IGNORE_CASE) to " ÷ ",
            Regex("""\bover\b""", RegexOption.IGNORE_CASE) to " ÷ ",
            Regex("""\bplus\b""", RegexOption.IGNORE_CASE) to " + ",
            Regex("""\badd\b""", RegexOption.IGNORE_CASE) to " + ",
            Regex("""\bminus\b""", RegexOption.IGNORE_CASE) to " − ",
            Regex("""\bsubtract\b""", RegexOption.IGNORE_CASE) to " − ",
            Regex("""\bpercentage\b""", RegexOption.IGNORE_CASE) to "%",
            Regex("""\bpercent\b""", RegexOption.IGNORE_CASE) to "%",
            Regex("""\bopen\s+parenthes(?:is|es)\b|\bopen\s+bracket\b|\bleft\s+parenthes(?:is|es)\b""", RegexOption.IGNORE_CASE) to "(",
            Regex("""\bclose\s+parenthes(?:is|es)\b|\bclose\s+bracket\b|\bright\s+parenthes(?:is|es)\b""", RegexOption.IGNORE_CASE) to ")",
            Regex("""\bto\s+the\s+power\s+of\b|\braised\s+to\b|\bpower\b""", RegexOption.IGNORE_CASE) to "^",
            Regex("""\bsquared\b""", RegexOption.IGNORE_CASE) to "^2",
            Regex("""\bcubed\b""", RegexOption.IGNORE_CASE) to "^3",
            Regex("""\bmodulo\b""", RegexOption.IGNORE_CASE) to " mod ",
            Regex("""\bpi\b""", RegexOption.IGNORE_CASE) to "π"
        )
        for ((pattern, target) in replacements) {
            text = pattern.replace(text, target)
        }
        text = Regex("""(\d)\s+point\s+(\d)""", RegexOption.IGNORE_CASE).replace(text) {
            "${it.groupValues[1]}.${it.groupValues[2]}"
        }
        // STT frequently transcribes "66 times 67" as "66 x 67"
        text = Regex("""(\d|\)|%)\s*[xX]\s*(\d|\()""").replace(text) {
            "${it.groupValues[1]} × ${it.groupValues[2]}"
        }
        return text
            .replace("*", "×")
            .replace("/", "÷")
            .replace("-", "−")
            .replace(",", "")
            .replace(Regex("""\s+"""), "")
            .trim()
            .removeSuffix(".")
    }
}
