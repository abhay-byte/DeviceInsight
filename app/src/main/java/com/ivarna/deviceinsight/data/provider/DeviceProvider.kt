package com.ivarna.deviceinsight.data.provider

import android.content.Context
import android.os.Build
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DeviceProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getDeviceType(): String {
        val count = context.resources.configuration.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
        return when (count) {
            android.content.res.Configuration.SCREENLAYOUT_SIZE_SMALL -> "Small Phone"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_NORMAL -> "Phone"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE -> "Phablet"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE -> "Tablet"
            else -> "Unknown"
        }
    }

    fun getSerial(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Build.getSerial()
            } else {
                Build.SERIAL
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getKernelVersion(): String {
        return try {
            System.getProperty("os.version") ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getUpTime(): String {
        val uptimeMillis = SystemClock.elapsedRealtime()
        val uptimeSeconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMillis)
        val days = TimeUnit.SECONDS.toDays(uptimeSeconds)
        val hours = TimeUnit.SECONDS.toHours(uptimeSeconds) - TimeUnit.DAYS.toHours(days)
        val minutes = TimeUnit.SECONDS.toMinutes(uptimeSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(uptimeSeconds))
        return "${days}d ${hours}h ${minutes}m"
    }
}
