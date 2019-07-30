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
    BY_REVOLUTION, BY_ORDER
}

private fun ProviderIndexingMode.provide(revolution: Int, order: Int) = when (this) {
    ProviderIndexingMode.BY_ORDER -> order
    ProviderIndexingMode.BY_REVOLUTION -> revolution
}

private fun <Type> ProviderIndexingMode.fromList(revolution: Int, order: Int, list: List<Type>) =
    list[provide(revolution, order)]

private fun <Type> ProviderIndexingMode.fromListClamped(revolution: Int, order: Int, list: List<Type>) =
    list[provide(revolution, order).clamp(0, list.lastIndex)]

class ConstantFactorProvider : KnobView.FactorProvider {

    @FloatRange(from = 0.0, to = 1.0)
    var factor = 0.5f

    override fun provide(view: KnobView, revolution: Int, order: Int) = factor
}

class ListFactorProvider(@Size(min = 1) factors: Iterable<Float>) : KnobView.FactorProvider {

    @Size(min = 1)
    val factors = mutableListOf<Float>()
    var indexingMode = ProviderIndexingMode.BY_ORDER

    override fun provide(view: KnobView, revolution: Int, order: Int) = indexingMode.fromListClamped(revolution, order, factors)

}

class BackoffFactorProvider : KnobView.FactorProvider {

    @FloatRange(from = 0.0, to = 1.0)
    var backoff = 0.9f
    var indexingMode = ProviderIndexingMode.BY_ORDER

    override fun provide(view: KnobView, revolution: Int, order: Int): Float = backoff.pow(indexingMode.provide(revolution, order).f)

}

class CurveFactorProvider : KnobView.FactorProvider {

    @FloatRange(from = 0.0, to = 1.0)
    var from = 0f
    @FloatRange(from = 0.0, to = 1.0)
    var to = 1f
    var interpolator = LinearInterpolator()
    var indexingMode = ProviderIndexingMode.BY_ORDER

    override fun provide(view: KnobView, revolution: Int, order: Int) =
        lerp(from, to, interpolator.getInterpolation(indexingMode.provide(revolution, order).f / max(view.revolutionCount - 1, 1)))

}

// Color

class ConstantColorProvider(@ColorInt val color: Int) : KnobView.ColorProvider {
    override fun provide(view: KnobView, revolution: Int, order: Int) = color
}

class ListColorProvider : KnobView.ColorProvider {

    @Size(min = 1)
    val colors = mutableListOf<Int>()
    var indexingMode = ProviderIndexingMode.BY_ORDER

    override fun provide(view: KnobView, revolution: Int, order: Int) = indexingMode.fromListClamped(revolution, order, colors)
}

class RGBCurveColorProvider : KnobView.ColorProvider {

    @ColorInt
    var from = Color.BLACK
    @ColorInt
    var to = Color.WHITE
    var interpolator = LinearInterpolator()
    var indexingMode = ProviderIndexingMode.BY_ORDER

    override fun provide(view: KnobView, revolution: Int, order: Int) =
        lerpColor(from, to, interpolator.getInterpolation(indexingMode.provide(revolution, order).f / max(view.revolutionCount - 1, 1)))

}

class HSVCurveColorProvider : KnobView.ColorProvider {

    private companion object {
        private val hsv = FloatArray(3)
    }

    @FloatRange(from = 0.0, to = 360.0)
    var fromHue = 0f
    @FloatRange(from = 0.0, to = 1.0)
    var fromSaturation = 0f
    @FloatRange(from = 0.0, to = 1.0)
    var fromValue = 0f
    @FloatRange(from = 0.0, to = 1.0)
    var fromAlpha = 1f
    @FloatRange(from = 0.0, to = 360.0)
    var toHue = 0f
    @FloatRange(from = 0.0, to = 1.0)
    var toSaturation = 0f
    @FloatRange(from = 0.0, to = 1.0)
    var toValue = 1f
    @FloatRange(from = 0.0, to = 1.0)
    var toAlpha = 1f
    var interpolator = LinearInterpolator()
    var indexingMode = ProviderIndexingMode.BY_ORDER

    override fun provide(view: KnobView, revolution: Int, order: Int): Int {
        val progress = interpolator.getInterpolation(indexingMode.provide(revolution, order).f / max(view.revolutionCount - 1, 1))
        hsv[0] = lerp(fromHue, toHue, progress)
        hsv[1] = lerp(fromSaturation, toSaturation, progress)
        hsv[2] = lerp(fromValue, toValue, progress)
        val a = (lerp(fromAlpha, toAlpha, progress) * 255f).roundToInt()
        return Color.HSVToColor(a, hsv)
    }

}

// Label

class ValueLabelProvider : KnobView.LabelProvider {

    @IntRange(from = 0L, to = 4L)
    var decimalPlaces = 0
    @Size(min = 0, max = 3)
    var prefix = ""
    @Size(min = 0, max = 3)
    var suffix = ""

    override fun provide(view: KnobView, revolution: Int, thick: Int, value: Float): String {
        val rounded = BigDecimal(value.d).setScale(decimalPlaces, RoundingMode.HALF_UP)
        return "$prefix$rounded$suffix"
    }

}

class PercentageLabelProvider : KnobView.LabelProvider {
    override fun provide(view: KnobView, revolution: Int, thick: Int, value: Float) =
        "${((value - view.startValue) / (view.maxValue - view.startValue) * 100f).roundToInt()}%"

}

class LabelListProvider : KnobView.LabelProvider {

    val labels = mutableListOf<String>()
    var restartOnRevolution = true

    override fun provide(view: KnobView, revolution: Int, thick: Int, value: Float) =
        if (restartOnRevolution) labels[thick + view.labelThicks * revolution]
        else labels[thick]

}
