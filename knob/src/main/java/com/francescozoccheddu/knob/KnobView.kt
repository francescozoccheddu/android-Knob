package com.francescozoccheddu.knob

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.*
import androidx.annotation.IntRange
import com.francescozoccheddu.animatorhelpers.ABFloat
import com.francescozoccheddu.animatorhelpers.SmoothFloat
import com.francescozoccheddu.animatorhelpers.SpringFloat
import kotlin.math.*


class KnobView : View {

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    companion object {
        private const val MAX_REVOLUTIONS = 3
        const val GLOBAL_SMOOTHNESS_FACTOR = 1f / 4f
        private val INPUT_DRAG_ARC_SNAP_THRESHOLD = 60f.dp
        private const val LENGTH_SNAP_THRESHOLD = 1f / 500f
        private const val THICKNESS_FACTOR_SNAP_THRESHOLD = 1f / 1000f
        private const val RADIUS_FACTOR_SNAP_THRESHOLD = 1f / 1000f
        private const val COLOR_SNAP_THRESHOLD = 2f
        private const val MIN_COLLAPSING_TRACK_LENGTH = 1f / 4f
        private const val SCROLL_HOLD_RADIUS_FACTOR = 1f / 2f
        private const val SCROLL_FACTOR = 1f / 2f
        private const val THICK_CULLING_LENGHT_PADDING = 1f / 10f

        fun makeColorList(@FloatRange(from = 0.0, to = 360.0) fromHue: Float,
                          @FloatRange(from = 0.0, to = 1.0) fromSat: Float,
                          @FloatRange(from = 0.0, to = 1.0) fromValue: Float,
                          @FloatRange(from = 0.0, to = 1.0) fromAlpha: Float,
                          @FloatRange(from = 0.0, to = 360.0) toHue: Float,
                          @FloatRange(from = 0.0, to = 1.0) toSat: Float,
                          @FloatRange(from = 0.0, to = 1.0) toValue: Float,
                          @FloatRange(from = 0.0, to = 1.0) toAlpha: Float,
                          @IntRange(from = 1) count: Int): Iterable<Int> {
            val list = IntArray(count)
            for (i in 0 until count) {
                val a = i.f / max(count - 1, 1).f
                list[i] = hsv(lerp(fromHue, toHue, a), lerp(fromSat, toSat, a), lerp(fromValue, toValue, a), lerp(fromAlpha, toAlpha, a))
            }
            return list.asIterable()
        }

        fun makeColorList(@ColorInt from: Int, @ColorInt to: Int,
                          @IntRange(from = 1) count: Int): Iterable<Int> {
            val fromR = Color.red(from).f
            val fromG = Color.green(from).f
            val fromB = Color.blue(from).f
            val fromA = Color.alpha(from).f
            val toR = Color.red(to).f
            val toG = Color.green(to).f
            val toB = Color.blue(to).f
            val toA = Color.alpha(to).f
            val list = IntArray(count)
            for (i in 0 until count) {
                val a = i.f / max(count - 1, 1).f
                list[i] = Color.argb(lerp(fromA, toA, a).roundToInt(),
                                     lerp(fromR, toR, a).roundToInt(),
                                     lerp(fromG, toG, a).roundToInt(),
                                     lerp(fromB, toB, a).roundToInt())
            }
            return list.asIterable()
        }

    }

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
    var maxValue = 300f
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
    var revolutionValue = 100f
        set(value) {
            if (revolutionValue <= 0.0f)
                throw IllegalArgumentException("'${::revolutionValue.name}' must be positive")
            field = value
            invalidate()
        }
    @Dimension
    var thickness = 32f.dp
        set(value) {
            if (revolutionValue <= 0.0f)
                throw IllegalArgumentException("'${::thickness.name}' must be positive")
            field = value
            invalidate()
        }
    @FloatRange(from = 0.0, to = 1.0)
    var trackThicknessFactor = 0.9f
        set(value) {
            if (revolutionValue !in 0f..1f)
                throw IllegalArgumentException("'${::trackThicknessFactor.name}' does not fall in [0,1] range")
            field = value
            invalidate()
        }
    var startAngle = 90f
        set(value) {
            field = value
            invalidate()
        }
    var radiusFactorProvider = FactorByOrderBackoff(0.9f)
    var thicknessFactorProvider = FactorByOrderBackoff(0.9f)
    @FloatRange(from = 1.0, to = 3.0)
    var inputThicknessFactor = 3f
    var scrollable = true
    var tappable = true
    var draggable = true
    var clockwise = true
        set(value) {
            field = value
            invalidate()
        }
    @FloatRange(from = 0.0)
    var snap = 0f
    @IntRange(from = 0, to = 20)
    var labelThicks = 12
        set(value) {
            field = value
            invalidate()
        }
    var trackColorProvider: ColorProvider = ConstantColor(hsv(0f, 0f, 0.1f))
    var progressColorProvider: ColorProvider = ColorByOrder(makeColorList(180f, 0.75f, 0.75f, 1f, 190f, 0.5f, 0.5f, 1f, 3))
    var labelBackgroundColorProvider: ColorProvider? = ColorByOrder(listOf(Color.DKGRAY, Color.TRANSPARENT))
    var labelForegroundColorProvider: ColorProvider = ColorByOrder(listOf(Color.WHITE, Color.TRANSPARENT))
    var labelProvider: LabelProvider = object : LabelProvider {
        override fun provide(view: KnobView, revolution: Int, thick: Int, value: Float): String {
            return thick.toString()
        }
    }
        set(value) {
            field = value
            invalidate()
        }
    @Dimension
    var labelSize = 16f.dp
        set(value) {
            field = value
            invalidate()
        }
    var labelTypeface: Typeface = Typeface.DEFAULT
        set(value) {
            field = value
            invalidate()
        }
    @FloatRange(from = 0.0, to = 2.0)
    var thumbThicknessFactor = 0.75f
    var thumbEnabled = false

