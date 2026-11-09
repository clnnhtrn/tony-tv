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
import androidx.compose.foundation.layout.size
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
    FocusOnStart(firstKey)
    val needle = q.trim().lowercase()
    val channels = remember(needle, s.countryRows, s.extraRows) {
        if (needle.isEmpty()) emptyList() else s.allChannels().filter { it.title.lowercase().contains(needle) }
            .sortedBy { if (it.name.lowercase().startsWith(needle)) 0 else 1 }.take(60)
    }

    Column(Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Spacer(Modifier.height(BarSpace))
        Row(Modifier.fillMaxSize().padding(top = 22.dp)) {
            // Left: query + letter grid (6 columns: far fewer presses than one long row)
            Column(Modifier.padding(start = T.Margin).width(282.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // v8: big query line with an underline, no box
                Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (q.isEmpty()) "Search channels" else q, style = ts(24.sp, 800, if (q.isEmpty()) Color.White.copy(alpha = 0.35f) else Color.White, (-0.6).sp), maxLines = 1)
                    Box(Modifier.padding(start = 2.dp).size(2.dp, 26.dp).background(T.Accent))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.25f)))
                Box(Modifier.height(8.dp))
                val keys = ("abcdefghijklmnopqrstuvwxyz1234").map { it.toString() }
                keys.chunked(6).forEachIndexed { r, rowKeys ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowKeys.forEachIndexed { i, k -> Key(k, Modifier.width(42.dp).then(if (r == 0 && i == 0) Modifier.focusRequester(firstKey) else Modifier)) { q += k } }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Key("Space", Modifier.width(90.dp)) { q += " " }
                    Key("Delete", Modifier.width(90.dp)) { q = q.dropLast(1) }
                    Key("Clear", Modifier.width(90.dp)) { q = "" }
                }
                Text("Or hold the mic button on the remote and say a channel.", style = ts(9.5.sp, 400, Color.White.copy(alpha = 0.5f)), modifier = Modifier.padding(top = 10.dp))
            }
            // Right: results
            LazyVerticalGrid(
                GridCells.Fixed(3), Modifier.padding(start = 36.dp, end = T.Margin),
                contentPadding = PaddingValues(bottom = 60.dp, top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (needle.isEmpty()) item(span = { GridItemSpan(3) }) {
                    Text("Start typing. Results appear here.", style = ts(12.sp, 500, Color.White.copy(alpha = 0.45f)))
                } else if (channels.isEmpty()) item(span = { GridItemSpan(3) }) {
                    Text("Nothing found for \"$q\". Try fewer letters.", style = ts(12.sp, 500, Color.White.copy(alpha = 0.45f)))
                }
                if (channels.isNotEmpty()) item(span = { GridItemSpan(3) }) { Text("Channels", style = ts(14.sp, 700, Color.White.copy(alpha = 0.92f))) }
                items(channels, key = { "c" + it.id }) { c ->
                    ChannelTile(c, wide = false, favourite = s.isFavourite(c), sub = c.country, onClick = { s.go(Screen.Details(c)) }, onLongClick = { s.toggleFavourite(c) })
                }
            }
        }
    }
}

@Composable
private fun Key(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = Color.White.copy(alpha = 0.85f), focusedContainerColor = Color.White, focusedContentColor = T.Bg),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
    ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label, style = ts(if (label.length == 1) 13.sp else 10.sp, 600, Color.Unspecified)) } }
}
