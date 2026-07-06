package com.rms.customs.domain.repository

import com.rms.customs.domain.model.SlaConfig
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface SlaRepository {
    fun observeAll(): Flow<List<SlaConfig>>
    fun observeAllForAdmin(): Flow<List<SlaConfig>>
    suspend fun getForSubPhase(phaseNumber: Int, subPhase: String): SlaConfig?
    suspend fun upsert(config: SlaConfig)
    suspend fun setActive(id: UUID, isActive: Boolean)
}
