package com.hananelsabag.autocare.domain.repository

import com.hananelsabag.autocare.data.local.entities.CarDocument
import com.hananelsabag.autocare.data.local.entities.DocumentType
import kotlinx.coroutines.flow.Flow

interface CarDocumentRepository {
    fun getDocumentsForCar(carId: Int): Flow<List<CarDocument>>
    suspend fun insertDocument(document: CarDocument): Long
    suspend fun deleteDocumentByType(carId: Int, type: DocumentType)
}
