package com.navidrome.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.navidrome.auth.AuthManager

class MainActivity : ComponentActivity() {
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            NavidromeApp(authManager = authManager)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavidromeApp(authManager: AuthManager) {
    var baseUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggedIn by remember { mutableStateOf(authManager.isConfigured) }

    if (isLoggedIn) {
        // Pantalla principal (por ahora solo un placeholder)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("¡Conectado a Navidrome!")
            Spacer(modifier = Modifier.height(16.dp))
            Text("Usuario: ${authManager.getUsername()}")
            Text("URL: ${authManager.getBaseUrl()}")
        }
    } else {
        // Pantalla de login
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Soundtrack") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Conectar a Navidrome",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("URL del servidor") },
                    placeholder = { Text("http://tu-servidor:4533") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        
                        kotlinx.coroutines.GlobalScope.launch {
                            val result = authManager.login(baseUrl, username, password)
                            result.fold(
                                onSuccess = {
                                    isLoggedIn = true
                                },
                                onFailure = { e ->
                                    errorMessage = "Error al conectar: ${e.message}"
                                }
                            )
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Conectar")
                    }
                }
            }
        }
    }
}
