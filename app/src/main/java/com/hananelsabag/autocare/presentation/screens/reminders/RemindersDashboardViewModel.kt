package com.hananelsabag.autocare.presentation.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.data.local.entities.ReminderType
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.domain.repository.MaintenanceRecordRepository
import com.hananelsabag.autocare.domain.repository.ReminderRepository
import com.hananelsabag.autocare.util.StatusLevel
import com.hananelsabag.autocare.util.daysFromNow
import com.hananelsabag.autocare.util.getStatusLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ReminderDashboardItem(
    val car: Car,
    val type: ReminderType,
    val expiryMs: Long?,       // null = date not set
    val daysLeft: Long?,       // null when expiryMs is null
    val level: StatusLevel
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RemindersDashboardViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val reminderRepository: ReminderRepository,
    private val maintenanceRecordRepository: MaintenanceRecordRepository
) : ViewModel() {

    val items = carRepository.getAllCars()
        .flatMapLatest { cars ->
            // For each car, combine its reminders + last maintenance date
            if (cars.isEmpty()) {
                flow { emit(emptyList<ReminderDashboardItem>()) }
            } else {
                val carFlows = cars.map { car ->
                    combine(
                        reminderRepository.getRemindersForCar(car.id),
                        flow { emit(maintenanceRecordRepository.getLastMaintenanceDateForCar(car.id)) }
                    ) { reminders, lastMaintenance ->
                        reminders
                            .filter { it.enabled }
                            .map { reminder ->
                                val expiryMs: Long? = when (reminder.type) {
                                    ReminderType.TEST_EXPIRY -> car.testExpiryDate
                                    ReminderType.INSURANCE_EXPIRY -> car.insuranceExpiryDate
                                    ReminderType.SERVICE_DATE ->
                                        lastMaintenance?.plus(TimeUnit.DAYS.toMillis(365))
                                }
                                ReminderDashboardItem(
                                    car = car,
                                    type = reminder.type,
                                    expiryMs = expiryMs,
                                    daysLeft = expiryMs?.daysFromNow(),
                                    level = getStatusLevel(expiryMs)
                                )
                            }
                    }
                }
                combine(carFlows) { arrays -> arrays.flatMap { it } }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
