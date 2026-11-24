package id.oscarmandala.otacix.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oscarmandala.otacix.OtacixApp
import id.oscarmandala.otacix.data.Store
import id.oscarmandala.otacix.player.AudioCache
import id.oscarmandala.otacix.util.Device
import id.oscarmandala.otacix.util.Tier

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier.glass(RoundedCornerShape(22.dp)).padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Txt(value, 24.sp, FontWeight.ExtraBold, Yellow)
        Txt(label, 12.sp, color = Color.White.copy(alpha = 0.65f), modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun ProfileScreen(ui: UiState) {
    val ctx = LocalContext.current
    val store = OtacixApp.store
    val db by store.db.collectAsStateWithLifecycle()
    val tier = LocalTier.current
    var editName by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var support by remember { mutableStateOf(false) }
    var cacheMb by remember { mutableStateOf(AudioCache.sizeBytes(ctx) / 1_048_576L) }

    val exportL = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            val ok = runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(store.exportJson().toByteArray()) } != null
            }.getOrDefault(false)
            toast(ctx, if (ok) "Cadangan tersimpan" else "Gagal menyimpan cadangan")
        }
    }
    val importL = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = runCatching { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            toast(ctx, if (text != null && store.importJson(text)) "Data berhasil dipulihkan" else "File cadangan tidak valid")
        }
    }

    val topArtists = db.history.groupBy { it.track.artist }.mapValues { (_, v) -> v.sumOf { it.n } }
        .entries.sortedByDescending { it.value }.take(8).map { it.key }.filter { it.isNotBlank() }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 220.dp)) {
        item { ScreenTitle("Kamu") }
        item {
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().tap { editName = true }.glass(RoundedCornerShape(26.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(60.dp).clip(CircleShape).background(Brush.linearGradient(listOf(BlueBright, Yellow))),
                    contentAlignment = Alignment.Center,
                ) { Txt((db.name.firstOrNull() ?: 'O').uppercase(), 26.sp, FontWeight.ExtraBold, Navy) }
                Column(Modifier.weight(1f)) {
                    Txt(db.name.ifEmpty { "Pendengar Otacix" }, 18.sp, FontWeight.Bold, maxLines = 1)
                    Txt("Ketuk untuk mengganti nama", 12.sp, color = Color.White.copy(alpha = 0.6f))
                }
                Icon2()
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${db.plays}", "Diputar", Modifier.weight(1f))
                Stat("${Store.streak(db.days)}", "Streak", Modifier.weight(1f))
                Stat("${db.playlists.size}", "Playlist", Modifier.weight(1f))
            }
        }
        if (topArtists.isNotEmpty()) {
            item { SectionTitle("Artis favorit") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(topArtists) { a -> Chip(a) { ui.pendingSearch = a; ui.tab = 1 } }
                }
            }
        }

        item { SectionTitle("Performa") }
        item {
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(16.dp)) {
                Txt(if (tier == Tier.FULL) "Mode aktif: Penuh (liquid glass lengkap)" else "Mode aktif: Hemat (ringan untuk HP spek rendah)", 14.sp, FontWeight.SemiBold)
                Txt(
                    if (tier == Tier.FULL) "Aurora bergerak, kilau kaca mengikuti kemiringan HP, warna latar mengikuti sampul lagu, cache audio 512 MB."
                    else "Animasi berat, kilau sensor, dan warna sampul dimatikan. Cache audio 128 MB. Pemutaran tetap penuh.",
                    12.5.sp, color = Color.White.copy(alpha = 0.65f), modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Otomatis", db.perf == 0) { store.setPerf(0) }
                    Chip("Hemat", db.perf == 1) { store.setPerf(1) }
                    Chip("Penuh", db.perf == 2) { store.setPerf(2) }
                }
                if (db.perf == 0) Txt(
                    "Otomatis: ${if (Device.isHighEnd(ctx)) "HP-mu terdeteksi spek atas" else "HP-mu terdeteksi spek standar/rendah"}.",
                    12.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        item { SectionTitle("Data") }
        item {
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Txt("Cache audio: $cacheMb MB (lagu yang pernah diputar bisa diputar ulang tanpa internet)", 13.sp, color = Color.White.copy(alpha = 0.75f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton("Cadangkan", Ic.Download) { exportL.launch("otacix-cadangan.json") }
                    PillButton("Pulihkan", Ic.Add) { importL.launch(arrayOf("application/json", "*/*")) }
                }
                PillButton("Bersihkan cache audio", Ic.Delete, modifier = Modifier.fillMaxWidth()) { confirmClear = true }
            }
        }

        item { SectionTitle("Dukung developer") }
        item {
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().tap { support = true }
                    .glass(RoundedCornerShape(26.dp), tint = Yellow, strength = 1.2f).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Txt("Sawer lewat QRIS", 15.sp, FontWeight.Bold)
                    Txt("Bantu Oscar Mandala terus mengembangkan Otacix.", 12.5.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 3.dp))
                }
                Icon(Ic.Heart, null, Modifier.size(28.dp), tint = Yellow)
            }
        }

        item { SectionTitle("Agar musik tetap jalan") }
        item {
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Txt("Otacix memutar di latar belakang lewat notifikasi pemutar. Di beberapa HP (Xiaomi, Oppo, Vivo, Samsung) hemat baterai bisa mematikannya; atur Otacix ke \"Tanpa batasan\".", 12.5.sp, color = Color.White.copy(alpha = 0.7f))
                PillButton("Buka pengaturan aplikasi") {
                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Txt("Otacix 1.0", 14.sp, FontWeight.Bold, Yellow)
                Txt("by Oscar Mandala", 12.sp, color = Color.White.copy(alpha = 0.55f))
            }
        }
    }

    if (support) SupportDialog { support = false }
    if (editName) TextInputDialog("Namamu", db.name, "Simpan", { editName = false }) { store.setName(it) }
    if (confirmClear) ConfirmDialog("Bersihkan cache audio?", "Lagu akan diunduh ulang saat diputar lagi.", "Bersihkan", { confirmClear = false }) {
        AudioCache.clear(ctx); cacheMb = AudioCache.sizeBytes(ctx) / 1_048_576L; toast(ctx, "Cache dibersihkan")
    }
}

@Composable
private fun Icon2() = androidx.compose.material3.Icon(Ic.Edit, null, Modifier.size(20.dp), tint = Color.White.copy(alpha = 0.6f))
