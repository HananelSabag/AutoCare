package com.hananelsabag.autocare.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hananelsabag.autocare.data.local.dao.CarDao
import com.hananelsabag.autocare.data.local.dao.MaintenanceRecordDao
import com.hananelsabag.autocare.data.local.dao.ReminderDao
import com.hananelsabag.autocare.data.local.dao.TestRecordDao
import com.hananelsabag.autocare.data.local.dao.VehicleRecordDao
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.data.local.entities.MaintenanceRecord
import com.hananelsabag.autocare.data.local.entities.Reminder
import com.hananelsabag.autocare.data.local.entities.TestRecord
import com.hananelsabag.autocare.data.local.entities.VehicleRecord

@Database(
    entities = [Car::class, MaintenanceRecord::class, Reminder::class, TestRecord::class, VehicleRecord::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun maintenanceRecordDao(): MaintenanceRecordDao
    abstract fun reminderDao(): ReminderDao
    abstract fun testRecordDao(): TestRecordDao
    abstract fun vehicleRecordDao(): VehicleRecordDao
}
