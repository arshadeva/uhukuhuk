package id.oscarmandala.otacix.ui

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.compose.AsyncImage
import id.oscarmandala.otacix.OtacixApp
import id.oscarmandala.otacix.util.Tier

private val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f)

@Composable
fun PlayerScreen(ui: UiState) {
    val ctx = LocalContext.current
    val tier = LocalTier.current
    val player = OtacixApp.player
    val store = OtacixApp.store
    val p by player.ui.collectAsStateWithLifecycle()
    val prog by player.progress.collectAsStateWithLifecycle()
    val sleep by player.sleep.collectAsStateWithLifecycle()
    val db by store.db.collectAsStateWithLifecycle()
    var queueOpen by remember { mutableStateOf(false) }
    var lyricsOpen by remember { mutableStateOf(false) }
    var sleepDlg by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf<Float?>(null) }

    val t = p.track
    LaunchedEffect(t == null) { if (t == null) ui.playerOpen = false }
    if (t == null) return
    val liked = store.isLiked(db, t.id)
    val coverScale by animateFloatAsState(if (p.playing) 1f else 0.86f, tween(if (tier == Tier.FULL) 500 else 150), label = "cover")

    Box(Modifier.fillMaxSize().background(Navy).clickable(remember { MutableInteractionSource() }, null) { }) {
        // latar: sampul diburamkan (HP spek atas, Android 12+) atau aurora biasa
        if (tier == Tier.FULL && Build.VERSION.SDK_INT >= 31) {
            AsyncImage(
                model = t.cover, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(70.dp).graphicsLayer { alpha = 0.6f },
            )
        } else AuroraBackground(BlueBright)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Navy.copy(alpha = 0.30f), Navy.copy(alpha = 0.88f)))))

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBtn(Ic.Down, 44.dp, 24.dp, glass = true) { ui.playerOpen = false }
                Txt("Sedang diputar", 13.sp, FontWeight.SemiBold, Color.White.copy(alpha = 0.7f), align = TextAlign.Center, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconBtn(Ic.Lyrics, 44.dp, 22.dp, if (lyricsOpen) Yellow else Color.White, glass = true) { lyricsOpen = !lyricsOpen; if (lyricsOpen) queueOpen = false }
                    IconBtn(Ic.Queue, 44.dp, 22.dp, if (queueOpen) Yellow else Color.White, glass = true) { queueOpen = !queueOpen; if (queueOpen) lyricsOpen = false }
                }
            }

            if (queueOpen) {
                LazyColumn(
                    Modifier.padding(top = 16.dp).fillMaxWidth().height(340.dp).glass(RoundedCornerShape(28.dp)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(p.queue) { i, q ->
                        val now = i == p.index
                        Row(
                            Modifier.fillMaxWidth().tap { player.jumpTo(i) }.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Cover(q.cover, Modifier.size(44.dp), 12.dp)
                            Column(Modifier.weight(1f)) {
                                Txt(q.title, 14.sp, FontWeight.SemiBold, if (now) Yellow else Color.White, maxLines = 1)
                                Txt(q.artist, 12.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1)
                            }
                            if (!now) IconBtn(Ic.Close, 32.dp, 16.dp, Color.White.copy(alpha = 0.6f)) { player.removeFromQueue(i) }
                        }
                    }
                }
            } else if (lyricsOpen) {
                LyricsPanel(t, prog.pos) { player.seekTo(it) }
            } else {
                Cover(
                    t.cover,
                    Modifier.padding(top = 22.dp).widthIn(max = 380.dp).fillMaxWidth().aspectRatio(1f)
                        .graphicsLayer { scaleX = coverScale; scaleY = coverScale },
                    32.dp,
                )
            }

            Txt(t.title, 24.sp, FontWeight.ExtraBold, maxLines = 2, align = TextAlign.Center, modifier = Modifier.padding(top = 24.dp))
            Txt(if (p.buffering) "Memuat…" else t.artist, 15.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1, modifier = Modifier.padding(top = 4.dp))

            // bilah seek
            val frac = if (prog.dur > 0) (prog.pos.toFloat() / prog.dur).coerceIn(0f, 1f) else 0f
            Slider(
                value = drag ?: frac,
                onValueChange = { drag = it },
                onValueChangeFinished = { drag?.let { player.seekTo((it * prog.dur).toLong()) }; drag = null },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White, activeTrackColor = Yellow, inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                ),
                modifier = Modifier.padding(top = 14.dp),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                Txt(fmtTime(((drag ?: frac) * prog.dur).toLong()), 12.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.weight(1f))
                Txt(fmtTime(prog.dur), 12.sp, color = Color.White.copy(alpha = 0.6f))
            }

            // kontrol utama
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconBtn(Ic.Shuffle, 48.dp, 24.dp, if (p.shuffle) Yellow else Color.White.copy(alpha = 0.7f)) { player.setShuffle(!p.shuffle) }
                IconBtn(Ic.Prev, 56.dp, 32.dp) { player.prev() }
                IconBtn(if (p.playing) Ic.Pause else Ic.Play, 76.dp, 38.dp, Navy, solid = Yellow) { player.toggle() }
                IconBtn(Ic.Next, 56.dp, 32.dp) { player.next() }
                IconBtn(
                    if (p.repeat == Player.REPEAT_MODE_ONE) Ic.RepeatOne else Ic.Repeat, 48.dp, 24.dp,
                    if (p.repeat == Player.REPEAT_MODE_OFF) Color.White.copy(alpha = 0.7f) else Yellow,
                ) { player.cycleRepeat() }
            }

            // kontrol tambahan
            Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                Chip(if (liked) "Disukai" else "Suka", liked, if (liked) Ic.Heart else Ic.HeartOutline) { store.toggleLike(t) }
                Chip("Playlist", icon = Ic.Add) { ui.addTo = t }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                val sleepLabel = when {
                    sleep == -1L -> "Akhir lagu"
                    sleep > 0 -> "Tidur ${(sleep / 60_000) + 1}m"
                    else -> "Tidur"
                }
                Chip(sleepLabel, sleep != 0L, Ic.Moon) { sleepDlg = true }
                Chip("${if (p.speed == 1f) "1" else p.speed.toString().trimEnd('0')}×", p.speed != 1f, Ic.Speed) {
                    val i = SPEEDS.indexOfFirst { it == p.speed }
                    player.setSpeed(SPEEDS[(i + 1) % SPEEDS.size])
                }
                Chip("Bagikan", icon = Ic.Share) { shareTrack(ctx, t) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (sleepDlg) SleepDialog { sleepDlg = false }
}
