package com.app2.core.data.repository

import com.app2.core.data.local.dao.ProductDao
import com.app2.core.data.local.entity.ProductEntity
import com.app2.core.data.remote.ProductApiService
import com.app2.core.data.remote.dto.CategoryDTO
import com.app2.core.data.remote.dto.ForSaleProductDTO
import com.app2.core.data.remote.dto.ProductDetailDTO
import com.app2.core.data.remote.dto.deserialize
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class ProductRepository(
    private val api: ProductApiService,
    private val productDao: ProductDao
) {
    suspend fun getProducts(includeArchived: Boolean = false): List<ProductDetailDTO> {
        return try {
            val response = api.getProducts(includeArchived)
            val products = response.deserialize<List<ProductDetailDTO>>()
            productDao.deleteAll()
            productDao.insertAll(products.map { it.toEntity() })
            products
        } catch (e: Exception) {
            val cached = productDao.getAllActive()
            if (cached.isNotEmpty()) {
                cached.map { it.toDTO() }
            } else {
                throw e
            }
        }
    }

    suspend fun getProductRaw(id: Int): JsonElement {
        return api.getProduct(id)
    }

    suspend fun getProduct(id: Int): ProductDetailDTO {
        return try {
            val response = api.getProduct(id)
            val product = response.deserialize<ProductDetailDTO>()
            productDao.insert(product.toEntity())
            product
        } catch (e: Exception) {
            val cached = productDao.getById(id)
            cached?.toDTO() ?: throw e
        }
    }

    suspend fun getProductsForSale(
        search: String? = null,
        customerId: Int? = null
    ): List<ForSaleProductDTO> {
        val response = api.getProductsForSale(search, customerId)
        return response.deserialize<List<ForSaleProductDTO>>()
    }

    suspend fun getCategories(): List<CategoryDTO> {
        val response = api.getCategories()
        return response.deserialize<List<CategoryDTO>>()
    }

    suspend fun createProduct(body: JsonObject): JsonElement {
        val response = api.createProduct(body)
        val product = response.deserialize<ProductDetailDTO>()
        productDao.insert(product.toEntity())
        return response
    }

    suspend fun updateProduct(id: Int, body: JsonObject): JsonElement {
        val response = api.updateProduct(id, body)
        val product = response.deserialize<ProductDetailDTO>()
        productDao.insert(product.toEntity())
        return response
    }

    suspend fun deleteProduct(id: Int) {
        api.deleteProduct(id)
        productDao.insert(ProductEntity(id = id, name = "", isDeleted = true))
    }

    suspend fun search(query: String): List<ProductDetailDTO> {
        val cached = productDao.search(query)
        return cached.map { it.toDTO() }
    }

    suspend fun refreshProducts(includeArchived: Boolean = false): List<ProductDetailDTO> {
        val response = api.getProducts(includeArchived)
        val products = response.deserialize<List<ProductDetailDTO>>()
        productDao.deleteAll()
        productDao.insertAll(products.map { it.toEntity() })
        return products
    }
}

private fun ProductDetailDTO.toEntity() = ProductEntity(
    id = id,
    name = name,
    description = description,
    sku = sku,
    barcode = barcode,
    quantity = quantity,
    minQuantity = minStock ?: 5,
    maxQuantity = maxStock ?: 100,
    price = price,
    priceBase = priceBase,
    priceLoyal = priceLoyal,
    priceSchool = priceSchool,
    priceStudent = priceStudent,
    category = category ?: "Général",
    warehouseId = warehouseId,
    locationId = locationId,
    isDeleted = isDeleted == 1,
    purchasePriceAvg = purchasePrice,
    discountCategory = discountCategory ?: "aucun",
    marginPercent = marginPercent ?: 15.0,
    isLiquidation = isLiquidation == 1,
    extraPrices = extraPrices ?: "[]",
    createdAt = createdAt
)

private fun ProductEntity.toDTO() = ProductDetailDTO(
    id = id,
    name = name,
    sku = sku ?: "",
    description = description,
    price = price,
    quantity = quantity,
    category = category,
    barcode = barcode,
    minStock = minQuantity,
    maxStock = maxQuantity,
    isDeleted = if (isDeleted) 1 else 0,
    weight = null,
    unit = null,
    purchasePrice = purchasePriceAvg,
    wholesalePrice = null,
    priceBase = priceBase,
    priceLoyal = priceLoyal,
    priceSchool = priceSchool,
    priceStudent = priceStudent,
    taxCategory = null,
    discountCategory = discountCategory,
    marginPercent = marginPercent,
    isLiquidation = if (isLiquidation) 1 else 0,
    extraPrices = extraPrices,
    warehouseId = warehouseId,
    locationId = locationId,
    createdAt = createdAt
)
