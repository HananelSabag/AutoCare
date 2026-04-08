package com.hananelsabag.autocare.data.repository

import com.hananelsabag.autocare.data.local.dao.TestRecordDao
import com.hananelsabag.autocare.data.local.entities.TestRecord
import com.hananelsabag.autocare.domain.repository.TestRecordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TestRecordRepositoryImpl @Inject constructor(
    private val dao: TestRecordDao
) : TestRecordRepository {
    override fun getByCarId(carId: Int): Flow<List<TestRecord>> = dao.getByCarId(carId)
    override suspend fun insert(record: TestRecord) = dao.insert(record)
    override suspend fun update(record: TestRecord) = dao.update(record)
    override suspend fun delete(record: TestRecord) = dao.delete(record)
}
