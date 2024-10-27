package org.openmw.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import kotlinx.coroutines.*
import org.libsdl.app.SDLActivity.onNativeKeyDown
import org.libsdl.app.SDLActivity.onNativeKeyUp
import org.openmw.ui.controls.ButtonState
import org.openmw.ui.controls.CustomCursorView
import org.openmw.ui.controls.DynamicButtonManager
import org.openmw.ui.controls.UIStateManager
import org.openmw.utils.*
import kotlin.math.roundToInt

fun sendKeyEvent(keyCode: Int) {
    onNativeKeyDown(keyCode)
    onNativeKeyUp(keyCode)
}

data class MemoryInfo(
    val totalMemory: String,
    val availableMemory: String,
    val usedMemory: String
)

@SuppressLint("RestrictedApi")
fun toggleCustomCursor(customCursorView: CustomCursorView) {
    runOnUiThread {
        UIStateManager.isCustomCursorEnabled = !UIStateManager.isCustomCursorEnabled
        customCursorView.visibility = if (UIStateManager.isCustomCursorEnabled) View.VISIBLE else View.GONE
    }
}

@Composable
fun OverlayUI(
    engineActivityContext: Context,
    editMode: MutableState<Boolean>,
    createdButtons: SnapshotStateList<ButtonState>,
    customCursorView: CustomCursorView
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val visible = UIStateManager.visible
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        getMessages() // Ensure logcat is enabled
        while (true) {
            if (UIStateManager.isMemoryInfoEnabled) {
                val memoryInfo = getMemoryInfo(context)
                UIStateManager.memoryInfoText = "Total memory: ${memoryInfo.totalMemory}\n" +
                        "Available memory: ${memoryInfo.availableMemory}\n" +
                        "Used memory: ${memoryInfo.usedMemory}"
            } else {
                UIStateManager.memoryInfoText = ""
            }
            if (UIStateManager.isBatteryStatusEnabled) {
                UIStateManager.batteryStatus = getBatteryStatus(context)
            } else {
                UIStateManager.batteryStatus = ""
            }
            if (UIStateManager.isLoggingEnabled) {
                UIStateManager.scrollState.animateScrollTo(UIStateManager.scrollState.maxValue)
                UIStateManager.logMessages = getMessages().joinToString("\n")
            } else {
                UIStateManager.logMessages = ""
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
                },
                label = "size transform"
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
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .background(Color.Red, shape = RoundedCornerShape(8.dp))
                                        .clickable { expanded = false }
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "Close", color = Color.White, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(20.dp))
                                ClickableBox("Hide UI", UIStateManager.isUIHidden) {
                                    UIStateManager.isUIHidden = !UIStateManager.isUIHidden
                                    UIStateManager.visible = !UIStateManager.isUIHidden
                                }
                                ClickableBox("Enable Vibration", UIStateManager.isVibrationEnabled) {
                                    UIStateManager.isVibrationEnabled = !UIStateManager.isVibrationEnabled
                                }
                                ClickableBox("Show Memory Info", UIStateManager.isMemoryInfoEnabled) {
                                    UIStateManager.isMemoryInfoEnabled = !UIStateManager.isMemoryInfoEnabled
                                }
                                ClickableBox("Show Battery Status", UIStateManager.isBatteryStatusEnabled) {
                                    UIStateManager.isBatteryStatusEnabled = !UIStateManager.isBatteryStatusEnabled
                                }
                                ClickableBox("Show Logcat", UIStateManager.isLoggingEnabled) {
                                    UIStateManager.isLoggingEnabled = !UIStateManager.isLoggingEnabled
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier
                                .padding(top = 10.dp, start = 20.dp)
                                .size(30.dp),
                            tint = Color.Black
                        )

                        AnimatedVisibility(
                            visible = visible,
                            enter = slideInVertically(
                                initialOffsetY = { with(density) { -20.dp.roundToPx() } },
                                animationSpec = tween(durationMillis = 1000)
                            ) + expandVertically(
                                expandFrom = Alignment.Bottom,
                                animationSpec = tween(durationMillis = 1000)
                            ) + fadeIn(
                                initialAlpha = 0.3f,
                                animationSpec = tween(durationMillis = 1000)
                            ),
                            exit = slideOutVertically(
                                targetOffsetY = { with(density) { -20.dp.roundToPx() } },
                                animationSpec = tween(durationMillis = 1000)
                            ) + shrinkVertically(
                                animationSpec = tween(durationMillis = 1000)
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 1000)
                            )
                        ) {
                            DynamicButtonManager(
                                context = engineActivityContext,
                                onNewButtonAdded = { newButtonState ->
                                    createdButtons.add(newButtonState)
                                },
                                editMode = editMode,
                                createdButtons = createdButtons
                            )
                            IconButton(
                                onClick = { toggleCustomCursor(customCursorView) },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Transparent
                                )
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "Toggle Custom Cursor",
                                    modifier = Modifier.size(30.dp),
                                    tint = Color.Black
                                )
                            }
                        }
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
            if (UIStateManager.isMemoryInfoEnabled) {
                DraggableBox(editMode = editMode.value) { fontSize ->
                    Text(
                        text = UIStateManager.memoryInfoText,
                        color = Color.White,
                        fontSize = fontSize.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (UIStateManager.isBatteryStatusEnabled) {
                DraggableBox(editMode = editMode.value) { fontSize ->
                    Text(
                        text = UIStateManager.batteryStatus,
                        color = Color.White,
                        fontSize = fontSize.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            if (UIStateManager.isLoggingEnabled) {
                DraggableBox(editMode = editMode.value) { fontSize ->
                    Text(
                        text = UIStateManager.logMessages,
                        color = Color.White,
                        fontSize = fontSize.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun DraggableBox(
    editMode: Boolean,
    content: @Composable (Float) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var boxWidth by remember { mutableFloatStateOf(200f) }
    var boxHeight by remember { mutableFloatStateOf(100f) }
    var isDragging by remember { mutableStateOf(false) }
    var isResizing by remember { mutableStateOf(false) }
    var fontSize by remember { mutableFloatStateOf(10f) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(width = boxWidth.dp, height = boxHeight.dp)
            .background(Color.Transparent)
            .then(
                if (editMode) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDrag = { change, dragAmount ->
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            },
                            onDragEnd = { isDragging = false }
                        )
                    }
                } else Modifier
            )
            .border(2.dp, if (isDragging || isResizing) Color.Red else Color.Transparent)
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                content(fontSize)
            }
            if (editMode) {
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 5f..30f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Red,
                        activeTrackColor = Color(alpha = .9f, red = 0f, green = 0f, blue = 0f),
                        inactiveTrackColor = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )

            }
        }
        if (editMode) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .offset { IntOffset(boxWidth.roundToInt() - 16.dp.toPx().roundToInt() - 16, boxHeight.roundToInt() - 16.dp.toPx().roundToInt() - 16) }
                    .background(Color.Red)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isResizing = true },
                            onDrag = { change, dragAmount ->
                                boxWidth += dragAmount.x
                                boxHeight += dragAmount.y
                            },
                            onDragEnd = { isResizing = false }
                        )
                    }
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun ClickableBox(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = if (enabled) Color.Green else Color.Red, fontSize = 15.sp)
    }
}

