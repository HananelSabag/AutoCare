package com.hananelsabag.autocare.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hananelsabag.autocare.data.local.entities.TestRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TestRecordDao {
    @Query("SELECT * FROM test_records WHERE carId = :carId ORDER BY date DESC")
    fun getByCarId(carId: Int): Flow<List<TestRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TestRecord)

    @Update
    suspend fun update(record: TestRecord)

    @Delete
    suspend fun delete(record: TestRecord)
}
