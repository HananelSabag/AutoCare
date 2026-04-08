package com.hananelsabag.autocare.domain.repository

import com.hananelsabag.autocare.data.local.entities.VehicleRecord
import com.hananelsabag.autocare.data.local.entities.VehicleRecordType
import kotlinx.coroutines.flow.Flow

interface VehicleRecordRepository {
    fun getActiveRecord(carId: Int, type: VehicleRecordType): Flow<VehicleRecord?>
    fun getAllRecordsForCar(carId: Int): Flow<List<VehicleRecord>>
    fun getRecordsByType(carId: Int, type: VehicleRecordType): Flow<List<VehicleRecord>>
    suspend fun saveRecord(record: VehicleRecord)
    suspend fun deleteRecord(id: Int)
}
