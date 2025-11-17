package com.pontoall.pontoallmobile // <-- LINHA ADICIONADA: Declara o pacote corretamente

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File

// Importa a LoginScreen do seu outro arquivo
import com.pontoall.pontoallmobile.LoginScreen // <-- ADICIONADO: Importa a LoginScreen
import com.pontoall.pontoallmobile.DarkBlue // <-- ADICIONADO: Importa a DarkBlue (e outras se usar)
import com.pontoall.pontoallmobile.MediumGray
import com.pontoall.pontoallmobile.DarkPink
import com.pontoall.pontoallmobile.White


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

    // --- FUNÇÕES DE LÓGICA (câmera, localização, API) ---

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
    }
}

// --- GERENCIADOR DE NAVEGAÇÃO ---
@Composable
fun AppNavigator(
    onMarcarPonto: () -> Unit
) {
    val context = LocalContext.current
    var telaAtual by remember { mutableStateOf("login") }

    when (telaAtual) {
        "login" -> {
            LoginScreen( // Usamos a LoginScreen importada do outro arquivo
                onLoginClicked = { email, password ->
                    Log.d("LoginAttempt", "Email: $email, Senha (tamanho): ${password.length}")
                    Toast.makeText(context, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()
                    telaAtual = "ponto"
                }
            )
        }

        "ponto" -> {
            PontoAllApp(
                onMarcarPontoClick = onMarcarPonto
            )
        }
    }
}

// --- COMPOSABLE DA TELA DE MARCAR PONTO ---
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PontoAllApp(onMarcarPontoClick: () -> Unit) {
    val context = LocalContext.current
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA)
    )

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                if (permissionsState.allPermissionsGranted) {
                    onMarcarPontoClick()
                } else {
                    Toast.makeText(context, "Por favor, conceda as permissões.", Toast.LENGTH_LONG)
                        .show()
                    permissionsState.launchMultiplePermissionRequest()
                }
            }) {
                Text("Marcar Ponto")
            }
        }
    }
}

// --- PREVIEWS ---
@Preview(showBackground = true, name = "Tela de Marcar Ponto")
@Composable
fun PontoAllAppPreview() {
    MaterialTheme {
        PontoAllApp(onMarcarPontoClick = {})
    }
}