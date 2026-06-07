package com.app2.core.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Products : Screen("products")
    data object ProductDetail : Screen("products/{productId}") {
        fun createRoute(productId: Int) = "products/$productId"
    }
    data object POS : Screen("pos")
    data object CustomerDetail : Screen("customers/{customerId}") {
        fun createRoute(customerId: Int) = "customers/$customerId"
    }
    data object SupplierDetail : Screen("suppliers/{supplierId}") {
        fun createRoute(supplierId: Int) = "suppliers/$supplierId"
    }
    data object More : Screen("more")
    data object Warehouses : Screen("warehouses")
    data object Locations : Screen("warehouses/{warehouseId}/{warehouseName}") {
        fun createRoute(warehouseId: Int, warehouseName: String) =
            "warehouses/$warehouseId/${java.net.URLEncoder.encode(warehouseName, "UTF-8")}"
    }
    data object Movements : Screen("movements")
    data object OrderDetail : Screen("orders/{orderId}") {
        fun createRoute(orderId: Int) = "orders/$orderId"
    }
    data object InvoiceDetail : Screen("invoices/{invoiceId}") {
        fun createRoute(invoiceId: Int) = "invoices/$invoiceId"
    }
    data object Notifications : Screen("notifications")
    data object PinSetup : Screen("pin-setup")
    data object PinChange : Screen("pin-change")
    data object Settings : Screen("settings")
}
