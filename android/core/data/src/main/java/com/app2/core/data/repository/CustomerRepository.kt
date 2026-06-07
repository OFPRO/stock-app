package com.app2.core.data.repository

import com.app2.core.data.local.dao.CustomerDao
import com.app2.core.data.local.entity.CustomerEntity
import com.app2.core.data.remote.CustomerApiService
import com.app2.core.data.remote.dto.CustomerDTO
import com.app2.core.data.remote.dto.deserialize
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class CustomerRepository(
    private val api: CustomerApiService,
    private val customerDao: CustomerDao
) {
    suspend fun getCustomers(search: String? = null): List<CustomerDTO> {
        return try {
            val response = api.getCustomers(search)
            val customers = response.deserialize<List<CustomerDTO>>()
            customerDao.deleteAll()
            customerDao.insertAll(customers.map { it.toEntity() })
            customers
        } catch (e: Exception) {
            val cached = customerDao.getAllActive()
            if (cached.isNotEmpty()) {
                cached.map { it.toDTO() }
            } else {
                throw e
            }
        }
    }

    suspend fun getCustomer(id: Int): CustomerDTO {
        return try {
            val response = api.getCustomer(id)
            val customer = response.deserialize<CustomerDTO>()
            customerDao.insert(customer.toEntity())
            customer
        } catch (e: Exception) {
            val cached = customerDao.getById(id)
            cached?.toDTO() ?: throw e
        }
    }

    suspend fun createCustomer(body: JsonObject): JsonElement {
        val response = api.createCustomer(body)
        val customer = response.deserialize<CustomerDTO>()
        customerDao.insert(customer.toEntity())
        return response
    }

    suspend fun updateCustomer(id: Int, body: JsonObject): JsonElement {
        val response = api.updateCustomer(id, body)
        val customer = response.deserialize<CustomerDTO>()
        customerDao.insert(customer.toEntity())
        return response
    }

    suspend fun deleteCustomer(id: Int) {
        api.deleteCustomer(id)
        customerDao.insert(CustomerEntity(id = id, name = "", isActive = false))
    }

    suspend fun refreshCustomers(search: String? = null): List<CustomerDTO> {
        val response = api.getCustomers(search)
        val customers = response.deserialize<List<CustomerDTO>>()
        customerDao.deleteAll()
        customerDao.insertAll(customers.map { it.toEntity() })
        return customers
    }
}

private fun CustomerDTO.toEntity() = CustomerEntity(
    id = id,
    name = name,
    type = type ?: "particulier",
    email = email,
    phone = phone,
    address = address,
    clientCode = clientCode,
    discountRate = discountRate ?: 0.0,
    isLoyal = isLoyal == 1,
    isActive = isActive?.let { it != 0 } ?: true,
    ice = ice,
    notes = notes,
    createdAt = createdAt
)

private fun CustomerEntity.toDTO() = CustomerDTO(
    id = id,
    name = name,
    type = type,
    email = email,
    phone = phone,
    address = address,
    clientCode = clientCode,
    discountRate = discountRate,
    isLoyal = if (isLoyal) 1 else 0,
    isActive = if (isActive) 1 else 0,
    ice = ice,
    notes = notes,
    createdAt = createdAt
)
