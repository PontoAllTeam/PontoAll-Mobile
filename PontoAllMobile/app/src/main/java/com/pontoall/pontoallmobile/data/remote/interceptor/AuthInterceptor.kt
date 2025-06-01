package com.pontoall.pontoallmobile.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestWithAuth = chain.request()
            .newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(requestWithAuth)
    }
}