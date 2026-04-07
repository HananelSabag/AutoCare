package com.hananelsabag.autocare.presentation.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.data.local.entities.Reminder
import com.hananelsabag.autocare.data.local.entities.ReminderType
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.domain.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CarRemindersViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _carId = MutableStateFlow<Int?>(null)

    val car = _carId
        .filterNotNull()
        .flatMapLatest { id -> carRepository.getCarById(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val reminders = _carId
        .filterNotNull()
        .flatMapLatest { id -> reminderRepository.getRemindersForCar(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun init(carId: Int) {
        if (_carId.value == null) _carId.value = carId
    }

    fun saveReminders(updatedReminders: List<Reminder>) {
        val carId = _carId.value ?: return
        viewModelScope.launch {
            reminderRepository.deleteAllForCar(carId)
            reminderRepository.insertReminders(updatedReminders)
        }
    }

    fun enableDefaultReminders(car: Car) {
        val defaults = buildDefaultReminders(car)
        viewModelScope.launch {
            reminderRepository.deleteAllForCar(car.id)
            reminderRepository.insertReminders(defaults)
        }
    }

    companion object {
        fun buildDefaultReminders(car: Car): List<Reminder> {
            val defaults = mutableListOf<Reminder>()
            if (car.testExpiryDate != null) {
                defaults += Reminder(
                    carId = car.id,
                    type = ReminderType.TEST_EXPIRY,
                    enabled = true,
                    daysBeforeExpiry = 14
                )
            }
            if (car.insuranceExpiryDate != null) {
                defaults += Reminder(
                    carId = car.id,
                    type = ReminderType.INSURANCE_COMPULSORY_EXPIRY,
                    enabled = true,
                    daysBeforeExpiry = 14
                )
            }
            if (car.comprehensiveInsuranceExpiryDate != null) {
                defaults += Reminder(
                    carId = car.id,
                    type = ReminderType.INSURANCE_COMPREHENSIVE_EXPIRY,
                    enabled = true,
                    daysBeforeExpiry = 14
                )
            }
            return defaults
        }
    }
}
