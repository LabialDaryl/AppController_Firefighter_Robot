package com.firefighter.robotcontroller

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.*

/**
 * Custom virtual joystick view.
 */
class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cx = 0f
    private var cy = 0f
    private var baseR = 0f
    private var hatR = 0f
    private var maxHatTravel = 0f
    private var hatX = 0f
    private var hatY = 0f

    private var joystickListener: JoystickListener? = null

    private val baseFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val baseRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 6f }
    private val innerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
    private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f) }
    private val hatFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val hatStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f }

    init {
        // Initialize colors safely in init
        baseFillPaint.color   = ContextCompat.getColor(context, R.color.joystick_base)
        baseRingPaint.color   = ContextCompat.getColor(context, R.color.joystick_ring)
        innerRingPaint.color  = Color.argb(60, 255, 111, 0)
        crossPaint.color      = Color.argb(90, 255, 111, 0)
        hatFillPaint.color    = ContextCompat.getColor(context, R.color.joystick_hat)
        hatStrokePaint.color  = ContextCompat.getColor(context, R.color.joystick_hat_stroke)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val d = min(getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
                    getDefaultSize(suggestedMinimumHeight, heightMeasureSpec))
        setMeasuredDimension(d, d)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cx = w / 2f
        cy = h / 2f
        baseR = min(w, h) / 2f * 0.82f
        hatR  = baseR * 0.36f
        maxHatTravel = baseR - hatR
        hatX = cx
        hatY = cy
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(cx, cy, baseR, baseFillPaint)
        canvas.drawCircle(cx, cy, baseR, baseRingPaint)
        canvas.drawCircle(cx, cy, baseR * 0.55f, innerRingPaint)
        
        val cl = baseR * 0.70f
        canvas.drawLine(cx - cl, cy, cx + cl, cy, crossPaint)
        canvas.drawLine(cx, cy - cl, cx, cy + cl, crossPaint)
        
        canvas.drawCircle(hatX, hatY, hatR, hatFillPaint)
        canvas.drawCircle(hatX, hatY, hatR, hatStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - cx
                val dy = event.y - cy
                val dist = sqrt(dx * dx + dy * dy)
                if (dist <= maxHatTravel) {
                    hatX = event.x
                    hatY = event.y
                } else {
                    val angle = atan2(dy, dx)
                    hatX = cx + maxHatTravel * cos(angle)
                    hatY = cy + maxHatTravel * sin(angle)
                }
                notifyListener()
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                hatX = cx
                hatY = cy
                joystickListener?.onJoystickMoved(0f, 0f)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun notifyListener() {
        if (maxHatTravel == 0f) return
        val nx = (hatX - cx) / maxHatTravel
        val ny = (hatY - cy) / maxHatTravel
        joystickListener?.onJoystickMoved(nx, ny)
    }

    fun setJoystickListener(listener: JoystickListener) {
        joystickListener = listener
    }

    interface JoystickListener {
        fun onJoystickMoved(x: Float, y: Float)
    }
}
