package app.termosh.feature.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.termosh.core.terminal.TerminalView
import kotlin.math.abs
import androidx.compose.ui.platform.LocalContext
import app.termosh.core.common.BuildFlags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onBack: () -> Unit,
    viewModel: TerminalTabsViewModel = hiltViewModel(),
) {
    val __ctx = androidx.compose.ui.platform.LocalContext.current
    val __onDoubleTap: () -> Unit = {
        val __cm = __ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val __t = __cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
        if (!__t.isNullOrEmpty()) viewModel.sendBracketedPaste(__t)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val inputFocus = remember { FocusRequester() }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showTmux by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var topMenu by remember { mutableStateOf(false) }

    if (state.showTabLimitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTabLimitDialog() },
            title = { Text("Лимит Free-версии") },
            text = {
                Text(
                    "В бесплатной версии можно открыть до ${state.freeTabLimit} вкладок. " +
                        "Активируйте лицензию, чтобы снять лимит.",
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissTabLimitDialog() }) { Text("OK") }
            },
        )
    }

    if (state.showPicker) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPicker() },
            title = { Text("Открыть сервер") },
            text = {
                if (state.availableServers.isEmpty()) {
                    Text("Нет серверов")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(state.availableServers, key = { it.id }) { s ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.openTabForServer(s.id) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                            ) {
                                Text(s.name, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    "${s.username}@${s.host}:${s.port}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.dismissPicker() }) { Text("Закрыть") } },
        )
    }

    if (state.showSnippets) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSnippets() },
            title = { Text("Сниппеты") },
            text = {
                if (state.snippets.isEmpty()) {
                    Text("Сниппетов нет")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(state.snippets, key = { it.id }) { sn ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.sendSnippet(sn.command) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                            ) {
                                Text(sn.name, color = MaterialTheme.colorScheme.onSurface)
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
            },
            confirmButton = { TextButton(onClick = { viewModel.dismissSnippets() }) { Text("Закрыть") } },
        )
    }

    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Скопировать весь вывод") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF7AA2F7)) },
                            onClick = {
                                clipboard.setText(AnnotatedString(viewModel.getAllOutput()))
                                showMenu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Вставить из буфера") },
                            leadingIcon = { Icon(Icons.Default.ContentPaste, null, tint = Color(0xFF7AA2F7)) },
                            onClick = {
                                val txt = clipboard.getText()?.text
                                if (!txt.isNullOrEmpty()) viewModel.sendBracketedPaste(txt)
                                showMenu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Очистить экран") },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, null, tint = Color(0xFF7AA2F7)) },
                            onClick = {
                                viewModel.clearActiveBuffer()
                                showMenu = false
                            },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(if (showSearch) "Скрыть поиск" else "Поиск") },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF7AA2F7)) },
                            onClick = {
                                showSearch = !showSearch
                                if (!showSearch) searchQuery = ""
                                showMenu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (state.splitTabId != null) "Убрать Split view" else "Split view") },
                            leadingIcon = { Icon(
                                if (state.splitTabId != null) Icons.Default.Close else Icons.Default.VerticalSplit,
                                null,
                                tint = Color(0xFF7AA2F7),
                            ) },
                            onClick = {
                                viewModel.toggleSplit()
                                showMenu = false
                            },
                        )
                        if (state.splitTabId != null) {
                            DropdownMenuItem(
                                text = { Text("Поменять местами") },
                                leadingIcon = { Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFF7AA2F7)) },
                                onClick = {
                                    viewModel.swapSplit()
                                    showMenu = false
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (showTmux) "Скрыть tmux" else "Показать tmux") },
                            leadingIcon = { Icon(Icons.Default.GridView, null, tint = Color(0xFF7AA2F7)) },
                            onClick = {
                                showTmux = !showTmux
                                showMenu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Сниппеты") },
                            leadingIcon = { Icon(Icons.Default.Code, null, tint = Color(0xFF7AA2F7)) },
                            onClick = {
                                viewModel.openSnippets()
                                showMenu = false
                            },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Отключиться", color = Color(0xFFF7768E)) },
                            leadingIcon = { Icon(Icons.Default.PowerSettingsNew, null, tint = Color(0xFFF7768E)) },
                            onClick = {
                                viewModel.disconnectActive()
                                showMenu = false
                            },
                        )
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
                .onPreviewKeyEvent { ev ->
                    if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val seq = KeyMapper.map(ev)
                    if (seq != null) {
                        viewModel.sendText(seq)
                        true
                    } else false
                },
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF16161E)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(state.tabs, key = { it.tabId }) { tab ->
                    val active = tab.tabId == state.activeTabId
                    val __tabStatus = state.tabStatuses[tab.tabId]
                    val tabCs = __tabStatus?.state ?: TerminalConnectionState.DISCONNECTED
                    val dot = when {
                        tabCs == TerminalConnectionState.CONNECTED -> Color(0xFF9ECE6A)
                        tabCs == TerminalConnectionState.CONNECTING -> Color(0xFFE0AF68)
                        tabCs == TerminalConnectionState.RECONNECTING -> Color(0xFFFF9E64)
                        tabCs == TerminalConnectionState.ERROR -> Color(0xFFF7768E)
                        else -> Color(0xFF565F89)
                    }
                    Column(
                        modifier = Modifier
                            .background(if (active) Color(0xFF1F2335) else Color(0xFF16161E))
                            .clickable { viewModel.selectTab(tab.tabId) },
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(dot, CircleShape),
                            )
                            Text(
                                text = tab.name,
                                color = if (active) Color(0xFF7AA2F7) else Color(0xFFA9B1D6),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                            IconButton(onClick = { viewModel.closeTab(tab.tabId) }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF565F89))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(if (active) Color(0xFF7AA2F7) else Color.Transparent),
                        )
                    }
                }
                item {
                    IconButton(onClick = { viewModel.openPicker() }) {
                        Icon(Icons.Default.Add, contentDescription = "New tab", tint = Color(0xFF7AA2F7))
                    }
                }
            }

            if (showSearch) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Поиск") },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showSearch = false; searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            val tabs = state.tabs
            val activeIdx = tabs.indexOfFirst { it.tabId == state.activeTabId }
            var dragAccum by remember { mutableStateOf(0f) }
            val mainModifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(tabs, activeIdx, state.splitTabId) {
                    if (state.splitTabId == null) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccum = 0f },
                            onDragEnd = {
                                if (abs(dragAccum) > 200f) {
                                    if (dragAccum > 0 && activeIdx > 0) {
                                        viewModel.selectTab(tabs[activeIdx - 1].tabId)
                                    } else if (dragAccum < 0 && activeIdx < tabs.size - 1) {
                                        viewModel.selectTab(tabs[activeIdx + 1].tabId)
                                    }
                                }
                            },
                        ) { _, delta -> dragAccum += delta }
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { runCatching { inputFocus.requestFocus() } }

            val splitId = state.splitTabId
            if (splitId != null && state.activeTabId != null) {
                val bufferA = viewModel.bufferFor(state.activeTabId!!)
                val bufferB = viewModel.bufferFor(splitId)
                if (bufferA != null && bufferB != null) {
                    val paneA = state.splitActivePane == 0
                    val borderColor = MaterialTheme.colorScheme.primary

                    Column(mainModifier) {
                        Box(
                            Modifier
                                .weight(1f)
                                .border(
                                    width = if (paneA) 2.dp else 0.dp,
                                    color = if (paneA) borderColor else Color.Transparent,
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    viewModel.setSplitActivePane(0)
                                    runCatching { inputFocus.requestFocus() }
                                },
                        ) {
                            TerminalView(buffer = bufferA, searchQuery = searchQuery, modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectDoubleTapOnly { __onDoubleTap() } })
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .border(
                                    width = if (!paneA) 2.dp else 0.dp,
                                    color = if (!paneA) borderColor else Color.Transparent,
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    viewModel.setSplitActivePane(1)
                                    runCatching { inputFocus.requestFocus() }
                                },
                        ) {
                            TerminalView(buffer = bufferB, searchQuery = searchQuery, modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectDoubleTapOnly { __onDoubleTap() } })
                        }
                    }
                }
            } else {
                val activeBuffer = state.activeTabId?.let { viewModel.bufferFor(it) }
                if (activeBuffer != null) {
                    TerminalView(buffer = activeBuffer, searchQuery = searchQuery, modifier = mainModifier.pointerInput(Unit) { detectDoubleTapOnly { __onDoubleTap() } })
                } else {
                    Text(
                        "No active session. Tap + to open.",
                        color = MaterialTheme.colorScheme.outline,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                    )
                }
            }

            if (showTmux) {
                TmuxPanel(onCommand = { cmd -> viewModel.sendText(cmd) })
            }

            KeyboardBar(
                modifiers = state.modifiers,
                onKey = { viewModel.sendRawKey(it) },
                onToggleModifier = { viewModel.toggleModifier(it) },
                onHistoryPrev = { viewModel.historyPrev() },
                onHistoryNext = { viewModel.historyNext() },
                onCopyResponse = { viewModel.copyLastResponseToClipboard() },
                showPersonalKeys = BuildFlags.isPersonal,
            )

            InstantInput(
                enabled = state.connectionState == TerminalConnectionState.CONNECTED,
                pendingInput = state.pendingInput,
                onPendingConsumed = { viewModel.consumePendingInput() },
                onChar = { viewModel.sendText(it) },
                onBackspace = { viewModel.sendBackspace() },
                onEnter = { viewModel.sendEnter() },
                onSubmit = { text ->
                    viewModel.submitCommand(text)
                },
                onPaste = { text -> viewModel.sendBracketedPaste(text) },
                focusRequester = inputFocus,
            )
        }
    }
}

@Composable
private fun InstantInput(
    enabled: Boolean,
    pendingInput: String? = null,
    onPendingConsumed: () -> Unit = {},
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSubmit: (String) -> Unit,
    onPaste: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    var value by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(pendingInput) {
        if (pendingInput != null) {
            value = TextFieldValue(pendingInput)
            onPendingConsumed()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF16161E))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = { newValue -> value = newValue },
            enabled = enabled,
            textStyle = TextStyle(
                color = Color(0xFFC0CAF5),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(Color(0xFF7AA2F7)),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (value.text.isNotEmpty()) {
                    onSubmit(value.text)
                    value = TextFieldValue("")
                }
            }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .background(Color(0xFF1A1B26))
                .padding(8.dp),
        )
        IconButton(onClick = {
            if (value.text.isNotEmpty()) {
                onSubmit(value.text)
                value = TextFieldValue("")
            }
        }, enabled = enabled) {
            Icon(Icons.Default.Send, contentDescription = "Enter", tint = Color(0xFF7AA2F7))
        }
    }
}
