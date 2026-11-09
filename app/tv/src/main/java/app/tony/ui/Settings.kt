package app.tony.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.tony.AppState
import app.tony.BuildConfig
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private enum class Section(val label: String) { Countries("Countries"), Channels("Channels"), Subtitles("Subtitles"), Opens("When Tony opens"), Check("Check channels"), About("About Tony") }

@Composable
fun SettingsScreen(s: AppState) {
    var sec by rememberSaveable { mutableStateOf(Section.Countries) }
    var editingCountries by remember { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    FocusOnStart(first)
    if (editingCountries) { WelcomeScreen(s, fromSettings = true); androidx.activity.compose.BackHandler { editingCountries = false }; return }

    Row(Modifier.fillMaxSize().background(T.Bg)) {
        Column(Modifier.width(230.dp).fillMaxHeight().background(T.Panel).border(0.5.dp, T.Hairline).padding(start = 26.dp, end = 14.dp, top = T.Top),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Wordmark(16f)
            Text("Settings", style = ts(20.sp, 800, Color.White, (-0.5).sp), modifier = Modifier.padding(top = 18.dp, bottom = 12.dp))
            Section.entries.forEachIndexed { i, x ->
                MenuItem(x.label, x == sec, if (i == 0) Modifier.focusRequester(first) else Modifier) { sec = x }
            }
        }
        Column(Modifier.fillMaxSize().padding(start = 34.dp, end = T.Margin, top = T.Top + 40.dp)) {
            Text(sec.label, style = ts(16.sp, 750, Color.White), modifier = Modifier.padding(bottom = 12.dp))
            when (sec) {
                Section.Countries -> {
                    Text("These countries each get a row on Home, in this order.", style = ts(10.sp, 400, T.Text3), modifier = Modifier.padding(bottom = 10.dp))
                    s.saved.countries.mapNotNull { f -> s.index?.countries?.firstOrNull { it.file == f } }.forEach { e ->
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(e.flag + "  " + e.name, style = ts(11.sp, 600), modifier = Modifier.weight(1f))
                            Text("${e.channels} channels", style = ts(9.sp, 500, T.Muted))
                        }
                    }
                    TonyButton("Change countries", { editingCountries = true }, Modifier.padding(top = 12.dp), primary = true)
                }
                Section.Channels -> {
                    Text("Hide channels you never watch. Hidden channels disappear from Home.", style = ts(10.sp, 400, T.Text3), modifier = Modifier.padding(bottom = 10.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
                        s.countryRowsAll.forEach { r ->
                            item("h" + r.key) { Text(r.title, style = ts(11.sp, 700, T.Text2), modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) }
                            items(r.channels, key = { r.key + it.id }) { c -> ToggleRow(c.name, !s.saved.hidden.contains(c.id), "Shown", "Hidden") { s.setHidden(c.id, it.not()) } }
                        }
                        if (s.saved.hidden.isNotEmpty()) item("restore") {
                            TonyButton("Show all hidden channels (${s.saved.hidden.size})", { s.saved.hidden.forEach { s.setHidden(it, false) } }, Modifier.padding(top = 10.dp))
                        }
                    }
                }
                Section.Subtitles -> {
                    Text("Turn subtitles on automatically when a channel has them (some Australian channels do).", style = ts(10.sp, 400, T.Text3), modifier = Modifier.padding(bottom = 10.dp))
                    ToggleRow("Subtitles", s.saved.subtitles, "On", "Off") { s.setSubtitles(it) }
                }
                Section.Opens -> {
                    Text("Tony opens on the Home screen, so everyone starts in the same familiar place.", style = ts(10.sp, 400, T.Text3), modifier = Modifier.padding(bottom = 10.dp))
                    ToggleRow("Home screen", true, "Selected", "") { }
                }
                Section.Check -> {
                    Text("Tony's channel list is refreshed every day, and a channel that stops working switches to a backup link by itself.", style = ts(10.sp, 400, T.Text3), modifier = Modifier.padding(bottom = 6.dp))
                    Text("Channel list updated: " + friendlyDate(s.index?.generated), style = ts(11.sp, 600), modifier = Modifier.padding(bottom = 4.dp))
                    val total = s.countryRows.sumOf { it.channels.size }; val ok = s.countryRows.sumOf { r -> r.channels.count { it.isWorking } }
                    Text("$ok of $total channels in your countries were working at the last check.", style = ts(10.sp, 400, T.Text3), modifier = Modifier.padding(bottom = 12.dp))
                    TonyButton(if (s.loading) "Checking…" else "Check now", { s.reload(); s.showToast("Channel list refreshed") }, primary = true)
                }
                Section.About -> {
                    Text("Tony ${BuildConfig.VERSION_NAME}", style = ts(12.sp, 700))
                    Text("Free TV from Lebanon, Australia and around the world.", style = ts(10.sp, 400, T.Text3), modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                    Text("Channels: Free-TV/IPTV and iptv-org (public, free streams).\nClassic films: Internet Archive (public domain).\nTony has no ads, no accounts, and doesn't change anything else on this TV.",
                        style = ts(9.5.sp, 400, T.Muted, lineHeight = 15.sp))
                }
            }
        }
    }
}

private fun friendlyDate(iso: String?): String = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a"))
}.getOrDefault("not yet")

@Composable
private fun MenuItem(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(30.dp).onFocusChanged { if (it.isFocused) onClick() },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(7.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.10f) else Color.Transparent, contentColor = if (selected) Color.White else T.Text3,
            focusedContainerColor = Color.White, focusedContentColor = T.Bg),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
    ) { Box(Modifier.fillMaxSize().padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) { Text(label, style = ts(10.5.sp, 600, Color.Unspecified)) } }
}

@Composable
fun ToggleRow(label: String, on: Boolean, onText: String, offText: String, onChange: (Boolean) -> Unit) {
    Surface(
        onClick = { onChange(!on) },
        modifier = Modifier.width(420.dp).height(32.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(7.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.04f), contentColor = T.Text, focusedContainerColor = Color.White.copy(alpha = 0.12f), focusedContentColor = Color.White),
        border = ClickableSurfaceDefaults.border(
            border = androidx.tv.material3.Border(androidx.compose.foundation.BorderStroke(0.5.dp, T.Hairline)),
            focusedBorder = androidx.tv.material3.Border(androidx.compose.foundation.BorderStroke(T.FocusEdge, Color.White))),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = ts(10.sp, 600, Color.Unspecified), modifier = Modifier.weight(1f))
            Text(if (on) onText else offText, style = ts(8.5.sp, 500, T.Muted), modifier = Modifier.padding(end = 8.dp))
            // Switch (accent when on)
            Box(Modifier.size(26.dp, 15.dp).clip(CircleShape).background(if (on) T.Accent else Color.White.copy(alpha = 0.18f)), contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart) {
                Box(Modifier.padding(2.dp).size(11.dp).clip(CircleShape).background(Color.White))
            }
        }
    }
}
