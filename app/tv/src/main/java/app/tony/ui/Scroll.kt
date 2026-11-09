package app.tony.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec

/**
 * Scroll only as far as needed to show the focused item (plus a little breathing room).
 * Android TV's default keeps focus ~30% down the screen, which pushed Home's header and hero
 * off-screen as soon as the Watch button took focus.
 */
@OptIn(ExperimentalFoundationApi::class)
object MinimalScroll : BringIntoViewSpec {
    private const val PAD = 140f  // px of room when we do scroll, so the label under a focused tile stays visible
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val end = offset + size
        return when {
            offset >= 0f && end <= containerSize -> 0f                 // already fully visible: don't move
            size > containerSize -> offset                              // taller than the screen: align top
            offset < 0f -> offset - PAD                                 // above: scroll up to it
            else -> end - containerSize + PAD                           // below: scroll down just enough
        }
    }
}

/** Ask for focus once the target is actually on screen (retries briefly; a first request can land
 *  before layout and be ignored, leaving nothing focused, so the remote seems to do nothing). */
@androidx.compose.runtime.Composable
fun FocusOnStart(requester: androidx.compose.ui.focus.FocusRequester, key: Any? = Unit) {
    androidx.compose.runtime.LaunchedEffect(key) {
        repeat(12) {
            kotlinx.coroutines.delay(if (it == 0) 16 else 60)
            // requestFocus() throws until the target is attached; once it doesn't, focus has moved.
            if (runCatching { requester.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }
}
