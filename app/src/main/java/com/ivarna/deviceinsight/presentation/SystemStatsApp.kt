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
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
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
import com.ivarna.deviceinsight.ui.caliper.CaliperTheme
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

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_HARDWARE = "hardware"
private const val ROUTE_OVERLAY = "overlay"
private const val ROUTE_TASKS = "tasks"

internal const val GRAPH_OVERVIEW = "overview_graph"
internal const val GRAPH_DEVICE = "device_graph"
internal const val GRAPH_OVERLAY = "overlay_graph"
internal const val GRAPH_PROCESSES = "processes_graph"

internal sealed class ScreenRoute(val route: String, val number: Int, val label: String, val graph: String) {
    data object Dashboard : ScreenRoute(ROUTE_DASHBOARD, 1, "OVERVIEW", GRAPH_OVERVIEW)
    data object Hardware : ScreenRoute(ROUTE_HARDWARE, 2, "DEVICE", GRAPH_DEVICE)
    data object Overlay : ScreenRoute(ROUTE_OVERLAY, 3, "OVERLAY", GRAPH_OVERLAY)
    data object Tasks : ScreenRoute(ROUTE_TASKS, 4, "PROCESSES", GRAPH_PROCESSES)
}

internal val railRoutes = listOf(
    ScreenRoute.Dashboard,
    ScreenRoute.Hardware,
    ScreenRoute.Overlay,
    ScreenRoute.Tasks,
)

internal val caliperRailOrder: List<Pair<Int, String>> = railRoutes.map { it.number to it.label }

private const val WIDE_MIN_DP = 600

fun NavController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = false
    }
}

