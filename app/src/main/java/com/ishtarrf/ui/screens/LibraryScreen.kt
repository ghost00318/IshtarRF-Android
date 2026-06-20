package com.ishtarrf.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ishtarrf.domain.SubEntry
import com.ishtarrf.ui.MainViewModel
import com.ishtarrf.ui.UiState
import com.ishtarrf.ui.components.LabeledDropdown
import com.ishtarrf.ui.components.SaveSignalDialog
import com.ishtarrf.ui.components.TextPromptDialog
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(state: UiState, vm: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var showSave by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<SubEntry?>(null) }
    var deleting by remember { mutableStateOf<SubEntry?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            val name = (queryDisplayName(context, uri) ?: "imported").removeSuffix(".sub")
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (content != null) vm.importSub(name, content)
        }
    }

    val filtered = state.library.filter { it.name.contains(query, ignoreCase = true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            LabeledDropdown(
                label = "Folder",
                options = state.folders,
                selected = state.selectedFolder,
                optionLabel = { it },
                onSelect = vm::selectFolder,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.FileDownload, null, Modifier.padding(end = 4.dp)); Text("Import")
                }
                OutlinedButton(onClick = { showNewFolder = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CreateNewFolder, null, Modifier.padding(end = 4.dp)); Text("Folder")
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { showSave = true },
                enabled = state.currentSignal != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save current signal…") }
        }
        item {
            androidx.compose.material3.OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text("Search") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    "No .sub files in this folder.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
        } else {
            items(filtered, key = { it.path }) { entry ->
                SignalRow(
                    entry = entry,
                    onLoad = { vm.loadSignal(entry) },
                    onShare = { scope.launch { shareSub(context, vm, entry) } },
                    onRename = { renaming = entry },
                    onDelete = { deleting = entry },
                )
            }
        }
    }

    if (showSave) {
        SaveSignalDialog(
            folders = state.folders,
            defaultFolder = state.selectedFolder,
            onDismiss = { showSave = false },
            onSave = { name, folder -> vm.saveCurrentAsSub(name, folder); showSave = false },
        )
    }
    if (showNewFolder) {
        TextPromptDialog(
            title = "New folder",
            label = "Folder name",
            confirmLabel = "Create",
            onDismiss = { showNewFolder = false },
            onConfirm = { vm.createFolder(it); showNewFolder = false },
        )
    }
    renaming?.let { entry ->
        TextPromptDialog(
            title = "Rename",
            label = "New name",
            initial = entry.name,
            confirmLabel = "Rename",
            onDismiss = { renaming = null },
            onConfirm = { vm.renameSignal(entry, it); renaming = null },
        )
    }
    deleting?.let { entry ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${entry.name}.sub?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { vm.deleteSignal(entry); deleting = null }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleting = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SignalRow(
    entry: SubEntry,
    onLoad: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${entry.name}.sub",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            IconButton(onClick = onLoad) {
                Icon(Icons.Filled.PlayArrow, "Load", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onShare) { Icon(Icons.Filled.Share, "Share") }
            IconButton(onClick = onRename) { Icon(Icons.Filled.DriveFileRenameOutline, "Rename") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else {
                null
            }
        }

private fun shareSub(context: Context, vm: MainViewModel, entry: SubEntry) {
    val file = vm.fileFor(entry)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share ${entry.name}.sub"))
}
