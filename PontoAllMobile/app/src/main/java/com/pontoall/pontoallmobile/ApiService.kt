package com.pontoall.pontoallmobile

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

// --- Data Classes ---

data class User(
    val id: Int,
    val name: String,
    val cpf: String,
    val phone: String,
    val email: String,
    val recoveryEmail: String,
    val registration: String,
    val password: String,
    val userType: Int,
    val userStatus: Int,
    val companyId: Int,
    val sectorId: Int,
)

// Modelo para receber o histórico (GET)
data class TimeRecordResponse(
    val id: Int,
    val date: String?,
    val time: String?,
    val latitude: Double,
    val longitude: Double,
    val photo: String?
)

data class ApiResponse<T>(
    val code: Int,
    val data: T? = null,
    val message: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: User
)

// --- Interface da API ---

interface ApiService {
    // Login
    @POST("/api/v1/User/Login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    // Bater Ponto
    @POST("/api/v1/TimeRecord")
    suspend fun registrarPonto(
        @Header("Authorization") token: String,
        @Body request: TimeRecordRequest
    ): ApiResponse<Any>

    // Buscar Histórico (NOVO)
    @GET("/api/v1/TimeRecord")
    suspend fun obterHistorico(
        @Header("Authorization") token: String
    ): ApiResponse<List<TimeRecordResponse>>
}

// --- Configuração do Retrofit ---

object RetrofitClient {
    // ATENÇÃO: Verifique se este IP ainda é o correto da sua rede atual
    private const val API_URL = "http://192.168.42.9:5221"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(UnsafeOkHttpClient.getUnsafeOkHttpClient())
            .build()
        retrofit.create(ApiService::class.java)
    }
}

object UnsafeOkHttpClient {
    fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            return OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}