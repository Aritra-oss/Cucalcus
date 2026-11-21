
package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.platform.ensureFirebaseInitialized
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "AiModeSolver"

sealed interface ScanProblemStreamEvent {
    data object Loading : ScanProblemStreamEvent
    data class Streaming(val partialText: String) : ScanProblemStreamEvent
    data class Completed(
        val fullText: String,
        val finalAnswer: String?
    ) : ScanProblemStreamEvent
    data class Error(val message: String) : ScanProblemStreamEvent
}

interface ProblemScannerRepository {
    fun solveProblemFromBitmapStream(
        bitmap: Bitmap
    ): Flow<ScanProblemStreamEvent>

    fun solveProblemFromTextStream(
        problemText: String
    ): Flow<ScanProblemStreamEvent>
}

class FirebaseProblemScannerRepository(
    private val appContext: Context? = null
) : ProblemScannerRepository {

    // Initialize and verify the DEFAULT Firebase app before
    // accessing Firebase AI Logic.
    private val model by lazy {
        val ctx = requireNotNull(appContext) {
            "AI Mode requires an application Context."
        }

        val firebaseApp = ensureFirebaseInitialized(ctx)
            ?: throw IllegalStateException(
                "Firebase is not configured for this app. " +
                    "Check app/google-services.json and rebuild."
            )

        check(firebaseApp.name == FirebaseApp.DEFAULT_APP_NAME) {
            "The default Firebase app was not initialized."
        }

        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(MATH_ONLY_SYSTEM_INSTRUCTION)
            }
        )
    }

    override fun solveProblemFromBitmapStream(
        bitmap: Bitmap
    ): Flow<ScanProblemStreamEvent> = flow {

        emit(ScanProblemStreamEvent.Loading)

        val prepared = withContext(Dispatchers.Default) {
            scaleDownBitmapIfNeeded(bitmap, maxSidePx = 1280)
        }

        val promptText = """
            Inspect the image carefully.
            If it shows a mathematical, scientific, or educational problem or study question, solve or explain it cleanly step by step and end with "Final Answer: <concise result>" when there is a specific final answer.
            If it is unnecessary, non-educational, or offensive, do NOT answer it and output ONLY:
            $NOT_EDUCATIONAL_RESPONSE
        """.trimIndent()

        streamContent(
            content {
                image(prepared)
                text(promptText)
            }
        ).collect { emit(it) }
    }

    override fun solveProblemFromTextStream(
        problemText: String
    ): Flow<ScanProblemStreamEvent> = flow {

        val cleaned = problemText.trim()

        if (cleaned.isEmpty()) {
            emit(
                ScanProblemStreamEvent.Completed(
                    fullText = NOT_EDUCATIONAL_RESPONSE,
                    finalAnswer = null
                )
            )
            return@flow
        }

        emit(ScanProblemStreamEvent.Loading)

        val promptText = """
            Evaluate the following question strictly under the educational-only rule:
            "$cleaned"

            If it relates to mathematics, science (physics, chemistry, biology, computer science, astronomy, earth science), or legitimate educational/academic learning, provide a clear, concise answer (and end with "Final Answer: <value>" if it resolves to a specific calculation or value). Use readable plain text without Markdown asterisks.
            If it is unnecessary chatter, non-educational, or offensive/inappropriate, do NOT answer it and output ONLY:
            $NOT_EDUCATIONAL_RESPONSE
        """.trimIndent()

        streamContent(
            content {
                text(promptText)
            }
        ).collect { emit(it) }
    }

    private fun streamContent(
        inputContent: Content
    ): Flow<ScanProblemStreamEvent> = flow {

        val accumulated = StringBuilder()

        withTimeout(60_000L) {
            model.generateContentStream(inputContent).collect { chunk ->

                val part = chunk.text.orEmpty()

                if (part.isNotEmpty()) {
                    accumulated.append(part)

                    emit(
                        ScanProblemStreamEvent.Streaming(
                            accumulated.toString().trim()
                        )
                    )
                }
            }
        }

        val rawText = accumulated.toString().trim()

        if (rawText.isBlank()) {
            emit(
                ScanProblemStreamEvent.Error(
                    "AI Mode returned an empty answer. Please try again."
                )
            )
        } else {
            val (normalizedText, finalAns) =
                normalizeMathResponse(rawText)

            emit(
                ScanProblemStreamEvent.Completed(
                    fullText = normalizedText,
                    finalAnswer = finalAns
                )
            )
        }
    }.catch { throwable ->

        if (throwable is CancellationException) throw throwable

        Log.e(TAG, "AI Mode problem solve failed", throwable)

        val message = when (throwable) {
            is TimeoutCancellationException ->
                "AI Mode timed out after 60 seconds. Check your connection and try again."

            else ->
                throwable.localizedMessage?.takeIf {
                    it.isNotBlank()
                } ?: "Could not solve problem in AI Mode."
        }

        emit(ScanProblemStreamEvent.Error(message))
    }

    companion object {

        const val NOT_EDUCATIONAL_RESPONSE =
            "This is not an educational question."

        private val MATH_ONLY_SYSTEM_INSTRUCTION = """
            You are AI Mode inside Cucalcus, an educational calculator and study companion.
            ALLOWED TOPICS: Answer all questions related to mathematics, science (physics, chemistry, biology, earth/space science, computer science, engineering), or genuine educational/academic purposes (history, geography, literature, definitions, formulas, derivations, study help).
            REJECTION RULE: If a prompt or image is unnecessary, trivial off-topic chatter, entertainment gossip, or offensive/abusive/inappropriate, NEVER answer it, NEVER lecture or apologize. Reply with ONLY this exact string:
            $NOT_EDUCATIONAL_RESPONSE
            OUTPUT FORMAT (for valid educational questions):
            1. Keep answers crisp, structured, and easy to read on a mobile calculator display box.
            2. Use plain text and standard symbols (×, ÷, −, √, π, ², =). Avoid Markdown ** asterisks or LaTeX syntax.
            3. For calculations or problems with a final answer, end cleanly with: Final Answer: <value>
        """.trimIndent()

        internal fun normalizeMathResponse(
            responseText: String
        ): Pair<String, String?> {

            val trimmed = responseText.trim()

            val cleanedComparison = trimmed
                .removeSuffix(".")
                .removeSuffix("!")
                .trim()

            val targetBase =
                NOT_EDUCATIONAL_RESPONSE.removeSuffix(".")

            if (
                cleanedComparison.equals(
                    targetBase,
                    ignoreCase = true
                ) ||
                trimmed.startsWith(
                    targetBase,
                    ignoreCase = true
                ) ||
                cleanedComparison.equals(
                    "This is not a Educational question",
                    ignoreCase = true
                )
            ) {
                return NOT_EDUCATIONAL_RESPONSE to null
            }

            return trimmed to extractFinalAnswer(trimmed)
        }

        internal fun extractFinalAnswer(
            responseText: String
        ): String? {

            val match = Regex(
                """(?im)^final\s+answer\s*:\s*(.+)$"""
            ).find(responseText)

            val raw = match
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: return null

            return raw.removeSuffix(".")
        }

        internal fun scaleDownBitmapIfNeeded(
            bitmap: Bitmap,
            maxSidePx: Int = 1280
        ): Bitmap {

            val w = bitmap.width
            val h = bitmap.height
            val longest = max(w, h)

            if (longest <= maxSidePx) return bitmap

            val scale = maxSidePx.toFloat() / longest.toFloat()

            val targetW = max(
                1,
                (w * scale).roundToInt()
            )

            val targetH = max(
                1,
                (h * scale).roundToInt()
            )

            return Bitmap.createScaledBitmap(
                bitmap,
                targetW,
                targetH,
                true
            )
        }
    }
}

