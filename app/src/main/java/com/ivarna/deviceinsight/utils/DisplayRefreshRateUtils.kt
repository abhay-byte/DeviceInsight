package com.ivarna.deviceinsight.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class DisplayRefreshRateUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    
    @Volatile
    private var currentRefreshRate: Int = 60

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) {
                updateRefreshRate()
            }
        }
    }

    init {
        updateRefreshRate()
        displayManager.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
    }

    private fun updateRefreshRate() {
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        currentRefreshRate = display?.refreshRate?.roundToInt() ?: 60
    }

    fun getRefreshRate(): Int {
        return currentRefreshRate
    }
}
