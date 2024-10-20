package org.openmw.ui.controls

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

class CustomCursorView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
    private var cursorX = 0f
    private var cursorY = 0f
    private var offset = -150f
    var sdlView: View? = null
    private var isCursorEnabled = true

    fun setCursorPosition(x: Float, y: Float) {
        cursorX = x + offset
        cursorY = y + offset
        invalidate()
    }

    fun performMouseClick() {
        val adjustedX = cursorX
        val adjustedY = cursorY
        val eventDown = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            adjustedX,
            adjustedY,
            0
        )
        val eventUp = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_UP,
            adjustedX,
            adjustedY,
            0
        )
        Log.d("CustomCursorView", "Click at X: $adjustedX, Y: $adjustedY")
        sdlView?.dispatchTouchEvent(eventDown)
        sdlView?.dispatchTouchEvent(eventUp)
        eventDown.recycle()
        eventUp.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isCursorEnabled) {
            canvas.drawCircle(cursorX, cursorY, 20f, paint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                setCursorPosition(event.x, event.y)
                return true
            }
        }
        return false
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}
