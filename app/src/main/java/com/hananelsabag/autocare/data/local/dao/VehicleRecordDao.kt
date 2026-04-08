package com.hananelsabag.autocare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hananelsabag.autocare.data.local.entities.VehicleRecord
import com.hananelsabag.autocare.data.local.entities.VehicleRecordType
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleRecordDao {

    @Query("SELECT * FROM vehicle_records WHERE carId = :carId AND type = :type AND isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    fun getActiveRecord(carId: Int, type: VehicleRecordType): Flow<VehicleRecord?>

    @Query("SELECT * FROM vehicle_records WHERE carId = :carId ORDER BY createdAt DESC")
    fun getAllRecordsForCar(carId: Int): Flow<List<VehicleRecord>>

    @Query("SELECT * FROM vehicle_records WHERE carId = :carId AND type = :type ORDER BY createdAt DESC")
    fun getRecordsByType(carId: Int, type: VehicleRecordType): Flow<List<VehicleRecord>>

    @Query("UPDATE vehicle_records SET isActive = 0 WHERE carId = :carId AND type = :type")
    suspend fun deactivateAllByType(carId: Int, type: VehicleRecordType)

    @Insert
    suspend fun insert(record: VehicleRecord): Long

    @Update
    suspend fun update(record: VehicleRecord)

    @Query("DELETE FROM vehicle_records WHERE id = :id")
    suspend fun deleteById(id: Int)
}
