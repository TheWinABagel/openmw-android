package org.openmw

import android.content.Context
import android.os.Bundle
import android.os.Process
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.libsdl.app.SDLActivity
import org.openmw.ui.overlay.OverlayUI
import org.openmw.utils.LogCat
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager


class EngineActivity : SDLActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.engine_activity)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Hide the system bars
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Create an instance of LogCat and call enableLogcat
        val logCat = LogCat(this)
        logCat.enableLogcat()

        Log.d("EngineActivity", "parentDir: ${filesDir.parent}")
        Log.d("EngineActivity", "USER_FILE_STORAGE: ${Constants.USER_FILE_STORAGE}")

        getPathToJni(filesDir.parent!!, Constants.USER_FILE_STORAGE)

        // Add SDL view programmatically
        val sdlView = SDLActivity.getContentView()
        val sdlContainer = findViewById<FrameLayout>(R.id.sdl_container)
        sdlContainer.addView(sdlView) // Add SDL view to the sdl_container

        // Setup Compose overlay
        val composeView = findViewById<ComposeView>(R.id.compose_overlay)
        composeView.setContent {
            MaterialTheme {
                OverlayUI()
            }
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            // nothing yet
        }
    }
}
