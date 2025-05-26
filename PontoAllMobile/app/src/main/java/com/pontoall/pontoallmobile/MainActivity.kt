package com.pontoall.pontoallmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.pontoall.pontoallmobile.ui.theme.PontoAllMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PontoAllMobileTheme {
                Surface {
                    MarkPointButton()
                }
            }
        }
    }
}

@Composable
fun MarkPointButton() {
    Button(onClick = {

    }) {
        Text("Marcar Ponto")
    }
}

@Preview(showBackground = true)
@Composable
fun MarkPointButtonPreview() {
    PontoAllMobileTheme {
        MarkPointButton()
    }
}