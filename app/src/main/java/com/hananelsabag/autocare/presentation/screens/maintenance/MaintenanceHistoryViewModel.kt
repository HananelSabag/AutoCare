package com.hananelsabag.autocare.presentation.screens.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananelsabag.autocare.data.local.entities.MaintenanceRecord
import com.hananelsabag.autocare.domain.repository.MaintenanceRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
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
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    val records = _carId
        .filterNotNull()
        .flatMapLatest { id -> repository.getRecordsForCar(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun init(carId: Int) {
        if (_carId.value != carId) _carId.value = carId
    }

    fun clearError() { _error.value = null }

    fun insertRecord(record: MaintenanceRecord) {
        viewModelScope.launch {
            try { repository.insertRecord(record) }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    fun updateRecord(record: MaintenanceRecord) {
        viewModelScope.launch {
            try { repository.updateRecord(record) }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    fun deleteRecord(record: MaintenanceRecord) {
        viewModelScope.launch {
            try { repository.deleteRecord(record) }
            catch (e: Exception) { _error.value = e.message }
        }
    }
}
