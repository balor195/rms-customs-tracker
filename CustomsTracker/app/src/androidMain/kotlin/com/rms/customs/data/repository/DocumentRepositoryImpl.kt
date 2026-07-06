package com.rms.customs.data.repository

import com.rms.customs.data.local.dao.ActivityLogDao
import com.rms.customs.data.local.dao.DocumentDao
import com.rms.customs.data.local.entity.ActivityLogEntity
import com.rms.customs.data.local.entity.toDomain
import com.rms.customs.data.local.entity.toEntity
import com.rms.customs.domain.model.TransactionDocument
import com.rms.customs.domain.model.enums.DocumentType
import com.rms.customs.domain.model.enums.LogAction
import com.rms.customs.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class DocumentRepositoryImpl(
    private val documentDao: DocumentDao,
    private val activityLogDao: ActivityLogDao,
) : DocumentRepository {

    override fun observeForTransaction(transactionId: String): Flow<List<TransactionDocument>> =
        documentDao.observeForTransaction(transactionId)
            .map { it.map { e -> e.toDomain() } }

    override fun observeForPhase(transactionId: String, phaseRef: String): Flow<List<TransactionDocument>> =
        documentDao.observeForPhase(transactionId, phaseRef)
            .map { it.map { e -> e.toDomain() } }

    override suspend fun getById(id: String): TransactionDocument? =
        documentDao.getById(id)?.toDomain()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun save(document: TransactionDocument) {
        documentDao.insert(document.toEntity())
        activityLogDao.insert(
            ActivityLogEntity(
                id = Uuid.random().toString(),
                transactionId = document.transactionId,
                userId = document.uploadedByUserId,
                action = LogAction.DOC_UPLOADED.name,
                fromStatus = null,
                toStatus = null,
                payload = """{"docType":"${document.documentType.name}","filename":"${document.filename}"}""",
                occurredAt = document.uploadedAt,
            )
        )
    }

    override suspend fun markVerified(id: String, verifiedByUserId: String) {
        documentDao.markVerified(id)
    }

    override suspend fun delete(id: String) {
        documentDao.delete(id)
    }

    override suspend fun updateRemoteUrl(id: String, remoteUrl: String) {
        documentDao.updateRemoteUrl(id, remoteUrl)
    }

    override suspend fun hasMissingRequiredDocs(
        transactionId: String,
        phaseRef: String,
        types: List<DocumentType>,
    ): Boolean {
        val count = documentDao.countExistingDocs(
            transactionId = transactionId,
            phaseRef = phaseRef,
            types = types.map { it.name },
        )
        return count < types.size
    }
}
