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

private val dimFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(T.Dim, T.Dim, T.Dim, 1f) })

/** Focusable card with Tony's hairline at rest and lift + crisp white edge on focus. */
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
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.size(width, height).onFocusChanged { if (it.isFocused) onFocus() },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(radius)),
        colors = ClickableSurfaceDefaults.colors(containerColor = T.Panel, focusedContainerColor = T.Panel, pressedContainerColor = T.Panel),
        scale = ClickableSurfaceDefaults.scale(focusedScale = T.FocusScale),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(0.5.dp, T.Hairline)),
            focusedBorder = Border(BorderStroke(T.FocusEdge, Color.White)),
        ),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(Color.Black.copy(alpha = 0.6f), 12.dp)),
        interactionSource = src,
    ) { content(focused) }
}

/** A channel tile: live still (or brand-colour fallback), logo badge, chips. */
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
    val w = if (wide) T.WideW else T.StdW
    val h = if (wide) T.WideH else T.StdH
    var focusedLabel by remember { mutableStateOf(false) }
    Column(modifier.width(w), verticalArrangement = Arrangement.spacedBy(if (wide) 6.dp else 5.dp)) {
        TonyCard(w, h, if (wide) T.RadiusWide else T.RadiusStd, onClick, onLongClick = onLongClick, onFocus = onFocus,
            modifier = Modifier.onFocusChanged { focusedLabel = it.hasFocus }) { focused ->
            TileArt(c, wide, focused)
            if (wide && !c.isRadio) LiveChip(Modifier.align(Alignment.BottomStart).padding(7.dp))
            if (c.isRadio) LiveChip(Modifier.align(Alignment.BottomStart).padding(if (wide) 7.dp else 5.dp), "RADIO", dot = false)
            if (watching) LiveChip(Modifier.align(Alignment.TopEnd).padding(5.dp), "WATCHING")
            if (favourite && !wide) Box(
                Modifier.align(Alignment.BottomEnd).padding(5.dp).size(14.dp).clip(RoundedCornerShape(4.dp))
                    .background(T.ChipBg).border(0.5.dp, Color.White.copy(0.14f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Heart, null, tint = T.Accent, modifier = Modifier.size(7.5.dp)) }
        }
        Column(Modifier.padding(start = 1.dp, top = if (focusedLabel) 4.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(c.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = ts(if (wide) 10.5.sp else 9.5.sp, if (focusedLabel) 650 else 500, if (focusedLabel) Color.White else T.Text3))
            if (sub != null) Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis, style = ts(8.sp, 500, T.Muted))
        }
    }
}

@Composable
fun BoxScope.TileArt(c: Channel, wide: Boolean, focused: Boolean) {
    var stillOk by remember(c.id) { mutableStateOf(false) }
    // Fallback underneath: brand colour + centred logo (shown when no live still is available)
    Box(Modifier.fillMaxSize().background(parseColor(c.tileColor, T.Surface)), contentAlignment = Alignment.Center) {
        if (!stillOk) LogoBadge(c, if (wide) BadgeCentre else BadgeWide)
    }
    if (!c.isYouTube && !c.isRadio) AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(Repository.stillUrl(c)).crossfade(true).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        colorFilter = if (focused) null else dimFilter,
        onSuccess = { stillOk = true },
        onError = { stillOk = false },
        modifier = Modifier.fillMaxSize(),
    )
    if (stillOk) {
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(if (wide) 48.dp else 30.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))))
        LogoBadge(c, if (wide) BadgeWide else BadgeStd, Modifier.align(Alignment.TopStart).padding(if (wide) 7.dp else 5.5.dp))
    }
}

/** Primary/secondary pill-rect button. Focused = solid white (CLAUDE.md 7.1, v7). */
@Composable
fun TonyButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, iconTint: Color? = null, primary: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = modifier.height(29.dp),
        shape = ButtonDefaults.shape(RoundedCornerShape(6.dp)),
        colors = ButtonDefaults.colors(
            containerColor = if (primary) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.07f),
            contentColor = T.Text,
            focusedContainerColor = Color.White,
            focusedContentColor = T.Bg,
            pressedContainerColor = Color.White.copy(alpha = 0.85f),
            pressedContentColor = T.Bg,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.05f),
        border = ButtonDefaults.border(border = Border(BorderStroke(0.5.dp, T.HairlineStrong)), focusedBorder = Border.None),
        glow = ButtonDefaults.glow(focusedGlow = Glow(Color.Black.copy(alpha = 0.5f), 10.dp)),
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(10.dp), tint = iconTint ?: androidx.tv.material3.LocalContentColor.current)
            Spacer(Modifier.width(5.5.dp))
        }
        Text(text, style = ts(10.5.sp, 650, Color.Unspecified))
    }
}

@Composable
fun RowHeader(title: String, count: Int? = null, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(start = T.Margin, bottom = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = ts(13.sp, 700, T.Text, (-0.15).sp))
            if (count != null && count > 0) Box(Modifier.border(0.5.dp, T.HairlineStrong, RoundedCornerShape(3.5.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                Text("$count", style = ts(7.sp, 600, T.Text3))
            }
        }
        if (subtitle != null) Text(subtitle, style = ts(8.5.sp, 400, T.Muted))
    }
}

@Composable
fun Clock() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(15_000) } }
    Text(SimpleDateFormat("h:mm", Locale.getDefault()).format(now), style = ts(10.sp, 500, T.Text2, 0.1.sp))
}

enum class Tab { Home, Favourites, Search }

@Composable
fun TopBar(selected: Tab?, onTab: (Tab) -> Unit, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(start = T.Margin, end = T.Margin, top = T.Top), verticalAlignment = Alignment.CenterVertically) {
        Wordmark()
        Spacer(Modifier.width(24.dp))
        Row(
            Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.04f)).border(0.5.dp, T.Hairline, CircleShape).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Tab.entries.forEach { t ->
                val src = remember { MutableInteractionSource() }
                val f by src.collectIsFocusedAsState()
                Surface(
                    onClick = { onTab(t) },
                    shape = ClickableSurfaceDefaults.shape(CircleShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (t == selected) Color.White.copy(alpha = 0.13f) else Color.Transparent,
                        focusedContainerColor = Color.White,
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                    interactionSource = src,
                ) {
                    Text(t.name, style = ts(9.5.sp, if (t == selected || f) 600 else 500, if (f) T.Bg else if (t == selected) Color.White else T.Text3),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.5.dp))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Clock()
        Spacer(Modifier.width(14.dp))
        val src = remember { MutableInteractionSource() }
        val f by src.collectIsFocusedAsState()
        Surface(
            onClick = onSettings,
            modifier = Modifier.size(23.dp),
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.04f), focusedContainerColor = Color.White),
            border = ClickableSurfaceDefaults.border(border = Border(BorderStroke(0.5.dp, Color.White.copy(0.12f)))),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
            interactionSource = src,
        ) { Icon(Icons.Gear, "Settings", tint = if (f) T.Bg else T.Text2, modifier = Modifier.size(11.dp).align(Alignment.Center)) }
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
