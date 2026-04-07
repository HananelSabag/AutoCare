package com.hananelsabag.autocare.data.repository

import com.hananelsabag.autocare.data.local.dao.CarDao
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.domain.repository.CarRepository
import javax.inject.Inject

class CarRepositoryImpl @Inject constructor(
    private val carDao: CarDao
) : CarRepository {
    override fun getAllCars() = carDao.getAllCars()
    override fun getCarById(id: Int) = carDao.getCarById(id)
    override suspend fun insertCar(car: Car) = carDao.insertCar(car)
    override suspend fun updateCar(car: Car) = carDao.updateCar(car)
    override suspend fun deleteCar(car: Car) = carDao.deleteCar(car)
}
