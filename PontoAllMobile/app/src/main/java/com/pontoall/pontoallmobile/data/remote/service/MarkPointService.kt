package com.pontoall.pontoallmobile.data.remote.service

import com.pontoall.pontoallmobile.data.remote.model.MarkPointDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MarkPointService {
    @POST("Markpoint")
    suspend fun create(@Body markPointDTO: MarkPointDTO): Response<MarkPointDTO>
}