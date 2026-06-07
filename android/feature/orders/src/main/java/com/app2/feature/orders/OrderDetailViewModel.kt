package com.app2.feature.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.OrderRepository
import com.app2.core.data.remote.dto.OrderDTO
import com.app2.core.data.remote.dto.OrderItemDTO
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<OrderDTO>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _items = MutableStateFlow<ViewState<List<OrderItemDTO>>>(ViewState.Loading)
    val items = _items.asStateFlow()

    private var lastLoadedId: Int? = null

    fun loadOrder(id: Int) {
        if (lastLoadedId == id) return
        lastLoadedId = id
        viewModelScope.launch {
            _state.value = ViewState.Loading
            _items.value = ViewState.Loading
            try {
                _state.value = ViewState.Loaded(orderRepository.getOrder(id))
                _items.value = ViewState.Loaded(orderRepository.getOrderItems(id))
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement")
            }
        }
    }

    fun updateStatus(newStatus: String, onSuccess: () -> Unit) {
        val id = lastLoadedId ?: return
        viewModelScope.launch {
            try {
                val body = buildJsonObject { put("status", newStatus) }
                orderRepository.updateOrder(id, body)
                onSuccess()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de mise à jour")
            }
        }
    }

    fun deleteOrder() {
        val id = lastLoadedId ?: return
        viewModelScope.launch {
            try {
                orderRepository.deleteOrder(id)
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }
}
