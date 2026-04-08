package com.hananelsabag.autocare.presentation.screens.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananelsabag.autocare.data.local.entities.VehicleRecord
import com.hananelsabag.autocare.domain.repository.VehicleRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VehicleRecordHistoryViewModel @Inject constructor(
    private val repository: VehicleRecordRepository
) : ViewModel() {

    private val _carId = MutableStateFlow<Int?>(null)

    val allRecords = _carId
        .filterNotNull()
        .flatMapLatest { id -> repository.getAllRecordsForCar(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList<VehicleRecord>()
        )

    fun init(carId: Int) {
        if (_carId.value == null) _carId.value = carId
    }
}
