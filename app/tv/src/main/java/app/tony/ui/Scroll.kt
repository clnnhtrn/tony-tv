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
    override val scrollAnimationSpec: androidx.compose.animation.core.AnimationSpec<Float> =
        androidx.compose.animation.core.tween(durationMillis = 300, easing = androidx.compose.animation.core.CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val horizontal = containerSize > 1500f            // rows span the screen width (1920 px); pages scroll vertically
        if (horizontal) {
            val margin = containerSize * 0.0542f            // page margin (104 px at 1080p): every row starts here
            val d = offset - margin                         // selected card snaps to the margin line, like the first card
            return if (kotlin.math.abs(d) < 1f) 0f else d
        }
        val pad = containerSize * 0.12f
        val end = offset + size
        return when {
            offset >= 0f && end <= containerSize - pad * 0.4f -> 0f
            size > containerSize -> offset
            offset < 0f -> offset - pad
            else -> end - containerSize + pad
        }
    }
}

/** Give a screen its starting focus. Requested a few times over the first half-second: the first
 *  request can land before layout, or be undone when the previous screen is torn down, which
 *  would leave nothing focused (the remote then seems to do nothing). */
@androidx.compose.runtime.Composable
fun FocusOnStart(requester: androidx.compose.ui.focus.FocusRequester, key: Any? = Unit) {
    androidx.compose.runtime.LaunchedEffect(key) {
        for (wait in longArrayOf(16, 120, 300, 600)) {
            kotlinx.coroutines.delay(wait)
            runCatching { requester.requestFocus() }
        }
    }
}


/** While [enabled], Back runs [onBack] (e.g. closes this panel) instead of leaving the screen. */
@androidx.compose.runtime.Composable
fun BackLayer(s: app.tony.AppState, enabled: Boolean, onBack: () -> Unit) {
    val latest = androidx.compose.runtime.rememberUpdatedState(onBack)
    androidx.compose.runtime.DisposableEffect(enabled) {
        val layer: () -> Unit = { latest.value() }
        if (enabled) s.backLayers.add(layer)
        onDispose { s.backLayers.remove(layer) }
    }
}
