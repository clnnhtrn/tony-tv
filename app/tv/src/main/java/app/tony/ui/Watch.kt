package app.tony.ui

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
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
    val main = LocalPlayers.current.main
    var list by remember { mutableStateOf(startList.ifEmpty { listOf(start) }) }
    var current by remember { mutableStateOf(start) }
    var stripOpen by remember { mutableStateOf(false) }
    var optionsOpen by remember { mutableStateOf(false) }
    var chipVisible by remember { mutableStateOf(true) }
    var fill by remember { mutableStateOf(true) }
    var subtitles by remember { mutableStateOf(s.saved.subtitles) }
    val rootFocus = remember { FocusRequester() }

    fun tune(c: Channel) { current = c; chipVisible = true }
    fun zap(by: Int) {
        val i = list.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        tune(list[(i + by + list.size) % list.size])
    }

    LaunchedEffect(current.id) { main.play(current.id, current.url, current.backupUrls, current.headers); main.setSubtitles(subtitles); s.watched(current) }
    LifecycleStartEffect(current.id) {   // resume after the TV returns to Tony; stop when it leaves
        main.play(current.id, current.url, current.backupUrls, current.headers)
        onStopOrDispose { }
    }
    LaunchedEffect(current.id, chipVisible) { if (chipVisible) { delay(4000); chipVisible = false } }
    LaunchedEffect(stripOpen, optionsOpen) { if (!stripOpen && !optionsOpen) runCatching { rootFocus.requestFocus() } }
    BackHandler(enabled = stripOpen || optionsOpen) { if (optionsOpen) optionsOpen = false else stripOpen = false }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .focusRequester(rootFocus).focusable()
            .onKeyEvent { e ->
                if (stripOpen || optionsOpen || e.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                when (e.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> { zap(-1); true }
                    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> { zap(1); true }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> { stripOpen = true; true }
                    KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO -> { optionsOpen = true; true }
                    else -> false
                }
            }
    ) {
        VideoSurface(main, Modifier.fillMaxSize(), fill = fill)

        when {
            main.state == StreamState.Failed -> FailedCard(current, onRetry = { main.retry() }, onNext = { zap(1) }, onBack = { s.back() })
            main.state == StreamState.Loading || main.state == StreamState.Idle -> TuningIn(current)
            main.isRadio || current.isRadio -> RadioCard(current)
        }

        AnimatedVisibility(chipVisible && !stripOpen, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.padding(start = T.Margin, top = T.Top)) {
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).background(T.ChipBg).border(0.5.dp, Color.White.copy(0.14f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                LogoBadge(current, BadgeStd)
                Column {
                    Text("NOW WATCHING", style = ts(6.sp, 700, T.Muted, 0.8.sp))
                    Text(current.name, style = ts(10.5.sp, 650, Color.White))
                }
                Spacer(Modifier.width(4.dp))
                LiveChip()
            }
        }

        AnimatedVisibility(stripOpen, enter = slideInVertically { it / 2 } + fadeIn(), exit = slideOutVertically { it / 2 } + fadeOut(), modifier = Modifier.align(Alignment.BottomStart)) {
            Strip(s, current, list,
                onSwitch = { c, l -> list = l; tune(c); stripOpen = false },
                onOptions = { stripOpen = false; optionsOpen = true })
        }

        AnimatedVisibility(optionsOpen, enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut(), modifier = Modifier.align(Alignment.CenterEnd)) {
            OptionsPanel(
                hasCaptions = main.hasCaptions, subtitles = subtitles, onSubtitles = { subtitles = it; main.setSubtitles(it); s.setSubtitles(it) },
                audioTracks = main.audioTracks, onAudio = { main.nextAudioTrack() },
                fill = fill, onFill = { fill = it },
                onReport = { s.showToast("Thanks. Tony will check ${current.name}"); optionsOpen = false },
            )
        }
    }
}

@Composable
private fun TuningIn(c: Channel) {
    Box(Modifier.fillMaxSize().background(T.Bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LogoBadge(c, BadgeHero)
            Text("Tuning in to ${c.name}…", style = ts(12.sp, 500, T.Text2))
            PulsingDots()
        }
    }
}

@Composable
private fun PulsingDots() {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(350); step = (step + 1) % 3 } }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { i -> Box(Modifier.size(5.dp).clip(CircleShape).background(if (i == step) T.Accent else Color.White.copy(alpha = 0.2f))) }
    }
}

@Composable
private fun RadioCard(c: Channel) {
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(parseColor(c.tileColor, T.Panel), T.Bg), radius = 1400f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LogoBadge(c, BadgeHero)
            Text(c.name, style = ts(22.sp, 800, Color.White))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Radio, null, tint = T.Accent, modifier = Modifier.size(12.dp)); Text("Radio · sound only", style = ts(10.sp, 500, T.Text3))
            }
        }
    }
}

@Composable
private fun FailedCard(c: Channel, onRetry: () -> Unit, onNext: () -> Unit, onBack: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(c.id) { delay(100); runCatching { focus.requestFocus() } }
    Box(Modifier.fillMaxSize().background(T.Bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LogoBadge(c, BadgeHero)
            Text("${c.name} isn't working right now", style = ts(18.sp, 750, Color.White))
            Text(if (c.geoblocked) "The station may only allow viewers in ${c.country}." else "The station may be off air. Try again later, or watch another channel.",
                style = ts(10.5.sp, 400, T.Text3))
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                TonyButton("Next channel", onNext, Modifier.focusRequester(focus), icon = Icons.Chevron, primary = true)
                TonyButton("Try again", onRetry)
                TonyButton("Back", onBack, icon = Icons.Back)
            }
        }
    }
}

