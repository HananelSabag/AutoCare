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
    val isEditing: Boolean get() = editingCarId != null

    // Emits the ID of the last newly-inserted car (not edits) so the UI can prompt for reminders
    private val _lastSavedCarId = MutableStateFlow<Int?>(null)
    val lastSavedCarId: StateFlow<Int?> = _lastSavedCarId.asStateFlow()

    fun clearLastSavedCarId() { _lastSavedCarId.value = null }

    fun resetForm() {
        editingCarId = null
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
                make = car.make
                model = car.model
                year = car.year.toString()
                licensePlate = car.licensePlate
                color = car.color ?: ""
                photoUri = car.photoUri
                currentKm = car.currentKm?.toString() ?: ""
                testExpiryDate = car.testExpiryDate
                insuranceExpiryDate = car.insuranceExpiryDate
                notes = car.notes ?: ""
            }
        }
    }

    private fun validate(): Boolean {
        makeError = null; modelError = null; yearError = null; licensePlateError = null
        var valid = true

        if (make.isBlank()) { makeError = FieldError.Required; valid = false }
        if (model.isBlank()) { modelError = FieldError.Required; valid = false }
        if (licensePlate.isBlank()) { licensePlateError = FieldError.Required; valid = false }

        val yearInt = year.toIntOrNull()
        when {
            year.isBlank() -> { yearError = FieldError.Required; valid = false }
            yearInt == null || yearInt < 1900 || yearInt > 2030 -> {
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
                licensePlate = licensePlate.trim().uppercase(),
                color = color.trim().ifBlank { null },
                photoUri = photoUri,
                currentKm = currentKm.toIntOrNull(),
                testExpiryDate = testExpiryDate,
                insuranceExpiryDate = insuranceExpiryDate,
                notes = notes.trim().ifBlank { null }
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
