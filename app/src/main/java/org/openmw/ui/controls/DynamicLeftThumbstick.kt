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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.libsdl.app.SDLActivity.onNativeKeyUp
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ResizableDraggableThumbstick(
    context: Context,
    id: Int,
    keyCode: Int,
    editMode: Boolean,
    onWClick: () -> Unit,
    onAClick: () -> Unit,
    onSClick: () -> Unit,
    onDClick: () -> Unit,
    onRelease: () -> Unit
) {
    var thumbstickState by remember {
        mutableStateOf(loadButtonState(context).find { it.id == id } ?: ButtonState(
            id,
            200f,
            0f,
            0f,
            false,
            keyCode
        ))
    }
    var buttonSize by remember { mutableStateOf(thumbstickState.size.dp) }
    var offsetX by remember { mutableStateOf(thumbstickState.offsetX) }
    var offsetY by remember { mutableStateOf(thumbstickState.offsetY) }
    var isDragging by remember { mutableStateOf(false) }
    val visible = UIStateManager.visible
    val density = LocalDensity.current
    val radiusPx = with(density) { (buttonSize / 2).toPx() }
    val deadZone = 0.2f * radiusPx
    var touchState by remember { mutableStateOf(Offset(0f, 0f)) }
    val saveState = {
        val updatedState = ButtonState(id, buttonSize.value, offsetX, offsetY, false, keyCode)
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
                contentAlignment = Alignment.Center,
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
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, Color.Black, CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val location = Offset(offset.x, offset.y)
                                    val isInBounds = hypot(
                                        location.x - radiusPx,
                                        location.y - radiusPx
                                    ) <= radiusPx
                                    if (isInBounds) {
                                        touchState = location - Offset(radiusPx, radiusPx)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    val newOffset = touchState + Offset(dragAmount.x, dragAmount.y)
                                    val isInBounds = hypot(newOffset.x, newOffset.y) <= radiusPx
                                    if (isInBounds) {
                                        touchState = newOffset
                                        val xRatio = touchState.x / radiusPx
                                        val yRatio = touchState.y / radiusPx
                                        onNativeKeyUp(KeyEvent.KEYCODE_W)
                                        onNativeKeyUp(KeyEvent.KEYCODE_A)
                                        onNativeKeyUp(KeyEvent.KEYCODE_S)
                                        onNativeKeyUp(KeyEvent.KEYCODE_D)
                                        when {
                                            abs(yRatio) > abs(xRatio) -> {
                                                if (touchState.y < -deadZone) onWClick()
                                                if (touchState.y > deadZone) onSClick()
                                            }

                                            abs(xRatio) > abs(yRatio) -> {
                                                if (touchState.x < -deadZone) onAClick()
                                                if (touchState.x > deadZone) onDClick()
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    touchState = Offset.Zero
                                    onNativeKeyUp(KeyEvent.KEYCODE_W)
                                    onNativeKeyUp(KeyEvent.KEYCODE_A)
                                    onNativeKeyUp(KeyEvent.KEYCODE_S)
                                    onNativeKeyUp(KeyEvent.KEYCODE_D)
                                    onRelease()
                                },
                                onDragCancel = {
                                    touchState = Offset.Zero
                                    onNativeKeyUp(KeyEvent.KEYCODE_W)
                                    onNativeKeyUp(KeyEvent.KEYCODE_A)
                                    onNativeKeyUp(KeyEvent.KEYCODE_S)
                                    onNativeKeyUp(KeyEvent.KEYCODE_D)
                                    onRelease()
                                }
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(25.dp)
                            .offset(
                                x = (touchState.x / density.density).dp,
                                y = (touchState.y / density.density).dp
                            )
                            .background(
                                Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f),
                                shape = CircleShape
                            )
                    )
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
                                if (buttonSize < 50.dp) buttonSize = 50.dp
                                saveState()
                            }
                            .border(2.dp, Color.White, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "-", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
