package com.pontoall.pontoallmobile

import LoginRequest
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var photoUri: Uri? = null
    private var lastLocation: Location? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            Log.d("MainActivity", "Foto capturada com sucesso: $photoUri")
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
                    onMarcarPonto = { handleMarcarPonto() }
                )
            }
        }
    }

    fun handleMarcarPonto() {
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
                        Log.d(
                            "MainActivity",
                            "Localização obtida: Lat ${location.latitude}, Lon ${location.longitude}"
                        )
                        iniciarCapturaDeFoto()
                    } else {
                        Toast.makeText(
                            this,
                            "Não foi possível obter a localização. Ative o GPS.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: SecurityException) {
                Log.e("MainActivity", "Erro de segurança ao obter localização.", e)
            }
        } else {
            Toast.makeText(this, "Permissão de localização não concedida.", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun iniciarCapturaDeFoto() {
        val photoFile =
            File.createTempFile("JPEG_${System.currentTimeMillis()}_", ".jpg", externalCacheDir)
        photoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            photoFile
        )
        photoUri?.let { uri -> takePictureLauncher.launch(uri) }
    }

    private fun enviarDadosParaAPI() {
        val userId = "id_do_usuario_logado"
        val location = this.lastLocation
        val imageUri = this.photoUri

        if (location == null || imageUri == null) {
            Toast.makeText(this, "Dados incompletos para envio.", Toast.LENGTH_LONG).show()
            return
        }

        Log.d("API_CALL", "Preparando para enviar dados para o TimeRecord:")
        Log.d("API_CALL", "UserID: $userId")
        Log.d("API_CALL", "Latitude: ${location.latitude}")
        Log.d("API_CALL", "Longitude: ${location.longitude}")
        Log.d("API_CALL", "URI da Foto: $imageUri")

        Toast.makeText(this, "Dados prontos para envio!", Toast.LENGTH_LONG).show()
        // Implementar a lógica de envio real para a API aqui
    }
}

// --- GERENCIADOR DE NAVEGAÇÃO ---
@Composable
fun AppNavigator(
    onMarcarPonto: () -> Unit
) {
    val context = LocalContext.current
    var telaAtual by remember { mutableStateOf("login") }
    val coroutineScope = rememberCoroutineScope()

    when (telaAtual) {
        "login" -> {
            LoginScreen(
                onLoginClicked = { email, password ->
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Email e senha são obrigatórios.", Toast.LENGTH_SHORT).show()
                        return@LoginScreen
                    }

                    // Inicia a chamada de rede em uma coroutine
                    coroutineScope.launch {
                        try {
                            val request = LoginRequest(email = email, password = password)
                            Log.d("LoginAPI", "Enviando requisição para o servidor...")
                            val response = RetrofitClient.instance.login(request)

                            // Sucesso!
                            Log.d("LoginAPI", "Login bem-sucedido! Token: ${response.data?.token}")
                            Toast.makeText(context, "Login bem-sucedido! Bem vindo(a), ${response.data?.user?.name}!", Toast.LENGTH_SHORT).show()

                            // Navega para a tela de ponto
                            telaAtual = "ponto"

                        } catch (e: Exception) {
                            // Erro!
                            Log.e("LoginAPI", "Falha no login: ${e.message}", e)
                            Toast.makeText(context, "Falha no login. Verifique as credenciais ou a rede.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
        "ponto" -> {
            PontoAllApp(
                onMarcarPontoClick = onMarcarPonto,
                onBackToLogin = { telaAtual = "login" }
            )
        }
    }
}
// --- COMPOSABLE DA TELA DE MARCAR PONTO (PontoAllApp) ---
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

    Surface(modifier = Modifier.fillMaxSize(), color = White) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TOP HEADER: Logo Centralizada e Ícone de Sair no Canto ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .height(80.dp) // Define uma altura para o header
            ) {
                // Logo centralizada independentemente do ícone de sair
                Image(
                    painter = painterResource(id = R.drawable.logopontoall),
                    contentDescription = "Logo PontoAll",
                    modifier = Modifier
                        .align(Alignment.Center) // Centraliza a logo dentro da Box
                        .size(120.dp) // Mantém o tamanho da logo
                )

                // Ícone de Sair no canto superior direito
                IconButton(
                    onClick = onBackToLogin,
                    modifier = Modifier.align(Alignment.CenterEnd) // <-- AQUI FOI ALTERADO: Alinha o botão ao CENTRO e ao FIM (direita)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = "Sair para Login",
                        tint = DarkPink,
                        modifier = Modifier.size(24.dp) // Tamanho pequeno para o ícone
                    )
                }
            }
            // --- FIM DO TOP HEADER ---

            // Spacer para empurrar o Card para o centro da página (verticalmente)
            Spacer(modifier = Modifier.weight(1f))

            // Card principal (centralizado horizontalmente pela Column pai)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F5FB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Fico feliz em ter você conosco novamente. Clique no botão abaixo para fazer o registro 🥳",
                        fontSize = 18.sp,
                        color = DarkBlue,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (permissionsState.allPermissionsGranted) {
                                onMarcarPontoClick()
                            } else {
                                Toast.makeText(context, "Por favor, conceda as permissões de localização e câmera.", Toast.LENGTH_LONG).show()
                                permissionsState.launchMultiplePermissionRequest()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkPink,
                            contentColor = White
                        )
                    ) {
                        Text(
                            text = "Marcar Ponto",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Spacer para empurrar o Card para o centro da página (verticalmente)
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// --- PREVIEWS ---
@Preview(showBackground = true, name = "Tela de Marcar Ponto")
@Composable
fun PontoAllAppPreview() {
    MaterialTheme {
        PontoAllApp(onMarcarPontoClick = {}, onBackToLogin = {})
    }
}