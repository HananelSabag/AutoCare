package com.hananelsabag.autocare.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "test_records",
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
data class TestRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val carId: Int,
    val date: Long,
    val passed: Boolean,
    val notes: String? = null,
    /** URI of the test certificate photo / scan (optional) */
    val certificateUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
