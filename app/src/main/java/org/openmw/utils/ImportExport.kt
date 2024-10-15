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
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

fun zipFilesAndDirectories(rootDirectory: File, files: List<File>, directories: List<File>, zipFile: File) {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { out ->
        val addedEntries = mutableSetOf<String>()

        // Add files to the zip
        files.forEach { file ->
            FileInputStream(file).use { fi ->
                BufferedInputStream(fi).use { origin ->
                    val entryName = file.relativeTo(rootDirectory).path
                    if (addedEntries.add(entryName)) {
                        val entry = ZipEntry(entryName)
                        out.putNextEntry(entry)
                        origin.copyTo(out, 1024)
                        out.closeEntry()
                    }
                }
            }
        }

        // Add directories to the zip
        directories.forEach { dir ->
            dir.walkTopDown().forEach { file ->
                val entryName = file.relativeTo(rootDirectory).path
                if (file.isDirectory && !addedEntries.contains(entryName)) {
                    out.putNextEntry(ZipEntry("$entryName/"))
                    addedEntries.add(entryName)
                    out.closeEntry()
                } else if (file.isFile) {
                    FileInputStream(file).use { fi ->
                        BufferedInputStream(fi).use { origin ->
                            if (addedEntries.add(entryName)) {
                                val entry = ZipEntry(entryName)
                                out.putNextEntry(entry)
                                origin.copyTo(out, 1024)
                                out.closeEntry()
                            }
                        }
                    }
                }
            }
        }
    }
}

fun exportFilesAndDirectories(context: Context) {
    val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    val date = dateFormat.format(Date())
    val zipFile = File(downloadFolder, "openmw_$date.zip")
    val rootDirectory = File(Constants.USER_FILE_STORAGE)

    try {
        val filesToZip = listOf(
            File("${Constants.USER_FILE_STORAGE}/config/openmw.cfg"),
            File("${Constants.USER_FILE_STORAGE}/config/settings.cfg"),
            File("${Constants.USER_FILE_STORAGE}/config/shaders.yaml")
        )
        val directoriesToZip = listOf(
            File("${Constants.USER_FILE_STORAGE}/saves"),
            File("${Constants.USER_FILE_STORAGE}/screenshots")
        )
        zipFilesAndDirectories(rootDirectory, filesToZip, directoriesToZip, zipFile)
        Toast.makeText(context, "Files and directories zipped and exported to Downloads", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun unzipFiles(zipFile: File, targetDirectory: File) {
    ZipInputStream(FileInputStream(zipFile)).use { zis ->
        var entry: ZipEntry?
        while (zis.nextEntry.also { entry = it } != null) {
            val newFile = File(targetDirectory, entry!!.name)
            // Create directories for subfolders
            if (entry!!.isDirectory) {
                newFile.mkdirs()
            } else {
                // Extract files
                FileOutputStream(newFile).use { fos ->
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (zis.read(buffer).also { len = it } > 0) {
                        fos.write(buffer, 0, len)
                    }
                }
            }
            zis.closeEntry()
        }
    }
}

fun importFilesAndDirectories(context: Context) {
    val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val targetDirectory = File(Constants.USER_FILE_STORAGE)

    try {
        val newestZipFile = downloadFolder.listFiles { _, name ->
            name.startsWith("openmw_") && name.endsWith(".zip")
        }?.maxByOrNull { it.lastModified() }

        if (newestZipFile != null) {
            unzipFiles(newestZipFile, targetDirectory)
            Toast.makeText(context, "Files and directories imported successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No zip files found for import", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

