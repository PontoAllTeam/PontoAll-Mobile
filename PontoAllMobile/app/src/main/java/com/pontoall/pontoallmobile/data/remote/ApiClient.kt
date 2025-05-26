package com.pontoall.pontoallmobile.data.remote

import retrofit2.Retrofit

object ApiClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://localhost:7201/api/v1/")
        .build()

    val markPointService: MarkPointService = retrofit.create(MarkPointService::class.java)
}