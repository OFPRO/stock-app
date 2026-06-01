package com.app2.feature.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.POSApiService
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

private fun JsonElement?.optString(): String? {
    val prim = this?.jsonPrimitive
    return if (prim != null && prim !is JsonNull) prim.content else null
}

private fun JsonElement?.optDouble(): Double? = this?.jsonPrimitive?.doubleOrNull
private fun JsonElement?.optInt(): Int? = this?.jsonPrimitive?.intOrNull

data class POSSession(
    val id: Int,
    val sessionNumber: String,
    val openingCash: Double,
    val status: String,
    val openedAt: String
)

@HiltViewModel
class POSSessionViewModel @Inject constructor(
    private val posApi: POSApiService
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
                val response = posApi.getSessions()
                val sessions = response.jsonArray
                if (sessions.isEmpty()) {
                    _session.value = ViewState.Loaded(null)
                } else {
                    val obj = sessions[0].jsonObject
                    _session.value = ViewState.Loaded(
                        POSSession(
                            id = obj["id"].optInt() ?: 0,
                            sessionNumber = obj["session_number"].optString() ?: "",
                            openingCash = obj["opening_cash"].optDouble() ?: 0.0,
                            status = obj["status"].optString() ?: "",
                            openedAt = obj["opened_at"].optString() ?: ""
                        )
                    )
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
                val response = posApi.openSession(body)
                val sessionObj = response.jsonObject["session"]?.jsonObject
                if (sessionObj == null) {
                    _session.value = ViewState.Error("Réponse invalide du serveur")
                    return@launch
                }
                _session.value = ViewState.Loaded(
                    POSSession(
                        id = sessionObj["id"].optInt() ?: 0,
                        sessionNumber = sessionObj["session_number"].optString() ?: "",
                        openingCash = sessionObj["opening_cash"].optDouble() ?: 0.0,
                        status = sessionObj["status"].optString() ?: "",
                        openedAt = sessionObj["opened_at"].optString() ?: ""
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
                val response = posApi.closeSession(current.id, body)
                val obj = response.jsonObject
                val success = obj["success"]?.jsonPrimitive?.content == "true"
                if (success) {
                    _session.value = ViewState.Loaded(null)
                } else {
                    _session.value = ViewState.Error(obj["message"].optString() ?: "Erreur de fermeture")
                }
            } catch (e: Exception) {
                _session.value = ViewState.Error(e.message ?: "Erreur de fermeture de session")
            }
        }
    }
}
