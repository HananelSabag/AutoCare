package com.hananelsabag.autocare.export

import android.content.Context
import android.net.Uri
import com.hananelsabag.autocare.data.local.entities.Car
import com.hananelsabag.autocare.data.local.entities.MaintenanceRecord
import com.hananelsabag.autocare.data.local.entities.RecordType
import com.hananelsabag.autocare.data.local.entities.Reminder
import com.hananelsabag.autocare.data.local.entities.ReminderType
import com.hananelsabag.autocare.data.local.entities.TestRecord
import com.hananelsabag.autocare.data.local.entities.VehicleRecord
import com.hananelsabag.autocare.data.local.entities.VehicleRecordType
import com.hananelsabag.autocare.domain.repository.CarRepository
import com.hananelsabag.autocare.domain.repository.MaintenanceRecordRepository
import com.hananelsabag.autocare.domain.repository.ReminderRepository
import com.hananelsabag.autocare.domain.repository.TestRecordRepository
import com.hananelsabag.autocare.domain.repository.VehicleRecordRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject

data class ImportResult(val carsImported: Int)

class JsonImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val carRepository: CarRepository,
    private val maintenanceRepository: MaintenanceRecordRepository,
    private val testRecordRepository: TestRecordRepository,
    private val reminderRepository: ReminderRepository,
    private val vehicleRecordRepository: VehicleRecordRepository
) {

    /**
     * Reads the JSON backup at [uri] and inserts all cars and their data
     * into the local database. Existing data is kept — nothing is deleted.
     * IDs are reset to 0 so Room auto-generates new primary keys.
     *
     * @return [ImportResult] with the count of cars imported.
     * @throws Exception if the JSON is malformed or unreadable.
     */
    suspend fun importFrom(uri: Uri): ImportResult {
        val jsonText = context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader(Charsets.UTF_8).readText()
        } ?: error("Could not read file")

        val root = JSONObject(jsonText)
        val carsArray = root.getJSONArray("cars")
        var carsImported = 0

        for (i in 0 until carsArray.length()) {
            val entry = carsArray.getJSONObject(i)
            val carObj = entry.getJSONObject("car")

            // Insert car with id=0 → Room generates a new id
            val car = Car(
                id = 0,
                make = carObj.getString("make"),
                model = carObj.getString("model"),
                year = carObj.getInt("year"),
                licensePlate = carObj.getString("licensePlate"),
                color = carObj.optString("color").takeIf { it.isNotEmpty() && it != "null" },
                photoUri = carObj.optString("photoUri").takeIf { it.isNotEmpty() && it != "null" },
                currentKm = carObj.optInt("currentKm").takeIf { carObj.has("currentKm") && !carObj.isNull("currentKm") },
                testExpiryDate = carObj.optLong("testExpiryDate").takeIf { carObj.has("testExpiryDate") && !carObj.isNull("testExpiryDate") },
                insuranceExpiryDate = carObj.optLong("insuranceExpiryDate").takeIf { carObj.has("insuranceExpiryDate") && !carObj.isNull("insuranceExpiryDate") },
                notes = carObj.optString("notes").takeIf { it.isNotEmpty() && it != "null" },
                createdAt = carObj.optLong("createdAt", System.currentTimeMillis())
            )
            val newCarId = carRepository.insertCar(car).toInt()
            carsImported++

            // Maintenance records
            val recordsArray = entry.optJSONArray("maintenanceRecords")
            if (recordsArray != null) {
                for (j in 0 until recordsArray.length()) {
                    val r = recordsArray.getJSONObject(j)
                    val recordType = runCatching { RecordType.valueOf(r.getString("type")) }
                        .getOrDefault(RecordType.MAINTENANCE)
                    maintenanceRepository.insertRecord(
                        MaintenanceRecord(
                            id = 0,
                            carId = newCarId,
                            type = recordType,
                            date = r.getLong("date"),
                            description = r.getString("description"),
                            km = r.optInt("km").takeIf { r.has("km") && !r.isNull("km") },
                            costAmount = r.optDouble("costAmount").takeIf { r.has("costAmount") && !r.isNull("costAmount") },
                            notes = r.optString("notes").takeIf { it.isNotEmpty() && it != "null" },
                            receiptUri = r.optString("receiptUri").takeIf { it.isNotEmpty() && it != "null" },
                            createdAt = r.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Test records
            val testArray = entry.optJSONArray("testRecords")
            if (testArray != null) {
                for (j in 0 until testArray.length()) {
                    val t = testArray.getJSONObject(j)
                    testRecordRepository.insert(
                        TestRecord(
                            id = 0,
                            carId = newCarId,
                            date = t.getLong("date"),
                            passed = t.getBoolean("passed"),
                            notes = t.optString("notes").takeIf { it.isNotEmpty() && it != "null" },
                            certificateUri = t.optString("certificateUri").takeIf { it.isNotEmpty() && it != "null" },
                            createdAt = t.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Reminders
            val remindersArray = entry.optJSONArray("reminders")
            if (remindersArray != null) {
                for (j in 0 until remindersArray.length()) {
                    val rem = remindersArray.getJSONObject(j)
                    val reminderType = runCatching { ReminderType.valueOf(rem.getString("type")) }
                        .getOrDefault(ReminderType.SERVICE_DATE)
                    reminderRepository.insertReminder(
                        Reminder(
                            id = 0,
                            carId = newCarId,
                            type = reminderType,
                            enabled = rem.optBoolean("enabled", true),
                            daysBeforeExpiry = rem.optInt("daysBeforeExpiry", 14),
                            createdAt = rem.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Vehicle records (insurance / test docs)
            val vehicleArray = entry.optJSONArray("vehicleRecords")
            if (vehicleArray != null) {
                for (j in 0 until vehicleArray.length()) {
                    val v = vehicleArray.getJSONObject(j)
                    val vType = runCatching { VehicleRecordType.valueOf(v.getString("type")) }
                        .getOrDefault(VehicleRecordType.INSURANCE)
                    vehicleRecordRepository.saveRecord(
                        VehicleRecord(
                            id = 0,
                            carId = newCarId,
                            type = vType,
                            expiryDate = v.getLong("expiryDate"),
                            fileUri = v.optString("fileUri").takeIf { it.isNotEmpty() && it != "null" },
                            isActive = v.optBoolean("isActive", true),
                            createdAt = v.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }

        return ImportResult(carsImported = carsImported)
    }
}
