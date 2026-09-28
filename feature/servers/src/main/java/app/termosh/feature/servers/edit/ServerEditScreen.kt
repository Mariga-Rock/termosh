package app.termosh.feature.servers.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerEditScreen(
    onBack: () -> Unit,
    viewModel: ServerEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New server" else "Edit server") },
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
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Field("Name", state.name, viewModel::setName)
            Field("Host", state.host, viewModel::setHost, mono = true)
            Field("Port", state.port, viewModel::setPort, keyboard = KeyboardType.Number, mono = true)
            Field("Username", state.username, viewModel::setUsername, mono = true)
            Field("Tags (через запятую)", state.tags, viewModel::setTags)

            Text("ProxyJump (бастион)", style = MaterialTheme.typography.titleSmall)
            ProxyJumpDropdown(
                servers = state.availableServers.map { it.id to it.name },
                selectedId = state.proxyJumpId,
                onSelect = viewModel::setProxyJump,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Использовать mosh")
                    Text(
                        "Поддержка roaming: сессия не рвётся при смене сети",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.useMosh, onCheckedChange = viewModel::setUseMosh)
            }

            if (state.useMosh) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Постоянные сессии (tmux)")
                        Text(
                            "Сессия живёт на сервере, переживает закрытие приложения",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.useTmux, onCheckedChange = viewModel::setUseTmux)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Использовать учётку jump-хоста")
                    Text(
                        "Применять логин/пароль bastion для целевого хоста",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.useJumpCredentials,
                    onCheckedChange = viewModel::setUseJumpCredentials,
                )
            }

            Text("TOTP / 2FA", style = MaterialTheme.typography.titleSmall)
            TotpDropdown(
                items = state.availableTotp.map { it.id to it.label },
                selectedId = state.totpSecretId,
                onSelect = viewModel::setTotpSecret,
            )

            Text("Стартовая сессия", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = state.startupCommandsText,
                onValueChange = viewModel::setStartupCommandsText,
                label = { Text("Команды (одна на строку)") },
                minLines = 3,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.envVarsText,
                onValueChange = viewModel::setEnvVarsText,
                label = { Text("Переменные (VAR=value)") },
                minLines = 2,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.envSecretsText,
                onValueChange = viewModel::setEnvSecretsText,
                label = { Text("Секретные переменные (шифруются)") },
                minLines = 2,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Authentication", style = MaterialTheme.typography.titleSmall)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.authMode == AuthMode.PASSWORD,
                    onClick = { viewModel.setAuthMode(AuthMode.PASSWORD) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Password") }
                SegmentedButton(
                    selected = state.authMode == AuthMode.KEY,
                    onClick = { viewModel.setAuthMode(AuthMode.KEY) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Key") }
            }

            when (state.authMode) {
                AuthMode.PASSWORD -> Field(
                    "Password",
                    state.password,
                    viewModel::setPassword,
                    keyboard = KeyboardType.Password,
                    isPassword = true,
                    mono = true,
                )
                AuthMode.KEY -> KeyMultiSelect(
                    keys = state.availableKeys.map { it.id to (it.comment ?: it.type.name) },
                    selectedIds = state.selectedKeyIds,
                    onToggle = viewModel::toggleKey,
                )
            }

            val err = state.error
            if (err != null) {
                Text(err, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::save,
                enabled = state.canSave && !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.saving) "Saving..." else "Save")
            }

            if (!state.isNew) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.outline,
                )
                var showDeleteConfirm by remember { mutableStateOf(false) }
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(
                        "Удалить сервер",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("Удалить сервер?") },
                        text = { Text("Профиль \"${state.name}\" будет удалён. Отменить нельзя.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showDeleteConfirm = false
                                viewModel.delete()
                            }) {
                                Text("Удалить", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    mono: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = if (mono) {
            MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
        } else MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProxyJumpDropdown(
    servers: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = if (selectedId == null) "Не использовать"
        else servers.firstOrNull { it.first == selectedId }?.second ?: "Не использовать"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Jump host") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Не использовать") },
                onClick = { onSelect(null); expanded = false },
            )
            servers.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { onSelect(id); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TotpDropdown(
    items: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (selectedId == null) "Не использовать"
        else items.firstOrNull { it.first == selectedId }?.second ?: "Не использовать"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("TOTP-секрет") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Не использовать") },
                onClick = { onSelect(null); expanded = false },
            )
            items.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { onSelect(id); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun KeyMultiSelect(
    keys: List<Pair<String, String>>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
) {
    Column {
        Text("Ключи (порядок выбора = приоритет)", style = MaterialTheme.typography.labelMedium)
        if (keys.isEmpty()) {
            Text(
                "Нет ключей. Создайте в разделе Keys.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            keys.forEach { (id, label) ->
                val idx = selectedIds.indexOf(id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(id) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = idx >= 0, onCheckedChange = { onToggle(id) })
                    Text(
                        text = if (idx >= 0) "${idx + 1}. $label" else label,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
