package com.rms.customs.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rms.customs.data.network.ServerUrlHolder
import com.rms.customs.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(
    private val urlHolder: ServerUrlHolder,
    private val syncRepository: SyncRepository,
) : ViewModel() {

    private val _serverUrl     = MutableStateFlow(urlHolder.readUrl())
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _urlSaved      = MutableStateFlow(false)
    val urlSaved: StateFlow<Boolean> = _urlSaved.asStateFlow()

    private val _syncing       = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _syncResult    = MutableStateFlow<String?>(null)
    val syncResult: StateFlow<String?> = _syncResult.asStateFlow()

    val lastSyncFormatted: String
        get() {
            val ms = syncRepository.getLastSyncTimeMs()
            return if (ms == 0L) "لم يتم المزامنة بعد"
            else SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(ms))
        }

    fun onUrlChange(url: String) {
        _serverUrl.value = url
        _urlSaved.value  = false
    }

    fun saveUrl() {
        val trimmed = _serverUrl.value.trim()
        if (trimmed.isNotEmpty()) {
            urlHolder.saveUrl(trimmed)
            _urlSaved.value = true
        }
    }

    fun syncNow() = viewModelScope.launch {
        _syncing.value = true
        _syncResult.value = null
        syncRepository.sync().fold(
            onSuccess = { (p, r) -> _syncResult.value = "✓ أُرسل $p، استُقبل $r" },
            onFailure = { e  -> _syncResult.value = "✗ ${e.message ?: "فشلت المزامنة"}" },
        )
        _syncing.value = false
    }

    fun clearSyncResult() { _syncResult.value = null }
}
