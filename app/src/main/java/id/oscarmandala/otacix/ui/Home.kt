package id.oscarmandala.otacix.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oscarmandala.otacix.OtacixApp
import id.oscarmandala.otacix.R
import id.oscarmandala.otacix.data.MOODS
import id.oscarmandala.otacix.data.Store
import java.util.Calendar

private fun greeting(name: String): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val g = when {
        h < 4 -> "Selamat malam"
        h < 11 -> "Selamat pagi"
        h < 15 -> "Selamat siang"
        h < 19 -> "Selamat sore"
        else -> "Selamat malam"
    }
    return if (name.isNotEmpty()) "$g, $name" else g
}

@Composable
fun HomeScreen(ui: UiState) {
    val store = OtacixApp.store
    val player = OtacixApp.player
    val db by store.db.collectAsStateWithLifecycle()
    val shelves by OtacixApp.repo.shelves.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { OtacixApp.repo.loadHome() }

    val trend = shelves.firstOrNull()?.items.orEmpty()
    val hist = db.history.map { it.track }
    val quick = (if (hist.size >= 2) hist else trend).take(6)
    val top = if (db.history.isNotEmpty()) db.history.sortedByDescending { it.n }.take(6).map { it.track } else trend.drop(6).take(6)

    LazyColumn(
        state = ui.lists[0], modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 220.dp),
    ) {
        item {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Image(painterResource(R.drawable.logo), "Otacix", Modifier.height(26.dp).aspectRatio(2.08f), contentScale = ContentScale.Fit)
                }
                Column {
                    Txt("Otacix", 20.sp, FontWeight.ExtraBold)
                    Txt("by Oscar Mandala", 11.sp, FontWeight.SemiBold, Yellow)
                }
            }
        }
        item { Txt(greeting(db.name), 30.sp, FontWeight.ExtraBold, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp)) }

        // Pilihan cepat
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (quick.isEmpty()) {
                    repeat(3) { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Skeleton(Modifier.weight(1f).height(60.dp), 18.dp); Skeleton(Modifier.weight(1f).height(60.dp), 18.dp) } }
                } else quick.chunked(2).forEachIndexed { r, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEachIndexed { c, t -> QuickTile(t, Modifier.weight(1f)) { player.play(quick, r * 2 + c) } }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item { StreakCard(db.days) }

        // Mood
        item { SectionTitle("Suasana hatimu") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(MOODS) { (key, label, q) -> Chip(label, icon = moodIcon(key)) { ui.pendingSearch = q; ui.tab = 1 } }
            }
        }

        if (top.isNotEmpty()) {
            item { SectionTitle(if (db.history.isNotEmpty()) "Andalanmu" else "Untukmu") }
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    top.chunked(3).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEachIndexed { c, t -> SquareCard(t, Modifier.weight(1f)) { player.play(top, r * 3 + c) } }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // Rak-rak
        items(shelves) { shelf ->
            Column {
                SectionTitle(shelf.title)
                val list = shelf.items
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        list == null -> items(4) { Column { Skeleton(Modifier.size(140.dp)); Spacer(Modifier.height(8.dp)); Skeleton(Modifier.width(100.dp).height(12.dp), 6.dp) } }
                        list.isEmpty() -> item { Txt("Tidak bisa memuat. Periksa koneksi internetmu.", 13.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 4.dp)) }
                        else -> itemsIndexed(list.take(12)) { i, t -> SquareCard(t, Modifier.width(140.dp)) { player.play(list, i) } }
                    }
                }
            }
        }

        // Poster Studio
        item {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 26.dp).fillMaxWidth().tap { ui.poster = true }
                    .glass(RoundedCornerShape(28.dp), tint = Yellow, strength = 1.2f).padding(20.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Txt("Poster Studio", 18.sp, FontWeight.Bold)
                    Txt("Ubah playlistmu jadi poster liquid glass, simpan atau bagikan.", 13.sp, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(top = 4.dp))
                }
                Icon(Ic.Image, null, Modifier.size(34.dp), tint = YellowSoft)
            }
        }
        item {
            Txt("Otacix · dibuat oleh Oscar Mandala", 12.sp, color = Color.White.copy(alpha = 0.4f), align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StreakCard(days: Set<String>) {
    val n = Store.streak(days)
    val todayDone = Store.dayKey(Calendar.getInstance()) in days
    val msg = when {
        n == 0 -> "Putar satu lagu hari ini untuk memulai streak!"
        todayDone -> "Mantap, streak hari ini aman."
        else -> "Putar lagu hari ini supaya streak tidak putus."
    }
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 18.dp).fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(if (n > 0) Ic.Flame else Ic.Sparkle, null, Modifier.size(44.dp), tint = if (n > 0) Color(0xFFFF9D3D) else YellowSoft)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Txt("$n", 28.sp, FontWeight.ExtraBold)
                Txt("  hari berturut-turut", 14.sp, FontWeight.SemiBold, Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 4.dp))
            }
            Txt(msg, 12.5.sp, color = Color.White.copy(alpha = 0.65f))
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 6 downTo 0) {
                    val c = Calendar.getInstance().also { it.add(Calendar.DAY_OF_MONTH, -i) }
                    val on = Store.dayKey(c) in days
                    val letter = "MSSRKJS"[c.get(Calendar.DAY_OF_WEEK) - 1].toString()
                    Box(
                        Modifier.size(24.dp).clip(CircleShape)
                            .background(if (on) Brush.linearGradient(listOf(Yellow, Color(0xFFFF9D3D))) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.14f)))),
                        contentAlignment = Alignment.Center,
                    ) { Txt(letter, 10.sp, FontWeight.Bold, if (on) Navy else Color.White.copy(alpha = 0.5f)) }
                }
            }
        }
    }
}
