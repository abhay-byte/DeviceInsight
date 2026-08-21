package com.ivarna.deviceinsight.presentation.hardware

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.presentation.hardware.components.*
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.caliperGrid
import com.ivarna.deviceinsight.ui.caliper.components.*

// № 04 — DEVICE DOSSIER (S-10). Flat SegKey tabs over the existing spec plates.
@Composable
fun HardwareScreen(
    viewModel: HardwareViewModel = hiltViewModel()
) {
    val hardwareInfo by viewModel.hardwareInfo.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("SYSTEM", "CPU", "DISPLAY", "GPU", "NETWORK", "BATTERY", "ANDROID", "HARDWARE", "THERMAL", "STORAGE", "SENSORS")

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("№ 02 — DEVICE DOSSIER", "Device.", "hardware spec sheets · plates")
        if (hardwareInfo == null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                CalibratingIndicator(percent = null)
            }
        } else {
            val info = hardwareInfo!!
            val tabContent: @Composable () -> Unit = {
                when (tab) {
                    0  -> SystemTab(info)
                    1  -> CpuTab(info)
                    2  -> DisplayTab(info)
                    3  -> GpuTab(info)
                    4  -> NetworkTab(info)
                    5  -> BatteryTab(info)
                    6  -> AndroidTab(info)
                    7  -> DevicesTab(info)
                    8  -> ThermalTab(info)
                    9  -> StorageTab(info)
                    10 -> SensorsTab(info)
                }
            }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                // Two-pane dossier (key list + spec sheet, §5.2/§7 S-14). Content
                // width already excludes the 232dp left rail on wide windows.
                val twoPane = maxWidth >= 560.dp
                if (twoPane) {
                    // §5.2/§7 S-14: ≥600dp → two-pane dossier (key list + spec sheet).
                    Row(Modifier.fillMaxSize()) {
                        Column(
                            Modifier.width(200.dp).fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)
                        ) {
                            tabs.forEachIndexed { i, t ->
                                val sel = i == tab
                                Text(
                                    t,
                                    style = Caliper.type.meta.copy(fontSize = 12.sp),
                                    color = if (sel) Caliper.colors.surface else Caliper.colors.ink,
                                    modifier = Modifier.fillMaxWidth()
                                        .background(if (sel) Caliper.colors.ink else Caliper.colors.panel)
                                        .border(1.dp, Caliper.colors.hairline)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { tab = i }
                                        .padding(horizontal = 12.dp, vertical = 12.dp)
                                )
                            }
                        }
                        Column(
                            Modifier.weight(1f).fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            tabContent()
                            Spacer(Modifier.height(24.dp))
                            EndOfSheet()
                        }
                    }
                } else {
                    val scroll = rememberScrollState()
                    Column(Modifier.fillMaxSize()) {
                        // B3: narrow phone tab strip — horizontal scroll, CALIPER style
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(scroll)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tabs.forEachIndexed { i, t ->
                                val sel = i == tab
                                Text(
                                    t,
                                    style = Caliper.type.meta.copy(fontSize = 10.sp),
                                    color = if (sel) Caliper.colors.surface else Caliper.colors.ink,
                                    modifier = Modifier
                                        .background(if (sel) Caliper.colors.ink else Caliper.colors.panel)
                                        .border(1.dp, if (sel) Caliper.colors.ink else Caliper.colors.hairline)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { tab = i }
                                        .semantics { role = Role.Tab }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                        DoubleRule(Modifier.padding(horizontal = 16.dp))
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            tabContent()
                            Spacer(Modifier.height(24.dp))
                            EndOfSheet()
                        }
                    }
                }
            }
        }
    }
}