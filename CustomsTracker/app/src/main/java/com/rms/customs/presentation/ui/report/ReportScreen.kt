package com.rms.customs.presentation.ui.report

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rms.customs.presentation.viewmodel.ExportState
import com.rms.customs.presentation.viewmodel.ReportType
import com.rms.customs.presentation.viewmodel.ReportViewModel

@Composable
fun ReportScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val context      = LocalContext.current
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val exportState  by viewModel.exportState.collectAsStateWithLifecycle()
    val snackbar     = remember { SnackbarHostState() }

    // Auto-launch share sheet when file is ready
    LaunchedEffect(exportState) {
        if (exportState is ExportState.Ready) {
            val ready = exportState as ExportState.Ready
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = ready.mimeType
                putExtra(Intent.EXTRA_STREAM, ready.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة التقرير"))
            viewModel.clearState()
        } else if (exportState is ExportState.Error) {
            snackbar.showSnackbar((exportState as ExportState.Error).message)
            viewModel.clearState()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Section: choose report type
            Text(
                text       = "اختر نوع التقرير",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary,
            )

            ReportType.entries.forEach { type ->
                ReportTypeCard(
                    type       = type,
                    selected   = type == selectedType,
                    onClick    = { viewModel.selectType(type) },
                )
            }

            Spacer(Modifier.height(8.dp))

            // Section: export actions
            Text(
                text       = "تصدير التقرير",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExportButton(
                    label    = "PDF",
                    icon     = Icons.Default.PictureAsPdf,
                    enabled  = exportState !is ExportState.Generating,
                    onClick  = viewModel::exportPdf,
                    modifier = Modifier.weight(1f),
                )
                ExportButton(
                    label    = "CSV / Excel",
                    icon     = Icons.Default.TableChart,
                    enabled  = exportState !is ExportState.Generating,
                    onClick  = viewModel::exportCsv,
                    modifier = Modifier.weight(1f),
                )
            }

            // Info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text  = "ملاحظة حول التصدير",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = "• PDF: ملف مُنسَّق مناسب للطباعة والتوزيع الرسمي\n" +
                                "• CSV: جدول بيانات يفتح مع Excel أو Google Sheets\n" +
                                "• سيُفتح قائمة المشاركة تلقائياً عند الانتهاء",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Loading overlay
        if (exportState is ExportState.Generating) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier            = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                        Text("جارٍ إنشاء التقرير…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ReportTypeCard(
    type: ReportType,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val icon: ImageVector = when (type) {
        ReportType.WEEKLY             -> Icons.AutoMirrored.Filled.Article
        ReportType.MONTHLY            -> Icons.Default.Assessment
        ReportType.EXECUTIVE          -> Icons.Default.PictureAsPdf
        ReportType.BY_OFFICER         -> Icons.Default.Person
        ReportType.EXPECTED_SHIPMENTS -> Icons.Default.DateRange
        ReportType.VALUE_BY_DIVISION  -> Icons.Default.BarChart
    }
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                         else MaterialTheme.colorScheme.surface

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border   = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        colors   = CardDefaults.outlinedCardColors(containerColor = containerColor),
        shape    = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                modifier           = Modifier.size(28.dp),
                tint               = if (selected) MaterialTheme.colorScheme.primary
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text       = type.labelAr,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color      = if (selected) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text  = type.descAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExportButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.height(48.dp),
        shape    = RoundedCornerShape(10.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
