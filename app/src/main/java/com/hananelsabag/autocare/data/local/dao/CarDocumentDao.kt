package com.hananelsabag.autocare.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hananelsabag.autocare.data.local.entities.CarDocument
import com.hananelsabag.autocare.data.local.entities.DocumentType
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDocumentDao {

    @Query("SELECT * FROM car_documents WHERE carId = :carId")
    fun getDocumentsForCar(carId: Int): Flow<List<CarDocument>>

    @Query("SELECT * FROM car_documents WHERE carId = :carId AND type = :type LIMIT 1")
    suspend fun getDocument(carId: Int, type: DocumentType): CarDocument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: CarDocument): Long

    @Delete
    suspend fun deleteDocument(document: CarDocument)

    @Query("DELETE FROM car_documents WHERE carId = :carId AND type = :type")
    suspend fun deleteDocumentByType(carId: Int, type: DocumentType)
}
