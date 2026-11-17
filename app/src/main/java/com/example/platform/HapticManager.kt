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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val (durationMs, amplitude) = when (strength) {
                    HapticStrength.GENTLE -> 10L to 65
                    HapticStrength.MEDIUM -> 16L to 140
                    HapticStrength.CRISP -> 24L to 235
                }
                val effectiveAmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vib.hasAmplitudeControl()) {
                    amplitude
                } else {
                    VibrationEffect.DEFAULT_AMPLITUDE
                }
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
