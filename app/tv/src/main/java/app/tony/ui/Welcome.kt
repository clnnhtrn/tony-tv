package app.tony.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.tony.AppState
import app.tony.data.CountryEntry

/** First run (and Settings → Countries): "Which countries do you want to watch?" */
@Composable
fun WelcomeScreen(s: AppState, fromSettings: Boolean = false) {
    val all = s.countryEntries()
    val chosen = remember { mutableStateListOf<String>().apply { addAll(s.saved.countries) } }   // stable: never reset while choosing
    val suggested = listOf("Lebanon", "Australia")
    val top = all.filter { it.name in suggested }.sortedBy { suggested.indexOf(it.name) }
    val rest = all.filter { it.name !in suggested }.sortedBy { it.name }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(top.isNotEmpty()) { runCatching { firstFocus.requestFocus() } }

    Box(Modifier.fillMaxSize().background(T.Bg)) {
        LazyVerticalGrid(
            GridCells.Fixed(6),
            contentPadding = PaddingValues(start = T.Margin, end = T.Margin, top = T.Top, bottom = 70.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(6) }) {
                Column(Modifier.padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!fromSettings) Wordmark()
                    Row(Modifier.fillMaxWidth().padding(top = if (fromSettings) 0.dp else 26.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Which countries do you want to watch?", style = ts(24.sp, 800, Color.White, (-0.6).sp))
                            Text("Pick as many as you like. You can change this later in Settings.", style = ts(10.5.sp, 400, T.Text3))
                        }
                        // Continue lives up here: one press up from the suggested countries (not 30 rows down)
                        if (chosen.isNotEmpty()) TonyButton(if (fromSettings) "Save" else "Continue",
                            { s.chooseCountries(chosen.toList()); if (fromSettings) s.back() }, primary = true, icon = Icons.Chevron)
                    }
                }
            }
            if (s.loading && all.isEmpty()) item(span = { GridItemSpan(6) }) { Text("Loading the channel list…", style = ts(10.sp, 500, T.Muted)) }
            if (s.offline && all.isEmpty()) item(span = { GridItemSpan(6) }) { OfflineNotice(s) }
            if (top.isNotEmpty()) item(span = { GridItemSpan(6) }) { SectionLabel("Suggested") }
            items(top, key = { "t" + it.file }) { e ->
                CountryCard(e, e.file in chosen, if (e == top.first()) Modifier.focusRequester(firstFocus) else Modifier) { toggle(chosen, e.file) }
            }
            if (rest.isNotEmpty()) item(span = { GridItemSpan(6) }) { SectionLabel("All countries") }
            items(rest, key = { it.file }) { e -> CountryCard(e, e.file in chosen) { toggle(chosen, e.file) } }
        }
        // Footer summary (solid, hairline on top)
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(T.Bg)) {
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.08f)))
            Text(if (chosen.isEmpty()) "Choose at least one country, then press Up to Continue"
                 else "${chosen.size} chosen · ${plural(chosen.sumOf { f -> all.firstOrNull { it.file == f }?.channels ?: 0 }, "channel")} · press Up to Continue",
                style = ts(10.sp, 500, T.Text3), modifier = Modifier.padding(horizontal = T.Margin, vertical = 12.dp))
        }
    }
}

fun plural(n: Int, word: String) = "$n $word" + if (n == 1) "" else "s"

private fun toggle(list: MutableList<String>, f: String) { if (f in list) list.remove(f) else list.add(f) }

@Composable
private fun SectionLabel(t: String) = Text(t.uppercase(), style = ts(7.5.sp, 700, T.Muted, 1.sp), modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))

@Composable
private fun CountryCard(e: CountryEntry, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(64.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(T.RadiusStd)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.03f),
            focusedContainerColor = if (selected) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.07f),
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(0.5.dp, if (selected) T.Accent.copy(alpha = 0.7f) else T.Hairline)),
            focusedBorder = Border(BorderStroke(T.FocusEdge, Color.White)),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = T.FocusScale),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(Color.Black.copy(alpha = 0.5f), 10.dp)),
        interactionSource = src,
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(e.flag.ifEmpty { "🏳" }, style = ts(17.sp), modifier = Modifier.weight(1f))
                if (selected) Box(Modifier.size(12.dp).clip(CircleShape).background(T.Accent), contentAlignment = Alignment.Center) {
                    Icon(Icons.Check, null, tint = T.Bg, modifier = Modifier.size(8.dp))
                }
            }
            Column {
                Text(e.name, style = ts(9.5.sp, if (focused || selected) 650 else 500, if (focused || selected) Color.White else T.Text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(plural(e.channels, "channel"), style = ts(7.5.sp, 500, T.Muted))
            }
        }
    }
}
