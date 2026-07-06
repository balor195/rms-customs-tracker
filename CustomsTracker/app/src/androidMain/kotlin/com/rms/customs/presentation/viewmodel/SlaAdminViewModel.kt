package com.rms.customs.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rms.customs.domain.model.SlaConfig
import com.rms.customs.domain.repository.SlaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SlaAdminViewModel @Inject constructor(
    private val slaRepository: SlaRepository,
) : ViewModel() {

    val configs: StateFlow<List<SlaConfig>> = slaRepository
        .observeAllForAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(config: SlaConfig) = viewModelScope.launch { slaRepository.upsert(config) }

    fun setActive(id: UUID, active: Boolean) = viewModelScope.launch {
        slaRepository.setActive(id, active)
    }
}
