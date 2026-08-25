package com.ivarna.deviceinsight.data.fps.util

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ShellResult(
    val output: String,
    val exitCode: Int
) {
    val isSuccess: Boolean get() = exitCode == 0
}

@Singleton
class ShellExecutor @Inject constructor() {

    @Volatile
    private var hasSu: Boolean? = null

    fun execute(command: String, useRoot: Boolean = false): ShellResult {
        val useSu = useRoot && checkIfSuAvailable()
        val shellCmd = if (useSu) {
            listOf("su", "-c", command)
        } else {
            listOf("sh", "-c", command)
        }
        return try {
            val process = ProcessBuilder()
                .command(shellCmd)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream))
                .readText()
                .trim()
            val exitCode = process.waitFor()
            ShellResult(output, exitCode)
        } catch (e: Exception) {
            ShellResult("error: ${e.message}", -1)
        }
    }

    fun isSuAvailable(): Boolean = checkIfSuAvailable()

    fun clearCache() {
        hasSu = null
    }

    fun checkIfSuAvailable(): Boolean {
        hasSu?.let { return it }
        try {
            val process = ProcessBuilder("su", "-c", "id")
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(3, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                hasSu = false
                return false
            }
            val output = process.inputStream.bufferedReader().readText()
            val ok = process.exitValue() == 0 && (output.contains("uid=0") || output.contains("root"))
            if (ok) {
                hasSu = true
                return true
            }
        } catch (_: Exception) {
        }
        hasSu = false
        return false
    }
}
