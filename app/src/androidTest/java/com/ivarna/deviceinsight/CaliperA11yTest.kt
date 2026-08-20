package com.ivarna.deviceinsight

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.ivarna.deviceinsight.ui.caliper.CaliperTheme
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.ui.caliper.components.Masthead
import com.ivarna.deviceinsight.ui.caliper.components.ModeRail
import com.ivarna.deviceinsight.ui.caliper.components.RailKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * m5: TalkBack order must match visual nav order, and the Masthead gear must be
 * a reachable, announced button that navigates to Settings.
 * Instrumented — requires an emulator/device (no-op on JVM unit runs).
 */
class CaliperA11yTest {

    @get:Rule
    val rule = createComposeRule()

    private val pinnedKeys = listOf(
        RailKey(1, "OVERVIEW"),
        RailKey(2, "DEVICE"),
        RailKey(3, "OVERLAY"),
        RailKey(4, "PROCESSES"),
    )

    @Test
    fun modeRailTalkBackOrderMatchesVisualOrder() {
        rule.setContent {
            CaliperTheme(Medium.PAPER) {
                ModeRail(keys = pinnedKeys, selected = 4, onSelect = {})
            }
        }
        rule.onAllNodes(
            hasContentDescription("[1] OVERVIEW") or
                hasContentDescription("[2] DEVICE") or
                hasContentDescription("[3] OVERLAY") or
                hasContentDescription("[4] PROCESSES")
        ).assertCountEquals(4)

        val descriptions = mutableListOf<String>()
        collectContentDescriptions(rule.onRoot().fetchSemanticsNode(), descriptions)
        assertEquals(
            "TalkBack order must match visual rail order — Tasks last",
            listOf("[1] OVERVIEW", "[2] DEVICE", "[3] OVERLAY", "[4] PROCESSES"),
            descriptions
        )
    }

    @Test
    fun mastheadGearAnnouncesAndNavigatesToSettings() {
        var settingsClicked = false
        rule.setContent {
            CaliperTheme(Medium.PAPER) {
                Masthead(onSettingsClick = { settingsClicked = true })
            }
        }
        rule.onNodeWithContentDescription("Settings").assertIsDisplayed().performClick()
        rule.runOnIdle { assertTrue("gear HardKey must invoke onSettingsClick", settingsClicked) }
    }

    private fun collectContentDescriptions(node: SemanticsNode, out: MutableList<String>) {
        node.config.getOrNull(SemanticsProperties.ContentDescription)?.let { out.addAll(it) }
        node.children.forEach { collectContentDescriptions(it, out) }
    }
}