package com.hananelsabag.autocare.presentation.screens.carprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.data.local.entities.MaintenanceRecord
import com.hananelsabag.autocare.data.local.entities.RecordType
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.domain.repository.MaintenanceRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class CarStats(
    val totalRecords: Int,
    val totalSpentThisYear: Double?,
    val lastServiceDate: Long?,
    val averageCost: Double?
)

data class TypeBreakdown(val type: RecordType, val count: Int, val totalCost: Double?)
data class MonthlySpend(val label: String, val amount: Double)

data class CarDetailedStats(
    val typeBreakdown: List<TypeBreakdown>,          // only types that have ≥1 record
    val monthlySpend: List<MonthlySpend>,            // last 12 months with cost data
    val kmFrom: Pair<Long, Int>?,                    // (dateMs, km) — earliest record with km
    val kmTo: Pair<Long, Int>?,                      // (dateMs, km) — latest record with km
    val biggestExpense: MaintenanceRecord?
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CarProfileViewModel @Inject constructor(
    private val repository: CarRepository,
    private val recordRepository: MaintenanceRecordRepository
) : ViewModel() {

    private val _carId = MutableStateFlow<Int?>(null)

    val car = _carId
        .filterNotNull()
        .flatMapLatest { id -> repository.getCarById(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val records = _carId
        .filterNotNull()
        .flatMapLatest { id -> recordRepository.getRecordsForCar(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val stats = records
        .map { list -> computeStats(list) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val detailedStats = records
        .map { list -> if (list.isEmpty()) null else computeDetailedStats(list) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // Next periodic service due = last MAINTENANCE record date + 365 days
    val nextServiceDueMs = records
        .map { list ->
            list.filter { it.type == com.hananelsabag.autocare.data.local.entities.RecordType.MAINTENANCE }
                .maxOfOrNull { it.date }
                ?.let { it + 365L * 24 * 60 * 60 * 1000 }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun init(carId: Int) {
        if (_carId.value == null) _carId.value = carId
    }

    fun deleteCar(onDeleted: () -> Unit) {
        viewModelScope.launch {
            car.value?.let { repository.deleteCar(it) }
            onDeleted()
        }
    }

    fun updateCar(car: Car) {
        viewModelScope.launch { repository.updateCar(car) }
    }

    private fun computeDetailedStats(records: List<MaintenanceRecord>): CarDetailedStats {
        val typeBreakdown = RecordType.entries.mapNotNull { type ->
            val filtered = records.filter { it.type == type }
            if (filtered.isEmpty()) null
            else TypeBreakdown(
                type = type,
                count = filtered.size,
                totalCost = filtered.mapNotNull { it.costAmount }.takeIf { it.isNotEmpty() }?.sum()
            )
        }

        val fmt = DateTimeFormatter.ofPattern("MMM yy", Locale.getDefault())
        val monthlySpend = records
            .filter { it.costAmount != null }
            .groupBy {
                val ldt = Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
                Pair(ldt.year, ldt.monthValue)
            }
            .entries
            .sortedWith(compareBy({ it.key.first }, { it.key.second }))
            .takeLast(12)
            .map { (yearMonth, recs) ->
                MonthlySpend(
                    label = YearMonth.of(yearMonth.first, yearMonth.second).format(fmt),
                    amount = recs.sumOf { it.costAmount!! }
                )
            }

        val recordsWithKm = records.filter { it.km != null }.sortedBy { it.date }
        val kmFrom = recordsWithKm.firstOrNull()?.let { Pair(it.date, it.km!!) }
        val kmTo = recordsWithKm.lastOrNull()?.let { Pair(it.date, it.km!!) }
        val biggestExpense = records.filter { it.costAmount != null }.maxByOrNull { it.costAmount!! }

        return CarDetailedStats(
            typeBreakdown = typeBreakdown,
            monthlySpend = monthlySpend,
            kmFrom = kmFrom,
            kmTo = kmTo,
            biggestExpense = biggestExpense
        )
    }

    private fun computeStats(records: List<MaintenanceRecord>): CarStats? {
        if (records.isEmpty()) return null

        val currentYear = java.time.LocalDate.now().year
        val recordsWithCost = records.filter { it.costAmount != null }

        val totalSpentThisYear = records
            .filter { record ->
                val year = Instant.ofEpochMilli(record.date)
                    .atZone(ZoneId.systemDefault())
                    .year
                year == currentYear && record.costAmount != null
            }
            .sumOf { it.costAmount!! }
            .takeIf { recordsWithCost.any { r ->
                Instant.ofEpochMilli(r.date).atZone(ZoneId.systemDefault()).year == currentYear
            }}

        val averageCost = if (recordsWithCost.isNotEmpty())
            recordsWithCost.sumOf { it.costAmount!! } / recordsWithCost.size
        else null

        return CarStats(
            totalRecords = records.size,
            totalSpentThisYear = totalSpentThisYear,
            lastServiceDate = records.maxOfOrNull { it.date },
            averageCost = averageCost
        )
    }
}
