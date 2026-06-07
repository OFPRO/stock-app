package com.app2.feature.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.dto.POSSessionDTO
import com.app2.core.data.repository.POSRepository
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

data class POSSession(
    val id: Int,
    val sessionNumber: String,
    val openingCash: Double,
    val status: String,
    val openedAt: String
)

@HiltViewModel
class POSSessionViewModel @Inject constructor(
    private val posRepository: POSRepository
) : ViewModel() {

    private val _session = MutableStateFlow<ViewState<POSSession?>>(ViewState.Loading)
    val session = _session.asStateFlow()

    private val _openingCash = MutableStateFlow("")
    val openingCash = _openingCash.asStateFlow()

    init {
        checkSession()
    }

    fun onOpeningCashChanged(value: String) { _openingCash.value = value }

    fun checkSession() {
        viewModelScope.launch {
            _session.value = ViewState.Loading
            try {
                val sessions = posRepository.getSessions()
                val latest = sessions.firstOrNull()
                _session.value = if (latest != null) {
                    ViewState.Loaded(
                        POSSession(
                            id = latest.id,
                            sessionNumber = latest.sessionNumber ?: "",
                            openingCash = latest.openingCash ?: 0.0,
                            status = latest.status ?: "",
                            openedAt = latest.openedAt ?: ""
                        )
                    )
                } else {
                    ViewState.Loaded(null)
                }
            } catch (e: Exception) {
                _session.value = ViewState.Error(e.message ?: "Erreur de vérification de session")
            }
        }
    }

    fun openSession() {
        viewModelScope.launch {
            try {
                val cash = _openingCash.value.toDoubleOrNull() ?: 0.0
                val body = buildJsonObject { put("opening_cash", cash) }
                val result = posRepository.openSession(body)
                val s = result.session
                if (s == null) {
                    _session.value = ViewState.Error("Réponse invalide du serveur")
                    return@launch
                }
                _session.value = ViewState.Loaded(
                    POSSession(
                        id = s.id,
                        sessionNumber = s.sessionNumber ?: "",
                        openingCash = s.openingCash ?: 0.0,
                        status = s.status ?: "",
                        openedAt = s.openedAt ?: ""
                    )
                )
            } catch (e: Exception) {
                _session.value = ViewState.Error(e.message ?: "Erreur d'ouverture de session")
            }
        }
    }

    fun closeSession(closingCash: Double, depositToMain: Boolean = true) {
        val current = (_session.value as? ViewState.Loaded)?.data ?: return
        viewModelScope.launch {
            try {
                val body = buildJsonObject {
                    put("closing_cash", closingCash)
                    put("deposit_to_main", depositToMain)
                }
                posRepository.closeSession(current.id, body)
                _session.value = ViewState.Loaded(null)
            } catch (e: Exception) {
                _session.value = ViewState.Error(e.message ?: "Erreur de fermeture de session")
            }
        }
    }
}