    interface ColorProvider {

        @ColorInt
        fun provide(view: KnobView,
                    @IntRange(from = 0) revolution: Int,
                    order: Int): Int
    }

    abstract class ColorListProvider(@Size(min = 1) colors: Iterable<Int>) : ColorProvider {

        private val colors = colors.toList()

        protected abstract fun getIndex(view: KnobView,
                                        @IntRange(from = 0) revolution: Int,
                                        order: Int): Int

        override fun provide(view: KnobView, revolution: Int, order: Int): Int {
            return colors[min(getIndex(view, revolution, order), colors.lastIndex)]
        }

    }

    class ConstantColor(@ColorInt val color: Int) : ColorProvider {
        override fun provide(view: KnobView, revolution: Int, order: Int): Int {
            return color
        }
    }

    class ColorByOrder(@Size(min = 1) colors: Iterable<Int>) : ColorListProvider(colors) {
        override fun getIndex(view: KnobView, revolution: Int, order: Int): Int {
            return max(order, 0)
        }
    }

    class ColorByRevolution(@Size(min = 1) colors: Iterable<Int>) : ColorListProvider(colors) {
        override fun getIndex(view: KnobView, revolution: Int, order: Int): Int {
            return revolution
        }
    }

    interface LabelProvider {

        fun provide(view: KnobView,
                    @IntRange(from = 0) revolution: Int,
                    @IntRange(from = 0) thick: Int,
                    value: Float): String

    }

    interface FactorProvider {

        fun provide(view: KnobView,
                    @IntRange(from = 0) revolution: Int,
                    order: Int): Float

    }

    class FactorByOrderBackoff(@FloatRange(from = 0.0, to = 1.0) val backoff: Float) : FactorProvider {

        override fun provide(view: KnobView, revolution: Int, order: Int): Float = backoff.pow(max(0, order).f)

    }

    class FactorByRevolutionBackoff(@FloatRange(from = 0.0, to = 1.0) val backoff: Float) : FactorProvider {

        override fun provide(view: KnobView, revolution: Int, order: Int): Float = backoff.pow(max(0, revolution).f)

    }

    abstract class FactorListProvider(@Size(min = 1) factors: Iterable<Float>) : FactorProvider {

        private val factors = factors.toList()

        protected abstract fun getIndex(view: KnobView,
                                        @IntRange(from = 0) revolution: Int,
                                        order: Int): Int

        override fun provide(view: KnobView, revolution: Int, order: Int): Float {
            return factors[min(getIndex(view, revolution, order), factors.lastIndex)]
        }

    }

    class FactorByOrder(@Size(min = 1) factors: Iterable<Float>) : FactorListProvider(factors) {
        override fun getIndex(view: KnobView, revolution: Int, order: Int): Int {
            return max(order, 0)
        }
    }

    class FactorByRevolution(@Size(min = 1) factors: Iterable<Float>) : FactorListProvider(factors) {
        override fun getIndex(view: KnobView, revolution: Int, order: Int): Int {
            return revolution
        }
    }

    private operator fun ColorProvider.invoke(view: KnobView,
                                              @IntRange(from = 0) revolution: Int,
                                              order: Int): Int = provide(view, revolution, order)

    private operator fun LabelProvider.invoke(view: KnobView,
                                              @IntRange(from = 0) revolution: Int,
                                              @IntRange(from = 0) thick: Int,
                                              value: Float): String = provide(view, revolution, thick, value)

