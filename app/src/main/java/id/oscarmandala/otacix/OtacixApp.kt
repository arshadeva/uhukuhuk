package id.oscarmandala.otacix

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import id.oscarmandala.otacix.data.Repo
import id.oscarmandala.otacix.data.Store
import id.oscarmandala.otacix.player.PlayerController
import id.oscarmandala.otacix.util.Device
import id.oscarmandala.otacix.util.Tier

class OtacixApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        store = Store(this)
        tier = Device.resolve(this, store.db.value.perf)
        repo = Repo()
        player = PlayerController(this, store)
    }

    override fun newImageLoader(): ImageLoader {
        val full = tier == Tier.FULL
        return ImageLoader.Builder(this)
            .crossfade(full)
            .allowRgb565(!full) // hemat RAM di HP spek rendah
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(if (full) 0.20 else 0.10).build() }
            .diskCache {
                DiskCache.Builder().directory(cacheDir.resolve("img"))
                    .maxSizeBytes(if (full) 150L * 1024 * 1024 else 60L * 1024 * 1024).build()
            }
            .respectCacheHeaders(false)
            .build()
    }

    companion object {
        lateinit var store: Store
        lateinit var repo: Repo
        lateinit var player: PlayerController
        @Volatile var tier: Tier = Tier.FULL
    }
}
