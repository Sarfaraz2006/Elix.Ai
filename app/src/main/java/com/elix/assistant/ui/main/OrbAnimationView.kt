package com.elix.assistant.ui.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.elix.assistant.R
import kotlin.math.sin

class OrbAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private enum class Mode { IDLE, LISTENING, SPEAKING }

    private var mode: Mode = Mode.IDLE
    private var amplitude: Float = 0f
    private var t: Float = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = context.getColor(R.color.red_accent)
        alpha = 32
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radiusBase = (width.coerceAtMost(height) / 2f) * 0.85f
        val pulse =
            when (mode) {
                Mode.IDLE -> 0f
                Mode.LISTENING -> 0.03f * sin(t).toFloat()
                Mode.SPEAKING -> 0.05f * sin(t * 1.5f).toFloat()
            }
        val ampScale = (amplitude.coerceIn(0f, 1f) * 0.08f)
        val radius = radiusBase * (1f + pulse + ampScale)

        paint.alpha =
            when (mode) {
                Mode.IDLE -> 18
                Mode.LISTENING -> 42
                Mode.SPEAKING -> 70
            }

        canvas.drawCircle(width / 2f, height / 2f, radius, paint)
        if (mode != Mode.IDLE) {
            t += 0.12f
            postInvalidateOnAnimation()
        }
    }

    fun startListening() {
        mode = Mode.LISTENING
        invalidate()
    }

    fun startSpeaking() {
        mode = Mode.SPEAKING
        invalidate()
    }

    fun setIdle() {
        mode = Mode.IDLE
        invalidate()
    }

    fun setAmplitude(rms: Float) {
        amplitude = rms
        if (mode != Mode.IDLE) postInvalidateOnAnimation() else invalidate()
    }
}
