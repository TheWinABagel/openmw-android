package org.openmw

import android.annotation.SuppressLint
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.libsdl.app.SDLActivity
import org.openmw.ui.controls.ButtonState
import org.openmw.ui.controls.CustomCursorView
import org.openmw.ui.controls.ResizableDraggableButton
import org.openmw.ui.controls.ResizableDraggableThumbstick
import org.openmw.ui.controls.UIStateManager
import org.openmw.ui.controls.loadButtonState
import org.openmw.ui.controls.saveButtonState
import org.openmw.ui.overlay.OverlayUI
import org.openmw.utils.enableLogcat

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
        val allButtons = loadButtonState(this)
        val thumbstick = allButtons.find { it.id == 99 }
        createdButtons.addAll(allButtons.filter { it.id != 99 })

        // Add SDL view programmatically
        val sdlContainer = findViewById<FrameLayout>(R.id.sdl_container)
        sdlContainer.addView(sdlView)

        // Remove sdlView from its parent if necessary
        (sdlView.parent as? ViewGroup)?.removeView(sdlView)
        sdlContainer.addView(sdlView) // Add SDL view to the sdl_container
        (customCursorView.parent as? ViewGroup)?.removeView(customCursorView)
        sdlContainer.addView(customCursorView)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Hide the system bars
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        getPathToJni(filesDir.parent!!, Constants.USER_FILE_STORAGE)

        if (UIStateManager.isLogcatEnabled) {
            enableLogcat()
        }

        // Setup Compose overlay for buttons
        val composeViewUI = findViewById<ComposeView>(R.id.compose_overlayUI)
        (composeViewUI.parent as? ViewGroup)?.removeView(composeViewUI)
        sdlContainer.addView(composeViewUI)
        composeViewUI.setContent {
            OverlayUI(
                engineActivityContext = this,
                editMode = editMode,
                createdButtons = createdButtons,
                customCursorView = customCursorView
            )
            createdButtons.forEach { button ->
                ResizableDraggableButton(
                    context = this,
                    id = button.id,
                    keyCode = button.keyCode,
                    editMode = editMode.value,
                    onDelete = { deleteButton(button.id) },
                    customCursorView = customCursorView
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
            //HiddenMenu() // RadialMenu.kt
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
