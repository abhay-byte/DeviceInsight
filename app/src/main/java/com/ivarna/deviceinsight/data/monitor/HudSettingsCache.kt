package com.ivarna.deviceinsight.data.monitor

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.ivarna.deviceinsight.data.fps.model.FpsMode
import com.ivarna.deviceinsight.ui.caliper.caliperDataStore
import com.ivarna.deviceinsight.ui.caliper.hudRuntimeConfigFlow
import com.ivarna.deviceinsight.ui.caliper.hud.HudDefaults

@Singleton
class HudSettingsCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile var fpsMode: FpsMode = HudDefaults.fpsMode

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            try {
                fpsMode = context.caliperDataStore.data.first().let { prefs ->
                    com.ivarna.deviceinsight.ui.caliper.hud.HudConfigCodec.fromPreferences(prefs).fpsMode
                }
                context.hudRuntimeConfigFlow.collect { config ->
                    fpsMode = config.fpsMode
                }
            } catch (t: Throwable) {
                android.util.Log.e("DeviceInsightFps", "FPS_MODE_LOAD_FAILED", t)
            }
        }
    }

    fun setImmediate(mode: FpsMode) {
        fpsMode = mode
    }
}
