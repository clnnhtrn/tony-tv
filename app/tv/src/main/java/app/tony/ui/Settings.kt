package app.tony.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.tony.AppState
import app.tony.BuildConfig
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private enum class Section(val label: String) { Countries("Countries"), Hide("Hide channels"), Updates("Channel updates"), About("About Tony") }

/** v8 Settings: big title, plain left menu, content in soft cards. */
@Composable
fun SettingsScreen(s: AppState) {
    var sec by rememberSaveable { mutableStateOf(Section.Countries) }
    var editingCountries by remember { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    FocusOnStart(first)
    BackLayer(s, editingCountries) { editingCountries = false }
    if (editingCountries) { WelcomeScreen(s, fromSettings = true); return }

    Row(Modifier.fillMaxSize()) {
        Column(Modifier.width(250.dp).padding(start = T.Margin, top = 44.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Settings", style = ts(30.sp, 850, Color.White, (-0.8).sp), modifier = Modifier.padding(bottom = 18.dp))
            Section.entries.forEachIndexed { i, x -> MenuItem(x.label, x == sec, if (i == 0) Modifier.focusRequester(first) else Modifier) { sec = x } }
        }
        AnimatedContent(sec, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "settings",
            modifier = Modifier.fillMaxSize().padding(start = 46.dp, end = T.Margin, top = 98.dp)) { x ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (x) {
                    Section.Countries -> CountriesSection(s) { editingCountries = true }
                    Section.Hide -> HideSection(s)
                    Section.Updates -> UpdatesSection(s)
                    Section.About -> AboutSection()
                }
            }
        }
    }
}

@Composable
private fun CountriesSection(s: AppState, onChange: () -> Unit) {
    Text("Each one has its own row on Home, in this order.", style = ts(12.sp, 400, Color.White.copy(alpha = 0.65f)))
    val entries = s.saved.countries.mapNotNull { f -> (s.countryEntries() + s.collectionEntries()).firstOrNull { it.file == f } }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(entries, key = { it.file }) { e ->
            val row = (s.countryRows + s.extraRows).firstOrNull { it.title == e.name }
            val working = row?.channels?.count { it.isWorking } ?: e.working
            Column(Modifier.width(150.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.06f)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(e.flag.ifEmpty { if (e.name == "Arabic") "ع" else "•" }, style = ts(24.sp, 800, Color.White))
                Text(e.name, style = ts(14.sp, 800, Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${plural(e.channels, "channel")} · $working working", style = ts(9.sp, 500, Color.White.copy(alpha = 0.55f)))
            }
        }
    }
    Row(Modifier.padding(top = 6.dp)) { TonyButton("Change countries", onChange, primary = true) }
}

/** Hide channels you never watch: OK hides / shows; hidden tiles fade with a "Hidden" label. */
@Composable
private fun HideSection(s: AppState) {
    Text("Press OK on a channel to hide it from Home. Press OK again to bring it back.", style = ts(12.sp, 400, Color.White.copy(alpha = 0.65f)))
    androidx.compose.runtime.CompositionLocalProvider(LocalInGrid provides true) { LazyVerticalGrid(GridCells.Fixed(4), contentPadding = PaddingValues(top = 8.dp, bottom = 60.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        s.countryRowsAll.forEach { r ->
            item("h" + r.key, span = { GridItemSpan(4) }) { Text(r.title, style = ts(13.sp, 700, Color.White.copy(alpha = 0.85f)), modifier = Modifier.padding(top = 6.dp)) }
            items(r.channels, key = { r.key + it.id }) { c ->
                val hidden = c.id in s.saved.hidden
                TonyCard(CardStdW, CardStdH, 10.dp, onClick = { s.setHidden(c.id, !hidden) }, label = c.title + if (hidden) ", hidden" else "") { f ->
                    Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (hidden) 0.3f else 1f }) {
                        TileArt(c, false, f)
                        Text(c.title, style = ts(12.sp, 850, Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.BottomStart).padding(9.dp))
                    }
                    if (hidden) Text("Hidden", style = ts(11.sp, 800, Color.White), modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    } }
}

@Composable
private fun UpdatesSection(s: AppState) {
    val total = s.countryRows.sumOf { it.channels.size }
    val ok = s.countryRows.sumOf { r -> r.channels.count { it.isWorking } }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF30D158)))
            Text("Updates itself", style = ts(15.sp, 800, Color.White))
        }
        Text("Tony checks every channel several times a day and refreshes the list while it's open. " +
            "If a channel stops working it switches to a backup link. There's nothing you need to do.",
            style = ts(11.sp, 400, Color.White.copy(alpha = 0.7f), lineHeight = 17.sp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Stat("Last checked", friendlyDate(s.index?.generated))
        Stat("Working now", "$ok of $total")
        Stat("Countries", "${s.countryRows.size}")
    }
}

@Composable
private fun AboutSection() {
    Card {
        Wordmark(26f)
        Text("Free live TV from around the world.", style = ts(14.sp, 700, Color.White))
        Text("Version ${BuildConfig.VERSION_NAME}", style = ts(11.sp, 500, Color.White.copy(alpha = 0.6f)))
    }
    Card {
        Text("Where the channels come from", style = ts(12.sp, 700, Color.White))
        Text("Free-TV and iptv-org: public lists of free-to-air streams. Tony has no ads and no accounts, and it doesn't change anything else on this TV.",
            style = ts(11.sp, 400, Color.White.copy(alpha = 0.7f), lineHeight = 17.sp))
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.06f)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(Modifier.width(190.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.06f)).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label.uppercase(), style = ts(8.sp, 700, Color.White.copy(alpha = 0.5f), 1.sp))
        Text(value, style = ts(15.sp, 800, Color.White))
    }
}

private fun friendlyDate(iso: String?): String = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM, h:mm a"))
}.getOrDefault("Not yet")

@Composable
private fun MenuItem(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(34.dp).onFocusChanged { if (it.isFocused) onClick() },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(9.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent, contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            focusedContainerColor = Color.White, focusedContentColor = T.Bg),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
    ) { Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) { Text(label, style = ts(12.sp, 650, Color.Unspecified)) } }
}
