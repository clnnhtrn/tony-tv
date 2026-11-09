package app.tony.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.tony.AppState
import app.tony.ChannelRow
import app.tony.Screen
import app.tony.data.Channel
import app.tony.data.Repository
import app.tony.player.LivePreview
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations

/** v8 page background: navy-slate gradient. */
/** The window already paints the navy gradient (res/drawable/window_bg.xml); screens add no extra layer. */
fun Modifier.pageBg() = this

@Composable
fun HomeScreen(s: AppState) {
    val favs = s.saved.favourites
    val firstChannel = s.countryRows.firstOrNull()?.let { s.topFor(it, 1).firstOrNull() ?: it.channels.firstOrNull() }
    var featuredId by rememberSaveable { mutableStateOf<String?>(null) }
    val featured: Channel? = favs.firstOrNull { it.id == featuredId } ?: favs.firstOrNull() ?: firstChannel
    val watchFocus = remember { FocusRequester() }
    if (featured != null) FocusOnStart(watchFocus)

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    ReportScroll(s, listState)
    var heroFocused by remember { mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(heroFocused) { if (heroFocused && listState.firstVisibleItemScrollOffset > 0) listState.animateScrollToItem(0) }
    LazyColumn(Modifier.fillMaxSize().pageBg(), state = listState, contentPadding = PaddingValues(bottom = 110.dp)) {
        item("hero") {
            Box(Modifier.fillMaxWidth().height(540.dp).onFocusChanged { heroFocused = it.hasFocus }) {
                HeroBackdrop(featured, height = 540.dp, live = heroFocused)
                if (featured != null) HeroInfo(s, featured, Modifier.padding(start = T.Margin, top = 132.dp), watchFocus,
                    onWatch = { s.go(Screen.Watch(featured, favs.ifEmpty { s.allChannels() })) })
                if (favs.size > 1) PageDots(favs.size.coerceAtMost(8), favs.indexOfFirst { it.id == featured?.id }.coerceAtLeast(0), Modifier.align(Alignment.TopCenter).padding(top = 370.dp))
                Column(Modifier.align(Alignment.BottomStart).padding(bottom = 18.dp)) {
                    RowHeader("Favourites")
                    if (favs.isEmpty()) EmptyCard("Hold OK on any channel to add it here")
                    else LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(T.RowGapX)) {
                        items(favs, key = { it.id }) { c ->
                            ChannelTile(c, wide = true, sub = c.country, onClick = { s.go(Screen.Details(c)) },
                                onLongClick = { s.toggleFavourite(c) }, onFocus = { featuredId = c.id })
                        }
                    }
                }
            }
        }
        if (s.saved.recent.isNotEmpty()) item("recent") { ChannelRowView(s, ChannelRow("recent", "Recent", null, s.saved.recent, 0), wide = true) }
        // One "Top in …" row for every country the family chose (no country is special)
        s.countryRows.forEach { r ->
            val top = s.topFor(r)
            if (top.isNotEmpty()) item("top:" + r.key) { PosterRow(s, "Top in ${r.title}", top, seeAll = { s.go(Screen.Channels(r.title)) }, total = r.channels.size) }
        }
        s.extraRows.filter { it.key != "movies" }.forEach { r ->
            item("x:" + r.key) { ChannelRowView(s, r.copy(channels = r.channels.take(15)), showCountry = true, seeAll = { s.go(Screen.Channels(r.title)) }) }
        }
        item("cats") { CategoryRow(s) }
        if (s.offline && s.countryRows.isEmpty()) item("offline") { OfflineNotice(s) }
    }
}

@Composable
fun PageDots(n: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.28f)).padding(horizontal = 7.dp, vertical = 4.5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(n) { i -> Box(Modifier.size(5.dp).clip(CircleShape).background(Color.White.copy(alpha = if (i == current) 1f else 0.35f))) }
    }
}

@Composable
fun PosterRow(s: AppState, title: String, list: List<Channel>, seeAll: (() -> Unit)? = null, total: Int = 0) {
    Column(Modifier.padding(top = T.RowGap + 6.dp)) {
        RowHeader(title, modifier = Modifier.padding(bottom = 6.dp))
        LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(start = T.Margin, end = T.Margin, top = 6.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(T.RowGapX)) {
            itemsIndexed(list, key = { _, c -> "top" + c.id }) { i, c ->
                PosterTile(c, i + 1, onClick = { s.go(Screen.Details(c)) }, onLongClick = { s.toggleFavourite(c) })
            }
            if (seeAll != null) item("seeall") {
                TonyCard(PosterW, PosterH, 11.dp, onClick = seeAll, maxScale = 1.06f, label = "See all") { _ ->
                    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("See all", style = ts(16.sp, 800, Color.White))
                            if (total > 0) Text("$total channels", style = ts(9.sp, 600, Color.White.copy(alpha = 0.6f)))
                        }
                    }
                }
            }
        }
    }
}

