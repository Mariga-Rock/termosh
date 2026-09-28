package app.termosh.feature.settings.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val createConfig = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { viewModel.exportTo(it) } }

    val createVault = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> uri?.let { viewModel.exportTo(it) } }

    val openDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.importFrom(it) } }

    if (state.showExportPassword) {
        val withSecrets = state.exportMode == ExportMode.FULL_BACKUP
        AlertDialog(
            onDismissRequest = { if (!state.busy) viewModel.dismiss() },
            title = {
                Text(if (withSecrets) "Полный бэкап" else "Экспорт конфига")
            },
            text = {
                Column {
                    Text(
                        if (withSecrets) {
                            "Файл будет зашифрован паролем. Внутри — все серверы, пароли, " +
                                "SSH-ключи и секретные переменные. Передавайте только себе на другое устройство."
                        } else {
                            "Открытый JSON без паролей, ключей и TOTP-секретов. " +
                                "Можно безопасно передавать коллегам."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (withSecrets) {
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = viewModel::setPassword,
                            label = { Text("Пароль") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                        OutlinedTextField(
                            value = state.passwordConfirm,
                            onValueChange = viewModel::setPasswordConfirm,
                            label = { Text("Повторите пароль") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                    state.error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                            .format(java.util.Date())
                        if (withSecrets) createVault.launch("termosh-$ts.termoshvault")
                        else createConfig.launch("termosh-$ts.termosh")
                    },
                    enabled = !state.busy,
                ) { Text(if (state.busy) "..." else "Продолжить") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismiss() }, enabled = !state.busy) { Text("Отмена") }
            },
        )
    }

    if (state.showImportPassword) {
        AlertDialog(
            onDismissRequest = { if (!state.busy) viewModel.dismiss() },
            title = { Text("Импорт") },
            text = {
                Column {
                    Text(
                        "Если файл .termoshvault — укажите пароль. " +
                            "Для .termosh (конфиг без секретов) пароль не нужен.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::setPassword,
                        label = { Text("Пароль (если файл зашифрован)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    state.error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { openDoc.launch(arrayOf("*/*")) },
                    enabled = !state.busy,
                ) { Text(if (state.busy) "..." else "Выбрать файл") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismiss() }, enabled = !state.busy) { Text("Отмена") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Резервная копия") },
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
                .imePadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Экспорт", style = MaterialTheme.typography.titleMedium)

            Text(
                "Конфиг без секретов — открытый JSON с серверами, тегами, сниппетами и настройками. " +
                    "Пароли и ключи не входят. Можно передавать коллегам.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { viewModel.openExportConfig() },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Экспорт .termosh (без секретов)") }

            Text(
                "Полный бэкап — зашифрованный файл со всеми серверами, паролями, SSH-ключами, " +
                    "TOTP и секретными переменными. Только для переноса на своё другое устройство.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { viewModel.openExportFull() },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Полный бэкап .termoshvault") }

            Text("Импорт", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { viewModel.openImport() },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Импорт из файла") }

            val st = state.status
            if (st.isNotBlank()) {
                Text(
                    st,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { viewModel.clearStatus() }) { Text("Очистить") }
            }
            val err = state.error
            if (err != null && !state.showExportPassword && !state.showImportPassword) {
                Text(err, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
