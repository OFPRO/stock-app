package com.app2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app2.core.ui.components.StockCard
import com.app2.core.ui.theme.App2Theme
import com.app2.feature.auth.PinLockScreen
import com.app2.feature.auth.PinManager
import com.app2.feature.auth.PinScreenMode
import com.app2.feature.auth.PinSetupScreen
import com.app2.feature.customers.CustomerDetailScreen
import com.app2.feature.customers.CustomersScreen
import com.app2.feature.dashboard.DashboardScreen
import com.app2.feature.invoices.InvoiceDetailScreen
import com.app2.feature.invoices.InvoicesScreen
import com.app2.feature.notifications.NotificationsScreen
import com.app2.feature.orders.OrderDetailScreen
import com.app2.feature.orders.OrdersScreen
import com.app2.feature.pos.POSScreen
import com.app2.feature.products.ProductDetailScreen
import com.app2.feature.products.ProductsScreen
import com.app2.feature.suppliers.SupplierDetailScreen
import com.app2.feature.suppliers.SuppliersScreen
import com.app2.feature.warehouses.CreateMovementScreen
import com.app2.feature.warehouses.LocationsScreen
import com.app2.feature.warehouses.MovementListScreen
import com.app2.feature.warehouses.WarehousesScreen
import com.app2.feature.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

sealed class PinAppState {
    data object Checking : PinAppState()
    data object Locked : PinAppState()
    data object FirstTimeSetup : PinAppState()
    data object Unlocked : PinAppState()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var pinManager: PinManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(pinManager = pinManager)
                }
            }
        }
    }
}

