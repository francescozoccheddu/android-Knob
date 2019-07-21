package com.francescozoccheddu.knob

import android.content.res.Resources
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.FloatRange
import kotlin.math.*

internal val Float.dp
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics)

internal fun hsv(@FloatRange(from = 0.0, to = 360.0) hue: Float, @FloatRange(from = 0.0, to = 1.0) saturation: Float,
                 @FloatRange(from = 0.0, to = 1.0)
                 value: Float, @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1f) =
    Color.HSVToColor((alpha * 255).roundToInt(), floatArrayOf(hue, saturation, value))

internal fun smooth(current: Float, target: Float, @FloatRange(from = 0.0) smoothness: Float, @FloatRange(from = 0.0) elapsedTime: Float): Float =
    if (smoothness == 0f) target else lerp(current, target, min(elapsedTime / smoothness, 1f))

internal fun lerp(from: Float, to: Float, progress: Float) = from * (1 - progress) + to * progress

internal fun Float.clamp(min: Float, max: Float) = if (this < min) min else if (this > max) max else this

internal fun Float.ceilToInt() = ceil(this).toInt()

internal fun Float.snap(target: Float, @FloatRange(from = 0.0) threshold: Float) = if (abs(this - target) <= threshold) target else this

internal val Int.f get() = toFloat()
internal val Int.d get() = toDouble()
internal val Float.d get() = toDouble()
internal val Double.f get() = toFloat()

internal fun distance(ax: Float, bx: Float, ay: Float, by: Float) = length(ax - bx, ay - by)

internal fun length(x: Float, y: Float) = sqrt(x * x + y * y)

internal fun angle(x: Float, y: Float): Float {
    if (y == 0f) {
        return if (x >= 0) 0f else 180f
    } else {
        val ra = Math.toDegrees(atan(y.d / x.d)).f
        return if (x >= 0) {
            if (y > 0) ra else 360f + ra
        } else 180f + ra
    }
}

internal fun normalizeAngle(angle: Float): Float {
    val m = angle % 360f
    return if (m < 0f) 360f + m else m
}

internal val Float.previous: Int
    get() {
        val floor = floor(this)
        if (floor == this)
            return floor.toInt() - 1
        else
            return floor.toInt()
    }

internal fun absMin(vararg values: Float) = values.map(::abs).min()!!