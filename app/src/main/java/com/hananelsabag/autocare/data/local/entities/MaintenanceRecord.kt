package com.hananelsabag.autocare.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RecordType { MAINTENANCE, REPAIR, WEAR }

@Entity(
    tableName = "maintenance_records",
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
data class MaintenanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val carId: Int,
    val type: RecordType,
    val date: Long,
    val description: String,
    val km: Int? = null,
    val costAmount: Double? = null,
    val notes: String? = null,
    /** URI of a receipt photo / PDF attached to this record (optional) */
    val receiptUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
