package app.tony.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.tony.data.Channel
import app.tony.data.Repository
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.sqrt

/* ---------------- icons (stroke/fill paths, 24×24) ---------------- */
object Icons {
    private fun fill(name: String, d: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes(d), fill = SolidColor(Color.Black)).build()
    private fun stroke(name: String, d: String, w: Float = 1.8f) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes(d), stroke = SolidColor(Color.Black), strokeLineWidth = w, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round).build()
    val Play = fill("play", "M7 4.5v15l12-7.5z")
    val Heart = fill("heart", "M12 20.5s-7.5-4.6-9.3-9.2C1.5 8.1 3.6 4.5 7.2 4.5c2 0 3.6 1.1 4.8 2.7 1.2-1.6 2.8-2.7 4.8-2.7 3.6 0 5.7 3.6 4.5 6.8-1.8 4.6-9.3 9.2-9.3 9.2z")
    val HeartOutline = stroke("heartOutline", "M12 20.5s-7.5-4.6-9.3-9.2C1.5 8.1 3.6 4.5 7.2 4.5c2 0 3.6 1.1 4.8 2.7 1.2-1.6 2.8-2.7 4.8-2.7 3.6 0 5.7 3.6 4.5 6.8-1.8 4.6-9.3 9.2-9.3 9.2z")
    val Gear = stroke("gear", "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z", 1.7f)
    val Chevron = stroke("chevron", "M9 6l6 6-6 6", 2f)
    val Check = stroke("check", "M5 12.5l4.5 4.5L19 7.5", 2.4f)
    val Back = stroke("back", "M15 6l-6 6 6 6", 2f)
    val Captions = stroke("cc", "M3 6.5h18v11H3zM10 10.5a2 2 0 1 0 0 3M17 10.5a2 2 0 1 0 0 3", 1.6f)
    val Warning = stroke("warn", "M12 4l9 16H3zM12 10v4M12 17v.01", 1.8f)
    val Globe = stroke("globe", "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM3 12h18M12 3c2.5 2.5 3.8 5.5 3.8 9s-1.3 6.5-3.8 9c-2.5-2.5-3.8-5.5-3.8-9S9.5 5.5 12 3z", 1.6f)
    val Grid = stroke("grid", "M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z", 1.6f)
    val Refresh = stroke("refresh", "M20 11a8 8 0 1 0-2.3 5.7M20 4v7h-7", 1.8f)
    val Info = stroke("info", "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM12 11v5M12 8v.01", 1.8f)
    val Search = stroke("search", "M11 4.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13zM16 16l4 4", 2f)
    val Radio = stroke("radio", "M4 9h16v11H4zM8 9l9-5M8 15h.01M14 13h4M14 16h4", 1.7f)
}

/* ---------------- small pieces ---------------- */

@Composable
fun LiveChip(modifier: Modifier = Modifier, text: String = "LIVE", dot: Boolean = true) {
    Row(
        modifier
            .clip(RoundedCornerShape(3.5.dp))
            .background(T.ChipBg)
            .border(0.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(3.5.dp))
            .padding(horizontal = 4.5.dp, vertical = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
    ) {
        if (dot) Box(Modifier.size(3.5.dp).clip(CircleShape).background(T.Accent))
        Text(text, style = ts(6.sp, 700, Color.White, 0.7.sp))
    }
}

@Composable
fun OutlineChip(text: String, modifier: Modifier = Modifier, color: Color = T.Text2) {
    Box(
        modifier.border(0.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(3.5.dp)).padding(horizontal = 4.5.dp, vertical = 2.5.dp)
    ) { Text(text, style = ts(6.5.sp, 700, color, 0.7.sp)) }
}

data class BadgeSpec(val area: Float, val maxW: Float, val maxH: Float, val padH: Dp, val padV: Dp, val radius: Dp)
val BadgeWide = BadgeSpec(918f, 68f, 23f, 5.dp, 4.dp, 4.5.dp)
val BadgeStd = BadgeSpec(540f, 48f, 17f, 4.dp, 3.dp, 4.dp)
val BadgeHero = BadgeSpec(2376f, 105f, 39f, 7.dp, 5.5.dp, 7.dp)
val BadgeCentre = BadgeSpec(1600f, 80f, 30f, 6.dp, 5.dp, 6.dp)

/** Logo on its badge. Optical sizing: every logo gets the same visual area (CLAUDE.md 7.1). */
@Composable
fun LogoBadge(c: Channel, spec: BadgeSpec, modifier: Modifier = Modifier) {
    val url = Repository.logoUrl(c)
    if (url == null) {
        Box(modifier.clip(RoundedCornerShape(spec.radius)).background(T.DarkBadge).padding(horizontal = spec.padH, vertical = spec.padV)) {
            Text(c.name, style = ts(8.sp, 700, Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(spec.maxW.dp))
        }
        return
    }
    val aspect = ((c.logoWidth ?: 2).toFloat() / (c.logoHeight ?: 1).coerceAtLeast(1)).coerceIn(0.3f, 8f)
    val w0 = sqrt(spec.area * aspect); val h0 = w0 / aspect
    val k = min(1f, min(spec.maxW / w0, spec.maxH / h0))
    val (bg, line) = when (c.logoBadge) {
        "dark" -> T.DarkBadge to Color.White.copy(alpha = 0.16f)
        "fill" -> parseColor(c.logoBadgeColor, Color.White) to Color.White.copy(alpha = 0.12f)
        else -> Color.White to Color.Transparent
    }
    Box(
        modifier.clip(RoundedCornerShape(spec.radius)).background(bg)
            .border(0.5.dp, line, RoundedCornerShape(spec.radius))
            .padding(horizontal = spec.padH, vertical = spec.padV)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
            contentDescription = c.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size((w0 * k).dp, (h0 * k).dp),
        )
    }
}

@Composable
fun dimFor(focused: Boolean): ColorFilter? {
    val b by androidx.compose.animation.core.animateFloatAsState(if (focused) 1f else T.Dim, androidx.compose.animation.core.tween(260), label = "dim")
    return if (b >= 0.999f) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(b, b, b, 1f) })
}