@Composable
private fun Strip(s: AppState, current: Channel, currentList: List<Channel>, onSwitch: (Channel, List<Channel>) -> Unit, onOptions: () -> Unit) {
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
    LaunchedEffect(Unit) {
        val i = chans.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
        rowState.scrollToItem(i); delay(60); runCatching { tileFocus.requestFocus() }
    }
    Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f), Color.Black.copy(alpha = 0.92f)))).padding(top = 120.dp, bottom = 22.dp)) {
        // Preview card above the focused tile
        val fc = chans.getOrNull(focusedIndex)
        Box(Modifier.height(150.dp).fillMaxWidth()) {
            if (fc != null && fc.id != current.id) {
                val x = (T.Margin + (T.StdW + T.Gap) * (focusedIndex - rowState.firstVisibleItemIndex).coerceAtLeast(0)).coerceAtMost(700.dp)
                Column(Modifier.offset(x = x - 40.dp).width(220.dp).clip(RoundedCornerShape(9.dp)).background(T.Panel).border(0.5.dp, T.HairlineStrong, RoundedCornerShape(9.dp))) {
                    Box(Modifier.fillMaxWidth().height(110.dp).background(parseColor(fc.tileColor, T.Surface)), contentAlignment = Alignment.Center) {
                        LogoBadge(fc, BadgeWide)
                        LivePreview(fc, Modifier.fillMaxSize(), delayMs = 700)
                        LiveChip(Modifier.align(Alignment.TopStart).padding(6.dp), "PREVIEW", dot = true)
                    }
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(fc.name, style = ts(10.sp, 650, Color.White), maxLines = 1, modifier = Modifier.weight(1f))
                        Text("OK to switch", style = ts(7.5.sp, 600, T.Accent))
                    }
                }
            }
        }
        Row(Modifier.padding(start = T.Margin, end = T.Margin, bottom = 9.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            lists.forEachIndexed { i, (name, _) -> Pill(name, i == pick) { pick = i; focusedIndex = -1 } }
            Spacer(Modifier.weight(1f))
            Pill("Options", false, onOptions)
        }
        LazyRow(Modifier.focusRestorer(), state = rowState, contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(T.Gap)) {
            itemsIndexed(chans, key = { _, c -> "strip" + c.id }) { i, c ->
                ChannelTile(c, wide = false, watching = c.id == current.id, favourite = s.isFavourite(c),
                    modifier = if (c.id == current.id || (i == 0 && chans.none { it.id == current.id })) Modifier.focusRequester(tileFocus) else Modifier,
                    onFocus = { focusedIndex = i },
                    onClick = { if (c.id == current.id) onSwitch(c, chans) else onSwitch(c, chans) },
                    onLongClick = { s.toggleFavourite(c) })
            }
        }
    }
}

@Composable
private fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
            contentColor = T.Text, focusedContainerColor = Color.White, focusedContentColor = T.Bg,
        ),
        border = ClickableSurfaceDefaults.border(border = androidx.tv.material3.Border(androidx.compose.foundation.BorderStroke(0.5.dp, T.Hairline))),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
    ) { Text(text, style = ts(9.sp, 600, Color.Unspecified), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.5.dp)) }
}

@Composable
private fun OptionsPanel(
    hasCaptions: Boolean, subtitles: Boolean, onSubtitles: (Boolean) -> Unit,
    audioTracks: Int, onAudio: () -> Unit, fill: Boolean, onFill: (Boolean) -> Unit, onReport: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(80); runCatching { first.requestFocus() } }
    Column(
        Modifier.fillMaxHeight().width(300.dp).background(T.Panel.copy(alpha = 0.97f)).border(0.5.dp, T.Hairline)
            .padding(horizontal = 20.dp, vertical = T.Top),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Options", style = ts(16.sp, 800, Color.White), modifier = Modifier.padding(bottom = 10.dp))
        // Focus starts on the first option that can actually be used; unavailable ones are skipped.
        OptionRow("Subtitles", if (!hasCaptions) "Not available" else if (subtitles) "On" else "Off", if (hasCaptions) Modifier.focusRequester(first) else Modifier, enabled = hasCaptions) { onSubtitles(!subtitles) }
        OptionRow("Audio", if (audioTracks > 1) "Change ($audioTracks)" else "Only one", enabled = audioTracks > 1, onClick = onAudio)
        OptionRow("Picture size", if (fill) "Fill screen" else "Fit", if (!hasCaptions) Modifier.focusRequester(first) else Modifier) { onFill(!fill) }
        OptionRow("Quality", "Auto") { }
        Spacer(Modifier.height(8.dp))
        OptionRow("Report a problem", "") { onReport() }
        Spacer(Modifier.weight(1f))
        Text("Press Back to close", style = ts(8.sp, 500, T.Muted))
    }
}

@Composable
fun OptionRow(label: String, value: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(34.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(7.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.04f), contentColor = T.Text, focusedContainerColor = Color.White, focusedContentColor = T.Bg),
        border = ClickableSurfaceDefaults.border(border = androidx.tv.material3.Border(androidx.compose.foundation.BorderStroke(0.5.dp, T.Hairline))),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = ts(10.sp, 600, if (enabled) Color.Unspecified else T.Muted), modifier = Modifier.weight(1f))
            Text(value, style = ts(9.sp, 500, Color.Unspecified.takeIf { enabled } ?: T.Muted))
        }
    }
}
