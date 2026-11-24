package id.oscarmandala.otacix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oscarmandala.otacix.OtacixApp

@Composable
fun LibraryScreen(ui: UiState) {
    val store = OtacixApp.store
    val db by store.db.collectAsStateWithLifecycle()
    var newDlg by remember { mutableStateOf(false) }

    LazyColumn(
        state = ui.lists[2], modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 220.dp),
    ) {
        item { ScreenTitle("Pustaka") }
        item {
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().tap { ui.playlist = "liked" }
                    .glass(RoundedCornerShape(26.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(BlueBright, Blue))),
                    contentAlignment = Alignment.Center,
                ) { Icon(Ic.Heart, null, Modifier.size(30.dp), tint = Yellow) }
                Column {
                    Txt("Lagu disukai", 17.sp, FontWeight.Bold)
                    Txt("${db.liked.size} lagu", 13.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        item {
            Row(Modifier.padding(start = 20.dp, end = 16.dp, top = 26.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt("Playlist", 22.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
                PillButton("Baru", Ic.Add, primary = true) { newDlg = true }
            }
        }
        if (db.playlists.isEmpty()) {
            item {
                Txt("Belum ada playlist. Buat satu, lalu tambahkan lagu dari menu ⋮ di mana saja.", 14.sp,
                    color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 20.dp))
            }
        } else {
            val rows = db.playlists.chunked(2)
            itemsIndexed(rows) { _, pair ->
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { pl ->
                        Column(
                            Modifier.weight(1f).tap { ui.playlist = pl.id }.glass(RoundedCornerShape(24.dp)).padding(12.dp),
                        ) {
                            Mosaic(pl.tracks, Modifier.fillMaxWidth().aspectRatio(1f), 16.dp)
                            Txt(pl.name, 15.sp, FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 10.dp))
                            Txt("${pl.tracks.size} lagu", 12.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (db.history.isNotEmpty()) {
            item { SectionTitle("Terakhir diputar") }
            val recent = db.history.take(10).map { it.track }
            itemsIndexed(recent) { i, t ->
                TrackRow(t, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onMore = { ui.openMenu(t) }) {
                    OtacixApp.player.play(recent, i)
                }
            }
        }
    }

    if (newDlg) TextInputDialog("Playlist baru", "", "Buat", { newDlg = false }) { name ->
        val id = store.addPlaylist(name)
        ui.playlist = id
    }
}

/** Detail playlist (id == "liked" untuk lagu disukai). Tampil sebagai lapisan di atas tab. */
@Composable
fun PlaylistScreen(ui: UiState, id: String) {
    val ctx = LocalContext.current
    val store = OtacixApp.store
    val player = OtacixApp.player
    val db by store.db.collectAsStateWithLifecycle()
    val liked = id == "liked"
    val pl = db.playlists.firstOrNull { it.id == id }
    val name = if (liked) "Lagu disukai" else pl?.name.orEmpty()
    val tracks = if (liked) db.liked else pl?.tracks.orEmpty()
    var rename by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf(false) }

    // playlist sudah dihapus -> tutup
    LaunchedEffect(pl == null, liked) { if (!liked && pl == null && ui.playlist == id) ui.playlist = null }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 220.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) { IconBtn(Ic.Close, 44.dp, 22.dp, glass = true) { ui.playlist = null } }
                Mosaic(tracks, Modifier.padding(top = 8.dp).size(190.dp), 28.dp)
                Txt(name, 26.sp, FontWeight.ExtraBold, align = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = 16.dp))
                Txt("${tracks.size} lagu", 13.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("Putar", Ic.Play, primary = true) {
                        if (tracks.isEmpty()) toast(ctx, "Playlist masih kosong") else player.play(tracks, 0)
                    }
                    PillButton("Acak", Ic.Shuffle) {
                        if (tracks.isEmpty()) toast(ctx, "Playlist masih kosong") else player.play(tracks.shuffled(), 0)
                    }
                    PillButton("Poster", Ic.Image) { ui.posterFor = id; ui.poster = true }
                }
                if (!liked) Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("Ganti nama", Ic.Edit) { rename = true }
                    PillButton("Hapus", Ic.Delete) { del = true }
                }
                Spacer(Modifier.size(10.dp))
            }
        }
        if (tracks.isEmpty()) item {
            Txt("Belum ada lagu. Tambahkan lewat menu ⋮ pada lagu mana pun.", 14.sp, color = Color.White.copy(alpha = 0.6f),
                align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 20.dp))
        }
        itemsIndexed(tracks) { i, t ->
            TrackRow(
                t,
                onMore = {
                    ui.openMenu(t, if (liked) null else ("Hapus dari playlist" to { store.removeFromPlaylist(id, i) }))
                },
            ) { player.play(tracks, i) }
        }
    }

    if (rename) TextInputDialog("Ganti nama playlist", name, "Simpan", { rename = false }) { store.renamePlaylist(id, it) }
    if (del) ConfirmDialog("Hapus playlist?", "\"$name\" akan dihapus dari perangkat ini.", "Hapus", { del = false }) {
        store.deletePlaylist(id); ui.playlist = null
    }
}