/**
 * v8 focus: the card springs up ~7% with a deep soft shadow and full brightness; nothing else.
 * (Apple TV style: no borders, no rings.) Unfocused cards sit slightly dimmed.
 */
@Composable
fun TonyCard(
    width: Dp, height: Dp, radius: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onFocus: () -> Unit = {},
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (focused) 1.07f else 1f,
        androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 420f), label = "lift")
    val lift by androidx.compose.animation.core.animateFloatAsState(if (focused) 1f else 0f, androidx.compose.animation.core.tween(220), label = "shadow")
    val shape = RoundedCornerShape(radius)
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.size(width, height)
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                shadowElevation = (6.dp.toPx() + 18.dp.toPx() * lift)
                this.shape = shape; clip = false
                ambientShadowColor = Color.Black; spotShadowColor = Color.Black
            }
            .onFocusChanged { if (it.isFocused) onFocus() },
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(containerColor = T.Card, focusedContainerColor = T.Card, pressedContainerColor = T.Card),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 0.97f),
        border = ClickableSurfaceDefaults.border(),
        glow = ClickableSurfaceDefaults.glow(),
        interactionSource = src,
    ) {
        content(focused)
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = (1f - lift) * 0.18f }.background(Color.Black))
    }
}

val CardWideW = 210.dp; val CardWideH = 118.dp     // 420×236 px
val CardStdW = 150.dp; val CardStdH = 84.5.dp      // 300×169 px
val PosterW = 131.dp; val PosterH = 196.dp         // 262×392 px

/** v8 channel card ("key art"): the channel name is the artwork; no text underneath. */
@Composable
fun ChannelTile(
    c: Channel,
    wide: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    favourite: Boolean = false,
    sub: String? = null,
    onLongClick: (() -> Unit)? = null,
    onFocus: () -> Unit = {},
    watching: Boolean = false,
) {
    val w = if (wide) CardWideW else CardStdW
    val h = if (wide) CardWideH else CardStdH
    TonyCard(w, h, if (wide) 11.dp else 10.dp, onClick, modifier, onLongClick = onLongClick, onFocus = onFocus) { focused ->
        KeyArt(c, h, focused)
        LogoMark(c, Modifier.align(Alignment.TopEnd).padding(if (wide) 7.dp else 6.dp), if (wide) 19.dp else 16.dp)
        Column(Modifier.align(Alignment.BottomStart).padding(start = if (wide) 11.dp else 9.dp, end = 8.dp, bottom = if (wide) 9.dp else 7.dp),
            verticalArrangement = Arrangement.spacedBy(if (wide) 3.dp else 2.dp)) {
            Text(c.title, style = ts(if (wide) 16.sp else 12.5.sp, 850, Color.White, (-0.4).sp, lineHeight = if (wide) 17.sp else 13.5.sp),
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.5.dp)) {
                if (favourite) Icon(Icons.Heart, null, tint = T.Accent, modifier = Modifier.size(if (wide) 8.dp else 7.dp))
                if (watching) Text("Watching", style = ts(if (wide) 8.sp else 7.sp, 700, T.Accent))
                else {
                    if (!c.isRadio) Box(Modifier.size(4.dp).clip(CircleShape).background(T.Live))
                    Text(listOfNotNull(if (c.isRadio) "Radio" else "Live", sub).joinToString(" · "), style = ts(if (wide) 8.sp else 7.sp, 600, Color.White.copy(alpha = 0.8f)), maxLines = 1)
                }
            }
        }
    }
}

