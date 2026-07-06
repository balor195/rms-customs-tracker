package com.rms.customs.domain.repository

import com.rms.customs.domain.model.ActivityLog
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.TransactionStatus
import com.rms.customs.domain.model.enums.UserRole
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    fun observeActive(): Flow<List<Transaction>>
    fun observeByStatus(vararg statuses: TransactionStatus): Flow<List<Transaction>>
    fun observeById(id: String): Flow<Transaction?>
    suspend fun getById(id: String): Transaction?
    suspend fun create(transaction: Transaction)
    suspend fun update(transaction: Transaction)
    suspend fun advanceStatus(
        transactionId: String,
        newStatus: TransactionStatus,
        actorUserId: String,
        actorRole: UserRole,
        payload: String = "{}",
    )
    suspend fun setExceptionState(
        transactionId: String,
        exceptionStatus: TransactionStatus,
        reason: String,
        actorUserId: String,
    )
    suspend fun clearExceptionState(transactionId: String, actorUserId: String)
    fun observeActivityLog(transactionId: String): Flow<List<ActivityLog>>
    suspend fun countByStatus(status: TransactionStatus): Int
    suspend fun generateRef(): String
}
