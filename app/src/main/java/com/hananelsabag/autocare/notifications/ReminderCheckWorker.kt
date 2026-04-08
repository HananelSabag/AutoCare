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
         * Escalation schedule:
         *  - daysLeft == 60                 → fire (first early warning)
         *  - daysLeft == 30                 → fire (one-month warning)
         *  - daysLeft in 8..29, every 7d   → fire (weekly: 23, 16, 9)
         *  - daysLeft in 0..7              → fire every run (~twice daily with 12h worker)
         *
         * The user's [daysBeforeExpiry] acts as the outermost gate:
         * if daysLeft > daysBeforeExpiry the event is not yet in the user's window.
         */
        fun shouldFireToday(daysLeft: Long, daysBeforeExpiry: Int): Boolean {
            if (daysLeft < 0) return false
            if (daysLeft > daysBeforeExpiry) return false
            return when {
                daysLeft <= 7L -> true
                daysLeft == 30L -> true
                daysLeft == 60L && daysBeforeExpiry >= 60 -> true
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
