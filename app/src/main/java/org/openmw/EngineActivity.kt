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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.libsdl.app.SDLActivity
import org.openmw.ui.controls.CustomCursorView
import org.openmw.ui.overlay.GameControllerButtons
import org.openmw.ui.overlay.OverlayUI
import org.openmw.ui.overlay.Thumbstick

class EngineActivity : SDLActivity() {
    private var customCursorView: CustomCursorView? = null
    private lateinit var sdlView: View
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.engine_activity)
        sdlView = getContentView()
        customCursorView = findViewById<CustomCursorView>(R.id.customCursorView).apply {
            sdlView = this@EngineActivity.sdlView // Set SDL view reference
        }

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
        Log.d("EngineActivity", "parentDir: ${filesDir.parent}")
        Log.d("EngineActivity", "USER_FILE_STORAGE: ${Constants.USER_FILE_STORAGE}")
        getPathToJni(filesDir.parent!!, Constants.USER_FILE_STORAGE)

        // Setup Compose overlay for thumbstick
        val composeView = findViewById<ComposeView>(R.id.compose_overlay)
        composeView.setContent {
            Thumbstick(
                onWClick = { onNativeKeyDown(KeyEvent.KEYCODE_W) },
                onAClick = { onNativeKeyDown(KeyEvent.KEYCODE_A) },
                onSClick = { onNativeKeyDown(KeyEvent.KEYCODE_S) },
                onDClick = { onNativeKeyDown(KeyEvent.KEYCODE_D) },
                onRelease = {
                    onNativeKeyUp(KeyEvent.KEYCODE_W)
                    onNativeKeyUp(KeyEvent.KEYCODE_A)
                    onNativeKeyUp(KeyEvent.KEYCODE_S)
                    onNativeKeyUp(KeyEvent.KEYCODE_D)
                    onNativeKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT) // Ensure SHIFT is also released
                },
                onShiftWClick = { onNativeKeyDown(KeyEvent.KEYCODE_W); onNativeKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT) },
                onShiftAClick = { onNativeKeyDown(KeyEvent.KEYCODE_A); onNativeKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT) },
                onShiftSClick = { onNativeKeyDown(KeyEvent.KEYCODE_S); onNativeKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT) },
                onShiftDClick = { onNativeKeyDown(KeyEvent.KEYCODE_D); onNativeKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT) }
            )
        }

        // Setup Compose overlay for buttons
        val composeViewMenu = findViewById<ComposeView>(R.id.compose_overlayMenu)
        composeViewMenu.setContent {
            OverlayUI()
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
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Mouse"
                    )
                }

                // Game Controller Buttons at the Bottom
                GameControllerButtons()
            }
        }
    }

    private var isCustomCursorEnabled = false
    fun toggleCustomCursor() {
        runOnUiThread {
            isCustomCursorEnabled = !isCustomCursorEnabled
            customCursorView?.visibility = if (isCustomCursorEnabled) View.VISIBLE else View.GONE
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
