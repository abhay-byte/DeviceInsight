package com.ivarna.deviceinsight.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.ivarna.deviceinsight.domain.model.AppProcessInfo
import com.ivarna.deviceinsight.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TaskRepository {

    override fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override suspend fun getRunningProcesses(): List<AppProcessInfo> = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission()) return@withContext emptyList()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, -24) // Last 24 hours
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val appList = mutableListOf<AppProcessInfo>()

        for (usageStats in usageStatsList) {
            if (usageStats.totalTimeInForeground > 0) {
                try {
                    val appInfo = pm.getApplicationInfo(usageStats.packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    appList.add(
                        AppProcessInfo(
                            packageName = usageStats.packageName,
                            appName = appName,
                            icon = icon,
                            totalTimeInForeground = usageStats.totalTimeInForeground,
                            lastTimeUsed = usageStats.lastTimeUsed,
                            isSystemApp = isSystem
                        )
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    // App might be uninstalled or hidden
                    continue
                }
            }
        }

        // Sort by most recently used
        appList.sortedByDescending { it.lastTimeUsed }
    }
}
