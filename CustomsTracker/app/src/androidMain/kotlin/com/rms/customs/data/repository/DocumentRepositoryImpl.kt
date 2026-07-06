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
import java.util.UUID

class DocumentRepositoryImpl(
    private val documentDao: DocumentDao,
    private val activityLogDao: ActivityLogDao,
) : DocumentRepository {

    override fun observeForTransaction(transactionId: UUID): Flow<List<TransactionDocument>> =
        documentDao.observeForTransaction(transactionId.toString())
            .map { it.map { e -> e.toDomain() } }

    override fun observeForPhase(transactionId: UUID, phaseRef: String): Flow<List<TransactionDocument>> =
        documentDao.observeForPhase(transactionId.toString(), phaseRef)
            .map { it.map { e -> e.toDomain() } }

    override suspend fun getById(id: UUID): TransactionDocument? =
        documentDao.getById(id.toString())?.toDomain()

    override suspend fun save(document: TransactionDocument) {
        documentDao.insert(document.toEntity())
        activityLogDao.insert(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                transactionId = document.transactionId.toString(),
                userId = document.uploadedByUserId.toString(),
                action = LogAction.DOC_UPLOADED.name,
                fromStatus = null,
                toStatus = null,
                payload = """{"docType":"${document.documentType.name}","filename":"${document.filename}"}""",
                occurredAt = document.uploadedAt,
            )
        )
    }

    override suspend fun markVerified(id: UUID, verifiedByUserId: UUID) {
        documentDao.markVerified(id.toString())
    }

    override suspend fun delete(id: UUID) {
        documentDao.delete(id.toString())
    }

    override suspend fun updateRemoteUrl(id: UUID, remoteUrl: String) {
        documentDao.updateRemoteUrl(id.toString(), remoteUrl)
    }

    override suspend fun hasMissingRequiredDocs(
        transactionId: UUID,
        phaseRef: String,
        types: List<DocumentType>,
    ): Boolean {
        val count = documentDao.countExistingDocs(
            transactionId = transactionId.toString(),
            phaseRef = phaseRef,
            types = types.map { it.name },
        )
        return count < types.size
    }
}
