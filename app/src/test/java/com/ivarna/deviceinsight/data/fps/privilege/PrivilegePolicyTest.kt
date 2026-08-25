package com.ivarna.deviceinsight.data.fps.privilege

import org.junit.Assert.*
import org.junit.Test

class PrivilegePolicyTest {

    @Test
    fun auto_triesRootThenShizukuThenStandard() {
        val policy = PrivilegePolicy(PrivilegeMode.AUTO)
        val chain = policy.chain(PrivilegePolicy.DEFAULT_CHAIN)
        assertEquals(listOf(PrivilegeTier.ROOT, PrivilegeTier.SHIZUKU, PrivilegeTier.STANDARD), chain)
    }

    @Test
    fun root_onlyRoot() {
        val policy = PrivilegePolicy(PrivilegeMode.ROOT)
        val chain = policy.chain(PrivilegePolicy.DEFAULT_CHAIN)
        assertEquals(listOf(PrivilegeTier.ROOT), chain)
    }

    @Test
    fun shizuku_onlyShizuku() {
        val policy = PrivilegePolicy(PrivilegeMode.SHIZUKU)
        val chain = policy.chain(PrivilegePolicy.DEFAULT_CHAIN)
        assertEquals(listOf(PrivilegeTier.SHIZUKU), chain)
    }

    @Test
    fun standard_onlyStandard() {
        val policy = PrivilegePolicy(PrivilegeMode.STANDARD)
        val chain = policy.chain(PrivilegePolicy.DEFAULT_CHAIN)
        assertEquals(listOf(PrivilegeTier.STANDARD), chain)
    }

    @Test
    fun auto_doesNotBlockWhenShizukuUnGranted() {
        // Simulate gateway behavior: canShizuku false, canRoot true
        // AUTO chain includes all, so fallback to ROOT should be possible
        val policy = PrivilegePolicy(PrivilegeMode.AUTO)
        val chain = policy.chain(listOf(PrivilegeTier.SHIZUKU, PrivilegeTier.ROOT))
        // With our ShellGateway execution, ROOT would be tried after SHIZUKU fails
        assertTrue(chain.contains(PrivilegeTier.ROOT))
        assertTrue(chain.contains(PrivilegeTier.SHIZUKU))
    }

    @Test
    fun forcedShizuku_neverFallsBackToRoot() {
        val policy = PrivilegePolicy(PrivilegeMode.SHIZUKU)
        val chain = policy.chain(PrivilegePolicy.DEFAULT_CHAIN)
        assertFalse(chain.contains(PrivilegeTier.ROOT))
        assertTrue(chain.contains(PrivilegeTier.SHIZUKU))
    }

    @Test
    fun forcedRoot_neverFallsBackToShizuku() {
        val policy = PrivilegePolicy(PrivilegeMode.ROOT)
        val chain = policy.chain(PrivilegePolicy.DEFAULT_CHAIN)
        assertFalse(chain.contains(PrivilegeTier.SHIZUKU))
        assertTrue(chain.contains(PrivilegeTier.ROOT))
    }
}
