package id.oscarmandala.otacix.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/** LITE = hemat (HP spek rendah): tanpa animasi berat, tanpa efek gyro/blur.  FULL = semua efek liquid glass. */
enum class Tier { LITE, FULL }

object Device {
    fun isHighEnd(ctx: Context): Boolean {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val gb = mi.totalMem / (1024.0 * 1024.0 * 1024.0)
        return Build.VERSION.SDK_INT >= 29 && !am.isLowRamDevice && gb >= 5.5 && Runtime.getRuntime().availableProcessors() >= 8
    }

    /** perf: 0 = otomatis, 1 = paksa hemat, 2 = paksa penuh */
    fun resolve(ctx: Context, perf: Int): Tier = when (perf) {
        1 -> Tier.LITE
        2 -> Tier.FULL
        else -> if (isHighEnd(ctx)) Tier.FULL else Tier.LITE
    }
}
