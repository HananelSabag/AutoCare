package com.hananelsabag.autocare.presentation.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.export.JsonExporter
import com.hananelsabag.autocare.export.JsonImporter
import com.hananelsabag.autocare.export.PdfExporter
import com.hananelsabag.autocare.export.PdfExportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ExportUiState(
    val showCarPicker: Boolean = false,
    val isGeneratingPdf: Boolean = false,
    val isGeneratingJson: Boolean = false,
    val isImporting: Boolean = false,
    val error: String? = null
)

sealed class ExportEvent {
    /** PDF saved to MediaStore Downloads (API 29+). */
    data class PdfSaved(val uri: Uri, val fileName: String) : ExportEvent()
    /** Generic share sheet intent — used for JSON sharing and API < 29 PDF fallback. */
    data class ShareIntent(val intent: Intent) : ExportEvent()
    /** JSON backup written to Downloads. */
    data class BackupSaved(val fileName: String, val shareUri: Uri) : ExportEvent()
    /** Import finished successfully. */
    data class ImportSuccess(val carsImported: Int) : ExportEvent()
    /** Any export / import failure. */
    data class Error(val tag: String) : ExportEvent()
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val pdfExporter: PdfExporter,
    private val jsonExporter: JsonExporter,
    private val jsonImporter: JsonImporter
) : ViewModel() {

    val cars: StateFlow<List<Car>> = carRepository.getAllCars()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState

    private val _events = MutableSharedFlow<ExportEvent>()
    val events: SharedFlow<ExportEvent> = _events.asSharedFlow()

    // ── PDF ──────────────────────────────────────────────────────────────────

    fun onPdfExportRequested() {
        val carList = cars.value
        when {
            carList.isEmpty() -> viewModelScope.launch { _events.emit(ExportEvent.Error("no_cars")) }
            carList.size == 1 -> startPdfExport(carList[0].id)
            else              -> _uiState.update { it.copy(showCarPicker = true) }
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
            _uiState.update { it.copy(isGeneratingPdf = true) }
            runCatching {
                withContext(Dispatchers.IO) { pdfExporter.exportCar(carId) }
            }.onSuccess { result ->
                when (result) {
                    is PdfExportResult.SavedToDownloads ->
                        _events.emit(ExportEvent.PdfSaved(result.uri, result.fileName))
                    is PdfExportResult.Share ->
                        _events.emit(ExportEvent.ShareIntent(result.intent))
                }
            }.onFailure {
                _events.emit(ExportEvent.Error("pdf_error"))
            }
            _uiState.update { it.copy(isGeneratingPdf = false) }
        }
    }

    // ── JSON Backup ───────────────────────────────────────────────────────────

    fun onJsonExportRequested() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingJson = true) }
            runCatching {
                withContext(Dispatchers.IO) { jsonExporter.exportAll() }
            }.onSuccess { result ->
                _events.emit(ExportEvent.BackupSaved(result.fileName, result.shareUri))
            }.onFailure {
                _events.emit(ExportEvent.Error("json_error"))
            }
            _uiState.update { it.copy(isGeneratingJson = false) }
        }
    }

    fun onShareBackup(uri: Uri) {
        viewModelScope.launch {
            _events.emit(ExportEvent.ShareIntent(jsonExporter.buildShareIntent(uri)))
        }
    }

    // ── JSON Import ───────────────────────────────────────────────────────────

    fun onImportFileSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            runCatching {
                withContext(Dispatchers.IO) { jsonImporter.importFrom(uri) }
            }.onSuccess { result ->
                _events.emit(ExportEvent.ImportSuccess(result.carsImported))
            }.onFailure {
                _events.emit(ExportEvent.Error("import_error"))
            }
            _uiState.update { it.copy(isImporting = false) }
        }
    }
}
