package com.app2.feature.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app2.core.ui.ViewState
import com.app2.core.ui.components.BadgeVariant
import com.app2.core.ui.components.StockBadge
import com.app2.core.ui.components.StockButton
import com.app2.core.ui.components.StockCard
import com.app2.core.ui.components.StockErrorView
import com.app2.core.ui.components.StockSkeletonRow
import com.app2.core.ui.components.StockTextField
import com.app2.core.ui.components.TextFieldVariant
import com.app2.core.ui.theme.Accent
import com.app2.core.ui.theme.Brand
import com.app2.core.ui.theme.Error
import com.app2.core.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSScreen(
    onBack: () -> Unit,
    sessionViewModel: POSSessionViewModel = hiltViewModel(),
    registerViewModel: POSRegisterViewModel = hiltViewModel()
) {
    val sessionState by sessionViewModel.session.collectAsStateWithLifecycle()
    val searchQuery by registerViewModel.searchQuery.collectAsStateWithLifecycle()
    val productsState by registerViewModel.products.collectAsStateWithLifecycle()
    val cart by registerViewModel.cart.collectAsStateWithLifecycle()
    val selectedCustomer by registerViewModel.selectedCustomer.collectAsStateWithLifecycle()
    val customers by registerViewModel.customers.collectAsStateWithLifecycle()
    val paymentState by registerViewModel.paymentState.collectAsStateWithLifecycle()
    val bestSellers by registerViewModel.bestSellers.collectAsStateWithLifecycle()

    var showScanner by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("cash") }
    var tenderedAmount by remember { mutableStateOf("") }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    when (val s = sessionState) {
        is ViewState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Vérification de la session...")
            }
        }
        is ViewState.Error -> {
            StockErrorView(
                message = s.message,
                onRetry = { sessionViewModel.checkSession() }
            )
        }
        is ViewState.Loaded -> {
            val session = s.data
            if (session == null) {
                POSStartSession(
                    viewModel = sessionViewModel,
                    onBack = onBack
                )
            } else {
                registerViewModel.currentSessionId = session.id
                LaunchedEffect(Unit) {
                    registerViewModel.loadProducts()
                    registerViewModel.loadCustomers()
                    registerViewModel.loadBestSellers()
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Column {
                                Text("Caisse", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = session.sessionNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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

                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StockTextField(
                                    value = searchQuery,
                                    onValueChange = { registerViewModel.loadProducts(it) },
                                    placeholder = "Rechercher un produit...",
                                    variant = TextFieldVariant.Search,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = { showScanner = true }
                                ) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "Scanner",
                                        tint = Accent
                                    )
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = customerDropdownExpanded,
                                onExpandedChange = { customerDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = if (selectedCustomer != null) "Client: ${selectedCustomer!!.name}" else "Client comptoir",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                ExposedDropdownMenu(
                                    expanded = customerDropdownExpanded,
                                    onDismissRequest = { customerDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Client comptoir") },
                                        onClick = {
                                            registerViewModel.selectCustomer(null)
                                            customerDropdownExpanded = false
                                        }
                                    )
                                    customers.forEach { customer ->
                                        DropdownMenuItem(
                                            text = {
                                                Text("${customer.name}${if (customer.discountRate != null) " (-${customer.discountRate}%)" else ""}")
                                            },
                                            onClick = {
                                                registerViewModel.selectCustomer(customer)
                                                customerDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            when (val p = productsState) {
                                is ViewState.Loading -> {
                                    LazyColumn(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(6) { StockSkeletonRow(modifier = Modifier.fillMaxWidth()) }
                                    }
                                }
                                is ViewState.Loaded -> {
                                    if (p.data.isEmpty() && bestSellers.isNotEmpty()) {
                                        BestSellersRow(
                                            products = bestSellers,
                                            onAdd = { registerViewModel.addToCart(it) }
                                        )
                                    }
                                    LazyColumn(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(p.data, key = { it.id }) { product ->
                                            POSProductRow(
                                                product = product,
                                                onAdd = { registerViewModel.addToCart(product) }
                                            )
                                        }
                                    }
                                }
                                is ViewState.Error -> {
                                    StockErrorView(
                                        message = p.message,
                                        onRetry = { registerViewModel.loadProducts() }
                                    )
                                }
                                is ViewState.Empty -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Aucun produit trouvé", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
        }
        is ViewState.Empty -> {}
    }
}

                        CartPanel(
                            cart = cart,
                            subtotal = registerViewModel.subtotal,
                            itemCount = registerViewModel.itemCount,
                            onUpdateQty = { id, qty -> registerViewModel.updateQuantity(id, qty) },
                            onRemove = { registerViewModel.removeFromCart(it) },
                            onCheckout = { showPaymentDialog = true },
                            modifier = Modifier
                                .width(320.dp)
                                .fillMaxHeight()
                        )
                    }
                }

                if (showScanner) {
                    ModalBottomSheet(
                        onDismissRequest = { showScanner = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ) {
                        BarcodeScannerSheet(
                            onBarcodeDetected = { barcode ->
                                registerViewModel.loadProducts(barcode)
                            },
                            onDismiss = { showScanner = false }
                        )
                    }
                }

                if (showPaymentDialog) {
                    val cartItems = cart
                    AlertDialog(
                        onDismissRequest = { showPaymentDialog = false },
                        title = { Text("Paiement") },
                        text = {
                            Column {
                                Text("Total: ${"%.2f".format(registerViewModel.subtotal)} MAD")
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { paymentMethod = "cash" },
                                        colors = if (paymentMethod == "cash")
                                            ButtonDefaults.buttonColors(containerColor = Accent)
                                        else ButtonDefaults.buttonColors()
                                    ) { Text("Espèces") }
                                    Button(
                                        onClick = { paymentMethod = "card" },
                                        colors = if (paymentMethod == "card")
                                            ButtonDefaults.buttonColors(containerColor = Accent)
                                        else ButtonDefaults.buttonColors()
                                    ) { Text("Carte") }
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = tenderedAmount,
                                    onValueChange = { tenderedAmount = it },
                                    label = { Text("Montant reçu") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    registerViewModel.checkout(
                                        paymentMethod = paymentMethod,
                                        tenderedAmount = tenderedAmount.toDoubleOrNull() ?: registerViewModel.subtotal
                                    )
                                    showPaymentDialog = false
                                },
                                enabled = paymentState !is ViewState.Loading
                            ) { Text("Payer") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPaymentDialog = false }) { Text("Annuler") }
                        }
                    )
                }

                paymentState?.let { ps ->
                    when (ps) {
                        is ViewState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Traitement du paiement...")
                            }
                        }
                        is ViewState.Loaded -> {
                            AlertDialog(
                                onDismissRequest = { registerViewModel.clearPaymentState() },
                                title = { Text(if (ps.data.success) "Paiement réussi" else "Échec") },
                                text = {
                                    Column {
                                        if (ps.data.success) {
                                            Text("Document: ${ps.data.documentNumber}")
                                            Text("Total: ${"%.2f".format(ps.data.total)} MAD")
                                            if (ps.data.changeAmount > 0) {
                                                Text("Monnaie: ${"%.2f".format(ps.data.changeAmount)} MAD")
                                            }
                                            ps.data.customerName?.let { Text("Client: $it") }
                                        } else {
                                            Text("Le paiement a échoué")
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { registerViewModel.clearPaymentState() }) {
                                        Text("OK")
                                    }
                                }
                            )
                        }
                        is ViewState.Error -> {
                            AlertDialog(
                                onDismissRequest = { registerViewModel.clearPaymentState() },
                                title = { Text("Erreur") },
                                text = { Text(ps.message) },
                                confirmButton = {
                                    TextButton(onClick = { registerViewModel.clearPaymentState() }) { Text("OK") }
                                }
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
        is ViewState.Empty -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun POSStartSession(
    viewModel: POSSessionViewModel,
    onBack: () -> Unit
) {
    val openingCash by viewModel.openingCash.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Caisse") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Aucune session ouverte",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ouvrir une nouvelle session de caisse",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = openingCash,
                        onValueChange = { viewModel.onOpeningCashChanged(it) },
                        label = { Text("Fond de caisse (MAD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.openSession() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Ouvrir la session") }
                }
            }
        }
    }
}

@Composable
private fun POSProductRow(
    product: POSProduct,
    onAdd: () -> Unit
) {
    StockCard(
        onClick = onAdd,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.sku,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "%.2f MAD".format(product.salePrice),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Brand
                )
                StockBadge(
                    text = if (product.quantity > 0) "${product.quantity} en stock" else "Rupture",
                    variant = if (product.quantity > 0) BadgeVariant.Success else BadgeVariant.Error
                )
            }
        }
    }
}