private val CATEGORIES = listOf("News" to Color(0xFF2B3A67), "Movies" to Color(0xFF5A2340), "Kids" to Color(0xFF6A4A1C),
    "Music" to Color(0xFF3E2A66), "Sport" to Color(0xFF1F4E5A), "Documentary" to Color(0xFF2F4D3A))

/** Netflix-style category tiles. */
@Composable
fun CategoryRow(s: AppState) {
    Column(Modifier.padding(top = T.RowGap)) {
        RowHeader("Browse")
        LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(T.RowGapX)) {
            items(CATEGORIES, key = { it.first }) { (name, tint) ->
                TonyCard(127.dp, 62.dp, 11.dp, onClick = { s.go(Screen.Category(name)) }) { _ ->
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(tint, T.Card))), contentAlignment = Alignment.Center) {
                        Text(name.uppercase(), style = ts(17.sp, 900, Color.White.copy(alpha = 0.95f), 0.8.sp))
                    }
                }
            }
        }
    }
}

/**
 * Full-bleed live backdrop: latest frame, then the live picture fading in. Both are anchored to the
 * top and taller than the screen so the channel's ticker (bottom of the frame) falls off-screen.
 */
@Composable
fun BoxScope.HeroBackdrop(c: Channel?, height: Dp, sound: Boolean = true, live: Boolean = true) {
    val ctx = LocalContext.current
    Box(Modifier.fillMaxWidth().height(height).clipToBounds()) {
        AnimatedContent(c?.id, transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(400)) }, label = "still") { id ->
            Box(Modifier.fillMaxSize()) {
                if (c != null && id == c.id) {
                    var ok by remember(c.id) { mutableStateOf(false) }
                    if (!ok) Box(Modifier.fillMaxSize().background(parseColor(c.tileColor, T.Card)), contentAlignment = Alignment.CenterEnd) {
                        Box(Modifier.padding(end = 150.dp)) { LogoBadge(c, BadgeHero) }
                    }
                    AsyncImage(ImageRequest.Builder(ctx).data(Repository.stillUrl(c)).size(960, 540).transformations(HeroArt(960, 540)).crossfade(400).build(), null,
                        onSuccess = { ok = true }, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                }
            }
        }
        LivePreview(c.takeIf { live }, Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true).height(height * 1.18f), sound = sound)
        // Fade layers are only needed over moving video (the still has them baked in)
        val p = app.tony.player.LocalPlayers.current.preview
        val videoOn = live && p.state == app.tony.player.StreamState.Playing && !p.isRadio
        if (videoOn) {
            Box(Modifier.fillMaxWidth(0.8f).fillMaxSize().background(Brush.horizontalGradient(*easedFade(T.Ink, 0.95f, 0f))))
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(height * 0.6f).background(Brush.verticalGradient(*easedFade(T.BgTop, 0f, 1f, 0f, 0.85f))))
            Box(Modifier.fillMaxWidth().height(130.dp).background(Brush.verticalGradient(listOf(T.Ink.copy(alpha = 0.9f), T.Ink.copy(alpha = 0.45f), Color.Transparent))))
        }
    }
}

/** v8 hero text: big name, one quiet line, one sentence, Watch + ♡ + ⓘ. */
@Composable
fun HeroInfo(s: AppState, c: Channel, modifier: Modifier, watchFocus: FocusRequester, onWatch: () -> Unit, showInfo: Boolean = true, watchLabel: String = "Watch") {
    Column(modifier.width(470.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedContent(c, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) }, label = "heroText") { ch ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(ch.title, style = ts(if (ch.title.length > 14) 42.sp else 54.sp, 850, Color.White, (-1.8).sp, lineHeight = 52.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    LogoMark(ch, size = 20.dp)
                    if (!ch.isRadio) Box(Modifier.size(4.5.dp).clip(CircleShape).background(T.Live))
                    Text(listOfNotNull(if (ch.isRadio) "Radio" else "Live", "Channel ${ch.number}".takeIf { ch.number > 0 }, ch.country, ch.category).joinToString("  ·  "),
                        style = ts(11.sp, 600, Color.White.copy(alpha = 0.85f)))
                }
                Text(describe(ch), style = ts(12.sp, 400, Color.White.copy(alpha = 0.72f), lineHeight = 17.sp), maxLines = 2)
            }
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TonyButton(watchLabel, onWatch, Modifier.focusRequester(watchFocus), icon = Icons.Play, primary = true)
            val fav = s.isFavourite(c)
            RoundButton(if (fav) Icons.Heart else Icons.HeartOutline, if (fav) "Favourited" else "Add to Favourites", { s.toggleFavourite(c) }, tint = if (fav) T.Accent else null)
        }
    }
}

