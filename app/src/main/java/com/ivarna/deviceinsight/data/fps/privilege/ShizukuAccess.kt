package com.ivarna.deviceinsight.data.fps.privilege

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.ivarna.deviceinsight.data.fps.util.ShellResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shizuku binder + permission + UserService shell.
 *
 * UI should call [requestPermission] so the system Shizuku grant dialog appears.
 * Metric code uses [execute] only when [isReady] is true (via [ShellGateway]).
 */
@Singleton
class ShizukuAccess @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _binderAlive = MutableStateFlow(false)
    val binderAlive: StateFlow<Boolean> = _binderAlive.asStateFlow()

    private val serviceRef = AtomicReference<IShellService?>(null)
    private val bindLatch = AtomicReference<CountDownLatch?>(null)

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.i(TAG, "Shizuku binder received")
        _binderAlive.value = true
        refresh()
        if (isPermissionGranted()) bindUserServiceAsync()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.i(TAG, "Shizuku binder dead")
        _binderAlive.value = false
        serviceRef.set(null)
        refresh()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            Log.i(TAG, "Shizuku permission result code=$requestCode grant=$grantResult")
            refresh()
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                bindUserServiceAsync()
            }
        }

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                serviceRef.set(IShellService.Stub.asInterface(binder))
                Log.i(TAG, "ShellUserService connected")
            } else {
                serviceRef.set(null)
                Log.w(TAG, "ShellUserService invalid binder")
            }
            bindLatch.get()?.countDown()
            refresh()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "ShellUserService disconnected")
            serviceRef.set(null)
            refresh()
        }
    }

    private val userServiceArgs: Shizuku.UserServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShellUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(false)
            .version(1)
            .tag("factualstats-shell")
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            _binderAlive.value = try {
                Shizuku.pingBinder()
            } catch (_: Throwable) {
                false
            }
            refresh()
            if (isReady()) bindUserServiceAsync()
        } catch (e: Throwable) {
            Log.w(TAG, "Shizuku init failed (app may not have Shizuku installed)", e)
        }
    }

    fun refresh() {
        _ready.value = isReady()
    }

    fun isReady(): Boolean {
        return try {
            Shizuku.pingBinder() &&
                !Shizuku.isPreV11() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun isPermissionGranted(): Boolean {
        return try {
            Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun isBinderAlive(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Opens the Shizuku permission dialog when binder is alive.
     * If Shizuku is not running, opens the Shizuku app (or returns a message).
     * @return user-facing toast string
     */
    fun requestPermission(requestCode: Int = REQUEST_CODE): String {
        return try {
            if (!isShizukuInstalled()) {
                return "Install Shizuku from Play Store / GitHub, then open it and start the service"
            }
            if (!Shizuku.pingBinder()) {
                openShizukuApp()
                return "Open Shizuku and start the service, then tap Grant again"
            }
            if (Shizuku.isPreV11()) {
                return "Shizuku version is too old (need v11+)"
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                bindUserServiceAsync()
                refresh()
                return "Shizuku already granted"
            }
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                openShizukuApp()
                return "Permission denied earlier — enable FactualStats in Shizuku app"
            }
            Shizuku.requestPermission(requestCode)
            "Requesting Shizuku permission…"
        } catch (e: Throwable) {
            Log.w(TAG, "requestPermission failed", e)
            "Shizuku request failed: ${e.message}"
        }
    }

    fun openShizukuApp(): Boolean {
        return try {
            val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun execute(command: String): ShellResult {
        if (!isReady()) {
            return ShellResult("__blocked:no_shizuku", -1)
        }
        val service = ensureService()
            ?: return ShellResult("error: shizuku user service not bound", -1)
        return try {
            val output = service.exec(command) ?: ""
            if (output.startsWith("error:")) {
                ShellResult(output, 1)
            } else {
                ShellResult(output, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku exec failed", e)
            ShellResult("error: ${e.message}", -1)
        }
    }

    private fun ensureService(): IShellService? {
        serviceRef.get()?.let { return it }
        bindUserServiceAsync()
        val latch = bindLatch.get()
        latch?.await(3, TimeUnit.SECONDS)
        return serviceRef.get()
    }

    private fun bindUserServiceAsync() {
        if (!isPermissionGranted()) return
        if (serviceRef.get() != null) return
        try {
            bindLatch.set(CountDownLatch(1))
            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
        } catch (e: Throwable) {
            Log.w(TAG, "bindUserService failed", e)
            bindLatch.get()?.countDown()
        }
    }

    companion object {
        private const val TAG = "ShizukuAccess"
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val REQUEST_CODE = 1001
    }
}