@Composable
private fun BestSellersRow(
    products: List<POSProduct>,
    onAdd: (POSProduct) -> Unit
) {
    Column(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)) {
        Text(
            text = "Meilleures ventes",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(products.take(5)) { product ->
                Card(
                    onClick = { onAdd(product) },
                    colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                        Text(
                            text = "%.0f MAD".format(product.salePrice),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartPanel(
    cart: List<CartItem>,
    subtotal: Double,
    itemCount: Int,
    onUpdateQty: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Panier ($itemCount)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (cart.isNotEmpty()) {
                    TextButton(onClick = { cart.forEach { onRemove(it.product.id) } }) {
                        Text("Vider", color = Error)
                    }
                }
            }

            HorizontalDivider()

            if (cart.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Panier vide",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(cart, key = { it.product.id }) { item ->
                        CartItemRow(
                            item = item,
                            onIncrement = { onUpdateQty(item.product.id, item.quantity + 1) },
                            onDecrement = { onUpdateQty(item.product.id, item.quantity - 1) },
                            onRemove = { onRemove(item.product.id) }
                        )
                    }
                }
            }

            HorizontalDivider()

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "%.2f MAD".format(subtotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Brand
                    )
                }
                Spacer(Modifier.height(8.dp))
                StockButton(
                    onClick = onCheckout,
                    enabled = cart.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    text = "Payer (${"%.2f".format(subtotal)} MAD)"
                )
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.product.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "%.2f MAD".format(item.product.salePrice),
                        style = MaterialTheme.typography.bodySmall,
                        color = Brand
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Error, modifier = Modifier.size(20.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDecrement, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Moins", modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "${item.quantity}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onIncrement, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(20.dp))
                    }
                }
                Text(
                    text = "%.2f MAD".format(item.lineTotal),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
