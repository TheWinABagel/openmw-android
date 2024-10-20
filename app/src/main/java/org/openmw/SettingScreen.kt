package org.openmw

import android.annotation.SuppressLint
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.openmw.navigation.MyFloatingActionButton
import org.openmw.navigation.MyTopBar
import org.openmw.utils.ExpandableBox
import org.openmw.utils.ReadAndDisplayIniValues
import org.openmw.utils.exportCrashAndLogcatFiles
import org.openmw.utils.exportFilesAndDirectories
import org.openmw.utils.importFilesAndDirectories
import org.openmw.utils.importSpecificFile
import java.io.File

@ExperimentalMaterial3Api
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingScreen(context: Context, navigateToHome: () -> Unit) {
    val transparentBlack = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
    var showDialog = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MyTopBar(context)
        },
        content = @Composable {
            BouncingBackground()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp, bottom = 80.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 40.dp, bottom = 60.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ReadAndDisplayIniValues()
                    ExpandableBox(expanded = remember { mutableStateOf(false) })

                    Button(
                        onClick = { exportFilesAndDirectories(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Black)
                            .height(56.dp),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
                        )
                    ) {
                        Text(text = "Backup all saves, config files and screenshots", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { showDialog.value = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Black)
                            .height(56.dp),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
                        )
                    ) {
                        Text(text = "Restore all saves, config files and screenshots", color = Color.White)
                    }
                    // Button to import a save game
                    Button(
                        onClick = { importSpecificFile(context, "settings.cfg") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
                        )
                    ) {
                        Text(text = "Import settings.cfg", color = Color.White)
                    }

                    Button(
                        onClick = { importSpecificFile(context, """.*\.omwsave$""") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
                        )
                    ) {
                        Text(text = "Import save game", color = Color.White)
                    }
                    Button(
                        onClick = { exportCrashAndLogcatFiles(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
                        )
                    ) {
                        Text(text = "Export Crash and Logcat Files", color = Color.White)
                    }
                    OpenLogFileDialogButton()
                    OpenLogcatLogFileDialogButton()
                }
                if (showDialog.value) {
                    AlertDialog(
                        onDismissRequest = { showDialog.value = false },
                        title = { Text("Confirm Import") },
                        text = { Text("Are you sure you want to restore all saves, config files, and screenshots?") },
                        confirmButton = {
                            Button(onClick = {
                                importFilesAndDirectories(context)
                                showDialog.value = false
                            }) {
                                Text("Yes")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDialog.value = false }) {
                                Text("No")
                            }
                        }
                    )
                }
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = transparentBlack,
                actions = {
                    Button(
                        onClick = { navigateToHome() },
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.Transparent),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home, // Make sure you have the correct icon import
                            contentDescription = "Settings",
                            modifier = Modifier.size(80.dp),
                            tint = Color.White
                        )
                    }
                },
                floatingActionButton = {
                    MyFloatingActionButton(context)
                }
            )
        }
    )
}

@Composable
fun OpenLogFileDialogButton() {
    val context = LocalContext.current
    val showDialog = remember { mutableStateOf(false) }
    val logContent = remember { mutableStateOf("") }

    if (showDialog.value) {
        logContent.value = readLogFile(Constants.CRASH_FILE)
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Crash Log") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Text(
                        text = logContent.value,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Row {
                    Button(onClick = { showDialog.value = false }) {
                        Text("OK")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        copyToClipboard(context, logContent.value)
                    }) {
                        Text("Copy")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        clearLogFile(context)
                        showDialog.value = false
                    }) {
                        Text("Clear Log")
                    }
                }
            }
        )
    }

    Button(
        onClick = {
            showDialog.value = true
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
        )
    ) {
        Text("Open Crash Log", color = Color.White)
    }
}

fun clearLogFile(context: Context) {
    val logFile = File(Constants.CRASH_FILE)
    if (logFile.exists()) {
        logFile.delete()
        Toast.makeText(context, "Log file deleted", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "Log file does not exist", Toast.LENGTH_SHORT).show()
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Log Content", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

fun readLogFile(logFilePath: String): String {
    return try {
        File(logFilePath).readText()
    } catch (e: Exception) {
        "Failed to read log file: ${e.message}"
    }
}

@Composable
fun OpenLogcatLogFileDialogButton() {
    val context = LocalContext.current
    val showDialog = remember { mutableStateOf(false) }
    val logContent = remember { mutableStateOf("") }

    if (showDialog.value) {
        logContent.value = readLogFile(Constants.LOGCAT_FILE)
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Logcat Log") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Text(
                        text = logContent.value,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Row {
                    Button(onClick = { showDialog.value = false }) {
                        Text("OK")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        copyToClipboard(context, logContent.value)
                    }) {
                        Text("Copy")
                    }
                }
            }
        )
    }

    Button(
        onClick = {
            showDialog.value = true
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f)
        )
    ) {
        Text("Open Logcat Log", color = Color.White)
    }
}



