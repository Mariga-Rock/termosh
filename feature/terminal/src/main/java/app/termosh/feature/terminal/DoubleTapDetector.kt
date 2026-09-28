package app.termosh.feature.terminal

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Ловит ТОЛЬКО двойной тап.
 * Не потребляет события (requireUnconsumed = false),
 * поэтому не мешает SelectionContainer'у и long-press-выделению.
 */
suspend fun PointerInputScope.detectDoubleTapOnly(
    onDoubleTap: (Offset) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        waitForUpOrCancellation() ?: return@awaitEachGesture

        withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
            awaitFirstDown(requireUnconsumed = false)
        } ?: return@awaitEachGesture

        val secondUp = waitForUpOrCancellation() ?: return@awaitEachGesture
        onDoubleTap(secondUp.position)
    }
}
