package com.appcontrolx.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.appcontrolx.ui.screens.about.AboutScreen
import com.appcontrolx.ui.screens.activitylauncher.ActivityLauncherScreen
import com.appcontrolx.ui.screens.apps.AppListScreen
import com.appcontrolx.ui.screens.dashboard.DashboardScreen
import com.appcontrolx.ui.screens.settings.SettingsScreen
import com.appcontrolx.ui.screens.setup.SetupScreen
import com.appcontrolx.ui.screens.tools.ToolsScreen
import com.appcontrolx.ui.viewmodels.MainViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val isSetupCompleted by mainViewModel.isSetupCompleted.collectAsStateWithLifecycle()

    val startDestination = if (isSetupCompleted) Screen.Dashboard.route else Screen.Setup.route

    NavHost(
        navController = navController,
        startDestination = startDestination
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
                onNavigateToApps = { navController.navigate(Screen.Apps.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
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
                        popUpTo(0) { inclusive = true }
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
