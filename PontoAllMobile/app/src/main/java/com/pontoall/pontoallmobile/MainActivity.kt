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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var photoUri: Uri? = null
    private var lastLocation: Location? = null

    // Variáveis globais de sessão
    private var currentToken: String = ""
    private var currentUserName: String = ""
    private var currentUserId: Int = 0

    // Estado global de carregamento e controle do Dialog
    private var isProcessing by mutableStateOf(false)
    private var showJustificationDialog by mutableStateOf(false)

    private var onPontoRegistradoCallback: ((TimeRecordResponse) -> Unit)? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            Log.d("MainActivity", "Foto capturada. Solicitando justificativa...")
            // Ao tirar a foto com sucesso, abre o diálogo em vez de enviar direto
            showJustificationDialog = true
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
                // Box permite sobrepor o Dialogo em cima do AppNavigator
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavigator(
                        isLoading = isProcessing,
                        onMarcarPonto = { token, nome, userId, callback ->
                            // --- CORREÇÃO AQUI: Removemos o "this." para evitar erro de escopo ---
                            currentToken = token
                            currentUserName = nome
                            currentUserId = userId
                            onPontoRegistradoCallback = callback
                            // --------------------------------------------------------------------

                            isProcessing = true
                            handleMarcarPonto()
                        }
                    )

                    // LÓGICA DO DIÁLOGO DE JUSTIFICATIVA
                    if (showJustificationDialog) {
                        JustificationDialog(
                            onConfirm = { justificativaDigitada ->
                                showJustificationDialog = false
                                // Envia para a API com o texto (pode ser vazio se ele pulou)
                                enviarDadosParaAPI(justificativaDigitada)
                            },
                            onDismiss = {
                                // Se o usuário clicar fora ou cancelar, cancelamos o processo
                                showJustificationDialog = false
                                isProcessing = false
                                Toast.makeText(this@MainActivity, "Registro cancelado.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
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
            Toast.makeText(this, "Permissão necessária.", Toast.LENGTH_LONG).show()
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

    private fun enviarDadosParaAPI(justificativa: String) {
        val location = this.lastLocation
        val workScheduleIdTeste = 0
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
                userId = this@MainActivity.currentUserId, // Aqui usamos this@MainActivity para garantir o contexto
                workScheduleId = workScheduleIdTeste,
                dailyRecordId = 0,
                photo = fotoString,
                justification = justificativa
            )

            try {
                val tokenFinal = if (currentToken.startsWith("Bearer ")) currentToken else "Bearer $currentToken"

                val response = RetrofitClient.instance.registrarPonto(tokenFinal, novoPonto)

                withContext(Dispatchers.Main) {
                    if (response.code == 200 || response.code == 201 || response.message?.contains("sucesso", ignoreCase = true) == true) {
                        Toast.makeText(this@MainActivity, "Ponto Registrado! ✅", Toast.LENGTH_SHORT).show()

                        val pontoCriado = response.data ?: TimeRecordResponse(
                            id = 0,
                            date = novoPonto.date,
                            time = novoPonto.time,
                            latitude = novoPonto.latitude,
                            longitude = novoPonto.longitude,
                            photo = null
                        )

                        onPontoRegistradoCallback?.invoke(pontoCriado)
                    } else {
                        Toast.makeText(this@MainActivity, "Erro: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msgErro = when (e) {
                        is SocketTimeoutException -> "Tempo limite esgotado."
                        is ConnectException -> "Servidor indisponível."
                        else -> "Erro: ${e.localizedMessage}"
                    }
                    Toast.makeText(this@MainActivity, msgErro, Toast.LENGTH_LONG).show()
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
    onMarcarPonto: (String, String, Int, (TimeRecordResponse) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var telaAtual by remember { mutableStateOf("login") }
    var userToken by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf(0) }

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

                            if (response.code == 200 || response.message?.contains("sucesso", ignoreCase = true) == true) {
                                userToken = response.data?.token ?: ""
                                userName = response.data?.user?.name ?: "Colaborador"
                                userId = response.data?.user?.id ?: 0

                                Toast.makeText(context, "Bem vindo, $userName!", Toast.LENGTH_SHORT).show()
                                telaAtual = "home"
                            } else {
                                Toast.makeText(context, "Erro: ${response.message}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            val msg = if (e is ConnectException) "Backend offline" else "Erro no login"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
        "home" -> {
            HomeScreen(
                userName = userName,
                userId = userId,
                userToken = userToken,
                isGlobalLoading = isLoading,
                onMarcarPontoClick = onMarcarPonto,
                onBackToLogin = { telaAtual = "login" }
            )
        }
    }
}

// --- TELA HOME ---
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    userName: String,
    userId: Int,
    userToken: String,
    isGlobalLoading: Boolean,
    onMarcarPontoClick: (String, String, Int, (TimeRecordResponse) -> Unit) -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA)
    )

    var historicoList by remember { mutableStateOf<List<TimeRecordResponse>>(emptyList()) }
    var isListLoading by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    var showComprovante by remember { mutableStateOf(false) }
    var pontoSelecionado by remember { mutableStateOf<TimeRecordResponse?>(null) }

    val coroutineScope = rememberCoroutineScope()

    fun carregarHistorico() {
        coroutineScope.launch {
            isListLoading = true
            try {
                val tokenFinal = if (userToken.startsWith("Bearer ")) userToken else "Bearer $userToken"
                val response = RetrofitClient.instance.obterHistorico(tokenFinal)
                if (response.data != null) {
                    historicoList = response.data.sortedWith(
                        compareByDescending<TimeRecordResponse> { it.date }
                            .thenByDescending { it.time }
                    ).take(10)
                }
            } catch (e: Exception) {
                Log.e("HomeAPI", "Erro ao carregar histórico", e)
            } finally {
                isListLoading = false
            }
        }
    }

    LaunchedEffect(Unit, refreshTrigger) { carregarHistorico() }
    LaunchedEffect(Unit) { permissionsState.launchMultiplePermissionRequest() }

    if (showComprovante && pontoSelecionado != null) {
        ComprovanteDialog(
            ponto = pontoSelecionado!!,
            userName = userName,
            onDismiss = { showComprovante = false }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F5)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 12.dp, horizontal = 24.dp)) {
                Image(painter = painterResource(id = R.drawable.logopontoall), contentDescription = "Logo", modifier = Modifier.align(Alignment.Center).height(40.dp))
                IconButton(onClick = onBackToLogin, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sair", tint = Color(0xFFB10C43))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Olá, $userName! 👋", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF021B2B))

            Spacer(modifier = Modifier.height(24.dp))

            // Botão de Registro
            Button(
                onClick = {
                    if (!isGlobalLoading) {
                        if (permissionsState.allPermissionsGranted) {
                            onMarcarPontoClick(userToken, userName, userId) { pontoCriado ->
                                refreshTrigger++
                                pontoSelecionado = pontoCriado
                                showComprovante = true
                            }
                        } else {
                            permissionsState.launchMultiplePermissionRequest()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.9f).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB10C43))
            ) {
                if (isGlobalLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("REGISTRAR PONTO AGORA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Histórico Recente", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF021B2B))
                IconButton(onClick = { carregarHistorico() }) { Icon(Icons.Filled.Refresh, "Atualizar", tint = Color.Gray) }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(historicoList) { ponto ->
                    HistoricoItem(ponto, onClick = {
                        pontoSelecionado = ponto
                        showComprovante = true
                    })
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun HistoricoItem(ponto: TimeRecordResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row {
                Icon(Icons.Filled.DateRange, null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(ponto.date ?: "--/--", fontWeight = FontWeight.Medium, color = Color(0xFF021B2B))
                    Text("ID: ${ponto.id}", fontSize = 12.sp, color = Color.LightGray)
                }
            }
            Row {
                Icon(Icons.Filled.AccessTime, null, tint = Color(0xFFB10C43))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ponto.time?.substringBefore(".") ?: "--:--",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF021B2B)
                )
            }
        }
    }
}

@Composable
fun ComprovanteDialog(ponto: TimeRecordResponse, userName: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("COMPROVANTE DE PONTO", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF021B2B))
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(16.dp))

                DetalheLinha("Colaborador:", userName)
                DetalheLinha("Data:", ponto.date ?: "--")
                DetalheLinha("Horário:", ponto.time?.substringBefore(".") ?: "--")

                val lat = String.format("%.4f", ponto.latitude)
                val lon = String.format("%.4f", ponto.longitude)
                DetalheLinha("Localização:", "$lat, $lon")

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("ID Registro: #${ponto.id}", fontSize = 12.sp, color = Color.LightGray)
                Text("Status: SINCRONIZADO", fontSize = 12.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF021B2B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("FECHAR")
                }
            }
        }
    }
}

@Composable
fun DetalheLinha(titulo: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(titulo, color = Color.Gray, fontSize = 14.sp)
        Text(valor, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF021B2B))
    }
}

// --- NOVO COMPONENTE: DIÁLOGO DE JUSTIFICATIVA ---
@Composable
fun JustificationDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ícone e Título
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFFB10C43),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Deseja justificar?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF021B2B)
                )

                Text(
                    text = "Opcional: Informe o motivo se o horário for diferente do esperado.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Campo de Texto
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Motivo (Ex: Trânsito, Médico)") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFB10C43),
                        focusedLabelColor = Color(0xFFB10C43)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botões
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Botão Pular/Enviar sem
                    TextButton(onClick = { onConfirm("") }) {
                        Text("Pular / Sem motivo", color = Color.Gray)
                    }

                    // Botão Enviar com texto
                    Button(
                        onClick = { onConfirm(text) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB10C43))
                    ) {
                        Text("Enviar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}