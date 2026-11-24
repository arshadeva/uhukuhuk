package id.oscarmandala.otacix.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.oscarmandala.otacix.data.Track
import id.oscarmandala.otacix.data.isItunesId
import id.oscarmandala.otacix.util.Tier

fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

fun shareTrack(ctx: Context, t: Track) {
    val link = if (isItunesId(t.id)) "https://music.youtube.com/search?q=" + android.net.Uri.encode("${t.artist} ${t.title}") else "https://music.youtube.com/watch?v=${t.id}"
    val i = Intent(Intent.ACTION_SEND).setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, "${t.title} - ${t.artist}\n$link\n(via Otacix by Oscar Mandala)")
    ctx.startActivity(Intent.createChooser(i, "Bagikan lagu"))
}

fun fmtTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

@Composable
fun Txt(
    text: String, size: TextUnit = 14.sp, weight: FontWeight = FontWeight.Normal, color: Color = Color.White,
    maxLines: Int = Int.MAX_VALUE, align: TextAlign? = null, modifier: Modifier = Modifier,
) = Text(
    text = text, fontSize = size, fontWeight = weight, color = color, maxLines = maxLines,
    overflow = TextOverflow.Ellipsis, textAlign = align, modifier = modifier,
)

/** Sampul persegi. Sampul YouTube (4:3 berbingkai hitam) diperbesar sedikit agar bingkainya terpotong. */
@Composable
fun Cover(url: String, modifier: Modifier = Modifier, radius: Dp = 16.dp) {
    val crop = url.contains("ytimg.com")
    Box(modifier.clip(RoundedCornerShape(radius)).background(Color.White.copy(alpha = 0.10f))) {
        AsyncImage(
            model = url, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { if (crop) { scaleX = 1.34f; scaleY = 1.34f } },
        )
    }
}

/** Mosaik sampul untuk playlist: kosong -> ikon, 1-3 lagu -> satu sampul, 4+ -> grid 2x2. */
@Composable
fun Mosaic(tracks: List<Track>, modifier: Modifier = Modifier, radius: Dp = 18.dp) {
    Box(modifier.clip(RoundedCornerShape(radius)).background(Brush.linearGradient(listOf(Blue, Navy)))) {
        when {
            tracks.isEmpty() -> Icon(Ic.Library, null, Modifier.align(Alignment.Center).size(40.dp), tint = Color.White.copy(alpha = 0.7f))
            tracks.size < 4 -> Cover(tracks[0].cover, Modifier.fillMaxSize(), 0.dp)
            else -> Column(Modifier.fillMaxSize()) {
                for (r in 0..1) Row(Modifier.weight(1f)) {
                    for (c in 0..1) Cover(tracks[r * 2 + c].cover, Modifier.weight(1f).fillMaxHeight(), 0.dp)
                }
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean = false, icon: ImageVector? = null, emoji: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.tap(onClick)
            .glass(RoundedCornerShape(18.dp), tint = if (selected) Yellow else Color.White, strength = if (selected) 1.7f else 1f)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (emoji != null) Txt(emoji, 15.sp)
        if (icon != null) Icon(icon, null, Modifier.size(17.dp), tint = if (selected) YellowSoft else Color.White)
        Txt(text, 14.sp, FontWeight.SemiBold, if (selected) YellowSoft else Color.White)
    }
}

@Composable
fun PillButton(text: String, icon: ImageVector? = null, primary: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val look = if (primary) Modifier.clip(shape).background(Yellow) else Modifier.glass(shape)
    Row(
        modifier.tap(onClick).then(look).padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = if (primary) Navy else Color.White)
        Txt(text, 14.sp, FontWeight.Bold, if (primary) Navy else Color.White)
    }
}

@Composable
fun IconBtn(
    icon: ImageVector, size: Dp = 48.dp, iconSize: Dp = 22.dp, tint: Color = Color.White,
    solid: Color? = null, glass: Boolean = false, onClick: () -> Unit,
) {
    val look = when {
        solid != null -> Modifier.clip(CircleShape).background(solid)
        glass -> Modifier.glass(CircleShape)
        else -> Modifier
    }
    Box(Modifier.size(size).tap(onClick).then(look), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(iconSize), tint = tint)
    }
}

@Composable
fun SectionTitle(text: String) =
    Txt(text, 22.sp, FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 12.dp))

@Composable
fun ScreenTitle(text: String) =
    Txt(text, 34.sp, FontWeight.ExtraBold, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp))

/** Kerangka pemuatan; berdenyut hanya di Tier.FULL. */
@Composable
fun Skeleton(modifier: Modifier = Modifier, radius: Dp = 20.dp) {
    if (LocalTier.current == Tier.FULL) {
        val a = rememberInfiniteTransition(label = "sk").animateFloat(0.06f, 0.18f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
        Box(modifier.clip(RoundedCornerShape(radius)).drawBehind { drawRect(Color.White.copy(alpha = a.value)) })
    } else {
        Box(modifier.clip(RoundedCornerShape(radius)).background(Color.White.copy(alpha = 0.10f)))
    }
}

/** Kartu persegi (sampul + judul + artis) untuk rak horizontal / grid. */
@Composable
fun SquareCard(t: Track, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.tap(onClick)) {
        Cover(t.cover, Modifier.fillMaxWidth().aspectRatio(1f), 20.dp)
        Txt(t.title, 14.sp, FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
        Txt(t.artist, 12.5.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun QuickTile(t: Track, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.tap(onClick).height(60.dp).glass(RoundedCornerShape(18.dp)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Cover(t.cover, Modifier.size(60.dp), 0.dp)
        Txt(t.title, 14.sp, FontWeight.SemiBold, maxLines = 2, modifier = Modifier.weight(1f).padding(end = 10.dp))
    }
}

/** Baris lagu: sampul, judul, artis, tombol menu. */
@Composable
fun TrackRow(t: Track, modifier: Modifier = Modifier, onMore: (() -> Unit)? = null, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().tap(onClick).glass(RoundedCornerShape(22.dp)).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Cover(t.cover, Modifier.size(52.dp), 14.dp)
        Column(Modifier.weight(1f)) {
            Txt(t.title, 15.sp, FontWeight.SemiBold, maxLines = 1)
            Txt(t.artist, 12.5.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        }
        if (onMore != null) IconBtn(Ic.More, 36.dp, 20.dp, Color.White.copy(alpha = 0.8f), onClick = onMore)
    }
}
