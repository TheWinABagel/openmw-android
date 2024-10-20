package org.openmw.utils

import android.app.Activity
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.openmw.Constants
import org.openmw.fragments.LaunchDocumentTree
import org.openmw.fragments.ModsFragment
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File
import java.util.Locale

data class ModValue(
    val category: String,
    val value: String,
    val originalIndex: Int,
    val isChecked: Boolean
)

fun readModValues(): List<ModValue> {
    val values = mutableListOf<ModValue>()
    val validCategories = setOf("data", "content", "groundcover")

    File(Constants.USER_OPENMW_CFG).forEachLine { line ->
        val trimmedLine = line.trim()
        if ("=" in trimmedLine) {
            val isChecked = !trimmedLine.startsWith("#")
            val (category, value) = trimmedLine.removePrefix("#").split("=", limit = 2).map { it.trim() }
            if (category in validCategories) {
                values.add(ModValue(category, value, values.size + 1, isChecked))
            }
        }
    }
    return values
}

fun writeModValuesToFile(modValues: List<ModValue>, filePath: String, ignoreList: List<String>) {
    val file = File(filePath)

    // Read existing lines while maintaining order and categories
    val existingLines = mutableListOf<String>().apply {
        if (file.exists()) {
            file.forEachLine { line ->
                if (line.trim().isNotEmpty()) {
                    add(line.trim())
                }
            }
        }
    }

    val sortedModValues = modValues.sortedWith(compareBy({ it.category }, { it.originalIndex }))
    val categorizedModValues = sortedModValues.groupBy { it.category }
    val orderedCategories = listOf("data", "content", "groundcover")

    // Create a map to track already present lines per category
    val existingCategoryLines = orderedCategories.associateWith { category ->
        existingLines.filter { it.startsWith(category) }.toMutableList()
    }

    // Append new lines to existing category lines
    orderedCategories.forEach { category ->
        categorizedModValues[category]?.sortedBy { it.originalIndex }?.forEach { modValue ->
            val line = "${modValue.category}=${modValue.value}" // Ignore isChecked
            val lineWithoutPrefix = line.removePrefix("#")
            val duplicates = existingCategoryLines[category]?.map { it.removePrefix("#") } ?: emptyList()
            if (!duplicates.contains(lineWithoutPrefix) && !ignoreList.contains(modValue.value)) { // Add only if it's not a duplicate and not in ignore list
                existingCategoryLines[category]!!.add(line)
            }
        }
    }

    // Combine final lines preserving the order
    val finalLines = mutableListOf<String>()
    orderedCategories.forEach { category ->
        finalLines.addAll(existingCategoryLines[category]!!)
        finalLines.add("") // Add a newline between categories
    }

    // Write all lines back to the file
    file.writeText(finalLines.joinToString("\n"))
}


