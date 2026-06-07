package com.app2.core.data.repository

import com.app2.core.data.local.dao.InvoiceDao
import com.app2.core.data.local.dao.InvoiceItemDao
import com.app2.core.data.local.entity.InvoiceEntity
import com.app2.core.data.local.entity.InvoiceItemEntity
import com.app2.core.data.remote.InvoiceApiService
import com.app2.core.data.remote.dto.InvoiceDTO
import com.app2.core.data.remote.dto.InvoiceItemDTO
import com.app2.core.data.remote.dto.deserialize
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class InvoiceRepository(
    private val api: InvoiceApiService,
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao
) {
    suspend fun getInvoices(
        status: String? = null,
        dateStart: String? = null,
        dateEnd: String? = null
    ): List<InvoiceDTO> {
        return try {
            val response = api.getInvoices(status, dateStart, dateEnd)
            val invoices = response.deserialize<List<InvoiceDTO>>()
            invoiceDao.deleteAll()
            invoiceDao.insertAll(invoices.map { it.toEntity() })
            invoices
        } catch (e: Exception) {
            val cached = invoiceDao.getAll()
            if (cached.isNotEmpty()) {
                cached.map { it.toDTO() }
            } else {
                throw e
            }
        }
    }

    suspend fun getInvoice(id: Int): InvoiceDTO {
        return try {
            val response = api.getInvoice(id)
            val invoice = response.deserialize<InvoiceDTO>()
            invoiceDao.insert(invoice.toEntity())
            invoice
        } catch (e: Exception) {
            val cached = invoiceDao.getById(id)
            cached?.toDTO() ?: throw e
        }
    }

    suspend fun getInvoiceItems(invoiceId: Int): List<InvoiceItemDTO> {
        return try {
            val response = api.getInvoiceItems(invoiceId)
            val items = response.deserialize<List<InvoiceItemDTO>>()
            invoiceItemDao.deleteByInvoiceId(invoiceId)
            invoiceItemDao.insertAll(items.map { it.toEntity(invoiceId) })
            items
        } catch (e: Exception) {
            val cached = invoiceItemDao.getByInvoiceId(invoiceId)
            if (cached.isNotEmpty()) {
                cached.map { it.toDTO() }
            } else {
                throw e
            }
        }
    }

    suspend fun createInvoice(body: JsonObject): JsonElement {
        val response = api.createInvoice(body)
        val invoice = response.deserialize<InvoiceDTO>()
        invoiceDao.insert(invoice.toEntity())
        return response
    }

    suspend fun updateInvoice(id: Int, body: JsonObject): JsonElement {
        val response = api.updateInvoice(id, body)
        val invoice = response.deserialize<InvoiceDTO>()
        invoiceDao.insert(invoice.toEntity())
        return response
    }

    suspend fun deleteInvoice(id: Int) {
        api.deleteInvoice(id)
        invoiceItemDao.deleteByInvoiceId(id)
        invoiceDao.insert(InvoiceEntity(id = id, invoiceNumber = "", status = "annulée"))
    }

    suspend fun addInvoiceItem(id: Int, body: JsonObject): JsonElement {
        val response = api.addInvoiceItem(id, body)
        val item = response.deserialize<InvoiceItemDTO>()
        invoiceItemDao.insertAll(listOf(item.toEntity(id)))
        return response
    }

    suspend fun deleteInvoiceItem(id: Int, itemId: Int) {
        api.deleteInvoiceItem(id, itemId)
        invoiceItemDao.deleteByInvoiceId(id)
    }

    suspend fun refreshInvoices(
        status: String? = null,
        dateStart: String? = null,
        dateEnd: String? = null
    ): List<InvoiceDTO> {
        val response = api.getInvoices(status, dateStart, dateEnd)
        val invoices = response.deserialize<List<InvoiceDTO>>()
        invoiceDao.deleteAll()
        invoiceDao.insertAll(invoices.map { it.toEntity() })
        return invoices
    }
}

private fun InvoiceDTO.toEntity() = InvoiceEntity(
    id = id,
    invoiceNumber = invoiceNumber ?: "",
    customerId = customerId,
    warehouseId = warehouseId,
    status = status ?: "brouillon",
    subtotal = subtotal ?: 0.0,
    discountTotal = discountTotal ?: 0.0,
    taxAmount = taxAmount ?: 0.0,
    total = total ?: 0.0,
    notes = notes,
    dueDate = dueDate,
    paidAt = paidAt,
    paymentMethod = paymentMethod,
    createdAt = createdAt
)

private fun InvoiceEntity.toDTO() = InvoiceDTO(
    id = id,
    invoiceNumber = invoiceNumber,
    customerId = customerId,
    warehouseId = warehouseId,
    status = status,
    subtotal = subtotal,
    discountTotal = discountTotal,
    taxAmount = taxAmount,
    total = total,
    notes = notes,
    dueDate = dueDate,
    paidAt = paidAt,
    customerName = null,
    paymentMethod = paymentMethod,
    createdAt = createdAt
)

private fun InvoiceItemDTO.toEntity(invoiceId: Int) = InvoiceItemEntity(
    id = id,
    invoiceId = invoiceId,
    productId = productId,
    productName = productName,
    quantity = quantity,
    unitPrice = unitPrice,
    discountPercent = discountPercent ?: 0.0,
    taxRate = taxRate ?: 20.0,
    lineTotal = lineTotal,
    createdAt = createdAt
)

private fun InvoiceItemEntity.toDTO() = InvoiceItemDTO(
    id = id,
    invoiceId = invoiceId,
    productId = productId,
    productName = productName,
    quantity = quantity,
    unitPrice = unitPrice,
    discountPercent = discountPercent,
    taxRate = taxRate,
    lineTotal = lineTotal,
    createdAt = createdAt
)
