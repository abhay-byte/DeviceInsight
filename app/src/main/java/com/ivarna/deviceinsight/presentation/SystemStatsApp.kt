package com.ivarna.deviceinsight.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ivarna.deviceinsight.R
import com.ivarna.deviceinsight.presentation.dashboard.DashboardScreen
import com.ivarna.deviceinsight.presentation.hardware.HardwareScreen
import com.ivarna.deviceinsight.presentation.overlay.OverlayScreen
import com.ivarna.deviceinsight.presentation.settings.SettingsScreen
import com.ivarna.deviceinsight.presentation.tasks.TasksScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import com.ivarna.deviceinsight.presentation.theme.AppTheme
import com.ivarna.deviceinsight.presentation.theme.SystemStatsTheme

sealed class Screen(val route: String, val titleRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Dashboard : Screen("dashboard", R.string.nav_dashboard, androidx.compose.material.icons.Icons.Filled.Home)
    data object Tasks : Screen("tasks", R.string.nav_tasks, androidx.compose.material.icons.Icons.Filled.List)
    data object Hardware : Screen("hardware", R.string.nav_hardware, androidx.compose.material.icons.Icons.Filled.Memory)
    data object Overlay : Screen("overlay", R.string.nav_overlay, androidx.compose.material.icons.Icons.Filled.Layers)
    data object Settings : Screen("settings", R.string.nav_settings, androidx.compose.material.icons.Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemStatsApp() {
    // Theme State
    var currentTheme by remember { mutableStateOf(AppTheme.TechNoir) }

    SystemStatsTheme(theme = currentTheme) {
        val navController = rememberNavController()
        val bottomNavItems = listOf(
            Screen.Dashboard,
            Screen.Tasks,
            Screen.Hardware,
            Screen.Overlay
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "App Icon",
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DeviceInsight")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            navController.navigate(Screen.Settings.route)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(stringResource(screen.titleRes)) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) { DashboardScreen() }
                composable(Screen.Tasks.route) { TasksScreen() }
                composable(Screen.Hardware.route) { HardwareScreen() }
                composable(Screen.Overlay.route) { OverlayScreen() }
                composable(Screen.Settings.route) { 
                    SettingsScreen(
                        currentTheme = currentTheme,
                        onThemeSelected = { newTheme -> currentTheme = newTheme }
                    ) 
                }
            }
        }
    }
}
