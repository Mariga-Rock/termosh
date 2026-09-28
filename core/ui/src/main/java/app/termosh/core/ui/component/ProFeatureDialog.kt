package app.termosh.core.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Диалог «функция ещё в разработке».
 *
 * Пока в приложении нет покупок, вместо «купить Pro» показываем честное
 * сообщение и два способа связаться с разработчиком: Telegram (быстро)
 * или GitHub Issues (публично).
 */
@Composable
fun ProFeatureDialog(
    featureName: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("В разработке") },
        text = {
            Column {
                Text(
                    "«$featureName» появится в будущих обновлениях.\n\n" +
                        "Если эта функция важна для вас — напишите разработчику, " +
                        "это поможет расставить приоритеты.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                ContactRow(
                    icon = Icons.Default.Chat,
                    label = "Telegram — быстро",
                    onClick = { openUrl("https://t.me/termosh_app_bot") },
                )
                ContactRow(
                    icon = Icons.Default.OpenInNew,
                    label = "GitHub Issues — публично",
                    onClick = {
                        val url = "https://github.com/Mariga-Rock/termosh-releases/issues/new" +
                            "?title=" + Uri.encode("Запрос функции: $featureName") +
                            "&body=" + Uri.encode(
                                "**Функция:** $featureName\n\n" +
                                    "**Почему она важна:**\n\n(напишите здесь)\n\n" +
                                    "**Версия приложения:** (см. в Настройках)\n" +
                                    "**Android:** (версия)\n" +
                                    "**Модель телефона:** \n"
                            )
                        openUrl(url)
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
    )
}

@Composable
private fun ContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 12.dp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
