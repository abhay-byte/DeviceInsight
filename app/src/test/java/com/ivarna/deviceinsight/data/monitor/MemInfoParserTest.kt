package com.ivarna.deviceinsight.data.monitor

import com.ivarna.deviceinsight.ui.caliper.HatchPattern
import org.junit.Assert.*
import org.junit.Test

class MemInfoParserTest {

    // Helper to build meminfo string with given kB values
    private fun meminfo(
        memTotalKb: Long,
        activeKb: Long? = null,
        activeAnonKb: Long? = null,
        cachedKb: Long = 0,
        swapTotalKb: Long = 0,
        swapFreeKb: Long = 0,
        memAvailableKb: Long? = null
    ): String = buildString {
        appendLine("MemTotal:       $memTotalKb kB")
        activeKb?.let { appendLine("Active:         $it kB") }
        activeAnonKb?.let { appendLine("Active(anon):   $it kB") }
        appendLine("Cached:         $cachedKb kB")
        appendLine("SwapTotal:      $swapTotalKb kB")
        appendLine("SwapFree:       $swapFreeKb kB")
        memAvailableKb?.let { appendLine("MemAvailable:   $it kB") }
        appendLine("MemFree:        123456 kB")
    }

    @Test
    fun parseNormal_sumIsOne_noNegative() {
        val totalKb = 8L * 1024 * 1024 // 8GB
        val activeKb = 2L * 1024 * 1024 // 2GB
        val cachedKb = 1L * 1024 * 1024 // 1GB
        val swapTotalKb = 2L * 1024 * 1024
        val swapFreeKb = 1L * 1024 * 1024 // used 1GB
        val zramBytes = 512L * 1024 * 1024 // 0.5GB
        val meminfoStr = meminfo(totalKb, activeKb, cachedKb = cachedKb, swapTotalKb = swapTotalKb, swapFreeKb = swapFreeKb)
        val comp = MemInfoParser.parse(meminfoStr, zramBytes)
        val sum = comp.segs.sumOf { it.fraction.toDouble() }.toFloat()
        assertEquals(1f, sum, 0.02f)
        assertTrue(comp.segs.all { it.fraction >= 0f })
        // hatchBar x+segW <= w check: fractions shouldn't exceed 1, and HatchBar coerce handled but parser should keep sum ~1
        // verify order: active SOLID CH-02 first
        assertEquals(HatchPattern.SOLID, comp.segs[0].pattern)
        assertEquals("CH-02", comp.segs[0].channelId)
    }

    @Test
    fun parse_dedupZramFromSwap() {
        val totalKb = 8000000L
        val activeKb = 1000000L
        val cachedKb = 500000L
        val swapTotalKb = 2000000L
        val swapFreeKb = 1000000L // swapUsed 1GB
        val zramBytes = 800L * 1024 * 1024 // ~0.8GB, less than swapUsed, swapF = swapRaw - zram
        val meminfoStr = meminfo(totalKb, activeKb, cachedKb = cachedKb, swapTotalKb = swapTotalKb, swapFreeKb = swapFreeKb)
        val zramF = zramBytes.toFloat() / (totalKb * 1024).toFloat()
        val swapRawF = (swapTotalKb - swapFreeKb).toFloat() / totalKb.toFloat()
        val expectedSwapF = kotlin.math.max(0f, swapRawF - zramF)
        val comp = MemInfoParser.parse(meminfoStr, zramBytes)
        // When zram present, swap bar omitted; swap lives as subline, so segs should not contain swap as bar segment
        // segs: active, cached, zram, free (4)
        assertEquals(4, comp.segs.size)
        // zram seg should be present
        assertTrue(comp.segs.any { it.pattern == HatchPattern.CROSS && it.channelId == "CH-04" && it.fraction > 0.001f })
        // free should be last with NONE pattern
        assertEquals(HatchPattern.NONE, comp.segs.last().pattern)
    }

