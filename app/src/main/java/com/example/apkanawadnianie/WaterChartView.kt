package com.example.apkanawadnianie

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class WaterChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: List<StatsFragment.StatItem> = emptyList()
    private var dailyGoal: Int = 2000

    private val barPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.BLACK
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private val blueColor = ContextCompat.getColor(context, R.color.water_blue)
    private val greenColor = android.graphics.Color.GREEN

    fun setData(newData: List<StatsFragment.StatItem>, goal: Int) {
        this.data = newData
        this.dailyGoal = if (goal > 0) goal else 2000
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val padding = 40f
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding
        
        val maxAmount = (data.maxByOrNull { it.amount }?.amount ?: dailyGoal).coerceAtLeast(dailyGoal).toFloat()
        
        // Show only last 7 days or so to avoid clutter
        val displayData = if (data.size > 7) data.takeLast(7) else data
        val barCount = displayData.size
        val barWidth = (chartWidth / barCount) * 0.8f
        val spacing = (chartWidth / barCount) * 0.2f

        displayData.forEachIndexed { index, item ->
            val barHeight = (item.amount.toFloat() / maxAmount) * chartHeight
            val left = padding + index * (barWidth + spacing) + spacing / 2
            val top = padding + chartHeight - barHeight
            val right = left + barWidth
            val bottom = padding + chartHeight

            barPaint.color = if (item.amount >= dailyGoal) greenColor else blueColor
            
            canvas.drawRect(left, top, right, bottom, barPaint)
            
            // Draw date (last 5 chars for MM-DD)
            val dateLabel = if (item.date.length >= 5) item.date.substring(item.date.length - 5) else item.date
            canvas.drawText(dateLabel, left + barWidth / 2, bottom + 30f, textPaint)
        }
    }
}
