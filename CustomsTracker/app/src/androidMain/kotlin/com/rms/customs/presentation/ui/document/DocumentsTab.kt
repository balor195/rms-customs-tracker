package com.rms.customs.presentation.ui.document

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rms.customs.domain.model.TransactionDocument
import com.rms.customs.domain.model.enums.DocumentType
import com.rms.customs.presentation.ui.LocalUserSession
import com.rms.customs.presentation.ui.theme.CustomsColors
import com.rms.customs.presentation.viewmodel.DocumentViewModel
import com.rms.customs.presentation.viewmodel.UploadState
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.util.UUID

@Composable
fun DocumentsTab(
    currentPhaseNumber: Int,
    canUpload: Boolean,
    viewModel: DocumentViewModel = koinViewModel(),
) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val session = LocalUserSession.current
    val context = LocalContext.current

    var selectedDocType by remember { mutableStateOf<DocumentType?>(null) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = pendingCameraFile
        if (success && file != null) {
            session?.user?.id?.let { userId ->
                selectedDocType?.let { dt -> viewModel.uploadFromFile(file, dt, userId) }
            }
        }
        selectedDocType = null
        pendingCameraFile = null
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = pendingCameraFile ?: return@rememberLauncherForActivityResult
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraLauncher.launch(uri)
        } else {
            pendingCameraFile = null
            selectedDocType = null
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            session?.user?.id?.let { userId ->
                selectedDocType?.let { dt -> viewModel.uploadFromContentUri(it, dt, userId) }
            }
        }
        selectedDocType = null
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            session?.user?.id?.let { userId ->
                selectedDocType?.let { dt -> viewModel.uploadFromContentUri(it, dt, userId) }
            }
        }
        selectedDocType = null
    }

    LaunchedEffect(uploadState) {
        when (val s = uploadState) {
            is UploadState.Success -> {
                uploadError = null
                viewModel.resetUploadState()
            }
            is UploadState.Error -> {
                uploadError = s.message
                viewModel.resetUploadState()
            }
            else -> Unit
        }
    }

    val requiredForPhase = remember(currentPhaseNumber) {
        DocumentType.entries.filter { it.requiredPhase == currentPhaseNumber }
    }
    val uploadedTypes = remember(documents) { documents.map { it.documentType }.toSet() }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uploadError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = uploadError!!,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        if (requiredForPhase.isNotEmpty()) {
            Text(
                text = "المستندات المطلوبة للمرحلة الحالية",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            requiredForPhase.forEach { docType ->
                val isUploaded = docType in uploadedTypes
                RequiredDocRow(
                    docType = docType,
                    isUploaded = isUploaded,
                    canUpload = canUpload && !isUploaded,
                    onUpload = { selectedDocType = docType },
                )
            }
            HorizontalDivider()
        }

        Text(
            text = "المستندات المرفوعة (${documents.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        if (documents.isEmpty()) {
            Text(
                text = "لا توجد مستندات مرفوعة بعد.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            documents.forEach { doc ->
                DocumentCard(
                    document = doc,
                    onView = { openDocument(doc, viewModel, context) },
                    onDelete = if (canUpload) { { viewModel.delete(doc.id) } } else null,
                )
            }
        }

        if (canUpload) {
            OutlinedButton(
                onClick = { selectedDocType = DocumentType.OTHER },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("رفع مستند إضافي")
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    selectedDocType?.let { docType ->
        DocumentUploadSheet(
            docType = docType,
            isUploading = uploadState is UploadState.Loading,
            onCamera = {
                val photoFile = File(
                    context.cacheDir,
                    "customs_documents/${UUID.randomUUID()}.jpg",
                ).also { it.parentFile?.mkdirs() }
                pendingCameraFile = photoFile
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile,
                    )
                    cameraLauncher.launch(uri)
                } else {
                    cameraPermLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onGallery = { galleryLauncher.launch("image/*") },
            onFiles = { fileLauncher.launch(arrayOf("image/*", "application/pdf")) },
            onDismiss = { selectedDocType = null },
        )
    }
}

@Composable
private fun RequiredDocRow(
    docType: DocumentType,
    isUploaded: Boolean,
    canUpload: Boolean,
    onUpload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (isUploaded) "✅" else "⚠️",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = docType.labelAr,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = if (isUploaded) MaterialTheme.colorScheme.onSurface else CustomsColors.Warning,
        )
        if (canUpload) {
            TextButton(onClick = onUpload) { Text("رفع") }
        }
    }
}

private fun openDocument(
    doc: TransactionDocument,
    viewModel: DocumentViewModel,
    context: android.content.Context,
) {
    val uri = viewModel.getFileProviderUri(doc) ?: return
    val mime = when {
        doc.filename.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
        doc.filename.endsWith(".png", ignoreCase = true) -> "image/png"
        else -> "image/jpeg"
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "فتح المستند")) }
}
