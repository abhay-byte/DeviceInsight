package com.ivarna.deviceinsight.presentation.hardware

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.presentation.hardware.components.*
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.caliperGrid
import com.ivarna.deviceinsight.ui.caliper.components.*

// № 02 — DEVICE DOSSIER (S-10). Flat SegKey tabs over the existing spec plates.
@Composable
fun HardwareScreen(
    viewModel: HardwareViewModel = hiltViewModel(),
    initialTab: Int? = null
) {
    val hardwareInfo by viewModel.hardwareInfo.collectAsStateWithLifecycle()
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, ev ->
            if (ev == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.loadHardwareInfo()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    var tab by rememberSaveable { mutableIntStateOf(initialTab ?: 0) }
    val tabs = listOf("SYSTEM", "CPU", "DISPLAY", "GPU", "NETWORK", "BATTERY", "ANDROID", "HARDWARE", "THERMAL", "STORAGE", "SENSORS")
    val tabRange = tabs.indices
    LaunchedEffect(initialTab) { if (initialTab != null && initialTab in tabRange) tab = initialTab }

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
                                        .semantics {
                                            role = Role.Tab
                                            selected = sel
                                        }
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
                    val scroll = rememberLazyListState()
                    val pagerState = rememberPagerState(pageCount = { tabs.size })
                    // tabs are the only switch this pass — swipe stays off; jump without animation loops
                    LaunchedEffect(tab) {
                        if (tab in tabRange && !scroll.isScrollInProgress) {
                            scroll.scrollToItem(tab)
                        }
                        if (pagerState.currentPage != tab) pagerState.scrollToPage(tab)
                    }
                    Column(Modifier.fillMaxSize()) {
                        // B3: narrow phone tab strip — horizontal scroll, CALIPER style
                        LazyRow(
                            state = scroll,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(tabs, key = { i, _ -> i }) { i, t ->
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
                                        .semantics {
                                            role = Role.Tab
                                            selected = sel
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                        DoubleRule(Modifier.padding(horizontal = 16.dp))
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = false,
                            beyondViewportPageCount = 0,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            Column(
                                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                when (page) {
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
                                Spacer(Modifier.height(24.dp))
                                EndOfSheet()
                            }
                        }
                    }
                }
            }
        }
    }
}