package id.oscarmandala.otacix.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NpRequest
import org.schabi.newpipe.extractor.downloader.Response as NpResponse
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Port Kotlin dari server Ocalix (index.js): cari lagu lewat YouTube InnerTube,
 * ambil URL audio lewat beberapa client (VISIONOS -> ANDROID), cadangan pencarian YouTube Music + Piped, dan iTunes (hanya metadata; audio penuh dicari di YouTube).
 * Catatan: ini API tidak resmi YouTube, bisa berubah/berhenti sewaktu-waktu.
 */
object Yt {
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
    private const val KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private val JSON_MT = "application/json".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS).build()
    private val quick = client.newBuilder().callTimeout(6, TimeUnit.SECONDS).build()

    /** Alamat API Piped milikmu sendiri (opsional), mis. listOf("https://pipedapi.domainmu.com") */
    @Volatile var customPiped: List<String> = emptyList()

    private class Resp(val code: Int, val body: String)

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { runCatching { cancel() } }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { if (cont.isActive) cont.resumeWithException(e) }
            override fun onResponse(call: Call, response: Response) { cont.resume(response) }
        })
    }

    private suspend fun http(c: OkHttpClient, url: String, headers: Map<String, String> = emptyMap(), body: String? = null): Resp {
        val rb = Request.Builder().url(url).header("User-Agent", UA)
        headers.forEach { (k, v) -> rb.header(k, v) }
        if (body != null) rb.post(body.toRequestBody(JSON_MT))
        c.newCall(rb.build()).await().use { r ->
            val text = withContext(Dispatchers.IO) { r.body?.string().orEmpty() }
            return Resp(r.code, text)
        }
    }

    // ------------------------------------------------------------------ PENCARIAN
    private val searchCache = ConcurrentHashMap<String, Pair<Long, SearchResult>>()

    suspend fun search(q: String): SearchResult {
        val key = q.trim().lowercase()
        searchCache[key]?.let { if (System.currentTimeMillis() - it.first < 600_000) return it.second }
        var items = emptyList<Track>()
        var src = "youtube"
        for (step in 0..2) {
            try {
                items = when (step) { 0 -> youtube(q); 1 -> ytm(q); else -> pipedSearch(q) }
            } catch (e: CancellationException) { throw e } catch (e: Exception) { }
            if (items.isNotEmpty()) break
        }
        if (items.isEmpty()) {
            src = "meta"   // YouTube tak terjangkau: hanya metadata iTunes, audio penuh dicari saat diputar
            try { items = itunes(q) } catch (e: CancellationException) { throw e } catch (e: Exception) { }
        }
        val res = SearchResult(items, src)
        if (items.isNotEmpty()) searchCache[key] = System.currentTimeMillis() to res
        return res
    }

    private suspend fun youtube(q: String): List<Track> {
        val ctx = JSONObject().put("client", JSONObject()
            .put("clientName", "WEB").put("clientVersion", "2.20240726.00.00").put("hl", "id").put("gl", "ID"))
        val body = JSONObject().put("context", ctx).put("query", q).put("params", "EgIQAQ==").toString()
        val r = http(client, "https://www.youtube.com/youtubei/v1/search?prettyPrint=false&key=$KEY",
            mapOf("Origin" to "https://www.youtube.com"), body)
        val secs = JSONObject(r.body).getJSONObject("contents").getJSONObject("twoColumnSearchResultsRenderer")
            .getJSONObject("primaryContents").getJSONObject("sectionListRenderer").getJSONArray("contents")
        val out = ArrayList<Track>()
        for (a in 0 until secs.length()) {
            val items = secs.getJSONObject(a).optJSONObject("itemSectionRenderer")?.optJSONArray("contents") ?: continue
            for (b in 0 until items.length()) {
                val v = items.getJSONObject(b).optJSONObject("videoRenderer") ?: continue
                val lt = v.optJSONObject("lengthText")?.optString("simpleText").orEmpty()
                if (lt.isEmpty()) continue
                val d = lt.split(":").fold(0) { acc, s -> acc * 60 + (s.toIntOrNull() ?: 0) }
                if (d <= 0 || d > 1500) continue
                val id = v.getString("videoId")
                val runs = v.getJSONObject("title").getJSONArray("runs")
                val title = (0 until runs.length()).joinToString("") { runs.getJSONObject(it).optString("text") }
                val artist = v.optJSONObject("ownerText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    .orEmpty().removeSuffix(" - Topic")
                out += Track(id, title, artist, d, "https://i.ytimg.com/vi/$id/hqdefault.jpg")
            }
        }
        return out
    }

    private suspend fun itunes(q: String): List<Track> {
        val r = http(client, "https://itunes.apple.com/search?entity=song&limit=25&term=" + URLEncoder.encode(q, "UTF-8"))
        val arr = JSONObject(r.body).getJSONArray("results")
        val out = ArrayList<Track>()
        for (i in 0 until arr.length()) {
            val x = arr.getJSONObject(i)
            val d = (x.optLong("trackTimeMillis") / 1000).toInt().takeIf { it > 0 } ?: 200
            out += Track("it" + x.optLong("trackId"), x.optString("trackName"), x.optString("artistName"), d,
                x.optString("artworkUrl100").replace("100x100", "600x600"), null)
        }
        return out
    }

    /** Cadangan: YouTube Music (WEB_REMIX, klien utama Metrolist). */
    private suspend fun ytm(q: String): List<Track> {
        val ctx = JSONObject().put("client", JSONObject()
            .put("clientName", "WEB_REMIX").put("clientVersion", "1.20260114.03.00").put("hl", "id").put("gl", "ID"))
        val body = JSONObject().put("context", ctx).put("query", q).put("params", "EgWKAQIIAWoKEAkQBRAKEAMQBA==").toString()
        val r = http(client, "https://music.youtube.com/youtubei/v1/search?prettyPrint=false",
            mapOf("Origin" to "https://music.youtube.com", "Referer" to "https://music.youtube.com/"), body)
        val out = ArrayList<Track>()
        val seen = HashSet<String>()
        val timeRe = Regex("^\\d+(:\\d+)+$")
        fun walk(o: Any?) {
            when (o) {
                is JSONObject -> {
                    val m = o.optJSONObject("musicResponsiveListItemRenderer")
                    if (m != null) {
                        val id = m.optJSONObject("playlistItemData")?.optString("videoId").orEmpty()
                        val cols = m.optJSONArray("flexColumns")
                        fun col(i: Int): String {
                            val runs = cols?.optJSONObject(i)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                ?.optJSONObject("text")?.optJSONArray("runs") ?: return ""
                            return (0 until runs.length()).joinToString("") { runs.getJSONObject(it).optString("text") }
                        }
                        val title = col(0)
                        val meta = col(1).split(" • ")
                        val d = meta.lastOrNull()?.trim()?.takeIf { timeRe.matches(it) }
                            ?.split(":")?.fold(0) { a, s -> a * 60 + (s.toIntOrNull() ?: 0) } ?: 0
                        val artist = meta.firstOrNull { !it.trim().matches(Regex("(?i)lagu|song|video|single")) }?.trim().orEmpty()
                        if (id.isNotEmpty() && title.isNotEmpty() && seen.add(id) && d <= 1500)
                            out += Track(id, title, artist, if (d > 0) d else 200, "https://i.ytimg.com/vi/$id/hqdefault.jpg")
                        return
                    }
                    for (k in o.keys()) walk(o.opt(k))
                }
                is JSONArray -> for (i in 0 until o.length()) walk(o.opt(i))
            }
        }
        walk(JSONObject(r.body))
        return out
    }

    /** Cadangan: pencarian lewat instance Piped. */
    private suspend fun pipedSearch(q: String): List<Track> {
        val list = (customPiped + listOfNotNull(goodPiped) + PIPED).distinct().take(6)
        return firstOf(list.map { base ->
            suspend {
                val r = http(quick, "$base/search?filter=music_songs&q=" + URLEncoder.encode(q, "UTF-8"), mapOf("Accept" to "application/json"))
                val arr = if (r.code == 200) JSONObject(r.body).optJSONArray("items") else null
                val out = ArrayList<Track>()
                if (arr != null) for (i in 0 until arr.length()) {
                    val x = arr.getJSONObject(i)
                    val id = Regex("v=([\\w-]{11})").find(x.optString("url"))?.groupValues?.get(1) ?: continue
                    val d = x.optInt("duration")
                    if (d <= 0 || d > 1500) continue
                    out += Track(id, x.optString("title"), x.optString("uploaderName").removeSuffix(" - Topic"), d,
                        x.optString("thumbnail").ifEmpty { "https://i.ytimg.com/vi/$id/hqdefault.jpg" })
                }
                out.takeIf { it.isNotEmpty() }
            }
        }) ?: emptyList()
    }

    // id "it..." (hasil iTunes) -> id YouTube asli, dicari dari "artis judul"
    private val itMap = ConcurrentHashMap<String, String>()

    private suspend fun resolveIt(id: String, q: String?): String? {
        itMap[id]?.let { return it }
        if (q.isNullOrBlank()) return null
        for (step in 0..2) {
            val r = try {
                when (step) { 0 -> ytm(q); 1 -> youtube(q); else -> pipedSearch(q) }
            } catch (e: CancellationException) { throw e } catch (e: Exception) { emptyList() }
            val first = r.firstOrNull()
            if (first != null) { itMap[id] = first.id; return first.id }
        }
        return null
    }

    // ------------------------------------------------------------------ URL AUDIO
    class Stream(val url: String, val ua: String, val t: Long)

    private class VC(val n: Int, val ua: String, val name: String, val version: String, val extra: () -> JSONObject)

    private val clients = listOf(
        VC(101, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15",
            "VISIONOS", "1.02") {
            JSONObject().put("deviceMake", "Apple").put("deviceModel", "RealityDevice17,1")
                .put("osName", "visionOS").put("osVersion", "26.5.23O471")
        },
        VC(3, "com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip", "ANDROID", "20.10.38") {
            JSONObject().put("androidSdkVersion", 30).put("osName", "Android").put("osVersion", "11")
        },
    )

    private val streams = ConcurrentHashMap<String, Stream>()
    private val neg = ConcurrentHashMap<String, Long>()
    @Volatile private var vcSkipUntil = 0L
    @Volatile private var goodPiped: String? = null

    /** Alasan terakhir kenapa audio gagal didapat (untuk ditampilkan saat lagu gagal diputar). */
    @Volatile var lastError: String = ""

    private val npClient = client.newBuilder().callTimeout(35, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    @Volatile private var npReady = false

    private fun npInit() {
        if (npReady) return
        synchronized(this) {
            if (!npReady) {
                NewPipe.init(NpDownloader(npClient), Localization("id", "ID"), ContentCountry("ID"))
                npReady = true
            }
        }
    }

    private fun uaFor(u: String): String = when {
        u.contains("c=ANDROID_VR") -> "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
        u.contains("c=ANDROID") -> "com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip"
        u.contains("c=IOS") -> "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)"
        else -> UA
    }

    /** Jalur utama: NewPipeExtractor (menangani signature, throttling, dan perubahan YouTube). */
    private suspend fun newPipe(id: String): Stream = withContext(Dispatchers.IO) {
        npInit()
        val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$id")
        val best = info.audioStreams
            .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && !it.content.isNullOrEmpty() }
            .sortedWith(compareBy({ if (it.format == MediaFormat.M4A) 0 else 1 }, { -it.averageBitrate }))
            .firstOrNull() ?: throw IOException("tanpa stream audio")
        Stream(best.content, uaFor(best.content), System.currentTimeMillis())
    }

    fun invalidate(id: String) { val real = itMap[id] ?: id; streams.remove(real); neg.remove(real) }

    /** Dipanggil dari thread pemuat ExoPlayer (boleh memblokir). */
    fun resolveBlocking(id: String, fresh: Boolean = false, q: String? = null): Stream? = runBlocking(Dispatchers.IO) { resolve(id, fresh, q) }

    suspend fun resolve(rawId: String, fresh: Boolean = false, q: String? = null): Stream? {
        val id = if (isItunesId(rawId)) (resolveIt(rawId, q) ?: return null) else rawId
        val now = System.currentTimeMillis()
        if (!fresh) {
            streams[id]?.let { if (now - it.t < 7_200_000) return it }
            if ((neg[id] ?: 0L) > now) return null
        }
        var o: Stream? = null
        val why = ArrayList<String>()
        try { o = newPipe(id) } catch (e: CancellationException) { throw e } catch (e: Throwable) {
            why += "NewPipe: " + (e.message?.lineSequence()?.firstOrNull()?.take(110) ?: e.javaClass.simpleName)
        }
        if (o == null && now >= vcSkipUntil) {
            for (v in clients) {
                try {
                    val f = pick(playerCall(v, id))
                    val u = f?.optString("url").orEmpty()
                    if (u.isNotEmpty() && isGoogleVideo(u)) { o = Stream(u, v.ua, now); break }
                } catch (e: CancellationException) { throw e } catch (e: Exception) { }
            }
            if (o == null) vcSkipUntil = now + 300_000
        }
        if (o == null) o = try { piped(id) } catch (e: CancellationException) { throw e } catch (e: Exception) { null }
        if (o != null) { streams[id] = o; lastError = "" } else { neg[id] = now + 60_000; lastError = (why + "cadangan langsung/Piped juga gagal").joinToString(" | ") }
        return o
    }

    private fun isGoogleVideo(u: String) = u.toHttpUrlOrNull()?.host?.endsWith(".googlevideo.com") == true

    private suspend fun playerCall(v: VC, id: String): JSONObject {
        val cl = JSONObject().put("hl", "en").put("gl", "US").put("clientName", v.name).put("clientVersion", v.version)
        val ex = v.extra()
        for (k in ex.keys()) cl.put(k, ex.get(k))
        val body = JSONObject().put("context", JSONObject().put("client", cl)).put("videoId", id)
            .put("contentCheckOk", true).put("racyCheckOk", true).toString()
        val r = http(client, "https://www.youtube.com/youtubei/v1/player?prettyPrint=false&key=$KEY", mapOf(
            "User-Agent" to v.ua, "Origin" to "https://www.youtube.com",
            "X-YouTube-Client-Name" to v.n.toString(), "X-YouTube-Client-Version" to v.version), body)
        return JSONObject(r.body)
    }

    private fun pick(j: JSONObject): JSONObject? {
        val fs = j.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats") ?: return null
        return (0 until fs.length()).map { fs.getJSONObject(it) }
            .filter { it.has("url") && it.optString("mimeType").startsWith("audio/") }
            .minByOrNull { if (it.optInt("itag") == 140) 0 else if (it.optString("mimeType").contains("mp4")) 1 else 2 }
    }

    // ---- cadangan: instance Piped ----
    private val PIPED = listOf(
        "https://pipedapi.kavin.rocks", "https://pipedapi.leptons.xyz", "https://pipedapi.nosebs.ru",
        "https://pipedapi-libre.kavin.rocks", "https://piped-api.privacy.com.de", "https://pipedapi.adminforge.de",
        "https://api.piped.yt", "https://pipedapi.drgns.space",
    )

    private suspend fun piped(id: String): Stream? {
        val list = (customPiped + listOfNotNull(goodPiped) + PIPED).distinct().take(8)
        for (chunk in list.chunked(4)) {
            val r = firstOf(chunk.map { base -> suspend { pipedOne(base, id) } })
            if (r != null) return r
        }
        return null
    }

    private suspend fun pipedOne(base: String, id: String): Stream? {
        val r = http(quick, "$base/streams/$id", mapOf("Accept" to "application/json"))
        if (r.code != 200) return null
        val arr = JSONObject(r.body).optJSONArray("audioStreams") ?: return null
        val best = (0 until arr.length()).map { arr.getJSONObject(it) }
            .filter { it.optString("url").startsWith("https://") && !it.optBoolean("videoOnly") && it.optString("mimeType").contains("mp4") }
            .minByOrNull { val b = it.optInt("bitrate"); if (b > 192_000) 1_000_000_000 else -b }
            ?: return null
        goodPiped = base
        return Stream(best.getString("url"), UA, System.currentTimeMillis())
    }

    private suspend fun <T : Any> firstOf(jobs: List<suspend () -> T?>): T? = coroutineScope {
        val ch = Channel<T?>(jobs.size)
        val running = jobs.map { j -> launch { ch.send(try { j() } catch (e: Exception) { null }) } }
        var res: T? = null
        for (i in jobs.indices) {
            val r = ch.receive()
            if (r != null) { res = r; break }
        }
        running.forEach { it.cancel() }
        res
    }
}

private class NpDownloader(private val http: OkHttpClient) : Downloader() {
    override fun execute(request: NpRequest): NpResponse {
        val method = request.httpMethod()
        val url = request.url()
        val data = request.dataToSend()
        val body = data?.toRequestBody() ?: if (method == "POST" || method == "PUT") ByteArray(0).toRequestBody() else null
        val rb = Request.Builder().method(method, body).url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
        request.headers().forEach { (n, values) ->
            rb.removeHeader(n)
            values.forEach { rb.addHeader(n, it) }
        }
        http.newCall(rb.build()).execute().use { r ->
            if (r.code == 429) throw ReCaptchaException("reCaptcha Challenge requested", url)
            val text = r.body?.string()
            return NpResponse(r.code, r.message, r.headers.toMultimap(), text, r.request.url.toString())
        }
    }
}
