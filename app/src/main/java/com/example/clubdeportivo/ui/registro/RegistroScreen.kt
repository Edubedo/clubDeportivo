package com.example.clubdeportivo.ui.registro

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.BotonSecundario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.EncabezadoAuth
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.RequisitosContrasena
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.theme.Exito
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.MarcaSuave
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario

@Composable
fun RegistroScreen(
    onRegistroExitoso: () -> Unit,
    onVolverALogin: () -> Unit,
    viewModel: RegistroViewModel = viewModel()
) {
    var codigoTexto by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val paso by viewModel.paso.observeAsState(PasoRegistro.CODIGO)
    val codigo by viewModel.codigo.observeAsState("")
    val yaRegistrado by viewModel.yaRegistrado.observeAsState(false)
    val codigoError by viewModel.codigoError.observeAsState()
    val nombreError by viewModel.nombreError.observeAsState()
    val emailError by viewModel.emailError.observeAsState()
    val passwordError by viewModel.passwordError.observeAsState()
    val confirmarError by viewModel.confirmarError.observeAsState()
    val cargando by viewModel.cargando.observeAsState(false)
    val errorGeneral by viewModel.errorGeneral.observeAsState()
    val registroExitoso by viewModel.registroExitoso.observeAsState(false)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(registroExitoso) {
        if (registroExitoso) onRegistroExitoso()
    }

    LaunchedEffect(errorGeneral) {
        errorGeneral?.let { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
            viewModel.onErrorMostrado()
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
            EncabezadoAuth(
                subtitulo = if (paso == PasoRegistro.CODIGO) "Registro de miembros" else "Crea tu cuenta"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 28.dp)
                    .navigationBarsPadding()
            ) {
                if (paso == PasoRegistro.CODIGO) {
                    Text(
                        text = "Escribe el código de miembro que te dieron en recepción. Con él confirmamos que eres parte del club.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    EtiquetaCampo("CÓDIGO DE MIEMBRO")
                    CampoTexto(
                        value = codigoTexto,
                        onValueChange = { codigoTexto = it.uppercase() },
                        placeholder = "CLB-7K3M9Q",
                        capitalizacion = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                        onAccion = { viewModel.verificarCodigo(codigoTexto) },
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Badge, contentDescription = null) },
                        isError = codigoError != null,
                        mensajeError = codigoError
                    )

                    if (yaRegistrado) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TarjetaClub(modifier = Modifier.fillMaxWidth(), fondo = MarcaSuave) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = Marca,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Este código ya tiene una cuenta creada",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextoPrincipal
                                    )
                                    Text(
                                        text = "Inicia sesión con el correo y la contraseña con los que te registraste. " +
                                            "Si no los recuerdas, usa \"¿Olvidaste tu contraseña?\" en la pantalla de inicio.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextoSecundario,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    if (yaRegistrado) {
                        BotonPrimario(texto = "Ir a iniciar sesión", onClick = onVolverALogin)
                    } else {
                        BotonPrimario(
                            texto = "Continuar",
                            onClick = { viewModel.verificarCodigo(codigoTexto) },
                            cargando = cargando
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Exito,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Código $codigo verificado",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextoPrincipal,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.cambiarCodigo() }) {
                            Text("Cambiar", color = Marca, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    EtiquetaCampo("NOMBRE COMPLETO")
                    CampoTexto(
                        value = nombre,
                        onValueChange = { nombre = it },
                        placeholder = "Tu nombre",
                        capitalizacion = KeyboardCapitalization.Words,
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Person, contentDescription = null) },
                        isError = nombreError != null,
                        mensajeError = nombreError
                    )

                    EspacioCampos()

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
                        placeholder = "Crea una contraseña segura",
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
                        isError = passwordError != null
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    RequisitosContrasena(contrasena = password)

                    EspacioCampos()

                    EtiquetaCampo("CONFIRMAR CONTRASEÑA")
                    CampoTexto(
                        value = confirmar,
                        onValueChange = { confirmar = it },
                        placeholder = "Repite la contraseña",
                        esContrasena = !passwordVisible,
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Lock, contentDescription = null) },
                        imeAction = ImeAction.Done,
                        onAccion = { viewModel.registrar(nombre, email, password, confirmar) },
                        isError = confirmarError != null,
                        mensajeError = confirmarError
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    BotonPrimario(
                        texto = "Crear cuenta",
                        onClick = { viewModel.registrar(nombre, email, password, confirmar) },
                        cargando = cargando
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "¿Ya tienes cuenta?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                    TextButton(onClick = onVolverALogin) {
                        Text(text = "Inicia sesión", color = Marca, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
