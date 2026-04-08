package com.hananelsabag.autocare.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class VehicleRecordType {
    TEST,
    INSURANCE
}

@Entity(
    tableName = "vehicle_records",
    foreignKeys = [
        ForeignKey(
            entity = Car::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId")]
)
data class VehicleRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val carId: Int,
    val type: VehicleRecordType,
    val expiryDate: Long,
    val fileUri: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
