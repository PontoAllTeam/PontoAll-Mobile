package com.pontoall.pontoallmobile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// OBS: Se der erro de "Unresolved reference: TimeRecordResponse",
// significa que faltou essa classe no seu ApiService.kt.
// Se der erro de "Redeclaration", apague esta classe daqui de baixo.
// Vou deixar comentado aqui caso você precise, mas o ideal é estar no ApiService.
/*
data class TimeRecordResponse(
    val id: Int,
    val date: String?,
    val time: String?,
    val latitude: Double,
    val longitude: Double,
    val photo: String?
)
*/

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var photoUri: Uri? = null
    private var lastLocation: Location? = null

    // Variáveis globais de sessão
    private var currentToken: String = ""
    private var currentUserName: String = ""

    // Estado global de carregamento
    private var isProcessing by mutableStateOf(false)

    // Callback para atualizar a lista
    private var onPontoRegistradoCallback: (() -> Unit)? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            Log.d("MainActivity", "Foto capturada. Iniciando envio...")
            enviarDadosParaAPI()
        } else {
            Toast.makeText(this, "Foto cancelada.", Toast.LENGTH_SHORT).show()
            isProcessing = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            MaterialTheme {
                AppNavigator(
                    isLoading = isProcessing,
                    onMarcarPonto = { token, nome, callback ->
                        this.currentToken = token
                        this.currentUserName = nome
                        this.onPontoRegistradoCallback = callback

                        isProcessing = true
                        handleMarcarPonto()
                    }
                )
            }
        }
    }

    private fun handleMarcarPonto() {
        obterLocalizacaoEProseguir()
    }

    private fun obterLocalizacaoEProseguir() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        this.lastLocation = location
                        iniciarCapturaDeFoto()
                    } else {
                        Toast.makeText(this, "GPS demorou, usando padrão.", Toast.LENGTH_SHORT).show()
                        iniciarCapturaDeFoto()
                    }
                }.addOnFailureListener {
                    iniciarCapturaDeFoto()
                }
            } catch (e: SecurityException) {
                Log.e("MainActivity", "Erro GPS", e)
                isProcessing = false
            }
        } else {
            Toast.makeText(this, "Permissão de localização necessária.", Toast.LENGTH_LONG).show()
            isProcessing = false
        }
    }

    private fun iniciarCapturaDeFoto() {
        try {
            val photoFile = File.createTempFile("JPEG_${System.currentTimeMillis()}_", ".jpg", externalCacheDir)
            photoUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", photoFile)
            photoUri?.let { uri -> takePictureLauncher.launch(uri) }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao criar arquivo: ${e.message}", Toast.LENGTH_LONG).show()
            isProcessing = false
        }
    }

    private fun converterFotoParaBase64(uri: Uri): String {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
                val bytes = outputStream.toByteArray()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else ""
        } catch (e: Exception) { "" }
    }

    private fun enviarDadosParaAPI() {
        val location = this.lastLocation
        val userIdTeste = 1
        val workScheduleIdTeste = 1
        val latFinal = location?.latitude ?: -20.469394
        val lonFinal = location?.longitude ?: -50.635562
        val agora = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        lifecycleScope.launch(Dispatchers.IO) {
            val fotoString = photoUri?.let { converterFotoParaBase64(it) } ?: ""

            val novoPonto = TimeRecordRequest(
                date = dateFormat.format(agora),
                time = timeFormat.format(agora),
                latitude = latFinal,
                longitude = lonFinal,
                userId = userIdTeste,
                workScheduleId = workScheduleIdTeste,
                dailyRecordId = 1,
                photo = fotoString
            )

            try {
                val tokenFinal = if (currentToken.startsWith("Bearer ")) currentToken else "Bearer $currentToken"
                val response = RetrofitClient.instance.registrarPonto(tokenFinal, novoPonto)

                withContext(Dispatchers.Main) {
                    if (response.code == 200 || response.code == 201 || response.message?.contains("sucesso", ignoreCase = true) == true) {
                        Toast.makeText(this@MainActivity, "Ponto Registrado com Sucesso! ✅", Toast.LENGTH_LONG).show()
                        onPontoRegistradoCallback?.invoke()
                    } else {
                        Toast.makeText(this@MainActivity, "Erro: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msgErro = when (e) {
                        is SocketTimeoutException -> "Tempo limite esgotado."
                        is ConnectException -> "Não foi possível conectar ao servidor."
                        else -> "Erro: ${e.localizedMessage}"
                    }
                    Toast.makeText(this@MainActivity, msgErro, Toast.LENGTH_LONG).show()
                    Log.e("API_CALL", "Erro detalhado: ", e)
                }
            } finally {
                isProcessing = false
            }
        }
    }
}

