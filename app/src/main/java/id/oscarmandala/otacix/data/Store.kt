package id.oscarmandala.otacix.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.util.Calendar

/** Penyimpanan lokal sederhana (SharedPreferences + JSON). Tanpa login, semua data di perangkat. */
class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("otacix", Context.MODE_PRIVATE)
    private val _db = MutableStateFlow(load())
    val db: StateFlow<Db> = _db

    private fun load(): Db = try {
        sp.getString("db", null)?.let { Db.from(JSONObject(it)) } ?: Db()
    } catch (e: Exception) {
        Db()
    }

    fun update(f: (Db) -> Db) {
        val n = f(_db.value)
        _db.value = n
        sp.edit().putString("db", n.toJson().toString()).apply()
    }

    // ---- riwayat & streak ----
    fun recordPlay(t: Track) = update { d ->
        val now = System.currentTimeMillis()
        val old = d.history.firstOrNull { it.track.id == t.id }
        val rest = d.history.filter { it.track.id != t.id }
        val h = (listOf(Played(t, (old?.n ?: 0) + 1, now)) + rest).take(200)
        d.copy(history = h, plays = d.plays + 1, days = d.days + dayKey(Calendar.getInstance()))
    }

    // ---- suka ----
    fun isLiked(d: Db, id: String) = d.liked.any { it.id == id }
    fun toggleLike(t: Track) = update { d ->
        if (d.liked.any { it.id == t.id }) d.copy(liked = d.liked.filter { it.id != t.id })
        else d.copy(liked = listOf(t) + d.liked)
    }

    // ---- playlist ----
    fun addPlaylist(name: String): String {
        val id = "pl" + System.currentTimeMillis().toString(36)
        update { it.copy(playlists = it.playlists + Playlist(id, name.trim().ifEmpty { "Playlist baru" }, emptyList())) }
        return id
    }
    fun renamePlaylist(id: String, name: String) = update { d ->
        d.copy(playlists = d.playlists.map { if (it.id == id) it.copy(name = name.trim().ifEmpty { it.name }) else it })
    }
    fun deletePlaylist(id: String) = update { d -> d.copy(playlists = d.playlists.filter { it.id != id }) }
    fun addToPlaylist(pid: String, t: Track) = update { d ->
        d.copy(playlists = d.playlists.map {
            if (it.id == pid && it.tracks.none { x -> x.id == t.id }) it.copy(tracks = it.tracks + t) else it
        })
    }
    fun removeFromPlaylist(pid: String, index: Int) = update { d ->
        d.copy(playlists = d.playlists.map {
            if (it.id == pid) it.copy(tracks = it.tracks.filterIndexed { i, _ -> i != index }) else it
        })
    }

    // ---- lain-lain ----
    fun pushRecent(q: String) = update { d -> d.copy(recent = (listOf(q) + d.recent.filter { it != q }).take(8)) }
    fun setName(n: String) = update { it.copy(name = n.trim()) }
    fun setPerf(p: Int) = update { it.copy(perf = p) }

    fun exportJson(): String = _db.value.toJson().toString(2)
    fun importJson(s: String): Boolean = try {
        val d = Db.from(JSONObject(s))
        update { d }
        true
    } catch (e: Exception) {
        false
    }

    companion object {
        fun dayKey(c: Calendar) = String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))

        fun streak(days: Set<String>): Int {
            val c = Calendar.getInstance()
            if (dayKey(c) !in days) c.add(Calendar.DAY_OF_MONTH, -1)
            var n = 0
            while (dayKey(c) in days) { n++; c.add(Calendar.DAY_OF_MONTH, -1) }
            return n
        }
    }
}
