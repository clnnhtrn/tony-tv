package app.tony.ui

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.tony.AppState
import app.tony.data.Channel
import app.tony.player.LivePreview
import app.tony.player.LocalPlayers
import app.tony.player.StreamState
import app.tony.player.VideoSurface
import kotlinx.coroutines.delay

@Composable
fun WatchScreen(s: AppState, start: Channel, startList: List<Channel>) {
    val players = LocalPlayers.current
    val main = players.main
    var list by remember { mutableStateOf(startList.ifEmpty { listOf(start) }) }
    var current by remember { mutableStateOf(start) }
    var stripOpen by remember { mutableStateOf(false) }
    var optionsOpen by remember { mutableStateOf(false) }
    var infoVisible by remember { mutableStateOf(true) }
    var fill by remember { mutableStateOf(true) }
    val rootFocus = remember { FocusRequester() }

    fun tune(c: Channel) { current = c; infoVisible = true }
    fun zap(by: Int) {
        val l = s.zapList(current, list)
        val i = l.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        // skip channels known not to play right now (unless nothing else does)
        var j = i
        repeat(l.size) {
            j = (j + by + l.size) % l.size
            if (!l[j].isDown || j == i) { tune(l[j]); return }
        }
        tune(l[(i + by + l.size) % l.size])
    }

    LaunchedEffect(Unit) { players.preview.stop() }
    LaunchedEffect(current.id) {
        main.captionSize = s.saved.captionSize; main.captionStyle = s.saved.captionStyle
        main.play(current.id, current.url, current.backupUrls, current.headers); main.setSubtitles(s.saved.subtitles); s.watched(current)
    }
    LifecycleStartEffect(current.id) { main.play(current.id, current.url, current.backupUrls, current.headers); onStopOrDispose { } }
    // Leaving the watch screen stops the sound (it kept playing behind Home before)
    DisposableEffect(Unit) { onDispose { main.stop() } }
    LaunchedEffect(current.id) {   // minutes watched per channel, kept on this TV only ("Top in …" ranking)
        while (true) { delay(30_000); if (main.state == StreamState.Playing) s.addWatchTime(current.id, 30_000) }
    }
    // Remember whether this channel plays on this TV (labels, order and Up/Down use it)
    LaunchedEffect(current.id, main.state) {
        when (main.state) {
            StreamState.Playing -> s.markHere(current, "ok")
            StreamState.Failed -> s.markHere(current, if (main.refused) "geoblocked" else "broken")
            else -> {}
        }
    }
    LaunchedEffect(current.id, infoVisible) { if (infoVisible) { delay(5000); infoVisible = false } }
    // Focus back on the picture (so OK, Up/Down and Right work) after opening and whenever a panel closes;
    // retried because the first request can land during the page fade and be lost
    LaunchedEffect(stripOpen, optionsOpen, main.state == StreamState.Failed) {
        if (!stripOpen && !optionsOpen && main.state != StreamState.Failed) for (w in longArrayOf(16, 120, 300, 600)) { delay(w); runCatching { rootFocus.requestFocus() } }
    }
    BackLayer(s, stripOpen || optionsOpen) { if (optionsOpen) optionsOpen = false else stripOpen = false }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .focusRequester(rootFocus).focusable()
            .onKeyEvent { e ->
                if (stripOpen || optionsOpen || e.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                when (e.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> { zap(-1); true }
                    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> { zap(1); true }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_DPAD_LEFT -> { stripOpen = true; true }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO -> { optionsOpen = true; true }
                    else -> { infoVisible = true; false }
                }
            }
    ) {
        VideoSurface(main, Modifier.fillMaxSize(), fill = fill)

        when {
            main.state == StreamState.Failed -> FailedCard(current, onRetry = { main.retry() }, onNext = { zap(1) }, onBack = { s.back() })
            main.state == StreamState.Loading || main.state == StreamState.Idle -> TuningIn(current)
            main.isRadio || current.isRadio -> RadioCard(current)
        }

        // Info: channel + how to use the remote (incl. Back), for 5 s after tuning in or any key press
        AnimatedVisibility(infoVisible && !stripOpen && !optionsOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent))))
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(130.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
                Row(Modifier.padding(start = T.Margin, top = 34.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LogoBadge(current, BadgeWide)
                    Column {
                        Text("NOW WATCHING", style = ts(7.sp, 700, T.Text3, 0.9.sp))
                        Text(current.name, style = ts(16.sp, 750, Color.White))
                    }
                    LiveChip()
                }
                Row(Modifier.align(Alignment.BottomStart).padding(start = T.Margin, bottom = 30.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    KeyHint("BACK", "Go back", Icons.Back)
                    KeyHint("OK", "Channels")
                    KeyHint("▲ ▼", "Change channel")
                    KeyHint("▶", "Options")
                }
            }
        }

        AnimatedVisibility(stripOpen, enter = slideInVertically { it / 3 } + fadeIn(), exit = slideOutVertically { it / 3 } + fadeOut(), modifier = Modifier.align(Alignment.BottomStart)) {
            Strip(s, current, list, onSwitch = { c, l -> list = l; tune(c); stripOpen = false }, onBack = { s.back() },
                controls = {
                    // Reminders of what the remote buttons do (labels only, not selectable)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        KeyHint("BACK", "Close", Icons.Back)
                        KeyHint("OK", "Switch")
                        KeyHint("▲ ▼", "Channel")
                        KeyHint("▶", "Options")
                    }
                })
        }

        AnimatedVisibility(optionsOpen, enter = fadeIn() + scaleIn(initialScale = 0.96f), exit = fadeOut() + scaleOut(targetScale = 0.96f), modifier = Modifier.align(Alignment.CenterEnd)) {
            OptionsCard(
                hasCaptions = main.hasCaptions, subtitles = s.saved.subtitles,
                onSubtitles = { on -> main.setSubtitles(on); s.setSubtitles(on) },
                size = main.captionSize, style = main.captionStyle,
                onLook = { sz, st -> main.captionSize = sz; main.captionStyle = st; s.setCaptionLook(sz, st) },
                audioTracks = main.audioTracks, onAudio = { main.nextAudioTrack() },
                fill = fill, onFill = { fill = it },
                onReport = { s.showToast("Thanks. Tony will check ${current.name}"); optionsOpen = false },
            )
        }
    }
}

