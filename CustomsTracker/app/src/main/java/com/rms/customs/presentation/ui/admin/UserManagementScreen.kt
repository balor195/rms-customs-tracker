package com.rms.customs.presentation.ui.admin

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rms.customs.domain.model.User
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.UserRole
import com.rms.customs.presentation.ui.LocalUserSession
import com.rms.customs.presentation.viewmodel.CreateUserForm
import com.rms.customs.presentation.viewmodel.UserManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBack: () -> Unit,
    viewModel: UserManagementViewModel = hiltViewModel(),
) {
    val session       = LocalUserSession.current
    val users         by viewModel.users.collectAsStateWithLifecycle()
    val errorMessage  by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val snackbar      = remember { SnackbarHostState() }
    var showCreate    by rememberSaveable { mutableStateOf(false) }
    var roleDialogUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(successMessage) {
        successMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة المستخدمين") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "إضافة مستخدم")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        if (users.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("لا يوجد مستخدمون نشطون", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier            = Modifier.padding(innerPadding),
                contentPadding      = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(users, key = { it.id.toString() }) { user ->
                    UserCard(
                        user         = user,
                        isSelf       = user.id == session?.user?.id,
                        onChangeRole = { roleDialogUser = user },
                        onDeactivate = { viewModel.deactivate(user.id) },
                    )
                }
            }
        }
    }

    // Create user dialog
    if (showCreate) {
        CreateUserDialog(
            onDismiss = { showCreate = false },
            onCreate  = { form -> viewModel.createUser(form); showCreate = false },
        )
    }

    // Change role dialog
    roleDialogUser?.let { user ->
        ChangeRoleDialog(
            user      = user,
            onDismiss = { roleDialogUser = null },
            onConfirm = { role, department ->
                viewModel.updateRole(user.id, role, department)
                roleDialogUser = null
            },
        )
    }
}

@Composable
private fun UserCard(
    user: User,
    isSelf: Boolean,
    onChangeRole: () -> Unit,
    onDeactivate: () -> Unit,
) {
    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar circle
            Surface(
                shape  = CircleShape,
                color  = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text  = user.displayNameAr.firstOrNull()?.toString() ?: "؟",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.displayNameAr, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold)
                Text("@${user.username}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text  = user.department?.let { "${user.role.labelAr} — ${it.labelAr}" } ?: user.role.labelAr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Actions
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onChangeRole, enabled = !isSelf) {
                    Text("تغيير الدور", style = MaterialTheme.typography.labelSmall)
                }
                if (!isSelf) {
                    IconButton(onClick = onDeactivate, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.PersonOff, contentDescription = "تعطيل",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Text("(أنت)", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onCreate: (CreateUserForm) -> Unit,
) {
    var form by remember { mutableStateOf(CreateUserForm()) }
    var roleExpanded by remember { mutableStateOf(false) }
    var deptExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مستخدم جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.displayNameAr, onValueChange = { form = form.copy(displayNameAr = it) },
                    label = { Text("الاسم بالعربية *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.displayNameEn, onValueChange = { form = form.copy(displayNameEn = it) },
                    label = { Text("الاسم بالإنجليزية") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.username, onValueChange = { form = form.copy(username = it) },
                    label = { Text("اسم المستخدم *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.password, onValueChange = { form = form.copy(password = it) },
                    label = { Text("كلمة المرور * (8+ أحرف)") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                // Role selector
                ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                    OutlinedTextField(
                        value = form.role.labelAr, onValueChange = {},
                        readOnly = true, label = { Text("الدور") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        UserRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.labelAr) },
                                onClick = { form = form.copy(role = role); roleExpanded = false },
                            )
                        }
                    }
                }
                // Department selector — hidden for roles that see every division
                if (!form.role.seesAllDivisions) {
                    ExposedDropdownMenuBox(expanded = deptExpanded, onExpandedChange = { deptExpanded = it }) {
                        OutlinedTextField(
                            value = form.department.labelAr, onValueChange = {},
                            readOnly = true, label = { Text("الشعبة *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(deptExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                        )
                        ExposedDropdownMenu(expanded = deptExpanded, onDismissRequest = { deptExpanded = false }) {
                            Department.entries.forEach { dept ->
                                DropdownMenuItem(
                                    text = { Text(dept.labelAr) },
                                    onClick = { form = form.copy(department = dept); deptExpanded = false },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(form) }) { Text("إنشاء") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeRoleDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: (UserRole, Department?) -> Unit,
) {
    var selectedRole by remember { mutableStateOf(user.role) }
    var selectedDept by remember { mutableStateOf(user.department ?: Department.PHARMACY) }
    var roleExpanded by remember { mutableStateOf(false) }
    var deptExpanded  by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغيير دور: ${user.displayNameAr}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                    OutlinedTextField(
                        value = selectedRole.labelAr, onValueChange = {},
                        readOnly = true, label = { Text("الدور الجديد") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        UserRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.labelAr) },
                                onClick = { selectedRole = role; roleExpanded = false },
                            )
                        }
                    }
                }
                if (!selectedRole.seesAllDivisions) {
                    ExposedDropdownMenuBox(expanded = deptExpanded, onExpandedChange = { deptExpanded = it }) {
                        OutlinedTextField(
                            value = selectedDept.labelAr, onValueChange = {},
                            readOnly = true, label = { Text("الشعبة *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(deptExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                        )
                        ExposedDropdownMenu(expanded = deptExpanded, onDismissRequest = { deptExpanded = false }) {
                            Department.entries.forEach { dept ->
                                DropdownMenuItem(
                                    text = { Text(dept.labelAr) },
                                    onClick = { selectedDept = dept; deptExpanded = false },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedRole, selectedDept) }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
