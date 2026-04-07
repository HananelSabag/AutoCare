package com.hananelsabag.autocare.data.repository

import com.hananelsabag.autocare.data.local.dao.ReminderDao
import com.hananelsabag.autocare.data.local.entities.Reminder
import com.hananelsabag.autocare.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao
) : ReminderRepository {

    override fun getRemindersForCar(carId: Int): Flow<List<Reminder>> =
        dao.getRemindersForCar(carId)

    override suspend fun insertReminder(reminder: Reminder): Long =
        dao.insertReminder(reminder)

    override suspend fun insertReminders(reminders: List<Reminder>) =
        dao.insertReminders(reminders)

    override suspend fun updateReminder(reminder: Reminder) =
        dao.updateReminder(reminder)

    override suspend fun deleteReminder(reminder: Reminder) =
        dao.deleteReminder(reminder)

    override suspend fun deleteAllForCar(carId: Int) =
        dao.deleteAllForCar(carId)
}
