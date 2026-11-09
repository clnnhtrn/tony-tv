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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.tony.AppState
import app.tony.Screen
import app.tony.data.Film
import app.tony.player.LocalPlayers
import app.tony.player.StreamState
import app.tony.player.VideoSurface
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun FilmDetailsScreen(s: AppState, f: Film, row: List<Film>) {
    val ctx = LocalContext.current
    val play = remember { FocusRequester() }
    FocusOnStart(play, f.id)
    val req = ImageRequest.Builder(ctx).data(f.poster).crossfade(true).build()
    Box(Modifier.fillMaxSize().background(T.Bg)) {
        // Soft backdrop: tiny decode stretched full-screen (cheap; no GPU blur)
        AsyncImage(ImageRequest.Builder(ctx).data(f.poster).size(32).build(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.30f)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to T.Bg, 0.45f to T.Bg.copy(alpha = 0.85f), 1f to T.Bg.copy(alpha = 0.4f))))
        Box(Modifier.align(Alignment.TopEnd).padding(top = T.Top, end = T.Margin).size(380.dp, 214.dp).clip(RoundedCornerShape(10.dp))
            .background(Color.Black).border(0.5.dp, T.Hairline, RoundedCornerShape(10.dp))) {
            AsyncImage(req, f.title, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.padding(start = T.Margin, top = 70.dp).width(420.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { OutlineChip("CLASSIC FILM"); OutlineChip("FREE · PUBLIC DOMAIN", color = T.Muted) }
            Text(f.title, style = ts(32.sp, 800, Color.White, (-0.9).sp), maxLines = 2)
            Text(listOfNotNull(f.year?.toString(), f.minutes?.let { "${it / 60}h ${it % 60}m".replace("0h ", "") }, f.source).joinToString("  ·  "), style = ts(10.sp, 500, T.Text3))
            if (f.description.isNotBlank()) Text(f.description, style = ts(10.5.sp, 400, T.Text2, lineHeight = 15.5.sp), maxLines = 4)
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                TonyButton("Play", { s.go(Screen.FilmWatch(f)) }, Modifier.focusRequester(play), icon = Icons.Play, primary = true)
            }
        }
        val more = row.filter { it.id != f.id }.take(30)
        if (more.isNotEmpty()) Column(Modifier.align(Alignment.BottomStart).padding(bottom = 22.dp)) {
            RowHeader("More classic films")
            LazyRow(Modifier.focusRestorer(), contentPadding = PaddingValues(horizontal = T.Margin), horizontalArrangement = Arrangement.spacedBy(T.Gap)) {
                items(more, key = { it.id }) { m -> FilmTile(m) { s.replaceTop(Screen.FilmDetails(m, row)) } }
            }
        }
    }
}

/** On-demand film: the standard Media3 controls (play/pause/seek with the D-pad). */
@Composable
fun FilmWatchScreen(s: AppState, f: Film) {
    val main = LocalPlayers.current.main
    LaunchedEffect(f.id) { main.play("film:" + f.id, f.url) }
    DisposableEffect(Unit) { onDispose { main.stop() } }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VideoSurface(main, Modifier.fillMaxSize(), fill = false, controls = true)
        if (main.state == StreamState.Loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading ${f.title}…", style = ts(12.sp, 500, T.Text2))
        }
        if (main.state == StreamState.Failed) Box(Modifier.fillMaxSize().background(T.Bg), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("This film won't play right now", style = ts(18.sp, 750, Color.White))
                Text("The Internet Archive may be busy. Try again in a minute.", style = ts(10.5.sp, 400, T.Text3))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { TonyButton("Try again", { main.retry() }, primary = true); TonyButton("Back", { s.back() }, icon = Icons.Back) }
            }
        }
    }
}
