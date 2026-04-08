package com.hananelsabag.autocare.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val make: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val color: String? = null,
    val photoUri: String? = null,
    // Updated from maintenance records; user can also enter manually on add
    val currentKm: Int? = null,
    // Epoch milliseconds — nullable means "not set"
    val testExpiryDate: Long? = null,
    val insuranceExpiryDate: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
