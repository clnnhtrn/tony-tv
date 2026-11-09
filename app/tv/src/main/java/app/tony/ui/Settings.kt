package app.tony.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.tony.AppState
import app.tony.BuildConfig
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private enum class Section(val label: String, val icon: ImageVector) {
    Countries("Countries", Icons.Globe), Channels("Channels", Icons.Grid), Updates("Channel updates", Icons.Refresh), About("About Tony", Icons.Info)
}

@Composable
fun SettingsScreen(s: AppState) {
    var sec by rememberSaveable { mutableStateOf(Section.Countries) }
    var editingCountries by remember { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    FocusOnStart(first)
    BackLayer(s, editingCountries) { editingCountries = false }
    if (editingCountries) { WelcomeScreen(s, fromSettings = true); return }

    Row(Modifier.fillMaxSize()) {
        // Left menu
        Column(Modifier.width(250.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.03f)).border(0.5.dp, T.Hairline)
            .padding(start = 30.dp, end = 16.dp, top = 34.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Wordmark(20f)
            Text("Settings", style = ts(26.sp, 800, Color.White, (-0.7).sp), modifier = Modifier.padding(top = 22.dp, bottom = 16.dp))
            Section.entries.forEachIndexed { i, x -> MenuItem(x.label, x.icon, x == sec, if (i == 0) Modifier.focusRequester(first) else Modifier) { sec = x } }
            Box(Modifier.weight(1f))
            Text("Press Back to return", style = ts(8.5.sp, 500, T.Muted), modifier = Modifier.padding(bottom = 26.dp))
        }
        // Content
        AnimatedContent(sec, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "settings",
            modifier = Modifier.fillMaxSize().padding(start = 40.dp, end = T.Margin, top = 34.dp)) { x ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(x.label, style = ts(22.sp, 800, Color.White, (-0.5).sp), modifier = Modifier.padding(top = 46.dp))
                when (x) {
                    Section.Countries -> CountriesSection(s) { editingCountries = true }
                    Section.Channels -> ChannelsSection(s)
                    Section.Updates -> UpdatesSection(s)
                    Section.About -> AboutSection()
                }
            }
        }
    }
}

@Composable
private fun CountriesSection(s: AppState, onChange: () -> Unit) {
    Text("Each country gets its own row on Home, in this order.", style = ts(11.sp, 400, T.Text2))
    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        s.saved.countries.mapNotNull { f -> s.index?.countries?.firstOrNull { it.file == f } }.forEach { e ->
            Column(Modifier.width(150.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.06f)).border(0.5.dp, T.Hairline, RoundedCornerShape(14.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(e.flag, style = ts(26.sp))
                Text(e.name, style = ts(12.sp, 700, Color.White))
                Text(plural(e.channels, "channel"), style = ts(9.sp, 500, T.Muted))
            }
        }
    }
    Row(Modifier.padding(top = 12.dp)) { TonyButton("Change countries", onChange, primary = true) }
}

/** Channels: a grid of tiles. OK hides or shows a channel; hidden ones fade out with a "Hidden" tag. */
@Composable
private fun ChannelsSection(s: AppState) {
    Text("Press OK on a channel you never watch to hide it from Home. Press OK again to bring it back.", style = ts(11.sp, 400, T.Text2))
    LazyVerticalGrid(GridCells.Fixed(4), contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp, start = 6.dp, end = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        s.countryRowsAll.forEach { r ->
            item("h" + r.key, span = { GridItemSpan(4) }) { Text(r.title, style = ts(12.sp, 700, T.Text2), modifier = Modifier.padding(top = 6.dp)) }
            items(r.channels, key = { r.key + it.id }) { c ->
                val hidden = c.id in s.saved.hidden
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    TonyCard(T.StdW, T.StdH, T.RadiusStd, onClick = { s.setHidden(c.id, !hidden) }) { f ->
                        Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (hidden) 0.35f else 1f }) { TileArt(c, false, f) }
                        if (hidden) LiveChip(Modifier.align(Alignment.Center), "HIDDEN", dot = false)
                    }
                    Text(c.name, style = ts(9.5.sp, 500, if (hidden) T.Muted else T.Text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun UpdatesSection(s: AppState) {
    val total = s.countryRows.sumOf { it.channels.size }
    val ok = s.countryRows.sumOf { r -> r.channels.count { it.isWorking } }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF30D158)))
            Text("Automatic", style = ts(13.sp, 700, Color.White))
        }
        Text("Tony checks every channel automatically several times a day and refreshes the list by itself while it's open. " +
            "If a channel stops working, it switches to a backup link. There's nothing you need to do.", style = ts(11.sp, 400, T.Text2, lineHeight = 17.sp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Stat("Last checked", friendlyDate(s.index?.generated))
        Stat("Working now", "$ok of $total")
        Stat("Countries", "${s.saved.countries.size}")
    }
}

@Composable
private fun AboutSection() {
    Card {
        Wordmark(26f)
        Text("Version ${BuildConfig.VERSION_NAME}", style = ts(11.sp, 600, T.Text2))
        Text("Free live TV from around the world.", style = ts(11.sp, 400, T.Text2))
    }
    Card {
        Text("Where the channels come from", style = ts(12.sp, 700, Color.White))
        Text("Free-TV and iptv-org: public lists of free-to-air streams. Tony has no ads and no accounts, and it doesn't change anything else on this TV.",
            style = ts(10.5.sp, 400, T.Text2, lineHeight = 16.sp))
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).border(0.5.dp, T.Hairline, RoundedCornerShape(16.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(Modifier.width(190.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.05f)).border(0.5.dp, T.Hairline, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), style = ts(7.5.sp, 700, T.Muted, 1.sp))
        Text(value, style = ts(14.sp, 700, Color.White))
    }
}

private fun friendlyDate(iso: String?): String = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM, h:mm a"))
}.getOrDefault("Not yet")

@Composable
private fun MenuItem(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(36.dp).onFocusChanged { if (it.isFocused) onClick() },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.10f) else Color.Transparent, contentColor = if (selected) Color.White else T.Text2,
            focusedContainerColor = Color.White, focusedContentColor = T.Bg),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, modifier = Modifier.size(13.dp))
            Text(label, style = ts(11.sp, 600, Color.Unspecified))
        }
    }
}
