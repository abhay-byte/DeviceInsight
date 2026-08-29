package com.ivarna.deviceinsight.ui.caliper

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle

/**
 * Launcher icon = in-app media (activity-alias swap). Enable the new alias FIRST, then
 * disable the others — never zero enabled LAUNCHERs.
 *
 * CRITICAL: a running activity's ActivityRecord component IS its launcher alias, so
 * disabling the enabled alias while any activity is started makes the system finish it
 * (`wm_finish_activity … disabled-package`) — the whole task dies and the user sees a
 * "crash". Therefore swaps are deferred: [request] only stages the desired medium and
 * [attach]'s lifecycle callbacks drain it on the next transition to background.
 */
object LauncherAlias {

    private val ALL = listOf(".MainActivityPaper", ".MainActivityCarbon", ".MainActivityBlueprint")

    @Volatile private var pending: Medium? = null
    private var startedActivities = 0

    fun aliasFor(medium: Medium): String = when (medium) {
        Medium.PAPER -> ".MainActivityPaper"
        Medium.CARBON -> ".MainActivityCarbon"
        Medium.BLUEPRINT -> ".MainActivityBlueprint"
    }

    /** Alias the system currently reports ENABLED (manifest pins explicit states). */
    fun enabledAlias(ctx: Context): String? {
        val pm = ctx.packageManager
        return ALL.firstOrNull { name ->
            runCatching {
                pm.getComponentEnabledSetting(ComponentName(ctx, ctx.packageName + name)) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }.getOrDefault(false)
        }
    }

    /** Stage a media change; applied only once every activity has stopped. */
    fun request(ctx: Context, medium: Medium) {
        if (enabledAlias(ctx) == aliasFor(medium)) {
            pending = null
            return
        }
        pending = medium
        if (startedActivities <= 0) drain(ctx)
    }

    /** Retry any pending alias swap — called after overlay stops or config stabilises. */
    fun retryPending(ctx: Context) {
        if (pending != null && startedActivities <= 0) drain(ctx)
    }

    fun attach(app: Application) {
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                startedActivities--
                if (startedActivities <= 0) {
                    startedActivities = 0
                    drain(app)
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }
        app.registerActivityLifecycleCallbacks(callbacks)
    }

    private fun drain(ctx: Context) {
        val medium = pending ?: return
        // Defer alias swap while the HUD overlay window is attached — the overlay's
        // WindowManager token survives theme switches, but swapping the LAUNCHER
        // alias while TYPE_APPLICATION_OVERLAY is on-screen triggers an OEM
        // BadToken/window-leak on some devices. Keep the pending value and retry
        // after the overlay stops (OverlayService.onDestroy calls retryPending).
        if (isOverlayRunning()) {
            return
        }
        pending = null
        runCatching { apply(ctx, medium) }
    }

    private fun isOverlayRunning(): Boolean {
        return try {
            Class.forName("com.ivarna.deviceinsight.service.OverlayService")
                .getDeclaredField("isRunning")
                .get(null)
                .let { it as java.util.concurrent.atomic.AtomicBoolean }
                .get()
        } catch (_: Exception) { false }
    }

    fun apply(ctx: Context, medium: Medium) {
        val pm = ctx.packageManager
        val target = aliasFor(medium)
        runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(ctx, ctx.packageName + target),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        ALL.filter { it != target }.forEach { name ->
            runCatching {
                pm.setComponentEnabledSetting(
                    ComponentName(ctx, ctx.packageName + name),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
