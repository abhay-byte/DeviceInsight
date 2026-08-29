package com.ivarna.deviceinsight.data.fps

import com.ivarna.deviceinsight.data.fps.model.FpsMethod
import com.ivarna.deviceinsight.data.fps.model.FpsMode
import com.ivarna.deviceinsight.data.fps.model.FpsSnapshot
import com.ivarna.deviceinsight.data.fps.privilege.PrivilegeTier
import com.ivarna.deviceinsight.data.fps.privilege.ShellGateway
import com.ivarna.deviceinsight.data.fps.source.GfxinfoFpsDataSource
import com.ivarna.deviceinsight.data.fps.source.SurfaceFlingerFpsDataSource
import com.ivarna.deviceinsight.data.fps.util.ForegroundApp
import com.ivarna.deviceinsight.data.fps.util.ForegroundAppResolver
import com.ivarna.deviceinsight.data.monitor.HudSettingsCache
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FpsRepositoryTest {

    private lateinit var surfaceSource: SurfaceFlingerFpsDataSource
    private lateinit var gfxSource: GfxinfoFpsDataSource
    private lateinit var resolver: ForegroundAppResolver
    private lateinit var gateway: ShellGateway
    private lateinit var cache: HudSettingsCache
    private lateinit var repo: FpsRepositoryImpl

    @Before
    fun setUp() {
        surfaceSource = mockk()
        gfxSource = mockk()
        resolver = mockk()
        gateway = mockk(relaxed = true)
        cache = mockk(relaxed = true)
        every { cache.fpsMode } returns FpsMode.AUTO
        repo = FpsRepositoryImpl(surfaceSource, gfxSource, resolver, gateway, cache)
    }

    private fun fg(pkg: String, isGame: Boolean = false) = ForegroundApp(pkg, 1234, 60f).also {
        every { resolver.resolve() } returns it
        every { resolver.isGameLikeSurface(pkg) } returns isGame
    }

    private fun snap(fps: Float, method: FpsMethod, pkg: String? = null) = FpsSnapshot(
        currentFps = fps,
        method = method,
        packageName = pkg,
        frametimes = listOf(16f)
    )

    @Test
    fun game_usesSurfaceFlingerNotGfxinfo() = runBlocking {
        fg("com.example.game", isGame = true)
        coEvery { surfaceSource.readFps() } returns snap(60f, FpsMethod.SURFACEFLINGER, "com.example.game")
        coEvery { gfxSource.readFps() } returns snap(60f, FpsMethod.GFXINFO, "com.example.game")
        val result = repo.getFps()
        assertEquals(FpsMethod.SURFACEFLINGER, result.method)
        // gfxinfo should not be queried for games
        coEvery { surfaceSource.readFps() } returns null
        coEvery { gfxSource.readFps() } returns snap(60f, FpsMethod.GFXINFO, "com.example.game")
        // Create new repo to isolate lastGood? But lastGood holds previous; clear by switching package
        fg("com.example.game2", isGame = true)
        coEvery { surfaceSource.readFps() } returns null
        val result2 = repo.getFps()
        // Should be ZERO (no gfxinfo fallback for games)
        assertEquals(FpsMethod.NONE, result2.method)
        assertEquals(0f, result2.currentFps)
    }

    @Test
    fun ui_fallsBackToGfxinfoWhenSurfaceFlingerFails() = runBlocking {
        fg("com.example.uiapp", isGame = false)
        coEvery { surfaceSource.readFps() } returns null
        coEvery { gfxSource.readFps() } returns snap(55f, FpsMethod.GFXINFO, "com.example.uiapp")
        val result = repo.getFps()
        assertEquals(FpsMethod.GFXINFO, result.method)
        assertTrue(result.currentFps > 0f)
    }

    @Test
    fun holdsLastGoodBrieflyOnTransientFailure() = runBlocking {
        fg("com.example.game", isGame = true)
        coEvery { surfaceSource.readFps() } returns snap(60f, FpsMethod.SURFACEFLINGER, "com.example.game")
        val first = repo.getFps()
        assertFalse(first.isStale)
        assertEquals(60f, first.currentFps)
        // Next poll transient failure — no surface
        coEvery { surfaceSource.readFps() } returns null
        val second = repo.getFps()
        assertTrue(second.isStale)
        assertEquals(60f, second.currentFps)
        assertEquals(FpsMethod.SURFACEFLINGER, second.method)
    }

    @Test
    fun clearsHeldSampleOnPackageChange() = runBlocking {
        fg("com.example.game", isGame = true)
        coEvery { surfaceSource.readFps() } returns snap(60f, FpsMethod.SURFACEFLINGER, "com.example.game")
        repo.getFps()
        // Transient failure would hold, but package changes — for game, no DISPLAY fallback, so NONE
        fg("com.other.game2", isGame = true)
        coEvery { surfaceSource.readFps() } returns null
        coEvery { gfxSource.readFps() } returns null
        val result = repo.getFps()
        assertEquals(FpsMethod.NONE, result.method)
        assertFalse(result.isStale)
        assertEquals(0f, result.currentFps)
    }

    @Test
    fun clearsHeldSampleOnPackageChange_uiFallsBackToRefresh() = runBlocking {
        fg("com.example.game", isGame = true)
        coEvery { surfaceSource.readFps() } returns snap(60f, FpsMethod.SURFACEFLINGER, "com.example.game")
        repo.getFps()
        // Package changes to UI app where SF/GFX blocked -> should fallback to REF (display) not old game FPS
        fg("com.other.app", isGame = false)
        coEvery { surfaceSource.readFps() } returns null
        coEvery { gfxSource.readFps() } returns null
        val result = repo.getFps()
        assertEquals(FpsMethod.DISPLAY, result.method)
        assertFalse(result.isStale)
        assertTrue(result.currentFps in 55f..65f) // refresh 60
    }

    @Test
    fun medianSmoothingOfThreeSamples() = runBlocking {
        // Directly test smoothDisplayFps via repeated getFps calls with varying raw FPS
        fg("com.example.uiapp", isGame = false)
        // First sample 60 -> no smoothing (size<2) => 60
        coEvery { surfaceSource.readFps() } returns snap(60f, FpsMethod.SURFACEFLINGER, "com.example.uiapp")
        coEvery { gfxSource.readFps() } returns null
        assertEquals(60f, repo.getFps().currentFps)

        // Second sample 90 -> median of [60,90] sorted [60,90] -> index 1 => 90
        coEvery { surfaceSource.readFps() } returns snap(90f, FpsMethod.SURFACEFLINGER, "com.example.uiapp")
        assertEquals(90f, repo.getFps().currentFps)

        // Third sample 30 -> window [60,90,30] sorted [30,60,90] median 60
        coEvery { surfaceSource.readFps() } returns snap(30f, FpsMethod.SURFACEFLINGER, "com.example.uiapp")
        val third = repo.getFps()
        assertEquals(60f, third.currentFps)

        // Fourth sample 120 -> window rolls to [90,30,120] sorted [30,90,120] median 90
        coEvery { surfaceSource.readFps() } returns snap(120f, FpsMethod.SURFACEFLINGER, "com.example.uiapp")
        val fourth = repo.getFps()
        assertEquals(90f, fourth.currentFps)
    }

    @Test
    fun sourceChangeClearsSmoothingWindow() = runBlocking {
        fg("com.example.uiapp", isGame = false)
        coEvery { surfaceSource.readFps() } returns snap(60f, FpsMethod.SURFACEFLINGER, "com.example.uiapp")
        coEvery { gfxSource.readFps() } returns null
        repo.getFps()
        coEvery { surfaceSource.readFps() } returns snap(62f, FpsMethod.SURFACEFLINGER, "com.example.uiapp")
        repo.getFps()
        // Now source changes to GFXINFO -> should clear window, next median not polluted
        coEvery { surfaceSource.readFps() } returns null
        coEvery { gfxSource.readFps() } returns snap(30f, FpsMethod.GFXINFO, "com.example.uiapp")
        val gfxFirst = repo.getFps()
        // After clear, single sample => returns raw (no median)
        assertEquals(30f, gfxFirst.currentFps)
    }

    @Test
    fun expiredLastGoodReturnsZero() = runBlocking {
        fg("com.example.game", isGame = true)
        coEvery { surfaceSource.readFps() } returns snap(60f, FpsMethod.SURFACEFLINGER, "com.example.game")
        repo.getFps()
        // Simulate expiry by setting lastGoodAtMs far in past via reflection
        val field = repo::class.java.getDeclaredField("lastGoodAtMs")
        field.isAccessible = true
        field.setLong(repo, System.currentTimeMillis() - FpsRepositoryImpl.LAST_GOOD_HOLD_MS - 1000)
        coEvery { surfaceSource.readFps() } returns null
        val result = repo.getFps()
        assertEquals(FpsMethod.NONE, result.method)
        assertEquals(0f, result.currentFps)
        assertFalse(result.isStale)
    }

    @Test
    fun neverReturnsZeroAsValidMeasurement() = runBlocking {
        fg("com.example.game", isGame = true)
        coEvery { surfaceSource.readFps() } returns snap(0f, FpsMethod.SURFACEFLINGER, "com.example.game")
        val result = repo.getFps()
        assertEquals(FpsMethod.NONE, result.method)
        assertTrue(result.currentFps == 0f || result.method == FpsMethod.NONE)
    }
}
