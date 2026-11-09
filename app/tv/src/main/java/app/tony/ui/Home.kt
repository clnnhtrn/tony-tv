package app.tony.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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

@Composable
fun HomeScreen(s: AppState) {
    val favs = s.saved.favourites
    val firstChannel = s.countryRows.firstOrNull()?.channels?.firstOrNull { it.isWorking } ?: s.countryRows.firstOrNull()?.channels?.firstOrNull()
    var featuredId by rememberSaveable { mutableStateOf<String?>(null) }
    val featured: Channel? = favs.firstOrNull { it.id == featuredId } ?: favs.firstOrNull() ?: firstChannel
    val watchFocus = remember { FocusRequester() }
    if (featured != null) FocusOnStart(watchFocus)

    LazyColumn(Modifier.fillMaxSize().background(T.Bg), contentPadding = PaddingValues(bottom = 40.dp)) {
        item("hero") { Hero(s, featured, favs, watchFocus) }
        item("div") { Box(Modifier.padding(start = T.Margin).fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.07f))) }
        item("favs") {
            Spacer(Modifier.height(14.dp))
            RowHeader("Favourites", favs.size)
            if (favs.isEmpty()) EmptyCard("Hold OK on any channel to add it here")
            else LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(T.Gap)) {
                items(favs, key = { it.id }) { c ->
                    ChannelTile(c, wide = true, sub = listOfNotNull(c.country, "Channel ${c.number}".takeIf { c.number > 0 }).joinToString(" · "),
                        onClick = { s.go(Screen.Details(c)) },
                        onLongClick = { s.toggleFavourite(c) },
                        onFocus = { featuredId = c.id })
                }
            }
        }
        if (s.saved.recent.isNotEmpty()) item("recent") {
            ChannelRowView(s, ChannelRow("recent", "Recent", null, s.saved.recent, 0), showCount = false)
        }
        s.countryRows.forEach { r -> item("c:" + r.key) { ChannelRowView(s, r) } }
        s.extraRows.forEach { r -> item("x:" + r.key) { ChannelRowView(s, r, showCountry = true) } }
        if (s.loading && s.countryRows.isEmpty()) item("loading") { LoadingRow() }
        if (s.offline && s.countryRows.isEmpty()) item("offline") { OfflineNotice(s) }
    }
}

@Composable
private fun Hero(s: AppState, featured: Channel?, favs: List<Channel>, watchFocus: FocusRequester) {
    // A little colour: a soft glow in the featured channel's own brand colour (from channels.json tileColor).
    val glow by animateColorAsState(parseColor(featured?.tileColor, T.Bg).copy(alpha = 0.55f), tween(600), label = "glow")
    Box(Modifier.fillMaxWidth().height(314.dp)) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(glow, Color.Transparent), center = androidx.compose.ui.geometry.Offset(260f, 900f), radius = 1300f)))
        if (featured != null) Box(Modifier.align(Alignment.TopEnd).width(670.dp).height(314.dp)) {
            // Designed fallback (no still yet / preview not playing): the channel's logo, large, on its brand colour
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Transparent, parseColor(featured.tileColor, T.Surface).copy(alpha = 0.9f)))),
                contentAlignment = Alignment.Center) { Box(Modifier.padding(start = 160.dp)) { LogoBadge(featured, BadgeHero) } }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(Repository.stillUrl(featured)).crossfade(true).build(),
                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
            LivePreview(featured, Modifier.fillMaxSize())
        }
        // Fades so the text always reads, whatever is playing (the only gradients in Tony)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to T.Bg, 0.27f to T.Bg.copy(alpha = 0.6f), 0.43f to T.Bg.copy(alpha = 0.55f), 0.65f to T.Bg.copy(alpha = 0.2f), 0.8f to Color.Transparent)))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(160.dp).background(Brush.verticalGradient(listOf(Color.Transparent, T.Bg.copy(alpha = 0.86f), T.Bg))))
        Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.verticalGradient(listOf(T.Bg.copy(alpha = 0.92f), T.Bg.copy(alpha = 0.55f), Color.Transparent))))

        TopBar(Tab.Home, onTab = { s.tab(it.toScreen()) }, onSettings = { s.go(Screen.Settings) })

        if (featured != null) Column(Modifier.padding(start = T.Margin, top = 98.dp).width(420.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { LiveChip(); OutlineChip(if (favs.any { it.id == featured.id }) "FAVOURITE" else "FEATURED") }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                LogoBadge(featured, BadgeHero)
                Text(featured.name, style = ts(38.sp, 800, Color.White, (-1.1).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(listOfNotNull("Channel ${featured.number}".takeIf { featured.number > 0 }, featured.country, featured.category).joinToString("  ·  "), style = ts(9.5.sp, 500, T.Text3))
                if (featured.hd) OutlineChip("HD")
            }
            Text(describe(featured), style = ts(10.5.sp, 400, T.Text2, lineHeight = 15.sp), maxLines = 2)
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                TonyButton("Watch", { s.go(Screen.Watch(featured, favs.ifEmpty { s.allChannels() })) }, Modifier.focusRequester(watchFocus), icon = Icons.Play, primary = true)
                FavouriteButton(s, featured)
            }
            if (favs.size > 1) Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                favs.take(8).forEach { c -> Box(Modifier.size(if (c.id == featured.id) 13.dp else 4.dp, 2.dp).clip(RoundedCornerShape(1.dp)).background(if (c.id == featured.id) Color.White else Color.White.copy(alpha = 0.28f))) }
            }
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

fun Tab.toScreen(): Screen = when (this) { Tab.Home -> Screen.Home; Tab.Favourites -> Screen.Favourites; Tab.Search -> Screen.Search }

@Composable
fun ChannelRowView(s: AppState, r: ChannelRow, showCount: Boolean = true, showCountry: Boolean = false) {
    if (r.channels.isEmpty()) return
    Column(Modifier.padding(top = T.RowGap)) {
        RowHeader(r.title, if (showCount) (if (r.total > 0) r.total else r.channels.size) else null, r.subtitle)
        LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(T.Gap)) {
            items(r.channels, key = { r.key + it.id }) { c ->
                ChannelTile(c, wide = false, favourite = s.isFavourite(c), sub = if (showCountry) c.country else null,
                    onClick = { s.go(Screen.Details(c)) }, onLongClick = { s.toggleFavourite(c) })
            }
        }
    }
}

@Composable
fun LoadingRow() {
    Column(Modifier.padding(top = T.RowGap)) {
        Box(Modifier.padding(start = T.Margin, bottom = 9.dp).size(90.dp, 10.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.06f)))
        Row(Modifier.padding(start = T.Margin), horizontalArrangement = Arrangement.spacedBy(T.Gap)) {
            repeat(6) { Box(Modifier.size(T.StdW, T.StdH).clip(RoundedCornerShape(T.RadiusStd)).background(Color.White.copy(alpha = 0.05f))) }
        }
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
