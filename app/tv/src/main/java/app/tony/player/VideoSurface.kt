package app.tony.player

import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.tony.data.Channel
import kotlinx.coroutines.delay

/** Tony uses at most two players: the main one and ONE muted preview (CLAUDE.md 5). */
class Players(val main: Stream, val preview: Stream)

val LocalPlayers = compositionLocalOf<Players> { error("Players not provided") }

@OptIn(UnstableApi::class)
@Composable
fun VideoSurface(stream: Stream, modifier: Modifier = Modifier, fill: Boolean = true, controls: Boolean = false, fullScreen: Boolean = !stream.preview) {
    AndroidView(
        modifier = modifier.clipToBounds(),   // "fill" scales the picture past its frame; keep it inside
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(if (fullScreen) app.tony.R.layout.player_surface else app.tony.R.layout.player_texture, null) as PlayerView).apply {
                useController = controls
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                setKeepContentOnPlayerReset(false)
                resizeMode = if (fill) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                player = stream.player
                if (!controls) { isFocusable = false; isFocusableInTouchMode = false; descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS }
            }
        },
        update = { v -> v.player = stream.player; v.resizeMode = if (fill) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT },
        onRelease = { v -> v.player = null },
    )
}

/**
 * Muted live preview that starts after the viewer rests on something for ~1 s and fades in only
 * once pictures are actually playing (so a dead stream just leaves the still visible).
 */
@Composable
fun LivePreview(channel: Channel?, modifier: Modifier = Modifier, delayMs: Long = 1000) {
    val p = LocalPlayers.current.preview
    LaunchedEffect(channel?.id) {
        p.stop()
        if (channel == null || channel.isYouTube || channel.isWebOnly) return@LaunchedEffect
        delay(delayMs)
        p.play("preview:" + channel.id, channel.url, channel.backupUrls, channel.headers)
    }
    DisposableEffect(Unit) { onDispose { p.stop() } }
    val a by animateFloatAsState(if (p.state == StreamState.Playing && !p.isRadio) 1f else 0f, tween(450), label = "preview")
    if (channel != null) VideoSurface(p, modifier.alpha(a))
}
