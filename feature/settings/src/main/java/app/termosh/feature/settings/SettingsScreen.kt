package app.termosh.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.termosh.core.ui.theme.TermoshThemeOption
import app.termosh.core.ui.component.ProFeatureDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenLicense: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenExports: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var proFeaturePrompt by remember { mutableStateOf<String?>(null) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    proFeaturePrompt?.let { name ->
        ProFeatureDialog(
            featureName = name,
            onDismiss = { proFeaturePrompt = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
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
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            // === Тема ===
            SectionTitle("Внешний вид")
            TermoshThemeOption.values().forEach { option ->
                val locked = !state.isPro && option != TermoshThemeOption.TOKYO_NIGHT
                ThemeRow(
                    label = option.displayName,
                    value = option,
                    selected = state.theme,
                    locked = locked,
                    onSelect = { if (!locked) viewModel.setTheme(it) },
                    onLockedClick = { proFeaturePrompt = "Тема ${option.displayName}" },
                )
            }

            SectionDivider()

            // === Логи ===
            SectionTitle("Диагностика")
            SettingsToggle(
                icon = Icons.Default.Article,
                title = "Логирование сессий",
                subtitle = "Каждая сессия пишется в файл",
                checked = state.logSessions,
                onChange = viewModel::setLogSessions,
            )
            SettingsToggle(
                icon = Icons.Default.Bolt,
                title = "Уведомлять о завершении команд",
                subtitle = "Когда команда закончилась, а приложение свёрнуто",
                checked = state.notifyOnFinish,
                onChange = viewModel::setNotifyOnFinish,
            )
            SettingsItem(
                icon = Icons.Default.Article,
                title = "Открыть логи",
                subtitle = "Просмотр termosh.log и сессий",
                onClick = onOpenLogs,
            )

            SectionDivider()

            // === Лицензия ===
            SectionTitle("Лицензия")
            SettingsItem(
                icon = Icons.Default.Verified,
                title = "Активация",
                subtitle = "Owner Grant или Pro-код",
                onClick = onOpenLicense,
            )

            SectionDivider()

            // === Резервная копия ===
            SectionTitle("Данные")
            SettingsItem(
                icon = Icons.Default.CloudUpload,
                title = "Резервная копия",
                subtitle = "Экспорт / импорт .termosh",
                onClick = onOpenBackup,
            )

            SectionDivider()

            SettingsItem(
                icon = Icons.Default.CloudUpload,
                title = "Экспорт / импорт форматов",
                subtitle = "OpenSSH config, ConnectBot",
                onClick = onOpenExports,
            )

            // Отступ снизу
            Row(modifier = Modifier.padding(24.dp)) {}
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ThemeRow(
    label: String,
    value: TermoshThemeOption,
    selected: TermoshThemeOption,
    locked: Boolean = false,
    onSelect: (TermoshThemeOption) -> Unit,
    onLockedClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = !locked && value == selected,
                onClick = { if (locked) onLockedClick() else onSelect(value) },
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (locked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Pro",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                RadioButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                )
            }
        }
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            color = if (locked) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
        )
    }
}
