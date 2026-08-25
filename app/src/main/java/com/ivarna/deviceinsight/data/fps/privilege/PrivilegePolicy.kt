package com.ivarna.deviceinsight.data.fps.privilege

/**
 * Resolves the tier chain for a metric based on current [PrivilegeMode].
 * Fail-closed: forced modes never silently demote.
 */
class PrivilegePolicy(private val mode: PrivilegeMode) {

    fun chain(default: List<PrivilegeTier>): List<PrivilegeTier> = when (mode) {
        PrivilegeMode.AUTO -> default
        PrivilegeMode.ROOT -> listOf(PrivilegeTier.ROOT)
        PrivilegeMode.SHIZUKU -> listOf(PrivilegeTier.SHIZUKU)
        PrivilegeMode.STANDARD -> listOf(PrivilegeTier.STANDARD)
    }

    companion object {
        val DEFAULT_CHAIN = listOf(PrivilegeTier.ROOT, PrivilegeTier.SHIZUKU, PrivilegeTier.STANDARD)
    }
}
