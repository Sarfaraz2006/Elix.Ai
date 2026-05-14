package com.elix.assistant.ui.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.elix.assistant.R
import kotlin.math.sin

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2f
        color = context.getColor(R.color.purple_accent)
        alpha = 160
    }

    private var amplitude01: Float = 0f
    private var isAnimating: Boolean = false
    private var phase: Float = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val midY = height / 2f
        val amplitude = (height / 4f) * (0.25f + amplitude01.coerceIn(0f, 1f) * 1.25f)
        val step = (resources.displayMetrics.density * 6f).coerceAtLeast(4f)

        var x = 0f
        var prevX = 0f
        var prevY = midY
        while (x <= width.toFloat()) {
            val t = x / width.toFloat()
            val y = midY + amplitude * sin((t * 6f + phase) * Math.PI).toFloat()
            if (x > 0f) canvas.drawLine(prevX, prevY, x, y, paint)
            prevX = x
            prevY = y
            x += step
        }

        if (isAnimating) {
            phase += 0.06f
            postInvalidateOnAnimation()
        }
    }

    fun startAnimation() {
        if (isAnimating) return
        isAnimating = true
        postInvalidateOnAnimation()
    }

    fun stopAnimation() {
        isAnimating = false
        invalidate()
    }

    fun setAmplitude(rms: Float) {
        amplitude01 = rms
        if (isAnimating) postInvalidateOnAnimation() else invalidate()
    }
}
