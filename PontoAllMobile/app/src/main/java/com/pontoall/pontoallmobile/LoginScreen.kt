package com.pontoall.pontoallmobile // Garante que o pacote está correto e no topo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// As definições de cores estão no lugar certo e corretas
val DarkBlue = Color(0xFF021B2B)
val MediumGray = Color(0xFF909090)
val DarkPink = Color(0xFFB10C43)
val White = Color(0xFFFFFFFF)

@Composable
fun LoginScreen(onLoginClicked: (String, String) -> Unit) {
    // Estados para armazenar o que o usuário digita no email e na senha
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = White // Cor de fundo da tela
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp), // Adiciona um espaçamento nas laterais
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título: "Realizar Login"
            Text(
                text = "Realizar Login",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtítulo: "Bem vindo de volta!"
            Text(
                text = "Bem vindo de volta!",
                fontSize = 16.sp,
                color = MediumGray
            )

            Spacer(modifier = Modifier.height(48.dp)) // Espaço maior

            // Campo de Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkPink,    // Cor da borda quando o campo está focado
                    unfocusedBorderColor = MediumGray,// Cor da borda quando não está focado
                    focusedLabelColor = DarkPink,     // Cor do label "Email" quando focado
                    unfocusedLabelColor = MediumGray, // Cor do label quando não focado
                    cursorColor = DarkPink,           // Cor do cursor de digitação
                    focusedTextColor = DarkBlue,      // Cor do texto digitado
                    unfocusedTextColor = DarkBlue
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Senha
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(), // Esconde a senha
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

            // Botão de Login
            Button(
                onClick = {
                    // Quando o botão é clicado, chama a função passada como parâmetro
                    onLoginClicked(email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium, // Bordas levemente arredondadas
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkPink, // Cor de fundo do botão
                    contentColor = White       // Cor do texto do botão
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

// Esta função de Preview permite visualizar a tela de login no painel de Design do Android Studio
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(onLoginClicked = { email, password ->
        // Ação de exemplo para o preview
        println("Email: $email, Senha: $password")
    })
}