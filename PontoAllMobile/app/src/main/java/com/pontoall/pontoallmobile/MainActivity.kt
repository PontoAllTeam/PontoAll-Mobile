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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var photoUri: Uri? = null
    private var lastLocation: Location? = null

    // Variável para segurar o token recebido do login
    private var currentToken: String = ""

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            Log.d("MainActivity", "Foto capturada com sucesso: $photoUri")
            // Se a foto deu certo, enviamos tudo para a API
            enviarDadosParaAPI()
        } else {
            Toast.makeText(this, "Captura de foto cancelada.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            MaterialTheme {
                AppNavigator(
                    onMarcarPonto = { tokenRecebido ->
                        this.currentToken = tokenRecebido
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
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        this.lastLocation = location
                        Log.d("MainActivity", "Localização: Lat ${location.latitude}, Lon ${location.longitude}")
                        iniciarCapturaDeFoto()
                    } else {
                        Toast.makeText(this, "Aviso: GPS demorou, usando localização padrão.", Toast.LENGTH_SHORT).show()
                        iniciarCapturaDeFoto()
                    }
                }
            } catch (e: SecurityException) {
                Log.e("MainActivity", "Erro de segurança GPS.", e)
            }
        } else {
            Toast.makeText(this, "Permissão de localização necessária.", Toast.LENGTH_LONG).show()
        }
    }

    private fun iniciarCapturaDeFoto() {
        try {
            val photoFile = File.createTempFile("JPEG_${System.currentTimeMillis()}_", ".jpg", externalCacheDir)
            photoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                photoFile
            )
            photoUri?.let { uri -> takePictureLauncher.launch(uri) }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao criar arquivo de foto: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // --- ATUALIZADO: Agora compacta a foto para não ficar gigante ---
    private fun converterFotoParaBase64(uri: Uri): String {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            // 1. Decodifica para Bitmap
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                // 2. Comprime para JPEG com 50% de qualidade (Reduz MUITO o tamanho)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
                val bytes = outputStream.toByteArray()

                // 3. Converte para Base64
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun enviarDadosParaAPI() {
        val location = this.lastLocation

        // IDs DE TESTE
        val userIdTeste = 1
        val workScheduleIdTeste = 1

        val latFinal = location?.latitude ?: -20.469394
        val lonFinal = location?.longitude ?: -50.635562

        val agora = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        lifecycleScope.launch(Dispatchers.IO) {

            // 1. Converte a foto (agora compactada)
            val fotoString = photoUri?.let { converterFotoParaBase64(it) } ?: ""
            Log.d("API_CALL", "Foto convertida. Novo tamanho da string: ${fotoString.length}")

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

            Log.e("DEBUG_TOKEN", "Enviando Ponto...")

            try {
                val tokenFinal = if (currentToken.startsWith("Bearer ")) currentToken else "Bearer $currentToken"

                val response = RetrofitClient.instance.registrarPonto(tokenFinal, novoPonto)

                withContext(Dispatchers.Main) {
                    // VERIFICAÇÃO DE SUCESSO FLEXÍVEL
                    if (response.code == 200 || response.code == 201 || response.message?.contains("sucesso", ignoreCase = true) == true) {
                        Toast.makeText(this@MainActivity, "Ponto Registrado com Sucesso! 📸✅", Toast.LENGTH_LONG).show()
                        Log.d("API_CALL", "Sucesso: ${response.message}")
                    } else {
                        Toast.makeText(this@MainActivity, "Erro: ${response.message}", Toast.LENGTH_LONG).show()
                        Log.e("API_CALL", "Erro API: ${response.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Falha de conexão: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("API_CALL", "Exceção: ", e)
                }
            }
        }
    }
}

// ==========================================
// AQUI ESTÁ O APP NAVIGATOR
// ==========================================

@Composable
fun AppNavigator(
    onMarcarPonto: (String) -> Unit
) {
    val context = LocalContext.current
    var telaAtual by remember { mutableStateOf("login") }
    var userToken by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

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

                            val tokenRecebido = response.data?.token ?: ""
                            userToken = tokenRecebido

                            Toast.makeText(context, "Bem vindo, ${response.data?.user?.name}!", Toast.LENGTH_SHORT).show()
                            telaAtual = "ponto"

                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro no login: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
        "ponto" -> {
            PontoAllApp(
                onMarcarPontoClick = { onMarcarPonto(userToken) },
                onBackToLogin = { telaAtual = "login"; userToken = "" }
            )
        }
    }
}

// ==========================================
// TELA DE MARCAR PONTO (UI)
// ==========================================

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PontoAllApp(onMarcarPontoClick: () -> Unit, onBackToLogin: () -> Unit) {
    val context = LocalContext.current
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA)
    )

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .height(80.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logopontoall),
                    contentDescription = "Logo",
                    modifier = Modifier.align(Alignment.Center).size(120.dp)
                )
                IconButton(
                    onClick = onBackToLogin,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sair",
                        tint = Color(0xFFB10C43),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F5FB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Clique no botão abaixo para registrar seu ponto 📍",
                        fontSize = 18.sp,
                        color = Color(0xFF021B2B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (permissionsState.allPermissionsGranted) {
                                onMarcarPontoClick()
                            } else {
                                Toast.makeText(context, "Aceite as permissões.", Toast.LENGTH_LONG).show()
                                permissionsState.launchMultiplePermissionRequest()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB10C43))
                    ) {
                        Text("MARCAR PONTO", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}