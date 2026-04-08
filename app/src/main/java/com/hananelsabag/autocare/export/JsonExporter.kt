package com.hananelsabag.autocare.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.domain.repository.MaintenanceRecordRepository
import com.hananelsabag.autocare.domain.repository.ReminderRepository
import com.hananelsabag.autocare.domain.repository.TestRecordRepository
import com.hananelsabag.autocare.domain.repository.VehicleRecordRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class BackupResult(
    /** Display name of the saved file, e.g. "autocare_backup_2025-01-01.json" */
    val fileName: String,
    /** URI for sharing the file afterwards if the user taps Share */
    val shareUri: Uri
)

class JsonExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carRepository: CarRepository,
    private val maintenanceRepository: MaintenanceRecordRepository,
    private val testRecordRepository: TestRecordRepository,
    private val reminderRepository: ReminderRepository,
    private val vehicleRecordRepository: VehicleRecordRepository
) {

    /**
     * Exports all data as JSON, saves to Downloads folder, and returns a [BackupResult]
     * containing the file name (for snackbar display) and a share URI.
     */
    suspend fun exportAll(): BackupResult {
        val json = buildJson()

        val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
        val fileName = "autocare_backup_$dateStr.json"
        val jsonBytes = json.toString(2).toByteArray(Charsets.UTF_8)

        val shareUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+: save to public Downloads via MediaStore (no permission needed)
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val collectionUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = context.contentResolver.insert(collectionUri, values)
                ?: error("MediaStore insert returned null")

            context.contentResolver.openOutputStream(itemUri)!!.use { it.write(jsonBytes) }

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(itemUri, values, null, null)

            itemUri
        } else {
            // API 26–28: write to app-specific external storage, then expose via FileProvider
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.cacheDir
            downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            file.writeBytes(jsonBytes)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        return BackupResult(fileName = fileName, shareUri = shareUri)
    }

    // ── JSON builder ──────────────────────────────────────────────────────────

    private suspend fun buildJson(): JSONObject {
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

            val vehicleRecordsArray = JSONArray()
            for (record in vehicleRecordRepository.getAllRecordsForCar(car.id).first()) {
                vehicleRecordsArray.put(JSONObject().apply {
                    put("id", record.id)
                    put("type", record.type.name)
                    put("expiryDate", record.expiryDate)
                    put("fileUri", record.fileUri ?: JSONObject.NULL)
                    put("isActive", record.isActive)
                    put("createdAt", record.createdAt)
                })
            }

            carsArray.put(JSONObject().apply {
                put("car", carObj)
                put("maintenanceRecords", recordsArray)
                put("testRecords", testRecordsArray)
                put("reminders", remindersArray)
                put("vehicleRecords", vehicleRecordsArray)
            })
        }

        return JSONObject().apply {
            put("exportedAt", System.currentTimeMillis())
            put("appVersion", "2.0")
            put("cars", carsArray)
        }
    }

    /** Returns a share [Intent] for the given URI so the user can forward the backup. */
    fun buildShareIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
