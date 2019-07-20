package com.francescozoccheddu.knob

import android.content.res.Resources
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.FloatRange
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

internal val Float.dp
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics)

internal fun hsv(
    @FloatRange(from = 0.0, to = 360.0) hue: Float, @FloatRange(from = 0.0, to = 1.0) saturation: Float, @FloatRange(from = 0.0, to = 1.0)
    value: Float, @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1f
                ) = Color.HSVToColor((alpha * 255).roundToInt(), floatArrayOf(hue, saturation, value))

internal fun smooth(
    current: Float, target: Float, @FloatRange(from = 0.0) smoothness: Float, @FloatRange(from = 0.0) elapsedTime: Float
                   ): Float = if (smoothness == 0f) target else lerp(current, target, min(elapsedTime / smoothness, 1f))

internal fun lerp(from: Float, to: Float, progress: Float) = from * (1 - progress) + to * progress

internal fun Float.clamp(min: Float, max: Float) = if (this < min) min else if (this > max) max else this

internal fun Float.ceilToInt() = ceil(this).toInt()

internal fun Float.snap(target: Float, @FloatRange(from = 0.0) threshold: Float) = if (abs(this - target) <= threshold) target else this

internal val Int.f get() = toFloat()
internal val Int.d get() = toDouble()
internal val Float.d get() = toDouble()
internal val Double.f get() = toFloat()