@Composable
private fun KeyHint(key: String, label: String, icon: ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.88f)).padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, null, tint = T.Bg, modifier = Modifier.size(8.dp))
            Text(key, style = ts(7.sp, 800, T.Bg, 0.5.sp), maxLines = 1, softWrap = false)
        }
        Text(label, style = ts(9.5.sp, 600, Color.White.copy(alpha = 0.8f)), maxLines = 1, softWrap = false)
    }
}

@Composable
private fun TuningIn(c: Channel) {
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(parseColor(c.tileColor, T.Panel).copy(alpha = 0.6f), T.Bg), radius = 1500f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LogoBadge(c, BadgeHero)
            Text("Tuning in to ${c.name}", style = ts(13.sp, 600, Color.White))
            PulsingDots()
        }
    }
}

@Composable
fun PulsingDots() {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(320); step = (step + 1) % 3 } }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i -> Box(Modifier.size(6.dp).clip(CircleShape).background(if (i == step) T.Accent else Color.White.copy(alpha = 0.22f))) }
    }
}

@Composable
private fun RadioCard(c: Channel) {
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(parseColor(c.tileColor, T.Panel), T.Bg), radius = 1400f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LogoBadge(c, BadgeHero)
            Text(c.name, style = ts(24.sp, 800, Color.White))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Radio, null, tint = T.Accent, modifier = Modifier.size(12.dp)); Text("Radio · sound only", style = ts(10.5.sp, 500, T.Text2))
            }
        }
    }
}

