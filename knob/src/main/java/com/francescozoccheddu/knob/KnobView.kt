package com.francescozoccheddu.knob

import android.animation.TimeAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.FloatRange
import java.lang.Math.toRadians
import kotlin.math.*
import kotlin.reflect.KMutableProperty0

class KnobView : View {

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

    private fun getLength(value: Float) = (value - startValue) / revolutionValue

    private fun KMutableProperty0<Float>.smooth(target: Float, smoothness: Float, elapsed: Float, snapThreshold: Float, min: Float, max: Float) {
        val before = get()
        set(smooth(before, target, smoothness * GLOBAL_SMOOTHNESS_FACTOR, elapsed).snap(target, snapThreshold).clamp(min, max))
        if (before != get()) invalidate()
    }

    private inner class Revolution(val index: Int) {

        private var thicknessFactor = 0f
        private var radiusFactor = 1f
        private val backgroundColor = ColorF()
        private val foregroundColor = ColorF()
        // TODO Add labels if last (fade animation)

        fun update(elapsed: Float) {
            val order = floor(progressLength - index).toInt()
            val positiveOrder = max(order, 0)
            run {
                val target = if (order < 0 && trackLength - index < MIN_COLLAPSING_TRACK_LENGTH) 0f
                else Math.pow(revolutionThicknessBackoff.d, positiveOrder.d).f
                ::thicknessFactor.smooth(target, trackLayoutSmoothness, elapsed, THICKNESS_FACTOR_SNAP_THRESHOLD, 0f, 1f)
            }
            run {
                val target = Math.pow(revolutionRadiusBackoff.d, positiveOrder.d).f
                ::radiusFactor.smooth(target, trackLayoutSmoothness, elapsed, RADIUS_FACTOR_SNAP_THRESHOLD, 0f, 1f)
            }
            run {
                backgroundColor.smooth(trackColors[index], trackLayoutSmoothness, elapsed)
                foregroundColor.smooth(progressColors[positiveOrder], trackLayoutSmoothness, elapsed)
            }
        }

        fun finishAnimation() {

        }

        fun draw(canvas: Canvas) {
            trackPaint.strokeWidth = thickness * thicknessFactor
            val cx = contentRect.centerX()
            val cy = contentRect.centerY()
            val r = (contentRect.width() / 2f * radiusFactor) - trackPaint.strokeWidth

            val minValueLength = getLength(minValue)
            fun getSweep(length: Float) = (if (index == 0) min(length, 1f) - minValueLength else min(length - index, 1f)) * 360f

            val arcStart = (if (index == 0) minValueLength * 360f else 0f) - startAngle
            fun draw(sweep: Float, color: Int) {
                if (Color.alpha(color) > 0 && thicknessFactor > 0f) {
                    trackPaint.color = color
                    if (sweep > 0f) {
                        canvas.drawArc(cx - r, cy - r, cx + r, cy + r, arcStart, sweep, false, trackPaint)
                    } else {
                        val arcStartRad = toRadians(-arcStart.d).f
                        val x = cos(arcStartRad) * r
                        val y = -sin(arcStartRad) * r
                        canvas.drawPoint(cx + x, cy + y, trackPaint)
                    }
                }
            }

            draw(getSweep(trackLength), backgroundColor.int)
            draw(getSweep(progressLength), foregroundColor.int)
        }

    }

    private val tracks = TRACK_INDICES.map { Revolution(it) }.toTypedArray()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    companion object {
        const val MAX_REVOLUTION_COUNT = 3
        const val GLOBAL_SMOOTHNESS_FACTOR = 1f / 4f
        private const val LENGTH_SNAP_THRESHOLD = 1f / 500f
        private const val THICKNESS_FACTOR_SNAP_THRESHOLD = 1f / 1000f
        private const val RADIUS_FACTOR_SNAP_THRESHOLD = 1f / 1000f
        private const val COLOR_SNAP_THRESHOLD = 2f
        private const val MIN_COLLAPSING_TRACK_LENGTH = 1f / 4f
        private val TRACK_INDICES = 0..(MAX_REVOLUTION_COUNT - 1)
    }

    var minValue = 0f
        set(value) {
            field = value
            invalidate()
        }
    var maxValue = 100f
        set(value) {
            field = value
            invalidate()
        }
    var startValue = 0f
        set(value) {
            field = value
            invalidate()
        }
    var value = 50f
    var revolutionValue = maxValue
        set(value) {
            field = value
            invalidate()
        }
    @Dimension
    var thickness = 20f.dp
    @ColorInt
    val progressColors = TRACK_INDICES.map {
        val a = it / max(MAX_REVOLUTION_COUNT - 1, 1).f
        hsv(lerp(180f, 190f, a), lerp(0.75f, 0.5f, a), lerp(0.75f, 0.5f, a))
    }
    @ColorInt
    val trackColors = IntArray(MAX_REVOLUTION_COUNT).apply {
        fill(hsv(0f, 0f, 0.1f))
    }
    var trackColor
        get() = trackColors[0]
        set(value) = trackColors.fill(value)
    var progressColor
        get() = progressColors[0]
        set(value) = trackColors.fill(value)
    // degrees, counter-clockwise
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
    var inputThicknessFactor = 2f
    var tappable = false
    @FloatRange(from = 0.0, to = 1.0)
    var progressSmoothness = 0.4f
    @FloatRange(from = 0.0, to = 1.0)
    var trackLayoutSmoothness = 0.5f
    @FloatRange(from = 0.0, to = 1.0)
    var trackLengthSmoothness = 0.2f
    var clockwise = true

    private fun updateValue(elapsed: Float) {
        value = value.clamp(minValue, maxValue)
        val valueLength = getLength(value)
        val minValueLength = getLength(minValue)
        val maxValueLength = getLength(maxValue)
        ::progressLength.smooth(valueLength, progressSmoothness, elapsed, LENGTH_SNAP_THRESHOLD, minValueLength, maxValueLength)
        run {
            val targetTrackLength = max(ceil(valueLength), 1f).clamp(progressLength, maxValueLength)
            ::trackLength.smooth(targetTrackLength, trackLengthSmoothness, elapsed, LENGTH_SNAP_THRESHOLD, progressLength, maxValueLength)
        }
    }

    private val animator = TimeAnimator().apply {
        setTimeListener { _, _, elapsedMillis ->
            // Validation
            if (minValue > maxValue) throw IllegalStateException("'${::minValue.name}' cannot be greater than '${::maxValue.name}'")
            if (startValue > minValue) throw IllegalStateException("'${::startValue.name}' cannot be greater than '${::minValue.name}'")
            if (revolutionValue <= 0) throw IllegalStateException("'${::revolutionValue.name}' must be positive")
            if ((maxValue - startValue) / revolutionValue > MAX_REVOLUTION_COUNT) throw IllegalStateException("Revolution count cannot be greater than $MAX_REVOLUTION_COUNT")
            if (getLength(minValue) >= 1f) throw IllegalStateException("'${::minValue.name}' does not fall inside the first revolution")
            // Update
            val elapsed = elapsedMillis / 1000f
            updateValue(elapsed)
            tracks.forEach { it.update(elapsed) }
        }
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }

    private val contentRect = RectF()

    private var progressLength = value
    private var trackLength = 0f

    fun finishLayoutSmoothing() {

    }

    fun finishValueSmoothing() {
        progressLength = value
    }

    fun finishTrackSmoothing() {

    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) animator.start()
        else animator.pause()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.apply {
            tracks.forEach { it.draw(this) }
            // TODO Draw label
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

}