package com.rms.customs.presentation.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.UserRole
import com.rms.customs.presentation.ui.LocalUserSession
import com.rms.customs.presentation.ui.RequireRole
import com.rms.customs.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToSlaAdmin: () -> Unit,
    onNavigateToUserManagement: () -> Unit,
    onViewAs: (UserRole, Department?) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val session    = LocalUserSession.current
    val serverUrl  by viewModel.serverUrl.collectAsStateWithLifecycle()
    val urlSaved   by viewModel.urlSaved.collectAsStateWithLifecycle()
    val syncing    by viewModel.syncing.collectAsStateWithLifecycle()
    val syncResult by viewModel.syncResult.collectAsStateWithLifecycle()
    val snackbar   = remember { SnackbarHostState() }

    LaunchedEffect(syncResult) {
        syncResult?.let {
            snackbar.showSnackbar(it)
            viewModel.clearSyncResult()
        }
    }
    LaunchedEffect(urlSaved) {
        if (urlSaved) snackbar.showSnackbar("✓ تم حفظ عنوان الخادم")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Server section ────────────────────────────────────────────────
            SettingsCard(title = "الاتصال بالخادم", icon = Icons.Default.Cloud) {
                Text(
                    text  = "عنوان خادم المزامنة",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value         = serverUrl,
                    onValueChange = viewModel::onUrlChange,
                    singleLine    = true,
                    placeholder   = { Text("http://10.0.2.2:8000/") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier      = Modifier.fillMaxWidth(),
                )
                Text(
                    text  = "⚠ يُطبَّق التغيير فوراً على الطلبات القادمة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick  = viewModel::saveUrl,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("  حفظ العنوان", style = MaterialTheme.typography.labelLarge)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier            = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text("آخر مزامنة", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(viewModel.lastSyncFormatted, style = MaterialTheme.typography.bodySmall)
                    }
                    FilledTonalButton(
                        onClick  = viewModel::syncNow,
                        enabled  = !syncing,
                    ) {
                        if (syncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("  مزامنة الآن")
                        }
                    }
                }
            }

            // ── Admin section (ADMIN only) ─────────────────────────────────────
            RequireRole(UserRole.ADMIN) {
                SettingsCard(title = "الإدارة", icon = Icons.Default.AdminPanelSettings) {
                    FilledTonalButton(
                        onClick  = onNavigateToUserManagement,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("  إدارة المستخدمين")
                    }
                    FilledTonalButton(
                        onClick  = onNavigateToSlaAdmin,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Text("  أهداف الوقت (SLA)")
                    }
                }
            }

            // ── View-as / test mode (ADMIN only, in-memory, not persisted) ─────
            RequireRole(UserRole.ADMIN) {
                ViewAsCard(onViewAs = onViewAs)
            }

            // ── App info ─────────────────────────────────────────────────────
            SettingsCard(title = "حول التطبيق", icon = Icons.Default.Info) {
                InfoRow("الجهة", "مديرية الصيدلة والتزويد الطبّي")
                InfoRow("المؤسسة", "الخدمات الطبية الملكية — الأردن")
                InfoRow("الإصدار", "1.0.0")
                InfoRow("المنصة", "Android — Jetpack Compose + Hilt + Room")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewAsCard(onViewAs: (UserRole, Department?) -> Unit) {
    val viewAsRoles = remember { UserRole.entries.filter { it != UserRole.ADMIN } }
    var role       by remember { mutableStateOf(viewAsRoles.first()) }
    var department by remember { mutableStateOf(Department.PHARMACY) }
    var roleExpanded by remember { mutableStateOf(false) }
    var deptExpanded  by remember { mutableStateOf(false) }

    SettingsCard(title = "وضع التجربة", icon = Icons.Default.Science) {
        Text(
            text  = "تصفّح التطبيق مؤقتاً كأنك حساب من نوع آخر — لغايات التجربة فقط، ولا يُحفظ بعد إغلاق التطبيق",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
            OutlinedTextField(
                value = role.labelAr, onValueChange = {},
                readOnly = true, label = { Text("الدور") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roleExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                viewAsRoles.forEach { r ->
                    DropdownMenuItem(
                        text = { Text(r.labelAr) },
                        onClick = { role = r; roleExpanded = false },
                    )
                }
            }
        }

        if (!role.seesAllDivisions) {
            ExposedDropdownMenuBox(expanded = deptExpanded, onExpandedChange = { deptExpanded = it }) {
                OutlinedTextField(
                    value = department.labelAr, onValueChange = {},
                    readOnly = true, label = { Text("الشعبة") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(deptExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = deptExpanded, onDismissRequest = { deptExpanded = false }) {
                    Department.entries.forEach { d ->
                        DropdownMenuItem(
                            text = { Text(d.labelAr) },
                            onClick = { department = d; deptExpanded = false },
                        )
                    }
                }
            }
        }

        Button(
            onClick  = { onViewAs(role, if (role.seesAllDivisions) null else department) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("  تفعيل وضع التجربة")
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(
                    text       = "  $title",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary,
                )
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium)
    }
}
