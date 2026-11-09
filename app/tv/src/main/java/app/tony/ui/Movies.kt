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

@Composable
fun MoviesScreen(s: AppState) {
    val ctx = LocalContext.current
    val all = s.extraRows.firstOrNull { it.key == "movies" }?.channels.orEmpty()
    val arabic = all.filter { "ara" in it.languages }
    val english = all.filter { "eng" in it.languages }
    val other = all.filter { it !in arabic && it !in english }
    var featuredId by remember { mutableStateOf<String?>(null) }
    val featured: Channel? = all.firstOrNull { it.id == featuredId } ?: all.firstOrNull()
    val watch = remember { FocusRequester() }
    if (featured != null) FocusOnStart(watch)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 48.dp)) {
        item("hero") {
            Box(Modifier.fillMaxWidth().height(540.dp)) {
                HeroBackdrop(featured, height = 540.dp)
                TopBar(Tab.Movies, onTab = { s.tab(it.toScreen()) }, onSettings = { s.go(Screen.Settings) })
                if (featured != null) Column(Modifier.padding(start = T.Margin, top = 104.dp).width(470.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { LiveChip(text = "FILM ON NOW"); OutlineChip("FREE") }
                    LogoBadge(featured, BadgeHero)
                    Text(featured.name, style = ts(40.sp, 800, Color.White, (-1.2).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(featured.country, "Films around the clock").joinToString("  ·  "), style = ts(11.sp, 500, T.Text2))
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TonyButton("Watch", { s.go(Screen.Watch(featured, all)) }, Modifier.focusRequester(watch), icon = Icons.Play, primary = true)
                        FavouriteButton(s, featured)
                    }
                }
                Column(Modifier.align(Alignment.BottomStart).padding(bottom = 20.dp)) {
                    RowHeader("On now", all.size, "Movie channels playing films right now")
                    LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(T.RowGapX)) {
                        items(all.take(20), key = { "on" + it.id }) { c ->
                            ChannelTile(c, wide = true, sub = c.country, onClick = { s.go(Screen.Details(c)) },
                                onLongClick = { s.toggleFavourite(c) }, onFocus = { featuredId = c.id })
                        }
                    }
                }
            }
        }
        if (arabic.isNotEmpty()) item("ar") { ChannelRowView(s, ChannelRow("m-ar", "Arabic films", "Egyptian, Gulf and Lebanese film channels", arabic, arabic.size), showCountry = true) }
        if (english.isNotEmpty()) item("en") { ChannelRowView(s, ChannelRow("m-en", "English films", "Action, thrillers, classics and more", english, english.size), showCountry = true) }
        item("apps") {
            Column(Modifier.padding(top = T.RowGap)) {
                RowHeader("More free films", subtitle = "Pick any film you like in these free apps (they have ads)")
                LazyRow(contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(T.RowGapX)) {
                    items(FILM_APPS, key = { it.pkg }) { app -> FilmAppCard(app, installed(ctx, app.pkg)) { openApp(ctx, app.pkg) } }
                }
            }
        }
        if (other.isNotEmpty()) item("other") { ChannelRowView(s, ChannelRow("m-o", "From around the world", null, other, other.size), showCountry = true) }
    }
}

@Composable
private fun FilmAppCard(app: FilmApp, isInstalled: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(T.WideW), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TonyCard(T.WideW, T.StdH, T.RadiusWide, onClick) { _ ->
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(app.tint, T.Panel))).padding(14.dp)) {
                Text(app.name, style = ts(20.sp, 800, Color.White, (-0.5).sp), modifier = Modifier.align(Alignment.TopStart))
                OutlineChip(if (isInstalled) "OPEN APP" else "GET THE APP", Modifier.align(Alignment.BottomStart))
            }
        }
        Text(app.line, style = ts(8.5.sp, 500, T.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
