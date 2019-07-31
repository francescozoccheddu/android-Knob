package com.francescozoccheddu.knob

import android.graphics.Color
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.Size
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt


// Factor

enum class ProviderIndexingMode {
    BY_TRACK, BY_ORDER
}

private fun ProviderIndexingMode.provide(track: Int, order: Int) = when (this) {
    ProviderIndexingMode.BY_ORDER -> order
    ProviderIndexingMode.BY_TRACK -> track
}

private fun <Type> ProviderIndexingMode.fromListClamped(track: Int, order: Int, list: List<Type>) =
    list[provide(track, order).clamp(0, list.lastIndex)]

class ConstantFactorProvider : KnobView.FactorProvider {

    @FloatRange(from = 0.0, to = 1.0)
    var factor = 0.5f
        set(value) {
            if (value !in 0f..1f)
                throw IllegalArgumentException("'${this::factor.name}' does not fall in range [0,1]")
            field = value
        }

    override fun provide(view: KnobView, track: Int, order: Int) = factor

}

class ListFactorProvider : KnobView.FactorProvider {

    @Size(min = 1)
    val factors = mutableListOf<Float>()
    var indexingMode = ProviderIndexingMode.BY_ORDER

    override fun provide(view: KnobView, track: Int, order: Int) = indexingMode.fromListClamped(track, order, factors)

}

class BackoffFactorProvider : KnobView.FactorProvider {

    @FloatRange(from = 0.0, to = 1.0)
    var backoff = 0.9f
        set(value) {
            if (value !in 0f..1f)
                throw IllegalArgumentException("'${this::backoff.name}' does not fall in range [0,1]")
            field = value
        }
    var indexingMode = ProviderIndexingMode.BY_ORDER

    override fun provide(view: KnobView, track: Int, order: Int): Float = backoff.pow(indexingMode.provide(track, order).f)

}

class CurveFactorProvider : KnobView.FactorProvider {

    @FloatRange(from = 0.0, to = 1.0)
    var from = 0f
        set(value) {
            if (value !in 0f..1f)
                throw IllegalArgumentException("'${this::from.name}' does not fall in range [0,1]")
            field = value
        }
    @FloatRange(from = 0.0, to = 1.0)
    var to = 1f
        set(value) {
            if (value !in 0f..1f)
                throw IllegalArgumentException("'${this::to.name}' does not fall in range [0,1]")
            field = value
        }
    var interpolator = LinearInterpolator()
    var indexingMode = ProviderIndexingMode.BY_ORDER

    override fun provide(view: KnobView, track: Int, order: Int) =
        lerp(from, to, interpolator.getInterpolation(indexingMode.provide(track, order).f / max(view.trackCount - 1, 1)))

}


// Color

class ConstantColorProvider : KnobView.ColorProvider {

    @ColorInt
    var color = Color.BLACK

    override fun provide(view: KnobView, track: Int, order: Int) = color

}

class ListColorProvider : KnobView.ColorProvider {

    @Size(min = 1)
    val colors = mutableListOf<Int>()
    var indexingMode = ProviderIndexingMode.BY_ORDER

    override fun provide(view: KnobView, track: Int, order: Int) = indexingMode.fromListClamped(track, order, colors)
}

class RGBCurveColorProvider : KnobView.ColorProvider {

    @ColorInt
    var from = Color.BLACK
    @ColorInt
    var to = Color.WHITE
    var interpolator = LinearInterpolator()
    var indexingMode = ProviderIndexingMode.BY_ORDER

    override fun provide(view: KnobView, track: Int, order: Int) =
        lerpColor(from, to, interpolator.getInterpolation(indexingMode.provide(track, order).f / max(view.trackCount - 1, 1)))

}

class HSVCurveColorProvider : KnobView.ColorProvider {

