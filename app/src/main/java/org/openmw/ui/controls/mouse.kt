package org.openmw.ui.controls

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import org.openmw.Constants
import org.openmw.R
import java.io.File

class CustomCursorView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var cursorX = 0f
    private var cursorY = 0f
    private var offsetX = 0f
    private var offsetY = 0f
    var sdlView: View? = null
    private var isCursorEnabled = true

    private val cursorIcon = ContextCompat.getDrawable(context, R.drawable.pointer_icon)!!

    private fun readSettingsFile(): Triple<Int, Int, Float> {
        val settingsFile = File(Constants.SETTINGS_FILE)
        var resolutionX = 0
        var resolutionY = 0
        var scalingFactor = 1.0f

        settingsFile.forEachLine { line ->
            when {
                line.startsWith("resolution x =") -> resolutionX = line.split("=").last().trim().toInt()
                line.startsWith("resolution y =") -> resolutionY = line.split("=").last().trim().toInt()
                line.startsWith("scaling factor =") -> scalingFactor = line.split("=").last().trim().toFloat()
            }
        }

        return Triple(resolutionX, resolutionY, scalingFactor)
    }

    private val settings = readSettingsFile()
    private val resolutionX = settings.first
    private val resolutionY = settings.second

    fun setCursorPosition(x: Float, y: Float) {
        cursorX = x.coerceIn(0f, width.toFloat() - cursorIcon.intrinsicWidth)
        cursorY = y.coerceIn(0f, height.toFloat() - cursorIcon.intrinsicHeight)
        Log.d("CustomCursorView", "Cursor Position: X=$cursorX, Y=$cursorY")
        invalidate()
    }

    fun performMouseClick() {
        val adjustedX = cursorX * (resolutionX.toFloat() / width.toFloat())
        val adjustedY = cursorY * (resolutionY.toFloat() / height.toFloat())
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
            val iconSize = 72
            cursorIcon.setBounds(cursorX.toInt(), cursorY.toInt(), cursorX.toInt() + iconSize, cursorY.toInt() + iconSize)
            cursorIcon.draw(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                offsetX = event.x - cursorX
                offsetY = event.y - cursorY
                Log.d("CustomCursorView", "Touch Down at X: ${event.x}, Y: ${event.y}")
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                setCursorPosition(event.x - offsetX, event.y - offsetY)
                Log.d("CustomCursorView", "Touch Move at X: ${event.x}, Y: ${event.y}")
                return true
            }
            MotionEvent.ACTION_UP -> {
                offsetX = event.x - cursorX
                offsetY = event.y - cursorY
                Log.d("CustomCursorView", "Touch Released at X: ${event.x}, Y: ${event.y}")
                return true
            }
        }
        return false
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}
