package com.appcontrolx.ui.navigation

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Main : Screen("main")
    object Dashboard : Screen("dashboard")
    object Apps : Screen("apps")
    object Tools : Screen("tools")
    object ActivityLauncher : Screen("activity_launcher")
    object Settings : Screen("settings")
    object About : Screen("about")
}