@Composable
fun FavouriteButton(s: AppState, c: Channel, modifier: Modifier = Modifier) {
    val fav = s.isFavourite(c)
    TonyButton(if (fav) "Favourited" else "Add to Favourites", { s.toggleFavourite(c) }, modifier,
        icon = if (fav) Icons.Heart else Icons.HeartOutline, iconTint = if (fav) T.Accent else null)
}

fun describe(c: Channel): String = when {
    c.isRadio -> "Radio from ${c.country ?: "around the world"}. Sound only."
    c.isYouTube -> "Live on YouTube. Opens in the YouTube app."
    c.category == "News" -> "Live news from ${c.country ?: "around the world"}."
    c.category == "Movies" -> "Films around the clock."
    c.category == "Kids" -> "Shows for children."
    c.category == "Sport" -> "Live sport and sports news."
    c.category == "Music" -> "Music videos and shows."
    c.category == "Religion" -> "Religious programmes and services."
    c.category == "Documentary" -> "Documentaries and factual shows."
    else -> "Live TV from ${c.country ?: "around the world"}."
}

fun Tab.toScreen(): Screen = when (this) { Tab.Home -> Screen.Home; Tab.Channels -> Screen.Channels(); Tab.Movies -> Screen.Movies; Tab.Favourites -> Screen.Favourites; Tab.Search -> Screen.Search }

@Composable
fun ChannelRowView(s: AppState, r: ChannelRow, showCount: Boolean = true, showCountry: Boolean = false, wide: Boolean = false, seeAll: (() -> Unit)? = null) {
    if (r.channels.isEmpty()) return
    Column(Modifier.padding(top = T.RowGap)) {
        RowHeader(r.title)
        LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(T.RowGapX)) {
            items(r.channels, key = { r.key + it.id }) { c ->
                ChannelTile(c, wide = wide, favourite = s.isFavourite(c), sub = if (showCountry) c.country else null,
                    onClick = { s.go(Screen.Details(c)) }, onLongClick = { s.toggleFavourite(c) })
            }
            if (seeAll != null) item("seeall" + r.key) {
                TonyCard(CardStdW, CardStdH, 10.dp, onClick = seeAll) { _ ->
                    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("See all", style = ts(14.sp, 800, Color.White))
                            Text("${r.total} channels", style = ts(8.5.sp, 600, Color.White.copy(alpha = 0.6f)))
                        }
                    }
                }
            }
        }
    }
}

/** Grid page for a category tile. */
@Composable
fun CategoryScreen(s: AppState, name: String) {
    val list = s.category(name)
    val first = remember { FocusRequester() }
    if (list.isNotEmpty()) FocusOnStart(first)
    Column(Modifier.fillMaxSize().pageBg()) {
        androidx.compose.foundation.layout.Spacer(Modifier.height(BarSpace))
    androidx.compose.runtime.CompositionLocalProvider(LocalInGrid provides true) { androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        androidx.compose.foundation.lazy.grid.GridCells.Fixed(4), Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = T.Margin, end = T.Margin, top = 18.dp, bottom = 110.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                Text(name, style = ts(30.sp, 850, Color.White, (-0.8).sp))
                Text("Browse  ·  Back to return", style = ts(10.sp, 600, Color.White.copy(alpha = 0.5f)), modifier = Modifier.padding(bottom = 6.dp))
            }
        }
        items(list.size) { i ->
            val c = list[i]
            ChannelTile(c, wide = true, sub = c.country, favourite = s.isFavourite(c), modifier = if (i == 0) Modifier.focusRequester(first) else Modifier,
                onClick = { s.go(Screen.Details(c)) }, onLongClick = { s.toggleFavourite(c) })
        }
    } }
    }
}

@Composable
fun OfflineNotice(s: AppState) {
    Column(Modifier.padding(start = T.Margin, top = 30.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tony can't reach the internet", style = ts(14.sp, 700))
        Text("Check the TV's Wi-Fi, then try again.", style = ts(10.sp, 400, T.Text3))
        TonyButton("Try again", { s.reload() }, primary = true)
    }
}