    @Test
    fun parse_swapWithoutZram_includesSwapBar() {
        val totalKb = 8000000L
        val activeKb = 1000000L
        val cachedKb = 500000L
        val swapTotalKb = 2000000L
        val swapFreeKb = 1000000L
        val meminfoStr = meminfo(totalKb, activeKb, cachedKb = cachedKb, swapTotalKb = swapTotalKb, swapFreeKb = swapFreeKb)
        val comp = MemInfoParser.parse(meminfoStr, null)
        // swap bar only when zramF==0 && swapF>0
        val hasSwapBar = comp.segs.count { it.pattern == HatchPattern.CROSS } == 1 // only swap if no zram? Actually swap cross when no zram
        // With zram null, there is no zram seg, swap should be present as CROSS
        assertTrue(comp.segs.any { it.pattern == HatchPattern.CROSS && it.fraction > 0.001f })
        // sum still ~1
        val sum = comp.segs.sumOf { it.fraction.toDouble() }.toFloat()
        assertEquals(1f, sum, 0.02f)
    }

    @Test
    fun parse_activeMissing_fallbackUsesMemAvailable() {
        val totalKb = 8000000L
        val cachedKb = 1000000L
        val memAvailableKb = 3000000L
        // No Active, should compute fallbackActive = (total - avail - cached - zram)/total
        val meminfoStr = meminfo(totalKb, activeKb = null, cachedKb = cachedKb, memAvailableKb = memAvailableKb)
        val comp = MemInfoParser.parse(meminfoStr, null)
        val expectedActive = (totalKb - memAvailableKb - cachedKb).toFloat() / totalKb.toFloat()
        assertEquals(expectedActive, comp.segs[0].fraction, 0.02f)
        val sum = comp.segs.sumOf { it.fraction.toDouble() }.toFloat()
        assertEquals(1f, sum, 0.02f)
    }

    @Test
    fun parse_normalizeWhenSumGreaterThanOne() {
        // Force sum >1 by using overlapping Active+Cached large
        val totalKb = 1000000L
        val activeKb = 700000L
        val cachedKb = 400000L // + active 0.7+0.4=1.1 already >1 before swap etc
        val meminfoStr = meminfo(totalKb, activeKb, cachedKb = cachedKb)
        val comp = MemInfoParser.parse(meminfoStr, null)
        val sum = comp.segs.sumOf { it.fraction.toDouble() }.toFloat()
        assertEquals(1f, sum, 0.02f)
        // All fractions scaled down
        assertTrue(comp.segs.all { it.fraction <= 1f })
        assertTrue(comp.segs[0].fraction < 0.7f) // normalized down from 0.7
    }

    @Test
    fun parse_missingMemTotal_fallbackSingleSolid() {
        val meminfoStr = "Active: 1000 kB\nCached: 500 kB\nSwapTotal: 1000 kB\n"
        val comp = MemInfoParser.parse(meminfoStr, null)
        // fallback: single SOLID used segment + NONE free
        assertEquals(2, comp.segs.size)
        assertEquals(HatchPattern.SOLID, comp.segs[0].pattern)
        assertEquals(HatchPattern.NONE, comp.segs[1].pattern)
    }

    @Test
    fun parse_freeNeverNegative() {
        val totalKb = 1000000L
        val activeKb = 900000L
        val cachedKb = 80000L
        val zramBytes = 50000L * 1024 // 50MB
        val swapTotalKb = 1000000L
        val swapFreeKb = 0L // swap used 1GB => swapRawF ~1.0, zram dedup => swapF ~0.95
        val meminfoStr = meminfo(totalKb, activeKb, cachedKb = cachedKb, swapTotalKb = swapTotalKb, swapFreeKb = swapFreeKb)
        val comp = MemInfoParser.parse(meminfoStr, zramBytes)
        assertTrue(comp.segs.all { it.fraction >= 0f })
        val sum = comp.segs.sumOf { it.fraction.toDouble() }.toFloat()
        assertTrue(sum in 0.98f..1.02f)
    }

    @Test
    fun parse_zramZero_swapDedupEdge() {
        val totalKb = 8000000L
        val activeKb = 2000000L
        val meminfoStr = meminfo(totalKb, activeKb, swapTotalKb = 0, swapFreeKb = 0)
        val comp = MemInfoParser.parse(meminfoStr, 0L)
        // zram 0 => should treat as 0, swap 0 => only active, cached, free
        // swapF =0, zF=0, so swap bar omitted
        assertFalse(comp.segs.any { it.pattern == HatchPattern.CROSS && it.fraction > 0.001f && it.channelId == "CH-04" && comp.segs.indexOf(it) != 1 })
        // But also ensure no crash and sum 1
        val sum = comp.segs.sumOf { it.fraction.toDouble() }.toFloat()
        assertEquals(1f, sum, 0.02f)
    }
}
