package com.app2.core.data.repository

import com.app2.core.data.local.dao.NotificationDao
import com.app2.core.data.local.entity.NotificationEntity
import com.app2.core.data.remote.NotificationApiService
import com.app2.core.data.remote.dto.NotificationDTO
import com.app2.core.data.remote.dto.deserialize
import kotlinx.serialization.json.JsonElement

class NotificationRepository(
    private val api: NotificationApiService,
    private val notificationDao: NotificationDao
) {
    suspend fun getNotifications(warehouseId: Int? = null): List<NotificationDTO> {
        return try {
            val response = api.getNotifications(warehouseId)
            val notifications = response.deserialize<List<NotificationDTO>>()
            notificationDao.deleteAll()
            notificationDao.insertAll(notifications.map { it.toEntity() })
            notifications
        } catch (e: Exception) {
            val cached = notificationDao.getAll()
            if (cached.isNotEmpty()) {
                cached.map { it.toDTO() }
            } else {
                throw e
            }
        }
    }

    suspend fun getUnread(): List<NotificationDTO> {
        val cached = notificationDao.getUnread()
        return cached.map { it.toDTO() }
    }

    suspend fun markNotificationRead(id: Int): JsonElement {
        val response = api.markNotificationRead(id)
        notificationDao.markRead(id)
        return response
    }

    suspend fun markAllNotificationsRead(): JsonElement {
        val response = api.markAllNotificationsRead()
        notificationDao.markAllRead()
        return response
    }

    suspend fun refreshNotifications(warehouseId: Int? = null): List<NotificationDTO> {
        val response = api.getNotifications(warehouseId)
        val notifications = response.deserialize<List<NotificationDTO>>()
        notificationDao.deleteAll()
        notificationDao.insertAll(notifications.map { it.toEntity() })
        return notifications
    }
}

private fun NotificationDTO.toEntity() = NotificationEntity(
    id = id,
    type = type,
    title = title ?: "",
    message = message,
    productId = productId,
    warehouseId = warehouseId,
    isRead = isRead == 1,
    createdAt = createdAt
)

private fun NotificationEntity.toDTO() = NotificationDTO(
    id = id,
    type = type,
    title = title,
    message = message,
    productId = productId,
    warehouseId = warehouseId,
    isRead = if (isRead) 1 else 0,
    createdAt = createdAt
)
