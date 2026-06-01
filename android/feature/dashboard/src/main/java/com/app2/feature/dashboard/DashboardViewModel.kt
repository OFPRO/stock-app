package com.app2.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.KPIApiService
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

data class DashboardDisplayData(
    val caToday: String = "0 MAD",
    val salesCount: String = "0",
    val averageTicket: String = "0 MAD",
    val grossMargin: String = "0 MAD",
    val receivables: String = "0 MAD",
    val collectionRate: String = "0%",
    val stockValue: String = "0 MAD",
    val stockouts: String = "0",
    val salesChartData: List<Pair<String, Double>> = emptyList(),
    val topProducts: List<Pair<String, Int>> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val kpiApi: KPIApiService
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<DashboardDisplayData>>(ViewState.Loading)
    val state = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                val kpiDeferred = async { kpiApi.getDashboardKPIs() }
                val salesDeferred = async { kpiApi.getSalesDaily(7) }
                val topProductsDeferred = async { kpiApi.getTopSellingProducts(5) }

                val kpiJson = kpiDeferred.await()
                val salesJson = salesDeferred.await()
                val topJson = topProductsDeferred.await()

                val data = parseDashboard(kpiJson, salesJson, topJson)
                _state.value = ViewState.Loaded(data)
            } catch (e: Exception) {
                _state.value = ViewState.Error(
                    e.message ?: "Erreur de chargement du tableau de bord"
                )
            }
        }
    }

    private fun parseDashboard(
        kpiJson: JsonElement,
        salesJson: JsonElement,
        topJson: JsonElement
    ): DashboardDisplayData {
        val obj = kpiJson.jsonObject

        fun getDouble(key: String): Double =
            obj[key]?.jsonPrimitive?.doubleOrNull ?: 0.0

        fun getString(key: String): String =
            obj[key]?.jsonPrimitive?.content ?: "0"

        val caToday = formatMoney(getDouble("ca_today"))
        val salesCount = getString("sales_count_today")
        val avgTicket = formatMoney(getDouble("average_ticket"))
        val margin = formatMoney(getDouble("gross_margin"))
        val receivables = formatMoney(getDouble("total_receivables"))
        val collection = formatPercent(getDouble("collection_rate"))
        val stockValue = formatMoney(getDouble("stock_value"))
        val stockouts = getString("stockout_count")

        val salesData: List<Pair<String, Double>> = salesJson.jsonArray.mapNotNull { item ->
            val date = item.jsonObject["date"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val total = item.jsonObject["total"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            Pair(date, total)
        }

        val topProducts: List<Pair<String, Int>> = topJson.jsonArray.mapNotNull { item ->
            val name = item.jsonObject["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val sold = item.jsonObject["total_sold"]?.jsonPrimitive?.intOrNull ?: 0
            Pair(name, sold)
        }

        return DashboardDisplayData(
            caToday = caToday,
            salesCount = salesCount,
            averageTicket = avgTicket,
            grossMargin = margin,
            receivables = receivables,
            collectionRate = collection,
            stockValue = stockValue,
            stockouts = stockouts,
            salesChartData = salesData,
            topProducts = topProducts
        )
    }

    private fun formatMoney(value: Double): String = "%.0f MAD".format(value)
    private fun formatPercent(value: Double): String = "%.0f%%".format(value)
}
