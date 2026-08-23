package com.ivarna.deviceinsight.ui.caliper.widget

import org.junit.Assert.*
import org.junit.Test

class WidgetReceiversExistTest {

    @Test
    fun receiversExistWithCorrectFqn() {
        val fqns = listOf(
            "com.ivarna.deviceinsight.ui.caliper.widget.SingleChannelWidgetReceiver",
            "com.ivarna.deviceinsight.ui.caliper.widget.DualChannelWidgetReceiver",
            "com.ivarna.deviceinsight.ui.caliper.widget.BenchWidgetReceiver",
            "com.ivarna.deviceinsight.ui.caliper.widget.FuelWidgetReceiver",
            "com.ivarna.deviceinsight.ui.caliper.widget.RasterWidgetReceiver"
        )
        fqns.forEach { fqn ->
            val cls = try { Class.forName(fqn) } catch (e: Exception) { null }
            assertNotNull("Receiver $fqn must exist and keep FQN stable (placements bind)", cls)
        }
    }

    @Test
    fun benchBudgetKeepSemantics() {
        // Verify BenchBudget uses KEEP and 15 minutes
        val f = listOf(
            java.io.File("src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchBudget.kt"),
            java.io.File("app/src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchBudget.kt")
        ).firstOrNull { it.exists() } ?: throw AssertionError("BenchBudget.kt not found")
        val src = f.readText()
        assertTrue("BenchBudget enqueue must use ExistingPeriodicWorkPolicy.KEEP", src.contains("ExistingPeriodicWorkPolicy.KEEP"))
        assertTrue("BenchBudget interval must be 15 MINUTES", src.contains("15") && src.contains("MINUTES"))
        assertTrue("BenchBudget UNIQUE must be bench-budget", src.contains("bench-budget"))
        assertTrue("cancelIfNone must sum all 5 widget kinds", src.contains("ScopeWidget") && src.contains("StackWidget") && src.contains("FuelWidget") && src.contains("RasterWidget") && src.contains("BenchWidgetAll"))
    }

    @Test
    fun previewDrawablesExist() {
        // Guard: 15 preview WEBP must be shipped — robust to cwd (app/ vs root)
        val baseCandidates = listOf(
            java.io.File("src/main/res/drawable-nodpi"),
            java.io.File("app/src/main/res/drawable-nodpi"),
            java.io.File("../app/src/main/res/drawable-nodpi"),
            java.io.File("app/src/main/res/drawable-nodpi"),
            java.io.File("drawable-nodpi")
        )
        val base = baseCandidates.firstOrNull { it.exists() && it.isDirectory }
            ?: throw AssertionError("drawable-nodpi not found, tried $baseCandidates cwd=${java.io.File(".").absolutePath}")
        val kinds = listOf("scope", "stack", "fuel", "raster", "bench")
        val mediums = listOf("paper", "carbon", "blueprint")
        kinds.forEach { kind ->
            mediums.forEach { med ->
                val f = java.io.File(base, "preview_${kind}_${med}.webp")
                assertTrue("Preview $f must exist (base=$base)", f.exists())
            }
        }
        // XML previewImage must not be launcher — find xml dir robustly
        val xmlDirCandidates = listOf(
            java.io.File("src/main/res/xml"),
            java.io.File("app/src/main/res/xml"),
            java.io.File("../app/src/main/res/xml")
        )
        val xmlDir = xmlDirCandidates.firstOrNull { it.exists() } ?: throw AssertionError("xml dir not found")
        val xmlNames = listOf("single_channel_widget_info.xml", "dual_channel_widget_info.xml", "bench_widget_info.xml", "fuel_widget_info.xml", "raster_widget_info.xml")
        xmlNames.forEach { name ->
            val xml = java.io.File(xmlDir, name)
            assertTrue("XML $xml must exist", xml.exists())
            val txt = xml.readText()
            assertFalse("XML $xml must not use @mipmap/ic_launcher, must use @drawable/preview_*", txt.contains("@mipmap/ic_launcher"))
            assertTrue("XML $xml must contain preview drawable", txt.contains("@drawable/preview_"))
            assertTrue("updatePeriodMillis must be 0", txt.contains("updatePeriodMillis=\"0\""))
        }
    }
}
