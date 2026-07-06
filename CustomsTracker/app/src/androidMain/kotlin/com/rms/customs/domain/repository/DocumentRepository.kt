package com.rms.customs.domain.repository

import com.rms.customs.domain.model.TransactionDocument
import com.rms.customs.domain.model.enums.DocumentType
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeForTransaction(transactionId: String): Flow<List<TransactionDocument>>
    fun observeForPhase(transactionId: String, phaseRef: String): Flow<List<TransactionDocument>>
    suspend fun getById(id: String): TransactionDocument?
    suspend fun save(document: TransactionDocument)
    suspend fun markVerified(id: String, verifiedByUserId: String)
    suspend fun delete(id: String)
    suspend fun updateRemoteUrl(id: String, remoteUrl: String)
    suspend fun hasMissingRequiredDocs(transactionId: String, phaseRef: String, types: List<DocumentType>): Boolean
}
