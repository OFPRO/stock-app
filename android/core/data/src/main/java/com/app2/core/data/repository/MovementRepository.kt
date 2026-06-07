package com.app2.core.data.repository

import com.app2.core.data.local.dao.MovementDao
import com.app2.core.data.local.entity.StockMovementEntity
import com.app2.core.data.remote.MovementApiService
import com.app2.core.data.remote.dto.StockMovementDTO
import com.app2.core.data.remote.dto.deserialize
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class MovementRepository(
    private val api: MovementApiService,
    private val movementDao: MovementDao
) {
    suspend fun getMovements(
        productId: Int? = null,
        warehouseId: Int? = null
    ): List<StockMovementDTO> {
        return try {
            val response = api.getMovements(productId, warehouseId)
            val movements = response.deserialize<List<StockMovementDTO>>()
            movementDao.deleteAll()
            movementDao.insertAll(movements.map { it.toEntity() })
            movements
        } catch (e: Exception) {
            val cached = if (productId != null) {
                movementDao.getByProductId(productId)
            } else {
                movementDao.getAll()
            }
            if (cached.isNotEmpty()) {
                cached.map { it.toDTO() }
            } else {
                throw e
            }
        }
    }

    suspend fun createMovement(productId: Int, body: JsonObject): JsonElement {
        val response = api.createMovement(productId, body)
        val movement = response.deserialize<StockMovementDTO>()
        movementDao.insertAll(listOf(movement.toEntity()))
        return response
    }

    suspend fun transferStock(body: JsonObject): JsonElement {
        return api.transferStock(body)
    }

    suspend fun interWarehouseTransfer(body: JsonObject): JsonElement {
        return api.interWarehouseTransfer(body)
    }

    suspend fun refreshMovements(
        productId: Int? = null,
        warehouseId: Int? = null
    ): List<StockMovementDTO> {
        val response = api.getMovements(productId, warehouseId)
        val movements = response.deserialize<List<StockMovementDTO>>()
        movementDao.deleteAll()
        movementDao.insertAll(movements.map { it.toEntity() })
        return movements
    }
}

private fun StockMovementDTO.toEntity() = StockMovementEntity(
    id = id,
    productId = productId,
    productName = productName,
    type = type,
    quantity = quantity,
    sourceLocationId = sourceLocationId,
    destLocationId = destLocationId,
    sourceLocation = sourceLocation,
    destLocation = destLocation,
    note = note,
    createdAt = createdAt
)

private fun StockMovementEntity.toDTO() = StockMovementDTO(
    id = id,
    productId = productId ?: 0,
    productName = productName,
    type = type,
    quantity = quantity,
    sourceLocationId = sourceLocationId,
    destLocationId = destLocationId,
    sourceLocation = sourceLocation,
    destLocation = destLocation,
    note = note,
    createdAt = createdAt ?: ""
)
