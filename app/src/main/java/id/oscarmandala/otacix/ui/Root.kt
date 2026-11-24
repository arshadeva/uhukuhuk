package id.oscarmandala.otacix.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import id.oscarmandala.otacix.OtacixApp
import id.oscarmandala.otacix.data.SearchResult
import id.oscarmandala.otacix.data.Track
import id.oscarmandala.otacix.player.PlayerUi
import id.oscarmandala.otacix.util.Device
import id.oscarmandala.otacix.util.Tier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Status navigasi sederhana (tanpa library navigasi tambahan -> ringan untuk HP spek rendah). */
class UiState {
    var tab by mutableIntStateOf(0)
    var playerOpen by mutableStateOf(false)
    var playlist by mutableStateOf<String?>(null)     // "liked" atau id playlist
    var poster by mutableStateOf(false)
    var posterFor by mutableStateOf<String?>(null)
    var pendingSearch by mutableStateOf<String?>(null)
    var menu by mutableStateOf<Track?>(null)
    var menuExtra by mutableStateOf<Pair<String, () -> Unit>?>(null)
    var addTo by mutableStateOf<Track?>(null)

    // status pencarian disimpan di sini agar tidak hilang saat pindah tab
    var sq by mutableStateOf("")
    var sres by mutableStateOf<SearchResult?>(null)
    var sload by mutableStateOf(false)

    val lists = List(4) { LazyListState() }

    fun openMenu(t: Track, extra: Pair<String, () -> Unit>? = null) { menuExtra = extra; menu = t }
}

