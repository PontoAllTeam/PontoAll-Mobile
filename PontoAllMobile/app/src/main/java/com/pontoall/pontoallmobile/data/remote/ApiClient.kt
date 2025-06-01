package com.pontoall.pontoallmobile.data.remote

import com.pontoall.pontoallmobile.data.remote.interceptor.AuthInterceptor
import com.pontoall.pontoallmobile.data.remote.service.MarkPointService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = ""
    private const val TOKEN = ""

    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(TOKEN))
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val markPointService: MarkPointService = retrofit.create(MarkPointService::class.java)
}