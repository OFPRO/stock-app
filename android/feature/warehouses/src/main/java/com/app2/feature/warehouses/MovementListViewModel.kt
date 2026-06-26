package com.app2.feature.warehouses

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.network.PdfExporter
import com.app2.core.data.remote.MovementApiService
import com.app2.core.data.repository.MovementRepository
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovementListItem(
    val id: Int,
    val productId: Int,
    val productName: String,
    val type: String,
    val quantity: Int,
    val sourceLocation: String?,
    val destLocation: String?,
    val note: String?,
    val createdAt: String?
)

@HiltViewModel
class MovementListViewModel @Inject constructor(
    private val movementRepository: MovementRepository,
    private val movementApiService: MovementApiService,
    private val pdfExporter: PdfExporter
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<MovementListItem>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(0)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    private val _pdfEvent = MutableSharedFlow<Uri>()
    val pdfEvent = _pdfEvent.asSharedFlow()

    private var allMovements: List<MovementListItem> = emptyList()

    val filters = listOf("Tous", "Entrées", "Sorties", "Transferts")

    init {
        loadMovements()
    }

    fun loadMovements() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                allMovements = movementRepository.getMovements().map { dto ->
                    MovementListItem(
                        id = dto.id,
                        productId = dto.productId,
                        productName = dto.productName,
                        type = dto.type,
                        quantity = dto.quantity,
                        sourceLocation = dto.sourceLocation,
                        destLocation = dto.destLocation,
                        note = dto.note,
                        createdAt = dto.createdAt
                    )
                }
                applyFilters()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des mouvements")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun onFilterSelected(index: Int) {
        _selectedFilter.value = index
        applyFilters()
    }

    fun refresh() {
        allMovements = emptyList()
        loadMovements()
    }

    fun exportPdf() {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val typeParam = when (_selectedFilter.value) {
                    0 -> null
                    1 -> "in"
                    2 -> "out"
                    else -> null
                }
                val response = movementApiService.exportMovementsPdf(type = typeParam)
                val uri = pdfExporter.saveResponseAndGetUri(response, "mouvements.pdf")
                _pdfEvent.emit(uri)
            } catch (e: Exception) {
                _state.value = ViewState.Error("Erreur d'export PDF: ${e.message}")
            } finally {
                _isExporting.value = false
            }
        }
    }

    private fun applyFilters() {
        val query = _searchQuery.value.trim().lowercase()
        val filterIndex = _selectedFilter.value

        val filtered = allMovements.filter { m ->
            val matchesSearch = query.isEmpty() ||
                m.productName.lowercase().contains(query) ||
                m.note?.lowercase()?.contains(query) == true

            val matchesFilter = when (filterIndex) {
                0 -> true
                1 -> m.type == "in" || m.type == "reception"
                2 -> m.type == "out" || m.type == "sale"
                3 -> m.type == "transfer" || m.type == "inter_warehouse"
                else -> true
            }

            matchesSearch && matchesFilter
        }

        _state.value = if (filtered.isEmpty()) {
            ViewState.Empty
        } else {
            ViewState.Loaded(filtered)
        }
    }
}
