package com.hananelsabag.autocare.data.repository

import com.hananelsabag.autocare.data.local.dao.CarDocumentDao
import com.hananelsabag.autocare.data.local.entities.CarDocument
import com.hananelsabag.autocare.data.local.entities.DocumentType
import com.hananelsabag.autocare.domain.repository.CarDocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CarDocumentRepositoryImpl @Inject constructor(
    private val dao: CarDocumentDao
) : CarDocumentRepository {

    override fun getDocumentsForCar(carId: Int): Flow<List<CarDocument>> =
        dao.getDocumentsForCar(carId)

    override suspend fun insertDocument(document: CarDocument): Long =
        dao.insertDocument(document)

    override suspend fun deleteDocumentByType(carId: Int, type: DocumentType) =
        dao.deleteDocumentByType(carId, type)
}
