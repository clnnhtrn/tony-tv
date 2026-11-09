package app.tony.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.tony.AppState
import app.tony.Screen

/** All channels of one country at a time: country pills on top, a grid you scroll down. */
@Composable
fun ChannelsScreen(s: AppState, start: String?) {
    val groups = s.countryRows + s.extraRows.filter { it.key != "movies" }
    var picked by rememberSaveable { mutableStateOf(start ?: groups.firstOrNull()?.title) }
    val row = groups.firstOrNull { it.title == picked } ?: groups.firstOrNull()
    val firstPill = remember { FocusRequester() }
    FocusOnStart(firstPill)
    val grid = rememberLazyGridState()
    LaunchedEffect(picked) { grid.scrollToItem(0) }
    Column(Modifier.fillMaxSize().pageBg()) {
        androidx.compose.foundation.layout.Spacer(Modifier.height(BarSpace))
        LazyRow(Modifier.padding(top = 18.dp).focusRestorer(), contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(groups, key = { it.key }) { g ->
                val src = remember { MutableInteractionSource() }
                val f by src.collectIsFocusedAsState()
                val sel = g.title == row?.title
                Surface(
                    onClick = { picked = g.title },
                    modifier = (if (g.title == (start ?: groups.first().title)) Modifier.focusRequester(firstPill) else Modifier)
                        .onFocusChanged { if (it.isFocused) picked = g.title },
                    shape = ClickableSurfaceDefaults.shape(CircleShape),
                    colors = ClickableSurfaceDefaults.colors(containerColor = if (sel) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.07f), focusedContainerColor = Color.White),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                    interactionSource = src,
                ) {
                    Text("${g.title}  ${g.channels.size}", style = ts(12.sp, 650, if (f) T.Bg else Color.White),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }
        if (row != null) androidx.compose.runtime.CompositionLocalProvider(LocalInGrid provides true) { LazyVerticalGrid(
            GridCells.Fixed(5), Modifier.fillMaxSize(), state = grid,
            contentPadding = PaddingValues(start = T.Margin, end = T.Margin, top = 22.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // In channel-number order, like a TV guide; the number shows after "Live ·"
            val byNumber = row.channels.sortedBy { if (it.number > 0) it.number else Int.MAX_VALUE }
            val (up, down) = byNumber.partition { !it.isDown }
            items(up, key = { row.key + it.id }) { c ->
                ChannelTile(c, wide = false, favourite = s.isFavourite(c), sub = if (c.number > 0) "Channel ${c.number}" else null, onClick = { s.go(Screen.Details(c)) }, onLongClick = { s.toggleFavourite(c) })
            }
            // Channels that don't play right now: kept (nothing is hidden), at the end, under their own heading
            if (down.isNotEmpty()) item("down-h", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(5) }) {
                Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Not working right now", style = ts(14.sp, 750, Color.White.copy(alpha = 0.9f)))
                    Text("Tony checks these again every 20 minutes", style = ts(9.sp, 500, Color.White.copy(alpha = 0.5f)), modifier = Modifier.padding(bottom = 2.dp))
                }
            }
            items(down, key = { row.key + it.id }) { c ->
                ChannelTile(c, wide = false, favourite = s.isFavourite(c), sub = if (c.number > 0) "Channel ${c.number}" else null, onClick = { s.go(Screen.Details(c)) }, onLongClick = { s.toggleFavourite(c) })
            }
        } }
    }
}
