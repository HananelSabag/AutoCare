package com.hananelsabag.autocare.domain.repository

import com.hananelsabag.autocare.data.local.entities.MaintenanceRecord
import kotlinx.coroutines.flow.Flow

interface MaintenanceRecordRepository {
    fun getRecordsForCar(carId: Int): Flow<List<MaintenanceRecord>>
    suspend fun insertRecord(record: MaintenanceRecord): Long
    suspend fun updateRecord(record: MaintenanceRecord)
    suspend fun deleteRecord(record: MaintenanceRecord)
}
