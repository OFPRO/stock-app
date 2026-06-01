package com.app2.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app2.core.ui.ViewState
import com.app2.core.ui.components.StockCard
import com.app2.core.ui.components.StockErrorView
import com.app2.core.ui.components.StockKPICard
import com.app2.core.ui.components.StockSkeletonGrid
import com.app2.core.ui.theme.Accent
import com.app2.core.ui.theme.Brand
import com.app2.core.ui.theme.Error
import com.app2.core.ui.theme.Info
import com.app2.core.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Tableau de Bord") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        when (val s = state) {
            is ViewState.Loading -> {
                Box(modifier = Modifier.padding(16.dp)) {
                    StockSkeletonGrid(columns = 2)
                }
            }
            is ViewState.Loaded -> {
                DashboardContent(
                    data = s.data,
                    onRefresh = { viewModel.loadDashboard() }
                )
            }
            is ViewState.Error -> {
                StockErrorView(
                    message = s.message,
                    onRetry = { viewModel.loadDashboard() }
                )
            }
            is ViewState.Empty -> { /* not used */}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    data: DashboardDisplayData,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiSection(
                data = data
            )
            Spacer(Modifier.height(8.dp))
            if (data.salesChartData.isNotEmpty() || data.topProducts.isNotEmpty()) {
                Text(
                    text = "Graphiques",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(4.dp))
                ChartsSection(data)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun KpiSection(data: DashboardDisplayData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StockKPICard(
            title = "CA Aujourd'hui",
            value = data.caToday,
            icon = Icons.Default.AttachMoney,
            color = Success,
            modifier = Modifier.weight(1f)
        )
        StockKPICard(
            title = "Ventes",
            value = data.salesCount,
            icon = Icons.Default.ShoppingCart,
            color = Info,
            modifier = Modifier.weight(1f)
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StockKPICard(
            title = "Ticket moyen",
            value = data.averageTicket,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            color = Accent,
            modifier = Modifier.weight(1f)
        )
        StockKPICard(
            title = "Marge brute",
            value = data.grossMargin,
            icon = Icons.Default.AccountBalance,
            color = Brand,
            modifier = Modifier.weight(1f)
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StockKPICard(
            title = "Créances",
            value = data.receivables,
            icon = Icons.Default.Warning,
            color = Error,
            modifier = Modifier.weight(1f)
        )
        StockKPICard(
            title = "Taux recouvr.",
            value = data.collectionRate,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            color = Success,
            modifier = Modifier.weight(1f)
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StockKPICard(
            title = "Valeur stock",
            value = data.stockValue,
            icon = Icons.Default.Inventory,
            color = Brand,
            modifier = Modifier.weight(1f)
        )
        StockKPICard(
            title = "Ruptures",
            value = data.stockouts,
            icon = Icons.Default.Warning,
            color = Error,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ChartsSection(data: DashboardDisplayData) {
    if (data.salesChartData.isNotEmpty()) {
        StockCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "Ventes (7 jours)",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                data.salesChartData.forEach { (date, total) ->
                    Text(
                        text = "$date  %.0f MAD".format(total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    if (data.topProducts.isNotEmpty()) {
        StockCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "Top produits",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                data.topProducts.forEach { (name, sold) ->
                    Text(
                        text = "$name — $sold vendus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
