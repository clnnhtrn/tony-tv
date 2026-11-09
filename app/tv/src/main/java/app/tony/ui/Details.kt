package app.tony.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.tony.AppState
import app.tony.Screen
import app.tony.data.Channel

/** Channel page: the live channel fills the screen and fades into the text (Apple TV / Netflix style). */
@Composable
fun DetailsScreen(s: AppState, c: Channel) {
    val ctx = LocalContext.current
    val watchFocus = remember { FocusRequester() }
    FocusOnStart(watchFocus, c.id)
    val more = (s.countryRows.firstOrNull { r -> r.channels.any { it.id == c.id } }?.channels
        ?: s.allChannels().filter { it.country == c.country }).filter { it.id != c.id }.take(30)
    val list = listOf(c) + more

    Box(Modifier.fillMaxSize().pageBg()) {
        HeroBackdrop(c, height = 540.dp)
        Column(Modifier.padding(start = T.Margin, top = 78.dp)) {
            HeroInfo(s, c, Modifier, watchFocus, showInfo = false, watchLabel = if (c.isYouTube) "Open in YouTube" else "Watch", onWatch = {
                if (c.isYouTube) runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(c.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    .onFailure { s.showToast("YouTube isn't installed on this TV") }
                else s.go(Screen.Watch(c, list))
            })
            val note = statusLine(c).trim()
            if (note.isNotEmpty()) Text(note, style = ts(10.sp, 500, Color.White.copy(alpha = 0.55f)), modifier = Modifier.padding(top = 10.dp).width(470.dp))
        }
        if (more.isNotEmpty()) Column(Modifier.align(Alignment.BottomStart).padding(bottom = 20.dp)) {
            RowHeader("More from ${c.country ?: "here"}")
            LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                items(more, key = { it.id }) { m ->
                    ChannelTile(m, wide = true, favourite = s.isFavourite(m), onClick = { s.replaceTop(Screen.Details(m)) }, onLongClick = { s.toggleFavourite(m) })
                }
            }
        }
    }
}

private fun statusLine(c: Channel) = when (c.status) {
    "broken" -> " It wasn't working at the last check, but Tony will try."
    "geoblocked" -> " The station may only allow viewers in ${c.country ?: "its country"}."
    "web" -> " This channel only plays on its own website."
    else -> if (c.notAlwaysOn) " Not on air all day." else ""
}
