package com.rms.customs.presentation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rms.customs.domain.model.enums.UserRole
import com.rms.customs.presentation.ui.dashboard.DashboardScreen
import com.rms.customs.presentation.ui.report.ReportScreen
import com.rms.customs.presentation.ui.transaction.TransactionListScreen
import com.rms.customs.presentation.viewmodel.NotificationViewModel
import com.rms.customs.presentation.viewmodel.SyncState
import com.rms.customs.presentation.viewmodel.SyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    onLogout: () -> Unit,
    notifViewModel: NotificationViewModel = hiltViewModel(),
    syncViewModel: SyncViewModel = hiltViewModel(),
) {
    val session     = LocalUserSession.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val unreadCount by notifViewModel.unreadCount.collectAsStateWithLifecycle()
    val syncState   by syncViewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("متابعة التخليص الجمركي") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.primary,
                    titleContentColor      = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    if (session != null) {
                        Text(
                            text     = session.user.role.labelAr,
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }

                    IconButton(onClick = { navController.navigate(Dest.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
                    }

                    // Sync status — tap to trigger manual sync
                    IconButton(onClick = syncViewModel::sync) {
                        when (syncState) {
                            is SyncState.Syncing -> CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                color       = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            is SyncState.Error -> Icon(
                                imageVector        = Icons.Default.CloudOff,
                                contentDescription = "خطأ في المزامنة — اضغط للمحاولة",
                                tint               = Color(0xFFFF8A80),
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
