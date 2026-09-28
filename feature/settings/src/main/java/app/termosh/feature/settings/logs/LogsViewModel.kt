package app.termosh.feature.settings.logs

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import app.termosh.core.terminal.SessionLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: SessionLogger,
) : ViewModel() {

    private val _state = MutableStateFlow(LogsUiState())
    val state: StateFlow<LogsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val sessionFiles = logger.list()
        val appLog = app.termosh.core.common.AppLogger.logFile()
        val all = mutableListOf<java.io.File>()
        if (appLog != null && appLog.exists()) all += appLog
        all += sessionFiles
        _state.value = _state.value.copy(
            files = all,
            dirPath = logger.dirPath(),
        )
    }

    fun open(file: File) {
        _state.value = _state.value.copy(
            openedFile = file,
            openedContent = logger.read(file),
        )
    }

    fun closeOpened() {
        _state.value = _state.value.copy(openedFile = null, openedContent = "")
    }

    fun delete(file: File) {
        logger.delete(file)
        if (_state.value.openedFile == file) closeOpened()
        refresh()
    }

    fun share(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Поделиться логом")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (t: Throwable) {
            app.termosh.core.common.AppLogger.e("share() failed: ${file.absolutePath}", t)
        }
    }

    fun copyToClipboard(text: String) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("termosh.log", text))
        } catch (t: Throwable) {
            app.termosh.core.common.AppLogger.e("copyToClipboard failed", t)
        }
    }
}
