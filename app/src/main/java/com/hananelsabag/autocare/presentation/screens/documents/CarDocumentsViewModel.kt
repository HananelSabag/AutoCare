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

    val activeTestRecord = _carId
        .filterNotNull()
        .flatMapLatest { id -> vehicleRecordRepository.getActiveRecord(id, VehicleRecordType.TEST) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeInsuranceRecord = _carId
        .filterNotNull()
        .flatMapLatest { id -> vehicleRecordRepository.getActiveRecord(id, VehicleRecordType.INSURANCE) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun init(carId: Int) {
        if (_carId.value == null) _carId.value = carId
    }

    fun saveRecord(
        carId: Int,
        type: VehicleRecordType,
        expiryDate: Long,
        fileUri: String?
    ) {
        viewModelScope.launch {
            vehicleRecordRepository.saveRecord(
                VehicleRecord(
                    carId = carId,
                    type = type,
                    expiryDate = expiryDate,
                    fileUri = fileUri
                )
            )
            // Sync the expiry date on the Car entity so StatusBanner and reminders stay updated
            val car = carRepository.getCarById(carId).first()
            car?.let {
                val updated = when (type) {
                    VehicleRecordType.TEST -> it.copy(testExpiryDate = expiryDate)
                    VehicleRecordType.INSURANCE -> it.copy(insuranceExpiryDate = expiryDate)
                }
                carRepository.updateCar(updated)
            }
        }
    }
}
