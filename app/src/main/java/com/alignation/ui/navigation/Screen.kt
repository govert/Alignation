package com.alignation.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Dashboard : Screen("dashboard")
    data object History : Screen("history")
    data object Settings : Screen("settings")
}
