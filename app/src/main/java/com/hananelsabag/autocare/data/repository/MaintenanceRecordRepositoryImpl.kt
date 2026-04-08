package com.hananelsabag.autocare.data.repository

import com.hananelsabag.autocare.data.local.dao.MaintenanceRecordDao
import com.hananelsabag.autocare.data.local.entities.MaintenanceRecord
import com.hananelsabag.autocare.domain.repository.MaintenanceRecordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MaintenanceRecordRepositoryImpl @Inject constructor(
    private val dao: MaintenanceRecordDao
) : MaintenanceRecordRepository {

    override fun getRecordsForCar(carId: Int): Flow<List<MaintenanceRecord>> =
        dao.getRecordsForCar(carId)

    override suspend fun getLastMaintenanceDateForCar(carId: Int): Long? =
        dao.getLastMaintenanceDateForCar(carId)

    override suspend fun insertRecord(record: MaintenanceRecord): Long =
        dao.insertRecord(record)

    override suspend fun updateRecord(record: MaintenanceRecord) =
        dao.updateRecord(record)

    override suspend fun deleteRecord(record: MaintenanceRecord) =
        dao.deleteRecord(record)
}
