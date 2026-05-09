package com.hananelsabag.autocare.presentation.screens.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananelsabag.autocare.data.local.entities.VehicleRecord
import com.hananelsabag.autocare.data.local.entities.VehicleRecordType
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.domain.repository.VehicleRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CarDocumentsViewModel @Inject constructor(
    private val vehicleRecordRepository: VehicleRecordRepository,
    private val carRepository: CarRepository
) : ViewModel() {

    private val _carId = MutableStateFlow<Int?>(null)
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    val activeTestRecord = _carId
        .filterNotNull()
        .flatMapLatest { id -> vehicleRecordRepository.getActiveRecord(id, VehicleRecordType.TEST) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeInsuranceRecord = _carId
        .filterNotNull()
        .flatMapLatest { id -> vehicleRecordRepository.getActiveRecord(id, VehicleRecordType.INSURANCE) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun init(carId: Int) {
        if (_carId.value != carId) _carId.value = carId
    }

    fun clearError() { _error.value = null }

    fun saveRecord(
        carId: Int,
        type: VehicleRecordType,
        expiryDate: Long,
        fileUri: String?
    ) {
        viewModelScope.launch {
            try {
                vehicleRecordRepository.saveRecord(
                    VehicleRecord(
                        carId = carId,
                        type = type,
                        expiryDate = expiryDate,
                        fileUri = fileUri
                    )
                )
                // Sync expiry on Car entity so StatusBanner + reminders stay current
                carRepository.getCarById(carId).first()?.let { car ->
                    val updated = when (type) {
                        VehicleRecordType.TEST -> car.copy(testExpiryDate = expiryDate)
                        VehicleRecordType.INSURANCE -> car.copy(insuranceExpiryDate = expiryDate)
                    }
                    carRepository.updateCar(updated)
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
