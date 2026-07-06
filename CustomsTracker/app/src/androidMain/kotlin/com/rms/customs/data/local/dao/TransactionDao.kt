package com.rms.customs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rms.customs.data.local.entity.ActivityLogEntity
import com.rms.customs.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE currentStatus NOT IN ('CLOSED') AND exceptionState IS NULL ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE currentStatus IN (:statuses) ORDER BY updatedAt DESC")
    fun observeByStatus(statuses: List<String>): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionEntity)

    @Update
    suspend fun update(entity: TransactionEntity)

    @Transaction
    suspend fun advanceStatus(updated: TransactionEntity, log: ActivityLogEntity) {
        update(updated)
        insertLog(log)
    }

    @Insert
    suspend fun insertLog(log: ActivityLogEntity)

    @Query("SELECT COUNT(*) FROM transactions WHERE currentStatus = :status")
    suspend fun countByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE transactionRef LIKE :prefix || '%'")
    suspend fun countWithPrefix(prefix: String): Int

    @Query("""SELECT * FROM transactions WHERE
        transactionRef LIKE '%' || :q || '%' OR supplierName LIKE '%' || :q || '%'
        ORDER BY updatedAt DESC""")
    fun search(q: String): Flow<List<TransactionEntity>>

    // ── Sync helpers ────────────────────────────────────────────────────────

    @Query("SELECT * FROM transactions WHERE updatedAt > :since ORDER BY updatedAt ASC")
    suspend fun getModifiedSince(since: Long): List<TransactionEntity>

    @Query("UPDATE transactions SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun bumpUpdatedAt(id: String, updatedAt: Long)
}
