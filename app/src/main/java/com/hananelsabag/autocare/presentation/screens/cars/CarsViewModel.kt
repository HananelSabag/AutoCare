package com.hananelsabag.autocare.presentation.screens.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.domain.repository.MaintenanceRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CarsViewModel @Inject constructor(
    private val repository: CarRepository,
    private val maintenanceRepository: MaintenanceRecordRepository
) : ViewModel() {

    val cars = repository.getAllCars()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // carId → next service due timestamp (last MAINTENANCE date + 365 days)
    @OptIn(ExperimentalCoroutinesApi::class)
    val nextServiceDueMsByCarId = cars
        .flatMapLatest { carList ->
            flow {
                val map = carList.associate { car ->
                    val lastDate = maintenanceRepository.getLastMaintenanceDateForCar(car.id)
                    car.id to lastDate?.let { it + 365L * 24 * 60 * 60 * 1000 }
                }
                emit(map)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    fun deleteCar(car: Car) {
        viewModelScope.launch { repository.deleteCar(car) }
    }

    fun moveLeft(car: Car) = applyReorder(car) { list, idx ->
        if (idx > 0) {
            list.removeAt(idx)
            list.add(idx - 1, car)
        }
    }

    fun moveRight(car: Car) = applyReorder(car) { list, idx ->
        if (idx < list.lastIndex) {
            list.removeAt(idx)
            list.add(idx + 1, car)
        }
    }

    private fun applyReorder(car: Car, transform: (MutableList<Car>, Int) -> Unit) {
        viewModelScope.launch {
            val sorted = cars.value.toMutableList()
            val idx = sorted.indexOfFirst { it.id == car.id }
            if (idx == -1) return@launch
            transform(sorted, idx)
            sorted.forEachIndexed { i, c ->
                if (c.displayOrder != i) repository.updateCar(c.copy(displayOrder = i))
            }
        }
    }
}
