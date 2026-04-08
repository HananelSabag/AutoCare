package com.hananelsabag.autocare.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hananelsabag.autocare.data.local.dao.CarDao
import com.hananelsabag.autocare.data.local.dao.CarDocumentDao
import com.hananelsabag.autocare.data.local.dao.MaintenanceRecordDao
import com.hananelsabag.autocare.data.local.dao.ReminderDao
import com.hananelsabag.autocare.data.local.dao.TestRecordDao
import com.hananelsabag.autocare.data.local.database.AppDatabase
import com.hananelsabag.autocare.data.repository.CarDocumentRepositoryImpl
import com.hananelsabag.autocare.data.repository.CarRepositoryImpl
import com.hananelsabag.autocare.data.repository.MaintenanceRecordRepositoryImpl
import com.hananelsabag.autocare.data.repository.ReminderRepositoryImpl
import com.hananelsabag.autocare.data.repository.TestRecordRepositoryImpl
import com.hananelsabag.autocare.domain.repository.CarDocumentRepository
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.domain.repository.MaintenanceRecordRepository
import com.hananelsabag.autocare.domain.repository.ReminderRepository
import com.hananelsabag.autocare.domain.repository.TestRecordRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "autocare.db")
            .fallbackToDestructiveMigration()
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
    fun provideCarDocumentDao(db: AppDatabase): CarDocumentDao = db.carDocumentDao()

    @Provides
    fun provideTestRecordDao(db: AppDatabase): TestRecordDao = db.testRecordDao()

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
    fun provideCarDocumentRepository(impl: CarDocumentRepositoryImpl): CarDocumentRepository = impl

    @Provides
    @Singleton
    fun provideTestRecordRepository(impl: TestRecordRepositoryImpl): TestRecordRepository = impl
}