    private operator fun FactorProvider.invoke(view: KnobView,
                                               @IntRange(from = 0) revolution: Int,
                                               order: Int): Float = provide(view, revolution, order)

    private var rawValue = 50f
        set(value) {
            field = value.clamp(minValue, maxValue)
            updateLength()
        }

    // Drawing properties
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
    private var trackLength by ABFloat(0f).apply {
        onUpdate = {
            order = max(it.value.nextDown().toInt(), 0).f
            invalidate()
        }
        interpolator = DecelerateInterpolator()
        speed = 4f
    }
    private var progressLength by SmoothFloat(0f).apply {
        onUpdate = { invalidate() }
        smoothness = 0.1f
        snap = 0.01f
    }

    private fun lengthByValue(value: Float) = (value - startValue) / revolutionValue

    private fun valueByLength(length: Float) = length * revolutionValue + startValue

    private val contentRect = RectF()

    fun finishLayoutSmoothing() {
        invalidate()
    }

    fun finishValueSmoothing() {
        progressLength = lengthByValue(value)
        invalidate()
    }

    fun finishTrackSmoothing() {
        trackLength = max(ceil(lengthByValue(value)), 1f).clamp(progressLength, lengthByValue(maxValue))
        invalidate()
    }

    fun finishSmoothing() {
        finishValueSmoothing()
        finishTrackSmoothing()
        finishLayoutSmoothing()
    }

