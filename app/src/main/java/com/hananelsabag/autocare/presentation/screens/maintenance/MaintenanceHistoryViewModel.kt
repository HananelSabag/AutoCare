package com.hananelsabag.autocare.presentation.screens.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananelsabag.autocare.data.local.entities.MaintenanceRecord
import com.hananelsabag.autocare.domain.repository.MaintenanceRecordRepository
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
class MaintenanceHistoryViewModel @Inject constructor(
    private val repository: MaintenanceRecordRepository
) : ViewModel() {

    private val _carId = MutableStateFlow<Int?>(null)

    val records = _carId
        .filterNotNull()
        .flatMapLatest { id -> repository.getRecordsForCar(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun init(carId: Int) {
        if (_carId.value == null) _carId.value = carId
    }

    fun insertRecord(record: MaintenanceRecord) {
        viewModelScope.launch { repository.insertRecord(record) }
    }

    fun updateRecord(record: MaintenanceRecord) {
        viewModelScope.launch { repository.updateRecord(record) }
    }

    fun deleteRecord(record: MaintenanceRecord) {
        viewModelScope.launch { repository.deleteRecord(record) }
    }
}
