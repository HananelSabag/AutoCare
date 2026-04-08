package com.hananelsabag.autocare.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hananelsabag.autocare.data.local.dao.CarDao
import com.hananelsabag.autocare.data.local.dao.CarDocumentDao
import com.hananelsabag.autocare.data.local.dao.MaintenanceRecordDao
import com.hananelsabag.autocare.data.local.dao.ReminderDao
import com.hananelsabag.autocare.data.local.dao.TestRecordDao
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.data.local.entities.CarDocument
import com.hananelsabag.autocare.data.local.entities.MaintenanceRecord
import com.hananelsabag.autocare.data.local.entities.Reminder
import com.hananelsabag.autocare.data.local.entities.TestRecord

@Database(
    entities = [Car::class, MaintenanceRecord::class, Reminder::class, CarDocument::class, TestRecord::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun maintenanceRecordDao(): MaintenanceRecordDao
    abstract fun reminderDao(): ReminderDao
    abstract fun carDocumentDao(): CarDocumentDao
    abstract fun testRecordDao(): TestRecordDao
}