@Composable
private fun FailedCard(c: Channel, onRetry: () -> Unit, onNext: () -> Unit, onBack: () -> Unit) {
    val focus = remember { FocusRequester() }
    FocusOnStart(focus, c.id)
    Box(Modifier.fillMaxSize().background(T.Bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LogoBadge(c, BadgeHero)
            Text("${c.name} isn't working right now", style = ts(19.sp, 750, Color.White))
            Text(if (c.geoblocked) "The station may only allow viewers in ${c.country}." else "The station may be off air. Try again later, or watch another channel.",
                style = ts(11.sp, 400, T.Text2))
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TonyButton("Next channel", onNext, Modifier.focusRequester(focus), primary = true)
                TonyButton("Try again", onRetry)
                TonyButton("Back", onBack)
            }
        }
    }
}

@Composable
private fun Strip(s: AppState, current: Channel, currentList: List<Channel>, onSwitch: (Channel, List<Channel>) -> Unit, onBack: () -> Unit,
                  controls: @Composable () -> Unit) {
    val lists = buildList {
        add("Channels" to currentList)
        if (s.saved.favourites.isNotEmpty()) add("Favourites" to s.saved.favourites)
        s.countryRows.forEach { add(it.title to it.channels) }
    }.distinctBy { it.first }
    var pick by remember { mutableIntStateOf(0) }
    var focusedIndex by remember { mutableIntStateOf(-1) }
    val chans = lists.getOrNull(pick)?.second.orEmpty()
    val rowState = rememberLazyListState()
    val tileFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { rowState.scrollToItem(chans.indexOfFirst { it.id == current.id }.coerceAtLeast(0)) }
    FocusOnStart(tileFocus)
    Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF08090D).copy(alpha = 0.85f), Color(0xFF08090D)))).padding(top = 40.dp, bottom = 26.dp)) {
        val fc = chans.getOrNull(focusedIndex)
        // v8: a large preview sits right above the selected card (cards snap to the margin, so it's always aligned)
        Box(Modifier.height(212.dp).fillMaxWidth().padding(start = T.Margin)) {
            if (fc != null && fc.id != current.id) {
                Column(Modifier.width(300.dp).clip(RoundedCornerShape(12.dp)).background(T.Card)) {
                    Box(Modifier.fillMaxWidth().height(169.dp).background(parseColor(fc.tileColor, T.Card)), contentAlignment = Alignment.Center) {
                        LogoBadge(fc, BadgeWide)
                        LivePreview(fc, Modifier.fillMaxSize(), delayMs = 600, sound = false)   // the channel you're watching keeps its sound
                        Row(Modifier.align(Alignment.TopStart).padding(8.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(4.dp).clip(CircleShape).background(T.Live)); Text("Preview", style = ts(8.sp, 700, Color.White))
                        }
                    }
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(fc.title, style = ts(13.sp, 800, Color.White), maxLines = 1, modifier = Modifier.weight(1f))
                        Text("Press OK to switch", style = ts(9.sp, 600, Color.White.copy(alpha = 0.7f)))
                    }
                }
            }
        }
        Row(Modifier.padding(end = T.Margin, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            // tabs scroll sideways if a family has many countries; the key legend stays put on the right
            LazyRow(Modifier.weight(1f).focusRestorer(), contentPadding = PaddingValues(start = T.Margin, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                item("home") { Row { BackDot(onBack); Spacer(Modifier.width(8.dp)) } }
                itemsIndexed(lists, key = { _, l -> "tab" + l.first }) { i, (name, _) -> Pill(name, i == pick) { pick = i; focusedIndex = -1 } }
            }
            controls()
        }
        LazyRow(Modifier.focusRestorer(), state = rowState, contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(T.RowGapX)) {
            itemsIndexed(chans, key = { _, c -> "strip" + c.id }) { i, c ->
                ChannelTile(c, wide = false, watching = c.id == current.id, favourite = s.isFavourite(c),
                    modifier = if (c.id == current.id || (i == 0 && chans.none { it.id == current.id })) Modifier.focusRequester(tileFocus) else Modifier,
                    onFocus = { focusedIndex = i }, onClick = { onSwitch(c, chans) }, onLongClick = { s.toggleFavourite(c) })
            }
        }
    }
}

