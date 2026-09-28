package app.termosh.feature.settings.logs

import java.io.File

data class LogsUiState(
    val files: List<File> = emptyList(),
    val openedFile: File? = null,
    val openedContent: String = "",
    val dirPath: String = "",
)
