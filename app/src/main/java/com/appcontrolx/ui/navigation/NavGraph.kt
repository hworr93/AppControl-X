package com.appcontrolx.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.appcontrolx.ui.screens.about.AboutScreen
import com.appcontrolx.ui.screens.activity_launcher.ActivityLauncherScreen
import com.appcontrolx.ui.screens.apps.AppListScreen
import com.appcontrolx.ui.screens.dashboard.DashboardScreen
import com.appcontrolx.ui.screens.settings.SettingsScreen
import com.appcontrolx.ui.screens.setup.SetupScreen
import com.appcontrolx.ui.screens.tools.ToolsScreen
import com.appcontrolx.ui.MainViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val isSetupCompleted by mainViewModel.isSetupCompleted.collectAsStateWithLifecycle()

    if (isSetupCompleted == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (isSetupCompleted == true) Screen.Dashboard.route else Screen.Setup.route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute != null && currentRoute != Screen.Setup.route
    val toolsRoutes = setOf(Screen.Tools.route, Screen.Apps.route, Screen.ActivityLauncher.route)
    val settingsRoutes = setOf(Screen.Settings.route, Screen.About.route)

    val enterTransition: androidx.compose.animation.AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth / 3 }) + fadeIn()
    }
    val exitTransition: androidx.compose.animation.AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth / 6 }) + fadeOut()
    }
    val popEnterTransition: androidx.compose.animation.AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth / 3 }) + fadeIn()
    }
    val popExitTransition: androidx.compose.animation.AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth / 6 }) + fadeOut()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Home") },
                        selected = currentRoute == Screen.Dashboard.route,
                        onClick = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Build, contentDescription = null) },
                        label = { Text("Tools") },
                        selected = currentRoute in toolsRoutes,
                        onClick = {
                            navController.navigate(Screen.Tools.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                        selected = currentRoute in settingsRoutes,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = popExitTransition
        ) {
            composable(Screen.Setup.route) {
                SetupScreen(
                    onComplete = {
                        mainViewModel.completeSetup()
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Setup.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToApps = { navController.navigate(Screen.Apps.route) }
                )
            }

            composable(Screen.Apps.route) {
                AppListScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Tools.route) {
                ToolsScreen(
                    onNavigateToApps = { navController.navigate(Screen.Apps.route) },
                    onNavigateToActivityLauncher = { navController.navigate(Screen.ActivityLauncher.route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ActivityLauncher.route) {
                ActivityLauncherScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToAbout = { navController.navigate(Screen.About.route) },
                    onNavigateBack = { navController.popBackStack() },
                    onResetSetup = {
                        mainViewModel.resetSetup()
                        navController.navigate(Screen.Setup.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
