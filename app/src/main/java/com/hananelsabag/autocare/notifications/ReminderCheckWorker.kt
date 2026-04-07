package com.hananelsabag.autocare.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.ReminderType
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.domain.repository.ReminderRepository
import com.hananelsabag.autocare.util.daysFromNow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ReminderCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val carRepository: CarRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val allCars = carRepository.getAllCars().first()

        allCars.forEach { car ->
            val reminders = reminderRepository.getRemindersForCar(car.id).first()

            reminders.filter { it.enabled }.forEach { reminder ->
                val expiryMs = when (reminder.type) {
                    ReminderType.TEST_EXPIRY                    -> car.testExpiryDate
                    ReminderType.INSURANCE_COMPULSORY_EXPIRY   -> car.insuranceExpiryDate
                    ReminderType.INSURANCE_COMPREHENSIVE_EXPIRY -> car.comprehensiveInsuranceExpiryDate
                    ReminderType.SERVICE_DATE                   -> null // km-based, not date-based yet
                } ?: return@forEach

                val daysLeft = expiryMs.daysFromNow()
                if (daysLeft in 0..reminder.daysBeforeExpiry) {
                    val title = "${car.make} ${car.model}"
                    val message = buildMessage(reminder.type, daysLeft)
                    showReminderNotification(
                        context = context,
                        notificationId = reminder.id,
                        title = title,
                        message = message
                    )
                }
            }
        }

        return Result.success()
    }

    private fun buildMessage(type: ReminderType, daysLeft: Long): String {
        val typeLabel = when (type) {
            ReminderType.TEST_EXPIRY                    -> context.getString(R.string.reminder_type_test_expiry)
            ReminderType.INSURANCE_COMPULSORY_EXPIRY   -> context.getString(R.string.reminder_type_insurance_compulsory)
            ReminderType.INSURANCE_COMPREHENSIVE_EXPIRY -> context.getString(R.string.reminder_type_insurance_comprehensive)
            ReminderType.SERVICE_DATE                   -> context.getString(R.string.reminder_type_service_date)
        }
        return when (daysLeft) {
            0L   -> context.getString(R.string.notification_expires_today, typeLabel)
            1L   -> context.getString(R.string.notification_expires_tomorrow, typeLabel)
            else -> context.getString(R.string.notification_expires_in_days, typeLabel, daysLeft)
        }
    }
}
