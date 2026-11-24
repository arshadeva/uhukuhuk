package id.oscarmandala.otacix.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.oscarmandala.otacix.data.LyricsData
import id.oscarmandala.otacix.data.Lyrics
import id.oscarmandala.otacix.data.Track
import kotlinx.coroutines.CancellationException

private sealed interface LyricsUi {
    object Loading : LyricsUi
    object None : LyricsUi
    object Failed : LyricsUi
    class Found(val data: LyricsData) : LyricsUi
}

/** Panel lirik di pemutar penuh. Lirik bertanda waktu disorot mengikuti lagu; ketuk baris untuk loncat ke bagian itu. */
@Composable
fun LyricsPanel(t: Track, posMs: Long, onSeek: (Long) -> Unit) {
    var state by remember(t.id) { mutableStateOf<LyricsUi>(LyricsUi.Loading) }
    var retry by remember(t.id) { mutableIntStateOf(0) }
    LaunchedEffect(t.id, retry) {
        state = LyricsUi.Loading
        state = try {
            val d = Lyrics.load(t, force = retry > 0)
            if (d == null) LyricsUi.None else LyricsUi.Found(d)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LyricsUi.Failed
        }
    }

    Box(
        Modifier.padding(top = 16.dp).fillMaxWidth().height(340.dp).glass(RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when (val s = state) {
            is LyricsUi.Loading -> Txt("Memuat lirik…", 14.sp, color = Color.White.copy(alpha = 0.7f))
            is LyricsUi.None -> Txt("Lirik lagu ini belum ditemukan.\nKetuk untuk coba lagi.", 14.sp, color = Color.White.copy(alpha = 0.7f), align = TextAlign.Center, modifier = Modifier.tap { retry++ }.padding(24.dp))
            is LyricsUi.Failed -> Txt("Gagal memuat lirik. Periksa koneksi internetmu.\nKetuk untuk coba lagi.", 14.sp, color = Color.White.copy(alpha = 0.7f), align = TextAlign.Center, modifier = Modifier.tap { retry++ }.padding(24.dp))
            is LyricsUi.Found -> LyricsList(s.data, posMs, onSeek)
        }
    }
}

@Composable
private fun LyricsList(d: LyricsData, posMs: Long, onSeek: (Long) -> Unit) {
    val listState = rememberLazyListState()
    val cur = if (d.synced) d.lines.indexOfLast { it.timeMs <= posMs + 250L } else -1

    LaunchedEffect(cur) {
        if (cur >= 0) listState.animateScrollToItem((cur - 2).coerceAtLeast(0))
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(d.lines) { i, l ->
            val target = when {
                !d.synced -> Color.White.copy(alpha = 0.92f)
                i == cur -> Yellow
                i < cur -> Color.White.copy(alpha = 0.42f)
                else -> Color.White.copy(alpha = 0.72f)
            }
            val col by animateColorAsState(target, tween(250), label = "lyric")
            Txt(
                l.text, 21.sp, FontWeight.Bold, col,
                modifier = if (d.synced) Modifier.fillMaxWidth().tap { onSeek(l.timeMs) } else Modifier.fillMaxWidth(),
            )
        }
    }
}
