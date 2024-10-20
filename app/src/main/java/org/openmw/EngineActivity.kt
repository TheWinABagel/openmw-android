package org.openmw


import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import org.openmw.ui.controls.CustomCursorView
import org.openmw.ui.overlay.GameControllerButtons
import org.openmw.ui.overlay.HiddenMenu
import org.openmw.ui.overlay.OverlayUI
import org.openmw.ui.overlay.Thumbstick
import org.openmw.ui.overlay.sendKeyEvent
import org.openmw.utils.LogCat

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

    override fun getMainSharedObject(): String {
        return OPENMW_MAIN_LIB
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
                }
            )
        }

        // Setup Compose overlay for buttons
        val composeViewMenu = findViewById<ComposeView>(R.id.compose_overlayMenu)
        composeViewMenu.setContent {
            OverlayUI()
            HiddenMenu()
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
                Button(
                    onClick = {
                        onNativeKeyDown(KeyEvent.KEYCODE_F)
                        sendKeyEvent(KeyEvent.KEYCODE_F)
                        onNativeKeyUp(KeyEvent.KEYCODE_F)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.size(50.dp).border(3.dp, Color.Black, shape = CircleShape)
                ) {
                    Text(
                        text = "W",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Button to perform mouse click
                Button(
                    onClick = {
                        customCursorView!!.performMouseClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.size(50.dp).border(3.dp, Color.Black, shape = CircleShape)
                ) {
                    Text(
                        text = "C",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
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
