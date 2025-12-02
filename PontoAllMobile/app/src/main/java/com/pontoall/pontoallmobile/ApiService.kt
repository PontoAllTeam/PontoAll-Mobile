package com.pontoall.pontoallmobile

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

// --- Data Classes Existentes ---

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

// Wrapper padrão da sua API
data class ApiResponse<T>(
    val code: Int,
    val data: T? = null,
    val message: String? = null
)

// Classe para o corpo da requisição de login
data class LoginRequest(
    val email: String,
    val password: String
)

// Classe para a resposta do login
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
}

// --- Configuração do Retrofit (Singleton) ---

object RetrofitClient {
    // ATUALIZADO: IP DA FACULDADE (192.168.100.154)
    private const val API_URL = "http://192.168.100.154:5221"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            // Mantemos o client para evitar erros, mas no HTTP ele ignora SSL
            .client(UnsafeOkHttpClient.getUnsafeOkHttpClient())
            .build()
        retrofit.create(ApiService::class.java)
    }
}

// --- Cliente HTTP (Mantido por compatibilidade) ---

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