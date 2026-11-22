package com.example.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.preferences.HapticStrength

class HapticManager(context: Context) {
    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Throwable) {
        null
    }

    val isSupported: Boolean
        get() = try {
            vibrator?.hasVibrator() == true
        } catch (_: Throwable) {
            false
        }

    fun performTick(strength: HapticStrength, enabled: Boolean) {
        if (!enabled || !isSupported) return
        try {
            val vib = vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use Android's native touch-tick primitive first. Some devices
                // suppress very short custom one-shot vibrations but still play
                // their system haptic primitive reliably.
                val effect = when (strength) {
                    HapticStrength.GENTLE -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    HapticStrength.MEDIUM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    HapticStrength.CRISP -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                }
                vib.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val (durationMs, amplitude) = when (strength) {
                    HapticStrength.GENTLE -> 12L to 75
                    HapticStrength.MEDIUM -> 20L to 150
                    HapticStrength.CRISP -> 28L to 235
                }
                val effectiveAmp = if (vib.hasAmplitudeControl()) amplitude else VibrationEffect.DEFAULT_AMPLITUDE
                vib.vibrate(VibrationEffect.createOneShot(durationMs, effectiveAmp))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(
                    when (strength) {
                        HapticStrength.GENTLE -> 10L
                        HapticStrength.MEDIUM -> 16L
                        HapticStrength.CRISP -> 24L
                    }
                )
            }
        } catch (_: Throwable) {
            // Respect device constraints silently
        }
    }
}
