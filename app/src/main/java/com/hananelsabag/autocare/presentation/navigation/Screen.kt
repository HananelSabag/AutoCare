package com.hananelsabag.autocare.presentation.navigation

sealed class Screen(val route: String) {
    data object Cars : Screen("cars")
    data object Reminders : Screen("reminders")
    data object Settings : Screen("settings")

    data object CarProfile : Screen("car_profile/{carId}") {
        fun createRoute(carId: Int) = "car_profile/$carId"
        const val ARG_CAR_ID = "carId"
    }

    data object MaintenanceHistory : Screen("maintenance_history/{carId}") {
        fun createRoute(carId: Int) = "maintenance_history/$carId"
        const val ARG_CAR_ID = "carId"
    }

    data object CarReminders : Screen("car_reminders/{carId}") {
        fun createRoute(carId: Int) = "car_reminders/$carId"
        const val ARG_CAR_ID = "carId"
    }

    data object CarDocuments : Screen("car_documents/{carId}") {
        fun createRoute(carId: Int) = "car_documents/$carId"
        const val ARG_CAR_ID = "carId"
    }
}
