package org.openmw

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.libsdl.app.SDLActivity
import org.openmw.ui.controls.ButtonState
import org.openmw.ui.controls.CustomCursorView
import org.openmw.ui.controls.ResizableDraggableButton
import org.openmw.ui.controls.ResizableDraggableThumbstick
import org.openmw.ui.controls.UIStateManager
import org.openmw.ui.controls.UIStateManager.isThumbDragging
import org.openmw.ui.controls.loadButtonState
import org.openmw.ui.controls.saveButtonState
import org.openmw.ui.overlay.OverlayUI
import org.openmw.utils.enableLogcat
import java.io.File
import kotlin.math.roundToInt

class EngineActivity : SDLActivity() {
    private lateinit var customCursorView: CustomCursorView
    private lateinit var sdlView: View
    private val createdButtons = mutableStateListOf<ButtonState>()
    private var editMode = mutableStateOf(false)

    init {
        setEnvironmentVariables()
    }
    override fun getLibraries(): Array<String> {
        return try {
            Log.d("EngineActivity", "Loading libraries: ${jniLibsArray.joinToString(", ")}")
            jniLibsArray
        } catch (e: Exception) {
            Log.e("EngineActivity", "Error loading libraries", e)
            emptyArray()
        }
    }

    override fun getMainSharedObject(): String {
        return OPENMW_MAIN_LIB
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.engine_activity)
        sdlView = getContentView()
        customCursorView = findViewById(R.id.customCursorView)

        // Ensure the correct initial state of the cursor
        setupInitialCursorState()

        // Load UI saved buttons, 99 is the Thumbstick. Without these 3 lines the button loader will read 99
        // from the UI.cfg file and create a duplicate as a button
        val allButtons = loadButtonState(this@EngineActivity)
        val thumbstick = allButtons.find { it.id == 99 }
        createdButtons.addAll(allButtons.filter { it.id != 99 })

        // Add SDL view programmatically
        val sdlContainer = findViewById<FrameLayout>(R.id.sdl_container)
        sdlContainer.addView(sdlView, 0)
        sdlView.setOnTouchListener { view, event ->
            onTouchEvent(event)
        }

        // Remove sdlView from its parent if necessary
        (sdlView.parent as? ViewGroup)?.removeView(sdlView)
        sdlContainer.addView(sdlView) // Add SDL view to the sdl_container
        (customCursorView.parent as? ViewGroup)?.removeView(customCursorView)
        sdlContainer.addView(customCursorView)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Hide the system bars
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        getPathToJni(filesDir.parent!!, Constants.USER_FILE_STORAGE)

        if (UIStateManager.isLogcatEnabled) {
            enableLogcat()
        }

        // Setup Compose overlay for buttons
        val composeViewUI = findViewById<ComposeView>(R.id.compose_overlayUI)
        (composeViewUI.parent as? ViewGroup)?.removeView(composeViewUI)
        sdlContainer.addView(composeViewUI, 2)
        composeViewUI.setContent {
            OverlayUI(
                context = this,
                editMode = editMode,
                createdButtons = createdButtons,
                sdlView = sdlView,
                sdlContainer = sdlContainer,
                customCursorView = customCursorView,
                addButtonView = ::addButtonView
            )
        }

