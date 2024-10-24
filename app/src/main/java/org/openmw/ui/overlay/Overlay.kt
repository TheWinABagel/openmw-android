package org.openmw.ui.overlay

import android.content.Context
import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import org.libsdl.app.SDLActivity.onNativeKeyDown
import org.libsdl.app.SDLActivity.onNativeKeyUp
import org.openmw.ui.controls.UIStateManager
import org.openmw.utils.*

fun sendKeyEvent(keyCode: Int) {
    onNativeKeyDown(keyCode)
    onNativeKeyUp(keyCode)
}

data class MemoryInfo(
    val totalMemory: String,
    val availableMemory: String,
    val usedMemory: String
)

@Composable
fun OverlayUI(engineActivityContext: Context) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var memoryInfoText by remember { mutableStateOf("") }
    var batteryStatus by remember { mutableStateOf("") }
    var logMessages by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    var isMemoryInfoEnabled by remember { mutableStateOf(false) }
    var isBatteryStatusEnabled by remember { mutableStateOf(false) }
    var isLoggingEnabled by remember { mutableStateOf(false) }
    val isUIHidden = UIStateManager.isUIHidden
    var isRunEnabled = UIStateManager.isRunEnabled
    var isF10Enabled by remember { mutableStateOf(false) } // New state for F10
    var isBacktickEnabled by remember { mutableStateOf(false) }
    var isF2Enabled by remember { mutableStateOf(false) }
    var isF3Enabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        getMessages() // Ensure logcat is enabled

        while (true) {
            if (isMemoryInfoEnabled) {
                val memoryInfo = getMemoryInfo(context)
                memoryInfoText = "Total memory: ${memoryInfo.totalMemory}\n" +
                        "Available memory: ${memoryInfo.availableMemory}\n" +
                        "Used memory: ${memoryInfo.usedMemory}"
            } else {
                memoryInfoText = ""
            }

            if (isBatteryStatusEnabled) {
                batteryStatus = getBatteryStatus(context)
            } else {
                batteryStatus = ""
            }

            if (isLoggingEnabled) {
                scrollState.animateScrollTo(scrollState.maxValue)
                logMessages = getMessages().joinToString("\n")
            } else {
                logMessages = ""
            }

            delay(1000)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = Color.Transparent,
            onClick = { expanded = !expanded }
        ) {
            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150, 150)) togetherWith
                            fadeOut(animationSpec = tween(150)) using
                            SizeTransform { initialSize, targetSize ->
                                if (targetState) {
                                    keyframes {
                                        // Expand horizontally first.
                                        IntSize(targetSize.width, initialSize.height) at 150
                                        durationMillis = 600
                                    }
                                } else {
                                    keyframes {
                                        // Shrink vertically first.
                                        IntSize(initialSize.width, targetSize.height) at 150
                                        durationMillis = 600
                                    }
                                }
                            }
                }, label = "size transform"
            ) { targetExpanded ->
                if (targetExpanded) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f))
                            .padding(5.dp)
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .background(Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f))
                                .padding(5.dp)
                        ) {
                            item {
                                Text(
                                    text = "Show Memory Info",
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                                Switch(
                                    checked = isMemoryInfoEnabled,
                                    onCheckedChange = { isMemoryInfoEnabled = it }
                                )
                                Text(
                                    text = "Show Battery Status",
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                                Switch(
                                    checked = isBatteryStatusEnabled,
                                    onCheckedChange = { isBatteryStatusEnabled = it }
                                )
                                Text(
                                    text = "Show Logcat",
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                                Switch(
                                    checked = isLoggingEnabled,
                                    onCheckedChange = { isLoggingEnabled = it }
                                )
                                Text(text = "Enable Vibration", color = Color.White, fontSize = 10.sp)
                                Switch(
                                    checked = UIStateManager.isVibrationEnabled,
                                    onCheckedChange = { UIStateManager.isVibrationEnabled = it })
                                Text(text = "Hide UI", color = Color.White, fontSize = 10.sp)
                                Switch(checked = isUIHidden, onCheckedChange = {
                                    UIStateManager.isUIHidden = it
                                    UIStateManager.visible = !it
                                })
                                // Run/Walk Toggle Switch
                                Text(text = "Run / Walk", color = Color.White, fontSize = 10.sp)
                                Switch(checked = isRunEnabled, onCheckedChange = {
                                    UIStateManager.isRunEnabled = it
                                    if (it) onNativeKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT)
                                    else onNativeKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT)
                                })
                                // F10 Toggle Switch
                                Text(text = "Press F10", color = Color.White, fontSize = 10.sp)
                                Switch(checked = isF10Enabled, onCheckedChange = {
                                    isF10Enabled = it
                                    if (it) {
                                        onNativeKeyDown(KeyEvent.KEYCODE_F10)
                                        onNativeKeyUp(KeyEvent.KEYCODE_F10)
                                    } else {
                                        onNativeKeyDown(KeyEvent.KEYCODE_F10)
                                        onNativeKeyUp(KeyEvent.KEYCODE_F10)
                                    }
                                })
                                // Backtick Toggle Switch
                                Text(text = "Console", color = Color.White, fontSize = 10.sp)
                                Switch(checked = isBacktickEnabled, onCheckedChange = {
                                    isBacktickEnabled = it
                                    if (it) {
                                        onNativeKeyDown(KeyEvent.KEYCODE_GRAVE)
                                        onNativeKeyUp(KeyEvent.KEYCODE_GRAVE)
                                    } else {
                                        onNativeKeyDown(KeyEvent.KEYCODE_GRAVE)
                                        onNativeKeyUp(KeyEvent.KEYCODE_GRAVE)
                                    }
                                })
                            }
                        }
                        LazyRow(
                            modifier = Modifier
                                .background(Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f))
                                .padding(5.dp)
                        ) {
                            item {
                                // Button for J (Journal)
                                IconButton(onClick = {
                                    onNativeKeyDown(KeyEvent.KEYCODE_J)
                                    onNativeKeyUp(KeyEvent.KEYCODE_J)
                                }) {
                                    Text(text = "Journal", color = Color.White, fontSize = 10.sp)
                                }
                                // Button for F5 (Quicksave)
                                IconButton(onClick = {
                                    onNativeKeyDown(KeyEvent.KEYCODE_F5)
                                    onNativeKeyUp(KeyEvent.KEYCODE_F5)
                                }) {
                                    Text(text = "Quicksave", color = Color.White, fontSize = 10.sp)
                                }

                                // Button for F6 (Quickload)
                                IconButton(onClick = {
                                    onNativeKeyDown(KeyEvent.KEYCODE_F6)
                                    onNativeKeyUp(KeyEvent.KEYCODE_F6)
                                }) {
                                    Text(text = "Quickload", color = Color.White, fontSize = 10.sp)
                                }

                                // Button for F12 (Screenshot)
                                IconButton(onClick = {
                                    onNativeKeyDown(KeyEvent.KEYCODE_F12)
                                    onNativeKeyUp(KeyEvent.KEYCODE_F12)
                                }) {
                                    Text(text = "Screenshot", color = Color.White, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                // F2 Icon
                                IconButton(onClick = {
                                    isF2Enabled = !isF2Enabled
                                    if (isF2Enabled) {
                                        onNativeKeyDown(KeyEvent.KEYCODE_F2)
                                        onNativeKeyUp(KeyEvent.KEYCODE_F2)
                                    } else {
                                        onNativeKeyDown(KeyEvent.KEYCODE_F2)
                                        onNativeKeyUp(KeyEvent.KEYCODE_F2)
                                    }
                                }) {
                                    Text(text = "F2", color = Color.White, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                // F3 Icon
                                IconButton(onClick = {
                                    isF3Enabled = !isF3Enabled
                                    if (isF3Enabled) {
                                        onNativeKeyDown(KeyEvent.KEYCODE_F3)
                                        onNativeKeyUp(KeyEvent.KEYCODE_F3)
                                    } else {
                                        onNativeKeyDown(KeyEvent.KEYCODE_F3)
                                        onNativeKeyUp(KeyEvent.KEYCODE_F3)
                                    }
                                }) {
                                    Text(text = "F3", color = Color.White, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                } else {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    Button(
                        onClick = {
                            onNativeKeyDown(KeyEvent.KEYCODE_J)
                            onNativeKeyUp(KeyEvent.KEYCODE_J)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .padding(start = 60.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Sneak", tint = Color.Black)
                    }
                    Button(
                        onClick = {
                            onNativeKeyDown(KeyEvent.KEYCODE_T)
                            onNativeKeyUp(KeyEvent.KEYCODE_T)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .padding(start = 20.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rest", tint = Color.Black)
                    }
                }
            }
        }
        // Information display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isMemoryInfoEnabled) {
                Text(
                    text = memoryInfoText,
                    color = Color.White,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isBatteryStatusEnabled) {
                Text(
                    text = batteryStatus,
                    color = Color.White,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isLoggingEnabled) {
                Box(
                    modifier = Modifier
                        .width(400.dp)
                        .height(200.dp)
                        .padding(vertical = 35.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        // Your text content here
                        Text(
                            text = logMessages,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
