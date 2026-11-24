package id.oscarmandala.otacix.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.oscarmandala.otacix.OtacixApp
import id.oscarmandala.otacix.data.SearchResult
import kotlinx.coroutines.launch

private val DEFAULT_SUGGEST = listOf("Tulus", "Hindia", "Sheila on 7", "Taylor Swift", "lofi hip hop", "Dewa 19")

@Composable
fun SearchScreen(ui: UiState) {
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val db by OtacixApp.store.db.collectAsStateWithLifecycle()
    val player = OtacixApp.player

    fun doSearch(text: String) {
        val s = text.trim()
        if (s.isEmpty()) return
        ui.sq = s; ui.sload = true; ui.sres = null
        focus.clearFocus()
        OtacixApp.store.pushRecent(s)
        scope.launch {
            val r = try { OtacixApp.repo.search(s) } catch (e: Exception) { SearchResult(emptyList(), "youtube") }
            if (ui.sq == s) { ui.sres = r; ui.sload = false } // abaikan hasil pencarian lama
        }
    }

    LaunchedEffect(ui.pendingSearch) {
        ui.pendingSearch?.let { ui.pendingSearch = null; doSearch(it) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle("Cari")
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Ic.Search, null, Modifier.size(22.dp), tint = Color.White.copy(alpha = 0.7f))
            BasicTextField(
                value = ui.sq, onValueChange = { ui.sq = it }, singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                cursorBrush = SolidColor(Yellow),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { doSearch(ui.sq) }),
                modifier = Modifier.weight(1f).padding(vertical = 16.dp),
                decorationBox = { inner ->
                    if (ui.sq.isEmpty()) Txt("Lagu, artis, album…", 17.sp, color = Color.White.copy(alpha = 0.45f))
                    inner()
                },
            )
            if (ui.sq.isNotEmpty()) IconBtn(Ic.Close, 32.dp, 18.dp, Color.White.copy(alpha = 0.7f)) { ui.sq = ""; ui.sres = null; ui.sload = false }
        }

        val chips = db.recent.ifEmpty { DEFAULT_SUGGEST }
        Txt(if (db.recent.isEmpty()) "Coba cari" else "Pencarian terakhir", 13.sp, FontWeight.SemiBold, Color.White.copy(alpha = 0.55f),
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chips) { c -> Chip(c) { doSearch(c) } }
        }

        LazyColumn(
            state = ui.lists[1], modifier = Modifier.fillMaxSize().padding(top = 12.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (ui.sload) {
                items(6) { Skeleton(Modifier.fillMaxWidth().height(70.dp), 22.dp) }
            } else {
                val r = ui.sres
                if (r != null) {
                    if (r.src == "meta") item {
                        Txt("Pencarian YouTube sedang terbatas, lagu dicari ulang di YouTube saat diputar.", 12.5.sp,
                            color = YellowSoft, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    if (r.items.isEmpty()) item {
                        Txt("Tidak ada hasil. Coba kata kunci lain.", 14.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 20.dp))
                    }
                    itemsIndexed(r.items) { i, t ->
                        TrackRow(t, onMore = { ui.openMenu(t) }) { player.play(r.items, i) }
                    }
                }
            }
        }
    }
}
