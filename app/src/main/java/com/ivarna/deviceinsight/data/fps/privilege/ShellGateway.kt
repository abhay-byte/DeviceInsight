package com.ivarna.deviceinsight.data.fps.privilege

import com.ivarna.deviceinsight.data.fps.util.ShellExecutor
import com.ivarna.deviceinsight.data.fps.util.ShellResult
import com.ivarna.deviceinsight.data.fps.model.FpsMode
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShellGateway @Inject constructor(
    private val shellExecutor: ShellExecutor,
    private val shizukuAccess: ShizukuAccess
) {
    // Cache mode? We'll derive from HudSettingsCache via injection or set externally
    @Volatile
    private var currentMode: PrivilegeMode = PrivilegeMode.AUTO

    fun setMode(mode: PrivilegeMode) {
        if (currentMode != mode) {
            currentMode = mode
            clearCache()
        }
    }

    fun setMode(mode: FpsMode) {
        setMode(
            when (mode) {
                FpsMode.AUTO -> PrivilegeMode.AUTO
                FpsMode.ROOT -> PrivilegeMode.ROOT
                FpsMode.SHIZUKU -> PrivilegeMode.SHIZUKU
            }
        )
    }

    fun setModeFromString(modeStr: String?) {
        setMode(
            when (modeStr?.uppercase()) {
                FpsMode.ROOT.name -> FpsMode.ROOT
                FpsMode.SHIZUKU.name -> FpsMode.SHIZUKU
                else -> FpsMode.AUTO
            }
        )
    }

    fun currentPolicy(): PrivilegePolicy = PrivilegePolicy(currentMode)

    fun canRoot(): Boolean = shellExecutor.isSuAvailable()
    fun canShizuku(): Boolean = shizukuAccess.isReady()
    fun canStandard(): Boolean = true

    fun execute(command: String, tier: PrivilegeTier): ShellResult = when (tier) {
        PrivilegeTier.ROOT -> {
            if (!canRoot()) ShellResult("__blocked:no_su", -1)
            else shellExecutor.execute(command, useRoot = true)
        }
        PrivilegeTier.SHIZUKU -> {
            if (!canShizuku()) ShellResult("__blocked:no_shizuku", -1)
            else shizukuAccess.execute(command)
        }
        PrivilegeTier.STANDARD -> shellExecutor.execute(command, useRoot = false)
    }

    fun executeChain(command: String, chain: List<PrivilegeTier>): Pair<ShellResult, PrivilegeTier?> {
        var lastResult = ShellResult("", -1)
        var lastTier: PrivilegeTier? = null
        for (tier in chain) {
            lastTier = tier
            lastResult = execute(command, tier)
            if (lastResult.isSuccess) return lastResult to tier
        }
        return lastResult to lastTier
    }

    fun executePolicy(command: String, defaultChain: List<PrivilegeTier> = PrivilegePolicy.DEFAULT_CHAIN): Pair<ShellResult, PrivilegeTier?> =
        executeChain(command, currentPolicy().chain(defaultChain))

    fun readPath(path: String, defaultChain: List<PrivilegeTier> = PrivilegePolicy.DEFAULT_CHAIN): Pair<String?, PrivilegeTier?> {
        val chain = currentPolicy().chain(defaultChain)
        if (chain.contains(PrivilegeTier.STANDARD)) {
            try {
                val f = File(path)
                if (f.canRead()) {
                    val text = f.readText().trim().takeIf { it.isNotEmpty() }
                    if (text != null) return text to PrivilegeTier.STANDARD
                }
            } catch (_: Exception) {}
        }
        for (tier in chain) {
            val result = execute("cat \"$path\" 2>/dev/null", tier)
            if (result.isSuccess && result.output.isNotBlank() && !isBlocked(result.output)) {
                return result.output.trim() to tier
            }
        }
        return null to null
    }

    fun clearCache() {
        shellExecutor.clearCache()
        shizukuAccess.refresh()
    }

    companion object {
        private fun isBlocked(output: String) = output.startsWith("__blocked:")
    }
}
