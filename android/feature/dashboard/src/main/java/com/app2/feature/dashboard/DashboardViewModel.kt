package com.app2.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.dto.AlertesDTO
import com.app2.core.data.remote.dto.CategoryDistributionDTO
import com.app2.core.data.remote.dto.DashboardKPIDTO
import com.app2.core.data.remote.dto.InvoicesStatusDTO
import com.app2.core.data.remote.dto.MainAccountDTO
import com.app2.core.data.remote.dto.MarginDTO
import com.app2.core.data.remote.dto.PaymentMethodsDTO
import com.app2.core.data.remote.dto.ReceivablesKPIDTO
import com.app2.core.data.remote.dto.SalesDayDTO
import com.app2.core.data.remote.dto.SalesKPIDTO
import com.app2.core.data.remote.dto.StockTrendDTO
import com.app2.core.data.remote.dto.TopProductDTO
import com.app2.core.data.repository.KPIRepository
import com.app2.core.data.repository.MainAccountRepository
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardDisplayData(
    val caToday: String = "0 MAD",
    val salesCount: String = "0",
    val averageTicket: String = "0 MAD",
    val grossMargin: String = "0%",
    val receivables: String = "0 MAD",
    val collectionRate: String = "0%",
    val stockValue: String = "0 MAD",
    val stockouts: String = "0",
    val totalEncaissement: String = "0 MAD",
    val cashAmount: String = "0 MAD",
    val cardAmount: String = "0 MAD",
    val mainAccountBalance: String = "0 MAD",
    val todayMovements: String = "0",
    val totalAlerts: String = "0",
    val rotationRate: String = "0%",
    val hasAlerts: Boolean = false,
    val salesChartData: List<SalesDayData> = emptyList(),
    val topProducts: List<TopProductData> = emptyList(),
    val categoryData: List<CategoryData> = emptyList(),
    val invoicesStatusData: InvoicesStatusData = InvoicesStatusData(),
    val trendData: List<StockTrendData> = emptyList(),
    val reorderItems: List<ReorderItem> = emptyList(),
    val lowStockItems: List<AlertItem> = emptyList(),
    val outOfStockItems: List<AlertItem> = emptyList(),
    val expiringItems: List<ExpiringItem> = emptyList(),
    val lowStockCount: String = "0",
    val totalProducts: String = "0"
)

