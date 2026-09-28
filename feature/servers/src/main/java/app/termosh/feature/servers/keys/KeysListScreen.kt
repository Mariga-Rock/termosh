package app.termosh.feature.servers.keys

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.termosh.core.ui.component.EmptyState
import app.termosh.domain.model.SshKeyType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeysListScreen(
    onBack: () -> Unit,
    viewModel: KeysViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.showGenerateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissGenerate() },
            title = { Text("Новый SSH-ключ") },
            text = {
                Column {
                    Text("Тип", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        SshKeyType.values().forEach { t ->
                            FilterChip(
                                selected = state.generateType == t,
                                onClick = { viewModel.setType(t) },
                                label = { Text(t.name) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.generateComment,
                        onValueChange = viewModel::setComment,
                        label = { Text("Комментарий") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val err = state.error
                    if (err != null) {
                        Text(
                            err,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.generate() },
                    enabled = !state.generating,
                ) { Text(if (state.generating) "Генерация..." else "Создать") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissGenerate() }) { Text("Отмена") }
            },
        )
    }

    state.showPublicKeyFor?.let { key ->
        var keyMenu by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { viewModel.hidePublic() },
            title = { Text("Публичный ключ") },
            text = {
                Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        key.publicKeyOpenSsh,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                }
            },
            confirmButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { viewModel.copyToClipboard(key.publicKeyOpenSsh) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Копировать")
                    }
                    Box {
                        IconButton(onClick = { keyMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Ещё")
                        }
                        DropdownMenu(expanded = keyMenu, onDismissRequest = { keyMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    viewModel.delete(key.id)
                                    viewModel.hidePublic()
                                    keyMenu = false
                                },
                            )
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hidePublic() }) { Text("Закрыть") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SSH-ключи") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openGenerate() }) {
                Icon(Icons.Default.Add, contentDescription = "Generate")
            }
        },
    ) { padding ->
        when {
            state.loading -> CircularProgressIndicator(modifier = Modifier.fillMaxSize().padding(padding))
            state.keys.isEmpty() -> EmptyState(
                icon = Icons.Default.VpnKey,
                title = "SSH-ключей нет",
                subtitle = "Сгенерируйте новый ключ (Ed25519, RSA, ECDSA) или " +
                    "импортируйте существующий. Приватная часть шифруется Keystore.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.keys, key = { it.id }) { key ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        onClick = { viewModel.showPublic(key) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    key.comment ?: key.type.name,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    key.fingerprintSha256,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { viewModel.delete(key.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
