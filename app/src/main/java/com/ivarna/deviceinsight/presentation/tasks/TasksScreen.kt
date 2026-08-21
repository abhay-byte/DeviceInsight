package com.ivarna.deviceinsight.presentation.tasks

import android.content.Intent
import android.provider.Settings
import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.domain.model.AppProcessInfo
import com.ivarna.deviceinsight.ui.caliper.*
import com.ivarna.deviceinsight.ui.caliper.components.*
import kotlinx.coroutines.delay

// Task "Application Active" page → PROCESSES ledger (S-09). Nav entry moved to
// last rail position (caliper-001 m2) — see SystemStatsApp.kt.
@Composable
fun TasksScreen(
    viewModel: TasksViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    val runningApps by viewModel.runningApps.collectAsStateWithLifecycle()
    val c = Caliper.colors
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionAndLoadApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // B5: live re-rank throttled 2s — poll while permission held
    LaunchedEffect(hasPermission) {
        while (hasPermission) {
            delay(2000)
            viewModel.checkPermissionAndLoadApps()
        }
    }

    if (!hasPermission) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ScreenHeader(
                sheetLabel = "№ 04 — PROCESSES",
                title = "Processes.",
                sub = "usage access required",
                warn = true
            )
            Spacer(Modifier.height(16.dp))
            MarginNote(
                message = "Usage access was revoked. CPU per-process figures are now estimated (≈).",
                error = true,
                actionLabel = "GRANT",
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )
            Spacer(Modifier.height(16.dp))
            EmptyState(
                title = "CHANNEL LOCKED",
                message = "grant usage access to read the process ledger",
                actionLabel = "GRANT USAGE ACCESS",
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )
        }
        return
    }

    // B5: join ActivityManager for live pid/rss (best-effort)
    val pidMap = remember(runningApps) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val procs = am.runningAppProcesses ?: emptyList()
            // pid per package: take max pid for that package
            procs.groupBy { it.processName.substringBefore(":") }
                .mapValues { (_, v) -> v.maxOf { it.pid } }
        } catch (_: Exception) { emptyMap() }
    }
    val rssMap = remember(runningApps, pidMap) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val pids = pidMap.values.toIntArray()
            if (pids.isEmpty()) emptyMap()
            else {
                val infos = am.getProcessMemoryInfo(pids)
                pidMap.entries.associate { (pkg, pid) ->
                    val idx = pids.indexOf(pid)
                    val rss = if (idx >= 0 && idx < infos.size) infos[idx].totalPss * 1024L else 0L
                    pkg to rss
                }
            }
        } catch (_: Exception) { emptyMap() }
    }
    ProcessesScreen(
        rows = runningApps.mapIndexed { index, app ->
            val pid = pidMap[app.packageName] ?: 0
            val rss = rssMap[app.packageName] ?: 0L
            ProcRow(
                index = index + 1,
                pkg = app.packageName,
                cpu = 0f,                       // usage-stats API does not expose live CPU
                rssBytes = rss,
                pid = pid,
                uptime = formatForeground(app.totalTimeInForeground),
                state = ProcState.BACKGROUND,
                isSelf = app.packageName == context.packageName,
                isSystem = app.isSystemApp,
                threads = 1
            )
        },
        rootAvailable = false,                  // no root kill path in current repo
        onForceStop = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:${it.pkg}")
                )
            )
        },
        onTerminate = {}
    )
}

private fun formatForeground(ms: Long): String {
    val seconds = ms / 1000
    val m = seconds / 60
    val h = m / 60
    return if (h > 0) "${h}h ${m % 60}m" else "${m}m"
}