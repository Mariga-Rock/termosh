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

private data class KeySpec(val label: String, val seq: String)

private val KEY_ROW = listOf(
    KeySpec("ESC", "\u001b"),
    KeySpec("TAB", "\t"),
    KeySpec("←", "\u001b[D"),
    KeySpec("↑", "\u001b[A"),
    KeySpec("↓", "\u001b[B"),
    KeySpec("→", "\u001b[C"),
    KeySpec("PGUP", "\u001b[5~"),
    KeySpec("PGDN", "\u001b[6~"),
    KeySpec("HOME", "\u001b[H"),
    KeySpec("END", "\u001b[F"),
    KeySpec("^C", "\u0003"),
    KeySpec("^D", "\u0004"),
    KeySpec("^Z", "\u001a"),
    KeySpec("|", "|"),
    KeySpec("~", "~"),
    KeySpec("-", "-"),
    KeySpec("/", "/"),
)

@Composable
fun KeyboardBar(
    modifiers: Set<String>,
    onKey: (String) -> Unit,
    onToggleModifier: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
    ) {
        ModifierKey("CTRL", "CTRL" in modifiers) { onToggleModifier("CTRL") }
        ModifierKey("ALT", "ALT" in modifiers) { onToggleModifier("ALT") }

        SpecialKey("⌫") { onKey("\u007F") }
        SpecialKey("⏎") { onKey("\r") }
        SpecialKey("␣") { onKey(" ") }

        KEY_ROW.forEach { key ->
            TextButton(onClick = { onKey(key.seq) }) {
                Text(
                    text = key.label,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SpecialKey(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun ModifierKey(label: String, active: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            color = if (active) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    }
}
