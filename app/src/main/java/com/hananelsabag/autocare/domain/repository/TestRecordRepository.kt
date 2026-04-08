package com.hananelsabag.autocare.domain.repository

import com.hananelsabag.autocare.data.local.entities.TestRecord
import kotlinx.coroutines.flow.Flow

interface TestRecordRepository {
    fun getByCarId(carId: Int): Flow<List<TestRecord>>
    suspend fun insert(record: TestRecord)
    suspend fun update(record: TestRecord)
    suspend fun delete(record: TestRecord)
}
