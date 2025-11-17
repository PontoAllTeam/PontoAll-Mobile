package com.pontoall.pontoallmobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pontoall.pontoallmobile.R

val DarkBlue = Color(0xFF021B2B)
val MediumGray = Color(0xFF909090)
val DarkPink = Color(0xFFB10C43)
val White = Color(0xFFFFFFFF)

@Composable
fun LoginScreen(onLoginClicked: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logopontoall),
                contentDescription = "Logo PontoAll",
                modifier = Modifier.size(200.dp)
            )

            // --- AQUI FOI ALTERADO: Espaçamento reduzido de 24.dp para 16.dp ---
            Spacer(modifier = Modifier.height(16.dp))
            // --- FIM DA ALTERAÇÃO ---

            Text(
                text = "Realizar Login",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bem vindo de volta!",
                fontSize = 16.sp,
                color = MediumGray
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkPink,
                    unfocusedBorderColor = MediumGray,
                    focusedLabelColor = DarkPink,
                    unfocusedLabelColor = MediumGray,
                    cursorColor = DarkPink,
                    focusedTextColor = DarkBlue,
                    unfocusedTextColor = DarkBlue
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkPink,
                    unfocusedBorderColor = MediumGray,
                    focusedLabelColor = DarkPink,
                    unfocusedLabelColor = MediumGray,
                    cursorColor = DarkPink,
                    focusedTextColor = DarkBlue,
                    unfocusedTextColor = DarkBlue
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    onLoginClicked(email, password)
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
                    text = "LOGIN",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(onLoginClicked = { email, password ->
            println("Email: $email, Senha: $password")
        })
    }
}