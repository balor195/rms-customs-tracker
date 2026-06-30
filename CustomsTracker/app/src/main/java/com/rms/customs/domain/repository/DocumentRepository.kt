package com.rms.customs.domain.repository

import com.rms.customs.domain.model.TransactionDocument
import com.rms.customs.domain.model.enums.DocumentType
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface DocumentRepository {
    fun observeForTransaction(transactionId: UUID): Flow<List<TransactionDocument>>
    fun observeForPhase(transactionId: UUID, phaseRef: String): Flow<List<TransactionDocument>>
    suspend fun getById(id: UUID): TransactionDocument?
    suspend fun save(document: TransactionDocument)
    suspend fun markVerified(id: UUID, verifiedByUserId: UUID)
    suspend fun delete(id: UUID)
    suspend fun updateRemoteUrl(id: UUID, remoteUrl: String)
    suspend fun hasMissingRequiredDocs(transactionId: UUID, phaseRef: String, types: List<DocumentType>): Boolean
}
