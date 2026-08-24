package com.ivarna.deviceinsight.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.ivarna.deviceinsight.ui.caliper.Medium
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ivarna.deviceinsight.presentation.calibration.CalibrationScreen
import com.ivarna.deviceinsight.presentation.dashboard.DashboardScreen
import com.ivarna.deviceinsight.presentation.dashboard.channels.GpuChannel
import com.ivarna.deviceinsight.presentation.dashboard.channels.MemoryChannel
import com.ivarna.deviceinsight.presentation.dashboard.channels.NetworkChannel
import com.ivarna.deviceinsight.presentation.dashboard.channels.PowerChannel
import com.ivarna.deviceinsight.presentation.dashboard.channels.ProcessorChannel
import com.ivarna.deviceinsight.presentation.dashboard.channels.StorageChannel
import com.ivarna.deviceinsight.presentation.hardware.HardwareScreen
import com.ivarna.deviceinsight.presentation.overlay.OverlayScreen
import com.ivarna.deviceinsight.presentation.settings.SettingsScreen
import com.ivarna.deviceinsight.presentation.settings.SettingsViewModel
import com.ivarna.deviceinsight.presentation.tasks.TasksScreen
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.CaliperMotion
import com.ivarna.deviceinsight.ui.caliper.caliperGrid
import com.ivarna.deviceinsight.ui.caliper.caliperMigratedFlow
import com.ivarna.deviceinsight.ui.caliper.markCaliperMigrated
import com.ivarna.deviceinsight.ui.caliper.components.MarginNote
import com.ivarna.deviceinsight.ui.caliper.components.Masthead
import com.ivarna.deviceinsight.ui.caliper.components.ModeRail
import com.ivarna.deviceinsight.ui.caliper.components.RailKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Nav order — PINNED (caliper-001 m2). Task: Tasks (Application Active /
 * PROCESSES) must be the LAST key. Deviation from design §5.2/§6 IA
 * (OVERVIEW/ACTIVITY/PROCESSES/DEVICE) is intentional: minimal 4-key diff.
 * [1] OVERVIEW (Dashboard) · [2] DEVICE (Hardware) · [3] OVERLAY · [4] PROCESSES (Tasks)
 */
private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_HARDWARE = "hardware"
private const val ROUTE_OVERLAY = "overlay"
private const val ROUTE_TASKS = "tasks"

internal sealed class ScreenRoute(val route: String, val number: Int, val label: String) {
    data object Dashboard : ScreenRoute(ROUTE_DASHBOARD, 1, "OVERVIEW")
    data object Hardware : ScreenRoute(ROUTE_HARDWARE, 2, "DEVICE")
    data object Overlay : ScreenRoute(ROUTE_OVERLAY, 3, "OVERLAY")
    data object Tasks : ScreenRoute(ROUTE_TASKS, 4, "PROCESSES")
}

// internal (not private) so [caliperRailOrder] is unit-testable (m5).
internal val railRoutes = listOf(
    ScreenRoute.Dashboard,
    ScreenRoute.Hardware,
    ScreenRoute.Overlay,
    ScreenRoute.Tasks,
)

/** Pinned visual+TalkBack order: [1] OVERVIEW · [2] DEVICE · [3] OVERLAY · [4] PROCESSES (Tasks last). */
internal val caliperRailOrder: List<Pair<Int, String>> = railRoutes.map { it.number to it.label }

// ≥600dp = WindowWidthSizeClass.Medium/Expanded per §5.2 — switches ModeRail to a
// left rail. BoxWithConstraints gate keeps the same threshold without a new dep.
private const val WIDE_MIN_DP = 600

