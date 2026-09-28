package app.termosh.core.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val DEFAULT_FG = Color(0xFFC0CAF5)
private val LINK_FG = Color(0xFF7AA2F7)
private val HIGHLIGHT_BG = Color(0x66E0AF68)


@Composable
fun TerminalView(
    buffer: TermScreen,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    searchCaseSensitive: Boolean = false,
) {
    val snapshot by buffer.snapshot.collectAsStateWithLifecycle()
    val rows = remember(snapshot) { buffer.rowsSnapshot() }
    val listState = rememberLazyListState()

    val matches = remember(searchQuery, snapshot) {
        if (searchQuery.isBlank()) emptySet()
        else buffer.findMatches(searchQuery, searchCaseSensitive).toSet()
    }

    LaunchedEffect(snapshot) {
        val lastNonEmpty = rows.indexOfLast { row -> row.any { it.ch != ' ' } }
        if (lastNonEmpty >= 0) listState.scrollToItem(lastNonEmpty)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1B26))
            .padding(6.dp),
    ) {
        LazyColumn(state = listState) {
            items(rows.size) { idx ->
                Text(
                    text = rowToAnnotated(rows[idx], idx, matches, searchQuery),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

private fun rowToAnnotated(
    row: List<TermCell>,
    rowIndex: Int,
    matches: Set<Pair<Int, Int>>,
    query: String,
): AnnotatedString = buildAnnotatedString {
    var lastNonSpace = -1
    for (i in row.indices) if (row[i].ch != ' ') lastNonSpace = i
    val end = lastNonSpace + 1
    if (end <= 0) {
        append(" ")
        return@buildAnnotatedString
    }

    val lineText = buildString { for (i in 0 until end) append(row[i].ch) }
    val linkRanges = mutableListOf<Triple<Int, Int, String>>() // start, end, url

    TextPatterns.URL.findAll(lineText).forEach { m ->
        val u = if (m.value.startsWith("http")) m.value else "https://${m.value}"
        linkRanges += Triple(m.range.first, m.range.last, u)
    }
    if (linkRanges.isEmpty()) {
        TextPatterns.PATH.findAll(lineText).forEach { m ->
            linkRanges += Triple(m.range.first, m.range.last, m.value)
        }
    }

    // Матч текущего символа в множестве выделенных
    fun isMatch(c: Int): Boolean {
        for ((r, c0) in matches) {
            if (r == rowIndex && c >= c0 && c < c0 + query.length) return true
        }
        return false
    }

    for (i in 0 until end) {
        val c = row[i]
        val inLink = linkRanges.firstOrNull { i in it.first..it.second }
        val style = SpanStyle(
            color = if (inLink != null) LINK_FG else (c.fg ?: DEFAULT_FG),
            background = if (isMatch(i)) HIGHLIGHT_BG else (c.bg ?: Color.Transparent),
            fontWeight = if (c.bold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (c.italic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = when {
                inLink != null -> TextDecoration.Underline
                c.underline -> TextDecoration.Underline
                else -> null
            },
        )
        withStyle(style) { append(c.ch) }
    }
}
