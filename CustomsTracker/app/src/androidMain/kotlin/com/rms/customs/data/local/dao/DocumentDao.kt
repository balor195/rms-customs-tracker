package com.rms.customs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rms.customs.data.local.entity.TransactionDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM transaction_documents WHERE transactionId = :transactionId ORDER BY uploadedAt DESC")
    fun observeForTransaction(transactionId: String): Flow<List<TransactionDocumentEntity>>

    @Query("SELECT * FROM transaction_documents WHERE transactionId = :transactionId AND phaseRef = :phaseRef ORDER BY uploadedAt DESC")
    fun observeForPhase(transactionId: String, phaseRef: String): Flow<List<TransactionDocumentEntity>>

    @Query("SELECT * FROM transaction_documents WHERE id = :id")
    suspend fun getById(id: String): TransactionDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionDocumentEntity)

    @Update
    suspend fun update(entity: TransactionDocumentEntity)

    @Query("UPDATE transaction_documents SET isVerified = 1 WHERE id = :id")
    suspend fun markVerified(id: String)

    @Query("DELETE FROM transaction_documents WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE transaction_documents SET remoteUrl = :url WHERE id = :id")
    suspend fun updateRemoteUrl(id: String, url: String)

    @Query("SELECT COUNT(*) FROM transaction_documents WHERE transactionId = :transactionId AND phaseRef = :phaseRef AND documentType IN (:types)")
    suspend fun countExistingDocs(transactionId: String, phaseRef: String, types: List<String>): Int
}