suspend fun loadScanBitmapFromUri(
    context: Context,
    uri: Uri
): Bitmap? = withContext(Dispatchers.IO) {

    runCatching {

        val rawBitmap = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        ) {

            val source = ImageDecoder.createSource(
                context.contentResolver,
                uri
            )

            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->

                decoder.allocator =
                    ImageDecoder.ALLOCATOR_SOFTWARE

                val longest = max(
                    info.size.width,
                    info.size.height
                )

                if (longest > 1600) {

                    val ratio = 1600f / longest.toFloat()

                    decoder.setTargetSize(
                        max(
                            1,
                            (info.size.width * ratio).roundToInt()
                        ),
                        max(
                            1,
                            (info.size.height * ratio).roundToInt()
                        )
                    )
                }
            }

        } else {

            context.contentResolver
                .openInputStream(uri)
                ?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
        }

        rawBitmap?.let {
            FirebaseProblemScannerRepository
                .scaleDownBitmapIfNeeded(it, 1280)
        }

    }.getOrNull()
}

fun createScanCaptureFile(context: Context): File {

    val dir = File(
        context.cacheDir,
        "scans"
    ).apply {
        mkdirs()
    }

    return File(
        dir,
        "scan_${System.currentTimeMillis()}.jpg"
    )
}
