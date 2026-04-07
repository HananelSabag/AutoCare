package com.hananelsabag.autocare.presentation.screens.carprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.data.local.entities.MaintenanceRecord
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.domain.repository.MaintenanceRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class CarStats(
    val totalRecords: Int,
    val totalSpentThisYear: Double?,   // null if no record has a cost
    val lastServiceDate: Long?,         // null if no records
    val averageCost: Double?            // null if no record has a cost
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CarProfileViewModel @Inject constructor(
    private val repository: CarRepository,
    private val recordRepository: MaintenanceRecordRepository
) : ViewModel() {

    private val _carId = MutableStateFlow<Int?>(null)

    val car = _carId
        .filterNotNull()
        .flatMapLatest { id -> repository.getCarById(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val records = _carId
        .filterNotNull()
        .flatMapLatest { id -> recordRepository.getRecordsForCar(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val stats = records
        .map { list -> computeStats(list) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun init(carId: Int) {
        if (_carId.value == null) _carId.value = carId
    }

    fun deleteCar(onDeleted: () -> Unit) {
        viewModelScope.launch {
            car.value?.let { repository.deleteCar(it) }
            onDeleted()
        }
    }

    fun updateCar(car: Car) {
        viewModelScope.launch { repository.updateCar(car) }
    }

    private fun computeStats(records: List<MaintenanceRecord>): CarStats? {
        if (records.isEmpty()) return null

        val currentYear = java.time.LocalDate.now().year
        val recordsWithCost = records.filter { it.costAmount != null }

        val totalSpentThisYear = records
            .filter { record ->
                val year = Instant.ofEpochMilli(record.date)
                    .atZone(ZoneId.systemDefault())
                    .year
                year == currentYear && record.costAmount != null
            }
            .sumOf { it.costAmount!! }
            .takeIf { recordsWithCost.any { r ->
                Instant.ofEpochMilli(r.date).atZone(ZoneId.systemDefault()).year == currentYear
            }}

        val averageCost = if (recordsWithCost.isNotEmpty())
            recordsWithCost.sumOf { it.costAmount!! } / recordsWithCost.size
        else null

        return CarStats(
            totalRecords = records.size,
            totalSpentThisYear = totalSpentThisYear,
            lastServiceDate = records.maxOfOrNull { it.date },
            averageCost = averageCost
        )
    }
}