data class SalesDayData(val date: String, val ca: Double, val nbVentes: Int)
data class TopProductData(val name: String, val qtySold: Int, val ca: Double)
data class CategoryData(val category: String, val qtyVendue: Int, val ca: Double)
data class InvoicesStatusData(
    val brouillon: Int = 0, val envoyee: Int = 0, val payee: Int = 0, val annulee: Int = 0
)
data class StockTrendData(val date: String, val entries: Int, val exits: Int, val net: Int)
data class ReorderItem(val name: String, val sku: String, val quantity: Int, val minQuantity: Int, val needed: Int)
data class AlertItem(val name: String, val sku: String, val quantity: Int)
data class ExpiringItem(val name: String, val lotNumber: String?, val daysLeft: Int, val quantity: Int)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val kpiRepository: KPIRepository,
    private val mainAccountRepository: MainAccountRepository
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
                val d = async { kpiRepository.getDashboardKPIs() }
                val s = async { kpiRepository.getSalesKPIs() }
                val sd = async { kpiRepository.getSalesDaily(7) }
                val tp = async { kpiRepository.getTopSellingProducts(5) }
                val m = async { kpiRepository.getMargins() }
                val r = async { kpiRepository.getReceivablesKPIs() }
                val pm = async { kpiRepository.getPaymentMethods() }
                val ma = async { mainAccountRepository.getMainAccount() }
                val al = async { kpiRepository.getAlertes() }
                val cd = async { kpiRepository.getCategoriesDistribution() }
                val iv = async { kpiRepository.getInvoicesStatus() }
                val tr = async { kpiRepository.getTrends() }

                val data = mapToDisplayData(
                    d.await(), s.await(), sd.await(), tp.await(),
                    m.await(), r.await(), pm.await(), ma.await(),
                    al.await(), cd.await(), iv.await(), tr.await()
                )
                _state.value = ViewState.Loaded(data)
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement")
            }
        }
    }

    private fun mapToDisplayData(
        dash: DashboardKPIDTO, sales: SalesKPIDTO, salesDaily: List<SalesDayDTO>,
        topJson: List<TopProductDTO>, margins: MarginDTO, receivables: ReceivablesKPIDTO,
        payments: PaymentMethodsDTO, mainAccount: MainAccountDTO,
        alertes: AlertesDTO, categories: List<CategoryDistributionDTO>,
        invoicesStatus: InvoicesStatusDTO, trends: List<StockTrendDTO>
    ): DashboardDisplayData {
        val caToday = formatMoney(sales.caJour ?: 0.0)
        val salesCount = (sales.nbVentesJour ?: 0).toString()
        val avgTicket = formatMoney(sales.ticketMoyen ?: 0.0)
        val margin = formatPercent(margins.margeGlobale ?: 0.0)
        val recv = formatMoney(receivables.totalCreances ?: 0.0)
        val collection = formatPercent(receivables.tauxEncaissement ?: 0.0)
        val stockValue = formatMoney(dash.totalValue ?: 0.0)
        val stockouts = (dash.outOfStock ?: 0).toString()
        val lowStock = (dash.lowStock ?: 0).toString()
        val totalProds = (dash.totalProducts ?: 0).toString()
        val todayMov = (dash.todayMovements ?: 0).toString()
        val totalAl = (dash.totalAlerts ?: 0).toString()
        val rotation = formatPercent(dash.rotationRate ?: 0.0)
        val hasAlerts = (dash.totalAlerts ?: 0) > 0

        val totalEnc = formatMoney(payments.total ?: 0.0)
        val methods = payments.methods
        val cashAmt = formatMoney(methods?.get("cash")?.total ?: 0.0)
        val cardAmt = formatMoney(methods?.get("card")?.total ?: 0.0)

        val mainBalance = formatMoney(mainAccount.account?.currentBalance ?: mainAccount.currentBalance ?: 0.0)

        val reorderItems: List<ReorderItem> = dash.productsToOrder?.mapNotNull { item ->
            val name = item.name ?: return@mapNotNull null
            ReorderItem(
                name = name, sku = item.sku ?: "",
                quantity = item.quantity ?: 0, minQuantity = item.minQuantity ?: 0,
                needed = item.needed ?: 0
            )
        } ?: emptyList()

        val lowStockItems = alertes.lowStock?.map { AlertItem(name = it.name ?: "", sku = it.sku ?: "", quantity = it.quantity ?: 0) } ?: emptyList()
        val outOfStockItems = alertes.outOfStock?.map { AlertItem(name = it.name ?: "", sku = it.sku ?: "", quantity = 0) } ?: emptyList()
        val expiringItems = alertes.expiring?.map { ExpiringItem(name = it.name ?: "", lotNumber = it.lotNumber, daysLeft = it.daysLeft ?: 0, quantity = it.quantity ?: 0) } ?: emptyList()

        val salesData = salesDaily.mapNotNull { item ->
            val date = item.date ?: return@mapNotNull null
            SalesDayData(date = date, ca = item.ca ?: 0.0, nbVentes = item.nbVentes ?: 0)
        }

        val topProducts = topJson.mapNotNull { item ->
            val name = item.name ?: return@mapNotNull null
            TopProductData(name = name, qtySold = item.qtyVendue ?: 0, ca = item.ca ?: 0.0)
        }

        val categoryData = categories.mapNotNull { item ->
            val cat = item.category ?: return@mapNotNull null
            CategoryData(category = cat, qtyVendue = item.qtyVendue ?: 0, ca = item.ca ?: 0.0)
        }

        val invStatusData = InvoicesStatusData(
            brouillon = invoicesStatus.brouillon ?: 0,
            envoyee = invoicesStatus.envoyee ?: 0,
            payee = invoicesStatus.payee ?: 0,
            annulee = invoicesStatus.annulee ?: 0
        )

        val trendData = trends.mapNotNull { item ->
            val date = item.date ?: return@mapNotNull null
            StockTrendData(date = date, entries = item.entries ?: 0, exits = item.exits ?: 0, net = item.net ?: 0)
        }

        return DashboardDisplayData(
            caToday = caToday, salesCount = salesCount,
            averageTicket = avgTicket, grossMargin = margin,
            receivables = recv, collectionRate = collection,
            stockValue = stockValue, stockouts = stockouts,
            totalEncaissement = totalEnc, cashAmount = cashAmt,
            cardAmount = cardAmt, mainAccountBalance = mainBalance,
            todayMovements = todayMov, totalAlerts = totalAl,
            rotationRate = rotation, hasAlerts = hasAlerts,
            salesChartData = salesData, topProducts = topProducts,
            categoryData = categoryData, invoicesStatusData = invStatusData,
            trendData = trendData, reorderItems = reorderItems,
            lowStockItems = lowStockItems, outOfStockItems = outOfStockItems,
            expiringItems = expiringItems, lowStockCount = lowStock,
            totalProducts = totalProds
        )
    }

    private fun formatMoney(value: Double): String = "%.0f MAD".format(value)
    private fun formatPercent(value: Double): String = "%.0f%%".format(value)
}
