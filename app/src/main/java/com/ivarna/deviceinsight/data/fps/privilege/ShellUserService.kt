package com.ivarna.deviceinsight.data.fps.privilege

import android.content.Context
import android.os.Binder
import android.system.Os
import android.util.Log
import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.InputStreamReader

class ShellUserService : IShellService.Stub {

    constructor() {
        Log.i(TAG, "ShellUserService created (no context)")
    }

    @Keep
    constructor(context: Context) {
        Log.i(TAG, "ShellUserService created with context=$context")
    }

    override fun destroy() {
        Log.i(TAG, "destroy")
        System.exit(0)
    }

    override fun exec(command: String?): String {
        if (command.isNullOrBlank()) return ""
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val err = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val code = process.waitFor()
            if (code != 0) {
                val detail = (err.ifBlank { output }).trim()
                return if (detail.isEmpty()) "error: exit $code" else "error: exit $code: $detail"
            }
            output.trim()
        } catch (e: Exception) {
            Log.w(TAG, "exec failed: $command", e)
            "error: ${e.message}"
        }
    }

    override fun getUid(): Int = Os.getuid()

    companion object {
        private const val TAG = "DeviceInsightShellSvc"
    }
}
