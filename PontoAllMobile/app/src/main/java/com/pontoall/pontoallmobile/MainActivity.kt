package com.pontoall.pontoallmobile

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var photoUri: Uri? = null
    private var lastLocation: Location? = null

    // Contrato para tirar foto
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
            // A UI do nosso app
            PontoAllApp(
                onMarcarPontoClick = {
                    // Lógica que acontece quando o botão é clicado
                    obterLocalizacaoEProseguir()
                }
            )
        }
    }

    private fun obterLocalizacaoEProseguir() {
        // Checamos a permissão antes de tentar obter a localização
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
            // Se a permissão não foi dada, o Composable cuidará de pedir.
            // O usuário precisará clicar no botão de novo após dar a permissão.
            Toast.makeText(
                this,
                "Por favor, conceda a permissão de localização e tente novamente.",
                Toast.LENGTH_LONG
            ).show()
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
        photoUri?.let { uri ->
            takePictureLauncher.launch(uri)
        }
    }

    private fun enviarDadosParaAPI() {
        val userId = "id_do_usuario_exemplo"
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

        // --- A LÓGICA DE ENVIO PARA O BACKEND ENTRARÁ AQUI (usando Retrofit) ---
    }
}

// --- UI definida com Jetpack Compose ---
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PontoAllApp(onMarcarPontoClick: () -> Unit) {
    val context = LocalContext.current
    // Gerenciador de permissões do Accompanist
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )
    )

    // Lançado na primeira vez que o Composable é exibido
    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = {
                    if (permissionsState.allPermissionsGranted) {
                        // Se todas as permissões estão OK, executa a ação
                        onMarcarPontoClick()
                    } else {
                        // Se não, pede de novo
                        Toast.makeText(
                            context,
                            "Por favor, conceda as permissões de câmera e localização.",
                            Toast.LENGTH_LONG
                        ).show()
                        permissionsState.launchMultiplePermissionRequest()
                    }
                }) {
                    Text("Marcar Ponto")
                }
            }
        }
    }
}