@Composable
fun SystemStatsApp(initialRoute: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // SETTINGS is a frontmost full-bleed sheet, not a NavHost destination: the
    // Overview below stays composed, so the back reveal is a pure transform
    // (no dashboard rebuild → no dropped frames mid-transition).
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val onSettings = { showSettings = true }
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val currentMedium by settingsViewModel.medium.collectAsStateWithLifecycle()
    var hardwareTab by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(initialRoute) {
        when (initialRoute) {
            "overview" -> navController.navigate(ScreenRoute.Dashboard.route) { launchSingleTop = true }
            // BENCH tile taps land on the matching channel page (both NavHosts serve these routes)
            "CH-01", "processor" -> navController.navigate("processor") { launchSingleTop = true }
            "CH-02" -> navController.navigate("memory") { launchSingleTop = true }
            "CH-03" -> navController.navigate("network") { launchSingleTop = true }
            "CH-04" -> navController.navigate("power") { launchSingleTop = true }
            "CH-05" -> navController.navigate("storage") { launchSingleTop = true }
            "CH-06" -> navController.navigate("gpu") { launchSingleTop = true }
            "processes" -> navController.navigate(ScreenRoute.Tasks.route) { launchSingleTop = true }
            "calibrate" -> navController.navigate(ScreenRoute.Overlay.route) { launchSingleTop = true }
            "hud-config" -> navController.navigate(ScreenRoute.Overlay.route) { launchSingleTop = true }
            else -> if (initialRoute?.startsWith("dossier:") == true) navController.navigate(ScreenRoute.Tasks.route) { launchSingleTop = true }
        }
    }

    val selectedRail = railRoutes.firstOrNull { route ->
        currentDestination?.hierarchy?.any { it.route == route.route } == true
    }?.number

    // B2: SETTINGS owns its own ← BACK HardKey; the sheet covers the chrome,
    // so the masthead and ModeRail step aside while it is frontmost.
    val isSettings = showSettings

    // B1: light status bars on Paper so clock/ink visible on light surface
    val view = LocalView.current
    val useDarkIcons = currentMedium == Medium.PAPER
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkIcons
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = useDarkIcons
    }

    // S-00 first-launch calibration gate (skippable). After finishing, the
    // one-time "recalibrated" MarginNote is shown via caliperMigrated flag.
    val context = LocalContext.current
    val migrated by context.caliperMigratedFlow.collectAsStateWithLifecycle(initialValue = true)
    var showCalibration by remember { mutableStateOf(!migrated) }
    var showMigratedNote by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showCalibration) {
        Column(Modifier.fillMaxSize().background(Caliper.colors.surface)) {
            Masthead()
            CalibrationScreen(
                initialMedium = currentMedium,
                onMedium = settingsViewModel::setMedium,
                onFinish = {
                    scope.launch { context.markCaliperMigrated() }
                    showCalibration = false
                    showMigratedNote = !migrated
                }
            )
        }
        return
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Caliper.colors.surface)
    ) {
        // M2: ≥600dp → left instrument rail; Processes/Device screens go two-pane.
        val wide = maxWidth >= WIDE_MIN_DP.dp

        val rail = @Composable {
            ModeRail(
                keys = railRoutes.map { RailKey(it.number, it.label) },
                selected = selectedRail ?: 0,
                vertical = wide,
                onSelect = { key ->
                    val route = railRoutes.first { it.number == key.number }
                    if (currentDestination?.hierarchy?.any { it.route == route.route } != true) {
                        navController.navigate(route.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }

        if (wide) {
            Row(Modifier.fillMaxSize()) {
                if (!isSettings) rail()
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    if (!isSettings) {
                        Masthead(
                            onSettingsClick = onSettings
                            // degraded/rootVerified wired by each screen's ViewModel as needed
                        )
                    }
                    if (showMigratedNote && !isSettings) {
                        MarginNote(
                            message = "Your instrument has been recalibrated to the CALIPER standard.",
                            title = "NOTE 001",
                            onDismiss = { showMigratedNote = false }
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .caliperGrid()
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = ScreenRoute.Dashboard.route,
                            enterTransition = {
                                fadeIn(tween(160)) + slideInVertically(tween(160)) { it / 8 }
                            },
                            exitTransition = { fadeOut(tween(160)) },
                            popEnterTransition = { fadeIn(tween(160)) },
                            popExitTransition = { fadeOut(tween(160)) }
                        ) {
                            composable(ScreenRoute.Dashboard.route) {
                                DashboardScreen(onChannel = { route ->
                                    navController.navigate(route) { launchSingleTop = true }
                                })
                            }
                            composable(ScreenRoute.Hardware.route) { HardwareScreen(initialTab = hardwareTab) }
                            composable(ScreenRoute.Overlay.route) { OverlayScreen() }
                            composable(ScreenRoute.Tasks.route) { TasksScreen() }
                            composable("processor") { ProcessorChannel(onBack = { navController.popBackStack() }) }
                            composable("memory") {
                                MemoryChannel(
                                    onBack = { navController.popBackStack() },
                                    onTasks = { navController.navigate(ScreenRoute.Tasks.route) { launchSingleTop = true } }
                                )
                            }
                            composable("network") { NetworkChannel(onBack = { navController.popBackStack() }) }
                            composable("power") { PowerChannel(onBack = { navController.popBackStack() }) }
                            composable("storage") { StorageChannel(onBack = { navController.popBackStack() }) }
                            composable("gpu") { GpuChannel(onBack = { navController.popBackStack() }) }
                        }
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                if (!isSettings) {
                    Masthead(
                        onSettingsClick = onSettings
                        // degraded/rootVerified wired by each screen's ViewModel as needed
                    )
                }
                if (showMigratedNote && !isSettings) {
                    MarginNote(
                        message = "Your instrument has been recalibrated to the CALIPER standard.",
                        title = "NOTE 001",
                        onDismiss = { showMigratedNote = false }
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .caliperGrid()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ScreenRoute.Dashboard.route,
                        enterTransition = {
                            fadeIn(tween(160)) + slideInVertically(tween(160)) { it / 8 }
                        },
                        exitTransition = { fadeOut(tween(160)) },
                        popEnterTransition = { fadeIn(tween(160)) },
                        popExitTransition = { fadeOut(tween(160)) }
                    ) {
                        composable(ScreenRoute.Dashboard.route) {
                            DashboardScreen(onChannel = { route ->
                                navController.navigate(route) { launchSingleTop = true }
                            })
                        }
                        composable(ScreenRoute.Hardware.route) { HardwareScreen(initialTab = hardwareTab) }
                        composable(ScreenRoute.Overlay.route) { OverlayScreen() }
                        composable(ScreenRoute.Tasks.route) { TasksScreen() }
                        composable("processor") { ProcessorChannel(onBack = { navController.popBackStack() }) }
                        composable("memory") {
                            MemoryChannel(
                                onBack = { navController.popBackStack() },
                                onTasks = { navController.navigate(ScreenRoute.Tasks.route) { launchSingleTop = true } }
                            )
                        }
                        composable("network") { NetworkChannel(onBack = { navController.popBackStack() }) }
                        composable("power") { PowerChannel(onBack = { navController.popBackStack() }) }
                        composable("storage") { StorageChannel(onBack = { navController.popBackStack() }) }
                        composable("gpu") { GpuChannel(onBack = { navController.popBackStack() }) }
                    }
                }
                if (!isSettings) rail()
            }
        }

        // № 05 — SETTINGS sheet: rises over the app (CaliperMotion.tBase), sinks
        // back on dismiss. Underlying Overview never leaves composition.
        BackHandler(enabled = showSettings) { showSettings = false }
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically(tween(CaliperMotion.tBase, easing = CaliperMotion.Ease)) { it } +
                fadeIn(tween(120)),
            exit = slideOutVertically(tween(CaliperMotion.tBase, easing = CaliperMotion.Ease)) { it } +
                fadeOut(tween(120)),
            label = "settings-sheet"
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(
                        WindowInsets.statusBars.union(WindowInsets.navigationBars)
                    )
            ) {
                SettingsScreen(
                    currentMedium = currentMedium,
                    onMediumSelected = settingsViewModel::setMedium,
                    onBack = { showSettings = false }
                )
            }
        }
    }
}