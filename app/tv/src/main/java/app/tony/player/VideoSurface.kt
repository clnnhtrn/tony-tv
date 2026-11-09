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
        update = { v ->
            v.player = stream.player
            v.resizeMode = if (fill) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
            styleSubtitles(v, stream.captionSize, stream.captionStyle)
        },
        onRelease = { v -> v.player = null },
    )
}

/** Tony's subtitle look: Inter, softly rounded dark box (classic), yellow, or outlined text. */
@OptIn(UnstableApi::class)
fun styleSubtitles(v: PlayerView, size: Int, style: Int) {
    val sv = v.subtitleView ?: return
    val inter = androidx.core.content.res.ResourcesCompat.getFont(v.context, app.tony.R.font.inter)
    val cs = when (style) {
        1 -> androidx.media3.ui.CaptionStyleCompat(0xFFFFE14D.toInt(), 0x00000000, 0x00000000, androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW, 0xE6000000.toInt(), inter)
        2 -> androidx.media3.ui.CaptionStyleCompat(0xFFFFFFFF.toInt(), 0x00000000, 0x00000000, androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE, 0xFF000000.toInt(), inter)
        else -> androidx.media3.ui.CaptionStyleCompat(0xFFFFFFFF.toInt(), 0xB3101114.toInt(), 0x00000000, androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_NONE, 0, inter)
    }
    sv.setApplyEmbeddedStyles(false)
    sv.setApplyEmbeddedFontSizes(false)
    sv.setStyle(cs)
    sv.setFractionalTextSize(androidx.media3.ui.SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * when (size) { 0 -> 0.85f; 2 -> 1.35f; else -> 1.08f })
    sv.setBottomPaddingFraction(0.09f)
}

/**
 * Live preview that starts after the viewer rests on something for ~1 s and fades in only
 * once pictures are actually playing (so a dead stream just leaves the still visible).
 */
@Composable
fun LivePreview(channel: Channel?, modifier: Modifier = Modifier, delayMs: Long = 1000, sound: Boolean = true) {
    val p = LocalPlayers.current.preview
    LaunchedEffect(channel?.id) {
        p.stop()
        if (channel == null || channel.isYouTube || channel.isWebOnly) return@LaunchedEffect
        delay(delayMs)
        p.player.volume = 0f
        p.play("preview:" + channel.id, channel.url, channel.backupUrls, channel.headers)
        if (sound) p.fadeVolumeTo(1f)   // owner wants to hear previews on Home and Details
    }
    DisposableEffect(Unit) { onDispose { p.stop() } }
    val a by animateFloatAsState(if (p.state == StreamState.Playing && !p.isRadio) 1f else 0f, tween(450), label = "preview")
    if (channel != null) VideoSurface(p, modifier.alpha(a))
}
