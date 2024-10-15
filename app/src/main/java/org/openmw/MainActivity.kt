package org.openmw

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.delay
import org.openmw.ui.theme.OpenMWTheme
import org.openmw.utils.CaptureCrash
import org.openmw.utils.ModValue
import org.openmw.utils.PermissionHelper
import org.openmw.utils.getAvailableStorageSpace
import org.openmw.utils.readModValues
import java.io.File

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_files_prefs")

object GameFilesPreferences {
    val GAME_FILES_URI_KEY = stringPreferencesKey("game_files_uri")
}

@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.R)
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        PermissionHelper.getManageExternalStoragePermission(this@MainActivity)
        Thread.setDefaultUncaughtExceptionHandler(CaptureCrash())

        val (width, height) = getScreenWidthAndHeight(applicationContext)
        updateResolutionInConfig(width, height)
        val modValues = readModValues()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Hide the system bars
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            OpenMWTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App(applicationContext, modValues)
                }
            }
        }

        // Get storage space
        val availableSpace = getAvailableStorageSpace(this)
        println("Available storage space: $availableSpace bytes")
    }

    private fun updateResolutionInConfig(width: Int, height: Int) {
        val file = File(Constants.SETTINGS_FILE)
        val lines = file.readLines().map { line ->
            when {
                line.startsWith("resolution y =") -> "resolution y = $height" // $height
                line.startsWith("resolution x =") -> "resolution x = $width" // $width
                else -> line
            }
        }
        file.writeText(lines.joinToString("\n"))
    }

    private fun getScreenWidthAndHeight(context: Context): Pair<Int, Int> {
        val windowMetrics: WindowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)
        val bounds = windowMetrics.bounds
        var width = bounds.width()
        var height = bounds.height()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
            val windowInsets: WindowInsets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowManager.currentWindowMetrics.windowInsets
            } else {
                TODO("VERSION.SDK_INT < R")
            }
            val displayCutout = windowInsets.displayCutout
            if (displayCutout != null) {
                width += displayCutout.safeInsetLeft + displayCutout.safeInsetRight
                height += displayCutout.safeInsetTop + displayCutout.safeInsetBottom
            }
        }
        return Pair(width, height)
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun App(context: Context, modValues: List<ModValue>) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Setting.route) {
            SettingScreen(context) {
                navController.navigate(Screen.Home.route)
            }
        }
        composable(Screen.Home.route) {
            HomeScreen(context, modValues) {
                navController.navigate(Screen.Setting.route)
            }
        }
    }
}

private object Route {
    const val SETTINGS = "setting"
    const val HOME = "home"
}

sealed class Screen(val route: String) {
    object Setting: Screen(Route.SETTINGS)
    object Home: Screen(Route.HOME)
}

@Composable
fun BouncingBackground() {
    val image: Painter = painterResource(id = R.drawable.backgroundbouncebw)
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp * configuration.densityDpi / 160
    val screenHeight = configuration.screenHeightDp * configuration.densityDpi / 160

    val imageWidth = 2000 // Replace with your image width
    val imageHeight = 2337 // Replace with your image height

    var offset: Offset by remember { mutableStateOf(Offset.Zero) }
    val xDirection by remember { mutableFloatStateOf(1f) }
    val yDirection by remember { mutableFloatStateOf(1f) }

    // Adjust this value to increase the distance
    val stepSize = 1f

    LaunchedEffect(Unit) {
        while (true) {
            offset = Offset(
                x = (offset.x + xDirection * stepSize) % screenWidth,
                y = (offset.y + yDirection * stepSize) % screenHeight
            )

            delay(16L) // Update every frame (approx 60fps)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = image,
            contentDescription = null,
            modifier = Modifier
                .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                .size(imageWidth.dp, imageHeight.dp) // Convert Int to Dp
                .scale(6f) // Scale the image up by a factor of 5
                .background(color = Color.LightGray))
    }
}
