package com.ivarna.deviceinsight.data.fps.util

import com.ivarna.deviceinsight.data.fps.privilege.PrivilegeMode
import com.ivarna.deviceinsight.data.fps.privilege.PrivilegePolicy
import com.ivarna.deviceinsight.data.fps.privilege.PrivilegeTier
import com.ivarna.deviceinsight.data.fps.privilege.ShellGateway
import com.ivarna.deviceinsight.data.fps.privilege.ShizukuAccess
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class ForegroundAppResolverTest {

    // Test extractPackage directly via resolver's internal method by calling via reflection or testing resolver's parsing
    // We'll create a resolver with mocked gateway that returns controlled dumpsys outputs.

    private fun createResolver(
        dumpsysWindowOutput: String,
        dumpsysActivityOutput: String = "",
        packageListOutput: String = ""
    ): ForegroundAppResolver {
        val shellGateway = mockk<ShellGateway>(relaxed = true)
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.packageName } returns "com.ivarna.deviceinsight"

        every {
            shellGateway.executePolicy(any(), any())
        } answers {
            val cmd = firstArg<String>()
            when {
                cmd.contains("pidof") -> com.ivarna.deviceinsight.data.fps.util.ShellResult("1234", 0) to PrivilegeTier.STANDARD
                cmd.contains("dumpsys window 2>/dev/null | grep -E") -> com.ivarna.deviceinsight.data.fps.util.ShellResult(dumpsysWindowOutput, 0) to PrivilegeTier.STANDARD
                cmd.contains("dumpsys activity activities") -> com.ivarna.deviceinsight.data.fps.util.ShellResult(dumpsysActivityOutput, 0) to PrivilegeTier.STANDARD
                cmd == "dumpsys SurfaceFlinger --list 2>/dev/null" -> com.ivarna.deviceinsight.data.fps.util.ShellResult(packageListOutput, 0) to PrivilegeTier.STANDARD
                cmd.contains("dumpsys window 2>/dev/null | grep mCurrentFocus") && !cmd.contains("grep -E") -> com.ivarna.deviceinsight.data.fps.util.ShellResult(dumpsysWindowOutput, 0) to PrivilegeTier.STANDARD
                cmd.contains("dumpsys display") -> com.ivarna.deviceinsight.data.fps.util.ShellResult("mActiveRenderFrameRate=60.0", 0) to PrivilegeTier.STANDARD
                else -> com.ivarna.deviceinsight.data.fps.util.ShellResult("", 0) to PrivilegeTier.STANDARD
            }
        }

        val resolver = ForegroundAppResolver(shellGateway, context)
        return resolver
    }

    @Test
    fun extractPackage_u0Variant() {
        val resolver = createResolver("")
        val line = "  mCurrentFocus=Window{abc u0 com.example.game/com.example.GameActivity}"
        val pkg = resolver.extractPackage(line)
        assertEquals("com.example.game", pkg)
    }

    @Test
    fun extractPackage_braceVariant() {
        val resolver = createResolver("")
        val line = "  * ActivityRecord{abc u0 com.example.app/.MainActivity t123}"
        val pkg = resolver.extractPackage(line)
        assertEquals("com.example.app", pkg)
    }

    @Test
    fun resolve_ignoresNullFocus() {
        val dumpsysWindow = """
            mCurrentFocus=Window{abc u0 null}
            mFocusedApp=AppWindowToken{def u0 com.example.game/com.example.GameActivity}
        """.trimIndent()
        val resolver = createResolver(dumpsysWindow)
        val app = resolver.resolve()
        assertNotNull(app)
        assertEquals("com.example.game", app!!.packageName)
        assertNotEquals("com.ivarna.deviceinsight", app.packageName)
    }

    @Test
    fun resolve_neverSelectSelf() {
        val dumpsysWindow = """
            mCurrentFocus=Window{abc u0 com.ivarna.deviceinsight/com.ivarna.deviceinsight.MainActivity}
            mFocusedApp=AppWindowToken{def u0 com.example.game/com.example.GameActivity}
        """.trimIndent()
        val resolver = createResolver(dumpsysWindow)
        val app = resolver.resolve()
        assertNotNull(app)
        // Should skip self and pick next valid
        assertEquals("com.example.game", app!!.packageName)
    }

    @Test
    fun isGameLike_detectsSurfaceView() {
        val resolver = createResolver(
            dumpsysWindowOutput = "mCurrentFocus=Window{abc u0 com.example.game/com.example.GameActivity}",
            packageListOutput = "SurfaceView[com.example.game/com.example.GameActivity]#0\ncom.example.game/com.example.GameActivity#1"
        )
        assertTrue(resolver.isGameLikeSurface("com.example.game"))
    }

    @Test
    fun isGameLike_rejectsPlainBlast() {
        // BLAST alone should not classify as game (normal UI uses BLAST)
        val resolver = createResolver(
            dumpsysWindowOutput = "mCurrentFocus=Window{abc u0 com.android.settings/com.android.settings.Settings}",
            packageListOutput = "BLAST#123 com.android.settings\ncom.android.settings/com.android.settings.Settings#0"
        )
        // Should be false because only BLAST marker, not SurfaceView/Vulkan etc, and not known game package
        assertFalse(resolver.isGameLikeSurface("com.android.settings"))
    }

    @Test
    fun resolve_viaResumedActivityFallback() {
        val shellGateway = mockk<ShellGateway>(relaxed = true)
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.packageName } returns "com.ivarna.deviceinsight"
        every {
            shellGateway.executePolicy(any(), any())
        } answers {
            val cmd = firstArg<String>()
            when {
                cmd.contains("dumpsys window 2>/dev/null | grep -E") -> ShellResult("", 0) to PrivilegeTier.STANDARD
                cmd.contains("dumpsys activity activities") -> ShellResult("  ResumedActivity: ActivityRecord{abc u0 com.example.fallback/.MainActivity t123}", 0) to PrivilegeTier.STANDARD
                cmd.contains("pidof") -> ShellResult("1234", 0) to PrivilegeTier.STANDARD
                cmd.contains("dumpsys display") -> ShellResult("mActiveRenderFrameRate=90.0", 0) to PrivilegeTier.STANDARD
                else -> ShellResult("", 0) to PrivilegeTier.STANDARD
            }
        }
        val resolver = ForegroundAppResolver(shellGateway, context)
        val app = resolver.resolve()
        assertNotNull(app)
        assertEquals("com.example.fallback", app!!.packageName)
    }

    @Test
    fun resolve_handlesMResumedActivity() {
        val shellGateway = mockk<ShellGateway>(relaxed = true)
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.packageName } returns "com.ivarna.deviceinsight"
        every {
            shellGateway.executePolicy(any(), any())
        } answers {
            val cmd = firstArg<String>()
            when {
                cmd.contains("dumpsys window 2>/dev/null | grep -E") -> ShellResult("", 0) to PrivilegeTier.STANDARD
                cmd.contains("dumpsys activity activities") -> ShellResult("    mResumedActivity: ActivityRecord{abc u0 com.example.mresumed/.MainActivity}", 0) to PrivilegeTier.STANDARD
                cmd.contains("pidof") -> ShellResult("9999", 0) to PrivilegeTier.STANDARD
                cmd.contains("dumpsys display") -> ShellResult("mActiveRenderFrameRate=60.0", 0) to PrivilegeTier.STANDARD
                else -> ShellResult("", 0) to PrivilegeTier.STANDARD
            }
        }
        val resolver = ForegroundAppResolver(shellGateway, context)
        val app = resolver.resolve()
        assertNotNull(app)
        assertEquals("com.example.mresumed", app!!.packageName)
    }

    @Test
    fun resolve_nullFocusFollowedByValidFocus() {
        val dumpsysWindow = """
            mCurrentFocus=Window{abc u0 com.ivarna.deviceinsight/com.ivarna.deviceinsight.MainActivity}
            mFocusedApp=Window{def u0 com.valid.app/com.valid.app.MainActivity}
            mCurrentFocus=Window{ghi u0 com.valid.app/com.valid.app.MainActivity}
        """.trimIndent()
        // First line is self -> should skip, second valid -> pick com.valid.app
        val resolver = createResolver(dumpsysWindow)
        val app = resolver.resolve()
        assertNotNull(app)
        assertEquals("com.valid.app", app!!.packageName)
    }

    @Test
    fun extractPackage_handlesNullString() {
        val resolver = createResolver("")
        assertNull(resolver.extractPackage("  mCurrentFocus=Window{abc u0 null}"))
        assertNull(resolver.extractPackage("  mFocusedApp=null"))
    }
}
