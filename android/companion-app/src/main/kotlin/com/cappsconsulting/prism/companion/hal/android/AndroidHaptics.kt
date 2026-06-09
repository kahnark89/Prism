package com.cappsconsulting.prism.companion.hal.android

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import com.cappsconsulting.prism.companion.hal.HapticGesture
import com.cappsconsulting.prism.companion.hal.HapticOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [VibrationEffect] implementation of [HapticOutput] — replatform item 1's haptics half.
 *
 * Two rendering tiers, exactly as [HapticOutput.supportsComposition] documents:
 *
 * **API 30+ (Android R):** [VibrationEffect.startComposition] with named primitives
 * (`PRIMITIVE_SLOW_RISE`, `PRIMITIVE_QUICK_FALL`, `PRIMITIVE_THUD`, `PRIMITIVE_TICK`).
 * These map to distinct, felt gestures on supported actuators — a real heartbeat waveform
 * ([HapticGesture.HEARTBEAT]'s "soft-then-sharp double-pulse with the right rhythm") is
 * achievable here; a generic buzz is not. Doc 2.3 §2's design premise.
 *
 * **API 28–29 (our minSdk to Android Q):** [VibrationEffect.createWaveform] with explicit
 * timing + amplitude arrays — shaped but not primitive-based. Still produces a discernible
 * double-pulse for [HapticGesture.HEARTBEAT] vs. a brief tick for [HapticGesture.SPARK_TICK]
 * vs. a single medium pulse for [HapticGesture.SINGLE_PULSE]; just without the hardware's
 * named waveform vocabulary.
 *
 * `intensity` maps linearly to amplitude 1–255 ([VibrationEffect.DEFAULT_AMPLITUDE] is used
 * only as a fallback if amplitude control isn't available).
 */
class AndroidHaptics(context: Context) : HapticOutput {

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override val supportsComposition: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.hasAmplitudeControl()

    override suspend fun play(gesture: HapticGesture, intensity: Float) =
        withContext(Dispatchers.Default) {
            val amplitude = (intensity * 255).toInt().coerceIn(1, 255)
            val effect = if (supportsComposition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                compositionEffect(gesture, intensity)
            } else {
                waveformEffect(gesture, amplitude)
            }
            vibrator.vibrate(effect)
        }

    override suspend fun off() = withContext(Dispatchers.Default) {
        vibrator.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun compositionEffect(gesture: HapticGesture, intensity: Float): VibrationEffect =
        when (gesture) {
            HapticGesture.SPARK_TICK ->
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity * 0.35f)
                    .compose()

            HapticGesture.HEARTBEAT ->
                // Soft rise → sharp fall → gap → soft rise → sharp fall → decay
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, intensity * 0.3f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, intensity * 0.75f, 15)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, intensity * 0.3f, 110)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, intensity * 0.75f, 15)
                    .compose()

            HapticGesture.SINGLE_PULSE ->
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, intensity * 0.5f)
                    .compose()
        }

    private fun waveformEffect(gesture: HapticGesture, amplitude: Int): VibrationEffect =
        when (gesture) {
            HapticGesture.SPARK_TICK ->
                VibrationEffect.createOneShot(15, (amplitude / 3).coerceAtLeast(1))

            HapticGesture.HEARTBEAT ->
                // timings: [off, on(soft), off, on(strong), silence]
                VibrationEffect.createWaveform(
                    longArrayOf(0, 70, 30, 110, 380),
                    intArrayOf(0, amplitude / 3, 0, amplitude, 0),
                    -1,
                )

            HapticGesture.SINGLE_PULSE ->
                VibrationEffect.createOneShot(85, amplitude / 2)
        }
}
