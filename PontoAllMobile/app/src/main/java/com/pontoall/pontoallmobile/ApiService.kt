import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

// 1. Classe para o corpo da requisição de login
data class LoginRequest(
    val email: String,
    val password: String
)

// 2. Classe para a resposta do login
data class LoginResponse(
    val token: String,
    val userId: String,
    val name: String
)

// 3. Interface da API para o Retrofit
interface ApiService {
    @POST("/api/v1/User/Login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}

// 4. Objeto Singleton do Retrofit
object RetrofitClient {
    // MUDANÇA AQUI: Usamos 127.0.0.1 (localhost) para funcionar via cabo USB com 'adb reverse'
    // Isso evita problemas com bloqueios de firewall da empresa ou mudança de IP do Wi-Fi
    private const val API_URL = "https://127.0.0.1:7201"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(UnsafeOkHttpClient.getUnsafeOkHttpClient()) // Client para aceitar o certificado de dev
            .build()
        retrofit.create(ApiService::class.java)
    }
}

// 5. Client OkHttp que confia em certificados não seguros (APENAS PARA DESENVOLVIMENTO)
object UnsafeOkHttpClient {
    fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            // Cria um trust manager que não valida as cadeias de certificado
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            // Instala o trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            return OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true } // Aceita qualquer hostname (importante para 127.0.0.1)
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}