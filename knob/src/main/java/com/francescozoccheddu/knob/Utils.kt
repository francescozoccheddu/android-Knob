package com.francescozoccheddu.knob

import android.content.res.Resources
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.util.TypedValue
import kotlin.math.*


// Visual

internal val Float.dp
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics)

internal val RectF.center get() = PointF(centerX(), centerY())


// Color

private val tempHsvArray = FloatArray(3)

internal fun hsv(hue: Float, saturation: Float,
                 value: Float, alpha: Float = 1f) =
    Color.HSVToColor((alpha * 255).roundToInt(), tempHsvArray.apply {
        this[0] = hue
        this[1] = saturation
        this[2] = value
    })

internal val Int.red get() = Color.red(this)
internal val Int.green get() = Color.green(this)
internal val Int.blue get() = Color.blue(this)
internal val Int.alpha get() = Color.alpha(this)

internal fun lerpColor(from: Int, to: Int, progress: Float): Int {
    val r = lerp(from.red.f, to.red.f, progress).roundToInt()
    val g = lerp(from.green.f, to.green.f, progress).roundToInt()
    val b = lerp(from.blue.f, to.blue.f, progress).roundToInt()
    val a = lerp(from.alpha.f, to.alpha.f, progress).roundToInt()
    return Color.argb(a, r, g, b)
}


// Math

internal fun lerp(from: Float, to: Float, progress: Float) = from * (1 - progress) + to * progress

internal fun Float.clamp(min: Float, max: Float) = if (this < min) min else if (this > max) max else this

internal fun Int.clamp(min: Int, max: Int) = if (this < min) min else if (this > max) max else this

internal val Float.clamp01 get() = clamp(0f, 1f)

internal fun Float.ceilToInt() = ceil(this).toInt()
internal fun Float.floorToInt() = floor(this).toInt()

internal val Int.f get() = toFloat()
internal val Float.d get() = toDouble()
internal val Double.f get() = toFloat()

internal fun length(x: Float, y: Float) = sqrt(x * x + y * y)

internal fun angle(x: Float, y: Float): Float {
    return if (y == 0f) {
        if (x >= 0) 0f else 180f
    } else {
        val ra = Math.toDegrees(atan(y.d / x.d)).f
        if (x >= 0) {
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
        return if (floor == this)
            floor.toInt() - 1
        else
            floor.toInt()
    }

internal val Float.next: Int
    get() {
        val ceil = ceil(this)
        return if (ceil == this)
            ceil.toInt() + 1
        else
            ceil.toInt()
    }

internal fun absMin(vararg values: Float) = values.map(::abs).min()!!

internal val Float.rad get() = Math.toRadians(this.d).f