    private fun updateLength() {
        val length = lengthByValue(value)
        progressLength = length
        trackLength = min(max(ceil(length), 1f), lengthByValue(maxValue))
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
                max(0, orderCount - MAX_REVOLUTIONS)..orderCount
            }) {
                val prevOrder = (order - t).floorToInt()
                val prevPositiveOrder = max(0, prevOrder)
                val nextOrder = (order - t).ceilToInt()
                val alpha = order - floor(order)
                fun getSweep(length: Float) = (if (t == 0) min(length, 1f) - minValueLength else (length - t).clamp01) * 360f
                val startLength = if (t == 0) minValueLength else 0f
                val trackStartAngle = startLength * 360f * angleSign + startAngle
                val radius = lerp(radiusFactorProvider(this@KnobView, t, prevPositiveOrder),
                                  radiusFactorProvider(this@KnobView, t, nextOrder), alpha) * outerTrackRadius
                val thicknessFactor = lerp(if (prevOrder >= 0) thicknessFactorProvider(this@KnobView, t, prevOrder) else 0f,
                                           thicknessFactorProvider(this@KnobView, t, nextOrder), alpha)
                val baseThickness = thicknessFactor * thickness
                val trackThumbActiveness = (1f - min(abs(order - t), 1f)) * thumbActiveness
                val progressThickness = lerp(baseThickness, baseThickness * min(thumbThicknessFactor, 1f), trackThumbActiveness)

                fun drawThicks(startLength: Float,
                               endLength: Float,
                               color: Int,
                               trackThickness: Float) {
                    if (Color.alpha(color) > 0 && progressThickness > 0f && labelThicks > 0) {
                        var clip = false
                        val interspace = (1f / labelThicks)
                        val range = if (startLength <= 0.0f && endLength >= 1.0f) 0 until labelThicks
                        else run {
                            fun getEndPoint(length: Float, start: Boolean): Int {
                                val p = if (start)
                                    max(((length - THICK_CULLING_LENGHT_PADDING) / interspace).ceilToInt(), 0)
                                else
                                    min(((length + THICK_CULLING_LENGHT_PADDING) / interspace).floorToInt(), labelThicks - 1)
                                clip = clip || abs(p * interspace - length) <= THICK_CULLING_LENGHT_PADDING
                                return p
                            }
                            getEndPoint(startLength, true)..getEndPoint(endLength, false)
                        }
                        if (clip) {
                            save()
                            canvas.clipTrack(center, radius, trackStartAngle, getSweep(endLength + t) * angleSign, trackThickness)
                        }
                        for (thick in range) {
                            val length = interspace * thick
                            val angle = startAngle + length * 360f * angleSign
                            val text = labelProvider(this@KnobView, t, thick, valueByLength(t + length))
                            drawThick(center, radius, angle, text, color, labelSize * thicknessFactor, labelTypeface)
                        }
                        if (clip)
                            restore()
                    }
                }

                val labelBackgroundColor = run {
                    val provider = labelBackgroundColorProvider
                    if (provider != null)
                        lerpColor(provider(this@KnobView, t, prevPositiveOrder),
                                  provider(this@KnobView, t, nextOrder), alpha)
                    else Color.TRANSPARENT
                }
                val labelForegroundColor = lerpColor(labelForegroundColorProvider(this@KnobView, t, prevPositiveOrder),
                                                     labelForegroundColorProvider(this@KnobView, t, nextOrder), alpha)
                val singlePassLabel = labelBackgroundColorProvider == null || run {
                    val bg = labelBackgroundColor
                    val fg = labelForegroundColor
                    fg.alpha == 255 && fg.red == bg.red && fg.green == bg.green && fg.blue == bg.blue
                }

                run {
                    // Track
                    val color = lerpColor(trackColorProvider(this@KnobView, t, prevPositiveOrder),
                                          trackColorProvider(this@KnobView, t, nextOrder), alpha)
                    drawTrack(center, radius, trackStartAngle, getSweep(trackLength) * angleSign, color, progressThickness * trackThicknessFactor)
                }
                run {
                    val progressColor = lerpColor(progressColorProvider(this@KnobView, t, prevPositiveOrder),
                                                  progressColorProvider(this@KnobView, t, nextOrder), alpha)
                    // Background labels
                    if (!singlePassLabel) {
                        val startLength = if (progressColor.alpha < 255) startLength else max((progressLength - t), 0f)
                        drawThicks(startLength, max(trackLength - t, 0f), labelBackgroundColor, progressThickness * trackThicknessFactor)
                    }
                    // Progress
                    val sweep = getSweep(progressLength) * angleSign
                    drawTrack(center, radius, trackStartAngle, sweep, progressColor, progressThickness)
                    // Thumb
                    run {
                        val thumbThickness = lerp(baseThickness, baseThickness * max(thumbThicknessFactor, 1f), trackThumbActiveness)
                        drawTrack(center, radius, trackStartAngle + sweep, 0f, progressColor, thumbThickness)
                    }
                }
                run {
                    // Foreground labels
                    val endLength = if (singlePassLabel) max(progressLength, trackLength) else progressLength
                    drawThicks(startLength, max(endLength - t, 0f), labelForegroundColor, progressThickness)
                }
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

    private fun pickLengthAt(distance: Float, angle: Float, thicknessFactor: Float): Float? {
        val d = abs(outerTrackRadius - distance)
        val maxD = thickness / 2f * thicknessFactor
        if (d <= maxD) {
            val ua = (angle - startAngle) * if (clockwise) -1f else 1f
            val frl = normalizeAngle(ua) / 360f
            var lrl = frl + max(lengthByValue(rawValue).nextDown().toInt(), 0)
            if (lrl > lengthByValue(maxValue)) lrl -= 1f
            if (lrl >= lengthByValue(minValue)) return lrl
        }
        return null
    }

    private val contentRadius get() = contentRect.width() / 2f
    private val outerTrackRadius get() = (contentRect.width() - thickness * max(1f, thumbThicknessFactor)) / 2f

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

        private val MotionEvent.offX get() = getX(actionIndex) - contentRect.centerX()
        private val MotionEvent.offY get() = getY(actionIndex) - contentRect.centerY()
        private val MotionEvent.distance get() = length(offX, offY)

        private fun pickLengthAt(event: MotionEvent, thicknessFactor: Float): Float? {
            val nx = event.offX
            val ny = event.offY
            return pickLengthAt(length(nx, ny), angle(nx, -ny), thicknessFactor)
        }


        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            if (e1 != null) {
                if (draggable && e2 != null && pickLengthAt(e1, inputThicknessFactor) != null) {
                    val length = pickLengthAt(e2, inputThicknessFactor)
                    if (length != null) {
                        val rawLength = lengthByValue(rawValue)
                        val snappedLength = lengthByValue(value)
                        val minLength = lengthByValue(minValue)
                        val maxLength = lengthByValue(maxValue)
                        fun trySet(length: Float): Boolean {
                            if (length in minLength..maxLength) {
                                val minDiff = absMin(length - rawLength, length - snappedLength, length - progressLength)
                                if (minDiff * Math.PI * 2 * outerTrackRadius <= INPUT_DRAG_ARC_SNAP_THRESHOLD) {
                                    rawValue = valueByLength(length)
                                    return true
                                }
                            }
                            return false
                        }
                        // FIXME Not working if minValue > 0f
                        // TODO Choose closest
                        return trySet(length) || trySet(length + 1f) || trySet(length - 1f)
                    }
                } else if (scrollable) {
                    val r = contentRadius
                    if (r > 0 && e1.distance <= r * SCROLL_HOLD_RADIUS_FACTOR) {
                        // REVIEW Size relative or absolute dp amount?
                        rawValue += distanceY / r * SCROLL_FACTOR * revolutionValue
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
                val length = pickLengthAt(e, inputThicknessFactor)
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

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
        // TODO Keyboard arrows
    }

    // TODO Focusable

}