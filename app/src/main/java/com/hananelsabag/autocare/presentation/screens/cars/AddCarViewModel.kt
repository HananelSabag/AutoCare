package com.hananelsabag.autocare.presentation.screens.cars

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.domain.repository.CarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FieldError {
    data object Required : FieldError()
    data object InvalidYear : FieldError()
    data object InvalidLicensePlate : FieldError()
}

@HiltViewModel
class AddCarViewModel @Inject constructor(
    private val repository: CarRepository
) : ViewModel() {

    // Form fields
    var make by mutableStateOf("")
    var model by mutableStateOf("")
    var year by mutableStateOf("")
    var licensePlate by mutableStateOf("")
    var color by mutableStateOf("")
    var photoUri by mutableStateOf<String?>(null)
    var currentKm by mutableStateOf("")
    var testExpiryDate by mutableStateOf<Long?>(null)
    var insuranceExpiryDate by mutableStateOf<Long?>(null)
    var notes by mutableStateOf("")

    // Validation errors
    var makeError by mutableStateOf<FieldError?>(null)
        private set
    var modelError by mutableStateOf<FieldError?>(null)
        private set
    var yearError by mutableStateOf<FieldError?>(null)
        private set
    var licensePlateError by mutableStateOf<FieldError?>(null)
        private set

    private var editingCarId: Int? = null
    private var carCreatedAt: Long = 0L
    val isEditing: Boolean get() = editingCarId != null

    // Emits the ID of the last newly-inserted car (not edits) so the UI can prompt for reminders
    private val _lastSavedCarId = MutableStateFlow<Int?>(null)
    val lastSavedCarId: StateFlow<Int?> = _lastSavedCarId.asStateFlow()

    fun clearLastSavedCarId() { _lastSavedCarId.value = null }

    fun resetForm() {
        editingCarId = null
        carCreatedAt = 0L
        make = ""; model = ""; year = ""; licensePlate = ""
        color = ""; photoUri = null; currentKm = ""
        testExpiryDate = null; insuranceExpiryDate = null
        notes = ""
        makeError = null; modelError = null; yearError = null; licensePlateError = null
    }

    fun loadCarForEdit(carId: Int) {
        viewModelScope.launch {
            repository.getCarById(carId).first()?.let { car ->
                editingCarId = car.id
                carCreatedAt = car.createdAt
                make = car.make
                model = car.model
                year = car.year.toString()
                licensePlate = car.licensePlate.filter { it.isDigit() }
                color = car.color ?: ""
                photoUri = car.photoUri
                currentKm = car.currentKm?.toString() ?: ""
                testExpiryDate = car.testExpiryDate
                insuranceExpiryDate = car.insuranceExpiryDate
                notes = car.notes ?: ""
            }
        }
    }

    fun hasUnsavedData(): Boolean =
        make.isNotBlank() || model.isNotBlank() || year.isNotBlank() ||
        licensePlate.isNotBlank() || notes.isNotBlank() || photoUri != null

    private fun validate(): Boolean {
        makeError = null; modelError = null; yearError = null; licensePlateError = null
        var valid = true

        if (make.isBlank()) { makeError = FieldError.Required; valid = false }
        if (model.isBlank()) { modelError = FieldError.Required; valid = false }
        val plateDigits = licensePlate.filter { it.isDigit() }
        when {
            licensePlate.isBlank() -> { licensePlateError = FieldError.Required; valid = false }
            plateDigits.length !in 7..8 -> { licensePlateError = FieldError.InvalidLicensePlate; valid = false }
        }

        val yearInt = year.toIntOrNull()
        val maxYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 1
        when {
            year.isBlank() -> { yearError = FieldError.Required; valid = false }
            yearInt == null || yearInt < 1900 || yearInt > maxYear -> {
                yearError = FieldError.InvalidYear; valid = false
            }
        }
        return valid
    }

    fun save(onSaved: () -> Unit) {
        if (!validate()) return
        viewModelScope.launch {
            val car = Car(
                id = editingCarId ?: 0,
                make = make.trim(),
                model = model.trim(),
                year = year.toInt(),
                licensePlate = run {
                    val d = licensePlate.filter { it.isDigit() }
                    when (d.length) {
                        in 0..2 -> d
                        in 3..5 -> "${d.substring(0, 2)}-${d.substring(2)}"
                        in 6..7 -> "${d.substring(0, 2)}-${d.substring(2, 5)}-${d.substring(5)}"
                        8       -> "${d.substring(0, 3)}-${d.substring(3, 5)}-${d.substring(5, 8)}"
                        else    -> d
                    }
                },
                color = color.trim().ifBlank { null },
                photoUri = photoUri,
                currentKm = currentKm.toIntOrNull(),
                testExpiryDate = testExpiryDate,
                insuranceExpiryDate = insuranceExpiryDate,
                notes = notes.trim().ifBlank { null },
                createdAt = if (isEditing) carCreatedAt else System.currentTimeMillis()
            )
            if (isEditing) {
                repository.updateCar(car)
            } else {
                val newId = repository.insertCar(car)
                _lastSavedCarId.value = newId.toInt()
            }
            onSaved()
        }
    }
}
