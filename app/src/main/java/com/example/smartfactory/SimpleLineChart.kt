package com.example.smartfactory

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.roundToInt

class SimpleLineChart @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Style for the main data line
    private val linePaint = Paint().apply {
        color = Color.parseColor("#38bdf8") // Primary Blue
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    // Style for data points
    private val dotPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Style for grid lines
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#33ffffff") // Semi-transparent white
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    // Style for text labels
    private val textPaint = Paint().apply {
        color = Color.parseColor("#94a3b8") // Muted text color
        textSize = 30f
        isAntiAlias = true
        textAlign = Paint.Align.RIGHT
    }
    
    // Dashed effect for grid
    private val dashEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)

    private var dataPoints = listOf(800f, 1200f, 950f, 1100f)

    fun setData(newData: List<Float>) {
        dataPoints = newData
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dataPoints.isEmpty()) return

        val paddingLeft = 100f
        val paddingBottom = 80f
        val paddingTop = 50f
        val paddingRight = 50f
        
        val width = width.toFloat()
        val height = height.toFloat()
        
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // Calculate Min/Max for Y Axis
        val maxVal = (dataPoints.maxOrNull() ?: 100f) * 1.1f // Add 10% headroom
        val minVal = 0f 

        // --- Draw Grid & Y-Axis Labels ---
        val steps = 5
        for (i in 0..steps) {
            val yRatio = i.toFloat() / steps
            val yPos = height - paddingBottom - (yRatio * chartHeight)
            val value = minVal + (yRatio * (maxVal - minVal))

            // Grid Line
            canvas.drawLine(paddingLeft, yPos, width - paddingRight, yPos, gridPaint)

            // Label
            canvas.drawText(value.roundToInt().toString(), paddingLeft - 20f, yPos + 10f, textPaint)
        }

        // --- Draw X-Axis Labels ---
        val stepX = chartWidth / (dataPoints.size - 1)
        textPaint.textAlign = Paint.Align.CENTER
        
        dataPoints.forEachIndexed { index, _ ->
            val xPos = paddingLeft + (index * stepX)
            // Vertical Grid Line component (optional, let's keep it clean like the user image)
            // canvas.drawLine(xPos, paddingTop, xPos, height - paddingBottom, gridPaint)
            
            // Label
            canvas.drawText("S${index + 1}", xPos, height - 20f, textPaint)
        }

        // --- Draw Data Line ---
        val path = Path()
        dataPoints.forEachIndexed { index, value ->
            val x = paddingLeft + (index * stepX)
            val ratio = (value - minVal) / (maxVal - minVal)
            val y = height - paddingBottom - (ratio * chartHeight)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            // Draw dot
            canvas.drawCircle(x, y, 8f, dotPaint)
        }
        canvas.drawPath(path, linePaint)
        
        // --- Draw Axes Lines ---
        gridPaint.color = Color.WHITE
    // --- Draw Axes Lines ---
        // Y-Axis
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, height - paddingBottom, gridPaint)
        // X-Axis
        canvas.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom, gridPaint)
    }
    
    fun setTheme(isDark: Boolean) {
        if (isDark) {
            // Dark Mode
            linePaint.color = Color.parseColor("#38bdf8")
            dotPaint.color = Color.WHITE
            gridPaint.color = Color.parseColor("#33ffffff")
            textPaint.color = Color.parseColor("#94a3b8")
        } else {
            // Light Mode (for PDF)
            linePaint.color = Color.BLUE
            dotPaint.color = Color.BLACK
            gridPaint.color = Color.LTGRAY
            textPaint.color = Color.BLACK
        }
        invalidate()
    }
}
