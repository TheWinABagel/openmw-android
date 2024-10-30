package org.openmw.ui.controls

import android.content.Context
import android.view.KeyEvent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.openmw.Constants
import java.io.File

data class ButtonState(
    val id: Int,
    val size: Float,
    val offsetX: Float,
    val offsetY: Float,
    val isLocked: Boolean,
    val keyCode: Int
)

object UIStateManager {
    var isUIHidden by mutableStateOf(false)
    var visible by mutableStateOf(true)
    var isVibrationEnabled by mutableStateOf(true)
    var isCustomCursorEnabled by mutableStateOf(false)

    // Add the shared states
    var memoryInfoText by mutableStateOf("")
    var batteryStatus by mutableStateOf("")
    var logMessages by mutableStateOf("")
    var isMemoryInfoEnabled by mutableStateOf(false)
    var isBatteryStatusEnabled by mutableStateOf(false)
    var isLoggingEnabled by mutableStateOf(false)
    var isLogcatEnabled by mutableStateOf(false)
    val scrollState = ScrollState(0)
    var isThumbDragging by mutableStateOf(false)

    var newX by mutableFloatStateOf(0f) // New shared variable
    var newY by mutableFloatStateOf(0f) // New shared variable
}

fun saveButtonState(context: Context, state: List<ButtonState>) {
    val file = File("${Constants.USER_CONFIG}/UI.cfg")
    if (!file.exists()) {
        file.createNewFile()
    }

    val thumbstick = loadButtonState(context).find { it.id == 99 }
    val existingStates = state.filter { it.id != 99 }.toMutableList()

    thumbstick?.let { existingStates.add(it) }

    file.printWriter().use { out ->
        existingStates.forEach { button ->
            out.println("ButtonID_${button.id}(${button.size};${button.offsetX};${button.offsetY};${button.isLocked};${button.keyCode})")
        }
    }
}


fun loadButtonState(context: Context): List<ButtonState> {
    val file = File("${Constants.USER_CONFIG}/UI.cfg")
    return if (file.exists()) {
        file.readLines().mapNotNull { line ->
            val regex = """ButtonID_(\d+)\(([\d.]+);([\d.]+);([\d.]+);(true|false);(\d+)\)""".toRegex()
            val matchResult = regex.find(line)
            matchResult?.let {
                ButtonState(
                    id = it.groupValues[1].toInt(),
                    size = it.groupValues[2].toFloat(),
                    offsetX = it.groupValues[3].toFloat(),
                    offsetY = it.groupValues[4].toFloat(),
                    isLocked = it.groupValues[5].toBoolean(),
                    keyCode = it.groupValues[6].toInt()
                )
            }
        }
    } else {
        emptyList()
    }
}

@Composable
fun KeySelectionMenu(onKeySelected: (Int) -> Unit, usedKeys: List<Int>, editMode: MutableState<Boolean>) {
    // Add A, S, D, and W to usedKeys
    val reservedKeys = listOf(
        KeyEvent.KEYCODE_A,
        KeyEvent.KEYCODE_S,
        KeyEvent.KEYCODE_D,
        KeyEvent.KEYCODE_W
    )
    val allUsedKeys = usedKeys + reservedKeys

    val letterKeys = ('A'..'Z').toList().filter { key ->
        val keyCode = KeyEvent.KEYCODE_A + key.minus('A')
        keyCode !in allUsedKeys
    }
    val fKeys = (KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12).filter { keyCode ->
        keyCode !in allUsedKeys
    }
    val additionalKeys = listOf(
        KeyEvent.KEYCODE_SHIFT_LEFT,
        KeyEvent.KEYCODE_SHIFT_RIGHT,
        KeyEvent.KEYCODE_CTRL_LEFT,
        KeyEvent.KEYCODE_CTRL_RIGHT,
        KeyEvent.KEYCODE_SPACE,
        KeyEvent.KEYCODE_ESCAPE,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_GRAVE
    ).filter { keyCode -> keyCode !in allUsedKeys }

    var showDialog by remember { mutableStateOf(false) }
    IconButton(onClick = {
        showDialog = true
        editMode.value = true
    }) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add Button",
            modifier = Modifier.size(36.dp), // Adjust the icon size here
            tint = Color.Red // Change the color here
        )
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .widthIn(min = 300.dp, max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Select a Key",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    letterKeys.chunked(6).forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { key ->
                                val keyCode = KeyEvent.KEYCODE_A + key.minus('A')
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.LightGray, shape = CircleShape)
                                        .clickable {
                                            onKeySelected(keyCode)
                                            showDialog = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color.White, thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = "Select a Function Key",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    fKeys.chunked(6).forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { keyCode ->
                                val key = "F${keyCode - KeyEvent.KEYCODE_F1 + 1}"
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.LightGray, shape = CircleShape)
                                        .clickable {
                                            onKeySelected(keyCode)
                                            showDialog = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color.White, thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = "Select a Unique Key, The shift keys toggle.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    additionalKeys.chunked(4).forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { keyCode ->
                                val key = when (keyCode) {
                                    KeyEvent.KEYCODE_SHIFT_LEFT -> "Shift-L"
                                    KeyEvent.KEYCODE_SHIFT_RIGHT -> "Shift-R"
                                    KeyEvent.KEYCODE_CTRL_LEFT -> "Ctrl-L"
                                    KeyEvent.KEYCODE_CTRL_RIGHT -> "Ctrl-R"
                                    KeyEvent.KEYCODE_SPACE -> "Space"
                                    KeyEvent.KEYCODE_ESCAPE -> "Escape"
                                    KeyEvent.KEYCODE_ENTER -> "Enter"
                                    KeyEvent.KEYCODE_GRAVE -> "`"
                                    else -> keyCode.toString()
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.LightGray, shape = CircleShape)
                                        .clickable {
                                            onKeySelected(keyCode)
                                            showDialog = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showDialog = false
                            editMode.value = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicButtonManager(
    context: Context,
    onNewButtonAdded: (ButtonState) -> Unit,
    editMode: MutableState<Boolean>,
    createdButtons: List<ButtonState>
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(start = 40.dp) // Add padding to space from the left
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                showDialog = !showDialog
                editMode.value = showDialog
            }) {
                Icon(Icons.Default.Build, contentDescription = "Button Menu")
            }
        }

        if (showDialog) {
            KeySelectionMenu(
                onKeySelected = { keyCode ->
                    val allButtons = loadButtonState(context)
                    val thumbstick = allButtons.find { it.id == 99 }
                    val otherButtons = allButtons.filter { it.id != 99 }
                    val maxExistingId = otherButtons.maxOfOrNull { it.id } ?: 0
                    val newId = maxExistingId + 1
                    val newButtonState = ButtonState(
                        id = newId,
                        size = 100f,
                        offsetX = 100f,
                        offsetY = 100f,
                        isLocked = false,
                        keyCode = keyCode
                    )
                    val updatedButtons = otherButtons + newButtonState
                    thumbstick?.let { updatedButtons + it }
                    saveButtonState(context, updatedButtons)
                    onNewButtonAdded(newButtonState)
                    showDialog = false
                    editMode.value = true
                },
                usedKeys = createdButtons.map { it.keyCode },
                editMode = editMode
            )
        }
    }
}
