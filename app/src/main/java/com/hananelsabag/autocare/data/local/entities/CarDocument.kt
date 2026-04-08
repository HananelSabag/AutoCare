package com.hananelsabag.autocare.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DocumentType {
    INSURANCE_COMPULSORY,
    VEHICLE_LICENSE
}

@Entity(
    tableName = "car_documents",
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
data class CarDocument(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val carId: Int,
    val type: DocumentType,
    val fileUri: String,
    val createdAt: Long = System.currentTimeMillis()
)
