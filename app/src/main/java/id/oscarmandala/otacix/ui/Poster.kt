package id.oscarmandala.otacix.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oscarmandala.otacix.OtacixApp
import id.oscarmandala.otacix.data.Track
import id.oscarmandala.otacix.util.PosterRenderer
import id.oscarmandala.otacix.util.Tier
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Poster Studio: playlist -> poster liquid glass (simpan ke galeri / bagikan). */
@Composable
fun PosterScreen(ui: UiState) {
    val ctx = LocalContext.current
    val tier = LocalTier.current
    val scope = rememberCoroutineScope()
    val db by OtacixApp.store.db.collectAsStateWithLifecycle()

    val sources = listOf("liked" to "Disukai") + db.playlists.map { it.id to it.name } + ("history" to "Riwayat")
    var sel by remember { mutableStateOf(ui.posterFor ?: "liked") }
    var style by remember { mutableIntStateOf(0) }
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var bg by remember { mutableStateOf<Uri?>(null) }
    var tpl by remember { mutableIntStateOf(0) }
    var layout by remember { mutableStateOf<List<PosterRenderer.CardPose>?>(null) }
    var rev by remember { mutableIntStateOf(0) }
    var edit by remember { mutableStateOf(false) }
    var selCard by remember { mutableIntStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u: Uri? -> if (u != null) bg = u }

    val tracks: List<Track> = when (sel) {
        "liked" -> db.liked
        "history" -> db.history.map { it.track }
        else -> db.playlists.firstOrNull { it.id == sel }?.tracks.orEmpty()
    }
    val title = sources.firstOrNull { it.first == sel }?.second ?: "Playlist"

    val n = minOf(4, tracks.size)
    val poses = layout?.takeIf { it.size == n } ?: PosterRenderer.template(tpl, n)

    fun tweak(f: (PosterRenderer.CardPose) -> PosterRenderer.CardPose) {
        if (poses.isEmpty()) return
        val i = selCard.coerceIn(0, poses.lastIndex)
        val l = poses.toMutableList()
        l[i] = f(l[i])
        layout = l
        rev++
    }

    var lastKey by remember { mutableStateOf<List<Any?>?>(null) }
    LaunchedEffect(sel, style, tracks.size, bg, rev) {
        val key = listOf(sel, style, tracks.size, bg)
        if (lastKey != key) bmp = null   // hanya ganti sumber/gaya/foto yang mengosongkan pratinjau; geser kartu tidak
        lastKey = key
        if (tracks.isEmpty()) { bmp = null; return@LaunchedEffect }
        busy = true
        bmp = try { PosterRenderer.render(ctx, title, tracks, style, tier == Tier.LITE, bg, if (style == 0) poses else null) } catch (e: Throwable) { null }
        busy = false
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBtn(Ic.Close, 44.dp, 22.dp, glass = true) { ui.poster = false; ui.posterFor = null }
            Txt("Poster Studio", 22.sp, FontWeight.ExtraBold)
        }
        Txt("Sumber", 13.sp, FontWeight.SemiBold, Color.White.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 8.dp, bottom = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sources) { (id, name) -> Chip(name, sel == id) { sel = id } }
        }
        Txt("Gaya", 13.sp, FontWeight.SemiBold, Color.White.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Kartu", style == 0) { style = 0 }
            Chip("Grid 3×3", style == 1) { style = 1 }
            Chip("Tumpuk", style == 2) { style = 2 }
        }
        if (style == 0) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Foto latar", Ic.Image) { picker.launch("image/*") }
                if (bg != null) PillButton("Hapus foto", Ic.Close) { bg = null }
            }
            Txt("Tata letak kartu", 13.sp, FontWeight.SemiBold, Color.White.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(PosterRenderer.templateNames) { i, name -> Chip(name, tpl == i && layout == null) { tpl = i; layout = null; rev++ } }
                item { Chip(if (edit) "Selesai atur" else "Atur sendiri", edit) { edit = !edit } }
            }
            if (edit && poses.isNotEmpty()) {
                Txt("Ketuk kartu di poster lalu geser. Pakai tombol di bawah untuk memutar atau mengubah ukuran.", 12.sp, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { PillButton("Putar −") { tweak { it.copy(angle = it.angle - 3f) } } }
                    item { PillButton("Putar +") { tweak { it.copy(angle = it.angle + 3f) } } }
                    item { PillButton("Kecil") { tweak { it.copy(scale = (it.scale - 0.05f).coerceAtLeast(0.3f)) } } }
                    item { PillButton("Besar") { tweak { it.copy(scale = (it.scale + 0.05f).coerceAtMost(1.4f)) } } }
                    item { PillButton("Reset") { layout = null; rev++ } }
                }
            }
        }

        Box(
            Modifier.padding(top = 20.dp).widthIn(max = 340.dp).fillMaxWidth(0.8f).aspectRatio(2f / 3f)
                .glass(RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val b = bmp
            when {
                b != null -> Image(b.asImageBitmap(), "Poster", Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Fit)
                tracks.isEmpty() -> Txt("Sumber ini masih kosong.\nSukai lagu atau isi playlist dulu.", 13.sp, color = Color.White.copy(alpha = 0.7f), align = TextAlign.Center, modifier = Modifier.padding(20.dp))
                else -> Txt(if (busy) "Membuat poster…" else "Gagal membuat poster", 14.sp, color = Color.White.copy(alpha = 0.7f))
            }
            if (style == 0 && edit && poses.isNotEmpty()) {
                CardEditor(
                    poses = poses, sel = selCard,
                    onSel = { i -> if (i >= 0) selCard = i },
                    onChange = { layout = it },
                    onCommit = { rev++ },
                    modifier = Modifier.matchParentSize(),
                )
            }
        }

        Row(Modifier.padding(top = 20.dp, bottom = 30.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Simpan", Ic.Download, primary = true) {
                val b = bmp
                when {
                    b == null -> toast(ctx, "Poster belum siap")
                    PosterRenderer.saveToGallery(ctx, b) -> toast(ctx, "Tersimpan di Galeri › Pictures/Otacix")
                    else -> toast(ctx, "Simpan langsung butuh Android 10+. Gunakan Bagikan.")
                }
            }
            PillButton("Bagikan", Ic.Share) {
                val b = bmp
                if (b == null) toast(ctx, "Poster belum siap") else PosterRenderer.share(ctx, b)
            }
        }
    }
}

/** Lapisan atur kartu di atas pratinjau: ketuk memilih kartu, geser memindahkannya. Poster dirender ulang saat jari dilepas. */
@Composable
private fun CardEditor(
    poses: List<PosterRenderer.CardPose>,
    sel: Int,
    onSel: (Int) -> Unit,
    onChange: (List<PosterRenderer.CardPose>) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cur by rememberUpdatedState(poses)
    val curSel by rememberUpdatedState(sel)
    var boxW by remember { mutableStateOf(1f) }

    fun hit(p: Offset): Int {
        val k = boxW / PosterRenderer.POSTER_W
        val px = p.x / k
        val py = p.y / k
        for (i in cur.indices.reversed()) {
            val c = cur[i]
            val rad = Math.toRadians(-c.angle.toDouble())
            val dx = px - c.x
            val dy = py - c.y
            val lx = dx * cos(rad) - dy * sin(rad)
            val ly = dx * sin(rad) + dy * cos(rad)
            if (abs(lx) <= PosterRenderer.CARD_W / 2f * c.scale && abs(ly) <= PosterRenderer.CARD_H / 2f * c.scale) return i
        }
        return -1
    }

    Box(
        modifier
            .onSizeChanged { boxW = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) { detectTapGestures { o -> onSel(hit(o)) } }
            .pointerInput(Unit) {
                var idx = -1
                var pose: PosterRenderer.CardPose? = null
                detectDragGestures(
                    onDragStart = { o ->
                        idx = hit(o)
                        pose = if (idx >= 0) cur[idx] else null
                        if (idx >= 0) onSel(idx)
                    },
                    onDrag = { change, amt ->
                        val p0 = pose
                        if (idx >= 0 && p0 != null) {
                            change.consume()
                            val k = boxW / PosterRenderer.POSTER_W
                            val np = p0.copy(
                                x = (p0.x + amt.x / k).coerceIn(0f, PosterRenderer.POSTER_W),
                                y = (p0.y + amt.y / k).coerceIn(0f, PosterRenderer.POSTER_H),
                            )
                            pose = np
                            val l = cur.toMutableList()
                            if (idx <= l.lastIndex) { l[idx] = np; onChange(l) }
                        }
                    },
                    onDragEnd = { if (idx >= 0) onCommit(); idx = -1; pose = null },
                    onDragCancel = { idx = -1; pose = null },
                )
            }
            .drawBehind {
                val k = size.width / PosterRenderer.POSTER_W
                cur.forEachIndexed { i, c ->
                    val w = PosterRenderer.CARD_W * c.scale * k
                    val h = PosterRenderer.CARD_H * c.scale * k
                    val on = i == curSel
                    rotate(c.angle, pivot = Offset(c.x * k, c.y * k)) {
                        drawRoundRect(
                            color = if (on) Yellow else Color.White.copy(alpha = 0.7f),
                            topLeft = Offset(c.x * k - w / 2f, c.y * k - h / 2f),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(52f * c.scale * k),
                            style = Stroke(width = (if (on) 3.5f else 1.5f) * density),
                        )
                    }
                }
            }
    )
}
