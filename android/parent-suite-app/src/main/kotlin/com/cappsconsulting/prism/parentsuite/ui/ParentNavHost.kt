package com.cappsconsulting.prism.parentsuite.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cappsconsulting.prism.parentsuite.ui.map.MapScreen
import com.cappsconsulting.prism.parentsuite.ui.pairing.PairingScreen
import com.cappsconsulting.prism.parentsuite.ui.preview.PreviewModeScreen
import com.cappsconsulting.prism.parentsuite.ui.settings.SettingsScreen
import com.cappsconsulting.prism.parentsuite.ui.trajectory.TrajectoryScreen
import com.cappsconsulting.prism.parentsuite.viewmodel.ParentSuiteViewModel

private const val ROUTE_MAP = "map"
private const val ROUTE_TRAJECTORY = "trajectory"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_PAIRING = "pairing"
private const val ROUTE_PREVIEW = "preview"

/**
 * Navigation host for the Parent Suite — bottom nav (Map / Trajectory / Settings)
 * plus two nested destinations accessible from Settings:
 * [PairingScreen] and [PreviewModeScreen].
 *
 * [ParentSuiteViewModel] is passed directly to each screen composable — no DI
 * framework, consistent with the rest of this codebase.
 */
@Composable
fun ParentNavHost(viewModel: ParentSuiteViewModel, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavRoutes = setOf(ROUTE_MAP, ROUTE_TRAJECTORY, ROUTE_SETTINGS)
    val showBottomBar = currentDestination?.hierarchy?.any { it.route in bottomNavRoutes } == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                        label = { Text("Map") },
                        selected = currentDestination?.hierarchy?.any { it.route == ROUTE_MAP } == true,
                        onClick = {
                            navController.navigate(ROUTE_MAP) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.History, contentDescription = null) },
                        label = { Text("Trajectory") },
                        selected = currentDestination?.hierarchy?.any { it.route == ROUTE_TRAJECTORY } == true,
                        onClick = {
                            navController.navigate(ROUTE_TRAJECTORY) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                        selected = currentDestination?.hierarchy?.any { it.route == ROUTE_SETTINGS } == true,
                        onClick = {
                            navController.navigate(ROUTE_SETTINGS) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_MAP,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(ROUTE_MAP) {
                MapScreen(viewModel = viewModel)
            }
            composable(ROUTE_TRAJECTORY) {
                TrajectoryScreen(viewModel = viewModel)
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToPairing = { navController.navigate(ROUTE_PAIRING) },
                    onNavigateToPreview = { navController.navigate(ROUTE_PREVIEW) },
                )
            }
            composable(ROUTE_PAIRING) {
                PairingScreen(viewModel = viewModel)
            }
            composable(ROUTE_PREVIEW) {
                PreviewModeScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
