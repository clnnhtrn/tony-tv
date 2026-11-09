package app.tony.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

enum class StreamState { Idle, Loading, Playing, Failed }

/**
 * One ExoPlayer plus Tony's rules for free streams:
 *  - try `url`, then each backup URL, on error or if nothing plays within [stallMs]
 *  - send only headers the playlist publishes for that stream
 *  - preview mode: muted, ≤ 640×360, small buffer (Chromecast has little RAM)
 */
@OptIn(UnstableApi::class)
class Stream(context: Context, val preview: Boolean) {
    private val main = Handler(Looper.getMainLooper())
    private val selector = DefaultTrackSelector(context).apply {
        if (preview) setParameters(buildUponParameters().setMaxVideoSize(640, 360).setMaxVideoBitrate(1_200_000))
    }
    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setTrackSelector(selector)
        .setLoadControl(
            if (preview) DefaultLoadControl.Builder().setBufferDurationsMs(2_000, 8_000, 1_000, 1_500).build()
            else DefaultLoadControl.Builder().setBufferDurationsMs(15_000, 40_000, 2_000, 4_000).build()
        )
        .build().apply {
            volume = if (preview) 0f else 1f
            playWhenReady = true
        }

    var state by mutableStateOf(StreamState.Idle); private set
    var hasCaptions by mutableStateOf(false); private set
    var audioTracks by mutableStateOf(0); private set
    var isRadio by mutableStateOf(false); private set
    private var urls: List<String> = emptyList()
    private var headers: Map<String, String> = emptyMap()
    private var attempt = 0
    private var key: String? = null
    private val stallMs = if (preview) 12_000L else 15_000L
    private val stall = Runnable { if (state == StreamState.Loading) next() }

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(s: Int) {
                if (s == Player.STATE_READY && player.playWhenReady) { state = StreamState.Playing; main.removeCallbacks(stall) }
                if (s == Player.STATE_ENDED && state == StreamState.Playing && !preview) next()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) { if (isPlaying) state = StreamState.Playing }
            override fun onPlayerError(error: PlaybackException) { next() }
            override fun onTracksChanged(tracks: Tracks) {
                hasCaptions = tracks.groups.any { it.type == C.TRACK_TYPE_TEXT && it.isSupported }
                audioTracks = tracks.groups.count { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }
                isRadio = tracks.groups.isNotEmpty() && tracks.groups.none { it.type == C.TRACK_TYPE_VIDEO }
            }
        })
    }

    /** Play a channel (or film). Same key again = keep playing. */
    fun play(key: String, url: String, backups: List<String> = emptyList(), headers: Map<String, String>? = null) {
        if (this.key == key && state != StreamState.Failed && state != StreamState.Idle) return
        this.key = key
        this.urls = (listOf(url) + backups).distinct().filter { it.startsWith("http://") || it.startsWith("https://") || it.startsWith("rtmp://") || it.startsWith("rtsp://") }
        this.headers = headers.orEmpty().filterKeys { it.equals("User-Agent", true) || it.equals("Referer", true) }
        attempt = -1
        next()
    }

    fun retry() { key?.let { k -> val u = urls; this.key = null; if (u.isNotEmpty()) play(k, u.first(), u.drop(1), headers) } }

    private fun next() {
        main.removeCallbacks(stall)
        attempt++
        if (attempt >= urls.size) { state = StreamState.Failed; player.stop(); return }
        state = StreamState.Loading
        val u = urls[attempt]
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(headers.entries.firstOrNull { it.key.equals("User-Agent", true) }?.value ?: "Tony/1.0 (Android TV)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8_000).setReadTimeoutMs(10_000)
            .setDefaultRequestProperties(headers.filterKeys { it.equals("Referer", true) })
        val item = MediaItem.Builder().setUri(u).apply {
            val l = u.lowercase()
            when {
                ".m3u8" in l || "m3u8" in l -> setMimeType(MimeTypes.APPLICATION_M3U8)
                ".mpd" in l -> setMimeType(MimeTypes.APPLICATION_MPD)
            }
        }.build()
        player.setMediaSource(DefaultMediaSourceFactory(http).createMediaSource(item))
        player.prepare()
        player.play()
        main.postDelayed(stall, stallMs)
    }

    fun setSubtitles(on: Boolean) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !on)
            .setPreferredTextLanguage(if (on) "en" else null)
            .build()
    }

    fun nextAudioTrack() {
        val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }
        if (groups.size < 2) return
        val cur = groups.indexOfFirst { it.isSelected }
        val nxt = groups[(cur + 1) % groups.size]
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(androidx.media3.common.TrackSelectionOverride(nxt.mediaTrackGroup, 0)).build()
    }

    fun stop() { main.removeCallbacks(stall); key = null; state = StreamState.Idle; player.stop(); player.clearMediaItems() }
    fun release() { main.removeCallbacks(stall); player.release() }
}
