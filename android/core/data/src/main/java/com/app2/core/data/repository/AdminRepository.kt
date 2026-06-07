package com.app2.core.data.repository

import com.app2.core.data.remote.AdminApiService

class AdminRepository(
    private val api: AdminApiService
) {
    suspend fun resetData() {
        api.resetData()
    }

    suspend fun seedData() {
        api.seedData()
    }
}
