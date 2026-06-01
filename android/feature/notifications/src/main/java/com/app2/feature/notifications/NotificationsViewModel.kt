package com.app2.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.NotificationApiService
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

private fun JsonElement?.optString(): String? {
    val prim = this?.jsonPrimitive
    return if (prim != null && prim !is JsonNull) prim.content else null
}

private fun JsonElement?.optInt(): Int? =
    this?.jsonPrimitive?.intOrNull

data class NotificationItem(
    val id: Int,
    val type: String,
    val title: String?,
    val message: String?,
    val isRead: Boolean,
    val linkType: String?,
    val linkId: Int?,
    val createdAt: String?
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationApi: NotificationApiService
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<NotificationItem>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()

    private var allNotifications: List<NotificationItem> = emptyList()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                val response = notificationApi.getNotifications()
                allNotifications = parseNotificationList(response)
                updateState()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des notifications")
            }
        }
    }

    fun markRead(id: Int) {
        viewModelScope.launch {
            try {
                notificationApi.markNotificationRead(id)
                loadNotifications()
            } catch (_: Exception) {}
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                notificationApi.markAllNotificationsRead()
                loadNotifications()
            } catch (_: Exception) {}
        }
    }

    fun refresh() {
        allNotifications = emptyList()
        loadNotifications()
    }

    private fun updateState() {
        _unreadCount.value = allNotifications.count { !it.isRead }
        _state.value = if (allNotifications.isEmpty()) {
            ViewState.Empty
        } else {
            ViewState.Loaded(allNotifications)
        }
    }

    private fun parseNotificationList(json: JsonElement): List<NotificationItem> {
        return json.jsonArray.mapNotNull { item ->
            val o = item.jsonObject
            val id = o["id"].optInt() ?: return@mapNotNull null
            NotificationItem(
                id = id,
                type = o["type"].optString() ?: "",
                title = o["title"].optString(),
                message = o["message"].optString(),
                isRead = (o["is_read"].optInt() ?: 0) == 1,
                linkType = o["link_type"].optString(),
                linkId = o["link_id"].optInt(),
                createdAt = o["created_at"].optString()
            )
        }
    }
}
