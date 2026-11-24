package id.oscarmandala.otacix.data

import org.json.JSONArray
import org.json.JSONObject

/** id hasil iTunes berbentuk "it" + angka; id video YouTube tidak pernah cocok pola ini. */
fun isItunesId(id: String): Boolean = Regex("^it\\d{6,}$").matches(id)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val dur: Int,
    val cover: String,
    val src: String? = null,   // tidak dipakai lagi (dulu: URL pratinjau iTunes)
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("title", title).put("artist", artist)
        .put("dur", dur).put("cover", cover)
        .apply { if (src != null) put("src", src) }

    companion object {
        fun from(o: JSONObject) = Track(
            id = o.getString("id"),
            title = o.optString("title"),
            artist = o.optString("artist"),
            dur = o.optInt("dur"),
            cover = o.optString("cover"),
            src = o.optString("src").takeIf { it.isNotEmpty() && !isItunesId(o.getString("id")) },
        )
    }
}

data class Playlist(val id: String, val name: String, val tracks: List<Track>)

data class Played(val track: Track, val n: Int, val last: Long)

data class Db(
    val name: String = "",
    val liked: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val history: List<Played> = emptyList(),
    val recent: List<String> = emptyList(),
    val days: Set<String> = emptySet(),
    val plays: Int = 0,
    val perf: Int = 0, // 0 = otomatis, 1 = hemat (lite), 2 = penuh (full)
) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("liked", JSONArray(liked.map { it.toJson() }))
        .put("playlists", JSONArray(playlists.map {
            JSONObject().put("id", it.id).put("name", it.name).put("tracks", JSONArray(it.tracks.map { t -> t.toJson() }))
        }))
        .put("history", JSONArray(history.map {
            JSONObject().put("t", it.track.toJson()).put("n", it.n).put("last", it.last)
        }))
        .put("recent", JSONArray(recent))
        .put("days", JSONArray(days.toList()))
        .put("plays", plays)
        .put("perf", perf)

    companion object {
        private fun <T> JSONArray?.mapObj(f: (JSONObject) -> T): List<T> =
            if (this == null) emptyList() else (0 until length()).map { f(getJSONObject(it)) }

        private fun JSONArray?.strings(): List<String> =
            if (this == null) emptyList() else (0 until length()).map { getString(it) }

        fun from(o: JSONObject) = Db(
            name = o.optString("name"),
            liked = o.optJSONArray("liked").mapObj { Track.from(it) },
            playlists = o.optJSONArray("playlists").mapObj { p ->
                Playlist(p.getString("id"), p.optString("name"), p.optJSONArray("tracks").mapObj { Track.from(it) })
            },
            history = o.optJSONArray("history").mapObj { Played(Track.from(it.getJSONObject("t")), it.optInt("n", 1), it.optLong("last")) },
            recent = o.optJSONArray("recent").strings(),
            days = o.optJSONArray("days").strings().toSet(),
            plays = o.optInt("plays"),
            perf = o.optInt("perf"),
        )
    }
}

data class SearchResult(val items: List<Track>, val src: String) // src: "youtube" | "meta"
