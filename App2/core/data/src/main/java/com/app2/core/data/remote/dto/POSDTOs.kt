package com.app2.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenSessionResponse(
    val success: Boolean = true,
    val session: POSSessionDTO? = null,
    @SerialName("session_number") val sessionNumber: String? = null
)

@Serializable
data class CreateCashMovementResponse(
    val success: Boolean = true,
    val movement: CashMovementDTO? = null
)

@Serializable
data class BestSellerDTO(
    val id: Int,
    val name: String,
    val price: Double = 0.0,
    @SerialName("price_base") val priceBase: Double? = null,
    val sku: String = "",
    val quantity: Int? = null,
    @SerialName("total_sold") val totalSold: Int = 0
)

@Serializable
data class POSSessionDTO(
    val id: Int,
    @SerialName("session_number") val sessionNumber: String? = null,
    @SerialName("warehouse_id") val warehouseId: Int? = null,
    @SerialName("opening_cash") val openingCash: Double? = null,
    @SerialName("closing_cash") val closingCash: Double? = null,
    @SerialName("expected_cash") val expectedCash: Double? = null,
    val status: String? = null,
    @SerialName("opened_at") val openedAt: String? = null,
    @SerialName("closed_at") val closedAt: String? = null
)

@Serializable
data class CloseSessionResponse(
    val success: Boolean = true,
    @SerialName("expected_cash") val expectedCash: Double? = null,
    val deposited: Boolean? = null
)

@Serializable
data class CashMovementDTO(
    val id: Int,
    @SerialName("session_id") val sessionId: Int? = null,
    val type: String? = null,
    val amount: Double? = null,
    val reason: String? = null,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class POSTransactionDTO(
    val id: Int,
    @SerialName("ticket_number") val ticketNumber: String? = null,
    @SerialName("transaction_number") val transactionNumber: String? = null,
    @SerialName("session_id") val sessionId: Int? = null,
    @SerialName("customer_id") val customerId: Int? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val subtotal: Double? = null,
    @SerialName("discount_total") val discountTotal: Double? = null,
    @SerialName("tax_amount") val taxAmount: Double? = null,
    val total: Double? = null,
    @SerialName("tendered_amount") val tenderedAmount: Double? = null,
    @SerialName("change_given") val changeGiven: Double? = null,
    val status: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class POSCustomerDTO(
    val id: Int,
    val name: String,
    @SerialName("client_code") val clientCode: String? = null,
    @SerialName("discount_rate") val discountRate: Double? = null
)

@Serializable
data class POSTransactionRequest(
    @SerialName("session_id") val sessionId: Int,
    @SerialName("customer_id") val customerId: Int? = null,
    val items: List<TransactionItem>,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("tendered_amount") val tenderedAmount: Double,
    val notes: String = "",
    @SerialName("is_credit") val isCredit: Boolean = false
) {
    @Serializable
    data class TransactionItem(
        @SerialName("product_id") val productId: Int,
        val quantity: Int,
        @SerialName("unit_price") val unitPrice: Double,
        @SerialName("discount_percent") val discountPercent: Double = 0.0,
        @SerialName("product_name") val productName: String = "",
        @SerialName("product_sku") val productSku: String = ""
    )
}

@Serializable
data class POSTransactionResponse(
    val success: Boolean = true,
    @SerialName("document_number") val documentNumber: String? = null,
    @SerialName("document_id") val documentId: Int? = null,
    @SerialName("document_type") val documentType: String? = null,
    @SerialName("document_status") val documentStatus: String? = null,
    val total: Double? = null,
    @SerialName("change_amount") val changeAmount: Double? = null,
    @SerialName("customer_name") val customerName: String? = null
)

@Serializable
enum class DiscountTier {
    @SerialName("normal") Normal,
    @SerialName("loyal") Loyal,
    @SerialName("student") Student,
    @SerialName("school") School;

    val label: String
        get() = when (this) {
            Normal -> "Normal"
            Loyal -> "Fidèle"
            Student -> "Étudiant"
            School -> "École"
        }
}

@Serializable
enum class PaymentMethod {
    @SerialName("cash") Cash,
    @SerialName("card") Card,
    @SerialName("mixed") Mixed;

    val label: String
        get() = when (this) {
            Cash -> "Espèces"
            Card -> "Carte bancaire"
            Mixed -> "Mixte"
        }
}

@Serializable
data class CartItem(
    @SerialName("product_id") val productId: Int,
    @SerialName("product_name") val productName: String,
    @SerialName("product_sku") val productSku: String,
    val quantity: Int = 1,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("base_unit_price") val baseUnitPrice: Double,
    @SerialName("price_loyal") val priceLoyal: Double? = null,
    @SerialName("price_school") val priceSchool: Double? = null,
    @SerialName("price_student") val priceStudent: Double? = null,
    @SerialName("discount_percent") val discountPercent: Double = 0.0
)
