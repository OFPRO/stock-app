package com.app2.core.data.repository

import com.app2.core.data.remote.KPIApiService
import com.app2.core.data.remote.dto.AlertesDTO
import com.app2.core.data.remote.dto.CategoryDistributionDTO
import com.app2.core.data.remote.dto.DashboardKPIDTO
import com.app2.core.data.remote.dto.InvoicesStatusDTO
import com.app2.core.data.remote.dto.MarginDTO
import com.app2.core.data.remote.dto.PaymentMethodsDTO
import com.app2.core.data.remote.dto.ReceivablesKPIDTO
import com.app2.core.data.remote.dto.SalesDayDTO
import com.app2.core.data.remote.dto.SalesKPIDTO
import com.app2.core.data.remote.dto.StockTrendDTO
import com.app2.core.data.remote.dto.TopProductDTO
import com.app2.core.data.remote.dto.deserialize

class KPIRepository(
    private val api: KPIApiService
) {
    suspend fun getDashboardKPIs(): DashboardKPIDTO {
        val response = api.getDashboardKPIs()
        return response.deserialize<DashboardKPIDTO>()
    }

    suspend fun getSalesKPIs(): SalesKPIDTO {
        val response = api.getSalesKPIs()
        return response.deserialize<SalesKPIDTO>()
    }

    suspend fun getSalesDaily(period: Int = 7): List<SalesDayDTO> {
        val response = api.getSalesDaily(period)
        return response.deserialize<List<SalesDayDTO>>()
    }

    suspend fun getTopSellingProducts(limit: Int = 10): List<TopProductDTO> {
        val response = api.getTopSellingProducts(limit)
        return response.deserialize<List<TopProductDTO>>()
    }

    suspend fun getMargins(): MarginDTO {
        val response = api.getMargins()
        return response.deserialize<MarginDTO>()
    }

    suspend fun getReceivablesKPIs(): ReceivablesKPIDTO {
        val response = api.getReceivablesKPIs()
        return response.deserialize<ReceivablesKPIDTO>()
    }

    suspend fun getPaymentMethods(): PaymentMethodsDTO {
        val response = api.getPaymentMethods()
        return response.deserialize<PaymentMethodsDTO>()
    }

    suspend fun getAlertes(): AlertesDTO {
        val response = api.getAlertes()
        return response.deserialize<AlertesDTO>()
    }

    suspend fun getCategoriesDistribution(): List<CategoryDistributionDTO> {
        val response = api.getCategoriesDistribution()
        return response.deserialize<List<CategoryDistributionDTO>>()
    }

    suspend fun getInvoicesStatus(): InvoicesStatusDTO {
        val response = api.getInvoicesStatus()
        return response.deserialize<InvoicesStatusDTO>()
    }

    suspend fun getTrends(): List<StockTrendDTO> {
        val response = api.getTrends()
        return response.deserialize<List<StockTrendDTO>>()
    }
}
