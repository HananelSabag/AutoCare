package com.hananelsabag.autocare.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hananelsabag.autocare.data.local.entities.Car
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {

    @Query("SELECT * FROM cars ORDER BY displayOrder ASC, createdAt DESC")
    fun getAllCars(): Flow<List<Car>>

    @Query("SELECT COALESCE(MAX(displayOrder), -1) FROM cars")
    suspend fun getMaxDisplayOrder(): Int

    @Query("SELECT * FROM cars WHERE id = :id")
    fun getCarById(id: Int): Flow<Car?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: Car): Long

    @Update
    suspend fun updateCar(car: Car)

    @Delete
    suspend fun deleteCar(car: Car)
}
