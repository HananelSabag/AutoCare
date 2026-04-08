package com.hananelsabag.autocare.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hananelsabag.autocare.data.local.dao.CarDao
import com.hananelsabag.autocare.data.local.dao.MaintenanceRecordDao
import com.hananelsabag.autocare.data.local.dao.ReminderDao
import com.hananelsabag.autocare.data.local.dao.TestRecordDao
import com.hananelsabag.autocare.data.local.dao.VehicleRecordDao
import com.hananelsabag.autocare.data.local.database.AppDatabase
import com.hananelsabag.autocare.data.repository.CarRepositoryImpl
import com.hananelsabag.autocare.data.repository.MaintenanceRecordRepositoryImpl
import com.hananelsabag.autocare.data.repository.ReminderRepositoryImpl
import com.hananelsabag.autocare.data.repository.TestRecordRepositoryImpl
import com.hananelsabag.autocare.data.repository.VehicleRecordRepositoryImpl
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.domain.repository.MaintenanceRecordRepository
import com.hananelsabag.autocare.domain.repository.ReminderRepository
import com.hananelsabag.autocare.domain.repository.TestRecordRepository
import com.hananelsabag.autocare.domain.repository.VehicleRecordRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ── Migrations ────────────────────────────────────────────────────────────────
//
// Provides a safe upgrade path from ANY old version (1-6) to the current v7.
// Uses CREATE TABLE IF NOT EXISTS so the migration is idempotent.
// New columns from intermediate versions are added with try/catch because
// SQLite doesn't support ADD COLUMN IF NOT EXISTS.
//
// Rule going forward: every schema change MUST add a new migration here
// and bump AppDatabase.version. Never use fallbackToDestructiveMigration again.

private fun safeMigrateTo7(from: Int) = object : Migration(from, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ── Create tables that may not exist yet ──────────────────────────

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cars` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `make` TEXT NOT NULL,
                `model` TEXT NOT NULL,
                `year` INTEGER NOT NULL,
                `licensePlate` TEXT NOT NULL,
                `color` TEXT,
                `photoUri` TEXT,
                `currentKm` INTEGER,
                `testExpiryDate` INTEGER,
                `insuranceExpiryDate` INTEGER,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `maintenance_records` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `carId` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `date` INTEGER NOT NULL,
                `description` TEXT NOT NULL,
                `km` INTEGER,
                `costAmount` REAL,
                `notes` TEXT,
                `receiptUri` TEXT,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_records_carId` ON `maintenance_records`(`carId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `reminders` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `carId` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `enabled` INTEGER NOT NULL,
                `daysBeforeExpiry` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_carId` ON `reminders`(`carId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `test_records` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `carId` INTEGER NOT NULL,
                `date` INTEGER NOT NULL,
                `passed` INTEGER NOT NULL,
                `notes` TEXT,
                `certificateUri` TEXT,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_test_records_carId` ON `test_records`(`carId`)")

        // vehicle_records replaces the old car_documents table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `vehicle_records` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `carId` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `expiryDate` INTEGER NOT NULL,
                `fileUri` TEXT,
                `isActive` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_records_carId` ON `vehicle_records`(`carId`)")

        // Drop old car_documents table if it existed
        db.execSQL("DROP TABLE IF EXISTS `car_documents`")

        // ── Add columns that may be missing from older schema versions ────
        // SQLite has no ADD COLUMN IF NOT EXISTS, so we catch and ignore duplicates.
        fun tryAdd(sql: String) = try { db.execSQL(sql) } catch (_: Exception) {}

        tryAdd("ALTER TABLE `cars` ADD COLUMN `color` TEXT")
        tryAdd("ALTER TABLE `cars` ADD COLUMN `photoUri` TEXT")
        tryAdd("ALTER TABLE `cars` ADD COLUMN `currentKm` INTEGER")
        tryAdd("ALTER TABLE `cars` ADD COLUMN `testExpiryDate` INTEGER")
        tryAdd("ALTER TABLE `cars` ADD COLUMN `insuranceExpiryDate` INTEGER")
        tryAdd("ALTER TABLE `cars` ADD COLUMN `notes` TEXT")
        tryAdd("ALTER TABLE `maintenance_records` ADD COLUMN `km` INTEGER")
        tryAdd("ALTER TABLE `maintenance_records` ADD COLUMN `costAmount` REAL")
        tryAdd("ALTER TABLE `maintenance_records` ADD COLUMN `notes` TEXT")
        tryAdd("ALTER TABLE `maintenance_records` ADD COLUMN `receiptUri` TEXT")
        tryAdd("ALTER TABLE `reminders` ADD COLUMN `daysBeforeExpiry` INTEGER NOT NULL DEFAULT 14")
        tryAdd("ALTER TABLE `test_records` ADD COLUMN `notes` TEXT")
        tryAdd("ALTER TABLE `test_records` ADD COLUMN `certificateUri` TEXT")
    }
}

// One migration object per source version so Room finds the direct path.
private val MIGRATIONS_TO_7 = Array(6) { i -> safeMigrateTo7(i + 1) }

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "autocare.db")
            .addMigrations(*MIGRATIONS_TO_7)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            })
            .build()

    @Provides
    fun provideCarDao(db: AppDatabase): CarDao = db.carDao()

    @Provides
    fun provideMaintenanceRecordDao(db: AppDatabase): MaintenanceRecordDao = db.maintenanceRecordDao()

    @Provides
    fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun provideTestRecordDao(db: AppDatabase): TestRecordDao = db.testRecordDao()

    @Provides
    fun provideVehicleRecordDao(db: AppDatabase): VehicleRecordDao = db.vehicleRecordDao()

    @Provides
    @Singleton
    fun provideCarRepository(impl: CarRepositoryImpl): CarRepository = impl

    @Provides
    @Singleton
    fun provideMaintenanceRecordRepository(impl: MaintenanceRecordRepositoryImpl): MaintenanceRecordRepository = impl

    @Provides
    @Singleton
    fun provideReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository = impl

    @Provides
    @Singleton
    fun provideTestRecordRepository(impl: TestRecordRepositoryImpl): TestRecordRepository = impl

    @Provides
    @Singleton
    fun provideVehicleRecordRepository(impl: VehicleRecordRepositoryImpl): VehicleRecordRepository = impl
}
