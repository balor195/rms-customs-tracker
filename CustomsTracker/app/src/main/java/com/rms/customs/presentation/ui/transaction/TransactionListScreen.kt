package com.rms.customs.presentation.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.rms.customs.presentation.ui.ShimmerBox
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rms.customs.domain.model.enums.Beneficiary
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.presentation.ui.LocalUserSession
import com.rms.customs.presentation.viewmodel.TransactionListViewModel
import com.rms.customs.presentation.viewmodel.TxFilter

@Composable
fun TransactionListScreen(
    onTransactionClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TransactionListViewModel = hiltViewModel(),
) {
    val uiState   by viewModel.uiState.collectAsStateWithLifecycle()
    val session   = LocalUserSession.current
    val canCreate = session?.user?.role?.canCreateTransaction == true
    val seesAllDivisions = session?.user?.role?.seesAllDivisions == true

    LaunchedEffect(session?.user) {
        session?.user?.let { viewModel.setCurrentUser(it) }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (canCreate) {
                FloatingActionButton(onClick = onCreateClick) {
                    Icon(Icons.Default.Add, contentDescription = "إنشاء معاملة")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Search bar ───────────────────────────────────────────────
            OutlinedTextField(
                value         = uiState.searchQuery,
                onValueChange = viewModel::onSearchChanged,
                leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder   = { Text("بحث بالاعتماد، المورد، الضابط، البوليصة…") },
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // ── Status filter row ────────────────────────────────────────
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(TxFilter.entries.toTypedArray()) { filter ->
                    FilterChip(
                        selected = uiState.selectedFilter == filter,
                        onClick  = { viewModel.onFilterChanged(filter) },
                        label    = { Text(filter.labelAr) },
                    )
                }
            }

            // ── Division filter row (only meaningful for roles that see multiple divisions) ──
            if (seesAllDivisions) {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // "الكل" chip clears division filter
                    item {
                        FilterChip(
                            selected = uiState.selectedDivision == null,
                            onClick  = { viewModel.onDivisionChanged(null) },
                            label    = { Text("كل الشعب", style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                    items(Department.entries.toTypedArray()) { dept ->
                        FilterChip(
                            selected = uiState.selectedDivision == dept,
                            onClick  = {
                                viewModel.onDivisionChanged(if (uiState.selectedDivision == dept) null else dept)
                            },
                            label    = { Text(dept.labelAr, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }

            // ── Beneficiary filter row ───────────────────────────────────
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedBeneficiary == null,
                        onClick  = { viewModel.onBeneficiaryChanged(null) },
                        label    = { Text("كل الجهات", style = MaterialTheme.typography.labelSmall) },
                    )
                }
                items(Beneficiary.entries.toTypedArray()) { ben ->
                    FilterChip(
                        selected = uiState.selectedBeneficiary == ben,
                        onClick  = {
                            viewModel.onBeneficiaryChanged(
                                if (uiState.selectedBeneficiary == ben) null else ben
                            )
                        },
                        label    = { Text(ben.labelAr, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            HorizontalDivider()

            // ── Transaction list ─────────────────────────────────────────
            when {
                uiState.isLoading -> LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(5) { TransactionCardSkeleton() }
                }

                uiState.transactions.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text  = "لا توجد معاملات",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val hasActiveFilter = uiState.selectedFilter != TxFilter.ALL
                            || uiState.searchQuery.isNotEmpty()
                            || uiState.selectedDivision != null
                            || uiState.selectedBeneficiary != null
                        if (hasActiveFilter) {
                            Text(
                                text  = "جرِّب تغيير الفلتر أو مسح البحث",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                else -> LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.transactions, key = { it.id.toString() }) { tx ->
                        TransactionCard(
                            transaction = tx,
                            onClick     = { onTransactionClick(tx.id.toString()) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionCardSkeleton() {
    Card(
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            ShimmerBox(modifier = Modifier.width(4.dp).fillMaxHeight())
            Column(
                modifier            = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ShimmerBox(Modifier.fillMaxWidth(0.5f).height(14.dp).clip(RoundedCornerShape(4.dp)))
                ShimmerBox(Modifier.fillMaxWidth(0.72f).height(12.dp).clip(RoundedCornerShape(4.dp)))
                ShimmerBox(Modifier.fillMaxWidth(0.88f).height(12.dp).clip(RoundedCornerShape(4.dp)))
                Spacer(Modifier.height(2.dp))
                ShimmerBox(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)))
            }
        }
    }
}
