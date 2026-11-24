package id.oscarmandala.otacix.data

import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Satu baris lirik. timeMs = -1 bila lirik tidak bertanda waktu. */
class LyricLine(val timeMs: Long, val text: String)

class LyricsData(val lines: List<LyricLine>, val synced: Boolean)

/**
 * Pengambil lirik multi-sumber (pola dari Metrolist): LRCLIB, SimpMusic (berdasarkan id video YouTube) dan KuGou
 * dicoba BERSAMAAN. Lirik bertanda waktu diutamakan; satu sumber yang gagal/diblokir tidak lagi membuat lirik error.
 * Melempar IOException hanya bila semua sumber gagal karena jaringan; mengembalikan null bila lirik memang tidak ada.
 */
object Lyrics {
    private const val UA = "Otacix/1.0 (by Oscar Mandala)"

    private val client = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS).readTimeout(9, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS).build()

    private val cache = ConcurrentHashMap<String, LyricsData>()
    private val misses = ConcurrentHashMap.newKeySet<String>()

    /** force = true mengabaikan catatan "tidak ditemukan" (dipakai tombol coba lagi). */
    suspend fun load(t: Track, force: Boolean = false): LyricsData? {
        cache[t.id]?.let { return it }
        if (force) misses.remove(t.id)
        if (misses.contains(t.id)) return null
        val r = find(t)
        if (r != null) cache[t.id] = r else misses.add(t.id)
        return r
    }

    // ---------------------------------------------------------------- jaringan
    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    /** GET tanpa memblokir thread; null bila HTTP bukan 2xx (mis. 404 = tidak ada). IOException bila jaringan gagal. */
    private suspend fun get(url: String): String? = suspendCancellableCoroutine { cont ->
        val call = client.newCall(Request.Builder().url(url).header("User-Agent", UA).header("Accept", "application/json").build())
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.use { r -> if (r.isSuccessful) r.body?.string() else null }
                    if (cont.isActive) cont.resume(body)
                } catch (e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }
            }
        })
    }

    // ---------------------------------------------------------------- pembersihan judul
    private val noise = Regex(
        """[\(\[\{][^\)\]\}]*(official|lyric|lirik|video|audio|mv|m/v|visualizer|clip|hd|hq|4k|remaster|explicit|live|cover)[^\)\]\}]*[\)\]\}]""",
        RegexOption.IGNORE_CASE,
    )
    private val feat = Regex("""[\(\[]?\s*\b(feat|ft|featuring)\b\.?[^\)\]]*[\)\]]?""", RegexOption.IGNORE_CASE)
    private val spaces = Regex("""\s+""")
    private val topic = Regex("""\s*-\s*Topic\z""", RegexOption.IGNORE_CASE)
    private val vevo = Regex("""\s*(VEVO|Official)\z""", RegexOption.IGNORE_CASE)

    private fun cleanTitle(s: String): String {
        var x = s.substringBefore("|")
        x = x.replace(noise, " ").replace(spaces, " ").trim().trim('-', '|', ' ')
        return x
    }

    private fun stripFeat(s: String): String = s.replace(feat, " ").replace(spaces, " ").trim().trim('-', ' ')

    private fun cleanArtist(s: String): String =
        s.replace(topic, "").replace(vevo, "").trim()

    /** Pasangan (artis, judul) yang dicoba berurutan. Judul YouTube sering berbentuk "Artis - Lagu". */
    private fun candidates(t: Track): List<Pair<String, String>> {
        val art = cleanArtist(t.artist)
        val title = cleanTitle(t.title)
        val out = LinkedHashSet<Pair<String, String>>()
        val i = title.indexOf(" - ")
        if (i > 0) {
            val a = title.substring(0, i).trim()
            val b = title.substring(i + 3).trim()
            if (a.isNotEmpty() && b.isNotEmpty()) {
                out.add(a to stripFeat(b))
                if (art.isNotEmpty() && !a.equals(art, ignoreCase = true)) out.add(art to stripFeat(b))
            }
        }
        if (title.isNotEmpty()) {
            out.add(art to title)
            val nf = stripFeat(title)
            if (nf.isNotEmpty() && nf != title) out.add(art to nf)
        }
        return out.filter { it.second.isNotBlank() }
    }

    // ---------------------------------------------------------------- parser LRC
    private fun str(o: JSONObject, key: String): String = if (o.isNull(key)) "" else o.optString(key, "")

    private val stamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")
    private val credit = Regex(
        """^(作词|作曲|词|曲|编曲|制作人|词曲|填词|混音|录音|监制|Lyrics|Composer|Composed by|Written by|Arranger|Producer)\s*[:：]""",
        RegexOption.IGNORE_CASE,
    )

    private fun parseSynced(s: String): List<LyricLine> {
        val out = ArrayList<LyricLine>()
        for (raw in s.lines()) {
            val ms = stamp.findAll(raw).toList()
            if (ms.isEmpty()) continue
            val text = raw.substring(ms.last().range.last + 1).trim()
            if (text.isEmpty() || credit.containsMatchIn(text) || text.equals("Synced by Noob.exe", ignoreCase = true)) continue
            for (m in ms) {
                val mm = m.groupValues[1].toLong()
                val ss = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val part = if (frac.isEmpty()) 0L else frac.padEnd(3, '0').take(3).toLong()
                out.add(LyricLine(mm * 60_000L + ss * 1_000L + part, text))
            }
        }
        return out.sortedBy { it.timeMs }
    }

    private fun syncedData(s: String): LyricsData? = parseSynced(s).takeIf { it.isNotEmpty() }?.let { LyricsData(it, true) }

    private fun plainData(s: String): LyricsData? {
        val l = s.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { LyricLine(-1L, it) }
        return if (l.isNotEmpty()) LyricsData(l, false) else null
    }

    private fun fromLrclib(o: JSONObject): LyricsData? {
        if (o.optBoolean("instrumental", false)) return null
        val synced = str(o, "syncedLyrics")
        if (synced.isNotBlank()) syncedData(synced)?.let { return it }
        val plain = str(o, "plainLyrics")
        if (plain.isNotBlank()) return plainData(plain)
        return null
    }

    // ---------------------------------------------------------------- sumber 1: LRCLIB
    private suspend fun lrclib(t: Track): LyricsData? {
        val cands = candidates(t)
        if (cands.isEmpty()) return null
        var plainFallback: LyricsData? = null

        // 1) cocok persis (artis + judul + durasi)
        for ((a, b) in cands) {
            val url = "https://lrclib.net/api/get?artist_name=${enc(a)}&track_name=${enc(b)}" +
                if (t.dur > 0) "&duration=${t.dur}" else ""
            val body = get(url) ?: continue
            val d = runCatching { fromLrclib(JSONObject(body)) }.getOrNull() ?: continue
            if (d.synced) return d
            if (plainFallback == null) plainFallback = d
        }

        // 2) cari bebas (q=) dan terstruktur (track_name/artist_name), pilih yang durasinya paling mendekati
        for ((a, b) in cands) {
            val urls = listOf(
                "https://lrclib.net/api/search?q=${enc(if (a.isNotEmpty()) "$a $b" else b)}",
                "https://lrclib.net/api/search?track_name=${enc(b)}" + if (a.isNotEmpty()) "&artist_name=${enc(a)}" else "",
            )
            for (url in urls) {
                val body = get(url) ?: continue
                val arr = runCatching { JSONArray(body) }.getOrNull() ?: continue
                var best: LyricsData? = null
                var bestScore = Int.MAX_VALUE
                for (i in 0 until minOf(arr.length(), 15)) {
                    val o = arr.optJSONObject(i) ?: continue
                    val d = fromLrclib(o) ?: continue
                    val diff = if (t.dur > 0 && !o.isNull("duration")) Math.abs(o.optDouble("duration", 0.0) - t.dur).toInt() else 0
                    val score = diff * 2 + (if (d.synced) 0 else 30)
                    if (score < bestScore) { bestScore = score; best = d }
                }
                if (best != null && bestScore <= 40) {
                    if (best.synced) return best
                    if (plainFallback == null) plainFallback = best
                }
            }
        }
        return plainFallback
    }

    // ---------------------------------------------------------------- sumber 2: SimpMusic (id video YouTube)
    private suspend fun simpMusic(t: Track): LyricsData? {
        if (isItunesId(t.id)) return null
        val body = get("https://api-lyrics.simpmusic.org/v1/${enc(t.id)}") ?: return null
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (root.optString("type") != "success") return null
        val arr = root.optJSONArray("data") ?: return null
        var best: JSONObject? = null
        var bestDiff = Int.MAX_VALUE
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (str(o, "syncedLyrics").isBlank()) continue
            val diff = if (t.dur > 0) Math.abs(o.optInt("durationSeconds", 0) - t.dur) else 0
            if (diff < bestDiff) { bestDiff = diff; best = o }
        }
        val o = best ?: return null
        if (t.dur > 0 && bestDiff > 20) return null
        return syncedData(str(o, "syncedLyrics"))
    }

    // ---------------------------------------------------------------- sumber 3: KuGou
    private val kgBrackets = Regex("""[\(（\[<《「『〈＜].*?[\)）\]>》」』〉＞]""")

    private suspend fun kugou(t: Track): LyricsData? {
        for ((a, b) in candidates(t).take(2)) {
            val title = b.replace(kgBrackets, "").trim().ifEmpty { b }
            val keyword = if (a.isNotEmpty()) "$title - ${a.replace(kgBrackets, "").trim()}" else title
            val s = get(
                "https://lyrics.kugou.com/search?ver=1&man=yes&client=pc&keyword=${enc(keyword)}" +
                    if (t.dur > 0) "&duration=${t.dur * 1000L}" else ""
            ) ?: continue
            val cands = runCatching { JSONObject(s).optJSONArray("candidates") }.getOrNull() ?: continue
            var id = 0L
            var key = ""
            var bestDiff = Double.MAX_VALUE
            for (i in 0 until cands.length()) {
                val o = cands.optJSONObject(i) ?: continue
                val k = o.optString("accesskey")
                val cid = o.optLong("id")
                if (k.isEmpty() || cid == 0L) continue
                val raw = o.optLong("duration", 0L)
                val diff = if (t.dur > 0 && raw > 0) minOf(Math.abs(raw - t.dur).toDouble(), Math.abs(raw - t.dur * 1000L) / 1000.0) else 0.0
                if (diff < bestDiff) { bestDiff = diff; id = cid; key = k }
            }
            if (id == 0L || bestDiff > 8.0) continue
            val dl = get("https://lyrics.kugou.com/download?fmt=lrc&charset=utf8&client=pc&ver=1&id=$id&accesskey=${enc(key)}") ?: continue
            val content = runCatching { JSONObject(dl).optString("content") }.getOrNull().orEmpty()
            if (content.isEmpty()) continue
            val lrc = runCatching { String(Base64.decode(content, Base64.DEFAULT), Charsets.UTF_8) }.getOrNull() ?: continue
            syncedData(lrc)?.let { return it }
        }
        return null
    }

    // ---------------------------------------------------------------- gabungan
    private suspend fun find(t: Track): LyricsData? = coroutineScope {
        val jobs = listOf(
            async { runCatching { lrclib(t) } },
            async { runCatching { simpMusic(t) } },
            async { runCatching { kugou(t) } },
        )
        var plain: LyricsData? = null
        var ioFail = 0
        for (j in jobs) {
            val r = j.await()
            val e = r.exceptionOrNull()
            if (e is CancellationException) throw e
            if (e != null) { if (e is IOException) ioFail++; continue }
            val d = r.getOrNull() ?: continue
            if (d.synced) {
                jobs.forEach { it.cancel() }
                return@coroutineScope d
            }
            if (plain == null) plain = d
        }
        if (plain == null && ioFail == jobs.size) throw IOException("Semua sumber lirik gagal dijangkau")
        plain
    }
}