/** Tall numbered poster (Apple TV "Top 10" style). */
@Composable
fun PosterTile(c: Channel, rank: Int, onClick: () -> Unit, modifier: Modifier = Modifier, onLongClick: (() -> Unit)? = null) {
    TonyCard(PosterW, PosterH, 11.dp, onClick, modifier, onLongClick = onLongClick) { focused ->
        KeyArt(c, PosterH, focused)
        Text("$rank", style = ts(46.sp, 900, Color.White.copy(alpha = 0.93f), (-1.5).sp), modifier = Modifier.padding(start = 11.dp, top = 2.dp))
        Column(Modifier.align(Alignment.BottomCenter).padding(start = 8.dp, end = 8.dp, bottom = 13.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(c.title, style = ts(16.sp, 850, Color.White, (-0.5).sp, lineHeight = 17.sp), maxLines = 2, overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(c.category ?: "Live", style = ts(9.sp, 600, Color.White.copy(alpha = 0.75f)))
        }
    }
}

/** Small round logo mark (top corner of a card). */
@Composable
fun LogoMark(c: Channel, modifier: Modifier = Modifier, size: Dp = 19.dp) {
    val url = Repository.logoUrl(c) ?: return
    val bg = when (c.logoBadge) { "dark" -> Color(0xFF0B0C10); "fill" -> parseColor(c.logoBadgeColor, Color.White); else -> Color.White }
    Box(modifier.size(size).graphicsLayer { shadowElevation = 4.dp.toPx(); shape = CircleShape; clip = true }.background(bg), contentAlignment = Alignment.Center) {
        AsyncImage(ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(), c.name, contentScale = ContentScale.Fit,
            modifier = Modifier.size(size * 0.68f, size * 0.48f))
    }
}

/**
 * The card artwork: the channel's latest frame, anchored to the top and taller than the card so the
 * news ticker along the bottom of the frame falls outside; a deep fade for the text; brand-colour
 * fallback with the logo when there's no frame.
 */
@Composable
fun BoxScope.KeyArt(c: Channel, h: Dp, focused: Boolean) {
    var stillOk by remember(c.id) { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(parseColor(c.tileColor, T.Card), T.Card))), contentAlignment = Alignment.Center) {
        if (!stillOk) LogoBadge(c, BadgeWide, Modifier.padding(bottom = h * 0.25f))
    }
    if (!c.isYouTube && !c.isRadio) AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(Repository.stillUrl(c)).crossfade(true).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter,
        onSuccess = { stillOk = true },
        onError = { stillOk = false },
        modifier = Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true).height(h * 1.28f).align(Alignment.TopCenter),
    )
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
        0f to Color.Black.copy(alpha = 0.25f), 0.28f to Color.Transparent, 0.56f to T.Ink.copy(alpha = 0.72f), 1f to T.Ink.copy(alpha = 0.97f))))
}

@Composable
fun BoxScope.TileArt(c: Channel, wide: Boolean, focused: Boolean) = KeyArt(c, if (wide) CardWideH else CardStdH, focused)

/** Pill button: translucent at rest, solid white with dark text when focused (scale + shadow). */
@Composable
fun TonyButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, iconTint: Color? = null, primary: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = ButtonDefaults.shape(CircleShape),
        colors = ButtonDefaults.colors(
            containerColor = Color.White.copy(alpha = if (primary) 0.24f else 0.16f), contentColor = Color.White,
            focusedContainerColor = Color.White, focusedContentColor = T.Bg,
            pressedContainerColor = Color.White.copy(alpha = 0.85f), pressedContentColor = T.Bg,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.06f),
        border = ButtonDefaults.border(border = Border.None, focusedBorder = Border.None),
        glow = ButtonDefaults.glow(focusedGlow = Glow(Color.Black.copy(alpha = 0.55f), 12.dp)),
        contentPadding = PaddingValues(horizontal = if (primary) 20.dp else 15.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(11.dp), tint = iconTint ?: androidx.tv.material3.LocalContentColor.current)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = ts(if (primary) 12.sp else 11.sp, 700, Color.Unspecified))
    }
}

