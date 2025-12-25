package com.ivarna.deviceinsight

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SystemStatsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
