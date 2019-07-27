package com.francescozoccheddu.knob

import android.animation.TimeAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.annotation.*
import androidx.annotation.IntRange
import kotlin.math.*
import kotlin.reflect.KMutableProperty0


class KnobView : View {

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        context?.theme?.obtainStyledAttributes(attrs, R.styleable.KnobView, 0, 0)?.apply {
            try {
                // TODO Implement
            } finally {
                recycle()
            }
        }
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    companion object {
        const val MAX_REVOLUTION_COUNT = 3
        const val GLOBAL_SMOOTHNESS_FACTOR = 1f / 4f
        private val INPUT_DRAG_ARC_SNAP_THRESHOLD = 60f.dp
        private const val LENGTH_SNAP_THRESHOLD = 1f / 500f
        private const val THICKNESS_FACTOR_SNAP_THRESHOLD = 1f / 1000f
        private const val RADIUS_FACTOR_SNAP_THRESHOLD = 1f / 1000f
        private const val COLOR_SNAP_THRESHOLD = 2f
        private const val MIN_COLLAPSING_TRACK_LENGTH = 1f / 4f
        private const val SCROLL_HOLD_RADIUS_FACTOR = 1f / 2f
        private const val SCROLL_FACTOR = 1f / 2f
        private val TRACK_INDICES = 0 until MAX_REVOLUTION_COUNT

        fun makeColorList(@FloatRange(from = 0.0, to = 360.0) fromHue: Float,
                          @FloatRange(from = 0.0, to = 1.0) fromSat: Float,
                          @FloatRange(from = 0.0, to = 1.0) fromValue: Float,
                          @FloatRange(from = 0.0, to = 1.0) fromAlpha: Float,
                          @FloatRange(from = 0.0, to = 360.0) toHue: Float,
                          @FloatRange(from = 0.0, to = 1.0) toSat: Float,
                          @FloatRange(from = 0.0, to = 1.0) toValue: Float,
                          @FloatRange(from = 0.0, to = 1.0) toAlpha: Float,
                          @IntRange(from = 1, to = MAX_REVOLUTION_COUNT.toLong()) count: Int = MAX_REVOLUTION_COUNT): Iterable<Int> {
            val list = IntArray(count)
            for (i in 0 until count) {
                val a = i.f / max(count - 1, 1).f
                list[i] = hsv(lerp(fromHue, toHue, a), lerp(fromSat, toSat, a), lerp(fromValue, toValue, a), lerp(fromAlpha, toAlpha, a))
            }
            return list.asIterable()
        }

        fun makeColorList(@ColorInt from: Int, @ColorInt to: Int,
                          @IntRange(from = 1, to = MAX_REVOLUTION_COUNT.toLong()) count: Int = MAX_REVOLUTION_COUNT): Iterable<Int> {
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
            invalidate()
        }
    var maxValue = 300f
        set(value) {
            field = value
            invalidate()
        }
    var startValue = 0f
        set(value) {
            field = value
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
            rawValue = value
        }
    var revolutionValue = 100f
        set(value) {
            field = value
            invalidate()
        }
    @Dimension
    var thickness = 32f.dp
        set(value) {
            field = value
            invalidate()
        }
    @FloatRange(from = 0.0, to = 1.0)
    var trackThicknessFactor = 0.9f
        set(value) {
            field = value
            invalidate()
        }
    var startAngle = 90f
        set(value) {
            field = value
            invalidate()
        }
    @FloatRange(from = 0.7, to = 1.0)
    var revolutionRadiusBackoff = 0.9f
    @FloatRange(from = 0.7, to = 1.0)
    var revolutionThicknessBackoff = 0.9f
    @FloatRange(from = 1.0, to = 3.0)
    var inputThicknessFactor = 3f
    var scrollable = true
    var tappable = true
    var draggable = true
    @FloatRange(from = 0.0, to = 1.0)
    var progressSmoothness = 0.4f
    @FloatRange(from = 0.0, to = 1.0)
    var trackLayoutSmoothness = 0.4f
    @FloatRange(from = 0.0, to = 1.0)
    var trackLengthSmoothness = 0.4f
    var clockwise = true
        set(value) {
            field = value
            invalidate()
        }
    @FloatRange(from = 0.0)
    var snap = 0f
    @IntRange(from = 0, to = 20)
    var labelThicks = 0
        set(value) {
            field = value
            invalidate()
        }
    var trackColorProvider: ColorProvider = ConstantColor(hsv(0f, 0f, 0.1f))
    var progressColorProvider: ColorProvider = ColorByOrder(makeColorList(180f, 0.75f, 0.75f, 1f, 190f, 0.5f, 0.5f, 1f))
    var labelColorProvider: ColorProvider = ColorByOrder(listOf(Color.WHITE, Color.TRANSPARENT))
    var labelProvider: LabelProvider = object : LabelProvider {
        override fun provide(view: KnobView, track: Int, thick: Int, value: Float): String {
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
                    @IntRange(from = 0, to = MAX_REVOLUTION_COUNT - 1L) revolution: Int,
                    @IntRange(from = -MAX_REVOLUTION_COUNT + 1L, to = MAX_REVOLUTION_COUNT - 1L) order: Int): Int

    }

    abstract class ColorListProvider(@Size(min = 1, max = MAX_REVOLUTION_COUNT.toLong()) colors: Iterable<Int>) : ColorProvider {

        private val colors = colors.toList()

        protected abstract fun getIndex(view: KnobView,
                                        @IntRange(from = 0, to = MAX_REVOLUTION_COUNT - 1L) track: Int,
                                        @IntRange(from = -MAX_REVOLUTION_COUNT + 1L, to = MAX_REVOLUTION_COUNT - 1L) order: Int): Int

        override fun provide(view: KnobView, revolution: Int, order: Int): Int {
            return colors[min(getIndex(view, revolution, order), colors.lastIndex)]
        }

    }

    class ConstantColor(@ColorInt val color: Int) : ColorProvider {
        override fun provide(view: KnobView, revolution: Int, order: Int): Int {
            return color
        }
    }

    class ColorByOrder(@Size(min = 1, max = MAX_REVOLUTION_COUNT.toLong()) colors: Iterable<Int>) : ColorListProvider(colors) {
        override fun getIndex(view: KnobView, track: Int, order: Int): Int {
            return max(order, 0)
        }
    }

    class ColorByRevolution(@Size(min = 1, max = MAX_REVOLUTION_COUNT.toLong()) colors: Iterable<Int>) : ColorListProvider(colors) {
        override fun getIndex(view: KnobView, revolution: Int, order: Int): Int {
            return revolution
        }
    }

    interface LabelProvider {

        fun provide(view: KnobView,
                    @IntRange(from = 0, to = MAX_REVOLUTION_COUNT - 1L) track: Int,
                    @IntRange(from = 0) thick: Int,
                    value: Float): String

    }

    fun setTrackColor(@ColorInt color: Int) {
        trackColorProvider = ConstantColor(color)
    }

    fun setTrackColorsByRevolution(@Size(min = 1, max = MAX_REVOLUTION_COUNT.toLong()) colors: Iterable<Int>) {
        trackColorProvider = ColorByRevolution(colors)
    }

    fun setTrackColorsByOrder(@Size(min = 1, max = MAX_REVOLUTION_COUNT.toLong()) colors: Iterable<Int>) {
        trackColorProvider = ColorByOrder(colors)
    }

    fun setProgressColor(@ColorInt color: Int) {
        progressColorProvider = ConstantColor(color)
    }

    fun setProgressColorsByRevolution(@Size(min = 1, max = MAX_REVOLUTION_COUNT.toLong()) colors: Iterable<Int>) {
        progressColorProvider = ColorByRevolution(colors)
    }

    fun setProgressColorsByIndex(@Size(min = 1, max = MAX_REVOLUTION_COUNT.toLong()) colors: Iterable<Int>) {
        progressColorProvider = ColorByOrder(colors)
    }

    private var rawValue = 50f

    private inner class ColorF {

        var r = 0f
        var g = 0f
        var b = 0f
        var a = 0f

        private val Float.b8 get() = this.roundToInt()

        var int
            get() = Color.argb(a.b8, r.b8, g.b8, b.b8)
            set(value) {
                r = Color.red(value).f
                g = Color.green(value).f
                b = Color.blue(value).f
                a = Color.alpha(value).f
            }

        fun smooth(target: Int, smoothness: Float, elapsed: Float) {
            val old = int
            val tr = Color.red(target).f
            val tg = Color.green(target).f
            val tb = Color.blue(target).f
            val ta = Color.alpha(target).f
            val s = smoothness * GLOBAL_SMOOTHNESS_FACTOR
            r = smooth(r, tr, s, elapsed).snap(tr, COLOR_SNAP_THRESHOLD)
            g = smooth(g, tg, s, elapsed).snap(tg, COLOR_SNAP_THRESHOLD)
            b = smooth(b, tb, s, elapsed).snap(tb, COLOR_SNAP_THRESHOLD)
            a = smooth(a, ta, s, elapsed).snap(ta, COLOR_SNAP_THRESHOLD)
            if (old != int) invalidate()
        }

    }

    private fun lengthByValue(value: Float) = (value - startValue) / revolutionValue

    private fun valueByLength(length: Float) = length * revolutionValue + startValue

    private fun KMutableProperty0<Float>.smooth(target: Float, smoothness: Float, elapsed: Float, snapThreshold: Float, min: Float, max: Float) {
        val before = get()
        set(smooth(before, target, smoothness * GLOBAL_SMOOTHNESS_FACTOR, elapsed).snap(target, snapThreshold).clamp(min, max))
        if (before != get()) invalidate()
    }

    private inner class Revolution(val index: Int) {

        private var thicknessFactor = 0f
        private var thumbActiveness = 0f
        private var radiusFactor = 1f
        private val backgroundColor = ColorF()
        private val foregroundColor = ColorF()
        private val labelColor = ColorF()

        fun update(elapsed: Float) {
            val order = if (progressLength == 0f && index == 0) 0 else (progressLength - index).previous
            val positiveOrder = max(order, 0)
            run {
                val target = if (order < 0 && trackLength - index < MIN_COLLAPSING_TRACK_LENGTH) 0f
                else revolutionThicknessBackoff.pow(positiveOrder)
                ::thicknessFactor.smooth(target, trackLayoutSmoothness, elapsed, THICKNESS_FACTOR_SNAP_THRESHOLD, 0f, 1f)
            }
            run {
                val target = if (order == 0 && thumbEnabled) 1f else 0f
                ::thumbActiveness.smooth(target, trackLayoutSmoothness, elapsed, 1f / 1000f, 0f, 1f)
            }
            run {
                val target = revolutionRadiusBackoff.pow(positiveOrder)
                ::radiusFactor.smooth(target, trackLayoutSmoothness / 2f, elapsed, RADIUS_FACTOR_SNAP_THRESHOLD, 0f, 1f)
            }
            run {
                backgroundColor.smooth(trackColorProvider.provide(this@KnobView, index, order), trackLayoutSmoothness, elapsed)
                foregroundColor.smooth(progressColorProvider.provide(this@KnobView, index, order), trackLayoutSmoothness, elapsed)
                labelColor.smooth(labelColorProvider.provide(this@KnobView, index, order), trackLayoutSmoothness, elapsed)
            }
        }

        fun finishLayoutSmoothing() {
            val order = if (progressLength == 0f && index == 0) 0 else (progressLength - index).previous
            val positiveOrder = max(order, 0)
            thicknessFactor = if (order < 0 && trackLength - index < MIN_COLLAPSING_TRACK_LENGTH) 0f
            else revolutionThicknessBackoff.pow(positiveOrder)
            thumbActiveness = if (order == 0 && thumbEnabled) 1f else 0f
            radiusFactor = revolutionRadiusBackoff.pow(positiveOrder)
            backgroundColor.int = trackColorProvider.provide(this@KnobView, index, order)
            foregroundColor.int = progressColorProvider.provide(this@KnobView, index, order)
            labelColor.int = labelColorProvider.provide(this@KnobView, index, order)
        }

        fun draw(canvas: Canvas) {
            val center = contentRect.center
            val r = outerTrackRadius * radiusFactor
            val tf = lerp(1f, min(1f, thumbThicknessFactor), thumbActiveness) * thicknessFactor

            val minValueLength = lengthByValue(minValue)
            fun getSweep(length: Float) =
                (if (index == 0) min(length, 1f) - minValueLength else (length - index).clamp(0f, 1f)) * 360f

            val startAngle = (if (index == 0) minValueLength * 360f else 0f) - startAngle

            val sign = if (clockwise) -1f else 1f

            canvas.drawTrack(center, r, startAngle, sign * getSweep(trackLength), backgroundColor.int, thickness * tf * trackThicknessFactor)
            val progressSweep = sign * getSweep(progressLength)
            canvas.drawTrack(center, r, startAngle, progressSweep, foregroundColor.int, thickness * tf)
            canvas.drawTrack(center,
                             r,
                             startAngle - progressSweep,
                             0f,
                             foregroundColor.int,
                             thickness * max(1f, thumbThicknessFactor) * thumbActiveness)
            if (labelColor.a > 0f && thicknessFactor > 0f) {
                for (i in 0 until labelThicks) {
                    val lrl = i.f / labelThicks.f
                    val angle = lrl * 360f
                    val text = labelProvider.provide(this@KnobView, index, i, valueByLength(index + lrl))
                    canvas.drawThick(center, r, -startAngle + sign * angle, text, labelColor.int, labelSize * thicknessFactor, labelTypeface)
                }
            }

        }

    }

    private val tracks = TRACK_INDICES.map { Revolution(it) }.toTypedArray()

    private fun updateValue(elapsed: Float) {
        rawValue = rawValue.clamp(minValue, maxValue)
        val valueLength = lengthByValue(value)
        val minValueLength = lengthByValue(minValue)
        val maxValueLength = lengthByValue(maxValue)
        ::progressLength.smooth(valueLength, progressSmoothness, elapsed, LENGTH_SNAP_THRESHOLD, minValueLength, maxValueLength)
        run {
            val targetTrackLength = max(ceil(valueLength), 1f).clamp(progressLength, maxValueLength)
            ::trackLength.smooth(targetTrackLength, trackLengthSmoothness, elapsed, LENGTH_SNAP_THRESHOLD, progressLength, maxValueLength)
        }
    }

    private fun validate() {
        if (minValue > maxValue) throw IllegalStateException("'${::minValue.name}' cannot be greater than '${::maxValue.name}'")
        if (startValue > minValue) throw IllegalStateException("'${::startValue.name}' cannot be greater than '${::minValue.name}'")
        if (revolutionValue <= 0) throw IllegalStateException("'${::revolutionValue.name}' must be positive")
        if ((maxValue - startValue) / revolutionValue > MAX_REVOLUTION_COUNT) throw IllegalStateException("Revolution count cannot be greater than $MAX_REVOLUTION_COUNT")
        if (lengthByValue(minValue) >= 1f) throw IllegalStateException("'${::minValue.name}' does not fall inside the first revolution")
    }

    private val animator = TimeAnimator().apply {
        setTimeListener { _, _, elapsedMillis ->
            validate()
            val elapsed = elapsedMillis / 1000f
            updateValue(elapsed)
            tracks.forEach { it.update(elapsed) }
        }
    }

    private val contentRect = RectF()

    private var progressLength = rawValue
    private var trackLength = 0f

    fun finishLayoutSmoothing() {
        tracks.forEach { it.finishLayoutSmoothing() }
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

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) animator.start()
        else animator.cancel()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.apply {
            tracks.forEach { it.draw(this) }
            clipTest(contentRect.center, outerTrackRadius, 30f, 130f, thickness)
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

    private fun pickLengthAt(distance: Float, angle: Float, @FloatRange(from = 0.0) thicknessFactor: Float): Float? {
        val d = abs(outerTrackRadius - distance)
        val maxD = thickness / 2f * thicknessFactor
        if (d <= maxD) {
            val ua = (angle - startAngle) * if (clockwise) -1f else 1f
            val frl = normalizeAngle(ua) / 360f
            var lrl = frl + max(lengthByValue(rawValue).previous, 0)
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
                // FIXME Handle scroll wheel separately
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
                // TODO Snap to maxValue
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
}