private fun graphForRoute(route: String): String = when (route) {
    ROUTE_DASHBOARD, "processor", "memory", "network", "power", "storage", "gpu" -> GRAPH_OVERVIEW
    ROUTE_HARDWARE -> GRAPH_DEVICE
    ROUTE_OVERLAY -> GRAPH_OVERLAY
    ROUTE_TASKS -> GRAPH_PROCESSES
    else -> route
}

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
            "overview" -> navController.navigateToTopLevel(ScreenRoute.Dashboard.route)
            "CH-01", "processor" -> navController.navigate("processor") { launchSingleTop = true }
            "CH-02" -> navController.navigate("memory") { launchSingleTop = true }
            "CH-03" -> navController.navigate("network") { launchSingleTop = true }
            "CH-04" -> navController.navigate("power") { launchSingleTop = true }
            "CH-05" -> navController.navigate("storage") { launchSingleTop = true }
            "CH-06" -> navController.navigate("gpu") { launchSingleTop = true }
            "processes" -> navController.navigateToTopLevel(ScreenRoute.Tasks.route)
            "calibrate" -> navController.navigateToTopLevel(ScreenRoute.Overlay.route)
            "hud-config" -> navController.navigateToTopLevel(ScreenRoute.Overlay.route)
            else -> if (initialRoute?.startsWith("dossier:") == true) navController.navigateToTopLevel(ScreenRoute.Tasks.route)
        }
    }

    val selectedRail = remember(currentDestination) {
        railRoutes.firstOrNull { rail ->
            currentDestination?.hierarchy?.any { dest -> dest.route == rail.route || dest.route == rail.graph } == true
        }?.number
            ?: railRoutes.firstOrNull { rail ->
                currentDestination?.hierarchy?.any { dest -> graphForRoute(dest.route ?: "") == rail.graph } == true
            }?.number
    }

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

    val context = LocalContext.current
    val migrated by context.caliperMigratedFlow.collectAsStateWithLifecycle(initialValue = true)
    var showCalibration by remember { mutableStateOf(!migrated) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(migrated) {
        if (!migrated) showCalibration = true
    }

    if (showCalibration) {
        CaliperTheme(medium = Medium.PAPER) {
            Column(Modifier.fillMaxSize().background(Caliper.colors.surface)) {
                Masthead()
                CalibrationScreen(
                    initialMedium = Medium.PAPER,
                    onMedium = { medium ->
                        settingsViewModel.setMedium(medium)
                    },
                    onFinish = {
                        settingsViewModel.setMedium(Medium.PAPER)
                        scope.launch { context.markCaliperMigrated() }
                        showCalibration = false
                    }
                )
            }
        }
        return
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Caliper.colors.surface)
    ) {
        val wide = maxWidth >= WIDE_MIN_DP.dp

        val rail = @Composable {
            ModeRail(
                keys = railRoutes.map { RailKey(it.number, it.label) },
                selected = selectedRail ?: 0,
                vertical = wide,
                onSelect = { key ->
                    val route = railRoutes.first { it.number == key.number }
                    val isAlreadyOnRoot = currentDestination?.hierarchy?.any { it.route == route.route } == true
                    if (isAlreadyOnRoot) {
                        // already on root, no-op
                    } else {
                        val isInSameGraph = currentDestination?.hierarchy?.any { it.route == route.graph } == true
                        if (isInSameGraph) {
                            // Reselecting current graph while on a child -> pop to its root
                            val popped = navController.popBackStack(route.route, inclusive = false)
                            if (!popped) {
                                navController.navigate(route.route) {
                                    popUpTo(route.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        } else {
                            navController.navigateToTopLevel(route.route)
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
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .caliperGrid()
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = GRAPH_OVERVIEW,
                            enterTransition = {
                                fadeIn(tween(160)) + slideInVertically(tween(160)) { it / 8 }
                            },
                            exitTransition = { fadeOut(tween(160)) },
                            popEnterTransition = { fadeIn(tween(160)) },
                            popExitTransition = { fadeOut(tween(160)) }
                        ) {
                            navigation(startDestination = ScreenRoute.Dashboard.route, route = GRAPH_OVERVIEW) {
                                composable(ScreenRoute.Dashboard.route) {
                                    DashboardScreen(onChannel = { route ->
                                        navController.navigate(route) { launchSingleTop = true }
                                    })
                                }
                                composable("processor") { ProcessorChannel(onBack = { navController.popBackStack() }) }
                                composable("memory") {
                                    MemoryChannel(
                                        onBack = { navController.popBackStack() },
                                        onTasks = { navController.navigateToTopLevel(ScreenRoute.Tasks.route) }
                                    )
                                }
                                composable("network") { NetworkChannel(onBack = { navController.popBackStack() }) }
                                composable("power") { PowerChannel(onBack = { navController.popBackStack() }) }
                                composable("storage") { StorageChannel(onBack = { navController.popBackStack() }) }
                                composable("gpu") { GpuChannel(onBack = { navController.popBackStack() }) }
                            }
                            navigation(startDestination = ScreenRoute.Hardware.route, route = GRAPH_DEVICE) {
                                composable(ScreenRoute.Hardware.route) { HardwareScreen(initialTab = hardwareTab) }
                            }
                            navigation(startDestination = ScreenRoute.Overlay.route, route = GRAPH_OVERLAY) {
                                composable(ScreenRoute.Overlay.route) { OverlayScreen() }
                            }
                            navigation(startDestination = ScreenRoute.Tasks.route, route = GRAPH_PROCESSES) {
                                composable(ScreenRoute.Tasks.route) { TasksScreen() }
                            }
                        }
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                if (!isSettings) {
                    Masthead(
                        onSettingsClick = onSettings
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
                        startDestination = GRAPH_OVERVIEW,
                        enterTransition = {
                            fadeIn(tween(160)) + slideInVertically(tween(160)) { it / 8 }
                        },
                        exitTransition = { fadeOut(tween(160)) },
                        popEnterTransition = { fadeIn(tween(160)) },
                        popExitTransition = { fadeOut(tween(160)) }
                    ) {
                        navigation(startDestination = ScreenRoute.Dashboard.route, route = GRAPH_OVERVIEW) {
                            composable(ScreenRoute.Dashboard.route) {
                                DashboardScreen(onChannel = { route ->
                                    navController.navigate(route) { launchSingleTop = true }
                                })
                            }
                            composable("processor") { ProcessorChannel(onBack = { navController.popBackStack() }) }
                            composable("memory") {
                                MemoryChannel(
                                    onBack = { navController.popBackStack() },
                                    onTasks = { navController.navigateToTopLevel(ScreenRoute.Tasks.route) }
                                )
                            }
                            composable("network") { NetworkChannel(onBack = { navController.popBackStack() }) }
                            composable("power") { PowerChannel(onBack = { navController.popBackStack() }) }
                            composable("storage") { StorageChannel(onBack = { navController.popBackStack() }) }
                            composable("gpu") { GpuChannel(onBack = { navController.popBackStack() }) }
                        }
                        navigation(startDestination = ScreenRoute.Hardware.route, route = GRAPH_DEVICE) {
                            composable(ScreenRoute.Hardware.route) { HardwareScreen(initialTab = hardwareTab) }
                        }
                        navigation(startDestination = ScreenRoute.Overlay.route, route = GRAPH_OVERLAY) {
                            composable(ScreenRoute.Overlay.route) { OverlayScreen() }
                        }
                        navigation(startDestination = ScreenRoute.Tasks.route, route = GRAPH_PROCESSES) {
                            composable(ScreenRoute.Tasks.route) { TasksScreen() }
                        }
                    }
                }
                if (!isSettings) rail()
            }
        }

        // SETTINGS sheet: rises over the app (CaliperMotion.tBase), sinks
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