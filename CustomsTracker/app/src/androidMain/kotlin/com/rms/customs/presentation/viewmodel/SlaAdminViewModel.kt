package com.rms.customs.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rms.customs.domain.model.SlaConfig
import com.rms.customs.domain.repository.SlaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SlaAdminViewModel(
    private val slaRepository: SlaRepository,
) : ViewModel() {

    val configs: StateFlow<List<SlaConfig>> = slaRepository
        .observeAllForAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(config: SlaConfig) = viewModelScope.launch { slaRepository.upsert(config) }

    fun setActive(id: String, active: Boolean) = viewModelScope.launch {
        slaRepository.setActive(id, active)
    }
}
