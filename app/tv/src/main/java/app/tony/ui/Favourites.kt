package app.tony.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.tv.material3.Text
import app.tony.AppState
import app.tony.Screen
import app.tony.data.Channel

@Composable
fun FavouritesScreen(s: AppState) {
    val favs = s.saved.favourites
    var menuFor by remember { mutableStateOf<Channel?>(null) }
    BackLayer(s, menuFor != null) { menuFor = null }
    val firstTile = remember { FocusRequester() }
    LaunchedEffect(favs.isNotEmpty(), menuFor) { if (favs.isNotEmpty() && menuFor == null) runCatching { firstTile.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Spacer(Modifier.height(BarSpace))
            Row(Modifier.padding(start = T.Margin, top = 22.dp, bottom = 14.dp), verticalAlignment = Alignment.Bottom) {
                Text("Favourites", style = ts(22.sp, 800, Color.White, (-0.5).sp))
                if (favs.isNotEmpty()) Text("   Hold OK on a channel to remove or move it", style = ts(9.5.sp, 400, T.Muted), modifier = Modifier.padding(bottom = 3.dp))
            }
            if (favs.isEmpty()) {
                EmptyCard("Hold OK on any channel to add it here")
            } else LazyVerticalGrid(
                GridCells.Fixed(4),
                contentPadding = PaddingValues(start = T.Margin, end = T.Margin, bottom = 40.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(T.Gap), verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(favs, key = { it.id }) { c ->
                    ChannelTile(c, wide = true, sub = c.country, modifier = if (c == favs.first()) Modifier.focusRequester(firstTile) else Modifier, onClick = { s.go(Screen.Details(c)) }, onLongClick = { menuFor = c })
                }
            }
        }
        menuFor?.let { c -> FavMenu(c, onRemove = { s.toggleFavourite(c); menuFor = null }, onMove = { by -> s.moveFavourite(c, by); menuFor = null }, onClose = { menuFor = null }) }
    }
}

@Composable
private fun FavMenu(c: Channel, onRemove: () -> Unit, onMove: (Int) -> Unit, onClose: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(c.id) { runCatching { first.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(260.dp).clip(RoundedCornerShape(12.dp)).background(T.Panel).border(0.5.dp, T.HairlineStrong, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                LogoBadge(c, BadgeStd); Text(c.name, style = ts(12.sp, 700, Color.White))
            }
            OptionRow("Remove from Favourites", "", Modifier.focusRequester(first)) { onRemove() }
            OptionRow("Move earlier", "") { onMove(-1) }
            OptionRow("Move later", "") { onMove(1) }
            OptionRow("Cancel", "") { onClose() }
        }
    }
}