    var fromHue = 0f
    @FloatRange(from = 0.0, to = 1.0)
    var fromSaturation = 0f
        set(value) {
            if (value !in 0f..1f)
                throw IllegalArgumentException("'${this::fromSaturation.name}' does not fall in range [0,1]")
            field = value
        }
    @FloatRange(from = 0.0, to = 1.0)
    var fromValue = 0f
        set(value) {
            if (value !in 0f..1f)
                throw IllegalArgumentException("'${this::fromValue.name}' does not fall in range [0,1]")
            field = value
        }
    @FloatRange(from = 0.0, to = 1.0)
    var fromAlpha = 1f
        set(value) {
            if (value !in 0f..1f)
                throw IllegalArgumentException("'${this::fromAlpha.name}' does not fall in range [0,1]")
            field = value
        }
    var toHue = 0f
    @FloatRange(from = 0.0, to = 1.0)
    var toSaturation = 0f
        set(value) {
            if (value !in 0f..1f)
                throw IllegalArgumentException("'${this::toSaturation.name}' does not fall in range [0,1]")
            field = value
        }
    @FloatRange(from = 0.0, to = 1.0)
    var toValue = 1f
        set(value) {
            if (value !in 0f..1f)
                throw IllegalArgumentException("'${this::toValue.name}' does not fall in range [0,1]")
            field = value
        }
    @FloatRange(from = 0.0, to = 1.0)
    var toAlpha = 1f
        set(value) {
            if (value !in 0f..1f)
                throw IllegalArgumentException("'${this::toAlpha.name}' does not fall in range [0,1]")
            field = value
        }
    var interpolator = LinearInterpolator()
    var indexingMode = ProviderIndexingMode.BY_ORDER

    override fun provide(view: KnobView, track: Int, order: Int): Int {
        val progress = interpolator.getInterpolation(indexingMode.provide(track, order).f / max(view.trackCount - 1, 1))
        val h = normalizeAngle(lerp(fromHue, toHue, progress))
        val s = lerp(fromSaturation, toSaturation, progress)
        val v = lerp(fromValue, toValue, progress)
        val a = lerp(fromAlpha, toAlpha, progress)
        return hsv(h, s, v, a)
    }

}


// Text

abstract class ValueTextProvider {

    @IntRange(from = 0L)
    var decimalPlaces = 0
        set(value) {
            if (value < 0)
                throw IllegalArgumentException("'${this::decimalPlaces.name}' cannot be negative")
            field = value
        }
    @Size(min = 0)
    var prefix = ""
    @Size(min = 0)
    var suffix = ""

    protected fun provide(value: Float): String {
        val rounded = BigDecimal(value.d).setScale(decimalPlaces, RoundingMode.HALF_UP)
        return "$prefix$rounded$suffix"
    }

}

abstract class PercentageTextProvider {

    fun provide(view: KnobView, value: Float) =
        "${((value - view.startValue) / (view.maxValue - view.startValue) * 100f).roundToInt()}%"

}


// Thicks

class ValueThickTextProvider : ValueTextProvider(), KnobView.ThickTextProvider {

    override fun provide(view: KnobView, track: Int, thick: Int, value: Float) = provide(value)

}

class PercentageThickTextProvider : PercentageTextProvider(), KnobView.ThickTextProvider {

    override fun provide(view: KnobView, track: Int, thick: Int, value: Float) = provide(view, value)

}

class ThickListTextProvider : KnobView.ThickTextProvider {

    val thicks = mutableListOf<String>()
    var restartOnTrack = true

    override fun provide(view: KnobView, track: Int, thick: Int, value: Float) =
        if (restartOnTrack) thicks[thick + view.thicks * track]
        else thicks[thick]

}


// Label

class ValueLabelTextProvider : ValueTextProvider(), KnobView.LabelTextProvider {

    override fun provide(view: KnobView, value: Float): String = provide(value)

}

class PercentageLabelTextProvider : PercentageTextProvider(), KnobView.LabelTextProvider