object PaletteUtil {
    /** Warna dominan sampul (gambar kecil 96px supaya ringan). */
    suspend fun accentFor(ctx: Context, url: String): Color? = withContext(Dispatchers.Default) {
        val res = ctx.imageLoader.execute(ImageRequest.Builder(ctx).data(url).size(96).allowHardware(false).build())
        val bmp = ((res as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap ?: return@withContext null
        val p = Palette.from(bmp).maximumColorCount(12).generate()
        val sw = p.vibrantSwatch ?: p.lightVibrantSwatch ?: p.dominantSwatch ?: return@withContext null
        Color(sw.rgb)
    }
}

@Composable
fun OtacixRoot() {
    val ctx = LocalContext.current
    val db by OtacixApp.store.db.collectAsStateWithLifecycle()
    val tier = remember(db.perf) { Device.resolve(ctx, db.perf).also { OtacixApp.tier = it } }
    val tilt = rememberTilt(tier == Tier.FULL)
    val ui = remember { UiState() }
    var splash by rememberSaveable { mutableStateOf(true) }
    var support by rememberSaveable { mutableStateOf(true) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(splash) {
        if (!splash && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    CompositionLocalProvider(LocalTier provides tier, LocalTilt provides tilt) {
        OtacixTheme {
            Box(Modifier.fillMaxSize().background(Navy)) {
                AppContent(ui)
                if (splash) SplashOverlay { splash = false }
                if (!splash && support) SupportDialog { support = false }
            }
        }
    }
}

@Composable
private fun AppContent(ui: UiState) {
    val ctx = LocalContext.current
    val tier = LocalTier.current
    val player = OtacixApp.player
    val p by player.ui.collectAsStateWithLifecycle()
    var accent by remember { mutableStateOf(BlueBright) }

    LaunchedEffect(p.track?.cover, tier) {
        val url = p.track?.cover
        accent = if (tier == Tier.FULL && url != null) {
            PaletteUtil.accentFor(ctx, url)?.let { lerp(it, Blue, 0.3f) } ?: accent
        } else BlueBright
    }

    // urutan BackHandler: yang terakhir didaftarkan menang -> pemutar paling atas
    BackHandler(enabled = ui.playlist != null) { ui.playlist = null }
    BackHandler(enabled = ui.poster) { ui.poster = false }
    BackHandler(enabled = ui.playerOpen) { ui.playerOpen = false }

    var lastPlaylist by remember { mutableStateOf("liked") }
    ui.playlist?.let { lastPlaylist = it }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground(accent)

        Box(Modifier.fillMaxSize()) {
            when (ui.tab) {
                0 -> HomeScreen(ui)
                1 -> SearchScreen(ui)
                2 -> LibraryScreen(ui)
                else -> ProfileScreen(ui)
            }
        }

        AnimatedVisibility(
            visible = ui.playlist != null,
            enter = slideInHorizontally(tween(320)) { it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(260)) { it } + fadeOut(tween(200)),
        ) {
            Box(Modifier.fillMaxSize().background(Navy)) {
                AuroraBackground(accent)
                PlaylistScreen(ui, lastPlaylist)
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniPlayer(ui, p)
            NavBar(ui)
        }

        AnimatedVisibility(
            visible = ui.poster,
            enter = slideInVertically(tween(320)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(200)),
        ) {
            Box(Modifier.fillMaxSize().background(Navy)) {
                AuroraBackground(accent)
                PosterScreen(ui)
            }
        }

        AnimatedVisibility(
            visible = ui.playerOpen,
            enter = slideInVertically(tween(420)) { it },
            exit = slideOutVertically(tween(300)) { it },
        ) { PlayerScreen(ui) }

        ui.menu?.let { TrackMenu(it, ui) }
        ui.addTo?.let { AddToPlaylistDialog(it, ui) }
    }
}

@Composable
private fun NavBar(ui: UiState) {
    val tier = LocalTier.current
    val items = listOf("Beranda" to Ic.Home, "Cari" to Ic.Search, "Pustaka" to Ic.Library, "Kamu" to Ic.Person)
    val density = LocalDensity.current
    BoxWithConstraints(
        Modifier.padding(horizontal = 14.dp).widthIn(max = 560.dp).fillMaxWidth()
            .glass(RoundedCornerShape(34.dp), base = Navy.copy(alpha = if (tier == Tier.LITE) 0.92f else 0.55f))
            .padding(6.dp)
    ) {
        val w = maxWidth / 4
        val wPx = with(density) { w.toPx() }
        // posisi kiri pil saat digeser (px); null = tidak sedang digeser
        var dragX by remember { mutableStateOf<Float?>(null) }
        val dragging = dragX != null
        val hover = dragX?.let { ((it + wPx / 2f) / wPx).toInt().coerceIn(0, 3) } ?: ui.tab
        val x by animateDpAsState(w * ui.tab, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium), label = "pill")
        val bulge by animateFloatAsState(if (dragging) 1.14f else 1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "bulge")
        Box(
            Modifier.fillMaxWidth().pointerInput(wPx) {
                detectDragGestures(
                    onDragStart = { o -> dragX = (o.x - wPx / 2f).coerceIn(0f, wPx * 3f) },
                    onDrag = { change, amt ->
                        change.consume()
                        dragX = ((dragX ?: 0f) + amt.x).coerceIn(0f, wPx * 3f)
                    },
                    onDragEnd = {
                        val d = dragX
                        dragX = null
                        if (d != null) {
                            val idx = ((d + wPx / 2f) / wPx).toInt().coerceIn(0, 3)
                            if (idx != ui.tab) { ui.tab = idx; ui.playlist = null }
                        }
                    },
                    onDragCancel = { dragX = null },
                )
            }
        ) {
            Box(
                Modifier.offset { IntOffset(dragX?.toInt() ?: x.roundToPx(), 0) }.width(w).height(58.dp)
                    .graphicsLayer { scaleX = bulge; scaleY = bulge }
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Yellow.copy(alpha = if (dragging) 0.46f else 0.32f), Yellow.copy(alpha = if (dragging) 0.2f else 0.12f))))
            )
            Row(Modifier.fillMaxWidth()) {
                items.forEachIndexed { i, (label, icon) ->
                    val on = hover == i
                    Column(
                        Modifier.weight(1f).height(58.dp)
                            .clickable(remember { MutableInteractionSource() }, null) { ui.tab = i; ui.playlist = null },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(icon, label, Modifier.size(24.dp), tint = if (on) Yellow else Color.White.copy(alpha = 0.6f))
                        Txt(label, 11.sp, FontWeight.SemiBold, if (on) Yellow else Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(ui: UiState, p: PlayerUi) {
    val t = p.track ?: return
    val player = OtacixApp.player
    val store = OtacixApp.store
    val db by store.db.collectAsStateWithLifecycle()
    val prog by player.progress.collectAsStateWithLifecycle()
    val liked = store.isLiked(db, t.id)
    val frac = if (prog.dur > 0) (prog.pos.toFloat() / prog.dur).coerceIn(0f, 1f) else 0f

    Box(
        Modifier.padding(horizontal = 14.dp).widthIn(max = 560.dp).fillMaxWidth()
            .tap { ui.playerOpen = true }
            .glass(RoundedCornerShape(30.dp), base = Navy.copy(alpha = 0.55f))
    ) {
        Row(
            Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Cover(t.cover, Modifier.size(48.dp), 16.dp)
            Column(Modifier.weight(1f)) {
                Txt(t.title, 14.sp, FontWeight.SemiBold, maxLines = 1)
                Txt(if (p.buffering) "Memuat…" else t.artist, 12.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1)
            }
            IconBtn(if (liked) Ic.Heart else Ic.HeartOutline, 40.dp, 21.dp, if (liked) Yellow else Color.White.copy(alpha = 0.85f)) { store.toggleLike(t) }
            IconBtn(if (p.playing) Ic.Pause else Ic.Play, 46.dp, 22.dp, Navy, solid = Yellow) { player.toggle() }
            IconBtn(Ic.Next, 40.dp, 22.dp) { player.next() }
        }
        Box(
            Modifier.align(Alignment.BottomStart).padding(horizontal = 24.dp).padding(bottom = 4.dp)
                .fillMaxWidth().height(3.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.18f))
        ) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Yellow))
        }
    }
}
