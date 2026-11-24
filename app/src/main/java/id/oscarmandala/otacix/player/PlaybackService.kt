package id.oscarmandala.otacix.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import id.oscarmandala.otacix.MainActivity
import id.oscarmandala.otacix.OtacixApp
import id.oscarmandala.otacix.data.Yt
import id.oscarmandala.otacix.util.Tier
import java.io.File
import java.io.IOException

/** Cache audio di disk: lagu yang pernah diputar bisa diputar ulang tanpa internet. */
object AudioCache {
    @Volatile private var cache: SimpleCache? = null
    fun get(ctx: Context): SimpleCache = synchronized(this) {
        cache ?: run {
            val mb = if (OtacixApp.tier == Tier.FULL) 512L else 128L
            SimpleCache(File(ctx.cacheDir, "audio"), LeastRecentlyUsedCacheEvictor(mb * 1024 * 1024), StandaloneDatabaseProvider(ctx))
                .also { cache = it }
        }
    }
    fun clear(ctx: Context) {
        runCatching { get(ctx).keys.toList().forEach { get(ctx).removeResource(it) } }
    }
    fun sizeBytes(ctx: Context): Long = runCatching { get(ctx).cacheSpace }.getOrDefault(0L)
}

/** Mengubah otacix://yt/ID menjadi URL googlevideo asli tepat saat dibutuhkan. */
private object YtResolver : ResolvingDataSource.Resolver {
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val uri = dataSpec.uri
        if (uri.scheme != "otacix") return dataSpec
        val id = uri.lastPathSegment ?: throw IOException("ID lagu kosong")
        val s = Yt.resolveBlocking(id, false, uri.getQueryParameter("q")) ?: throw IOException("Tidak bisa mendapatkan audio untuk $id")
        return dataSpec.buildUpon()
            .setUri(Uri.parse(s.url))
            .setHttpRequestHeaders(dataSpec.httpRequestHeaders + ("User-Agent" to s.ua))
            .build()
    }
}

class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private val retried = mutableSetOf<String>()
    private var lastToast = 0L

    override fun onCreate() {
        super.onCreate()
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(15_000).setReadTimeoutMs(20_000)
        val resolving = ResolvingDataSource.Factory(ChunkedDataSource.Factory(http), YtResolver)
        val cached = CacheDataSource.Factory().setCache(AudioCache.get(this))
            .setUpstreamDataSourceFactory(resolving)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cached))
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true
            )
            .setHandleAudioBecomingNoisy(true)   // berhenti saat earphone dicabut
            .setWakeMode(C.WAKE_MODE_NETWORK)    // tetap jalan saat layar mati
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) player.currentMediaItem?.mediaId?.let { retried.remove(it) }
            }
            override fun onPlayerError(error: PlaybackException) {
                val id = player.currentMediaItem?.mediaId ?: return
                if (retried.add(id)) {
                    // URL kemungkinan kedaluwarsa: ambil ulang lalu lanjut dari posisi yang sama
                    Yt.invalidate(id)
                    player.seekTo(player.currentMediaItemIndex, player.currentPosition)
                    player.prepare()
                } else {
                    val t = System.currentTimeMillis()
                    if (t - lastToast > 5_000) {
                        lastToast = t
                        val why = Yt.lastError.ifEmpty { error.cause?.message ?: error.message ?: "tidak diketahui" }
                        android.widget.Toast.makeText(applicationContext, "Gagal memutar: " + why.take(160), android.widget.Toast.LENGTH_LONG).show()
                    }
                    if (player.hasNextMediaItem()) {
                        player.seekToNextMediaItem()
                        player.prepare()
                    }
                }
            }
        })

        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, player).setSessionActivity(open).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}
