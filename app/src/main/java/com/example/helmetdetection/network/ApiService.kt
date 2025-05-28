package com.example.helmetdetection.network

import com.example.helmetdetection.models.LogModel
import com.example.helmetdetection.models.LogPostRequest
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    @GET("get_log.php")
    fun getLog(): Call<LogResponse>
    @Headers("Content-Type: application/json")
    @POST("insert_log.php")
    fun insertLog(@Body request: LogPostRequest): Call<Map<String, String>>
}

data class LogResponse(
    @SerializedName("log_history") val log: List<LogModel>,
    @SerializedName("total_helm_false") val totalHelm: Int,
    @SerializedName("total_strap_false") val totalStrap: Int
)
