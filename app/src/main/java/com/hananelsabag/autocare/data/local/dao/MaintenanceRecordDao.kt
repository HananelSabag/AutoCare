package com.hananelsabag.autocare.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hananelsabag.autocare.data.local.entities.MaintenanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceRecordDao {

    @Query("SELECT * FROM maintenance_records WHERE carId = :carId ORDER BY date DESC")
    fun getRecordsForCar(carId: Int): Flow<List<MaintenanceRecord>>

    @Query("SELECT date FROM maintenance_records WHERE carId = :carId AND type = 'MAINTENANCE' ORDER BY date DESC LIMIT 1")
    suspend fun getLastMaintenanceDateForCar(carId: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MaintenanceRecord): Long

    @Update
    suspend fun updateRecord(record: MaintenanceRecord)

    @Delete
    suspend fun deleteRecord(record: MaintenanceRecord)
}
