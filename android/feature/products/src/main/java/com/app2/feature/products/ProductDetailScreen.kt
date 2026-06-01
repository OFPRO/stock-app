package com.app2.feature.products

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app2.core.ui.ViewState
import com.app2.core.ui.components.BadgeVariant
import com.app2.core.ui.components.StockBadge
import com.app2.core.ui.components.StockCard
import com.app2.core.ui.components.StockErrorView
import com.app2.core.ui.components.StockSkeletonCard
import com.app2.core.ui.theme.Accent
import com.app2.core.ui.theme.Brand
import com.app2.core.ui.theme.Error
import com.app2.core.ui.theme.Info
import com.app2.core.ui.theme.Success
import com.app2.core.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Int,
    onBack: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    viewModel.loadProduct(productId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val s = state) {
                        is ViewState.Loaded -> s.data.name
                        else -> "Détail du produit"
                    }
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        when (val s = state) {
            is ViewState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) { StockSkeletonCard() }
                }
            }
            is ViewState.Loaded -> {
                ProductDetailContent(
                    product = s.data,
                    modifier = Modifier.padding(padding)
                )
            }
            is ViewState.Error -> {
                StockErrorView(
                    message = s.message,
                    onRetry = { viewModel.loadProduct(productId) },
                    modifier = Modifier.padding(padding)
                )
            }
            is ViewState.Empty -> {}
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: ProductDetailDisplayData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoCard(product)
        PricingCard(product)
        StockCard(product)
        MovementsCard(product)
        StatsCard(product)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoCard(product: ProductDetailDisplayData) {
    StockCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailRow(label = "SKU", value = product.sku)
            if (product.barcode != null) {
                DetailRow(label = "Code-barres", value = product.barcode)
            }
            if (product.category != null) {
                DetailRow(label = "Catégorie", value = product.category)
            }
            if (product.description != null) {
                DetailRow(label = "Description", value = product.description)
            }
            if (product.warehouseName != null) {
                DetailRow(label = "Entrepôt", value = product.warehouseName)
            }
            if (product.locationName != null) {
                DetailRow(label = "Emplacement", value = product.locationName)
            }
            if (product.createdAt != null) {
                DetailRow(label = "Créé le", value = product.createdAt)
            }
        }
    }
}

@Composable
private fun PricingCard(product: ProductDetailDisplayData) {
    StockCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = Accent
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Prix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Accent
            )
            PriceRow(label = "Prix normal", value = product.price)
            if (product.priceLoyal != null) {
                PriceRow(label = "Loyal (-15%)", value = product.priceLoyal)
            }
            if (product.priceStudent != null) {
                PriceRow(label = "Étudiant (-15%)", value = product.priceStudent)
            }
            if (product.priceSchool != null) {
                PriceRow(label = "École (-20%)", value = product.priceSchool)
            }
            if (product.wholesalePrice != null) {
                HorizontalDivider()
                PriceRow(label = "Gros", value = product.wholesalePrice)
            }
            if (product.purchasePrice != null) {
                PriceRow(label = "Prix d'achat", value = product.purchasePrice)
            }
            if (product.marginPercent != null) {
                DetailRow(label = "Marge", value = "%.0f%%".format(product.marginPercent))
            }
        }
    }
}

@Composable
private fun StockCard(product: ProductDetailDisplayData) {
    val (statusVariant, statusColor) = when {
        product.quantity <= 0 -> BadgeVariant.Error to Error
        product.quantity <= 5 -> BadgeVariant.Warning to Warning
        else -> BadgeVariant.Success to Success
    }

    StockCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = statusColor
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stock",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                StockBadge(
                    text = when {
                        product.quantity <= 0 -> "Rupture"
                        product.quantity <= 5 -> "Stock faible"
                        else -> "En stock"
                    },
                    variant = statusVariant
                )
            }
            Text(
                text = "${product.quantity} unités",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            if (product.minStock != null) {
                DetailRow(label = "Stock min", value = "${product.minStock}")
            }
            if (product.maxStock != null) {
                DetailRow(label = "Stock max", value = "${product.maxStock}")
            }
        }
    }
}

@Composable
private fun MovementsCard(product: ProductDetailDisplayData) {
    val movements = listOf(
        "Entrées (achats)" to product.purchaseStats.first,
        "Sorties (ventes)" to product.salesStats.first
    )

    StockCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Mouvements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            movements.forEach { (label, qty) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = qty.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(product: ProductDetailDisplayData) {
    StockCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Statistiques",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            DetailRow(
                label = "Total achats",
                value = "%.0f MAD".format(product.purchaseStats.second)
            )
            DetailRow(
                label = "Total ventes",
                value = "%.0f MAD".format(product.salesStats.second)
            )
        }
    }
}

@Composable
private fun PriceRow(label: String, value: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "%.2f MAD".format(value),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
