package com.ivarna.deviceinsight.service.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

class OverlayGraphView(context: Context, val label: String, val color: Int) : View(context) {
    private val density = context.resources.displayMetrics.density
    var scaleFactor: Float = 1.0f
        set(value) {
            field = value
            requestLayout() // Height depends on scaleFactor
            invalidate()
        }
    
    private val linePaint = Paint().apply {
        this.color = this@OverlayGraphView.color
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = true
        setShadowLayer(1f, 1f, 1f, android.graphics.Color.BLACK)
    }

    private val borderPaint = Paint().apply {
        color = 0x80FFFFFF.toInt() // Semi-transparent white
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    private var points: List<Float> = emptyList()

    fun setData(newPoints: List<Float>) {
        points = newPoints
        invalidate() // Redraw
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (60 * scaleFactor).toInt()
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, desiredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Update paints based on current scaleFactor
        linePaint.strokeWidth = 2f * density * scaleFactor
        textPaint.textSize = 14f * density * scaleFactor
        borderPaint.strokeWidth = 1f * density * scaleFactor

        // Draw a semi-transparent background for the graph area
        canvas.drawColor(0x20000000) 
        
        // Draw border
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)
        
        // Draw label with padding
        val padding = 4f * density * scaleFactor
        val textY = textPaint.textSize + padding/2
        canvas.drawText(label, padding, textY, textPaint)
        
        if (points.isEmpty()) return
        
        // We want to show last 60 points or so
        val maxPoints = 60
        val displayPoints = if (points.size > maxPoints) points.takeLast(maxPoints) else points
        
        if (displayPoints.size < 2) return

        val path = Path()
        val w = width.toFloat()
        val h = height.toFloat()
        // Step X to fill the width
        val stepX = w / (points.size - 1).coerceAtLeast(1)
        
        // Find max value for scaling Y, default to 100 for percentage
        val maxY = if (label.contains("%")) 100f else (points.maxOrNull() ?: 10f).coerceAtLeast(0.1f) * 1.2f

        displayPoints.forEachIndexed { index, value ->
            val x = index * stepX
            
            // Add padding to Y to avoid drawing on edges (margin)
            val graphHeight = h * 0.9f 
            val yOffset = h * 0.05f 
            
            // Map value 0..maxY to graphHeight..0
            val ratio = (value / maxY).coerceIn(0f, 1f)
            val y = (h - yOffset) - (ratio * graphHeight)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, linePaint)
    }
}
