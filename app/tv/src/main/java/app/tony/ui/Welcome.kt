package app.tony.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

/** Soft, slowly drifting colour field used behind the loading and welcome screens. */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "ambient")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
    Box(modifier.fillMaxSize().background(T.Bg)) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(T.Accent.copy(alpha = 0.20f), Color.Transparent),
            center = Offset(300f + 500f * drift, 950f), radius = 1100f)))
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF2A5BD7).copy(alpha = 0.18f), Color.Transparent),
            center = Offset(1700f - 400f * drift, 150f + 200f * drift), radius = 1000f)))
    }
}

/** Shown while Tony starts up and fetches the channel list. */
@Composable
fun LoadingScreen() {
    val t = rememberInfiniteTransition(label = "loading")
    val pulse by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val sweep by t.animateFloat(-0.4f, 1.1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing)), label = "sweep")
    Box(Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("tony", style = ts(64.sp, 900, Color.White, (-2.5).sp))
                Box(Modifier.padding(start = 3.dp, bottom = 13.dp).size(15.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }.clip(CircleShape).background(T.Accent))
            }
            Box(Modifier.width(160.dp).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f))) {
                Box(Modifier.fillMaxSize().graphicsLayer { translationX = size.width * sweep; scaleX = 0.35f }.clip(CircleShape).background(Color.White.copy(alpha = 0.85f)))
            }
            Text("Getting your channels ready", style = ts(11.sp, 500, T.Text3))
        }
    }
}

/** First run: welcome, countries, all set. Also reused from Settings to change countries. */
@Composable
fun WelcomeScreen(s: AppState, fromSettings: Boolean = false) {
    var step by rememberSaveable { mutableIntStateOf(if (fromSettings) 1 else 0) }
    val chosen = remember { mutableStateListOf<String>().apply { addAll(s.saved.countries) } }
    AnimatedContent(step, transitionSpec = {
        (fadeIn(tween(450)) + slideInHorizontally(tween(450)) { it / 10 }) togetherWith (fadeOut(tween(250)) + slideOutHorizontally(tween(300)) { -it / 10 })
    }, label = "onboarding") { st ->
        when (st) {
            0 -> Intro { step = 1 }
            1 -> Countries(s, chosen, fromSettings) {
                if (fromSettings) { s.chooseCountries(chosen.toList()); s.handleBack() } else step = 2
            }
            else -> AllSet(s, chosen) { s.chooseCountries(chosen.toList()) }
        }
    }
}

@Composable
private fun Intro(onNext: () -> Unit) {
    val go = remember { FocusRequester() }
    FocusOnStart(go)
    Box(Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(Modifier.align(Alignment.CenterStart).padding(start = 90.dp).width(520.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Wordmark(30f)
            Text("Live TV from home,\nright here.", style = ts(44.sp, 800, Color.White, (-1.4).sp, lineHeight = 48.sp))
            Text("Channels from your country and around the world. Free, with nothing to sign up for.",
                style = ts(13.sp, 400, T.Text2, lineHeight = 19.sp))
            Row(Modifier.padding(top = 14.dp)) { TonyButton("Get started", onNext, Modifier.focusRequester(go), primary = true) }
        }
    }
}

@Composable
private fun Countries(s: AppState, chosen: MutableList<String>, fromSettings: Boolean, onDone: () -> Unit) {
    val all = s.countryEntries()
    val collections = s.collectionEntries()
    // Settings: what's already chosen comes first ("Selected"). First run: the TV's own region ("Suggested").
    val topNames = remember(all.size) { if (fromSettings) all.filter { it.file in chosen }.map { it.name } else s.suggestedCountries() }
    val top = all.filter { it.name in topNames }.sortedBy { topNames.indexOf(it.name) }
    val rest = all.filter { it.name !in topNames }.sortedBy { it.name }
    val grid = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val firstFocus = remember { FocusRequester() }
    FocusOnStart(firstFocus, all.size)

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            GridCells.Fixed(5), state = grid,
            contentPadding = PaddingValues(start = T.Margin, end = T.Margin, top = 44.dp, bottom = 60.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(5) }) {
                Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (fromSettings) "SETTINGS" else "STEP 1 OF 2", style = ts(9.sp, 700, Color.White.copy(alpha = 0.55f), 1.2.sp))
                        Text("Which countries?", style = ts(32.sp, 850, Color.White, (-1).sp))
                        Text(if (chosen.isEmpty()) "Each one gets its own row on Home. You can change this any time."
                             else "${chosen.size} chosen", style = ts(12.sp, 400, Color.White.copy(alpha = 0.65f)))
                    }
                    if (chosen.isNotEmpty()) TonyButton(if (fromSettings) "Save" else "Continue", onDone,
                        Modifier.onFocusChanged { if (it.isFocused) scope.launch { grid.animateScrollToItem(0) } }, primary = true)
                }
            }
            if (s.loading && all.isEmpty()) item(span = { GridItemSpan(5) }) { Text("Loading the channel list…", style = ts(11.sp, 500, T.Muted)) }
            if (s.offline && all.isEmpty()) item(span = { GridItemSpan(5) }) { OfflineNotice(s) }
            val firstKey = (top.firstOrNull() ?: collections.firstOrNull() ?: rest.firstOrNull())?.file
            if (top.isNotEmpty()) item(span = { GridItemSpan(5) }) { SectionLabel(if (fromSettings) "Selected" else "Suggested for you") }
            items(top, key = { "t" + it.file }) { e -> CountryCard(e, e.file in chosen, if (e.file == firstKey) Modifier.focusRequester(firstFocus) else Modifier) { toggle(chosen, e.file) } }
            if (collections.isNotEmpty()) item(span = { GridItemSpan(5) }) { SectionLabel("Languages") }
            items(collections, key = { "c" + it.file }) { e -> CountryCard(e, e.file in chosen, if (e.file == firstKey) Modifier.focusRequester(firstFocus) else Modifier) { toggle(chosen, e.file) } }
            if (rest.isNotEmpty()) item(span = { GridItemSpan(5) }) { SectionLabel("All countries") }
            items(rest, key = { it.file }) { e -> CountryCard(e, e.file in chosen, if (e.file == firstKey) Modifier.focusRequester(firstFocus) else Modifier) { toggle(chosen, e.file) } }
        }
    }
}

