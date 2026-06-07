package com.app2.core.data.repository

import com.app2.core.data.local.dao.WarehouseDao
import com.app2.core.data.local.entity.WarehouseEntity
import com.app2.core.data.remote.WarehouseApiService
import com.app2.core.data.remote.dto.WarehouseDTO
import com.app2.core.data.remote.dto.deserialize
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class WarehouseRepository(
    private val api: WarehouseApiService,
    private val warehouseDao: WarehouseDao
) {
    suspend fun getWarehouses(): List<WarehouseDTO> {
        return try {
            val response = api.getWarehouses()
            val warehouses = response.deserialize<List<WarehouseDTO>>()
            warehouseDao.deleteAll()
            warehouseDao.insertAll(warehouses.map { it.toEntity() })
            warehouses
        } catch (e: Exception) {
            val cached = warehouseDao.getAll()
            if (cached.isNotEmpty()) {
                cached.map { it.toDTO() }
            } else {
                throw e
            }
        }
    }

    suspend fun getWarehouse(id: Int): WarehouseDTO {
        return try {
            val response = api.getWarehouse(id)
            val warehouse = response.deserialize<WarehouseDTO>()
            warehouseDao.insertAll(listOf(warehouse.toEntity()))
            warehouse
        } catch (e: Exception) {
            val cached = warehouseDao.getAll().find { it.id == id }
            cached?.toDTO() ?: throw e
        }
    }

    suspend fun createWarehouse(body: JsonObject): JsonElement {
        val response = api.createWarehouse(body)
        val warehouse = response.deserialize<WarehouseDTO>()
        warehouseDao.insertAll(listOf(warehouse.toEntity()))
        return response
    }

    suspend fun deleteWarehouse(id: Int) {
        api.deleteWarehouse(id)
        warehouseDao.insertAll(listOf(WarehouseEntity(id = id, name = "")))
    }

    suspend fun refreshWarehouses(): List<WarehouseDTO> {
        val response = api.getWarehouses()
        val warehouses = response.deserialize<List<WarehouseDTO>>()
        warehouseDao.deleteAll()
        warehouseDao.insertAll(warehouses.map { it.toEntity() })
        return warehouses
    }
}

private fun WarehouseDTO.toEntity() = WarehouseEntity(
    id = id,
    name = name,
    address = address,
    manager = manager,
    phone = phone,
    ice = ice,
    patente = patente,
    rc = rc,
    taxeNumber = taxeNumber,
    isDefault = isDefault == 1,
    createdAt = createdAt
)

private fun WarehouseEntity.toDTO() = WarehouseDTO(
    id = id,
    name = name,
    address = address,
    manager = manager,
    phone = phone,
    isDefault = if (isDefault) 1 else 0,
    ice = ice,
    patente = patente,
    rc = rc,
    taxeNumber = taxeNumber,
    createdAt = createdAt
)
