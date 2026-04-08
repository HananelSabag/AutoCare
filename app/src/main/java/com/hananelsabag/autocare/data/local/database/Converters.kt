package com.hananelsabag.autocare.data.local.database

import androidx.room.TypeConverter
import com.hananelsabag.autocare.data.local.entities.RecordType
import com.hananelsabag.autocare.data.local.entities.ReminderType
import com.hananelsabag.autocare.data.local.entities.VehicleRecordType

class Converters {

    @TypeConverter
    fun fromRecordType(value: RecordType): String = value.name

    @TypeConverter
    fun toRecordType(value: String): RecordType = RecordType.valueOf(value)

    @TypeConverter
    fun fromReminderType(value: ReminderType): String = value.name

    @TypeConverter
    fun toReminderType(value: String): ReminderType = ReminderType.valueOf(value)

    @TypeConverter
    fun fromVehicleRecordType(value: VehicleRecordType): String = value.name

    @TypeConverter
    fun toVehicleRecordType(value: String): VehicleRecordType = VehicleRecordType.valueOf(value)
}
