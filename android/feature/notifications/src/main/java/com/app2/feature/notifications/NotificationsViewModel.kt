package com.app2.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.NotificationRepository
import com.app2.core.data.remote.dto.NotificationDTO
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<NotificationDTO>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()

    private var allNotifications: List<NotificationDTO> = emptyList()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                allNotifications = notificationRepository.getNotifications()
                updateState()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des notifications")
            }
        }
    }

    fun markRead(id: Int) {
        viewModelScope.launch {
            try {
                notificationRepository.markNotificationRead(id)
                loadNotifications()
            } catch (_: Exception) {}
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                notificationRepository.markAllNotificationsRead()
                loadNotifications()
            } catch (_: Exception) {}
        }
    }

    fun refresh() {
        allNotifications = emptyList()
        loadNotifications()
    }

    private fun updateState() {
        _unreadCount.value = allNotifications.count { (it.isRead ?: 0) == 0 }
        _state.value = if (allNotifications.isEmpty()) {
            ViewState.Empty
        } else {
            ViewState.Loaded(allNotifications)
        }
    }
}
