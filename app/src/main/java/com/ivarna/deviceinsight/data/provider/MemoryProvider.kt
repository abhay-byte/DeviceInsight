package com.ivarna.deviceinsight.data.provider

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MemoryProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getMemoryInfo(): Pair<Long, Long> {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return Pair(memInfo.totalMem, memInfo.availMem)
    }
}