/** Round icon button (♡, ⓘ). */
@Composable
fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color? = null) {
    Button(
        onClick = onClick,
        modifier = modifier.size(32.dp),
        shape = ButtonDefaults.shape(CircleShape),
        colors = ButtonDefaults.colors(containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White, focusedContainerColor = Color.White, focusedContentColor = T.Bg),
        scale = ButtonDefaults.scale(focusedScale = 1.1f),
        border = ButtonDefaults.border(border = Border.None, focusedBorder = Border.None),
        glow = ButtonDefaults.glow(focusedGlow = Glow(Color.Black.copy(alpha = 0.55f), 10.dp)),
        contentPadding = PaddingValues(0.dp),
    ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icon, description, modifier = Modifier.size(13.dp), tint = tint ?: androidx.tv.material3.LocalContentColor.current) } }
}

/** Row title only (v8: no counts, no subtitles). */
@Composable
fun RowHeader(title: String, count: Int? = null, subtitle: String? = null, modifier: Modifier = Modifier) {
    Text(title, style = ts(14.sp, 700, Color.White.copy(alpha = 0.92f)), modifier = modifier.padding(start = T.Margin, bottom = 10.dp))
}

@Composable
fun Clock() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(15_000) } }
    Text(SimpleDateFormat("h:mm", Locale.getDefault()).format(now), style = ts(12.sp, 600, Color.White.copy(alpha = 0.9f)))
}

enum class Tab(val label: String) { Home("Home"), Movies("Movies"), Favourites("Favourites"), Search("Search") }

/** v8 top bar: wordmark left, search icon + plain text tabs centred, clock + round settings right. */
@Composable
fun TopBar(selected: Tab?, onTab: (Tab) -> Unit, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(start = T.Margin, end = T.Margin, top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(150.dp)) { Wordmark(19f) }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            NavPill(null, selected == Tab.Search, Icons.Search) { onTab(Tab.Search) }
            listOf(Tab.Home, Tab.Movies, Tab.Favourites).forEach { t -> NavPill(t.label, t == selected) { onTab(t) } }
        }
        Row(Modifier.width(150.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Clock()
            Spacer(Modifier.width(11.dp))
            NavPill(null, false, Icons.Gear, round = true, onClick = onSettings)
        }
    }
}

@Composable
private fun NavPill(text: String?, selected: Boolean, icon: ImageVector? = null, round: Boolean = false, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val f by src.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.18f) else if (round) Color.White.copy(alpha = 0.12f) else Color.Transparent,
            focusedContainerColor = Color.White,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        interactionSource = src,
    ) {
        Box(Modifier.padding(horizontal = if (text == null) 6.dp else 12.dp, vertical = if (text == null) 6.dp else 5.dp), contentAlignment = Alignment.Center) {
            if (text == null && icon != null) Icon(icon, null, tint = if (f) T.Bg else Color.White.copy(alpha = 0.85f), modifier = Modifier.size(12.dp))
            else Text(text ?: "", style = ts(11.sp, if (selected || f) 650 else 550, if (f) T.Bg else if (selected) Color.White else Color.White.copy(alpha = 0.7f)))
        }
    }
}

@Composable
fun Wordmark(size: Float = 19f) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text("tony", style = ts(size.sp, 900, Color.White, (-0.6).sp))
        Box(Modifier.padding(start = 1.5.dp, bottom = (size * 0.2f).dp).size((size * 0.26f).dp).clip(CircleShape).background(T.Accent))
    }
}

@Composable
fun ToastHost(text: String?, modifier: Modifier = Modifier) {
    if (text == null) return
    Box(modifier.fillMaxWidth().padding(top = 22.dp), contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(T.Panel).border(0.5.dp, T.HairlineStrong, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Heart, null, tint = T.Accent, modifier = Modifier.size(10.dp))
            Text(text, style = ts(10.sp, 600, T.Text))
        }
    }
}

@Composable
fun EmptyCard(text: String, wide: Boolean = true) {
    Box(
        Modifier.padding(start = T.Margin).size(if (wide) 330.dp else 260.dp, 44.dp)
            .clip(RoundedCornerShape(T.RadiusWide)).border(0.5.dp, T.HairlineStrong, RoundedCornerShape(T.RadiusWide))
            .background(Color.White.copy(alpha = 0.03f)),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.HeartOutline, null, tint = T.Muted, modifier = Modifier.size(11.dp))
            Text(text, style = ts(10.sp, 500, T.Text3))
        }
    }
}
