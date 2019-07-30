package com.francescozoccheddu.knob

import android.graphics.*
import android.text.TextPaint
import kotlin.math.abs
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
                              radius: Float,
                              startAngle: Float,
                              sweep: Float,
                              color: Int,
                              thickness: Float) {
    if (Color.alpha(color) > 0 && thickness > 0f) {
        trackRect.set(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        trackPaint.strokeWidth = thickness
        trackPaint.color = color
        if (sweep != 0f) {
            drawArc(trackRect, -startAngle, -sweep, false, trackPaint)
        } else {
            val arcStartRad = startAngle.rad
            val x = cos(arcStartRad) * radius
            val y = -sin(arcStartRad) * radius
            drawPoint(center.x + x, center.y + y, trackPaint)
        }
    }
}


// Labels
private val tempTextRect = Rect()
private val tempTextPaint = TextPaint().apply {
    isAntiAlias = true
    textAlign = Paint.Align.CENTER
}

internal fun Canvas.drawThick(center: PointF,
                              radius: Float,
                              angle: Float,
                              text: String,
                              color: Int,
                              size: Float,
                              typeface: Typeface) {

    val angleRad = angle.rad
    val x = cos(angleRad) * radius
    val y = -sin(angleRad) * radius
    drawCenteredText(center.x + x, center.y + y, text, color, size, typeface)
}

internal fun Canvas.drawCenteredText(x: Float,
                                     y: Float,
                                     text: String,
                                     color: Int,
                                     size: Float,
                                     typeface: Typeface) {
    if (Color.alpha(color) > 0 && size > 0f) {
        tempTextPaint.typeface = typeface
        tempTextPaint.color = color
        tempTextPaint.textSize = size
        tempTextPaint.getTextBounds(text, 0, text.length, tempTextRect)
        drawText(text, x, y - tempTextRect.exactCenterY(), tempTextPaint)
    }
}

val RectF.center get() = PointF(centerX(), centerY())

private val Float.rad get() = Math.toRadians(this.d).f

private fun circleBB(x: Float, y: Float, radius: Float) = trackRect.apply {
    set(x - radius, y - radius, x + radius, y + radius)
}


// Clip
private val tempPath = Path()

private fun arcPath(center: PointF,
                    radius: Float,
                    startAngle: Float,
                    sweep: Float,
                    thickness: Float) = tempPath.apply {
    tempPath.reset()
    val w = abs(sweep)
    val or = radius + thickness / 2f
    if (w >= 360f) {
        addCircle(center.x, center.y, or, Path.Direction.CW)
    } else {
        val sa = if (sweep > 0f) startAngle else (startAngle + sweep)
        val ea = sa + abs(sweep)
        val sar = sa.rad
        val ear = ea.rad
        val ir = radius - thickness / 2f
        arcTo(circleBB(center.x, center.y, or), -sa, -w)
        run {
            val cx = center.x + cos(ear) * radius
            val cy = center.y - sin(ear) * radius
            val r = thickness / 2f
            arcTo(circleBB(cx, cy, r), -ea, -180f)
        }
        arcTo(circleBB(center.x, center.y, ir), -ea, w)
        run {
            val cx = center.x + cos(sar) * radius
            val cy = center.y - sin(sar) * radius
            val r = thickness / 2f
            arcTo(circleBB(cx, cy, r), -sa + 180f, -180f)
        }
    }
    close()
}

internal fun Canvas.clipTrack(center: PointF,
                              radius: Float,
                              startAngle: Float,
                              sweep: Float,
                              thickness: Float) =
    clipPath(arcPath(center, radius, startAngle, sweep, thickness))
