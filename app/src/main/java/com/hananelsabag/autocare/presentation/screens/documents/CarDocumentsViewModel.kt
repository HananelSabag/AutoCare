package com.hananelsabag.autocare.presentation.screens.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananelsabag.autocare.data.local.entities.CarDocument
import com.hananelsabag.autocare.data.local.entities.DocumentType
import com.hananelsabag.autocare.domain.repository.CarDocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CarDocumentsViewModel @Inject constructor(
    private val repository: CarDocumentRepository
) : ViewModel() {

    private val _carId = MutableStateFlow<Int?>(null)

    private val documents = _carId
        .filterNotNull()
        .flatMapLatest { id -> repository.getDocumentsForCar(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Map of type → document (null if not yet uploaded)
    val documentMap = documents
        .map { list -> list.associateBy { it.type } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    fun init(carId: Int) {
        if (_carId.value == null) _carId.value = carId
    }

    fun saveDocument(carId: Int, type: DocumentType, fileUri: String) {
        viewModelScope.launch {
            repository.insertDocument(
                CarDocument(carId = carId, type = type, fileUri = fileUri)
            )
        }
    }

    fun deleteDocument(carId: Int, type: DocumentType) {
        viewModelScope.launch {
            repository.deleteDocumentByType(carId, type)
        }
    }
}
