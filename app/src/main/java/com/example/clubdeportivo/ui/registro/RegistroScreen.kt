package com.example.clubdeportivo.ui.registro

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.nombreLegible
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    onRegistroExitoso: () -> Unit,
    onVolverALogin: () -> Unit,
    viewModel: RegistroViewModel = viewModel()
) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rol by remember { mutableStateOf(Rol.SOCIO) }
    var menuRolAbierto by remember { mutableStateOf(false) }

    val nombreError by viewModel.nombreError.observeAsState()
    val emailError by viewModel.emailError.observeAsState()
    val passwordError by viewModel.passwordError.observeAsState()
    val cargando by viewModel.cargando.observeAsState(false)
    val errorGeneral by viewModel.errorGeneral.observeAsState()
    val registroExitoso by viewModel.registroExitoso.observeAsState(false)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(registroExitoso) {
        if (registroExitoso) onRegistroExitoso()
    }

    LaunchedEffect(errorGeneral) {
        errorGeneral?.let { mensaje ->
            scope.launch { snackbarHostState.showSnackbar(mensaje) }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF22C55E),
                                Color(0xFF16A34A)
                            )
                        )
                    ),
                contentAlignment = Alignment.TopStart
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "C",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "BIENVENIDO",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Club Deportivo",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Crea tu nueva cuenta",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.Top
            ) {
                EtiquetaCampo("NOMBRE COMPLETO")
                CampoTexto(
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholder = "Tu nombre"
                )

                EspacioCampos()

                EtiquetaCampo("CORREO ELECTRÓNICO")
                CampoTexto(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "tu@correo.com",
                    tipoTeclado = KeyboardType.Email
                )

                EspacioCampos()

                EtiquetaCampo("CONTRASEÑA")
                CampoTexto(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "••••••••",
                    esContrasena = !passwordVisible,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                )

                EspacioCampos()

                EtiquetaCampo("ROL")
                ExposedDropdownMenuBox(
                    expanded = menuRolAbierto,
                    onExpandedChange = { menuRolAbierto = it }
                ) {
                    CampoTexto(
                        value = rol.nombreLegible(),
                        onValueChange = {},
                        readOnly = true,
                        placeholder = "Selecciona un rol",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuRolAbierto) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = menuRolAbierto,
                        onDismissRequest = { menuRolAbierto = false }
                    ) {
                        Rol.entries.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion.nombreLegible()) },
                                onClick = {
                                    rol = opcion
                                    menuRolAbierto = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                BotonPrimario(
                    texto = "Crear cuenta",
                    onClick = { viewModel.registrar(nombre, email, password, rol) },
                    cargando = cargando
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "¿Ya tienes cuenta? ",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "Inicia sesión",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22C55E),
                        modifier = Modifier.clickable { onVolverALogin() }
                    )
                }
            }
        }
    }
}
