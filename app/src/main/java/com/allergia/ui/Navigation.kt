package com.allergia.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.allergia.ui.screens.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Diary : Screen("diary")
    object Analysis : Screen("analysis")
    object ProductSearch : Screen("product_search")
    object Settings : Screen("settings")
    object PhotoArchive : Screen("photo_archive")
}

@Composable
fun AllergiaNavigation() {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val bottomBarRoutes = listOf(Screen.Home.route, Screen.Analysis.route, Screen.Settings.route)
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Дневник") },
                        selected = currentRoute == Screen.Home.route,
                        onClick = { navController.navigate(Screen.Home.route) { launchSingleTop = true; popUpTo(Screen.Home.route) } }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Psychology, null) },
                        label = { Text("AI-анализ") },
                        selected = currentRoute == Screen.Analysis.route,
                        onClick = { navController.navigate(Screen.Analysis.route) { launchSingleTop = true } }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Настройки") },
                        selected = currentRoute == Screen.Settings.route,
                        onClick = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } }
                    )
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
                HomeScreen(
                    onNavigateToDiary = { navController.navigate(Screen.Diary.route) },
                    onNavigateToAnalysis = { navController.navigate(Screen.Analysis.route) },
                    onNavigateToSearch = { navController.navigate(Screen.ProductSearch.route) }
                )
            }
            composable(Screen.Diary.route) {
                DiaryScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToArchive = { navController.navigate(Screen.PhotoArchive.route) }
                )
            }
            composable(Screen.PhotoArchive.route) {
                PhotoArchiveScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Analysis.route) {
                AnalysisScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.ProductSearch.route) {
                ProductSearchScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
