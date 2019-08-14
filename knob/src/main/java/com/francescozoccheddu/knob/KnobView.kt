package com.francescozoccheddu.knob

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getFloatOrThrow
import com.francescozoccheddu.animatorhelpers.ABFloat
import com.francescozoccheddu.animatorhelpers.SmoothFloat
import com.francescozoccheddu.animatorhelpers.SpringFloat
import kotlin.math.*


class KnobView : View {

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        context?.theme?.obtainStyledAttributes(attrs, R.styleable.KnobView, 0, 0)?.apply {

            fun has(id: Int) = hasValue(id)
            fun getFloat(id: Int) = if (has(id)) getFloatOrThrow(id) else null
            fun getColor(id: Int) = if (has(id)) getColorOrThrow(id) else null
            fun getFont(id: Int): Typeface? {
                val resId = getResourceId(id, -1)
                return if (resId != -1) ResourcesCompat.getFont(context, resId)
                else null
            }

            fun <Type> getList(id: Int, map: (TypedArray, Int) -> Type): List<Type>? {
                val value = TypedValue()
                if (getValue(id, value) && value.type == TypedValue.TYPE_REFERENCE) {
                    val resId = getResourceId(id, -1)
                    val array = resources.obtainTypedArray(resId)
                    try {
                        return (0 until array.length()).map { map(array, it) }
                    } finally {
                        array.recycle()
                    }
                }
                return null
            }

            fun <Type> pack(item: Type?) = if (item != null) listOf(item) else null

            fun moreThanOne(vararg values: Boolean): Boolean {
                var one = false
                for (value in values)
                    if (value) {
                        if (one)
                            return true
                        else
                            one = true
                    }
                return false
            }

            fun getFactorProvider(listProp: Int, backoffProp: Int, fromProp: Int, toProp: Int): FactorProvider? {
                val list = getList(listProp) { a, i -> a.getFloatOrThrow(i) } ?: pack(getFloat(listProp))
                val backoff = getFloat(backoffProp)
                val from = getFloat(fromProp)
                val to = getFloat(toProp)
                if (moreThanOne(list != null, backoff != null, from != null || to != null))
                    throw RuntimeException("Ambiguous FactorProvider creation attributes")
                return if (list != null) ListFactorProvider().apply { factors.addAll(list) }
                else if (backoff != null) BackoffFactorProvider().apply { this.backoff = backoff }
                else if (from != null || to != null) {
                    if (from == null || to == null)
                        throw RuntimeException("Missing CurveFactorProvider endpoint")
                    CurveFactorProvider().apply {
                        this.from = from
                        this.to = to
                    }
                } else null
            }

            fun getColorProvider(listProp: Int, fromProp: Int, toProp: Int): ColorProvider? {
                val list = getList(listProp) { a, i -> a.getColorOrThrow(i) } ?: pack(getColor(listProp))
                val from = getColor(fromProp)
                val to = getColor(toProp)
                if (moreThanOne(list != null, from != null || to != null))
                    throw RuntimeException("Ambiguous ColorProvider creation attributes")
                return if (list != null) ListColorProvider().apply { colors.addAll(list) }
                else if (from != null || to != null) {
                    if (from == null || to == null)
                        throw RuntimeException("Missing CurveFactorProvider endpoint")
                    RGBCurveColorProvider().apply {
                        this.from = from
                        this.to = to
                    }
                } else null
            }

            fun ValueTextProvider.set(prefixProp: Int, suffixProp: Int, decimalPlacesProp: Int) {
                decimalPlaces = getInt(decimalPlacesProp, decimalPlaces)
                prefix = getString(prefixProp) ?: prefix
                suffix = getString(suffixProp) ?: suffix
            }

            try {

                // Value
                run {
                    val min = getFloat(R.styleable.KnobView_minValue)
                    val start = getFloat(R.styleable.KnobView_startValue)
                    if (min != null && start != null && start > min)
                        throw RuntimeException("Start value cannot be greater than minimum value")
                    val max = getFloat(R.styleable.KnobView_maxValue)
                    if (min != null && max != null && min > max)
                        throw RuntimeException("Minimum value cannot be greater than maximum value")
                    minValue = min ?: minValue
                    maxValue = max ?: maxValue
                    startValue = start ?: startValue
                    value = getFloat(R.styleable.KnobView_value, value)
                    trackValue = getFloat(R.styleable.KnobView_trackValue, trackValue)
                    snap = getFloat(R.styleable.KnobView_snap, snap)
                }
                // Track
                run {
                    clockwise = getBoolean(R.styleable.KnobView_clockwise, clockwise)
                    trackThickness = getDimension(R.styleable.KnobView_trackThickness, trackThickness)
                    bedThicknessFactor = getFloat(R.styleable.KnobView_bedThicknessFactor, bedThicknessFactor)
                    startAngle = getFloat(R.styleable.KnobView_startAngle, startAngle)
                    trackRadiusFactor = getFactorProvider(
                        R.styleable.KnobView_trackRadiusFactor,
                        R.styleable.KnobView_trackRadiusBackoffFactor,
                        R.styleable.KnobView_trackRadiusFromFactor,
                        R.styleable.KnobView_trackRadiusToFactor
                    )
                        ?: trackRadiusFactor
                    trackThicknessFactor = getFactorProvider(
                        R.styleable.KnobView_trackThicknessFactor,
                        R.styleable.KnobView_trackThicknessBackoffFactor,
                        R.styleable.KnobView_trackThicknessFromFactor,
                        R.styleable.KnobView_trackThicknessToFactor
                    )
                        ?: trackThicknessFactor
                    bedColor = getColorProvider(
                        R.styleable.KnobView_bedColor,
                        R.styleable.KnobView_bedFromColor,
                        R.styleable.KnobView_bedToColor
                    )
                        ?: bedColor
                    progressColor = getColorProvider(
                        R.styleable.KnobView_progressColor,
                        R.styleable.KnobView_progressFromColor,
                        R.styleable.KnobView_progressToColor
                    )
                        ?: progressColor
                    thumbThicknessFactor = getFloat(R.styleable.KnobView_thumbThicknessFactor, thumbThicknessFactor)
                }
                // Input
                run {
                    dragThicknessFactor = getFloat(R.styleable.KnobView_dragThicknessFactor, dragThicknessFactor)
                    scrollableX = getBoolean(R.styleable.KnobView_scrollableX, scrollableX)
                    scrollableY = getBoolean(R.styleable.KnobView_scrollableY, scrollableY)
                    tappable = getBoolean(R.styleable.KnobView_tappable, tappable)
                    draggable = getBoolean(R.styleable.KnobView_draggable, draggable)
                    keyboardStep = getFloat(R.styleable.KnobView_keyboardStep, keyboardStep)
                }
                // Thicks
                run {
                    thicks = getInt(R.styleable.KnobView_thicks, thicks)
                    thickBedColor = getColorProvider(
                        R.styleable.KnobView_thickBedColor,
                        R.styleable.KnobView_thickBedFromColor,
                        R.styleable.KnobView_thickBedToColor
                    )
                        ?: thickBedColor
                    thickProgressColor = getColorProvider(
                        R.styleable.KnobView_thickProgressColor,
                        R.styleable.KnobView_thickProgressFromColor,
                        R.styleable.KnobView_thickProgressToColor
                    )
                        ?: thickProgressColor
                    thickText = run {
                        val listProp = R.styleable.KnobView_thickText
                        val prefixProp = R.styleable.KnobView_thickTextPrefix
                        val suffixProp = R.styleable.KnobView_thickTextSuffix
                        val decimalPlacesProp = R.styleable.KnobView_thickTextDecimalPlaces
                        val hasValueProviderProperties =
                            has(prefixProp) || has(suffixProp) || has(decimalPlacesProp)
                        val hasListProviderProperties = has(listProp)
                        if (moreThanOne(hasValueProviderProperties, hasListProviderProperties))
                            throw RuntimeException("Ambiguous ThickTextProvider creation attributes")
                        if (hasValueProviderProperties)
                            ValueThickTextProvider().apply { set(prefixProp, suffixProp, decimalPlacesProp) }
                        else if (hasListProviderProperties) {
                            ThickListTextProvider().apply {
                                thicks.addAll(getTextArray(R.styleable.KnobView_thickText).map { it.toString() })
                            }
                        } else thickText
                    }
                    thickSize = getDimension(R.styleable.KnobView_thickSize, thickSize)
                    thickTypeface = getFont(R.styleable.KnobView_thickTypeface) ?: thickTypeface
                }
                // Label
                run {
                    labelColor = getColor(R.styleable.KnobView_labelColor, labelColor)
                    labelText = run {
                        val prefixProp = R.styleable.KnobView_labelTextPrefix
                        val suffixProp = R.styleable.KnobView_labelTextSuffix
                        val decimalPlacesProp = R.styleable.KnobView_labelTextDecimalPlaces
                        if (has(prefixProp) || has(suffixProp) || has(decimalPlacesProp)) {
                            ValueLabelTextProvider().apply { set(prefixProp, suffixProp, decimalPlacesProp) }
                        } else labelText
                    }
                    labelSize = getDimension(R.styleable.KnobView_labelSize, labelSize)
                    labelTypeface = getFont(R.styleable.KnobView_labelTypeface) ?: labelTypeface
                }
            } finally {
                recycle()
            }
        }
    }


    // Value

    var minValue = 0f
        set(value) {
            field = value
            if (rawValue < minValue)
                rawValue = minValue
            if (maxValue < minValue)
                maxValue = minValue
            if (startValue > minValue)
                startValue = minValue
            invalidate()
        }
    var maxValue = 30f
        set(value) {
            field = value
            if (rawValue > maxValue)
                rawValue = maxValue
            if (minValue > maxValue)
                minValue = maxValue
            updateLength()
            invalidate()
        }
    var startValue = 0f
        set(value) {
            field = value
            if (startValue > minValue)
                minValue = startValue
            updateLength()
            invalidate()
        }
    var value: Float
        get() {
            return if (snap > 0f) {
                val ticks = ((rawValue - startValue) / snap).roundToInt()
                (ticks * snap + startValue).clamp(minValue, maxValue)
            } else rawValue
        }
        set(value) {
            if (value !in minValue..maxValue)
                throw IllegalArgumentException("'${this::value.name}' does not fall in [$minValue,$maxValue] range")
            rawValue = value
        }
    var trackValue = 10f
        set(value) {
            if (value <= 0.0f)
                throw IllegalArgumentException("'${::trackValue.name}' must be positive")
            field = value
            invalidate()
        }
    @FloatRange(from = 0.0)
    var snap = 0f
        set(value) {
            if (value < 0.0f)
                throw IllegalArgumentException("'${::snap.name}' cannot be negative")
            field = value
            rawValue = value
            invalidate()
        }
    var onValueChange: ((KnobView) -> Unit)? = null
    val trackCount
        get() = ((maxValue - startValue) / trackValue).ceilToInt()


    // Track

    var clockwise = true
        set(value) {
            field = value
            invalidate()
        }
    @Dimension
    var trackThickness = 32f.dp
        set(value) {
            if (value <= 0.0f)
                throw IllegalArgumentException("'${::trackThickness.name}' must be positive")
            field = value
            invalidate()
        }
    @FloatRange(from = 0.0, to = 1.0)
    var bedThicknessFactor = 0.9f
        set(value) {
            if (value !in 0f..1f)
                throw IllegalArgumentException("'${::bedThicknessFactor.name}' does not fall in [0,1] range")
            field = value
            invalidate()
        }
    var startAngle = 90f
        set(value) {
            field = value
            invalidate()
        }
    var trackRadiusFactor: FactorProvider = BackoffFactorProvider()
        set(value) {
            field = value
            invalidate()
        }
    var trackThicknessFactor: FactorProvider = BackoffFactorProvider()
        set(value) {
            field = value
            invalidate()
        }
    var bedColor: ColorProvider = ConstantColorProvider().apply {
        color = hsv(0f, 0f, 0.1f)
    }
        set(value) {
            field = value
            invalidate()
        }
    var progressColor: ColorProvider = HSVCurveColorProvider().apply {
        fromHue = 180f
        fromSaturation = 0.75f
        fromValue = 0.75f
        fromAlpha = 1f
        toHue = 190f
        toSaturation = 0.5f
        toValue = 0.5f
        toAlpha = 1f
    }
        set(value) {
            field = value
            invalidate()
        }
    @FloatRange(from = 0.0, to = 2.0)
    var thumbThicknessFactor = 0.75f
        set(value) {
            if (value !in 0f..2f)
                throw IllegalArgumentException("'${::thumbThicknessFactor.name}' does not fall in [0,2] range")
            field = value
            invalidate()
        }


    // Input

    @FloatRange(from = 1.0, to = 3.0)
    var dragThicknessFactor = 3f
        set(value) {
            if (value !in 1f..10f)
                throw IllegalArgumentException("'${::dragThicknessFactor.name}' does not fall in [1,10] range")
            field = value
        }
    var scrollableY = true
    var scrollableX = false
    var tappable = true
    var draggable = true
    @FloatRange(from = 1.0)
    var keyboardStep = 1f
        set(value) {
            if (value <= 0.0f)
                throw IllegalArgumentException("'${::keyboardStep.name}' must be positive")
            field = value
            invalidate()
        }


    // Thicks

    @IntRange(from = 0, to = 20)
    var thicks = 10
        set(value) {
            if (value !in 0..12)
                throw IllegalArgumentException("'${::thicks.name}' does not fall in [0,12] range")
            field = value
            invalidate()
        }
    var thickBedColor: ColorProvider? = ListColorProvider().apply {
        colors += Color.DKGRAY
        colors += Color.TRANSPARENT
    }
        set(value) {
            field = value
            invalidate()
        }
    var thickProgressColor: ColorProvider = ListColorProvider().apply {
        colors += Color.WHITE
        colors += Color.TRANSPARENT
    }
        set(value) {
            field = value
            invalidate()
        }
    var thickText: ThickTextProvider = ValueThickTextProvider()
        set(value) {
            field = value
            invalidate()
        }
    @Dimension
    var thickSize = 16f.dp
        set(value) {
            if (value < 0.0f)
                throw IllegalArgumentException("'${::thickSize.name}' cannot be negative")
            field = value
            invalidate()
        }
    var thickTypeface: Typeface = Typeface.DEFAULT
        set(value) {
            field = value
            invalidate()
        }


    // Label

    @ColorInt
    var labelColor = Color.WHITE
        set(value) {
            field = value
            invalidate()
        }
    var labelText: LabelTextProvider = ValueLabelTextProvider()
        set(value) {
            field = value
            invalidate()
        }
    @Dimension
    var labelSize = 0f
        set(value) {
            if (value < 0.0f)
                throw IllegalArgumentException("'${::labelSize.name}' cannot be negative")
            field = value
            invalidate()
        }
    var labelTypeface: Typeface = Typeface.DEFAULT
        set(value) {
            field = value
            invalidate()
        }


    // Providers

    interface ColorProvider {

        @ColorInt
        fun provide(
            view: KnobView,
            @IntRange(from = 0) track: Int,
            @IntRange(from = 0) order: Int
        ): Int
    }

    interface ThickTextProvider {

        fun provide(
            view: KnobView,
            @IntRange(from = 0) track: Int,
            @IntRange(from = 0) thick: Int,
            value: Float
        ): String

    }

    interface LabelTextProvider {

        fun provide(view: KnobView, value: Float): String

    }

    interface FactorProvider {

        @FloatRange(from = 0.0, to = 1.0)
        fun provide(
            view: KnobView,
            @IntRange(from = 0) track: Int,
            @IntRange(from = 0) order: Int
        ): Float

    }


    // Implementation

    companion object {
        private const val MAX_TRACKS = 4
        private val SCROLL_REASONABLE_RADIUS = 128f.dp
        private const val SCROLL_REASONABLE_RADIUS_INFLUENCE = 0.5f
        private val DRAG_ARC_SNAP_THRESHOLD = 60f.dp
        private const val SCROLL_HOLD_RADIUS_FACTOR = 1f / 2f
        private const val SCROLL_RADIUS_FACTOR = 1f
        private const val THICK_MAX_EXPECTED_HALF_LENGTH = 1f / 10f
    }

    private operator fun ColorProvider.invoke(track: Int, order: Int): Int = provide(this@KnobView, track, order)
    private operator fun ThickTextProvider.invoke(track: Int, thick: Int, value: Float): String =
        provide(this@KnobView, track, thick, value)

    private operator fun LabelTextProvider.invoke(): String = provide(this@KnobView, value)
    private operator fun FactorProvider.invoke(revolution: Int, order: Int): Float =
        provide(this@KnobView, revolution, order)

    private var rawValue = trackValue / 2f
        set(value) {
            val lastValue = value
            field = value.clamp(minValue, maxValue)
            if (lastValue != value) {
                onValueChange?.invoke(this)
            }
            updateLength()
        }

    private var order by SpringFloat(0f).apply {
        onUpdate = { invalidate() }
        acceleration = 100f
        maxVelocity = 1000f
        snap = 1f / 1000f
    }
    private var thumbActiveness by ABFloat(0f).apply {
        onUpdate = { invalidate() }
        interpolator = DecelerateInterpolator()
        speed = 3f
    }
    private var masterTrackLength by ABFloat(0f).apply {
        onUpdate = {
            order = max(it.value.previous, 0).f
            invalidate()
        }
        interpolator = DecelerateInterpolator()
        speed = 4f
    }
    private var masterProgressLength by SmoothFloat(0f).apply {
        onUpdate = { invalidate() }
        smoothness = 0.1f
        snap = 0.01f
    }

    private fun lengthByValue(value: Float) = (value - startValue) / trackValue

    private fun valueByLength(length: Float) = length * trackValue + startValue

    private val contentRect = RectF()

    private fun updateLength() {
        val length = lengthByValue(value)
        masterProgressLength = length
        masterTrackLength = min(max(ceil(length), 1f), lengthByValue(maxValue))
    }

    init {
        updateLength()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val minValueLength = lengthByValue(minValue)
        val angleSign = if (clockwise) -1f else 1f
        val center = contentRect.center

        canvas?.apply {

            for (t in run {
                val orderCount = order.ceilToInt();
                max(0, orderCount - MAX_TRACKS)..orderCount
            }) {
                val prevOrder = (order - t).floorToInt()
                val prevPositiveOrder = max(0, prevOrder)
                val nextOrder = (order - t).ceilToInt()
                val alpha = order - floor(order)
                fun getSweep(length: Float) =
                    (if (t == 0) min(length, 1f) - minValueLength else (length - t).clamp01) * 360f

                val startLength = if (t == 0) minValueLength else 0f
                val trackStartAngle = startLength * 360f * angleSign + startAngle
                val radius = lerp(
                    trackRadiusFactor(t, prevPositiveOrder),
                    trackRadiusFactor(t, nextOrder), alpha
                ) * outerTrackRadius
                val thicknessFactor = lerp(
                    if (prevOrder >= 0) trackThicknessFactor(t, prevPositiveOrder) else 0f,
                    trackThicknessFactor(t, nextOrder), alpha
                )
                val baseThickness = thicknessFactor * trackThickness
                val trackThumbActiveness = (1f - min(abs(order - t), 1f)) * thumbActiveness
                val progressThickness =
                    lerp(baseThickness, baseThickness * min(thumbThicknessFactor, 1f), trackThumbActiveness)

                fun drawThicks(
                    startLength: Float,
                    endLength: Float,
                    color: Int,
                    trackThickness: Float
                ) {
                    val actualThickSize = thickSize * thicknessFactor
                    if (color.alpha > 0 && actualThickSize > 0f && thicks > 0) {
                        var clip = false
                        val interspace = (1f / thicks)
                        val range = if (startLength <= 0.0f && endLength >= 1.0f) 0 until thicks
                        else run {
                            fun getEndPoint(length: Float, start: Boolean): Int {
                                val p = if (start)
                                    max(((length - THICK_MAX_EXPECTED_HALF_LENGTH) / interspace).ceilToInt(), 0)
                                else
                                    min(
                                        ((length + THICK_MAX_EXPECTED_HALF_LENGTH) / interspace).floorToInt(),
                                        if (startLength > THICK_MAX_EXPECTED_HALF_LENGTH) thicks else (thicks - 1)
                                    )
                                clip = clip || abs(p * interspace - length) <= THICK_MAX_EXPECTED_HALF_LENGTH
                                return p
                            }
                            getEndPoint(startLength, true)..getEndPoint(endLength, false)
                        }
                        if (clip) {
                            save()
                            canvas.clipTrack(
                                center,
                                radius,
                                trackStartAngle,
                                getSweep(endLength + t) * angleSign,
                                trackThickness
                            )
                        }
                        for (thick in range) {
                            val length = interspace * thick
                            val angle = startAngle + length * 360f * angleSign
                            val text = thickText(t, thick, valueByLength(t + length))
                            drawThick(center, radius, angle, text, color, actualThickSize, thickTypeface)
                        }
                        if (clip)
                            restore()
                    }
                }

                val trackThickBedColor = run {
                    val provider = thickBedColor
                    if (provider != null)
                        lerpColor(
                            provider(t, prevPositiveOrder),
                            provider(t, nextOrder), alpha
                        )
                    else Color.TRANSPARENT
                }
                val trackThickProgressColor = lerpColor(
                    thickProgressColor(t, prevPositiveOrder),
                    thickProgressColor(t, nextOrder), alpha
                )
                val singlePassThicks = thickBedColor == null || run {
                    val bg = trackThickBedColor
                    val fg = trackThickProgressColor
                    fg.alpha == 255 && fg.red == bg.red && fg.green == bg.green && fg.blue == bg.blue
                }

                run {
                    // Track
                    val trackBedColor = lerpColor(
                        bedColor(t, prevPositiveOrder),
                        bedColor(t, nextOrder), alpha
                    )
                    drawTrack(
                        center,
                        radius,
                        trackStartAngle,
                        getSweep(masterTrackLength) * angleSign,
                        trackBedColor,
                        progressThickness * bedThicknessFactor
                    )
                }
                run {
                    val trackProgressColor = lerpColor(
                        progressColor(t, prevPositiveOrder),
                        progressColor(t, nextOrder), alpha
                    )
                    // Background thicks
                    if (!singlePassThicks) {
                        val thicksStartLength =
                            if (trackProgressColor.alpha < 255) startLength else max((masterProgressLength - t), 0f)
                        drawThicks(
                            thicksStartLength,
                            max(masterTrackLength - t, 0f),
                            trackThickBedColor,
                            progressThickness * bedThicknessFactor
                        )
                    }
                    // Progress
                    val sweep = getSweep(masterProgressLength) * angleSign
                    drawTrack(center, radius, trackStartAngle, sweep, trackProgressColor, progressThickness)
                    // Thumb
                    run {
                        val thumbThickness =
                            lerp(baseThickness, baseThickness * max(thumbThicknessFactor, 1f), trackThumbActiveness)
                        drawTrack(center, radius, trackStartAngle + sweep, 0f, trackProgressColor, thumbThickness)
                    }
                }
                run {
                    // Foreground thicks
                    val endLength =
                        if (singlePassThicks) max(masterProgressLength, masterTrackLength) else masterProgressLength
                    drawThicks(startLength, max(endLength - t, 0f), trackThickProgressColor, progressThickness)
                }
            }

            run {
                // Label
                drawCenteredText(center.x, center.y, labelText(), labelColor, labelSize, labelTypeface)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val paddingX = paddingLeft + paddingRight
        val paddingY = paddingTop + paddingBottom
        val contentW = w - paddingX
        val contentH = h - paddingY
        val radius = min(contentW, contentH) / 2f
        val centerX = paddingLeft + contentW / 2f
        val centerY = paddingTop + contentH / 2f
        contentRect.left = centerX - radius
        contentRect.right = centerX + radius
        contentRect.top = centerY - radius
        contentRect.bottom = centerY + radius
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // TODO Implement
    }

    private val contentRadius get() = contentRect.width() / 2f
    private val outerTrackRadius get() = (contentRect.width() - trackThickness * max(1f, thumbThicknessFactor)) / 2f

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

        private fun pickDistanceHit(distance: Float, thicknessFactor: Float) =
            abs(outerTrackRadius - distance) <= trackThickness / 2f * thicknessFactor

        private fun pickAngleLength(angle: Float) =
            normalizeAngle((angle - startAngle) * if (clockwise) -1f else 1f) / 360f

        private fun pickTapAngleLength(angle: Float): Float? {
            var l = pickAngleLength(angle) + masterTrackLength.previous
            if (l > masterTrackLength)
                l -= 1f
            return if (l >= lengthByValue(minValue)) l else null
        }

        private fun pickTap(angle: Float, distance: Float) =
            if (pickDistanceHit(distance, 1f)) pickTapAngleLength(angle) else null

        private val MotionEvent.offX get() = getX(actionIndex) - contentRect.centerX()
        private val MotionEvent.offY get() = getY(actionIndex) - contentRect.centerY()
        private val MotionEvent.distance get() = length(offX, offY)
        private val MotionEvent.angle get() = angle(offX, -offY)

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            if (e1 != null) {
                if (draggable && e1 != null && pickTap(e1.angle, e1.distance) != null
                    && e2 != null && pickDistanceHit(e2.distance, dragThicknessFactor)
                ) {
                    val naive = pickAngleLength(e2.angle) + masterTrackLength.previous
                    val valueLength = lengthByValue(value)
                    val rawValueLength = lengthByValue(rawValue)
                    fun getMinDiff(l: Float) = absMin(l - valueLength, l - rawValueLength, l - masterProgressLength)
                    var bestDiff = 0f
                    var best: Float? = null
                    val circ = outerTrackRadius * 2f * PI
                    for (o in -1..+1) {
                        val c = naive + o
                        val diff = getMinDiff(c)
                        if ((best == null && diff * circ <= DRAG_ARC_SNAP_THRESHOLD) || diff < bestDiff) {
                            bestDiff = diff
                            best = c
                        }
                    }
                    if (best != null)
                        rawValue = valueByLength(best)
                    return true
                } else if (scrollableX || scrollableY) {
                    val r = contentRadius
                    if (r > 0 && e1.distance <= r * SCROLL_HOLD_RADIUS_FACTOR) {
                        val factor = SCROLL_RADIUS_FACTOR * trackValue / lerp(
                            r,
                            SCROLL_REASONABLE_RADIUS,
                            SCROLL_REASONABLE_RADIUS_INFLUENCE
                        )
                        if (scrollableX)
                            rawValue += distanceX * factor
                        if (scrollableY)
                            rawValue += distanceY * factor
                        return true
                    }
                }
            }
            return false
        }

        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            if (tappable && e != null) {
                val length = pickTap(e.angle, e.distance)
                if (length != null) {
                    rawValue = valueByLength(length)
                    return true
                }
            }
            return false
        }

    }).apply {
        setIsLongpressEnabled(false)
        setOnDoubleTapListener(null)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                rawValue -= keyboardStep
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                rawValue += keyboardStep
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                rawValue += keyboardStep * 2f
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                rawValue -= keyboardStep * 2f
                true
            }
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                rawValue = (value / trackValue).previous * trackValue
                true
            }
            KeyEvent.KEYCODE_PAGE_UP -> {
                rawValue = (value / trackValue).next * trackValue
                true
            }
            KeyEvent.KEYCODE_MOVE_HOME -> {
                rawValue = minValue
                true
            }
            KeyEvent.KEYCODE_MOVE_END -> {
                rawValue = maxValue
                true
            }
            KeyEvent.KEYCODE_ESCAPE -> {
                clearFocus()
                true
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        thumbActiveness = if (gainFocus) 1f else 0f
    }

}