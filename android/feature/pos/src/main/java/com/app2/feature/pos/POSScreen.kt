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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app2.core.ui.ViewState
import com.app2.core.ui.components.BadgeVariant
import com.app2.core.ui.components.StockBadge
import com.app2.core.ui.components.StockButton
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
                POSStartSession(viewModel = sessionViewModel, onBack = onBack)
            } else {
                registerViewModel.currentSessionId = session.id
                LaunchedEffect(Unit) {
                    registerViewModel.loadProducts()
                    registerViewModel.loadCustomers()
                    registerViewModel.loadBestSellers()
                }

                POSRegisterSession(
                    sessionNumber = session.sessionNumber,
                    searchQuery = searchQuery,
                    onSearchChange = { registerViewModel.loadProducts(it) },
                    productsState = productsState,
                    bestSellers = bestSellers,
                    onAddToCart = { registerViewModel.addToCart(it) },
                    cart = cart,
                    subtotal = registerViewModel.subtotal,
                    itemCount = registerViewModel.itemCount,
                    onUpdateQty = { id, qty -> registerViewModel.updateQuantity(id, qty) },
                    onRemove = { registerViewModel.removeFromCart(it) },
                    selectedCustomer = selectedCustomer,
                    customers = customers,
                    onSelectCustomer = { registerViewModel.selectCustomer(it) },
                    onCheckout = { method, amount -> registerViewModel.checkout(method, amount) },
                    paymentState = paymentState,
                    onClearPaymentState = { registerViewModel.clearPaymentState() },
                    onBack = onBack
                )
            }
        }
        is ViewState.Empty -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun POSRegisterSession(
    sessionNumber: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    productsState: ViewState<List<POSProduct>>,
    bestSellers: List<POSProduct>,
    onAddToCart: (POSProduct) -> Unit,
    cart: List<CartItem>,
    subtotal: Double,
    itemCount: Int,
    onUpdateQty: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    selectedCustomer: POSCustomer?,
    customers: List<POSCustomer>,
    onSelectCustomer: (POSCustomer?) -> Unit,
    onCheckout: (String, Double) -> Unit,
    paymentState: ViewState<PaymentResult>?,
    onClearPaymentState: () -> Unit,
    onBack: () -> Unit
) {
    var showScanner by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showCartSheet by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("cash") }
    var tenderedAmount by remember { mutableStateOf("") }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    val config = LocalConfiguration.current
    val isCompact = config.screenWidthDp < 600

    val gridColumns = when {
        config.screenWidthDp >= 840 -> 4
        config.screenWidthDp >= 600 -> 3
        else -> 2
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Caisse", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = sessionNumber,
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

        if (isCompact) {
            CompactPOSLayout(
                searchQuery = searchQuery,
                onSearchChange = onSearchChange,
                productsState = productsState,
                bestSellers = bestSellers,
                selectedCustomer = selectedCustomer,
                customers = customers,
                customerDropdownExpanded = customerDropdownExpanded,
                onCustomerDropdownExpandedChange = { customerDropdownExpanded = it },
                onSelectCustomer = onSelectCustomer,
                onShowScanner = { showScanner = true },
                onAddToCart = onAddToCart,
                onShowCart = { showCartSheet = true },
                itemCount = itemCount,
                gridColumns = gridColumns
            )
        } else {
            ExpandedPOSLayout(
                searchQuery = searchQuery,
                onSearchChange = onSearchChange,
                productsState = productsState,
                bestSellers = bestSellers,
                selectedCustomer = selectedCustomer,
                customers = customers,
                customerDropdownExpanded = customerDropdownExpanded,
                onCustomerDropdownExpandedChange = { customerDropdownExpanded = it },
                onSelectCustomer = onSelectCustomer,
                onShowScanner = { showScanner = true },
                onAddToCart = onAddToCart,
                cart = cart,
                subtotal = subtotal,
                itemCount = itemCount,
                onUpdateQty = onUpdateQty,
                onRemove = onRemove,
                onCheckout = {
                    showPaymentDialog = true
                    paymentMethod = "cash"
                    tenderedAmount = if (subtotal > 0) "%.2f".format(subtotal) else ""
                },
                gridColumns = gridColumns
            )
        }
    }

    if (showCartSheet && isCompact) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CartSheetContent(
                cart = cart,
                subtotal = subtotal,
                itemCount = itemCount,
                onUpdateQty = onUpdateQty,
                onRemove = onRemove,
                onCheckout = {
                    showPaymentDialog = true
                    paymentMethod = "cash"
                    tenderedAmount = if (subtotal > 0) "%.2f".format(subtotal) else ""
                    showCartSheet = false
                }
            )
        }
    }

    if (showScanner) {
        ModalBottomSheet(
            onDismissRequest = { showScanner = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            BarcodeScannerSheet(
                onBarcodeDetected = { barcode -> onSearchChange(barcode) },
                onDismiss = { showScanner = false }
            )
        }
    }

    if (showPaymentDialog) {
        PaymentDialog(
            subtotal = subtotal,
            paymentMethod = paymentMethod,
            onPaymentMethodChange = { paymentMethod = it },
            tenderedAmount = tenderedAmount,
            onTenderedAmountChange = { tenderedAmount = it },
            isProcessing = paymentState is ViewState.Loading,
            onConfirm = {
                onCheckout(paymentMethod, tenderedAmount.toDoubleOrNull() ?: subtotal)
                showPaymentDialog = false
            },
            onDismiss = { showPaymentDialog = false }
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
                PaymentResultDialog(
                    success = ps.data.success,
                    documentNumber = ps.data.documentNumber,
                    total = ps.data.total,
                    changeAmount = ps.data.changeAmount,
                    customerName = ps.data.customerName,
                    onDismiss = onClearPaymentState
                )
            }
            is ViewState.Error -> {
                AlertDialog(
                    onDismissRequest = onClearPaymentState,
                    title = { Text("Erreur") },
                    text = { Text(ps.message) },
                    confirmButton = { TextButton(onClick = onClearPaymentState) { Text("OK") } }
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun CompactPOSLayout(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    productsState: ViewState<List<POSProduct>>,
    bestSellers: List<POSProduct>,
    selectedCustomer: POSCustomer?,
    customers: List<POSCustomer>,
    customerDropdownExpanded: Boolean,
    onCustomerDropdownExpandedChange: (Boolean) -> Unit,
    onSelectCustomer: (POSCustomer?) -> Unit,
    onShowScanner: () -> Unit,
    onAddToCart: (POSProduct) -> Unit,
    onShowCart: () -> Unit,
    itemCount: Int,
    gridColumns: Int
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ProductsPanel(
                searchQuery = searchQuery,
                onSearchChange = onSearchChange,
                productsState = productsState,
                bestSellers = bestSellers,
                selectedCustomer = selectedCustomer,
                customers = customers,
                customerDropdownExpanded = customerDropdownExpanded,
                onCustomerDropdownExpandedChange = onCustomerDropdownExpandedChange,
                onSelectCustomer = onSelectCustomer,
                onShowScanner = onShowScanner,
                onAddToCart = onAddToCart,
                gridColumns = gridColumns,
                modifier = Modifier.fillMaxSize()
            )
        }

        FloatingActionButton(
            onClick = onShowCart,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Brand
        ) {
            BadgedBox(
                badge = {
                    if (itemCount > 0) {
                        Badge { Text("$itemCount") }
                    }
                }
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Panier", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun ExpandedPOSLayout(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    productsState: ViewState<List<POSProduct>>,
    bestSellers: List<POSProduct>,
    selectedCustomer: POSCustomer?,
    customers: List<POSCustomer>,
    customerDropdownExpanded: Boolean,
    onCustomerDropdownExpandedChange: (Boolean) -> Unit,
    onSelectCustomer: (POSCustomer?) -> Unit,
    onShowScanner: () -> Unit,
    onAddToCart: (POSProduct) -> Unit,
    cart: List<CartItem>,
    subtotal: Double,
    itemCount: Int,
    onUpdateQty: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onCheckout: () -> Unit,
    gridColumns: Int
) {
    Row(modifier = Modifier.fillMaxSize()) {
        ProductsPanel(
            searchQuery = searchQuery,
            onSearchChange = onSearchChange,
            productsState = productsState,
            bestSellers = bestSellers,
            selectedCustomer = selectedCustomer,
            customers = customers,
            customerDropdownExpanded = customerDropdownExpanded,
            onCustomerDropdownExpandedChange = onCustomerDropdownExpandedChange,
            onSelectCustomer = onSelectCustomer,
            onShowScanner = onShowScanner,
            onAddToCart = onAddToCart,
            gridColumns = gridColumns,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )

        CartPanel(
            cart = cart,
            subtotal = subtotal,
            itemCount = itemCount,
            onUpdateQty = onUpdateQty,
            onRemove = onRemove,
            onCheckout = onCheckout,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(min = 280.dp, max = 400.dp)
                .weight(0.38f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductsPanel(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    productsState: ViewState<List<POSProduct>>,
    bestSellers: List<POSProduct>,
    selectedCustomer: POSCustomer?,
    customers: List<POSCustomer>,
    customerDropdownExpanded: Boolean,
    onCustomerDropdownExpandedChange: (Boolean) -> Unit,
    onSelectCustomer: (POSCustomer?) -> Unit,
    onShowScanner: () -> Unit,
    onAddToCart: (POSProduct) -> Unit,
    gridColumns: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StockTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = "Rechercher un produit...",
                variant = TextFieldVariant.Search,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onShowScanner) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scanner", tint = Accent)
            }
        }

        ExposedDropdownMenuBox(
            expanded = customerDropdownExpanded,
            onExpandedChange = onCustomerDropdownExpandedChange
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
                onDismissRequest = { onCustomerDropdownExpandedChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("Client comptoir") },
                    onClick = {
                        onSelectCustomer(null)
                        onCustomerDropdownExpandedChange(false)
                    }
                )
                customers.forEach { customer ->
                    DropdownMenuItem(
                        text = {
                            Text("${customer.name}${if (customer.discountRate != null) " (-${customer.discountRate}%)" else ""}")
                        },
                        onClick = {
                            onSelectCustomer(customer)
                            onCustomerDropdownExpandedChange(false)
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
                if (searchQuery.isBlank() && bestSellers.isNotEmpty()) {
                    BestSellersRow(products = bestSellers, onAdd = onAddToCart)
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(p.data, key = { it.id }) { product ->
                        ProductCard(product = product, onAdd = { onAddToCart(product) })
                    }
                }
            }
            is ViewState.Error -> {
                StockErrorView(
                    message = p.message,
                    onRetry = { onSearchChange(searchQuery) }
                )
            }
            is ViewState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucun produit trouvé", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductCard(product: POSProduct, onAdd: () -> Unit) {
    Card(
        onClick = onAdd,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.sku,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "%.0f MAD".format(product.salePrice),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Brand
                )
                StockBadge(
                    text = if (product.quantity > 0) "${product.quantity}" else "0",
                    variant = if (product.quantity > 0) BadgeVariant.Success else BadgeVariant.Error
                )
            }
        }
    }
}

@Composable
private fun BestSellersRow(products: List<POSProduct>, onAdd: (POSProduct) -> Unit) {
    Column(modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp)) {
        Text(
            text = "Meilleures ventes",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(products.take(5)) { product ->
                Card(
                    onClick = { onAdd(product) },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "%.0f MAD".format(product.salePrice),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Accent
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Panier ($itemCount)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (cart.isNotEmpty()) {
                    TextButton(onClick = { cart.forEach { onRemove(it.product.id) } }) {
                        Text("Vider", color = Error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            HorizontalDivider()

            if (cart.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Panier vide", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "%.2f MAD".format(subtotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Brand
                    )
                }
                Spacer(Modifier.height(10.dp))
                StockButton(
                    onClick = onCheckout,
                    enabled = cart.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    text = "Payer ${"%.2f".format(subtotal)} MAD"
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
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
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
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Moins", modifier = Modifier.size(18.dp))
                }
                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.width(4.dp))
            Text(
                text = "%.0f MAD".format(item.lineTotal),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Error, modifier = Modifier.size(18.dp))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun CartSheetContent(
    cart: List<CartItem>,
    subtotal: Double,
    itemCount: Int,
    onUpdateQty: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onCheckout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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

        if (cart.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text("Panier vide", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(cart, key = { it.product.id }) { item ->
                    CartSheetItemRow(
                        item = item,
                        onIncrement = { onUpdateQty(item.product.id, item.quantity + 1) },
                        onDecrement = { onUpdateQty(item.product.id, item.quantity - 1) },
                        onRemove = { onRemove(item.product.id) }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
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
            Spacer(Modifier.height(12.dp))
            StockButton(
                onClick = onCheckout,
                enabled = cart.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                text = "Payer ${"%.2f".format(subtotal)} MAD"
            )
        }
    }
}

@Composable
private fun CartSheetItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Moins", modifier = Modifier.size(18.dp))
                }
                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "%.0f MAD".format(item.lineTotal),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Error, modifier = Modifier.size(20.dp))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun PaymentDialog(
    subtotal: Double,
    paymentMethod: String,
    onPaymentMethodChange: (String) -> Unit,
    tenderedAmount: String,
    onTenderedAmountChange: (String) -> Unit,
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paiement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total à payer", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "%.2f MAD".format(subtotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Brand
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StockButton(
                        text = "Espèces",
                        onClick = { onPaymentMethodChange("cash") },
                        variant = if (paymentMethod == "cash") com.app2.core.ui.components.ButtonVariant.Primary
                        else com.app2.core.ui.components.ButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    StockButton(
                        text = "Carte",
                        onClick = { onPaymentMethodChange("card") },
                        variant = if (paymentMethod == "card") com.app2.core.ui.components.ButtonVariant.Primary
                        else com.app2.core.ui.components.ButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = tenderedAmount,
                    onValueChange = onTenderedAmountChange,
                    label = { Text("Montant reçu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (paymentMethod == "cash" && tenderedAmount.toDoubleOrNull() != null && tenderedAmount.toDouble() >= subtotal) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Monnaie à rendre", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "%.2f MAD".format(tenderedAmount.toDouble() - subtotal),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Success
                        )
                    }
                }
            }
        },
        confirmButton = {
            StockButton(
                text = "Payer",
                onClick = onConfirm,
                enabled = !isProcessing && tenderedAmount.toDoubleOrNull() != null && tenderedAmount.toDouble() >= subtotal,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) { Text("Annuler") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentResultDialog(
    success: Boolean,
    documentNumber: String?,
    total: Double,
    changeAmount: Double,
    customerName: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (success) "Paiement réussi" else "Échec",
                color = if (success) Success else Error,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (success) {
                    documentNumber?.let {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Document", style = MaterialTheme.typography.bodyMedium)
                            Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Total", style = MaterialTheme.typography.bodyMedium)
                        Text("%.2f MAD".format(total), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    if (changeAmount > 0) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Monnaie", style = MaterialTheme.typography.bodyMedium)
                            Text("%.2f MAD".format(changeAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Success)
                        }
                    }
                    customerName?.let {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Client", style = MaterialTheme.typography.bodyMedium)
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    Text("Le paiement a échoué. Veuillez réessayer.")
                }
            }
        },
        confirmButton = {
            StockButton(text = "OK", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun POSStartSession(viewModel: POSSessionViewModel, onBack: () -> Unit) {
    val openingCash by viewModel.openingCash.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Caisse") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Aucune session ouverte", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("Ouvrir une nouvelle session de caisse", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = openingCash,
                        onValueChange = { viewModel.onOpeningCashChanged(it) },
                        label = { Text("Fond de caisse (MAD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    StockButton(text = "Ouvrir la session", onClick = { viewModel.openSession() }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
