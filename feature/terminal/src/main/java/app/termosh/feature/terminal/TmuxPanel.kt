package app.termosh.feature.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class TmuxAction(val label: String, val command: String)

private val TMUX_ACTIONS = listOf(
    TmuxAction("ls", "tmux ls\n"),
    TmuxAction("new", "tmux new -s main\n"),
    TmuxAction("attach", "tmux attach -t main\n"),
    TmuxAction("kill", "tmux kill-session -t main\n"),
    TmuxAction("detach", "\u0002d"),
    TmuxAction("split-v", "\u0002%"),
    TmuxAction("split-h", "\u0002\""),
    TmuxAction("win-new", "\u0002c"),
    TmuxAction("win-next", "\u0002n"),
    TmuxAction("win-prev", "\u0002p"),
)

@Composable
fun TmuxPanel(onCommand: (String) -> Unit) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
    ) {
        TMUX_ACTIONS.forEach { a ->
            TextButton(onClick = { onCommand(a.command) }) {
                Text(
                    text = a.label,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
