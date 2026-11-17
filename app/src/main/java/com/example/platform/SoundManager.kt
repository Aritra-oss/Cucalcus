package com.example.platform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.preferences.ClickSoundStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates crisp, low-latency synthesized tactile click audio buffers
 * for the four click sound styles without requiring external audio files.
 */
class SoundManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sampleRate = 22050
    private val cachedSamples: Map<ClickSoundStyle, ShortArray> by lazy {
        ClickSoundStyle.entries.associateWith { generateWaveform(it) }
    }

    fun playClick(style: ClickSoundStyle, volume: Float, enabled: Boolean) {
        if (!enabled || volume <= 0.01f) return
        val clampedVol = volume.coerceIn(0f, 1f)
        scope.launch {
            try {
                val basePcm = cachedSamples[style] ?: return@launch
                val scaled = ShortArray(basePcm.size) { i ->
                    (basePcm[i] * clampedVol).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(scaled.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(scaled, 0, scaled.size)
                track.play()
                // Release safely after playback completes
                val durationMs = (scaled.size * 1000L) / sampleRate + 35L
                kotlinx.coroutines.delay(durationMs)
                track.stop()
                track.release()
            } catch (_: Throwable) {
                // Ignore audio hardware limitations on headless/test devices
            }
        }
    }

    private fun generateWaveform(style: ClickSoundStyle): ShortArray {
        val durationSec = when (style) {
            ClickSoundStyle.SOFT_CLICK -> 0.014
            ClickSoundStyle.MECHANICAL_CLICK -> 0.022
            ClickSoundStyle.DIGITAL_CLICK -> 0.018
            ClickSoundStyle.CLASSIC_CALCULATOR -> 0.016
        }
        val count = (sampleRate * durationSec).toInt()
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val signal = when (style) {
                ClickSoundStyle.SOFT_CLICK -> {
                    val env = exp(-t * 320.0)
                    sin(2.0 * PI * 680.0 * t) * env * 0.45
                }
                ClickSoundStyle.MECHANICAL_CLICK -> {
                    val env1 = exp(-t * 450.0)
                    val env2 = exp(-t * 160.0)
                    val click = sin(2.0 * PI * 2200.0 * t) * env1 * 0.5 +
                        sin(2.0 * PI * 420.0 * t) * env2 * 0.45
                    click
                }
                ClickSoundStyle.DIGITAL_CLICK -> {
                    val env = exp(-t * 280.0)
                    val tone = sin(2.0 * PI * 1480.0 * t) + 0.3 * sin(2.0 * PI * 2960.0 * t)
                    tone * env * 0.35
                }
                ClickSoundStyle.CLASSIC_CALCULATOR -> {
                    val env = exp(-t * 340.0)
                    val tone = sin(2.0 * PI * 980.0 * t) + 0.25 * sin(2.0 * PI * 1960.0 * t)
                    tone * env * 0.45
                }
            }
            samples[i] = (signal * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return samples
    }
}
