package com.hananelsabag.autocare.domain.repository

import com.hananelsabag.autocare.data.local.entities.Car
import kotlinx.coroutines.flow.Flow

interface CarRepository {
    fun getAllCars(): Flow<List<Car>>
    fun getCarById(id: Int): Flow<Car?>
    suspend fun insertCar(car: Car): Long
    suspend fun updateCar(car: Car)
    suspend fun deleteCar(car: Car)
}
