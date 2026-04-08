package com.hananelsabag.autocare.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.hananelsabag.autocare.domain.repository.CarDocumentRepository
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.domain.repository.MaintenanceRecordRepository
import com.hananelsabag.autocare.domain.repository.ReminderRepository
import com.hananelsabag.autocare.domain.repository.TestRecordRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class JsonExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carRepository: CarRepository,
    private val maintenanceRepository: MaintenanceRecordRepository,
    private val testRecordRepository: TestRecordRepository,
    private val reminderRepository: ReminderRepository,
    private val documentRepository: CarDocumentRepository
) {

    suspend fun exportAll(): Intent {
        val cars = carRepository.getAllCars().first()

        val carsArray = JSONArray()
        for (car in cars) {
            val carObj = JSONObject().apply {
                put("id", car.id)
                put("make", car.make)
                put("model", car.model)
                put("year", car.year)
                put("licensePlate", car.licensePlate)
                put("color", car.color ?: JSONObject.NULL)
                put("photoUri", car.photoUri ?: JSONObject.NULL)
                put("currentKm", car.currentKm ?: JSONObject.NULL)
                put("testExpiryDate", car.testExpiryDate ?: JSONObject.NULL)
                put("insuranceExpiryDate", car.insuranceExpiryDate ?: JSONObject.NULL)
                put("notes", car.notes ?: JSONObject.NULL)
                put("createdAt", car.createdAt)
            }

            val recordsArray = JSONArray()
            for (record in maintenanceRepository.getRecordsForCar(car.id).first()) {
                recordsArray.put(JSONObject().apply {
                    put("id", record.id)
                    put("type", record.type.name)
                    put("date", record.date)
                    put("description", record.description)
                    put("km", record.km ?: JSONObject.NULL)
                    put("costAmount", record.costAmount ?: JSONObject.NULL)
                    put("notes", record.notes ?: JSONObject.NULL)
                    put("receiptUri", record.receiptUri ?: JSONObject.NULL)
                    put("createdAt", record.createdAt)
                })
            }

            val testRecordsArray = JSONArray()
            for (test in testRecordRepository.getByCarId(car.id).first()) {
                testRecordsArray.put(JSONObject().apply {
                    put("id", test.id)
                    put("date", test.date)
                    put("passed", test.passed)
                    put("notes", test.notes ?: JSONObject.NULL)
                    put("certificateUri", test.certificateUri ?: JSONObject.NULL)
                    put("createdAt", test.createdAt)
                })
            }

            val remindersArray = JSONArray()
            for (reminder in reminderRepository.getRemindersForCar(car.id).first()) {
                remindersArray.put(JSONObject().apply {
                    put("id", reminder.id)
                    put("type", reminder.type.name)
                    put("enabled", reminder.enabled)
                    put("daysBeforeExpiry", reminder.daysBeforeExpiry)
                    put("createdAt", reminder.createdAt)
                })
            }

            val documentsArray = JSONArray()
            for (doc in documentRepository.getDocumentsForCar(car.id).first()) {
                documentsArray.put(JSONObject().apply {
                    put("id", doc.id)
                    put("type", doc.type.name)
                    put("fileUri", doc.fileUri)
                    put("createdAt", doc.createdAt)
                })
            }

            carsArray.put(JSONObject().apply {
                put("car", carObj)
                put("maintenanceRecords", recordsArray)
                put("testRecords", testRecordsArray)
                put("reminders", remindersArray)
                put("documents", documentsArray)
            })
        }

        val root = JSONObject().apply {
            put("exportedAt", System.currentTimeMillis())
            put("appVersion", "2.0")
            put("cars", carsArray)
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, "autocare_backup_$dateStr.json")
        file.writeText(root.toString(2))

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
