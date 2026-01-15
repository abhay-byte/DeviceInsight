package com.ivarna.deviceinsight.presentation

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.foundation.Canvas
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
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
import com.ivarna.deviceinsight.presentation.settings.SettingsActivity
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
import com.ivarna.deviceinsight.presentation.theme.AppTheme
import com.ivarna.deviceinsight.presentation.theme.SystemStatsTheme

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", R.string.nav_dashboard, Icons.Filled.Home)
    data object Tasks : Screen("tasks", R.string.nav_tasks, Icons.Filled.List)
    data object Hardware : Screen("hardware", R.string.nav_hardware, Icons.Filled.Memory)
    data object Overlay : Screen("overlay", R.string.nav_overlay, Icons.Filled.Layers)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemStatsApp() {
    val context = LocalContext.current
    val hazeState = remember { HazeState() }
    
    SystemStatsTheme(theme = AppTheme.TechNoir) {
        val navController = rememberNavController()
        val bottomNavItems = listOf(
            Screen.Dashboard,
            Screen.Tasks,
            Screen.Hardware,
            Screen.Overlay
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Constant ambient background glows for all screens
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(400.dp)
                    .offset(x = 100.dp, y = (-50).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(400.dp)
                    .offset(x = (-100).dp, y = 100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Scaffold(
                modifier = Modifier.haze(hazeState),
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.logo),
                                    contentDescription = "App Icon",
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.Unspecified
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "DEVICE INSIGHTS",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        actions = {
                            IconButton(onClick = {
                                context.startActivity(Intent(context, SettingsActivity::class.java))
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    composable(Screen.Dashboard.route) { DashboardScreen() }
                    composable(Screen.Tasks.route) { TasksScreen() }
                    composable(Screen.Hardware.route) { HardwareScreen() }
                    composable(Screen.Overlay.route) { OverlayScreen() }
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                GlassBottomNav(
                    navController = navController,
                    items = bottomNavItems,
                    hazeState = hazeState
                )
            }
        }
    }
}

@Composable
fun GlassBottomNav(
    navController: androidx.navigation.NavHostController,
    items: List<Screen>,
    hazeState: HazeState
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .height(72.dp)
            .hazeChild(
                state = hazeState,
                shape = RoundedCornerShape(24.dp),
                style = HazeStyle(
                    tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                    blurRadius = 30.dp,
                    noiseFactor = 0.05f
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.05f)
                        )
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                GlassNavItem(
                    screen = screen,
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            navController.navigate(screen.route) {
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
        }
    }
}

@Composable
fun GlassNavItem(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = tween(300),
        label = "scale"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        animationSpec = tween(300),
        label = "color"
    )

    Column(
        modifier = Modifier
            .wrapContentSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                // Outer glow - constrained to ensure no clipping
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val color = iconColor
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.45f), Color.Transparent),
                            center = center,
                            radius = 28.dp.toPx() // Less than size/2 (32dp) to prevent square clip
                        ),
                        radius = 28.dp.toPx(),
                        center = center
                    )
                }
            }
            Icon(
                imageVector = screen.icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(0.dp))
        
        Text(
            text = stringResource(screen.titleRes).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            ),
            color = iconColor
        )
    }
}