/** Small round "back to Home" button at the start of the strip's tabs. */
@Composable
private fun BackDot(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(26.dp).semantics { contentDescription = "Back to Home" },
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.14f), contentColor = Color.White,
            focusedContainerColor = Color.White, focusedContentColor = T.Bg),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
    ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Back, null, modifier = Modifier.size(11.dp)) } }
}

@Composable
private fun Pill(text: String, selected: Boolean, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent,
            contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.65f), focusedContainerColor = Color.White, focusedContentColor = T.Bg,
            disabledContainerColor = Color.Transparent, disabledContentColor = Color.White.copy(alpha = 0.3f),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 5.5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (icon != null) Icon(icon, null, modifier = Modifier.size(10.dp))
            Text(text, style = ts(10.sp, 600, Color.Unspecified))
        }
    }
}

/** Floating options card, kept inside the TV-safe area (TVs crop the outer edge of the picture). */
@Composable
private fun OptionsCard(
    hasCaptions: Boolean, subtitles: Boolean, onSubtitles: (Boolean) -> Unit,
    size: Int, style: Int, onLook: (Int, Int) -> Unit,
    audioTracks: Int, onAudio: () -> Unit, fill: Boolean, onFill: (Boolean) -> Unit, onReport: () -> Unit,
) {
    val first = remember { FocusRequester() }
    FocusOnStart(first)
    val sizes = listOf("Small", "Medium", "Large")
    val styles = listOf("Classic", "Yellow", "Outline")
    Column(
        Modifier.padding(end = T.Margin, top = 40.dp, bottom = 40.dp).width(320.dp)
            .clip(RoundedCornerShape(18.dp)).background(Color(0xFF1C1D24).copy(alpha = 0.96f)).border(0.5.dp, T.HairlineStrong, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Options", style = ts(18.sp, 800, Color.White))
        Text("Press Back to close", style = ts(8.5.sp, 500, T.Muted), modifier = Modifier.padding(bottom = 8.dp))
        SectionTitle("Subtitles")
        OptionRow("Subtitles", if (!hasCaptions) "Not on this channel" else if (subtitles) "On" else "Off",
            if (hasCaptions) Modifier.focusRequester(first) else Modifier, enabled = hasCaptions, icon = Icons.Captions) { onSubtitles(!subtitles) }
        // Look settings only make sense when subtitles are showing: greyed out otherwise
        val lookOn = hasCaptions && subtitles
        OptionRow("Text size", sizes[size], enabled = lookOn) { onLook((size + 1) % 3, style) }
        OptionRow("Style", styles[style], enabled = lookOn) { onLook(size, (style + 1) % 3) }
        SectionTitle("Picture and sound")
        OptionRow("Picture", if (fill) "Fill screen" else "Fit", if (!hasCaptions) Modifier.focusRequester(first) else Modifier) { onFill(!fill) }
        OptionRow("Audio", if (audioTracks > 1) "Change ($audioTracks)" else "Only one", enabled = audioTracks > 1, onClick = onAudio)
        Spacer(Modifier.height(6.dp))
        OptionRow("Report a problem", "") { onReport() }
    }
}

@Composable
private fun SectionTitle(t: String) = Text(t.uppercase(), style = ts(7.5.sp, 700, T.Muted, 1.sp), modifier = Modifier.padding(top = 6.dp, bottom = 1.dp))

@Composable
fun OptionRow(label: String, value: String, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(36.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = T.Text, focusedContainerColor = Color.White, focusedContentColor = T.Bg),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, null, modifier = Modifier.size(12.dp))
            Text(label, style = ts(10.5.sp, 600, if (enabled) Color.Unspecified else T.Muted), modifier = Modifier.weight(1f))
            Text(value, style = ts(9.5.sp, 500, if (enabled) Color.Unspecified else T.Muted))
        }
    }
}
