package com.rms.customs.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rms.customs.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SyncState {
    object Idle     : SyncState()
    object Syncing  : SyncState()
    data class Success(val lastSyncMs: Long, val pushed: Int, val pulled: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<SyncState>(
        SyncState.Success(syncRepository.getLastSyncTimeMs(), 0, 0)
    )
    val state: StateFlow<SyncState> = _state.asStateFlow()

    fun sync() {
        if (_state.value is SyncState.Syncing) return
        viewModelScope.launch {
            _state.value = SyncState.Syncing
            syncRepository.sync().fold(
                onSuccess = { (pushed, pulled) ->
                    _state.value = SyncState.Success(System.currentTimeMillis(), pushed, pulled)
                },
                onFailure = { e ->
                    _state.value = SyncState.Error(e.message ?: "خطأ في المزامنة")
                },
            )
        }
    }
}
