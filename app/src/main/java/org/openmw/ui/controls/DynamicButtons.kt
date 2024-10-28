package org.openmw.ui.controls

import android.content.Context
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.libsdl.app.SDLActivity.onNativeKeyDown
import org.libsdl.app.SDLActivity.onNativeKeyUp
import org.openmw.ui.overlay.sendKeyEvent
import org.openmw.utils.vibrate
import kotlin.math.roundToInt

@Composable
fun ResizableDraggableButton(
    context: Context,
    id: Int,
    keyCode: Int,
    editMode: Boolean,
    onDelete: () -> Unit,
    customCursorView: CustomCursorView
) {
    var buttonState by remember {
        mutableStateOf(
            loadButtonState(context).find { it.id == id }
                ?: ButtonState(id, 100f, 0f, 0f, false, keyCode)
        )
    }
    var buttonSize by remember { mutableStateOf(buttonState.size.dp) }
    var offsetX by remember { mutableFloatStateOf(buttonState.offsetX) }
    var offsetY by remember { mutableFloatStateOf(buttonState.offsetY) }
    var isDragging by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val visible = UIStateManager.visible
    val saveState = {
        val updatedState = ButtonState(id, buttonSize.value, offsetX, offsetY, buttonState.isLocked, keyCode)
        val allStates = loadButtonState(context).filter { it.id != id } + updatedState
        saveButtonState(context, allStates)
    }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
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
                                    onDragEnd = {
                                        isDragging = false
                                        saveState()
                                    }
                                )
                            }
                        } else Modifier
                    )
                    .border(2.dp, if (isDragging) Color.Red else Color.Black, shape = CircleShape)
            ) {
                // Main button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
                                if (isPressed) Color.Green else Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
                            } else {
                                Color(alpha = 0.25f, red = 0f, green = 0f, blue = 0f)
                            }, shape = CircleShape
                        )
                        .clickable {
                            if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
                                isPressed = !isPressed
                                if (isPressed) {
                                    onNativeKeyDown(keyCode)
                                    sendKeyEvent(keyCode)
                                } else {
                                    onNativeKeyUp(keyCode)
                                }
                            } else {
                                onNativeKeyDown(keyCode)
                                sendKeyEvent(keyCode)
                                onNativeKeyUp(keyCode)
                            }
                            if (keyCode == KeyEvent.KEYCODE_Z) {
                                onNativeKeyDown(keyCode)
                                sendKeyEvent(keyCode)
                                onNativeKeyUp(keyCode)

                                if (UIStateManager.isCustomCursorEnabled) {
                                    customCursorView.performMouseClick()
                                } else {
                                    // Trigger KEYCODE_ENTER
                                    onNativeKeyDown(KeyEvent.KEYCODE_ENTER)
                                    sendKeyEvent(KeyEvent.KEYCODE_ENTER)
                                    onNativeKeyUp(KeyEvent.KEYCODE_ENTER)
                                }

                                if (UIStateManager.isVibrationEnabled) {
                                    vibrate(context)
                                }
                            } else if (keyCode == KeyEvent.KEYCODE_E) {
                                onNativeKeyDown(keyCode)
                                sendKeyEvent(keyCode)
                                onNativeKeyUp(keyCode)

                                if (UIStateManager.isVibrationEnabled) {
                                    vibrate(context)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (editMode) {
                        Text(text = "ID: $id, Key: ${keyCodeToChar(keyCode)}", color = Color.White)
                    } else {
                        Text(text = keyCodeToChar(keyCode), color = Color.White)
                    }
                }

                if (editMode) {
                    // + button
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(30.dp)
                            .background(Color.Black, shape = CircleShape)
                            .clickable {
                                buttonSize += 20.dp
                                saveState()
                            }
                            .border(2.dp, Color.White, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // - button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .background(Color.Black, shape = CircleShape)
                            .clickable {
                                buttonSize -= 20.dp
                                if (buttonSize < 50.dp) buttonSize = 50.dp // Minimum size
                                saveState()
                            }
                            .border(2.dp, Color.White, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "-", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Delete button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(30.dp)
                            .background(Color.Red, shape = CircleShape)
                            .clickable {
                                onDelete()
                            }
                            .border(2.dp, Color.White, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "X", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun keyCodeToChar(keyCode: Int): String {
    return when (keyCode) {
        in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 -> "F${keyCode - KeyEvent.KEYCODE_F1 + 1}"
        KeyEvent.KEYCODE_SHIFT_LEFT -> "Shift-L"
        KeyEvent.KEYCODE_SHIFT_RIGHT -> "Shift-R"
        KeyEvent.KEYCODE_CTRL_LEFT -> "Ctrl-L"
        KeyEvent.KEYCODE_CTRL_RIGHT -> "Ctrl-R"
        KeyEvent.KEYCODE_SPACE -> "Space"
        KeyEvent.KEYCODE_ESCAPE -> "Escape"
        KeyEvent.KEYCODE_ENTER -> "Enter"
        KeyEvent.KEYCODE_GRAVE -> "Grave"
        else -> (keyCode - KeyEvent.KEYCODE_A + 'A'.code).toChar().toString()
    }
}
