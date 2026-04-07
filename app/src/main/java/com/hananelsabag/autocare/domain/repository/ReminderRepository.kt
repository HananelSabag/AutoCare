package com.hananelsabag.autocare.domain.repository

import com.hananelsabag.autocare.data.local.entities.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getRemindersForCar(carId: Int): Flow<List<Reminder>>
    suspend fun insertReminder(reminder: Reminder): Long
    suspend fun insertReminders(reminders: List<Reminder>)
    suspend fun updateReminder(reminder: Reminder)
    suspend fun deleteReminder(reminder: Reminder)
    suspend fun deleteAllForCar(carId: Int)
}
