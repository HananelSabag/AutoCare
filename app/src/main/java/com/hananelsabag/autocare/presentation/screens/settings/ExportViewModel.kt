package com.hananelsabag.autocare.presentation.screens.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.export.JsonExporter
import com.hananelsabag.autocare.export.PdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ExportUiState(
    val showCarPicker: Boolean = false,
    val isGeneratingPdf: Boolean = false,
    val isGeneratingJson: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val pdfExporter: PdfExporter,
    private val jsonExporter: JsonExporter
) : ViewModel() {

    val cars: StateFlow<List<Car>> = carRepository.getAllCars()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState

    private val _shareIntent = MutableSharedFlow<Intent>()
    val shareIntent: SharedFlow<Intent> = _shareIntent.asSharedFlow()

    // ── PDF ──────────────────────────────────────────────────────────────────────

    fun onPdfExportRequested() {
        val carList = cars.value
        when {
            carList.isEmpty() -> _uiState.update { it.copy(error = "no_cars") }
            carList.size == 1 -> startPdfExport(carList[0].id)
            else -> _uiState.update { it.copy(showCarPicker = true) }
        }
    }

    fun onCarSelectedForPdf(carId: Int) {
        _uiState.update { it.copy(showCarPicker = false) }
        startPdfExport(carId)
    }

    fun dismissCarPicker() {
        _uiState.update { it.copy(showCarPicker = false) }
    }

    private fun startPdfExport(carId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPdf = true, error = null) }
            runCatching {
                withContext(Dispatchers.IO) { pdfExporter.exportCar(carId) }
            }.onSuccess { intent ->
                _shareIntent.emit(intent)
            }.onFailure {
                _uiState.update { s -> s.copy(error = "pdf_error") }
            }
            _uiState.update { it.copy(isGeneratingPdf = false) }
        }
    }

    // ── JSON ─────────────────────────────────────────────────────────────────────

    fun onJsonExportRequested() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingJson = true, error = null) }
            runCatching {
                withContext(Dispatchers.IO) { jsonExporter.exportAll() }
            }.onSuccess { intent ->
                _shareIntent.emit(intent)
            }.onFailure {
                _uiState.update { s -> s.copy(error = "json_error") }
            }
            _uiState.update { it.copy(isGeneratingJson = false) }
        }
    }

    // ── Error ─────────────────────────────────────────────────────────────────────

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
