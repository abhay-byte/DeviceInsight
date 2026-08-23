package com.ivarna.deviceinsight.data.monitor

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.ivarna.deviceinsight.ui.caliper.hudFpsModeFlow

@Singleton
class HudSettingsCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile var fpsMode: String = "AUTO"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            try {
                context.hudFpsModeFlow.collect { mode ->
                    fpsMode = mode
                }
            } catch (_: Exception) {}
        }
    }

    fun setImmediate(mode: String) {
        fpsMode = mode
    }
}
