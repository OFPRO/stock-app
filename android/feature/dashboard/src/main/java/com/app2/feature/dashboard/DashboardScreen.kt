package com.app2.feature.dashboard

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.app2.core.ui.theme.BrandDark
import com.app2.core.ui.theme.Error
import com.app2.core.ui.theme.Info
import com.app2.core.ui.theme.Success
import com.app2.core.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.pdfEvent.collect { uri ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

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
                    onRefresh = { viewModel.loadDashboard() },
                    isExporting = isExporting,
                    onExportPdf = { viewModel.exportTablePdf(it) }
                )
            }
            is ViewState.Error -> {
                StockErrorView(message = s.message, onRetry = { viewModel.loadDashboard() })
            }
            is ViewState.Empty -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    data: DashboardDisplayData,
    onRefresh: () -> Unit,
    isExporting: Set<String>,
    onExportPdf: (String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiSection(data = data)

            SectionTitle("Ventes (7 jours)")
            SalesChart(data = data.salesChartData)

            if (data.topProducts.isNotEmpty() || data.categoryData.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (data.topProducts.isNotEmpty()) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionTitle("Top produits")
                            HorizontalBarChart(
                                items = data.topProducts.map { it.name to it.qtySold },
                                color = Brand
                            )
                        }
                    }
                    if (data.categoryData.isNotEmpty()) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionTitleWithExport(
                                title = "Par cat\u00E9gorie",
                                isExporting = "categories" in isExporting,
                                onExport = { onExportPdf("categories") }
                            )
                            DonutChart(items = data.categoryData.map { it.category to it.ca })
                        }
                    }
                }
            }

            if (data.trendData.isNotEmpty()) {
                SectionTitle("Mouvements stock (tendance)")
                CombinedTrendChart(data = data.trendData)
            }

            if (data.invoicesStatusData.let { it.brouillon + it.envoyee + it.payee + it.annulee + it.partiellementPayee } > 0) {
                SectionTitle("\u00C9tat des factures")
                InvoicesStatusChart(data = data.invoicesStatusData)
            }

            if (data.reorderItems.isNotEmpty()) {
                SectionTitleWithExport(
                    title = "Produits \u00E0 commander",
                    isExporting = "to-order" in isExporting,
                    onExport = { onExportPdf("to-order") }
                )
                ReorderTable(items = data.reorderItems)
            }

            if (data.lowStockItems.isNotEmpty() || data.outOfStockItems.isNotEmpty() || data.expiringItems.isNotEmpty()) {
                SectionTitleWithExport(
                    title = "Alertes",
                    isExporting = "ruptures" in isExporting,
                    onExport = { onExportPdf("ruptures") }
                )
                AlertsSection(data = data)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SectionTitleWithExport(
    title: String,
    isExporting: Boolean,
    onExport: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (isExporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
        } else {
            IconButton(onClick = onExport, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Exporter PDF",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun KpiSection(data: DashboardDisplayData) {
    KpiRow(
        KpiCardData("CA Aujourd'hui", data.caToday, Icons.Default.AttachMoney, Success),
        KpiCardData("Ventes", data.salesCount, Icons.Default.ShoppingCart, Info)
    )
    KpiRow(
        KpiCardData("Ticket moyen", data.averageTicket, Icons.AutoMirrored.Filled.TrendingUp, Accent),
        KpiCardData("Marge brute", data.grossMargin, Icons.Default.AccountBalance, Brand)
    )
    KpiRow(
        KpiCardData("Cr\u00E9ances", data.receivables, Icons.Default.Warning, Error),
        KpiCardData("Taux recouvr.", data.collectionRate, Icons.AutoMirrored.Filled.TrendingUp, Success)
    )
    KpiRow(
        KpiCardData("Valeur stock", data.stockValue, Icons.Default.Inventory, Brand),
        KpiCardData("Ruptures", data.stockouts, Icons.Default.Warning, Error)
    )
    KpiRow(
        KpiCardData("Encaissement", data.totalEncaissement, Icons.Default.Payment, Success),
        KpiCardData("Compte principal", data.mainAccountBalance, Icons.Default.AccountBalance, Info)
    )
    KpiRow(
        KpiCardData("Mouvements ajd", data.todayMovements, Icons.Default.Refresh, Accent),
        KpiCardData(
            "Alertes", data.totalAlerts,
            Icons.Default.Warning,
            if (data.hasAlerts) Error else Success
        )
    )
    KpiRow(
        KpiCardData("CA P\u00E9riode", data.caPeriode, Icons.Default.AttachMoney, Brand),
        KpiCardData("Ventes P\u00E9riode", data.nbVentesPeriode, Icons.Default.ShoppingCart, Info)
    )
}

private data class KpiCardData(val title: String, val value: String, val icon: ImageVector, val color: Color)

@Composable
private fun KpiRow(left: KpiCardData, right: KpiCardData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StockKPICard(
            title = left.title, value = left.value, icon = left.icon,
            color = left.color, modifier = Modifier.weight(1f)
        )
        StockKPICard(
            title = right.title, value = right.value, icon = right.icon,
            color = right.color, modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SalesChart(data: List<SalesDayData>) {
    if (data.isEmpty()) return
    val maxCa = data.maxOf { it.ca }.coerceAtLeast(1.0)
    StockCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val pad = 30f
                val drawW = size.width - pad * 2
                val drawH = size.height - pad * 2
                val path = Path()
                data.forEachIndexed { i, point ->
                    val x = pad + (i.toFloat() / (data.size - 1).coerceAtLeast(1)) * drawW
                    val y = pad + drawH - (point.ca.toFloat() / maxCa.toFloat()) * drawH
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = Brand, style = Stroke(width = 2.5.dp.toPx()))
                data.forEachIndexed { i, point ->
                    val x = pad + (i.toFloat() / (data.size - 1).coerceAtLeast(1)) * drawW
                    val y = pad + drawH - (point.ca.toFloat() / maxCa.toFloat()) * drawH
                    drawCircle(color = Brand, radius = 3.dp.toPx(), center = Offset(x, y))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEach { day ->
                    Text(
                        text = day.date.takeLast(5),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalBarChart(
    items: List<Pair<String, Int>>,
    color: Color
) {
    if (items.isEmpty()) return
    val maxVal = items.maxOf { it.second }.coerceAtLeast(1)
    val barColors = listOf(Brand, Accent, Success, Info, Warning)

    StockCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEachIndexed { index, (label, value) ->
                val fraction = value.toFloat() / maxVal
                val barColor = barColors[index % barColors.size]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(72.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRoundRect(
                                color = barColor.copy(alpha = 0.2f),
                                size = size,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                            )
                            drawRoundRect(
                                color = barColor,
                                size = Size(size.width * fraction, size.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$value",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(items: List<Pair<String, Double>>) {
    if (items.isEmpty()) return
    val total = items.sumOf { it.second }.coerceAtLeast(1.0)
    val colors = listOf(Brand, Accent, Success, Warning, Info, Error, BrandDark, Color(0xFF9C27B0))
    val surfaceColor = MaterialTheme.colorScheme.surface

    StockCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(160.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 32.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    val topLeft = Offset(center.x - radius, center.y - radius)
                    val arcSize = Size(radius * 2, radius * 2)

                    var startAngle = -90f
                    items.forEachIndexed { index, (_, value) ->
                        val sweep = (value / total * 360).toFloat()
                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )
                        startAngle += sweep
                    }
                    drawCircle(
                        color = surfaceColor,
                        radius = radius - strokeWidth / 2
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            items.forEachIndexed { index, (label, value) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .padding(end = 2.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = colors[index % colors.size])
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "%.0f%%".format(value / total * 100),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Brand
                    )
                }
            }
        }
    }
}

@Composable
private fun CombinedTrendChart(data: List<StockTrendData>) {
    if (data.isEmpty()) return
    val maxVal = data.maxOf { maxOf(it.entries, it.exits).coerceAtLeast(1) }

    StockCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Canvas(
                modifier = Modifier.fillMaxWidth().height(180.dp)
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                val padding = 40f
                val drawW = chartWidth - padding * 2
                val drawH = chartHeight - padding * 2

                val entriesPath = Path()
                val exitsPath = Path()

                data.forEachIndexed { i, point ->
                    val x = padding + (i.toFloat() / (data.size - 1).coerceAtLeast(1)) * drawW
                    val entryY = padding + drawH - (point.entries.toFloat() / maxVal) * drawH
                    val exitY = padding + drawH - (point.exits.toFloat() / maxVal) * drawH

                    if (i == 0) {
                        entriesPath.moveTo(x, entryY)
                        exitsPath.moveTo(x, exitY)
                    } else {
                        entriesPath.lineTo(x, entryY)
                        exitsPath.lineTo(x, exitY)
                    }
                }

                drawPath(entriesPath, color = Success, style = Stroke(width = 3.dp.toPx()))
                drawPath(exitsPath, color = Error, style = Stroke(width = 3.dp.toPx()))

                data.forEachIndexed { i, point ->
                    val x = padding + (i.toFloat() / (data.size - 1).coerceAtLeast(1)) * drawW
                    val entryY = padding + drawH - (point.entries.toFloat() / maxVal) * drawH
                    val exitY = padding + drawH - (point.exits.toFloat() / maxVal) * drawH
                    drawCircle(color = Success, radius = 3.dp.toPx(), center = Offset(x, entryY))
                    drawCircle(color = Error, radius = 3.dp.toPx(), center = Offset(x, exitY))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Success)
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("Entr\u00E9es", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.width(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Error)
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("Sorties", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun InvoicesStatusChart(data: InvoicesStatusData) {
    val items = listOf(
        "Brouillon" to data.brouillon.toFloat() to MaterialTheme.colorScheme.onSurfaceVariant,
        "Envoy\u00E9e" to data.envoyee.toFloat() to Accent,
        "Pay\u00E9e" to data.payee.toFloat() to Success,
        "Partielle" to data.partiellementPayee.toFloat() to Info,
        "Annul\u00E9e" to data.annulee.toFloat() to Error
    )
    val maxVal = items.maxOf { it.first.second }.coerceAtLeast(1f)
    StockCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val colW = size.width / items.size
                items.forEachIndexed { i, (pair, color) ->
                    val (_, value) = pair
                    val barH = (value / maxVal) * (size.height - 20f)
                    val x = i * colW + colW * 0.15f
                    val y = size.height - 10f - barH
                    drawRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(colW * 0.7f, barH)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusLabel("Brouillon", data.brouillon, MaterialTheme.colorScheme.onSurfaceVariant)
                StatusLabel("Envoy\u00E9e", data.envoyee, Accent)
                StatusLabel("Pay\u00E9e", data.payee, Success)
                StatusLabel("Partielle", data.partiellementPayee, Info)
                StatusLabel("Annul\u00E9e", data.annulee, Error)
            }
        }
    }
}

@Composable
private fun StatusLabel(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReorderTable(items: List<ReorderItem>) {
    StockCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Produit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Stock", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                Text("Min", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                Text("Besoin", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(50.dp))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${item.quantity}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp), color = if (item.quantity <= item.minQuantity) Error else MaterialTheme.colorScheme.onSurface)
                    Text("${item.minQuantity}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(30.dp))
                    Text("${item.needed}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(50.dp), color = Error)
                }
            }
        }
    }
}

@Composable
private fun AlertsSection(data: DashboardDisplayData) {
    StockCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (data.outOfStockItems.isNotEmpty()) {
                Text("Rupture de stock", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Error)
                data.outOfStockItems.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text("Rupture", style = MaterialTheme.typography.labelSmall, color = Error)
                    }
                }
            }
            if (data.lowStockItems.isNotEmpty()) {
                Text("Stock faible", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Warning)
                data.lowStockItems.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text("${item.quantity}", style = MaterialTheme.typography.labelSmall, color = Warning)
                    }
                }
            }
            if (data.expiringItems.isNotEmpty()) {
                Text("P\u00E9rim\u00E9s (DLC < 30j)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Accent)
                data.expiringItems.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${item.daysLeft}j", style = MaterialTheme.typography.labelSmall, color = Accent)
                    }
                }
            }
        }
    }
}
