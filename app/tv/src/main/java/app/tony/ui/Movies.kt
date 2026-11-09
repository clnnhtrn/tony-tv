package app.tony.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.tony.AppState
import app.tony.ChannelRow
import app.tony.Screen
import app.tony.data.Channel

/** Free, legal film apps the family can open from Tony (they're separate apps on the Google TV stick). */
private data class FilmApp(val name: String, val pkg: String, val line: String, val tint: Color)
private val FILM_APPS = listOf(
    FilmApp("Tubi", "com.tubitv", "Thousands of free films, including action", Color(0xFF3A1F7A)),
    FilmApp("Pluto TV", "tv.pluto.android", "Free films on demand and film channels", Color(0xFF2B2B12)),
    FilmApp("Plex", "com.plexapp.android", "Free films and shows", Color(0xFF3B2A06)),
)

private val LANG = mapOf("ara" to "Arabic", "eng" to "English", "vie" to "Vietnamese", "fra" to "French", "spa" to "Spanish",
    "ita" to "Italian", "hin" to "Hindi", "por" to "Portuguese", "deu" to "German", "tur" to "Turkish", "ell" to "Greek",
    "rus" to "Russian", "pol" to "Polish", "zho" to "Chinese", "kor" to "Korean", "jpn" to "Japanese", "fas" to "Persian")

@Composable
fun MoviesScreen(s: AppState) {
    val ctx = LocalContext.current
    val all = s.extraRows.firstOrNull { it.key == "movies" }?.channels.orEmpty()
    // Film rows by language, the family's own languages first (works for any family, not one country)
    val homeLangs = s.countryRows.flatMap { r -> r.channels.flatMap { it.languages } }.groupingBy { it }.eachCount()
        .entries.sortedByDescending { it.value }.map { it.key }.take(3)
    // "Films on now": the family's own languages first, then the rest
    val onNow = remember(all, homeLangs) { all.sortedWith(compareBy({ c -> homeLangs.indexOfFirst { it in c.languages }.let { if (it < 0) 99 else it } }, { !it.isWorking })) }
    val byLang = all.groupBy { c -> c.languages.firstOrNull { it in LANG } ?: "other" }
    val order = (homeLangs + byLang.keys.sortedByDescending { byLang[it]?.size ?: 0 }).distinct().filter { it != "other" && byLang[it].orEmpty().size >= 3 }
    var featuredId by remember { mutableStateOf<String?>(null) }
    val featured: Channel? = all.firstOrNull { it.id == featuredId } ?: onNow.firstOrNull()
    val watch = remember { FocusRequester() }
    if (featured != null) FocusOnStart(watch)
    var heroFocused by remember { mutableStateOf(true) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    ReportScroll(s, listState)
    androidx.compose.runtime.LaunchedEffect(heroFocused) { if (heroFocused && listState.firstVisibleItemScrollOffset > 0) listState.animateScrollToItem(0) }

    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 110.dp)) {
        item("hero") {
            Box(Modifier.fillMaxWidth().height(540.dp).onFocusChanged { heroFocused = it.hasFocus }) {
                HeroBackdrop(featured, height = 540.dp, live = heroFocused)
                if (featured != null) HeroInfo(s, featured, Modifier.padding(start = T.Margin, top = 132.dp), watch, onWatch = { s.go(Screen.Watch(featured, all)) })
                Column(Modifier.align(Alignment.BottomStart).padding(bottom = 18.dp)) {
                    RowHeader("Films on now")
                    LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(T.RowGapX)) {
                        items(onNow.take(20), key = { "on" + it.id }) { c ->
                            ChannelTile(c, wide = true, sub = c.country, onClick = { s.go(Screen.Details(c)) },
                                onLongClick = { s.toggleFavourite(c) }, onFocus = { featuredId = c.id })
                        }
                    }
                }
            }
        }
        order.forEach { l -> item("l:$l") { ChannelRowView(s, ChannelRow("m-$l", "${LANG[l]} films", null, byLang[l].orEmpty(), byLang[l].orEmpty().size), showCountry = true) } }
        item("apps") {
            Column(Modifier.padding(top = T.RowGap)) {
                RowHeader("Pick any film: free apps")
                LazyRow(contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(T.RowGapX)) {
                    items(FILM_APPS, key = { it.pkg }) { app -> FilmAppCard(app, installed(ctx, app.pkg)) { openApp(ctx, app.pkg) } }
                }
            }
        }
        byLang["other"]?.takeIf { it.isNotEmpty() }?.let { o -> item("other") { ChannelRowView(s, ChannelRow("m-o", "More films", null, o, o.size), showCountry = true) } }
    }
}

@Composable
private fun FilmAppCard(app: FilmApp, isInstalled: Boolean, onClick: () -> Unit) {
    TonyCard(CardWideW, CardWideH, 11.dp, onClick, label = app.name) { _ ->
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(app.tint, T.Card))).padding(14.dp)) {
            Text(app.name, style = ts(22.sp, 850, Color.White, (-0.6).sp), modifier = Modifier.align(Alignment.TopStart))
            Column(Modifier.align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(app.line, style = ts(9.sp, 600, Color.White.copy(alpha = 0.8f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (isInstalled) "Open app" else "Get the app (free)", style = ts(9.sp, 800, Color.White))
            }
        }
    }
}

private fun installed(ctx: Context, pkg: String) =
    ctx.packageManager.getLeanbackLaunchIntentForPackage(pkg) != null || ctx.packageManager.getLaunchIntentForPackage(pkg) != null

/** Opens the app if it's on the TV, otherwise its Play Store page (only when the viewer chooses it). */
private fun openApp(ctx: Context, pkg: String) {
    val pm = ctx.packageManager
    val launch = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
    val intent = launch ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
    runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
