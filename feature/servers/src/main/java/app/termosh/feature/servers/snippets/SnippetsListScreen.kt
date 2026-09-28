package app.termosh.feature.servers.snippets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.termosh.core.ui.component.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetsListScreen(
    onBack: () -> Unit,
    viewModel: SnippetsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.showEditDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismiss() },
            title = { Text(if (state.editId.isBlank()) "Новый сниппет" else "Изменить сниппет") },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.editName,
                        onValueChange = viewModel::setName,
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.editCommand,
                        onValueChange = viewModel::setCommand,
                        label = { Text("Команда") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.save() },
                    enabled = !state.saving &&
                        state.editName.isNotBlank() &&
                        state.editCommand.isNotBlank(),
                ) { Text(if (state.saving) "..." else "Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismiss() }) { Text("Отмена") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сниппеты") },
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
        when {
            state.loading -> CircularProgressIndicator(modifier = Modifier.fillMaxSize().padding(padding))
            state.snippets.isEmpty() -> EmptyState(
                title = "Сниппетов нет",
                subtitle = "Нажми + чтобы добавить",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.snippets, key = { it.id }) { sn ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        onClick = { viewModel.openEdit(sn) },
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(sn.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                sn.command,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
