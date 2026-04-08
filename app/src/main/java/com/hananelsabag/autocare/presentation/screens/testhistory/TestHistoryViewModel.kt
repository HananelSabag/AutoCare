package com.hananelsabag.autocare.presentation.screens.testhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananelsabag.autocare.data.local.entities.TestRecord
import com.hananelsabag.autocare.domain.repository.TestRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TestHistoryViewModel @Inject constructor(
    private val repository: TestRecordRepository
) : ViewModel() {

    private val _carId = MutableStateFlow(-1)

    val records: StateFlow<List<TestRecord>> = _carId
        .flatMapLatest { id ->
            if (id == -1) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.getByCarId(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun init(carId: Int) {
        _carId.value = carId
    }

    fun insert(record: TestRecord) = viewModelScope.launch { repository.insert(record) }
    fun update(record: TestRecord) = viewModelScope.launch { repository.update(record) }
    fun delete(record: TestRecord) = viewModelScope.launch { repository.delete(record) }
}