@ExperimentalFoundationApi
@Composable
fun ModValuesList(modValues: List<ModValue>) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val categories = listOf("data", "content", "groundcover")
    var isExpanded by remember { mutableStateOf(false) }
    var categorizedModValues by remember {
        mutableStateOf(categories.map { category ->
            modValues.filter { it.category == category }
        })
    }
    val lazyListState = rememberLazyListState()
    val view = LocalView.current
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var newModPath by remember { mutableStateOf<String?>(null) }
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val currentList = categorizedModValues[selectedTabIndex].toMutableList()
        val movedItem = currentList.removeAt(from.index)
        currentList.add(to.index, movedItem)
        categorizedModValues = categorizedModValues.toMutableList().apply {
            this[selectedTabIndex] = currentList
        }
    }
    val openDocumentTreeLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    val modsFragment = ModsFragment()
                    modsFragment.modDocumentTreeSelection(context, uri) { modPath ->
                        newModPath = modPath
                    }
                }
            }
            // Reload the mod values and update the UI
            val newModValues = readModValues()
            categorizedModValues = categories.map { category ->
                newModValues.filter { it.category == category }
            }
        }
    Column {
        ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        selectedTabIndex = index
                    },
                    text = {
                        Text(category.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        })
                    }
                )
            }
            Tab(
                selected = selectedTabIndex == categories.size,
                onClick = {
                    LaunchDocumentTree(openDocumentTreeLauncher, context) { modPath ->
                        newModPath = modPath
                    }
                },
                text = { Text("Add Mod") }
            )
            Tab(
                selected = selectedTabIndex == categories.size,
                onClick = {
                    showDialog = true
                },
                icon = {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                }
            )
        }

        // Show dialog when showDialog is true
        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Card {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .height(200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Import / Export options for using a")
                        Text("custom openmw.cfg.")
                        CfgImport()
                    }
                }
            }
        } else {
            // Update UI after dialog closes
            val newModValues = readModValues()
            categorizedModValues = categories.map { category ->
                newModValues.filter { it.category == category }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categorizedModValues[selectedTabIndex], key = { it.originalIndex }) { modValue ->
                var isChecked by remember { mutableStateOf(modValue.isChecked) }
                ReorderableItem(reorderableLazyListState, key = modValue.originalIndex) {
                    //var isDragging by remember { mutableStateOf(false) }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (modValue.isChecked) Color.DarkGray else MaterialTheme.colorScheme.surface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = modValue.value,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Load Order: ${modValue.originalIndex}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            isChecked = checked
                                            val currentList =
                                                categorizedModValues[selectedTabIndex].toMutableList()
                                            val index =
                                                currentList.indexOfFirst { it.originalIndex == modValue.originalIndex }
                                            if (index != -1) {
                                                currentList[index] =
                                                    currentList[index].copy(isChecked = checked)
                                            }
                                            categorizedModValues =
                                                categorizedModValues.toMutableList().apply {
                                                    this[selectedTabIndex] = currentList
                                                }
                                            // Update file
                                            val file = File(Constants.USER_OPENMW_CFG)
                                            val existingLines = mutableListOf<String>()
                                            if (file.exists()) {
                                                file.forEachLine { line ->
                                                    if (line.trim().isNotEmpty()) {
                                                        existingLines.add(line.trim())
                                                    }
                                                }
                                            }
                                            val updatedLines = existingLines.map { line ->
                                                if (line.contains(modValue.value)) {
                                                    if (checked) {
                                                        modValue.category + "=" + modValue.value
                                                    } else {
                                                        "#" + modValue.category + "=" + modValue.value
                                                    }
                                                } else {
                                                    line
                                                }
                                            }
                                            file.writeText(updatedLines.joinToString("\n") + "\n")

                                            // Reload the mod values and update the UI
                                            val newModValues = readModValues()
                                            categorizedModValues = categories.map { category ->
                                                newModValues.filter { it.category == category }
                                            }
                                        }
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = { isExpanded = !isExpanded }
                                        ) {
                                            Text(if (isExpanded) "Collapse" else "Expand")
                                        }
                                        IconButton(
                                            modifier = Modifier.draggableHandle(
                                                onDragStarted = {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                        view.performHapticFeedback(HapticFeedbackConstants.DRAG_START)
                                                    }
                                                },
                                                onDragStopped = {
                                                    // Update the index numbers only when the drag stops
                                                    val currentList = categorizedModValues[selectedTabIndex].toMutableList()
                                                    currentList.forEachIndexed { index, item ->
                                                        currentList[index] = item.copy(originalIndex = index + 1)
                                                    }
                                                    categorizedModValues = categorizedModValues.toMutableList().apply {
                                                        this[selectedTabIndex] = currentList
                                                    }

                                                    // Directly update the file with the new order
                                                    val file = File(Constants.USER_OPENMW_CFG)
                                                    val existingLines = mutableListOf<String>()
                                                    if (file.exists()) {
                                                        file.forEachLine { line ->
                                                            if (line.trim().isNotEmpty()) {
                                                                existingLines.add(line.trim())
                                                            }
                                                        }
                                                    }

                                                    // Update file with reordered lines based only on index
                                                    val finalLines = categorizedModValues.flatten().sortedBy { it.originalIndex }.map { modValue ->
                                                        existingLines.find { it.contains(modValue.value) } ?: "${modValue.category}=${modValue.value}"
                                                    }
                                                    file.writeText(finalLines.joinToString("\n") + "\n")
                                                }
                                            ),
                                            onClick = { /* Handle any onClick action for the icon */ },
                                        ) {
                                            Icon(Icons.Rounded.Menu, contentDescription = "Reorder")
                                        }
                                    }
                                }
                                if (isExpanded) {
                                        Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Switch to ${if (modValue.category == "content") "groundcover" else "content"}")
                                            Button(
                                                onClick = {
                                                    val updatedList = categorizedModValues[selectedTabIndex].toMutableList()
                                                    val index = updatedList.indexOfFirst { it.originalIndex == modValue.originalIndex }
                                                    if (index != -1) {
                                                        val newCategory = if (modValue.category == "content") "groundcover" else "content"
                                                        updatedList[index] = updatedList[index].copy(category = newCategory)
                                                        categorizedModValues = categorizedModValues.toMutableList().apply {
                                                            this[selectedTabIndex] = updatedList
                                                    }

                                                    // Update the file directly with the new category values
                                                    val file = File(Constants.USER_OPENMW_CFG)
                                                    val existingLines = mutableListOf<String>()
                                                    if (file.exists()) {
                                                        file.forEachLine { line ->
                                                            if (line.trim().isNotEmpty()) {
                                                                existingLines.add(line.trim())
                                                            }
                                                        }
                                                    }

                                                    // Update file with new categories
                                                    val finalLines = categorizedModValues.flatten().map { modValue ->
                                                        "${modValue.category}=${modValue.value}"
                                                    }
                                                    file.writeText(finalLines.joinToString("\n") + "\n")

                                                    // Reload the mod values and update the UI
                                                    val newModValues = readModValues()
                                                    categorizedModValues = categories.map { category ->
                                                        newModValues.filter { it.category == category }
                                                    }
                                                }
                                                isExpanded = false
                                            }
                                        ) {
                                            Text("Switch Category")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
