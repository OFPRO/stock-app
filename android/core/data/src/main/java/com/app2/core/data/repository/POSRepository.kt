package com.app2.core.data.repository

import com.app2.core.data.local.dao.POSSessionDao
import com.app2.core.data.local.entity.POSSessionEntity
import com.app2.core.data.remote.POSApiService
import com.app2.core.data.remote.dto.BestSellerDTO
import com.app2.core.data.remote.dto.CashMovementDTO
import com.app2.core.data.remote.dto.CloseSessionResponse
import com.app2.core.data.remote.dto.OpenSessionResponse
import com.app2.core.data.remote.dto.POSCustomerDTO
import com.app2.core.data.remote.dto.POSSessionDTO
import com.app2.core.data.remote.dto.POSTransactionDTO
import com.app2.core.data.remote.dto.POSTransactionResponse
import com.app2.core.data.remote.dto.deserialize
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class POSRepository(
    private val api: POSApiService,
    private val sessionDao: POSSessionDao
) {
    suspend fun getSessions(): List<POSSessionDTO> {
        return try {
            val response = api.getSessions()
            val sessions = response.deserialize<List<POSSessionDTO>>()
            sessionDao.deleteAll()
            sessions.forEach { sessionDao.insert(it.toEntity()) }
            sessions
        } catch (e: Exception) {
            val cached = sessionDao.getAll()
            if (cached.isNotEmpty()) {
                cached.map { it.toDTO() }
            } else {
                throw e
            }
        }
    }

    suspend fun getOpenSession(): POSSessionEntity? {
        return try {
            val response = api.getSessions()
            val sessions = response.deserialize<List<POSSessionDTO>>()
            sessionDao.deleteAll()
            sessions.forEach { sessionDao.insert(it.toEntity()) }
            sessions.find { it.status == "open" }?.let {
                sessionDao.getOpenSession()
            }
        } catch (e: Exception) {
            sessionDao.getOpenSession()
        }
    }

    suspend fun openSession(body: JsonObject): OpenSessionResponse {
        val response = api.openSession(body)
        val result = response.deserialize<OpenSessionResponse>()
        result.session?.let { sessionDao.insert(it.toEntity()) }
        return result
    }

    suspend fun closeSession(id: Int, body: JsonObject): CloseSessionResponse {
        val response = api.closeSession(id, body)
        return response.deserialize<CloseSessionResponse>()
    }

    suspend fun createTransaction(body: JsonObject): POSTransactionResponse {
        val response = api.createTransaction(body)
        return response.deserialize<POSTransactionResponse>()
    }

    suspend fun getRecentTransactions(sessionId: Int? = null, limit: Int? = null): List<POSTransactionDTO> {
        val response = api.getRecentTransactions(sessionId, limit)
        return response.deserialize<List<POSTransactionDTO>>()
    }

    suspend fun getPOSCustomers(): List<POSCustomerDTO> {
        val response = api.getPOSCustomers()
        return response.deserialize<List<POSCustomerDTO>>()
    }

    suspend fun getBestSellers(): List<BestSellerDTO> {
        val response = api.getBestSellers()
        return response.deserialize<List<BestSellerDTO>>()
    }

    suspend fun getCashMovements(): List<CashMovementDTO> {
        val response = api.getCashMovements()
        return response.deserialize<List<CashMovementDTO>>()
    }

    suspend fun createCashMovement(body: JsonObject): JsonElement {
        return api.createCashMovement(body)
    }

    suspend fun refreshSessions(): List<POSSessionDTO> {
        val response = api.getSessions()
        val sessions = response.deserialize<List<POSSessionDTO>>()
        sessionDao.deleteAll()
        sessions.forEach { sessionDao.insert(it.toEntity()) }
        return sessions
    }
}

private fun POSSessionDTO.toEntity() = POSSessionEntity(
    id = id,
    sessionNumber = sessionNumber,
    warehouseId = warehouseId,
    userName = "Caissier",
    openingCash = openingCash ?: 0.0,
    closingCash = closingCash,
    expectedCash = expectedCash,
    status = status ?: "open",
    openedAt = openedAt,
    closedAt = closedAt
)

private fun POSSessionEntity.toDTO() = POSSessionDTO(
    id = id,
    sessionNumber = sessionNumber,
    warehouseId = warehouseId,
    openingCash = openingCash,
    closingCash = closingCash,
    expectedCash = expectedCash,
    status = status,
    openedAt = openedAt,
    closedAt = closedAt
)
