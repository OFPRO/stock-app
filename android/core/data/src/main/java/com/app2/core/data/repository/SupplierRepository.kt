package com.app2.core.data.repository

import com.app2.core.data.local.dao.SupplierDao
import com.app2.core.data.local.entity.SupplierEntity
import com.app2.core.data.remote.SupplierApiService
import com.app2.core.data.remote.dto.SupplierDTO
import com.app2.core.data.remote.dto.deserialize
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class SupplierRepository(
    private val api: SupplierApiService,
    private val supplierDao: SupplierDao
) {
    suspend fun getSuppliers(): List<SupplierDTO> {
        return try {
            val response = api.getSuppliers()
            val suppliers = response.deserialize<List<SupplierDTO>>()
            supplierDao.deleteAll()
            supplierDao.insertAll(suppliers.map { it.toEntity() })
            suppliers
        } catch (e: Exception) {
            val cached = supplierDao.getAll()
            if (cached.isNotEmpty()) {
                cached.map { it.toDTO() }
            } else {
                throw e
            }
        }
    }

    suspend fun createSupplier(body: JsonObject): JsonElement {
        val response = api.createSupplier(body)
        val supplier = response.deserialize<SupplierDTO>()
        supplierDao.insertAll(listOf(supplier.toEntity()))
        return response
    }

    suspend fun updateSupplier(id: Int, body: JsonObject): JsonElement {
        val response = api.updateSupplier(id, body)
        val supplier = response.deserialize<SupplierDTO>()
        supplierDao.insertAll(listOf(supplier.toEntity()))
        return response
    }

    suspend fun deleteSupplier(id: Int) {
        api.deleteSupplier(id)
        supplierDao.insertAll(listOf(SupplierEntity(id = id, name = "")))
    }

    suspend fun refreshSuppliers(): List<SupplierDTO> {
        val response = api.getSuppliers()
        val suppliers = response.deserialize<List<SupplierDTO>>()
        supplierDao.deleteAll()
        supplierDao.insertAll(suppliers.map { it.toEntity() })
        return suppliers
    }
}

private fun SupplierDTO.toEntity() = SupplierEntity(
    id = id,
    name = name,
    email = email,
    phone = phone,
    address = address,
    contactPerson = contactPerson,
    createdAt = createdAt
)

private fun SupplierEntity.toDTO() = SupplierDTO(
    id = id,
    name = name,
    email = email,
    phone = phone,
    address = address,
    contactPerson = contactPerson,
    createdAt = createdAt
)
