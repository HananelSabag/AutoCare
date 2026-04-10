package com.hananelsabag.autocare.presentation.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.data.local.entities.Reminder
import com.hananelsabag.autocare.data.local.entities.ReminderType
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.domain.repository.MaintenanceRecordRepository
import com.hananelsabag.autocare.domain.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hananelsabag.autocare.notifications.ReminderCheckWorker
import com.hananelsabag.autocare.util.daysFromNow
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class ReminderWindow(val days: Int) {
    EARLY(60),
    MEDIUM(30),
    LAST_WEEK(7);

    companion object {
        fun fromDays(days: Int): ReminderWindow = when {
            days >= 60 -> EARLY
            days >= 30 -> MEDIUM
            else       -> LAST_WEEK
        }
    }
}

data class ReminderUiState(
    val enabled: Boolean,
    val window: ReminderWindow
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CarRemindersViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val reminderRepository: ReminderRepository,
    private val maintenanceRecordRepository: MaintenanceRecordRepository
) : ViewModel() {

    private val _carId = MutableStateFlow<Int?>(null)

    val car = _carId
        .filterNotNull()
        .flatMapLatest { id -> carRepository.getCarById(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // Mutable form state — initialized once from DB, then owned by the UI until Save.
    // Lives in the ViewModel so it survives recomposition and DB flow re-emissions.
    private val _formState = MutableStateFlow(
        // Sensible defaults while loading (TEST_EXPIRY on, others off, EARLY window)
        ReminderType.entries.associateWith { type ->
            ReminderUiState(enabled = type == ReminderType.TEST_EXPIRY, window = ReminderWindow.EARLY)
        }
    )
    val formState = _formState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    // Last MAINTENANCE record date (null = no records).
    // Exposed so the UI can compute the next-service countdown for SERVICE_DATE.
    private val _lastMaintenanceDate = MutableStateFlow<Long?>(null)
    val lastMaintenanceDate = _lastMaintenanceDate
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private var formInitialized = false

    fun init(carId: Int) {
        if (_carId.value != null) return
        _carId.value = carId
        viewModelScope.launch {
            // One-time read to seed the form with existing saved reminder settings
            if (!formInitialized) {
                formInitialized = true
                val saved = reminderRepository.getRemindersForCar(carId).first()
                _formState.value = ReminderType.entries.associateWith { type ->
                    val existing = saved.find { it.type == type }
                    ReminderUiState(
                        enabled = existing?.enabled ?: (type == ReminderType.TEST_EXPIRY),
                        window = ReminderWindow.fromDays(existing?.daysBeforeExpiry ?: 60)
                    )
                }
            }
            _lastMaintenanceDate.value =
                maintenanceRecordRepository.getLastMaintenanceDateForCar(carId)
        }
    }

    fun updateEnabled(type: ReminderType, enabled: Boolean) {
        _formState.update { current ->
            current.toMutableMap().also {
                it[type] = (it[type] ?: ReminderUiState(false, ReminderWindow.EARLY)).copy(enabled = enabled)
            }
        }
    }

    fun updateWindow(type: ReminderType, window: ReminderWindow) {
        _formState.update { current ->
            current.toMutableMap().also {
                it[type] = (it[type] ?: ReminderUiState(false, ReminderWindow.EARLY)).copy(window = window)
            }
        }
    }

    fun saveReminders(onSaved: () -> Unit) {
        val carId = _carId.value ?: return
        viewModelScope.launch {
            val reminders = _formState.value.map { (type, state) ->
                Reminder(
                    carId = carId,
                    type = type,
                    enabled = state.enabled,
                    daysBeforeExpiry = state.window.days
                )
            }
            reminderRepository.deleteAllForCar(carId)
            reminderRepository.insertReminders(reminders)
            onSaved()
        }
    }

    fun enableDefaultReminders(car: Car) {
        viewModelScope.launch {
            val defaults = buildDefaultReminders(car)
            reminderRepository.deleteAllForCar(car.id)
            reminderRepository.insertReminders(defaults)
        }
    }

    companion object {
        val ONE_YEAR_MS: Long = TimeUnit.DAYS.toMillis(365)

        fun serviceDueDate(lastMaintenanceDate: Long?): Long? =
            lastMaintenanceDate?.plus(ONE_YEAR_MS)

        /**
         * Returns the epoch-millis of the next scheduled notification for this window + expiry.
         * Mirrors [ReminderCheckWorker.shouldFireToday] logic exactly.
         * Returns null if expiry is unknown or already past.
         * Returns [System.currentTimeMillis] if a notification would fire at the next worker run.
         */
        fun nextFireDateMs(expiryMs: Long?, window: ReminderWindow): Long? {
            if (expiryMs == null) return null
            val daysLeft = expiryMs.daysFromNow()
            if (daysLeft < 0) return null
            // Scan from today downward to find the next day that would trigger
            for (d in daysLeft downTo 0L) {
                if (ReminderCheckWorker.shouldFireToday(d, window.days)) {
                    val daysUntilFire = daysLeft - d
                    return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysUntilFire)
                }
            }
            return null
        }

        fun buildDefaultReminders(car: Car): List<Reminder> =
            listOf(
                Reminder(
                    carId = car.id,
                    type = ReminderType.TEST_EXPIRY,
                    enabled = true,
                    daysBeforeExpiry = ReminderWindow.EARLY.days
                ),
                Reminder(
                    carId = car.id,
                    type = ReminderType.INSURANCE_EXPIRY,
                    enabled = car.insuranceExpiryDate != null,
                    daysBeforeExpiry = ReminderWindow.EARLY.days
                ),
                Reminder(
                    carId = car.id,
                    type = ReminderType.SERVICE_DATE,
                    enabled = true,
                    daysBeforeExpiry = ReminderWindow.EARLY.days
                )
            )
    }
}
