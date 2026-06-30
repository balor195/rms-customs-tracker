@file:OptIn(ExperimentalMaterial3Api::class)

package com.rms.customs.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rms.customs.data.GovTrack
import com.rms.customs.data.MockData
import com.rms.customs.data.Phase4Detail
import com.rms.customs.data.TrackStatus
import com.rms.customs.ui.theme.*

@Composable
fun PhaseTrackingScreen(detail: Phase4Detail = MockData.phase4Detail) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorSurface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TransactionHeader(detail)
        PhaseStatusBanner(detail)
        SectionLabel("المسارات المتوازية — المرحلة الرابعة")
        detail.tracks.forEach { GovTrackCard(it) }
        HardGateNotice()
    }
}

// ─── Transaction header card ─────────────────────────────────────────────────

@Composable
private fun TransactionHeader(detail: Phase4Detail) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SlaChip(daysElapsed = detail.totalDaysInPhase, slaTarget = detail.slaTargetDays)
                Text(
                    text = detail.transactionRef,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detail.supplierName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
            Text(
                text = "المرحلة 4 — الجهات الحكومية",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

// ─── Overall phase status banner ─────────────────────────────────────────────

@Composable
private fun PhaseStatusBanner(detail: Phase4Detail) {
    val accent = if (detail.isDelayed) ColorDelayed else ColorOk
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, accent, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = if (detail.isDelayed)
                    "⚠️  متأخرة ${detail.daysOverdue} أيام عن هدف SLA (${detail.slaTargetDays} يوم)"
                else "✅  ضمن الهدف الزمني",
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = "إجمالي أيام المرحلة: ${detail.totalDaysInPhase} يوم  •  تكتمل عند اكتمال المسارات الثلاثة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Individual gov-agency track card ────────────────────────────────────────

private data class TrackVisuals(val color: Color, val label: String, val emoji: String)

private fun trackVisuals(track: GovTrack): TrackVisuals {
    val days = track.startedDaysAgo ?: 0
    return when (track.status) {
        TrackStatus.DONE       -> TrackVisuals(ColorOk,      "مكتمل",           "✅")
        TrackStatus.BLOCKED    -> TrackVisuals(ColorDelayed,  "معلق",            "🚫")
        TrackStatus.PENDING    -> TrackVisuals(Color.Gray,    "لم يبدأ",         "○")
        TrackStatus.IN_PROGRESS ->
            if (days > track.slaTargetDays)
                TrackVisuals(ColorDelayed, "جارٍ — متأخر",   "🔴")
            else
                TrackVisuals(ColorWarning, "جارٍ",            "⟳")
    }
}

@Composable
private fun GovTrackCard(track: GovTrack) {
    val vis = trackVisuals(track)
    val isOverdue = (track.startedDaysAgo ?: 0) > track.slaTargetDays

    val cardMod = if (track.isBottleneck && track.status == TrackStatus.IN_PROGRESS)
        Modifier.fillMaxWidth().border(2.dp, ColorDelayed, RoundedCornerShape(12.dp))
    else
        Modifier.fillMaxWidth()

    Card(
        modifier = cardMod,
        elevation = CardDefaults.cardElevation(if (track.isBottleneck) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Bottleneck banner
            if (track.isBottleneck) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorDelayed.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "⚠️  نقطة الاختناق الرئيسية — مصادقة إلزامية في كل معاملة",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorDelayed,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Entity name + status + day counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: day counter
                if (track.startedDaysAgo != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${track.startedDaysAgo}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = vis.color
                        )
                        Text(
                            text = "يوم",
                            style = MaterialTheme.typography.labelSmall,
                            color = vis.color
                        )
                        if (isOverdue) {
                            Text(
                                text = "+${track.startedDaysAgo - track.slaTargetDays} تأخر",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorDelayed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Right: name + status badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${vis.emoji}  ${vis.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = vis.color,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.entityNameAr,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = track.entityNameEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // SLA progress bar
            if (track.startedDaysAgo != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val progress = (track.startedDaysAgo.toFloat() / track.slaTargetDays).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = vis.color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SLA: ${track.slaTargetDays} يوم",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "بدأ منذ ${track.startedDaysAgo} أيام",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Notes / reference number
            if (track.notes != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = track.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// ─── Hard-gate notice at the bottom ─────────────────────────────────────────

@Composable
private fun HardGateNotice() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF8E1), RoundedCornerShape(8.dp))
            .border(1.dp, ColorWarning, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "🔒  بوابة إلزامية: لا يمكن إصدار أمر الإفراج الجمركي قبل اكتمال مصادقة القيادة العامة",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5D4037),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
    }
}

// ─── Utility composables ─────────────────────────────────────────────────────

@Composable
private fun SlaChip(daysElapsed: Int, slaTarget: Int) {
    val color = if (daysElapsed > slaTarget) ColorDelayed else ColorOk
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$daysElapsed / $slaTarget يوم",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.End
    )
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 400, heightDp = 900, locale = "ar")
@Composable
fun PhaseTrackingPreview() {
    com.rms.customs.ui.theme.CustomsTheme {
        PhaseTrackingScreen()
    }
}
