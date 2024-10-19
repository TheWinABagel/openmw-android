package org.openmw.ui.controls

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
    private val paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private var cursorX = 0f
    private var cursorY = 0f
    private var offset = -150f // Offset the cursor from the touch point
    var sdlView: View? = null // Reference to SDL view
    private var isCursorEnabled = true

    fun setCursorPosition(x: Float, y: Float) {
        cursorX = x + offset
        cursorY = y + offset
        //Log.d("CustomCursorView", "Cursor at X: $cursorX, Y: $cursorY")
        invalidate()
    }

    fun performMouseClick() {
        val adjustedX = cursorX // Use the cursor's position for the click
        val adjustedY = cursorY// Use the cursor's position for the click

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
            canvas.drawCircle(cursorX, cursorY, 20f, paint) // Draw a circle as the cursor
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                setCursorPosition(event.x, event.y)
                //Log.d("CustomCursorView", "Motion at X: ${event.x}, Y: ${event.y}")
                return true
            }
            MotionEvent.ACTION_UP -> {
                performMouseClick()
                return true
            }
        }
        return false
    }

    override fun performClick(): Boolean {
        performMouseClick()
        return super.performClick()
    }
}
