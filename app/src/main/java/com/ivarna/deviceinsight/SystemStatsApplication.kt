package com.ivarna.deviceinsight

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.ivarna.deviceinsight.ui.caliper.CaliperKeys
import com.ivarna.deviceinsight.ui.caliper.LauncherAlias
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.ui.caliper.caliperDataStore
import com.ivarna.deviceinsight.ui.caliper.mediumFlow
import com.ivarna.deviceinsight.ui.caliper.widget.BenchBudget
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@HiltAndroidApp
class SystemStatsApplication : Application(), ImageLoaderFactory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // HUD migration (single source caliper DataStore) + WM enqueue
        applicationScope.launch {
            runCatching { migrateOverlayPrefs() }
            runCatching { BenchBudget.enqueue(this@SystemStatsApplication) }
        }
        // launcher alias sync: stage DataStore medium, drain on first background —
        // never swap while an activity is started (system finishes the task)
        LauncherAlias.attach(this)
        applicationScope.launch {
            runCatching {
                val m = mediumFlow.first() ?: Medium.PAPER
                LauncherAlias.request(this@SystemStatsApplication, m)
            }
        }
    }

    private suspend fun migrateOverlayPrefs() {
        val prefs = caliperDataStore.data.first()
        if (prefs[CaliperKeys.hudMigrated] == true) return
        val legacy = getSharedPreferences("overlay_prefs", Context.MODE_PRIVATE)
        if (!legacy.all.isEmpty()) {
            val fpsModeLegacy = legacy.getString("fps_mode", null) ?: legacy.getString("fpsMode", null)
            val showFps = legacy.getBoolean("showFps", true)
            val showCpu = legacy.getBoolean("showCpu", true)
            val showRam = legacy.getBoolean("showRam", true)
            val showSwap = legacy.getBoolean("showSwap", true)
            val showPower = legacy.getBoolean("showPower", true)
            val showBattery = legacy.getBoolean("showBattery", true)
            val showNetwork = legacy.getBoolean("showNetwork", true)
            val showCpuGraph = legacy.getBoolean("showCpuGraph", false)
            val showPowerGraph = legacy.getBoolean("showPowerGraph", false)
            // map to modules
            val modules = mutableListOf<String>()
            if (showFps) modules.add("FPS")
            if (showCpu) modules.add("CPU")
            if (showRam || showSwap) modules.add("MEMORY")
            if (showPower || showBattery) modules.add("POWER")
            if (showNetwork) modules.add("NETWORK")
            if (showCpuGraph || showPowerGraph) modules.add("TRACE")
            // Gpu is not in legacy — add GPU if vendor known? default omit until fitted
            // Use defaults for other hud keys if legacy didn't have them
            val modulesCsv = if (modules.isEmpty()) "FPS,CPU,MEMORY,POWER" else modules.joinToString(",")
            caliperDataStore.edit { e ->
                fpsModeLegacy?.let { e[CaliperKeys.fpsMode] = it }
                e[CaliperKeys.hudModules] = modulesCsv
                if (!e.contains(CaliperKeys.hudMedium)) e[CaliperKeys.hudMedium] = "CARBON"
                if (!e.contains(CaliperKeys.hudScale)) e[CaliperKeys.hudScale] = "M"
                if (!e.contains(CaliperKeys.hudOpacity)) e[CaliperKeys.hudOpacity] = 0.75f
                if (!e.contains(CaliperKeys.hudBlur)) e[CaliperKeys.hudBlur] = true
                if (!e.contains(CaliperKeys.hudLocked)) e[CaliperKeys.hudLocked] = false
                if (!e.contains(CaliperKeys.hudShowCoreBank)) e[CaliperKeys.hudShowCoreBank] = true
                e[CaliperKeys.hudMigrated] = true
            }
            // clear legacy prefs after successful migration
            legacy.edit().clear().apply()
        } else {
            // No legacy data — just mark migrated
            caliperDataStore.edit { it[CaliperKeys.hudMigrated] = true }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header(
                                "User-Agent",
                                "DeviceInsight/1.0 (https://github.com/ivarna/deviceinsight; contact@deviceinsight.app)"
                            )
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .respectCacheHeaders(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
