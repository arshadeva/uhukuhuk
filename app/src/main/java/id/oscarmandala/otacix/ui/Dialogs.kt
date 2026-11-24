package id.oscarmandala.otacix.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import id.oscarmandala.otacix.OtacixApp
import id.oscarmandala.otacix.data.Track

@Composable
fun GlassDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp), base = NavyMid.copy(alpha = 0.96f)).padding(18.dp),
            content = content,
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, tint: Color = Color.White, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().tap(onClick).padding(vertical = 13.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = tint)
        Txt(label, 16.sp, FontWeight.Medium)
    }
}

@Composable
fun TrackMenu(t: Track, ui: UiState) {
    val ctx = LocalContext.current
    val store = OtacixApp.store
    val player = OtacixApp.player
    val db by store.db.collectAsState()
    val liked = store.isLiked(db, t.id)
    val close = { ui.menu = null; ui.menuExtra = null }
    GlassDialog(onDismiss = { close() }) {
        Txt(t.title, 17.sp, FontWeight.Bold, maxLines = 1)
        Txt(t.artist, 13.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1, modifier = Modifier.padding(bottom = 8.dp))
        ActionRow(Ic.Next, "Putar berikutnya") { player.playNext(t); toast(ctx, "Ditambahkan sebagai berikutnya"); close() }
        ActionRow(Ic.Queue, "Tambah ke antrian") { player.addToQueue(t); toast(ctx, "Masuk antrian"); close() }
        ActionRow(if (liked) Ic.Heart else Ic.HeartOutline, if (liked) "Batal suka" else "Suka", if (liked) Yellow else Color.White) {
            store.toggleLike(t); close()
        }
        ActionRow(Ic.Add, "Tambah ke playlist") { ui.addTo = t; close() }
        ActionRow(Ic.Share, "Bagikan") { shareTrack(ctx, t); close() }
        ui.menuExtra?.let { (label, act) ->
            ActionRow(Ic.Delete, label, Color(0xFFFF8A80)) { act(); close() }
        }
    }
}

@Composable
fun AddToPlaylistDialog(t: Track, ui: UiState) {
    val ctx = LocalContext.current
    val store = OtacixApp.store
    val db by store.db.collectAsState()
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        TextInputDialog("Playlist baru", "", "Buat", { creating = false }) { name ->
            val id = store.addPlaylist(name)
            store.addToPlaylist(id, t)
            toast(ctx, "Ditambahkan ke $name"); ui.addTo = null
        }
        return
    }
    GlassDialog(onDismiss = { ui.addTo = null }) {
        Txt("Tambah ke playlist", 18.sp, FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        ActionRow(Ic.Add, "Playlist baru", Yellow) { creating = true }
        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
            db.playlists.forEach { p ->
                ActionRow(Ic.Library, "${p.name} · ${p.tracks.size}") {
                    store.addToPlaylist(p.id, t); toast(ctx, "Ditambahkan ke ${p.name}"); ui.addTo = null
                }
            }
        }
    }
}

@Composable
fun TextInputDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var v by remember { mutableStateOf(initial) }
    GlassDialog(onDismiss) {
        Txt(title, 18.sp, FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        BasicTextField(
            value = v, onValueChange = { v = it.take(40) }, singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
            cursorBrush = SolidColor(Yellow),
            modifier = Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        )
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            PillButton("Batal", onClick = onDismiss)
            PillButton(confirm, primary = true) { onConfirm(v); onDismiss() }
        }
    }
}

@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    GlassDialog(onDismiss) {
        Txt(title, 18.sp, FontWeight.Bold)
        Txt(body, 14.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            PillButton("Batal", onClick = onDismiss)
            PillButton(confirm, primary = true) { onConfirm(); onDismiss() }
        }
    }
}

@Composable
fun SleepDialog(onDismiss: () -> Unit) {
    val player = OtacixApp.player
    val ctx = LocalContext.current
    GlassDialog(onDismiss) {
        Txt("Timer tidur", 18.sp, FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
        Txt("Volume memudar pelan lalu musik berhenti.", 13.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 8.dp))
        listOf(15, 30, 45, 60).forEach { m ->
            ActionRow(Ic.Moon, "$m menit") { player.setSleep(m); toast(ctx, "Musik berhenti dalam $m menit"); onDismiss() }
        }
        ActionRow(Ic.Moon, "Akhir lagu ini") { player.setSleep(-1); toast(ctx, "Berhenti di akhir lagu"); onDismiss() }
        ActionRow(Ic.Close, "Matikan timer", Color(0xFFFF8A80)) { player.setSleep(0); onDismiss() }
    }
}
