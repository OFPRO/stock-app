package com.app2.core.data.repository

import com.app2.core.data.local.dao.OrderDao
import com.app2.core.data.local.dao.OrderItemDao
import com.app2.core.data.local.entity.PurchaseOrderEntity
import com.app2.core.data.local.entity.PurchaseOrderItemEntity
import com.app2.core.data.remote.OrderApiService
import com.app2.core.data.remote.dto.OrderDTO
import com.app2.core.data.remote.dto.OrderItemDTO
import com.app2.core.data.remote.dto.deserialize
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class OrderRepository(
    private val api: OrderApiService,
    private val orderDao: OrderDao,
    private val orderItemDao: OrderItemDao
) {
    suspend fun getOrders(warehouseId: Int? = null, status: String? = null): List<OrderDTO> {
        return try {
            val response = api.getOrders(warehouseId, status)
            val orders = response.deserialize<List<OrderDTO>>()
            orderDao.deleteAll()
            orderDao.insertAll(orders.map { it.toEntity() })
            orders
        } catch (e: Exception) {
            val cached = orderDao.getAll()
            if (cached.isNotEmpty()) {
                cached.map { it.toDTO() }
            } else {
                throw e
            }
        }
    }

    suspend fun getOrder(id: Int): OrderDTO {
        return try {
            val response = api.getOrder(id)
            val order = response.deserialize<OrderDTO>()
            orderDao.insert(order.toEntity())
            order
        } catch (e: Exception) {
            val cached = orderDao.getById(id)
            cached?.toDTO() ?: throw e
        }
    }

    suspend fun getOrderItems(orderId: Int): List<OrderItemDTO> {
        return try {
            val response = api.getOrderItems(orderId)
            val items = response.deserialize<List<OrderItemDTO>>()
            orderItemDao.deleteByOrderId(orderId)
            orderItemDao.insertAll(items.map { it.toEntity(orderId) })
            items
        } catch (e: Exception) {
            val cached = orderItemDao.getByOrderId(orderId)
            if (cached.isNotEmpty()) {
                cached.map { it.toDTO() }
            } else {
                throw e
            }
        }
    }

    suspend fun createOrder(body: JsonObject): JsonElement {
        val response = api.createOrder(body)
        val order = response.deserialize<OrderDTO>()
        orderDao.insert(order.toEntity())
        return response
    }

    suspend fun updateOrder(id: Int, body: JsonObject): JsonElement {
        val response = api.updateOrder(id, body)
        val order = response.deserialize<OrderDTO>()
        orderDao.insert(order.toEntity())
        return response
    }

    suspend fun deleteOrder(id: Int) {
        api.deleteOrder(id)
        orderItemDao.deleteByOrderId(id)
        orderDao.insert(PurchaseOrderEntity(id = id, status = "annulée"))
    }

    suspend fun refreshOrders(warehouseId: Int? = null, status: String? = null): List<OrderDTO> {
        val response = api.getOrders(warehouseId, status)
        val orders = response.deserialize<List<OrderDTO>>()
        orderDao.deleteAll()
        orderDao.insertAll(orders.map { it.toEntity() })
        return orders
    }
}

private fun OrderDTO.toEntity() = PurchaseOrderEntity(
    id = id,
    orderNumber = orderNumber,
    supplierId = supplierId,
    warehouseId = warehouseId,
    status = status ?: "brouillon",
    total = total ?: 0.0,
    notes = notes,
    sentAt = sentAt,
    receivedAt = receivedAt,
    createdAt = createdAt
)

private fun PurchaseOrderEntity.toDTO() = OrderDTO(
    id = id,
    orderNumber = orderNumber,
    supplierId = supplierId,
    supplierName = null,
    warehouseId = warehouseId,
    status = status,
    total = total,
    notes = notes,
    sentAt = sentAt,
    receivedAt = receivedAt,
    createdAt = createdAt
)

private fun OrderItemDTO.toEntity(orderId: Int) = PurchaseOrderItemEntity(
    id = id,
    orderId = orderId,
    productId = productId,
    productName = productName,
    quantity = quantity ?: 0,
    unitPrice = unitPrice ?: 0.0,
    receivedQty = receivedQty
)

private fun PurchaseOrderItemEntity.toDTO() = OrderItemDTO(
    id = id,
    orderId = orderId,
    productId = productId,
    productName = productName,
    quantity = quantity,
    unitPrice = unitPrice,
    receivedQty = receivedQty
)
