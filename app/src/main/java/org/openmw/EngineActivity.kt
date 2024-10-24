package org.openmw

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Process
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.libsdl.app.SDLActivity
import org.openmw.ui.controls.ButtonState
import org.openmw.ui.controls.CustomCursorView
import org.openmw.ui.controls.DynamicButtonManager
import org.openmw.ui.controls.ResizableDraggableButton
import org.openmw.ui.controls.ResizableDraggableThumbstick
import org.openmw.ui.controls.UIStateManager
import org.openmw.ui.controls.loadButtonState
import org.openmw.ui.controls.saveButtonState
import org.openmw.ui.overlay.OverlayUI

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.engine_activity)
        sdlView = getContentView()
        customCursorView = findViewById(R.id.customCursorView)

        customCursorView.apply {
            sdlView = this@EngineActivity.sdlView
        }

        // Ensure the correct initial state of the cursor
        setupInitialCursorState()

        // Load saved buttons
        val allButtons = loadButtonState(this)
        val thumbstick = allButtons.find { it.id == 99 }
        createdButtons.addAll(allButtons.filter { it.id != 99 })

        // Add SDL view programmatically
        val sdlContainer = findViewById<FrameLayout>(R.id.sdl_container)
        sdlContainer.addView(sdlView) // Add SDL view to the sdl_container

        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Hide the system bars
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Create an instance of LogCat and call enableLogcat
        //val logCat = LogCat(this)
        //logCat.enableLogcat()
        getPathToJni(filesDir.parent!!, Constants.USER_FILE_STORAGE)

        // Setup Compose overlay for buttons
        val composeViewMenu = findViewById<ComposeView>(R.id.compose_overlayMenu)
        composeViewMenu.setContent {
            OverlayUI(engineActivityContext = this)

            createdButtons.forEach { button ->
                ResizableDraggableButton(
                    context = this@EngineActivity,
                    id = button.id,
                    keyCode = button.keyCode,
                    editMode = editMode.value,
                    onDelete = { deleteButton(button.id) }
                )
            }

            thumbstick?.let {
                ResizableDraggableThumbstick(
                    context = this,
                    id = 99,
                    keyCode = it.keyCode,
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

            DynamicButtonManager(
                context = this@EngineActivity,
                onNewButtonAdded = { newButtonState ->
                    createdButtons.add(newButtonState)
                },
                editMode = editMode,
                createdButtons = createdButtons // Pass created buttons to DynamicButtonManager
            )

            //HiddenMenu() // RadialMenu.kt
        }

        // Setup Compose overlay for buttons
        val composeViewButtons = findViewById<ComposeView>(R.id.compose_overlayButtons)
        composeViewButtons.setContent {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxHeight()
            ) {
                // Toggle Custom Cursor Visibility Button
                Button(
                    onClick = { toggleCustomCursor() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.End)
                        .border(3.dp, Color.Black, shape = CircleShape) // Add border
                ) {
                    Text(
                        text = "M",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    private fun setupInitialCursorState() {
        if (UIStateManager.isCustomCursorEnabled) {
            customCursorView.visibility = View.VISIBLE
        } else {
            customCursorView.visibility = View.GONE
        }
    }

    private fun deleteButton(buttonId: Int) {
        createdButtons.removeIf { it.id == buttonId }
        saveButtonState(this, createdButtons + loadButtonState(this).filter { it.id == 99 }) // Ensure thumbstick is not removed
    }

    fun toggleCustomCursor() {
        runOnUiThread {
            UIStateManager.isCustomCursorEnabled = !UIStateManager.isCustomCursorEnabled
            customCursorView.visibility = if (UIStateManager.isCustomCursorEnabled) View.VISIBLE else View.GONE
        }
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
