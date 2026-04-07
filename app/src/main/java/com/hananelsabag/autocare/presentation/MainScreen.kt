package com.hananelsabag.autocare.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.presentation.navigation.Screen
import com.hananelsabag.autocare.presentation.screens.carprofile.CarProfileScreen
import com.hananelsabag.autocare.presentation.screens.cars.CarsScreen
import com.hananelsabag.autocare.presentation.screens.maintenance.MaintenanceHistoryScreen
import com.hananelsabag.autocare.presentation.screens.documents.CarDocumentsScreen
import com.hananelsabag.autocare.presentation.screens.reminders.CarRemindersScreen
import com.hananelsabag.autocare.presentation.screens.reminders.RemindersScreen
import com.hananelsabag.autocare.presentation.screens.settings.SettingsScreen

private data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescriptionRes: Int
)

private val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Cars,
        labelRes = R.string.nav_cars,
        selectedIcon = Icons.Filled.DirectionsCar,
        unselectedIcon = Icons.Outlined.DirectionsCar,
        contentDescriptionRes = R.string.nav_cars_description
    ),
    BottomNavItem(
        screen = Screen.Reminders,
        labelRes = R.string.nav_reminders,
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        contentDescriptionRes = R.string.nav_reminders_description
    ),
    BottomNavItem(
        screen = Screen.Settings,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        contentDescriptionRes = R.string.nav_settings_description
    )
)

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Cars.route, Screen.Reminders.route, Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = stringResource(item.contentDescriptionRes)
                                )
                            },
                            label = { Text(stringResource(item.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Cars.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Cars.route) {
                CarsScreen(
                    onCarClick = { carId ->
                        navController.navigate(Screen.CarProfile.createRoute(carId))
                    }
                )
            }

            composable(Screen.Reminders.route) {
                RemindersScreen(
                    onCarClick = { carId ->
                        navController.navigate(Screen.CarReminders.createRoute(carId))
                    }
                )
            }

            composable(Screen.Settings.route) { SettingsScreen() }

            composable(
                route = Screen.CarProfile.route,
                arguments = listOf(
                    navArgument(Screen.CarProfile.ARG_CAR_ID) { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val carId = backStackEntry.arguments?.getInt(Screen.CarProfile.ARG_CAR_ID)
                    ?: return@composable
                CarProfileScreen(
                    carId = carId,
                    onBack = { navController.popBackStack() },
                    onMaintenanceHistory = {
                        navController.navigate(Screen.MaintenanceHistory.createRoute(carId))
                    },
                    onDocuments = {
                        navController.navigate(Screen.CarDocuments.createRoute(carId))
                    }
                )
            }

            composable(
                route = Screen.MaintenanceHistory.route,
                arguments = listOf(
                    navArgument(Screen.MaintenanceHistory.ARG_CAR_ID) { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val carId = backStackEntry.arguments?.getInt(Screen.MaintenanceHistory.ARG_CAR_ID)
                    ?: return@composable
                MaintenanceHistoryScreen(
                    carId = carId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CarReminders.route,
                arguments = listOf(
                    navArgument(Screen.CarReminders.ARG_CAR_ID) { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val carId = backStackEntry.arguments?.getInt(Screen.CarReminders.ARG_CAR_ID)
                    ?: return@composable
                CarRemindersScreen(
                    carId = carId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CarDocuments.route,
                arguments = listOf(
                    navArgument(Screen.CarDocuments.ARG_CAR_ID) { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val carId = backStackEntry.arguments?.getInt(Screen.CarDocuments.ARG_CAR_ID)
                    ?: return@composable
                CarDocumentsScreen(
                    carId = carId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
