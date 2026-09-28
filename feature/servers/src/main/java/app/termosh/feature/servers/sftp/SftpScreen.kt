package app.termosh.feature.servers.sftp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.termosh.core.ssh.RemoteFile
import app.termosh.core.ui.component.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SftpScreen(
    onBack: () -> Unit,
    viewModel: SftpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val uploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.startUpload(it) } }

    val downloadTarget = remember { arrayOfNulls<RemoteFile>(1) }
    val downloadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        uri?.let { u -> downloadTarget[0]?.let { f -> viewModel.startDownload(f, u) } }
    }

    if (state.showNewDirDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNewDirDialog() },
            title = { Text("Новая папка") },
            text = {
                OutlinedTextField(
                    value = state.newDirName,
                    onValueChange = viewModel::setNewDirName,
                    label = { Text("Имя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.createDir() }) { Text("Создать") } },
            dismissButton = { TextButton(onClick = { viewModel.dismissNewDirDialog() }) { Text("Отмена") } },
        )
    }

    if (state.showRenameDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRenameDialog() },
            title = { Text("Переименовать") },
            text = {
                OutlinedTextField(
                    value = state.renameNewName,
                    onValueChange = viewModel::setRenameNewName,
                    label = { Text("Новое имя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.doRename() }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { viewModel.dismissRenameDialog() }) { Text("Отмена") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.serverName.ifEmpty { "SFTP" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openNewDirDialog() }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New folder")
                    }
                    IconButton(onClick = { uploadLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.Upload, contentDescription = "Upload")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Путь + вверх
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.goUp() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.currentPath,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.status.isNotBlank()) {
                Text(
                    state.status,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }

            state.error?.let {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
                }
            }

            when {
                state.loading && state.files.isEmpty() ->
                    CircularProgressIndicator(modifier = Modifier.fillMaxSize())
                state.files.isEmpty() ->
                    EmptyState(title = "Пусто", subtitle = state.currentPath)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.files, key = { it.path }) { file ->
                        FileRow(
                            file = file,
                            onOpen = { viewModel.openItem(file) },
                            onRename = { viewModel.openRenameDialog(file) },
                            onDelete = { viewModel.startDelete(file) },
                            onDownload = {
                                downloadTarget[0] = file
                                downloadLauncher.launch(file.name)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    file: RemoteFile,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(file.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (file.isDirectory) "папка" else humanSize(file.size),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!file.isDirectory) {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun humanSize(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> "%.1f GB".format(bytes / gb)
        bytes >= mb -> "%.1f MB".format(bytes / mb)
        bytes >= kb -> "%.1f KB".format(bytes / kb)
        else -> "$bytes B"
    }
}
