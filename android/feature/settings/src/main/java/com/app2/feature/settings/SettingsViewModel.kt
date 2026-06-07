package com.app2.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.network.ApiConstants
import com.app2.core.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val apiUrl: String = ApiConstants.BASE_URL,
    val isResetting: Boolean = false,
    val isSeeding: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    fun resetData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isResetting = true, message = null, error = null)
            try {
                adminRepository.resetData()
                _state.value = _state.value.copy(isResetting = false, message = "Données réinitialisées avec succès")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isResetting = false, error = "Erreur: ${e.message}")
            }
        }
    }

    fun seedData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSeeding = true, message = null, error = null)
            try {
                adminRepository.seedData()
                _state.value = _state.value.copy(isSeeding = false, message = "Données de test générées avec succès")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSeeding = false, error = "Erreur: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }
}
