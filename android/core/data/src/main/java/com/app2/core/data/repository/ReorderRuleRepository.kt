package com.app2.core.data.repository

import com.app2.core.data.local.dao.ReorderRuleDao
import com.app2.core.data.local.entity.ReorderRuleEntity
import com.app2.core.data.remote.ReorderRuleApiService
import com.app2.core.data.remote.dto.ReorderRuleDTO
import com.app2.core.data.remote.dto.deserialize
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class ReorderRuleRepository(
    private val api: ReorderRuleApiService,
    private val reorderRuleDao: ReorderRuleDao
) {
    suspend fun getReorderRules(warehouseId: Int? = null): List<ReorderRuleDTO> {
        return try {
            val response = api.getReorderRules(warehouseId)
            val rules = response.deserialize<List<ReorderRuleDTO>>()
            reorderRuleDao.deleteAll()
            reorderRuleDao.insertAll(rules.map { it.toEntity() })
            rules
        } catch (e: Exception) {
            val cached = reorderRuleDao.getAll()
            if (cached.isNotEmpty()) {
                cached.map { it.toDTO() }
            } else {
                throw e
            }
        }
    }

    suspend fun createReorderRule(body: JsonObject): JsonElement {
        val response = api.createReorderRule(body)
        val rule = response.deserialize<ReorderRuleDTO>()
        reorderRuleDao.insertAll(listOf(rule.toEntity()))
        return response
    }

    suspend fun updateReorderRule(id: Int, body: JsonObject): JsonElement {
        val response = api.updateReorderRule(id, body)
        val rule = response.deserialize<ReorderRuleDTO>()
        reorderRuleDao.insertAll(listOf(rule.toEntity()))
        return response
    }

    suspend fun deleteReorderRule(id: Int) {
        api.deleteReorderRule(id)
        reorderRuleDao.insertAll(listOf(ReorderRuleEntity(id = id, productId = 0)))
    }

    suspend fun refreshRules(warehouseId: Int? = null): List<ReorderRuleDTO> {
        val response = api.getReorderRules(warehouseId)
        val rules = response.deserialize<List<ReorderRuleDTO>>()
        reorderRuleDao.deleteAll()
        reorderRuleDao.insertAll(rules.map { it.toEntity() })
        return rules
    }
}

private fun ReorderRuleDTO.toEntity() = ReorderRuleEntity(
    id = id,
    productId = productId,
    warehouseId = warehouseId,
    minQuantity = minQuantity ?: 5,
    maxQuantity = maxQuantity ?: 100,
    triggerType = triggerType ?: "manual",
    supplierId = supplierId,
    createdAt = createdAt
)

private fun ReorderRuleEntity.toDTO() = ReorderRuleDTO(
    id = id,
    productId = productId,
    warehouseId = warehouseId,
    minQuantity = minQuantity,
    maxQuantity = maxQuantity,
    triggerType = triggerType,
    supplierId = supplierId,
    createdAt = createdAt
)
