package com.alignation.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alignation.ui.dashboard.DashboardScreen
import com.alignation.ui.feedback.FeedbackScreen
import com.alignation.ui.history.HistoryScreen
import com.alignation.ui.home.HomeScreen
import com.alignation.ui.more.MoreScreen
import com.alignation.ui.photos.PhotoCaptureScreen
import com.alignation.ui.settings.AuditLogScreen
import com.alignation.ui.settings.SettingsScreen

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, Icons.Default.Home, "Home"),
    BottomNavItem(Screen.Dashboard, Icons.Default.BarChart, "Dashboard"),
    BottomNavItem(Screen.History, Icons.Default.History, "History"),
    BottomNavItem(Screen.More, Icons.Default.MoreHoriz, "More"),
    BottomNavItem(Screen.Settings, Icons.Default.Settings, "Settings")
)

// Routes that should show the bottom nav bar
private val bottomNavRoutes = bottomNavItems.map { it.screen.route }.toSet()

@Composable
fun AlignationNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.More.route) {
                MoreScreen(
                    onNavigateToPhotoCapture = {
                        navController.navigate(Screen.PhotoCapture.route)
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToAuditLog = {
                        navController.navigate(Screen.AuditLog.route)
                    },
                    onNavigateToFeedback = {
                        navController.navigate(Screen.Feedback.route)
                    }
                )
            }
            composable(Screen.AuditLog.route) {
                AuditLogScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.PhotoCapture.route) {
                PhotoCaptureScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Feedback.route) {
                FeedbackScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