@Composable
private fun AllSet(s: AppState, chosen: List<String>, onStart: () -> Unit) {
    val go = remember { FocusRequester() }
    FocusOnStart(go)
    val entries = chosen.mapNotNull { f -> s.countryEntries().firstOrNull { it.file == f } }
    Box(Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(Modifier.align(Alignment.CenterStart).padding(start = 90.dp).width(560.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("STEP 2 OF 2", style = ts(8.sp, 700, T.Accent, 1.2.sp))
            Text("You're all set.", style = ts(44.sp, 800, Color.White, (-1.4).sp))
            Text("Each country gets its own row on Home. Hold OK on any channel to add it to your Favourites.",
                style = ts(13.sp, 400, T.Text2, lineHeight = 19.sp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.take(6).forEach { e ->
                    Row(Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(e.flag, style = ts(13.sp)); Text(e.name, style = ts(11.sp, 600, Color.White))
                    }
                }
            }
            Row(Modifier.padding(top = 12.dp)) { TonyButton("Start watching", onStart, Modifier.focusRequester(go), icon = Icons.Play, primary = true) }
        }
    }
}

private fun toggle(list: MutableList<String>, f: String) { if (f in list) list.remove(f) else list.add(f) }

fun plural(n: Int, word: String) = "$n $word" + if (n == 1) "" else "s"

@Composable
private fun SectionLabel(t: String) = Text(t.uppercase(), style = ts(8.sp, 700, T.Muted, 1.1.sp), modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))

/** Country card. Selected = solid white card with a dark check (no orange outline). */
@Composable
private fun CountryCard(e: CountryEntry, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, spring(0.7f, 420f), label = "c")
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(72.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White else Color.White.copy(alpha = 0.05f),
            focusedContainerColor = if (selected) Color.White else Color.White.copy(alpha = 0.13f),
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(0.5.dp, if (selected) Color.Transparent else T.Hairline)),
            focusedBorder = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.85f))),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(Color.Black.copy(alpha = 0.55f), 14.dp)),
        interactionSource = src,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (e.kind == "collection") Box(Modifier.size(30.dp).clip(CircleShape).background(if (selected) T.Bg else Color.White.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Text(if (e.name == "Arabic") "ع" else e.name.take(1), style = ts(15.sp, 800, Color.White))
            } else Text(e.flag.ifEmpty { "🏳" }, style = ts(24.sp))
            Column(Modifier.weight(1f)) {
                Text(e.name, style = ts(11.sp, 650, if (selected) T.Bg else Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(plural(e.channels, "channel"), style = ts(8.5.sp, 500, if (selected) T.Bg.copy(alpha = 0.6f) else T.Muted))
            }
            if (selected) Box(Modifier.size(16.dp).clip(CircleShape).background(T.Bg), contentAlignment = Alignment.Center) {
                Icon(Icons.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
    }
}
