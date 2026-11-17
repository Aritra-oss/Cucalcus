package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class OpenErApiResponse(
    @param:Json(name = "result") val result: String? = null,
    @param:Json(name = "time_last_update_unix") val timeLastUpdateUnix: Long? = null,
    @param:Json(name = "base_code") val baseCode: String? = null,
    @param:Json(name = "rates") val rates: Map<String, Double>? = null
)

interface CurrencyApiService {
    @GET("v6/latest/{base}")
    suspend fun getLatestRates(@Path("base") baseCurrency: String): OpenErApiResponse
}
