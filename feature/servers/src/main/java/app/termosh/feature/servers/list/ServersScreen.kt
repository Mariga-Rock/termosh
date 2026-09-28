package app.termosh.feature.servers.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.termosh.core.ui.component.EmptyState
import app.termosh.core.ui.component.ProFeatureDialog
import app.termosh.core.ui.component.TermoshLoader
import app.termosh.domain.model.Server
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.layout.size

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    onAddServer: () -> Unit,
    onEditServer: (String) -> Unit,
    onConnectServer: (String) -> Unit,
    onOpenKeys: () -> Unit,
    onOpenSnippets: () -> Unit,
    onOpenForwards: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLicense: () -> Unit,
    onOpenTotp: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenSftp: (String) -> Unit,
    viewModel: ServersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var proFeaturePrompt by remember { mutableStateOf<String?>(null) }

    if (state.showLimitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLimitDialog() },
            title = { Text("Достигнут лимит Free-версии") },
            text = {
                Text(
                    "В бесплатной версии можно хранить до ${state.freeLimit} серверов. " +
                        "У вас уже ${state.totalCount}.\n\nАктивируйте лицензию, чтобы снять лимит."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissLimitDialog()
                    onOpenLicense()
                }) { Text("Активировать") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLimitDialog() }) { Text("Позже") }
            },
        )
    }

    proFeaturePrompt?.let { feature ->
        ProFeatureDialog(
            featureName = feature,
            onDismiss = { proFeaturePrompt = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isPro) "Servers"
                        else "Servers (${state.totalCount}/${state.freeLimit})",
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            SectionHeader("Сортировка")
                            DropdownMenuItem(
                                text = { Text(if (state.sort == ServerSort.MANUAL) "\u2713 Вручную" else "Вручную") },
                                onClick = { viewModel.setSort(ServerSort.MANUAL); menuOpen = false },
                            )
                            DropdownMenuItem(
                                text = { Text(if (state.sort == ServerSort.NAME) "\u2713 По имени" else "По имени") },
                                onClick = { viewModel.setSort(ServerSort.NAME); menuOpen = false },
                            )
                            DropdownMenuItem(
                                text = { Text(if (state.sort == ServerSort.LAST_USED) "\u2713 По последнему" else "По последнему") },
                                onClick = { viewModel.setSort(ServerSort.LAST_USED); menuOpen = false },
                            )
                            DropdownMenuItem(
                                text = { Text(if (state.sort == ServerSort.CREATED) "\u2713 По созданию" else "По созданию") },
                                onClick = { viewModel.setSort(ServerSort.CREATED); menuOpen = false },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Сниппеты") },
                                leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
                                onClick = { menuOpen = false; onOpenSnippets() },
                            )
                            DropdownMenuItem(
                                text = { Text("SSH-ключи") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                onClick = { menuOpen = false; onOpenKeys() },
                            )
                            DropdownMenuItem(
                                text = { Text("TOTP / 2FA") },
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                                onClick = { menuOpen = false; onOpenTotp() },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Импорт из OpenSSH") },
                                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                onClick = { menuOpen = false; onOpenImport() },
                            )
                            DropdownMenuItem(
                                text = { Text("Экспорт в OpenSSH") },
                                leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                                onClick = { menuOpen = false; onOpenExport() },
                            )
                            DropdownMenuItem(
                                text = { Text("Настройки") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = { menuOpen = false; onOpenSettings() },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddClicked(onAddServer) },
                containerColor = if (state.canAdd) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить сервер")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Поиск по имени, хосту, тегам") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when {
                state.loading -> TermoshLoader()
                state.servers.isEmpty() && state.query.isBlank() -> FirstRunWelcome(
                    onImport = onOpenImport,
                    onAddManual = onAddServer,
                )
                state.servers.isEmpty() -> EmptyState(
                    title = "Ничего не найдено",
                    subtitle = "Попробуй другой запрос",
                )
                else -> {
                    val items = remember(state.servers) {
                        mutableStateListOf<Server>().also { it.addAll(state.servers) }
                    }
                    if (items.map { it.id } != state.servers.map { it.id }) {
                        items.clear()
                        items.addAll(state.servers)
                    }
                    ReorderableServersList(
                        items = items,
                        forwardsByServer = state.forwardsByServer,
                        isPro = state.isPro,
                        onProRequired = { feature -> proFeaturePrompt = feature },
                        reorderEnabled = state.reorderEnabled,
                        onReorder = { viewModel.onReorder(it.toList()) },
                        onConnect = { onConnectServer(it.id) },
                        onEdit = { onEditServer(it.id) },
                        onForwards = { onOpenForwards(it.id) },
                        onSftp = { onOpenSftp(it.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ReorderableServersList(
    items: SnapshotStateList<Server>,
    forwardsByServer: Map<String, Boolean>,
    isPro: Boolean,
    onProRequired: (String) -> Unit,
    reorderEnabled: Boolean,
    onReorder: (List<Server>) -> Unit,
    onConnect: (Server) -> Unit,
    onEdit: (Server) -> Unit,
    onForwards: (Server) -> Unit,
    onSftp: (Server) -> Unit,
) {
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        items.add(to.index, items.removeAt(from.index))
        onReorder(items.toList())
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { server ->
            ReorderableItem(reorderState, key = server.id) { isDragging ->
                ServerRow(
                    server = server,
                    hasForwards = forwardsByServer[server.id] == true,
                    isPro = isPro,
                    onProRequired = onProRequired,
                    isDragging = isDragging,
                    showDragHandle = reorderEnabled,
                    dragHandle = {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.draggableHandle(),
                        ) {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = "Reorder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onConnect = { onConnect(server) },
                    onEdit = { onEdit(server) },
                    onForwards = { onForwards(server) },
                    onSftp = { onSftp(server) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerRow(
    server: Server,
    hasForwards: Boolean,
    isPro: Boolean,
    onProRequired: (String) -> Unit,
    isDragging: Boolean,
    showDragHandle: Boolean,
    dragHandle: @Composable () -> Unit,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onForwards: () -> Unit,
    onSftp: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onConnect, onLongClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showDragHandle) dragHandle()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (showDragHandle) 4.dp else 0.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (hasForwards) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Активные туннели",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp).padding(start = 6.dp),
                        )
                    }
                }
                Text(
                    text = "${server.username}@${server.host}:${server.port}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                if (server.tags.isNotEmpty()) {
                    Text(
                        text = server.tags.joinToString(" ") { "#$it" },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Действия",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Редактировать") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text("SFTP") },
                        leadingIcon = {
                            if (isPro) Icon(Icons.Default.Folder, contentDescription = null)
                            else Icon(Icons.Default.Lock, contentDescription = "Pro")
                        },
                        onClick = {
                            menuOpen = false
                            if (isPro) onSftp() else onProRequired("SFTP")
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Туннели") },
                        leadingIcon = {
                            if (isPro) Icon(Icons.Default.Settings, contentDescription = null)
                            else Icon(Icons.Default.Lock, contentDescription = "Pro")
                        },
                        onClick = {
                            menuOpen = false
                            if (isPro) onForwards() else onProRequired("Туннели")
                        },
                    )
                }
            }
        }
    }
}
