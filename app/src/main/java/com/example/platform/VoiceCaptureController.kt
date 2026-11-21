package com.example.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Wraps Android SpeechRecognizer lifecycle cleanly and provides an optional RecognizerIntent builder.
 */
class VoiceCaptureController(
    private val context: Context,
    private val onPartialTranscript: (String) -> Unit,
    private val onFinalTranscript: (String) -> Unit,
    private val onCaptureError: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        stopAndDestroy()
        if (!isAvailable()) {
            onCaptureError("Speech service unavailable")
            return
        }
        val instance = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = instance
        instance.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val first = matches?.firstOrNull()?.trim().orEmpty()
                if (first.isNotEmpty()) {
                    onPartialTranscript(first)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull()?.trim().orEmpty()
                stopAndDestroy()
                if (spoken.isNotEmpty()) {
                    onFinalTranscript(spoken)
                } else {
                    onCaptureError("No speech detected. Try again.")
                }
            }

            override fun onError(error: Int) {
                stopAndDestroy()
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Could not hear clearly. Please try again."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech network error. Please try again."
                    else -> "Voice input error ($error)."
                }
                onCaptureError(msg)
            }
        })

        val intent = createRecognizerIntent()
        runCatching {
            instance.startListening(intent)
        }.onFailure {
            stopAndDestroy()
            onCaptureError("Could not start microphone.")
        }
    }

    fun finishListening() {
        runCatching { recognizer?.stopListening() }
    }

    fun stopAndDestroy() {
        val current = recognizer
        recognizer = null
        if (current != null) {
            runCatching { current.cancel() }
            runCatching { current.destroy() }
        }
    }

    companion object {
        fun createRecognizerIntent(): Intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a calculation or educational question")
            }

        fun extractActivityResultTranscript(resultCode: Int, data: Intent?): String? {
            if (resultCode != Activity.RESULT_OK || data == null) return null
            return data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }
    }
}
