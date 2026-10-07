package com.example.clubdeportivo.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.EncabezadoAuth
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.FullScreenLoading
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.TextoSecundario

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
    val restaurando by viewModel.restaurando.observeAsState(true)
    val errorGeneral by viewModel.errorGeneral.observeAsState()
    val loginExitoso by viewModel.loginExitoso.observeAsState(false)
    val aviso by viewModel.aviso.observeAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(loginExitoso) {
        if (loginExitoso) {
            onLoginSuccess()
            viewModel.onNavegacionCompletada()
        }
    }

    LaunchedEffect(errorGeneral) {
        errorGeneral?.let { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
            viewModel.onErrorMostrado()
        }
    }

    LaunchedEffect(aviso) {
        aviso?.let { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
            viewModel.onAvisoMostrado()
        }
    }

    Scaffold(
        containerColor = FondoApp,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            EncabezadoAuth(subtitulo = "Accede a tu cuenta")

            if (restaurando) {
                // Revisando si el teléfono todavía recuerda la sesión: evita parpadear el formulario.
                Spacer(modifier = Modifier.height(48.dp))
                FullScreenLoading(modifier = Modifier.height(120.dp))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                        .navigationBarsPadding()
                ) {
                    EtiquetaCampo("CORREO ELECTRÓNICO")
                    CampoTexto(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "tu@correo.com",
                        tipoTeclado = KeyboardType.Email,
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Email, contentDescription = null) },
                        isError = emailError != null,
                        mensajeError = emailError
                    )

                    EspacioCampos()

                    EtiquetaCampo("CONTRASEÑA")
                    CampoTexto(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "••••••••",
                        esContrasena = !passwordVisible,
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        imeAction = ImeAction.Done,
                        onAccion = { viewModel.login(email, password) },
                        isError = passwordError != null,
                        mensajeError = passwordError
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { viewModel.olvideContrasena(email) }) {
                            Text(text = "¿Olvidaste tu contraseña?", color = Marca, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    BotonPrimario(
                        texto = "Iniciar sesión",
                        onClick = { viewModel.login(email, password) },
                        cargando = cargando
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿Eres miembro y aún no tienes cuenta?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextoSecundario
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        TextButton(onClick = onIrRegistro) {
                            Text(text = "Regístrate con tu código", color = Marca, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
