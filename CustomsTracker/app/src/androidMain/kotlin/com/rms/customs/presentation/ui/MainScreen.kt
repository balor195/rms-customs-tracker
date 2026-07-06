package com.rms.customs.presentation.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rms.customs.presentation.ui.dashboard.DashboardScreen
import com.rms.customs.presentation.ui.report.ReportScreen
import com.rms.customs.presentation.ui.theme.CustomsColors
import com.rms.customs.presentation.ui.transaction.TransactionListScreen
import com.rms.customs.presentation.viewmodel.NotificationViewModel
import com.rms.customs.presentation.viewmodel.SyncState
import com.rms.customs.presentation.viewmodel.SyncViewModel
import org.koin.androidx.compose.koinViewModel

// Light salmon-red for sync error state — legible against the dark navy top bar
private val TopBarErrorTint = Color(0xFFFF8A80)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    onLogout: () -> Unit,
    onExitViewAs: () -> Unit,
    notifViewModel: NotificationViewModel = koinViewModel(),
    syncViewModel: SyncViewModel = koinViewModel(),
) {
    val session     = LocalUserSession.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val unreadCount by notifViewModel.unreadCount.collectAsStateWithLifecycle()
    val syncState   by syncViewModel.state.collectAsStateWithLifecycle()

    val syncTransition = rememberInfiniteTransition(label = "sync_spin")
    val syncRotation   by syncTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing)),
        label         = "sync_rotation",
    )

    val tabTitle = when (selectedTab) {
        0    -> "المعاملات"
        1    -> "لوحة التحكم"
        else -> "التقارير"
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text       = tabTitle,
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onPrimary,
                            )
                            Text(
                                text  = "الخدمات الطبية الملكية",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.60f),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor         = MaterialTheme.colorScheme.primary,
                        titleContentColor      = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    actions = {
                        if (session != null) {
                            RoleChip(label = session.user.role.labelAr)
                        }

                        IconButton(onClick = { navController.navigate(Dest.SETTINGS) }) {
                            Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
                        }

                        // Sync status — tap to trigger manual sync
                        IconButton(onClick = syncViewModel::sync) {
                            when (syncState) {
                                is SyncState.Syncing -> Icon(
                                    imageVector        = Icons.Default.Sync,
                                    contentDescription = "جارية المزامنة",
                                    modifier           = Modifier.size(20.dp).rotate(syncRotation),
                                )
                                is SyncState.Error -> Icon(
                                    imageVector        = Icons.Default.CloudOff,
                                    contentDescription = "خطأ في المزامنة — اضغط للمحاولة",
                                    tint               = TopBarErrorTint,
                                )
                                else -> Icon(
                                    imageVector        = Icons.Default.CloudDone,
                                    contentDescription = "المزامنة — اضغط للمزامنة الآن",
                                )
                            }
                        }

                        IconButton(onClick = { navController.navigate(Dest.NOTIFICATIONS) }) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "الإشعارات")
                            }
                        }

                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "تسجيل الخروج")
                        }
                    },
                )
                // Gold accent line — institutional signature from the brand palette
                HorizontalDivider(
                    thickness = 1.5.dp,
                    color     = CustomsColors.GoldAccent,
                )
                if (session?.isViewingAs == true) {
                    ViewAsBanner(
                        roleLabel       = session.user.role.labelAr,
                        departmentLabel = session.user.department?.labelAr,
                        onExit          = onExitViewAs,
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    icon     = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null) },
                    label    = { Text("المعاملات") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    icon     = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label    = { Text("لوحة التحكم") },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick  = { selectedTab = 2 },
                    icon     = { Icon(Icons.Default.Assessment, contentDescription = null) },
                    label    = { Text("التقارير") },
                )
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            0    -> TransactionListScreen(
                onTransactionClick = { txId -> navController.navigate(Dest.transactionDetail(txId)) },
                onCreateClick      = { navController.navigate(Dest.CREATE) },
                isRefreshing       = syncState is SyncState.Syncing,
                onRefresh          = syncViewModel::sync,
                modifier           = Modifier.padding(innerPadding),
            )
            1    -> DashboardScreen(
                onTransactionClick = { txId -> navController.navigate(Dest.transactionDetail(txId)) },
                modifier           = Modifier.padding(innerPadding),
            )
            else -> ReportScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun ViewAsBanner(
    roleLabel: String,
    departmentLabel: String?,
    onExit: () -> Unit,
) {
    Row(
        modifier            = Modifier
            .fillMaxWidth()
            .background(CustomsColors.GoldAccent.copy(alpha = 0.16f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment   = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = Icons.Default.Science,
            contentDescription = null,
            tint               = CustomsColors.GoldAccent,
            modifier           = Modifier.size(16.dp),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text       = "وضع تجربة: $roleLabel" + (departmentLabel?.let { " — $it" } ?: ""),
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface,
            modifier   = Modifier.weight(1f),
        )
        TextButton(
            onClick  = onExit,
            colors   = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        ) {
            Text("الرجوع كمسؤول", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun RoleChip(label: String) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .background(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp),
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.20f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color      = Color.White.copy(alpha = 0.90f),
        )
    }
}
