package com.ivarna.deviceinsight.domain.model

import android.graphics.drawable.Drawable

data class AppProcessInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long,
    val isSystemApp: Boolean
)
