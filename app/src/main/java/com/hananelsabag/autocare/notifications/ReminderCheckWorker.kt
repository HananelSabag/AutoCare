package com.hananelsabag.autocare.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hananelsabag.autocare.R
import com.hananelsabag.autocare.data.local.entities.ReminderType
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.domain.repository.MaintenanceRecordRepository
import com.hananelsabag.autocare.domain.repository.ReminderRepository
import com.hananelsabag.autocare.util.daysFromNow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class ReminderCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val carRepository: CarRepository,
    private val maintenanceRecordRepository: MaintenanceRecordRepository
) : CoroutineWorker(context, params) {

    companion object {
        private val ONE_YEAR_MS = TimeUnit.DAYS.toMillis(365)

        /**
         * Three window tiers — last 7 days always fires regardless of tier:
         *
         *  EARLY (daysBeforeExpiry = 60):  fires at 60, 30, 23, 16, 9, then every run ≤7
         *  MEDIUM (daysBeforeExpiry = 30): fires at 30, 23, 16, 9, then every run ≤7
         *  LAST_WEEK (daysBeforeExpiry = 7): fires every run ≤7 only
         *
         * The last 7 days are always active — cannot be disabled.
         */
        fun shouldFireToday(daysLeft: Long, daysBeforeExpiry: Int): Boolean {
            if (daysLeft < 0) return false
            // Last 7 days always fire, regardless of window setting
            if (daysLeft <= 7L) return true
            // Outside the user's window — skip
            if (daysLeft > daysBeforeExpiry) return false
            return when {
                daysLeft == 60L -> true
                daysLeft == 30L -> true
                daysLeft in 8L..29L && (30L - daysLeft) % 7L == 0L -> true
                else -> false
            }
        }

        /**
         * Stable notification ID per car + reminder type.
         * Uses carId * 10 + type.ordinal so it never changes even when
         * reminders are deleted and re-inserted (which resets DB autoincrement IDs).
         */
        fun notificationId(carId: Int, type: ReminderType): Int = carId * 10 + type.ordinal
    }

    override suspend fun doWork(): Result {
        val allCars = carRepository.getAllCars().first()

        allCars.forEach { car ->
            val reminders = reminderRepository.getRemindersForCar(car.id).first()

            reminders.filter { it.enabled }.forEach { reminder ->
                val expiryMs: Long = when (reminder.type) {
                    ReminderType.TEST_EXPIRY ->
                        car.testExpiryDate ?: return@forEach

                    ReminderType.INSURANCE_EXPIRY ->
                        car.insuranceExpiryDate ?: return@forEach

                    ReminderType.SERVICE_DATE -> {
                        val lastServiceDate = maintenanceRecordRepository
                            .getLastMaintenanceDateForCar(car.id) ?: return@forEach
                        lastServiceDate + ONE_YEAR_MS
                    }
                }

                val daysLeft = expiryMs.daysFromNow()

                if (shouldFireToday(daysLeft, reminder.daysBeforeExpiry)) {
                    showReminderNotification(
                        context = context,
                        notificationId = notificationId(car.id, reminder.type),
                        title = "${car.make} ${car.model}",
                        message = buildMessage(reminder.type, daysLeft)
                    )
                }
            }
        }

        return Result.success()
    }

    private fun buildMessage(type: ReminderType, daysLeft: Long): String {
        val typeLabel = when (type) {
            ReminderType.TEST_EXPIRY ->
                context.getString(R.string.reminder_type_test_expiry)
            ReminderType.INSURANCE_EXPIRY ->
                context.getString(R.string.reminder_type_insurance_compulsory)
            ReminderType.SERVICE_DATE ->
                context.getString(R.string.reminder_type_service_date)
        }
        val base = when {
            daysLeft <= 0L -> context.getString(R.string.notification_expires_today, typeLabel)
            daysLeft == 1L -> context.getString(R.string.notification_expires_tomorrow, typeLabel)
            else           -> context.getString(R.string.notification_expires_in_days, typeLabel, daysLeft)
        }
        return if (type == ReminderType.TEST_EXPIRY) {
            "$base · ${context.getString(R.string.notification_test_reminder_suffix)}"
        } else base
    }
}
