package app.termosh.feature.servers.sftp

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.termosh.core.security.TotpGenerator
import app.termosh.core.ssh.RemoteFile
import app.termosh.core.ssh.SftpManager
import app.termosh.core.ssh.model.SshAuthMethod
import app.termosh.core.ssh.model.SshConnectionConfig
import app.termosh.domain.model.Server
import app.termosh.domain.model.ServerAuth
import app.termosh.domain.repository.SshKeyRepository
import app.termosh.domain.repository.TotpRepository
import app.termosh.domain.repository.SecretCodec
import app.termosh.domain.usecase.server.GetServerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SftpUiState(
    val serverName: String = "",
    val currentPath: String = "/",
    val files: List<RemoteFile> = emptyList(),
    val loading: Boolean = false,
    val status: String = "",
    val error: String? = null,
    val showNewDirDialog: Boolean = false,
    val newDirName: String = "",
    val showRenameDialog: Boolean = false,
    val renameTarget: RemoteFile? = null,
    val renameNewName: String = "",
)

@HiltViewModel
class SftpViewModel @Inject constructor(
    savedState: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val getServer: GetServerUseCase,
    private val codec: SecretCodec,
    private val keyRepository: SshKeyRepository,
    private val totpRepository: TotpRepository,
    private val totpGenerator: TotpGenerator,
    private val sftp: SftpManager,
) : ViewModel() {

    private val serverId: String = savedState.get<String>("serverId") ?: ""

    private val _state = MutableStateFlow(SftpUiState())
    val state: StateFlow<SftpUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { connect() }
    }

    private suspend fun connect() {
        _state.value = _state.value.copy(loading = true, error = null, status = "Подключение...")
        try {
            val server = getServer(serverId)
            val config = SshConnectionConfig(host = server.host, port = server.port, username = server.username)
            val auth = authOf(server)
            sftp.connect(config, auth)
            _state.value = _state.value.copy(serverName = server.name, status = "OK", loading = false)
            navigate("/")
        } catch (t: Throwable) {
            _state.value = _state.value.copy(
                loading = false,
                error = "Connection failed: ${t.message ?: t.javaClass.simpleName}",
                status = "",
            )
        }
    }

    private suspend fun authOf(server: Server): SshAuthMethod = when (val a = server.auth) {
        is ServerAuth.Password -> SshAuthMethod.Password(codec.decryptString(a.secretRef).toCharArray())
        is ServerAuth.Keys -> {
            val keys = keyRepository.getMany(a.keyIds)
            SshAuthMethod.PublicKeys(
                keys = keys.map {
                    SshAuthMethod.KeyMaterial(codec.decrypt(it.privateKeyRef), it.publicKeyOpenSsh)
                }
            )
        }
    }

    fun navigate(path: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, currentPath = path)
            try {
                val files = sftp.list(path)
                _state.value = _state.value.copy(files = files, loading = false)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(loading = false, error = t.message ?: "ls failed")
            }
        }
    }

    fun openItem(file: RemoteFile) {
        if (file.isDirectory) navigate(file.path)
        else {
            // Одиночный файл — просто подсветить
        }
    }

    fun goUp() {
        val cur = _state.value.currentPath
        if (cur == "/" || cur.isEmpty()) return
        val parent = cur.trimEnd('/').substringBeforeLast('/').ifEmpty { "/" }
        navigate(parent)
    }

    fun startDownload(file: RemoteFile, uri: Uri) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(status = "Скачивание ${file.name}...")
                withContext(Dispatchers.IO) {
                    val out = context.contentResolver.openOutputStream(uri) ?: error("Не открыть файл")
                    out.use { sftp.download(file.path, it) }
                }
                _state.value = _state.value.copy(status = "Скачано: ${file.name}")
            } catch (t: Throwable) {
                _state.value = _state.value.copy(error = "Download: ${t.message}")
            }
        }
    }

    fun startUpload(source: Uri) {
        viewModelScope.launch {
            try {
                val name = queryDisplayName(source) ?: "upload_${System.currentTimeMillis()}"
                val remotePath = _state.value.currentPath.trimEnd('/') + "/" + name
                _state.value = _state.value.copy(status = "Загрузка $name...")
                withContext(Dispatchers.IO) {
                    val inp = context.contentResolver.openInputStream(source) ?: error("Не открыть источник")
                    inp.use { sftp.upload(it, remotePath) }
                }
                _state.value = _state.value.copy(status = "Загружено: $name")
                navigate(_state.value.currentPath)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(error = "Upload: ${t.message}")
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
        return cursor.use {
            val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && idx >= 0) it.getString(idx) else null
        }
    }

    fun startDelete(file: RemoteFile) {
        viewModelScope.launch {
            try {
                if (file.isDirectory) sftp.deleteDir(file.path) else sftp.deleteFile(file.path)
                _state.value = _state.value.copy(status = "Удалено: ${file.name}")
                navigate(_state.value.currentPath)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(error = "Delete: ${t.message}")
            }
        }
    }

    fun openNewDirDialog() { _state.value = _state.value.copy(showNewDirDialog = true, newDirName = "") }
    fun dismissNewDirDialog() { _state.value = _state.value.copy(showNewDirDialog = false) }
    fun setNewDirName(v: String) { _state.value = _state.value.copy(newDirName = v) }

    fun createDir() {
        val name = _state.value.newDirName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            try {
                val path = _state.value.currentPath.trimEnd('/') + "/" + name
                sftp.mkdir(path)
                _state.value = _state.value.copy(showNewDirDialog = false, status = "Создано: $name")
                navigate(_state.value.currentPath)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(error = "mkdir: ${t.message}")
            }
        }
    }

    fun openRenameDialog(file: RemoteFile) {
        _state.value = _state.value.copy(showRenameDialog = true, renameTarget = file, renameNewName = file.name)
    }
    fun dismissRenameDialog() { _state.value = _state.value.copy(showRenameDialog = false, renameTarget = null) }
    fun setRenameNewName(v: String) { _state.value = _state.value.copy(renameNewName = v) }

    fun doRename() {
        val target = _state.value.renameTarget ?: return
        val newName = _state.value.renameNewName.trim()
        if (newName.isEmpty()) return
        viewModelScope.launch {
            try {
                val newPath = _state.value.currentPath.trimEnd('/') + "/" + newName
                sftp.rename(target.path, newPath)
                _state.value = _state.value.copy(showRenameDialog = false, renameTarget = null, status = "Переименовано")
                navigate(_state.value.currentPath)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(error = "rename: ${t.message}")
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    override fun onCleared() {
        sftp.disconnect()
        super.onCleared()
    }
}
