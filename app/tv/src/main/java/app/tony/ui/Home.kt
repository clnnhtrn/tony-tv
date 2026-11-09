package app.tony.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
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

@Composable
fun HomeScreen(s: AppState) {
    val favs = s.saved.favourites
    val firstChannel = s.countryRows.firstOrNull()?.channels?.firstOrNull { it.isWorking } ?: s.countryRows.firstOrNull()?.channels?.firstOrNull()
    var featuredId by rememberSaveable { mutableStateOf<String?>(null) }
    val featured: Channel? = favs.firstOrNull { it.id == featuredId } ?: favs.firstOrNull() ?: firstChannel
    val watchFocus = remember { FocusRequester() }
    if (featured != null) FocusOnStart(watchFocus)

    LazyColumn(Modifier.fillMaxSize().background(T.Bg), contentPadding = PaddingValues(bottom = 48.dp)) {
        item("hero") {
            // First screen = live hero + Favourites row over its bottom edge (Apple TV / Netflix layout)
            Box(Modifier.fillMaxWidth().height(540.dp)) {
                HeroBackdrop(featured, height = 540.dp)
                TopBar(Tab.Home, onTab = { s.tab(it.toScreen()) }, onSettings = { s.go(Screen.Settings) })
                if (featured != null) HeroText(s, featured, favs, watchFocus, Modifier.padding(start = T.Margin, top = 104.dp))
                Column(Modifier.align(Alignment.BottomStart).padding(bottom = 20.dp)) {
                    RowHeader("Favourites", favs.size.takeIf { it > 0 })
                    if (favs.isEmpty()) EmptyCard("Hold OK on any channel to add it here")
                    else LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(T.Gap)) {
                        items(favs, key = { it.id }) { c ->
                            ChannelTile(c, wide = true, sub = listOfNotNull(c.country, "Channel ${c.number}".takeIf { c.number > 0 }).joinToString(" · "),
                                onClick = { s.go(Screen.Details(c)) }, onLongClick = { s.toggleFavourite(c) }, onFocus = { featuredId = c.id })
                        }
                    }
                }
            }
        }
        if (s.saved.recent.isNotEmpty()) item("recent") { ChannelRowView(s, ChannelRow("recent", "Recent", null, s.saved.recent, 0), showCount = false) }
        s.countryRows.forEach { r -> item("c:" + r.key) { ChannelRowView(s, r) } }
        s.extraRows.filter { it.key != "movies" }.forEach { r -> item("x:" + r.key) { ChannelRowView(s, r, showCountry = true) } }
        if (s.offline && s.countryRows.isEmpty()) item("offline") { OfflineNotice(s) }
    }
}

/**
 * Full-bleed live backdrop: the channel's latest still, then its live picture (with sound) fading in,
 * softened into the page on the left (behind the text) and at the bottom, plus a faint glow in the
 * channel's own brand colour.
 */
@Composable
fun BoxScope.HeroBackdrop(c: Channel?, height: Dp, sound: Boolean = true) {
    val ctx = LocalContext.current
    val glow by animateColorAsState(parseColor(c?.tileColor, T.Bg).copy(alpha = 0.6f), tween(700), label = "glow")
    Box(Modifier.fillMaxWidth().height(height)) {
        AnimatedContent(c?.id, transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(400)) }, label = "still") { id ->
            Box(Modifier.fillMaxSize()) {
                if (c != null && id == c.id) {
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(T.Bg, parseColor(c.tileColor, T.Surface)))), contentAlignment = Alignment.CenterEnd) {
                        Box(Modifier.padding(end = 140.dp)) { LogoBadge(c, BadgeHero) }
                    }
                    AsyncImage(ImageRequest.Builder(ctx).data(Repository.stillUrl(c)).crossfade(400).build(), null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
        }
        LivePreview(c, Modifier.fillMaxSize(), sound = sound)
        // Fades: left (text side), bottom (into the page), top (behind the bar)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
            0f to T.Bg.copy(alpha = 0.98f), 0.34f to T.Bg.copy(alpha = 0.90f), 0.55f to T.Bg.copy(alpha = 0.45f), 0.78f to Color.Transparent)))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(height * 0.55f).background(Brush.verticalGradient(
            0f to Color.Transparent, 0.55f to T.Bg.copy(alpha = 0.85f), 1f to T.Bg)))
        Box(Modifier.fillMaxWidth().height(130.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent))))
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(glow.copy(alpha = 0.35f), Color.Transparent), center = Offset(0f, 1100f), radius = 1300f)))
    }
}

@Composable
private fun HeroText(s: AppState, featured: Channel, favs: List<Channel>, watchFocus: FocusRequester, modifier: Modifier) {
    Column(modifier.width(470.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedContent(featured, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) }, label = "heroText") { c ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { LiveChip(); OutlineChip(if (favs.any { it.id == c.id }) "FAVOURITE" else "FEATURED") }
                LogoBadge(c, BadgeHero)
                Text(c.name, style = ts(40.sp, 800, Color.White, (-1.2).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(listOfNotNull("Channel ${c.number}".takeIf { c.number > 0 }, c.country, c.category).joinToString("  ·  "), style = ts(10.5.sp, 500, T.Text2))
                    if (c.hd) OutlineChip("HD")
                }
                Text(describe(c), style = ts(11.sp, 400, T.Text2, lineHeight = 16.sp), maxLines = 2)
            }
        }
        // Buttons stay put (outside the animation) so focus never jumps while the hero changes
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TonyButton("Watch", { s.go(Screen.Watch(featured, favs.ifEmpty { s.allChannels() })) }, Modifier.focusRequester(watchFocus), icon = Icons.Play, primary = true)
            FavouriteButton(s, featured)
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

fun Tab.toScreen(): Screen = when (this) { Tab.Home -> Screen.Home; Tab.Movies -> Screen.Movies; Tab.Favourites -> Screen.Favourites; Tab.Search -> Screen.Search }

@Composable
fun ChannelRowView(s: AppState, r: ChannelRow, showCount: Boolean = true, showCountry: Boolean = false, wide: Boolean = false) {
    if (r.channels.isEmpty()) return
    Column(Modifier.padding(top = T.RowGap)) {
        RowHeader(r.title, if (showCount) (if (r.total > 0) r.total else r.channels.size) else null, r.subtitle)
        LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(T.Gap)) {
            items(r.channels, key = { r.key + it.id }) { c ->
                ChannelTile(c, wide = wide, favourite = s.isFavourite(c), sub = if (showCountry) c.country else null,
                    onClick = { s.go(Screen.Details(c)) }, onLongClick = { s.toggleFavourite(c) })
            }
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
