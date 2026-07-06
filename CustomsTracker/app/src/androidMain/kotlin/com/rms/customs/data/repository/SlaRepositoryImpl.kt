package com.rms.customs.data.repository

import com.rms.customs.data.local.dao.SlaConfigDao
import com.rms.customs.data.local.entity.toDomain
import com.rms.customs.data.local.entity.toEntity
import com.rms.customs.domain.model.SlaConfig
import com.rms.customs.domain.repository.SlaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SlaRepositoryImpl(
    private val slaConfigDao: SlaConfigDao,
) : SlaRepository {

    override fun observeAll(): Flow<List<SlaConfig>> =
        slaConfigDao.observeAll().map { it.map { e -> e.toDomain() } }

    override fun observeAllForAdmin(): Flow<List<SlaConfig>> =
        slaConfigDao.observeAllForAdmin().map { it.map { e -> e.toDomain() } }

    override suspend fun getForSubPhase(phaseNumber: Int, subPhase: String): SlaConfig? =
        slaConfigDao.getForSubPhase(phaseNumber, subPhase)?.toDomain()

    override suspend fun upsert(config: SlaConfig) {
        slaConfigDao.upsert(config.toEntity())
    }

    override suspend fun setActive(id: UUID, isActive: Boolean) {
        slaConfigDao.setActive(id.toString(), isActive)
    }
}
