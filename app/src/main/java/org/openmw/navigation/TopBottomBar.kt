package org.openmw.navigation

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.openmw.Constants.SETTINGS_FILE
import org.openmw.EngineActivity
import org.openmw.GameFilesPreferences
import org.openmw.R
import org.openmw.dataStore
import org.openmw.fragments.NavmeshActivity
import org.openmw.fragments.containsMorrowindFolder
import org.openmw.fragments.getGameFilesUri
import org.openmw.ui.theme.transparentBlack
import org.openmw.utils.UnzipWithProgress
import org.openmw.utils.UserManageAssets
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@ExperimentalMaterial3Api
@Composable
fun MyTopBar(context: Context) {
    var expanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    val settingsFile = File(SETTINGS_FILE)
    val destDirectory = LocalContext.current.getExternalFilesDir(null)?.absolutePath + "/Morrowind"
    var directoryExists by remember { mutableStateOf(File(destDirectory).exists()) }
    var showUnzipProgress by remember { mutableStateOf(false) }
    val zipFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/Morrowind.zip"
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = transparentBlack,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Text(
                "Openmw for Android",
                maxLines = 1,
                color = Color.White,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    modifier = Modifier
                        .border(
                            BorderStroke(width = 1.dp, color = Color.Black)
                        ),
                    contentDescription = "Localized description"
                )
            }
            DropdownMenu(
                modifier = Modifier
                    .background(color = transparentBlack)
                    .border(
                        BorderStroke(width = 1.dp, color = Color.Black)
                    ),
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = {
                        if (directoryExists) {
                            Text("Uninstall Morrowind files", color = Color.White)
                        } else {
                            Text("Install Morrowind files", color = Color.White)
                        }
                    },
                    onClick = {
                        if (directoryExists) {
                            // Code to delete the Morrowind folder
                            File(destDirectory).deleteRecursively()
                            directoryExists = false // Update the state
                        } else {
                            // Check if the zip file exists
                            val zipFile = File(zipFilePath)
                            if (!zipFile.exists()) {
                                Toast.makeText(context, "Zip file does not exist.", Toast.LENGTH_LONG).show()
                            } else {
                                // Check if the zip file contains the Morrowind folder before setting showUnzipProgress
                                if (containsMorrowindFolder(zipFilePath)) {
                                    showUnzipProgress = true
                                } else {
                                    Toast.makeText(context, "Zip file does not contain a Morrowind folder.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
                if (showUnzipProgress) {
                    UnzipWithProgress {
                        showUnzipProgress = false
                        directoryExists = true // Update the state after unzipping
                    }
                }
                DropdownMenuItem(
                    text = { Text("Build Navmesh", color = Color.White) },
                    onClick = {
                        Toast.makeText(context, "Not Implemented Yet", Toast.LENGTH_SHORT).show()
                        /*
                        val intent = Intent(context, NavmeshActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)

                         */
                    }
                )
                DropdownMenuItem(
                    text = { Text("Reset Settings", color = Color.White) },
                    onClick = {
                        showDialog = true
                    }
                )
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Confirm Reset") },
                        text = { Text("Are you sure you want to reset the settings?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (settingsFile.exists()) {
                                        settingsFile.delete()
                                        Log.d("ManageAssets", "Deleted existing file: $SETTINGS_FILE")
                                        // Copy over settings.cfg
                                        UserManageAssets(context).resetUserConfig()
                                        expanded = false
                                    }
                                    Toast.makeText(context, "Settings file reset", Toast.LENGTH_SHORT).show()
                                    showDialog = false
                                }
                            ) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = { showDialog = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        },
        modifier = Modifier.height(60.dp)
    )
}

@ExperimentalMaterial3Api
@Composable
fun MyFloatingActionButton(context: Context) {

    var gameFilesUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        gameFilesUri = getGameFilesUri(context)
    }

    FloatingActionButton(
        onClick = {
            val uri = getGameFilesUri(context)
            if (uri != null && uri.contains("Morrowind")) {
                val intent = Intent(context, EngineActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Morrowind folder not found. Please select game files.", Toast.LENGTH_LONG).show()
            }
        },
        containerColor = Color(alpha = 0.6f, red = 0f, green = 0f, blue = 0f),
        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Your Image"
        )
    }
}



