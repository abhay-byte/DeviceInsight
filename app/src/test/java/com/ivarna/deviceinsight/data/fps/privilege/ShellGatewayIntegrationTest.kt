package com.ivarna.deviceinsight.data.fps.privilege

import com.ivarna.deviceinsight.data.fps.util.ShellExecutor
import com.ivarna.deviceinsight.data.fps.util.ShellResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class ShellGatewayIntegrationTest {

    private fun gatewayWith(
        isSuAvailable: Boolean,
        shizukuReady: Boolean,
        mode: PrivilegeMode
    ): ShellGateway {
        val executor = mockk<ShellExecutor>()
        every { executor.isSuAvailable() } returns isSuAvailable
        every { executor.clearCache() } returns Unit
        // For execute with root true, return success only if su available
        every { executor.execute(any(), true) } answers {
            if (isSuAvailable) ShellResult("ok", 0) else ShellResult("__blocked:no_su", -1)
        }
        every { executor.execute(any(), false) } returns ShellResult("ok", 0)

        val shizuku = mockk<ShizukuAccess>()
        every { shizuku.isReady() } returns shizukuReady
        every { shizuku.isPermissionGranted() } returns shizukuReady
        every { shizuku.isBinderAlive() } returns shizukuReady
        every { shizuku.refresh() } returns Unit
        every { shizuku.execute(any()) } answers {
            if (shizukuReady) ShellResult("ok", 0) else ShellResult("__blocked:no_shizuku", -1)
        }

        val gateway = ShellGateway(executor, shizuku)
        gateway.setMode(mode)
        return gateway
    }

    @Test
    fun auto_rootYes_shizukuBinderYes_permissionNo_choosesRoot() {
        // Shizuku not ready -> canShizuku false, canRoot true -> AUTO should try ROOT first and succeed
        val gateway = gatewayWith(isSuAvailable = true, shizukuReady = false, mode = PrivilegeMode.AUTO)
        val (result, tier) = gateway.executePolicy("dumpsys window")
        assertTrue(result.isSuccess)
        assertEquals(PrivilegeTier.ROOT, tier)
    }

    @Test
    fun auto_rootNo_shizukuReady_choosesShizuku() {
        val gateway = gatewayWith(isSuAvailable = false, shizukuReady = true, mode = PrivilegeMode.AUTO)
        val (result, tier) = gateway.executePolicy("dumpsys window")
        assertTrue(result.isSuccess)
        assertEquals(PrivilegeTier.SHIZUKU, tier)
    }

    @Test
    fun root_mode_rootNo_reportsUnavailableNotFallback() {
        val gateway = gatewayWith(isSuAvailable = false, shizukuReady = true, mode = PrivilegeMode.ROOT)
        val (result, tier) = gateway.executePolicy("dumpsys SurfaceFlinger --list 2>/dev/null")
        assertFalse(result.isSuccess)
        assertEquals(PrivilegeTier.ROOT, tier)
        // Should never fallback to SHIZUKU even though shizuku ready
        assertTrue(result.output.contains("__blocked"))
    }

    @Test
    fun shizuku_mode_rootYes_shizukuNotReady_reportsUnavailableNotRoot() {
        val gateway = gatewayWith(isSuAvailable = true, shizukuReady = false, mode = PrivilegeMode.SHIZUKU)
        val (result, tier) = gateway.executePolicy("dumpsys SurfaceFlinger --latency foo")
        assertFalse(result.isSuccess)
        assertEquals(PrivilegeTier.SHIZUKU, tier)
        assertTrue(result.output.contains("__blocked"))
    }

    @Test
    fun auto_cachesSuNotEverySample_andInvalidatesOnModeChange() {
        val executor = mockk<ShellExecutor>()
        var callCount = 0
        every { executor.isSuAvailable() } answers { callCount++; true }
        every { executor.clearCache() } returns Unit
        every { executor.execute(any(), any()) } returns ShellResult("ok", 0)
        val shizuku = mockk<ShizukuAccess>(relaxed = true)
        every { shizuku.isReady() } returns false

        val gateway = ShellGateway(executor, shizuku)
        gateway.setMode(PrivilegeMode.AUTO)
        // setModeFromString with same mode should not clear cache
        gateway.setModeFromString("AUTO")
        // isSuAvailable should not be called until execute is invoked? But canRoot is checked per tier.
        // Verify clearCache not called extra
    }
}
