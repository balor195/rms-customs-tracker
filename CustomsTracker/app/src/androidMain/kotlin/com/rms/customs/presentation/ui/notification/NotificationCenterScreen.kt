package com.rms.customs.presentation.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rms.customs.domain.model.AppNotification
import com.rms.customs.domain.model.NotificationType
import com.rms.customs.presentation.viewmodel.NotifFilter
import com.rms.customs.presentation.viewmodel.NotificationViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onTransactionClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: NotificationViewModel = koinViewModel(),
) {
    val notifications  by viewModel.filtered.collectAsStateWithLifecycle()
    val unreadCount    by viewModel.unreadCount.collectAsStateWithLifecycle()
    val currentFilter  by viewModel.filter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = if (unreadCount > 0) "الإشعارات ($unreadCount غير مقروء)" else "الإشعارات",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = viewModel::markAllRead) {
                            Text("تحديد الكل كمقروء", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor     = MaterialTheme.colorScheme.primary,
                    titleContentColor  = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Filter chips
            LazyRow(
                modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = currentFilter == NotifFilter.ALL,
                        onClick  = { viewModel.setFilter(NotifFilter.ALL) },
                        label    = { Text("الكل") },
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == NotifFilter.UNREAD,
                        onClick  = { viewModel.setFilter(NotifFilter.UNREAD) },
                        label    = { Text("غير مقروء") },
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == NotifFilter.SLA,
                        onClick  = { viewModel.setFilter(NotifFilter.SLA) },
                        label    = { Text("تجاوز SLA") },
                    )
                }
            }

            HorizontalDivider()

            if (notifications.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector        = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier           = Modifier.size(64.dp),
                            tint               = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text  = "لا توجد إشعارات",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(notifications, key = { it.id.toString() }) { notif ->
                        NotificationItem(
                            notification     = notif,
                            onClick          = {
                                viewModel.markRead(notif.id)
                                onTransactionClick(notif.transactionId.toString())
                            },
                            onMarkRead       = { viewModel.markRead(notif.id) },
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: AppNotification,
    onClick: () -> Unit,
    onMarkRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEscalated = notification.type == NotificationType.SLA_ESCALATED
    val isSla       = notification.type == NotificationType.SLA_BREACH || isEscalated
    val iconTint    = when {
        isEscalated -> Color(0xFFD32F2F)
        isSla       -> Color(0xFFF57C00)
        else        -> MaterialTheme.colorScheme.primary
    }
    val bgColor = if (!notification.isRead)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    else
        Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Unread dot
        Box(
            modifier = Modifier.padding(top = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            } else {
                Box(Modifier.size(8.dp))
            }
        }

        Spacer(Modifier.width(8.dp))

        // Icon
        Icon(
            imageVector        = if (isSla) Icons.Default.Warning else Icons.Default.Notifications,
            contentDescription = null,
            tint               = iconTint,
            modifier           = Modifier.size(20.dp).padding(top = 2.dp),
        )

        Spacer(Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = notification.titleAr,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = notification.messageAr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = formatTime(notification.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        if (!notification.isRead) {
            TextButton(
                onClick  = onMarkRead,
                modifier = Modifier.align(Alignment.CenterVertically),
            ) {
                Text("قراءة", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun formatTime(epochMs: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(epochMs))
}
