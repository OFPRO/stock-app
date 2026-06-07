package com.app2.core.data.repository

import com.app2.core.data.local.dao.LocationDao
import com.app2.core.data.local.entity.LocationEntity
import com.app2.core.data.remote.LocationApiService
import com.app2.core.data.remote.dto.LocationDTO
import com.app2.core.data.remote.dto.deserialize
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class LocationRepository(
    private val api: LocationApiService,
    private val locationDao: LocationDao
) {
    suspend fun getLocations(warehouseId: Int? = null): List<LocationDTO> {
        return try {
            val response = api.getLocations(warehouseId)
            val locations = response.deserialize<List<LocationDTO>>()
            locationDao.deleteAll()
            locationDao.insertAll(locations.map { it.toEntity() })
            locations
        } catch (e: Exception) {
            val cached = if (warehouseId != null) {
                locationDao.getByWarehouseId(warehouseId)
            } else {
                locationDao.getAll()
            }
            if (cached.isNotEmpty()) {
                cached.map { it.toDTO() }
            } else {
                throw e
            }
        }
    }

    suspend fun createLocation(body: JsonObject): JsonElement {
        val response = api.createLocation(body)
        val location = response.deserialize<LocationDTO>()
        locationDao.insertAll(listOf(location.toEntity()))
        return response
    }

    suspend fun updateLocation(id: Int, body: JsonObject): JsonElement {
        val response = api.updateLocation(id, body)
        val location = response.deserialize<LocationDTO>()
        locationDao.insertAll(listOf(location.toEntity()))
        return response
    }

    suspend fun deleteLocation(id: Int) {
        api.deleteLocation(id)
    }

    suspend fun refreshLocations(warehouseId: Int? = null): List<LocationDTO> {
        val response = api.getLocations(warehouseId)
        val locations = response.deserialize<List<LocationDTO>>()
        locationDao.deleteAll()
        locationDao.insertAll(locations.map { it.toEntity() })
        return locations
    }
}

private fun LocationDTO.toEntity() = LocationEntity(
    id = id,
    warehouseId = warehouseId,
    name = name,
    type = type ?: "rack",
    capacity = capacity,
    createdAt = createdAt
)

private fun LocationEntity.toDTO() = LocationDTO(
    id = id,
    warehouseId = warehouseId,
    name = name,
    type = type,
    capacity = capacity,
    createdAt = createdAt
)
