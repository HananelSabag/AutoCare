package com.hananelsabag.autocare.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ReminderType {
    TEST_EXPIRY,
    INSURANCE_COMPULSORY_EXPIRY,
    INSURANCE_COMPREHENSIVE_EXPIRY,
    SERVICE_DATE
}

@Entity(
    tableName = "reminders",
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
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val carId: Int,
    val type: ReminderType,
    val enabled: Boolean = true,
    val daysBeforeExpiry: Int = 14,
    val createdAt: Long = System.currentTimeMillis()
)
