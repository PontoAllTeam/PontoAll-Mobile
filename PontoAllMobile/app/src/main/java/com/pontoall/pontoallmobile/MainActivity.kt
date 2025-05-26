package com.pontoall.pontoallmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pontoall.pontoallmobile.ui.theme.PontoAllMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PontoAllMobileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MarkPointButton(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MarkPointButton(modifier: Modifier = Modifier) {
    Text(
        text = "Marcar Ponto",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun MarkPointButtonPreview() {
    PontoAllMobileTheme {
        MarkPointButton()
    }
}