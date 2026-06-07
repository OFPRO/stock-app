package com.app2.core.data.repository

import com.app2.core.data.remote.MainAccountApiService
import com.app2.core.data.remote.dto.MainAccountDTO
import com.app2.core.data.remote.dto.deserialize
import kotlinx.serialization.json.JsonObject

class MainAccountRepository(
    private val api: MainAccountApiService
) {
    suspend fun getMainAccount(): MainAccountDTO {
        val response = api.getMainAccount()
        return response.deserialize<MainAccountDTO>()
    }

    suspend fun deposit(body: JsonObject) {
        api.deposit(body)
    }

    suspend fun withdraw(body: JsonObject) {
        api.withdraw(body)
    }

    suspend fun transferToPOS(body: JsonObject) {
        api.transferToPOS(body)
    }
}
