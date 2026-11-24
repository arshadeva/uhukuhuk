package id.oscarmandala.otacix.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import id.oscarmandala.otacix.data.Store
import id.oscarmandala.otacix.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PlayerUi(
    val track: Track? = null,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: Int = Player.REPEAT_MODE_OFF,
    val speed: Float = 1f,
    val queue: List<Track> = emptyList(),
    val index: Int = -1,
)

data class Progress(val pos: Long = 0L, val dur: Long = 0L)

/** Jembatan UI -> PlaybackService (MediaController). Semua pemanggilan di thread utama. */
class PlayerController(private val ctx: Context, private val store: Store) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private val pending = ArrayList<(MediaController) -> Unit>()
    private var started = false

    private val _ui = MutableStateFlow(PlayerUi())
    val ui: StateFlow<PlayerUi> = _ui
    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress

    /** Sisa timer tidur dalam ms. 0 = mati, -1 = berhenti di akhir lagu. */
    private val _sleep = MutableStateFlow(0L)
    val sleep: StateFlow<Long> = _sleep
    private var sleepJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { sync() }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { store.recordPlay(it.toTrack()) }
            if (_sleep.value == -1L && reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                controller?.pause(); _sleep.value = 0L
            }
        }
    }

    fun connect() {
        if (started) return
        started = true
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val f = MediaController.Builder(ctx, token).buildAsync()
        f.addListener({
            val c = try { f.get() } catch (e: Exception) { started = false; return@addListener }
            controller = c
            c.addListener(listener)
            sync()
            pending.forEach { it(c) }
            pending.clear()
        }, ContextCompat.getMainExecutor(ctx))

        scope.launch {
            while (true) {
                delay(500)
                val c = controller ?: continue
                if (c.isPlaying || c.playbackState == Player.STATE_BUFFERING) {
                    _progress.value = Progress(c.currentPosition.coerceAtLeast(0), durOf(c))
                }
            }
        }
    }

    private fun durOf(c: MediaController): Long {
        val d = c.duration
        return if (d == C.TIME_UNSET || d <= 0) (_ui.value.track?.dur ?: 0) * 1000L else d
    }

    private fun sync() {
        val c = controller ?: return
        val q = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).toTrack() }
        _ui.value = PlayerUi(
            track = c.currentMediaItem?.toTrack(),
            playing = c.isPlaying,
            buffering = c.playbackState == Player.STATE_BUFFERING,
            shuffle = c.shuffleModeEnabled,
            repeat = c.repeatMode,
            speed = c.playbackParameters.speed,
            queue = q,
            index = c.currentMediaItemIndex,
        )
        _progress.value = Progress(c.currentPosition.coerceAtLeast(0), durOf(c))
    }

    private fun withC(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else { pending.add(block); connect() }
    }

    // ---- perintah ----
    fun play(tracks: List<Track>, index: Int) {
        if (tracks.isEmpty()) return
        withC { c ->
            c.setMediaItems(tracks.map { it.toMediaItem() }, index.coerceIn(0, tracks.lastIndex), 0L)
            c.prepare(); c.play()
        }
    }
    fun playOne(t: Track) = play(listOf(t), 0)
    fun toggle() = withC { c -> if (c.isPlaying) c.pause() else { if (c.playbackState == Player.STATE_IDLE) c.prepare(); c.play() } }
    fun next() = withC { it.seekToNextMediaItem() }
    fun prev() = withC { it.seekToPrevious() }
    fun seekTo(ms: Long) = withC { it.seekTo(ms) }
    fun jumpTo(i: Int) = withC { it.seekToDefaultPosition(i); it.play() }
    fun setShuffle(v: Boolean) = withC { it.shuffleModeEnabled = v }
    fun cycleRepeat() = withC {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
    fun setSpeed(s: Float) = withC { it.setPlaybackSpeed(s) }
    fun playNext(t: Track) = withC { c ->
        if (c.mediaItemCount == 0) { play(listOf(t), 0) } else c.addMediaItem(c.currentMediaItemIndex + 1, t.toMediaItem())
    }
    fun addToQueue(t: Track) = withC { c -> if (c.mediaItemCount == 0) play(listOf(t), 0) else c.addMediaItem(t.toMediaItem()) }
    fun removeFromQueue(i: Int) = withC { c -> if (i != c.currentMediaItemIndex) c.removeMediaItem(i) }

    // ---- timer tidur (fade-out lembut) ----
    fun setSleep(minutes: Int) {
        sleepJob?.cancel()
        when {
            minutes == 0 -> _sleep.value = 0L
            minutes < 0 -> _sleep.value = -1L
            else -> sleepJob = scope.launch {
                var left = minutes * 60_000L
                while (left > 0) { _sleep.value = left; delay(1000); left -= 1000 }
                _sleep.value = 0L
                val c = controller ?: return@launch
                var v = 1f
                while (v > 0.05f) { v -= 0.05f; c.volume = v; delay(350) }
                c.pause(); c.volume = 1f
            }
        }
    }
}
