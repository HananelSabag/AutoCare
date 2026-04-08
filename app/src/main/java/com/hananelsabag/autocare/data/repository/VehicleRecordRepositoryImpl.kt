package com.hananelsabag.autocare.data.repository

import com.hananelsabag.autocare.data.local.dao.VehicleRecordDao
import com.hananelsabag.autocare.data.local.entities.VehicleRecord
import com.hananelsabag.autocare.data.local.entities.VehicleRecordType
import com.hananelsabag.autocare.domain.repository.VehicleRecordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class VehicleRecordRepositoryImpl @Inject constructor(
    private val dao: VehicleRecordDao
) : VehicleRecordRepository {

    override fun getActiveRecord(carId: Int, type: VehicleRecordType): Flow<VehicleRecord?> =
        dao.getActiveRecord(carId, type)

    override fun getAllRecordsForCar(carId: Int): Flow<List<VehicleRecord>> =
        dao.getAllRecordsForCar(carId)

    override fun getRecordsByType(carId: Int, type: VehicleRecordType): Flow<List<VehicleRecord>> =
        dao.getRecordsByType(carId, type)

    override suspend fun saveRecord(record: VehicleRecord) {
        // Deactivate all previous records of this type before saving the new one
        dao.deactivateAllByType(record.carId, record.type)
        dao.insert(record)
    }

    override suspend fun deleteRecord(id: Int) {
        dao.deleteById(id)
    }
}
