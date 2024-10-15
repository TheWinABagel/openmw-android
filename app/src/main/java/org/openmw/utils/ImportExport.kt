package org.openmw.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.openmw.Constants
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Composable
fun CfgImport() {
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var savedPath by remember { mutableStateOf<String?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        selectedFileUri = uri
    }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                openDocumentLauncher.launch(arrayOf("*/*"))
            }) {
                Text(text = "Import", color = Color.White)
            }

            selectedFileUri?.let { uri ->
                val destinationFile = File(Constants.USER_OPENMW_CFG)
                copyFile(context, uri, destinationFile)
                savedPath = destinationFile.absolutePath
            }

            savedPath?.let {
                Toast.makeText(context, "Saved Path: $it", Toast.LENGTH_LONG).show()
            }


            Button(onClick = {
                val downloadFolder =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destinationFile = File(downloadFolder, "openmw.cfg")
                try {
                    copyFile(
                        context,
                        Uri.fromFile(File(Constants.USER_OPENMW_CFG)),
                        destinationFile
                    )
                    Toast.makeText(context, "File exported to Downloads", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }) {
                Text(text = "Export", color = Color.White)
            }
        }
    }
}

fun copyFile(context: Context, uri: Uri, destinationFile: File) {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    val outputStream: FileOutputStream = FileOutputStream(destinationFile)
    inputStream?.use { input ->
        outputStream.use { output ->
            input.copyTo(output)
        }
    }
}
