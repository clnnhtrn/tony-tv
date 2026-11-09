package app.tony.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.tony.AppState
import app.tony.Screen

@Composable
fun SearchScreen(s: AppState) {
    var q by rememberSaveable { mutableStateOf("") }
    val firstKey = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstKey.requestFocus() } }
    val needle = q.trim().lowercase()
    val channels = remember(needle, s.countryRows, s.extraRows) {
        if (needle.isEmpty()) emptyList() else s.allChannels().filter { it.name.lowercase().contains(needle) }
            .sortedBy { if (it.name.lowercase().startsWith(needle)) 0 else 1 }.take(60)
    }
    val films = remember(needle, s.allFilms) {
        if (needle.length < 2) emptyList() else s.allFilms.filter { it.title.lowercase().contains(needle) }.take(30)
    }

    Column(Modifier.fillMaxSize().background(T.Bg)) {
        TopBar(Tab.Search, onTab = { s.tab(it.toScreen()) }, onSettings = { s.go(Screen.Settings) })
        Row(Modifier.fillMaxSize().padding(top = 22.dp)) {
            // Left: query + letter grid (6 columns: far fewer presses than one long row)
            Column(Modifier.padding(start = T.Margin).width(276.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(7.dp)).background(Color.White.copy(alpha = 0.05f))
                    .border(0.5.dp, T.HairlineStrong, RoundedCornerShape(7.dp)).padding(horizontal = 11.dp), contentAlignment = Alignment.CenterStart) {
                    Text(if (q.isEmpty()) "Type a channel or film name" else q + "▏", style = ts(12.sp, if (q.isEmpty()) 400 else 600, if (q.isEmpty()) T.Muted else Color.White))
                }
                val keys = ("abcdefghijklmnopqrstuvwxyz1234").map { it.toString() }
                keys.chunked(6).forEachIndexed { r, rowKeys ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowKeys.forEachIndexed { i, k -> Key(k, Modifier.width(42.dp).then(if (r == 0 && i == 0) Modifier.focusRequester(firstKey) else Modifier)) { q += k } }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Key("Space", Modifier.width(88.dp)) { q += " " }
                    Key("Delete", Modifier.width(88.dp)) { q = q.dropLast(1) }
                    Key("Clear", Modifier.width(88.dp)) { q = "" }
                }
                Text("Tip: hold the mic button on the remote and say a channel name.", style = ts(8.5.sp, 400, T.Muted), modifier = Modifier.padding(top = 6.dp))
            }
            // Right: results
            LazyVerticalGrid(
                GridCells.Fixed(3), Modifier.padding(start = 28.dp, end = T.Margin),
                contentPadding = PaddingValues(bottom = 40.dp, top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(T.Gap), verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (needle.isEmpty()) item(span = { GridItemSpan(3) }) {
                    Text("Start typing. Results appear here.", style = ts(10.5.sp, 400, T.Muted))
                } else if (channels.isEmpty() && films.isEmpty()) item(span = { GridItemSpan(3) }) {
                    Text("Nothing found for \"$q\". Try fewer letters.", style = ts(10.5.sp, 400, T.Muted))
                }
                if (channels.isNotEmpty()) item(span = { GridItemSpan(3) }) { Text("Channels", style = ts(11.sp, 700)) }
                items(channels, key = { "c" + it.id }) { c ->
                    ChannelTile(c, wide = false, favourite = s.isFavourite(c), sub = c.country, onClick = { s.go(Screen.Details(c)) }, onLongClick = { s.toggleFavourite(c) })
                }
                if (films.isNotEmpty()) item(span = { GridItemSpan(3) }) { Text("Classic films", style = ts(11.sp, 700), modifier = Modifier.padding(top = 8.dp)) }
                items(films, key = { "f" + it.id }) { f -> FilmTile(f) { s.go(Screen.FilmDetails(f, films)) } }
            }
        }
    }
}

@Composable
private fun Key(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(30.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = T.Text2, focusedContainerColor = Color.White, focusedContentColor = T.Bg),
        border = ClickableSurfaceDefaults.border(border = androidx.tv.material3.Border(androidx.compose.foundation.BorderStroke(0.5.dp, T.Hairline))),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
    ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label, style = ts(if (label.length == 1) 11.sp else 9.sp, 600, Color.Unspecified)) } }
}
