package org.openmw.ui.controls

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView

fun updateNavigationButtonsVisibility(buttons: List<Button>, visible: Boolean) {
    val visibility = if (visible) View.VISIBLE else View.GONE
    buttons.forEach { it.visibility = visibility }
}

fun addZoomAndMoveButtons(context: Context, sdlView: View, sdlContainer: FrameLayout) {
    // Add navigation buttons
    val moveUpButton = Button(context).apply {
        text = "Up"
        setOnClickListener {
            sdlView.translationY -= 300
        }
    }
    val moveDownButton = Button(context).apply {
        text = "Down"
        setOnClickListener {
            sdlView.translationY += 300
        }
    }
    val moveLeftButton = Button(context).apply {
        text = "Left"
        setOnClickListener {
            sdlView.translationX -= 300
        }
    }
    val moveRightButton = Button(context).apply {
        text = "Right"
        setOnClickListener {
            sdlView.translationX += 300
        }
    }

    // Add the move buttons to the layout
    sdlContainer.addView(moveUpButton, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        bottomMargin = 150
    })
    sdlContainer.addView(moveDownButton, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        bottomMargin = 50
    })
    sdlContainer.addView(moveLeftButton, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.START
        marginStart = 50
        bottomMargin = 100
    })
    sdlContainer.addView(moveRightButton, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.END
        bottomMargin = 100
    })

    // Initially set navigation buttons to GONE
    updateNavigationButtonsVisibility(listOf(moveUpButton, moveDownButton, moveLeftButton, moveRightButton), false)

    // Add a horizontal slider for zoom control at the top center
    val zoomSlider = SeekBar(context).apply {
        max = 200 // Set maximum zoom scale factor (2.0x)
        progress = 100 // Set initial zoom to 1.0x
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, // Make it span the entire width
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            marginStart = 16
            marginEnd = 16
            topMargin = 50 // Adjust margin as needed
        }
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val scaleFactor = 0.9f + (progress / 100f) * 1.9f // Ensure minimum scale factor is 0.9f
                sdlView.scaleX = scaleFactor
                sdlView.scaleY = scaleFactor
                updateNavigationButtonsVisibility(
                    listOf(moveUpButton, moveDownButton, moveLeftButton, moveRightButton),
                    scaleFactor > 1.0f // Show navigation buttons only if zoomed in
                )
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    // Add the zoom slider to the layout
    sdlContainer.addView(zoomSlider)

    // Add a simple button to zoom out the sdlView and reset its position
    val zoomOutButton = Button(context).apply {
        text = "Reset"
        setOnClickListener {
            sdlView.scaleX = 1.0f
            sdlView.scaleY = 1.0f
            sdlView.translationX = 0f // Reset position
            sdlView.translationY = 0f // Reset position
            updateNavigationButtonsVisibility(listOf(moveUpButton, moveDownButton, moveLeftButton, moveRightButton), false)
            zoomSlider.progress = 100 // Reset slider to reflect 1.0f zoom
        }
    }

    // Add the zoom out button to the layout
    sdlContainer.addView(zoomOutButton, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        gravity = Gravity.TOP or Gravity.END
        marginEnd = 16
        topMargin = 100 // Adjusted to avoid overlap with zoom in button
    })

    // Create an easter egg view
    val easterEggView = FrameLayout(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        visibility = View.GONE // Initially hide the easter egg view

        // Add a background color
        setBackgroundColor(Color.GRAY)

        // Add a TextView
        val textView = TextView(context).apply {
            text = "Easter Egg Found!"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        addView(textView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        })

        /*
        // Add an ImageView
    val imageView = ImageView(context).apply {
        setImageResource(R.drawable.your_image) // Make sure to have an image resource in your drawable folder
        scaleType = ImageView.ScaleType.CENTER_INSIDE
    }
    addView(imageView, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
        topMargin = 200
    })
         */
    }

    // Add the easter egg view to the layout
    sdlContainer.addView(easterEggView)

    // Add a horizontal slider for flip control at the bottom center
    val flipSlider = SeekBar(context).apply {
        max = 180 // Set maximum rotation angle to 180 degrees
        progress = 0 // Set initial rotation to 0 degrees
        alpha = 0f // Make it invisible
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, // Make it span the entire width
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            marginStart = 16
            marginEnd = 16
            bottomMargin = 50 // Adjust margin as needed
        }
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val rotationAngle = progress.toFloat() // Set rotation angle based on slider progress
                val scaleFactor = 1.0f - (progress / 180f * 2.0f) // Adjust scale factor to drop from 1.0f to -1.0f
                sdlView.rotationY = rotationAngle
                sdlView.scaleX = scaleFactor
                sdlView.scaleY = scaleFactor
                // Show easter egg view when fully flipped
                if (rotationAngle == 180f) {
                    sdlView.visibility = View.GONE
                    easterEggView.visibility = View.VISIBLE
                } else {
                    sdlView.visibility = View.VISIBLE
                    easterEggView.visibility = View.GONE
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    // Add the flip slider to the layout
    sdlContainer.addView(flipSlider)
}
