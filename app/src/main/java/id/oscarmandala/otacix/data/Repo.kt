package id.oscarmandala.otacix.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class Shelf(val title: String, val query: String, val items: List<Track>?) // items == null -> masih memuat

val MOODS = listOf(
    Triple("bolt", "Energize", "lagu semangat energik"),
    Triple("smile", "Feel good", "lagu feel good ceria"),
    Triple("dumbbell", "Workout", "workout gym music"),
    Triple("drop", "Sad", "lagu sedih galau"),
    Triple("moon", "Chill", "lagu chill santai"),
    Triple("target", "Fokus", "lofi study focus"),
    Triple("star", "Party", "party hits dance"),
    Triple("heart", "Romantis", "lagu romantis cinta"),
)

class Repo {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val defs = listOf(
        "Lagi Tren" to "lagu indonesia terpopuler 2026",
        "Santai Sore" to "lagu santai akustik indonesia",
        "Hits Global" to "top global hits 2026",
    )
    private val _shelves = MutableStateFlow(defs.map { Shelf(it.first, it.second, null) })
    val shelves: StateFlow<List<Shelf>> = _shelves
    private var loaded = false

    fun loadHome(force: Boolean = false) {
        if (loaded && !force) return
        loaded = true
        defs.forEachIndexed { i, (title, q) ->
            scope.launch {
                val r = try { Yt.search(q) } catch (e: Exception) { SearchResult(emptyList(), "youtube") }
                _shelves.value = _shelves.value.toMutableList().also { it[i] = Shelf(title, q, r.items) }
                if (r.items.isEmpty()) loaded = false // izinkan coba lagi
            }
        }
    }

    suspend fun search(q: String): SearchResult = Yt.search(q)
}
