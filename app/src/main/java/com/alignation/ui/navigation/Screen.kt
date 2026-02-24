package com.alignation.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Dashboard : Screen("dashboard")
    data object History : Screen("history")
    data object More : Screen("more")
    data object Settings : Screen("settings")
    data object AuditLog : Screen("audit_log")
    data object PhotoCapture : Screen("photo_capture")
    data object Feedback : Screen("feedback")
}
