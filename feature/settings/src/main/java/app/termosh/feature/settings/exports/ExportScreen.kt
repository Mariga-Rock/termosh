package app.termosh.feature.settings.exports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val createSshConfig = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri -> uri?.let { viewModel.exportOpenSshConfig(it) } }

    val openXml = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.importConnectBot(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Экспорт / импорт форматов") },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Экспорт в OpenSSH", style = MaterialTheme.typography.titleMedium)
            Text(
                "Генерирует текст ~/.ssh/config. Пароли не выгружаются, ключи указываются как пути — " +
                    "файлы ключей вы перенесёте сами.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { createSshConfig.launch("termosh_ssh_config") },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Сохранить ~/.ssh/config") }

            Text("Импорт из ConnectBot", style = MaterialTheme.typography.titleMedium)
            Text(
                "XML-экспорт ConnectBot. Пароли импортируются, если были сохранены.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { openXml.launch(arrayOf("text/xml", "application/xml", "*/*")) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Импортировать XML ConnectBot") }

            if (state.status.isNotBlank()) {
                Text(state.status, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { viewModel.clear() }) { Text("Очистить") }
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.clear() }) { Text("OK") }
            }
        }
    }
}
