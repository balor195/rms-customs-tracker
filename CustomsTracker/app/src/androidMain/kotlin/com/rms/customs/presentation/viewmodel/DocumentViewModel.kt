package com.rms.customs.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.FileProvider
import com.rms.customs.domain.model.TransactionDocument
import com.rms.customs.domain.model.enums.DocumentType
import com.rms.customs.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}

class DocumentViewModel(
    private val documentRepository: DocumentRepository,
    private val appContext: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val txId: UUID = UUID.fromString(requireNotNull(savedStateHandle["id"]))

    val documents: StateFlow<List<TransactionDocument>> =
        documentRepository.observeForTransaction(txId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    fun uploadFromContentUri(uri: Uri, docType: DocumentType, userId: UUID) {
        viewModelScope.launch(Dispatchers.IO) {
            _uploadState.value = UploadState.Loading
            runCatching {
                val extension = getMimeExtension(uri)
                val tempFile = File(appContext.cacheDir, "upload_${UUID.randomUUID()}.$extension")
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error("فشل فتح الملف")
                doUpload(tempFile, docType, userId)
                tempFile.delete()
            }
                .onSuccess { _uploadState.value = UploadState.Success }
                .onFailure { _uploadState.value = UploadState.Error(it.message ?: "خطأ في الرفع") }
        }
    }

    fun uploadFromFile(file: File, docType: DocumentType, userId: UUID) {
        viewModelScope.launch(Dispatchers.IO) {
            _uploadState.value = UploadState.Loading
            runCatching { doUpload(file, docType, userId) }
                .onSuccess { _uploadState.value = UploadState.Success }
                .onFailure { _uploadState.value = UploadState.Error(it.message ?: "خطأ في الرفع") }
        }
    }

    fun delete(docId: UUID) {
        viewModelScope.launch {
            val doc = documents.value.firstOrNull { it.id == docId } ?: return@launch
            File(doc.filePath).delete()
            documentRepository.delete(docId)
        }
    }

    fun getFileProviderUri(doc: TransactionDocument): Uri? {
        val file = File(doc.filePath)
        if (!file.exists()) return null
        return try {
            FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    fun resetUploadState() { _uploadState.value = UploadState.Idle }

    private suspend fun doUpload(sourceFile: File, docType: DocumentType, userId: UUID) {
        val destDir = File(appContext.filesDir, "documents/$txId").apply { mkdirs() }
        val ext = sourceFile.extension.ifBlank { "bin" }
        val destFile = File(destDir, "${UUID.randomUUID()}.$ext")
        sourceFile.copyTo(destFile, overwrite = true)

        if (destFile.isImageFile() && destFile.length() > 2 * 1024 * 1024L) {
            runCatching { compressImage(destFile) }
        }

        documentRepository.save(
            TransactionDocument(
                id = UUID.randomUUID(),
                transactionId = txId,
                phaseRef = docType.requiredPhase.toString(),
                documentType = docType,
                filename = sourceFile.name.ifBlank { "${UUID.randomUUID()}.$ext" },
                filePath = destFile.absolutePath,
                uploadedAt = System.currentTimeMillis(),
                uploadedByUserId = userId,
            )
        )
    }

    private fun File.isImageFile(): Boolean =
        extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")

    private fun getMimeExtension(uri: Uri): String {
        val mime = appContext.contentResolver.getType(uri) ?: return "bin"
        return when {
            "pdf" in mime -> "pdf"
            "png" in mime -> "png"
            else -> "jpg"
        }
    }

    private fun compressImage(file: File) {
        val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath) ?: return
        val out = java.io.ByteArrayOutputStream()
        var quality = 85
        do {
            out.reset()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
            quality -= 10
        } while (out.size() > 2 * 1024 * 1024 && quality > 30)
        file.writeBytes(out.toByteArray())
        bitmap.recycle()
    }
}
