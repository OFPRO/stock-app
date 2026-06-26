package com.app2.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DashboardKPIDTO(
    @SerialName("total_value") val totalValue: Double? = null,
    @SerialName("out_of_stock") val outOfStock: Int? = null,
    @SerialName("low_stock") val lowStock: Int? = null,
    @SerialName("total_products") val totalProducts: Int? = null,
    @SerialName("today_movements") val todayMovements: Int? = null,
    @SerialName("total_alerts") val totalAlerts: Int? = null,
    @SerialName("rotation_rate") val rotationRate: Double? = null,
    @SerialName("products_to_order") val productsToOrder: List<ReorderProductDTO>? = null
)

@Serializable
data class ReorderProductDTO(
    val name: String? = null,
    val sku: String? = null,
    val quantity: Int? = null,
    @SerialName("min_quantity") val minQuantity: Int? = null,
    val needed: Int? = null
)

@Serializable
data class SalesKPIDTO(
    @SerialName("ca_jour") val caJour: Double? = null,
    @SerialName("nb_ventes_jour") val nbVentesJour: Int? = null,
    @SerialName("ticket_moyen") val ticketMoyen: Double? = null,
    @SerialName("ca_periode") val caPeriode: Double? = null,
    @SerialName("nb_ventes_periode") val nbVentesPeriode: Int? = null
)

@Serializable
data class SalesDayDTO(
    val date: String? = null,
    val ca: Double? = null,
    @SerialName("nb_ventes") val nbVentes: Int? = null
)

@Serializable
data class TopProductDTO(
    val name: String? = null,
    @SerialName("qty_vendue") val qtyVendue: Int? = null,
    val ca: Double? = null
)

@Serializable
data class MarginDTO(
    @SerialName("marge_globale") val margeGlobale: Double? = null
)

@Serializable
data class ReceivablesKPIDTO(
    @SerialName("total_creances") val totalCreances: Double? = null,
    @SerialName("taux_encaissement") val tauxEncaissement: Double? = null
)

@Serializable
data class PaymentMethodsDTO(
    val total: Double? = null,
    val methods: Map<String, PaymentMethodDetailDTO>? = null
)

@Serializable
data class PaymentMethodDetailDTO(
    val total: Double? = null,
    val count: Int? = null
)

@Serializable
data class AlertesDTO(
    @SerialName("low_stock") val lowStock: List<AlertProductDTO>? = null,
    @SerialName("out_of_stock") val outOfStock: List<AlertProductDTO>? = null,
    val expiring: List<ExpiringProductDTO>? = null
)

@Serializable
data class AlertProductDTO(
    val name: String? = null,
    val sku: String? = null,
    val quantity: Int? = null
)

@Serializable
data class ExpiringProductDTO(
    val name: String? = null,
    val sku: String? = null,
    @SerialName("lot_number") val lotNumber: String? = null,
    @SerialName("days_left") val daysLeft: Int? = null,
    val quantity: Int? = null
)

@Serializable
data class CategoryDistributionDTO(
    val category: String? = null,
    @SerialName("qty_vendue") val qtyVendue: Int? = null,
    val ca: Double? = null
)

@Serializable
data class InvoicesStatusDTO(
    val brouillon: Int? = null,
    val envoyee: Int? = null,
    val payee: Int? = null,
    val annulee: Int? = null,
    @SerialName("partiellement_payee") val partiellementPayee: Int? = null
)

@Serializable
data class StockTrendDTO(
    val date: String? = null,
    val entries: Int? = null,
    val exits: Int? = null,
    val net: Int? = null
)
