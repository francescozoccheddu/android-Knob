package com.francescozoccheddu.knob

import android.graphics.*
import android.text.TextPaint
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.FloatRange
import kotlin.math.cos
import kotlin.math.sin

// Track
private val trackRect = RectF()
private val trackPaint = Paint().apply {
    isAntiAlias = true
    strokeJoin = Paint.Join.ROUND
    strokeCap = Paint.Cap.ROUND
    style = Paint.Style.STROKE
}

internal fun Canvas.drawTrack(center: PointF,
                              @Dimension radius: Float,
                              startAngle: Float,
                              @FloatRange(from = 0.0, to = 360.0) sweep: Float,
                              @ColorInt color: Int,
                              @Dimension thickness: Float) {
    if (Color.alpha(color) > 0 && thickness > 0f) {
        trackRect.set(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        trackPaint.strokeWidth = thickness
        trackPaint.color = color
        if (sweep != 0f) {
            drawArc(trackRect, startAngle, -sweep, false, trackPaint)
        } else {
            val arcStartRad = Math.toRadians(-startAngle.d).f
            val x = cos(arcStartRad) * radius
            val y = -sin(arcStartRad) * radius
            drawPoint(center.x + x, center.y + y, trackPaint)
        }
    }
}


// Labels
private val textRect = Rect()
private val textPaint = TextPaint().apply {
    isAntiAlias = true
    textAlign = Paint.Align.CENTER
}

internal fun Canvas.drawLabel(center: PointF,
                              @Dimension radius: Float,
                              angle: Float,
                              text: String,
                              @ColorInt color: Int,
                              @Dimension size: Float) {
    if (Color.alpha(color) > 0 && size > 0f) {
        textPaint.color = color
        textPaint.textSize = size
        val angleRad = Math.toRadians(angle.d).f
        val x = cos(angleRad) * radius
        val y = -sin(angleRad) * radius
        textPaint.getTextBounds(text, 0, text.length, textRect)
        drawText(text, center.x + x, center.y + y - textRect.exactCenterY(), textPaint)
    }
}

val RectF.center get() = PointF(centerX(), centerY())
