package com.example.clubdeportivo.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onIrRegistro: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val emailError by viewModel.emailError.observeAsState()
    val passwordError by viewModel.passwordError.observeAsState()
    val cargando by viewModel.cargando.observeAsState(false)
    val errorGeneral by viewModel.errorGeneral.observeAsState()
    val loginExitoso by viewModel.loginExitoso.observeAsState(false)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(loginExitoso) {
        if (loginExitoso) {
            onLoginSuccess()
            viewModel.onNavegacionCompletada()
        }
    }

    LaunchedEffect(errorGeneral) {
        errorGeneral?.let { mensaje ->
            scope.launch { snackbarHostState.showSnackbar(mensaje) }
        }
    }

    Scaffold(
        containerColor = Color(0xFFEAF1F8),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color(0xFF192338))
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF192338),
                                Color(0xFF1E2E4F)
                            )
                        )
                    ),
                contentAlignment = Alignment.TopStart
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "C",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "BIENVENIDO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8FB3E2),
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Club Deportivo",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Accede a tu cuenta",
                        fontSize = 14.sp,
                        color = Color(0xFF8FB3E2)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                val entraConCodigo = email.isNotBlank() && !email.contains("@")

                EtiquetaCampo("CORREO O CÓDIGO DE MIEMBRO")
                CampoTexto(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "tu@correo.com o CLB-7K3M9Q",
                    leadingIcon = {
                        Text(text = if (entraConCodigo) "#" else "@", color = Color(0xFF31487A), fontSize = 14.sp)
                    },
                    isError = emailError != null,
                    mensajeError = emailError
                )

                if (entraConCodigo) {
                    Text(
                        text = "Entras con tu código, no necesitas contraseña.",
                        fontSize = 12.sp,
                        color = Color(0xFF31487A),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    EspacioCampos()

                    EtiquetaCampo("CONTRASEÑA")
                    CampoTexto(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "••••••••",
                        esContrasena = !passwordVisible,
                        leadingIcon = { Text(text = "🔒", fontSize = 14.sp) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = Color(0xFF31487A)
                                )
                            }
                        },
                        isError = passwordError != null,
                        mensajeError = passwordError
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                BotonPrimario(
                    texto = "Iniciar sesión",
                    onClick = { viewModel.login(email, password) },
                    cargando = cargando
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "¿Personal del club sin cuenta? ",
                        fontSize = 13.sp,
                        color = Color(0xFF31487A)
                    )
                    Text(
                        text = "Regístrate aquí",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E2E4F),
                        modifier = Modifier.clickable { onIrRegistro() }
                    )
                }
            }
        }
    }
}