        createdButtons.forEach { button ->
            val buttonState = UIStateManager.buttonStates.getOrPut(button.id) {
                mutableStateOf(
                    ButtonState(
                        button.id,
                        button.size,
                        button.offsetX,
                        button.offsetY,
                        button.isLocked,
                        button.keyCode
                    )
                )
            }.value

            val buttonView = ComposeView(this).apply {
                id = button.id // Ensure unique ID
                var layoutParams = FrameLayout.LayoutParams(
                    dpToPx(buttonState.size.dp + 30.dp, this@EngineActivity),
                    dpToPx(buttonState.size.dp + 30.dp, this@EngineActivity)
                ).apply {
                    leftMargin = buttonState.offsetX.roundToInt()
                    topMargin = buttonState.offsetY.roundToInt()
                }
                this.layoutParams = layoutParams

                setContent {
                    val isDragging = remember { mutableStateOf(false) }
                    val offsetX = remember { mutableFloatStateOf(buttonState.offsetX) }
                    val offsetY = remember { mutableFloatStateOf(buttonState.offsetY) }
                    val buttonSize = remember { mutableStateOf(buttonState.size.dp) }

                    val saveState = {
                        val updatedState = ButtonState(
                            button.id,
                            buttonSize.value.value,
                            offsetX.floatValue,
                            offsetY.floatValue,
                            buttonState.isLocked,
                            button.keyCode
                        )
                        UIStateManager.buttonStates[button.id]?.value = updatedState
                        saveButtonState(
                            context,
                            UIStateManager.buttonStates.values.map { it.value })
                    }

                    Box(
                        contentAlignment = Alignment.TopStart,
                        modifier = Modifier
                            .size(buttonSize.value + 20.dp)
                            .background(Color.Transparent)
                            //.border(2.dp, Color.Black)
                            .then(
                                if (editMode.value) {
                                    Modifier.pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { isDragging.value = true },
                                            onDrag = { change, dragAmount ->
                                                offsetX.value += dragAmount.x
                                                offsetY.value += dragAmount.y
                                                // Update layout parameters dynamically
                                                layoutParams.leftMargin = offsetX.floatValue.roundToInt()
                                                layoutParams.topMargin = offsetY.floatValue.roundToInt()
                                                this@apply.layoutParams = layoutParams
                                            },
                                            onDragEnd = {
                                                isDragging.value = false
                                                saveState()
                                            },
                                            onDragCancel = {
                                                isDragging.value = false
                                            }
                                        )
                                    }
                                } else Modifier
                            )
                    ) {
                        ResizableDraggableButton(
                            context = this@EngineActivity,
                            id = button.id,
                            keyCode = button.keyCode,
                            editMode = editMode.value,
                            isDragging = isDragging.value,
                            onDelete = { deleteButton(button.id) },
                            customCursorView = customCursorView
                        )
                    }
                }
            }
            sdlContainer.addView(buttonView)
        }

        thumbstick?.let {
            val buttonState = UIStateManager.buttonStates.getOrPut(it.id) {
                mutableStateOf(
                    ButtonState(
                        it.id,
                        it.size,
                        it.offsetX,
                        it.offsetY,
                        it.isLocked,
                        it.keyCode
                    )
                )
            }.value

            val thumbstickView = ComposeView(this).apply {
                id = View.generateViewId() // Ensure unique ID
                var layoutParams = FrameLayout.LayoutParams(
                    dpToPx(buttonState.size.dp + 30.dp, this@EngineActivity),
                    dpToPx(buttonState.size.dp + 30.dp, this@EngineActivity)
                    ).apply {
                    leftMargin = buttonState.offsetX.roundToInt()
                    topMargin = buttonState.offsetY.roundToInt()
                }
                this.layoutParams = layoutParams

                setContent {
                    val offsetX = remember { mutableFloatStateOf(buttonState.offsetX) }
                    val offsetY = remember { mutableFloatStateOf(buttonState.offsetY) }
                    val buttonSize = remember { mutableStateOf(buttonState.size.dp) }

                    val saveState = {
                        val updatedState = buttonState.copy(
                            size = buttonSize.value.value,
                            offsetX = offsetX.floatValue,
                            offsetY = offsetY.floatValue
                        )
                        UIStateManager.buttonStates[buttonState.id]?.value = updatedState
                        saveButtonState(
                            context,
                            UIStateManager.buttonStates.values.map { it.value })
                    }

                    Box(
                        contentAlignment = Alignment.TopStart,
                        modifier = Modifier
                            .size(buttonSize.value + 30.dp)
                            .background(Color.Transparent)
                            //.border(2.dp, Color.Black)
                            .then(
                                if (editMode.value) {
                                    Modifier.pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { isThumbDragging = true },
                                            onDrag = { change, dragAmount ->
                                                offsetX.value += dragAmount.x
                                                offsetY.value += dragAmount.y
                                                // Update layout parameters dynamically
                                                layoutParams.leftMargin = offsetX.floatValue.roundToInt()
                                                layoutParams.topMargin = offsetY.floatValue.roundToInt()
                                                this@apply.layoutParams = layoutParams
                                            },
                                            onDragEnd = {
                                                isThumbDragging = false
                                                saveState()
                                            },
                                            onDragCancel = {
                                                isThumbDragging = false
                                            }
                                        )
                                    }
                                } else Modifier
                            )
                    ) {
                        ResizableDraggableThumbstick(
                            context = this@EngineActivity,
                            id = buttonState.id,
                            keyCode = buttonState.keyCode,
                            editMode = editMode.value,
                            onWClick = { onNativeKeyDown(KeyEvent.KEYCODE_W) },
                            onAClick = { onNativeKeyDown(KeyEvent.KEYCODE_A) },
                            onSClick = { onNativeKeyDown(KeyEvent.KEYCODE_S) },
                            onDClick = { onNativeKeyDown(KeyEvent.KEYCODE_D) },
                            onRelease = {
                                onNativeKeyUp(KeyEvent.KEYCODE_W)
                                onNativeKeyUp(KeyEvent.KEYCODE_A)
                                onNativeKeyUp(KeyEvent.KEYCODE_S)
                                onNativeKeyUp(KeyEvent.KEYCODE_D)
                            }
                        )
                    }
                }
            }
            sdlContainer.addView(thumbstickView)
        }
    }

    fun dpToPx(dp: Dp, context: Context): Int {
        return (dp.value * context.resources.displayMetrics.density).toInt()
    }

    private fun setupInitialCursorState() {
        if (UIStateManager.isCustomCursorEnabled) {
            customCursorView.visibility = View.VISIBLE
        } else {
            customCursorView.visibility = View.GONE
        }
    }

    private fun deleteButton(buttonId: Int) {
        // Remove button from createdButtons
        createdButtons.removeIf { it.id == buttonId }

        // Remove button's view from sdlContainer
        val sdlContainer = findViewById<FrameLayout>(R.id.sdl_container)
        val buttonView = sdlContainer.findViewById<View>(buttonId)
        if (buttonView != null) {
            sdlContainer.removeView(buttonView)
        }

        // Inline saveButtonState logic
        val file = File("${Constants.USER_CONFIG}/UI.cfg")
        if (!file.exists()) {
            file.createNewFile()
        }
        // Ensure thumbstick is not removed
        val thumbstick = loadButtonState(this).find { it.id == 99 }
        val existingStates = createdButtons.filter { it.id != 99 }.toMutableList()
        thumbstick?.let { existingStates.add(it) }
        file.printWriter().use { out ->
            existingStates.forEach { button ->
                out.println("ButtonID_${button.id}(${button.size};${button.offsetX};${button.offsetY};${button.isLocked};${button.keyCode})")
            }
        }
    }

    fun addButtonView(button: ButtonState, sdlContainer: FrameLayout, context: Context) {
        val buttonView = ComposeView(context).apply {
            id = button.id // Ensure unique ID

            val layoutParams = FrameLayout.LayoutParams(
                dpToPx(button.size.dp + 20.dp, context),
                dpToPx(button.size.dp + 20.dp, context)
            ).apply {
                leftMargin = button.offsetX.roundToInt()
                topMargin = button.offsetY.roundToInt()
            }
            this.layoutParams = layoutParams

            setContent {
                val isDragging = remember { mutableStateOf(false) }
                val offsetX = remember { mutableFloatStateOf(button.offsetX) }
                val offsetY = remember { mutableFloatStateOf(button.offsetY) }
                val buttonSize = remember { mutableStateOf(button.size.dp) }

                val saveState = {
                    val updatedState = ButtonState(
                        button.id, buttonSize.value.value, offsetX.floatValue, offsetY.floatValue, button.isLocked, button.keyCode
                    )
                    UIStateManager.buttonStates[button.id]?.value = updatedState
                    saveButtonState(context, UIStateManager.buttonStates.values.map { it.value })
                }

                Box(
                    contentAlignment = Alignment.TopStart,
                    modifier = Modifier
                        .size(buttonSize.value + 20.dp)
                        .background(Color.Transparent)
                        //.border(2.dp, Color.Black)
                        .then(
                            if (editMode.value) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { isDragging.value = true },
                                        onDrag = { change, dragAmount ->
                                            offsetX.value += dragAmount.x
                                            offsetY.value += dragAmount.y
                                            // Update layout parameters dynamically
                                            layoutParams.leftMargin = offsetX.floatValue.roundToInt()
                                            layoutParams.topMargin = offsetY.floatValue.roundToInt()
                                            this@apply.layoutParams = layoutParams
                                        },
                                        onDragEnd = {
                                            isDragging.value = false
                                            saveState()
                                        },
                                        onDragCancel = {
                                            isDragging.value = false
                                        }
                                    )
                                }
                            } else Modifier
                        )
                ) {
                    ResizableDraggableButton(
                        context = context,
                        id = button.id,
                        keyCode = button.keyCode,
                        editMode = editMode.value,
                        isDragging = isDragging.value,
                        onDelete = { deleteButton(button.id) },
                        customCursorView = customCursorView
                    )
                }
            }
        }
        sdlContainer.addView(buttonView)
    }


    private fun setEnvironmentVariables() {
        try {
            Os.setenv("OSG_TEXT_SHADER_TECHNIQUE", "ALL", true)
        } catch (e: ErrnoException) {
            Log.e("OpenMW", "Failed setting environment variables.")
            e.printStackTrace()
        }

        Os.setenv("OSG_VERTEX_BUFFER_HINT", "VBO", true)
        Os.setenv("OPENMW_USER_FILE_STORAGE", Constants.USER_FILE_STORAGE + "/", true)

        try {
            Os.setenv("OPENMW_GLES_VERSION", "2", true)
            Os.setenv("LIBGL_ES", "2", true)
        } catch (e: ErrnoException) {
            Log.e("OpenMW", "Failed setting environment variables.")
            e.printStackTrace()
        }
        Log.d("EngineActivity", "Environment variables set")
    }
    private external fun getPathToJni(path_global: String, path_user: String)

    public override fun onDestroy() {
        finish()
        Process.killProcess(Process.myPid())
        super.onDestroy()
    }
}