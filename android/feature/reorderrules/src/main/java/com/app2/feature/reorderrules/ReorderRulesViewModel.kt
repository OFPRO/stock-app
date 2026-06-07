package com.app2.feature.reorderrules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.ReorderRuleRepository
import com.app2.core.data.remote.dto.ReorderRuleDTO
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReorderRulesViewModel @Inject constructor(
    private val reorderRuleRepository: ReorderRuleRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<ReorderRuleDTO>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private var allRules: List<ReorderRuleDTO> = emptyList()

    init {
        loadRules()
    }

    fun loadRules() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                allRules = reorderRuleRepository.getReorderRules()
                applyFilter()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des règles")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                allRules = reorderRuleRepository.refreshRules()
                applyFilter()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de rafraîchissement")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun deleteRule(id: Int) {
        viewModelScope.launch {
            try {
                reorderRuleRepository.deleteReorderRule(id)
                loadRules()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        val filtered = allRules.filter { r ->
            query.isEmpty() ||
                r.productName?.lowercase()?.contains(query) == true
        }
        _state.value = if (filtered.isEmpty()) {
            ViewState.Empty
        } else {
            ViewState.Loaded(filtered)
        }
    }
}