// --- NAVEGAÇÃO ---
@Composable
fun AppNavigator(
    isLoading: Boolean,
    onMarcarPonto: (String, String, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var telaAtual by remember { mutableStateOf("login") }
    var userToken by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    var refreshTrigger by remember { mutableStateOf(0) }

    when (telaAtual) {
        "login" -> {
            LoginScreen(
                onLoginClicked = { email, password ->
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Preencha os campos.", Toast.LENGTH_SHORT).show()
                        return@LoginScreen
                    }
                    coroutineScope.launch {
                        try {
                            val request = LoginRequest(email = email, password = password)
                            val response = RetrofitClient.instance.login(request)

                            // --- CORREÇÃO DO LOGIN AQUI ---
                            // Agora aceita se o código for 200 OU se a mensagem disser "sucesso"
                            if (response.code == 200 || response.message?.contains("sucesso", ignoreCase = true) == true) {
                                val tokenRecebido = response.data?.token ?: ""
                                val nomeRecebido = response.data?.user?.name ?: "Colaborador"

                                userToken = tokenRecebido
                                userName = nomeRecebido

                                Toast.makeText(context, "Bem vindo, $nomeRecebido!", Toast.LENGTH_SHORT).show()
                                telaAtual = "home"
                            } else {
                                Toast.makeText(context, "Erro: ${response.message}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            val msg = if (e is ConnectException) "Backend offline" else "Erro no login"
                            Toast.makeText(context, "$msg: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
        "home" -> {
            HomeScreen(
                userName = userName,
                userToken = userToken,
                isGlobalLoading = isLoading,
                refreshTrigger = refreshTrigger,
                onMarcarPontoClick = {
                    onMarcarPonto(userToken, userName) {
                        refreshTrigger++
                    }
                },
                onBackToLogin = { telaAtual = "login"; userToken = ""; userName = "" }
            )
        }
    }
}

// --- TELA HOME COMPLETA ---
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    userName: String,
    userToken: String,
    isGlobalLoading: Boolean,
    refreshTrigger: Int,
    onMarcarPontoClick: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA)
    )

    var historicoList by remember { mutableStateOf<List<TimeRecordResponse>>(emptyList()) }
    var isListLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun carregarHistorico() {
        coroutineScope.launch {
            isListLoading = true
            try {
                val tokenFinal = if (userToken.startsWith("Bearer ")) userToken else "Bearer $userToken"
                val response = RetrofitClient.instance.obterHistorico(tokenFinal)

                if (response.data != null) {
                    // --- ORDENAÇÃO DECRESCENTE (Mais recente no topo) ---
                    val listaOrdenada = response.data.sortedWith(
                        compareByDescending<TimeRecordResponse> { it.date }
                            .thenByDescending { it.time }
                    )

                    // --- LIMITA A 10 REGISTROS ---
                    historicoList = listaOrdenada.take(10)
                }
            } catch (e: Exception) {
                Log.e("HomeAPI", "Erro ao carregar histórico", e)
            } finally {
                isListLoading = false
            }
        }
    }

    LaunchedEffect(Unit, refreshTrigger) {
        carregarHistorico()
    }

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F5)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 12.dp, horizontal = 24.dp)
                    .height(60.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logopontoall),
                    contentDescription = "Logo",
                    modifier = Modifier.align(Alignment.Center).height(40.dp)
                )
                IconButton(
                    onClick = onBackToLogin,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sair",
                        tint = Color(0xFFB10C43)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SAUDAÇÃO
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Olá, $userName! 👋",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF021B2B)
                )
                Text(
                    text = "Vamos registrar seu ponto hoje?",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BOTÃO
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            if (!isGlobalLoading) {
                                if (permissionsState.allPermissionsGranted) {
                                    onMarcarPontoClick()
                                } else {
                                    permissionsState.launchMultiplePermissionRequest()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB10C43))
                    ) {
                        if (isGlobalLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("ENVIANDO...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("REGISTRAR PONTO AGORA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // LISTA TÍTULO
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Histórico Recente (Últimos 10)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF021B2B)
                )
                IconButton(onClick = { carregarHistorico() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Atualizar", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // LISTA DE ITENS
            if (isListLoading && historicoList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFB10C43))
                }
            } else if (historicoList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhum registro encontrado ainda.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historicoList) { ponto ->
                        HistoricoItem(ponto)
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun HistoricoItem(ponto: TimeRecordResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = ponto.date ?: "--/--",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF021B2B)
                    )
                    Text(
                        text = "ID: ${ponto.id}",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccessTime, contentDescription = null, tint = Color(0xFFB10C43), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ponto.time ?: "--:--",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF021B2B)
                )
            }
        }
    }
}