@Composable
fun MainScreen(pinManager: PinManager) {
    var pinState by remember { mutableStateOf<PinAppState>(PinAppState.Checking) }
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        pinState = if (pinManager.isPinSet()) {
            PinAppState.Locked
        } else {
            PinAppState.FirstTimeSetup
        }
    }

    when (val s = pinState) {
        is PinAppState.Checking -> {}
        is PinAppState.Locked -> {
            PinLockScreen(onSuccess = { pinState = PinAppState.Unlocked })
        }
        is PinAppState.FirstTimeSetup -> {
            PinSetupScreen(
                mode = PinScreenMode.Setup,
                onSuccess = { pinState = PinAppState.Unlocked }
            )
        }
        is PinAppState.Unlocked -> {
            val items = listOf(
                BottomNavItem("Dashboard", Icons.Default.Dashboard, "dashboard"),
                BottomNavItem("Produits", Icons.Default.Inventory2, "products"),
                BottomNavItem("Caisse", Icons.Default.ShoppingCart, "pos"),
                BottomNavItem("Plus", Icons.Default.MoreHoriz, "more")
            )

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        items.forEach { item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("dashboard") { DashboardScreen() }
                    composable("products") {
                        ProductsScreen(
                            onProductClick = { productId ->
                                navController.navigate("products/$productId")
                            }
                        )
                    }
                    composable(
                        route = "products/{productId}",
                        arguments = listOf(navArgument("productId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val productId = backStackEntry.arguments?.getInt("productId") ?: return@composable
                        ProductDetailScreen(
                            productId = productId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("pos") { POSScreen(onBack = { navController.popBackStack() }) }
                    composable("customers") { CustomersScreen(onCustomerClick = { navController.navigate("customers/$it") }) }
                    composable(
                        route = "customers/{customerId}",
                        arguments = listOf(navArgument("customerId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val customerId = backStackEntry.arguments?.getInt("customerId") ?: return@composable
                        CustomerDetailScreen(
                            customerId = customerId,
                            onBack = { navController.popBackStack() },
                            onDeleted = { navController.popBackStack() }
                        )
                    }
                    composable("more") {
                        MoreScreen(
                            onCustomersClick = { navController.navigate("customers") },
                            onSuppliersClick = { navController.navigate("suppliers") },
                            onOrdersClick = { navController.navigate("orders") },
                            onInvoicesClick = { navController.navigate("invoices") },
                            onNotificationsClick = { navController.navigate("notifications") },
                            onWarehousesClick = { navController.navigate("warehouses") },
                            onMovementsClick = { navController.navigate("movements") },
                            onPinChangeClick = { navController.navigate("pin-change") },
                            onSettingsClick = { navController.navigate("settings") }
                        )
                    }
                    composable("invoices") {
                        InvoicesScreen(
                            onInvoiceClick = { invoiceId ->
                                navController.navigate("invoices/$invoiceId")
                            }
                        )
                    }
                    composable("notifications") {
                        NotificationsScreen(onBack = { navController.popBackStack() })
                    }
                    composable(
                        route = "invoices/{invoiceId}",
                        arguments = listOf(navArgument("invoiceId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val invoiceId = backStackEntry.arguments?.getInt("invoiceId") ?: return@composable
                        InvoiceDetailScreen(
                            invoiceId = invoiceId,
                            onBack = { navController.popBackStack() },
                            onDeleted = { navController.popBackStack() }
                        )
                    }
                    composable("orders") {
                        OrdersScreen(
                            onOrderClick = { orderId ->
                                navController.navigate("orders/$orderId")
                            }
                        )
                    }
                    composable(
                        route = "orders/{orderId}",
                        arguments = listOf(navArgument("orderId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getInt("orderId") ?: return@composable
                        OrderDetailScreen(
                            orderId = orderId,
                            onBack = { navController.popBackStack() },
                            onDeleted = { navController.popBackStack() }
                        )
                    }
                    composable("movements") {
                        MovementListScreen(
                            onCreateClick = { navController.navigate("create-movement") }
                        )
                    }
                    composable("create-movement") {
                        CreateMovementScreen(
                            onDismiss = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() }
                        )
                    }
                    composable("suppliers") { SuppliersScreen(onSupplierClick = { navController.navigate("suppliers/$it") }) }
                    composable("warehouses") {
                        WarehousesScreen(
                            onWarehouseClick = { id, name ->
                                val encoded = java.net.URLEncoder.encode(name, "UTF-8")
                                navController.navigate("warehouses/$id/$encoded")
                            }
                        )
                    }
                    composable(
                        route = "warehouses/{warehouseId}/{warehouseName}",
                        arguments = listOf(
                            navArgument("warehouseId") { type = NavType.IntType },
                            navArgument("warehouseName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val warehouseId = backStackEntry.arguments?.getInt("warehouseId") ?: return@composable
                        val warehouseName = backStackEntry.arguments?.getString("warehouseName") ?: ""
                        LocationsScreen(
                            warehouseId = warehouseId,
                            warehouseName = java.net.URLDecoder.decode(warehouseName, "UTF-8"),
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "suppliers/{supplierId}",
                        arguments = listOf(navArgument("supplierId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val supplierId = backStackEntry.arguments?.getInt("supplierId") ?: return@composable
                        SupplierDetailScreen(
                            supplierId = supplierId,
                            onBack = { navController.popBackStack() },
                            onDeleted = { navController.popBackStack() }
                        )
                    }
                    composable("pin-change") {
                        PinSetupScreen(
                            mode = PinScreenMode.Change,
                            onSuccess = { navController.popBackStack() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreScreen(
    onCustomersClick: () -> Unit = {},
    onSuppliersClick: () -> Unit = {},
    onOrdersClick: () -> Unit = {},
    onInvoicesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onWarehousesClick: () -> Unit = {},
    onMovementsClick: () -> Unit = {},
    onPinChangeClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Plus",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp)
        )
        StockCard(
            onClick = onCustomersClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Clients",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Gérer les clients",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
        StockCard(
            onClick = onSuppliersClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Fournisseurs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Gérer les fournisseurs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
        StockCard(
            onClick = onNotificationsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.NotificationImportant,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Centre d'alertes et notifications",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
        StockCard(
            onClick = onInvoicesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Factures",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Gérer les factures clients",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
        StockCard(
            onClick = onOrdersClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Commandes fournisseur",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Gérer les commandes fournisseur",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
        StockCard(
            onClick = onMovementsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Mouvements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Historique des mouvements de stock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
        StockCard(
            onClick = onWarehousesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warehouse,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Entrepôts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Gérer les entrepôts et zones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
        StockCard(
            onClick = onSettingsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Paramètres",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "À propos et options avancées",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
        StockCard(
            onClick = onPinChangeClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Sécurité",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Modifier le code de verrouillage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}
