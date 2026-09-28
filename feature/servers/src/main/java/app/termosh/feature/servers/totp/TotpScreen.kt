package app.termosh.feature.servers.totp

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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.termosh.core.ui.component.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotpScreen(
    onBack: () -> Unit,
    viewModel: TotpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    if (state.showEditDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismiss() },
            title = { Text(if (state.editId.isBlank()) "Новый TOTP" else "Изменить TOTP") },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.editLabel,
                        onValueChange = viewModel::setLabel,
                        label = { Text("Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.editIssuer,
                        onValueChange = viewModel::setIssuer,
                        label = { Text("Issuer (опционально)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    OutlinedTextField(
                        value = state.editSecret,
                        onValueChange = viewModel::setSecret,
                        label = { Text("Base32 secret") },
                        singleLine = false,
                        minLines = 2,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.save() }) { Text("Сохранить") } },
            dismissButton = { TextButton(onClick = { viewModel.dismiss() }) { Text("Отмена") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TOTP / 2FA") },
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
            FloatingActionButton(onClick = { viewModel.openNew() }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
    ) { padding ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color(0x33E0AF68),
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color(0xFFE0AF68),
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            "Раздел в разработке",
                            style = MaterialTheme.typography.titleSmall,
                            color = androidx.compose.ui.graphics.Color(0xFFE0AF68),
                        )
                        Text(
                            "Секреты хранятся и коды генерируются, но автоматическая подстановка TOTP при SSH-входе пока не подключена.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

        when {
            state.loading -> CircularProgressIndicator(modifier = Modifier.fillMaxSize().padding(padding))
            state.items.isEmpty() -> EmptyState(
                title = "Нет TOTP-секретов",
                subtitle = "Нажми + чтобы добавить base32-секрет",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            clipboard.setText(AnnotatedString(item.currentCode))
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.label, style = MaterialTheme.typography.titleMedium)
                                if (item.issuer.isNotBlank()) {
                                    Text(
                                        item.issuer,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    item.currentCode.chunked(3).joinToString(" "),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 24.sp,
                                    color = if (item.secondsLeft <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "${item.secondsLeft}с",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { viewModel.delete(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
