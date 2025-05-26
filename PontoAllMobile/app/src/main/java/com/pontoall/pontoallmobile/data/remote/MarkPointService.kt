package com.pontoall.pontoallmobile.data.remote

import com.pontoall.pontoallmobile.domain.model.MarkPoint
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MarkPointService {
    @POST("markpoint")
    suspend fun sendMarkPoint(@Body markPoint: MarkPoint): Response<Void>
}