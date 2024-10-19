package org.openmw

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.openmw.navigation.MyFloatingActionButton
import org.openmw.navigation.MyTopBar
import org.openmw.utils.ExpandableBox
import org.openmw.utils.ReadAndDisplayIniValues
import org.openmw.utils.exportFilesAndDirectories
import org.openmw.utils.importFilesAndDirectories

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

                    Button(onClick = { exportFilesAndDirectories(context) }) {
                        Text(text = "Backup all saves, config files and screenshots", color = Color.White)
                    }

                    Button(onClick = { showDialog.value = true }) {
                        Text(text = "Restore all saves, config files and screenshots", color = Color.White)
                    }
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
                    Button(onClick = { navigateToHome() }) {
                        Text("Return Home")
                    }
                },
                floatingActionButton = {
                    MyFloatingActionButton(context)
                }
            )
        }
    )
}