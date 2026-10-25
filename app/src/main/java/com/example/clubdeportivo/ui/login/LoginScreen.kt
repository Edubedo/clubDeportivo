package com.example.clubdeportivo.ui.login

import com.example.clubdeportivo.ui.components.CargandoMarca
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.R
import com.example.clubdeportivo.ui.components.BanderasIdioma
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.modifierReflejo
import com.example.clubdeportivo.ui.theme.LoginAccentShape
import com.example.clubdeportivo.ui.theme.LoginBackground
import com.example.clubdeportivo.ui.theme.LoginMutedText
import com.example.clubdeportivo.ui.theme.DeepNavy
import com.example.clubdeportivo.ui.theme.SoftBlueSelection

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
        containerColor = LoginBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = LoginAccentShape.copy(alpha = 0.75f),
                    radius = size.width * 0.34f,
                    center = Offset(size.width * 1.02f, size.height * 0.14f)
                )
                drawCircle(
                    color = LoginAccentShape.copy(alpha = 0.58f),
                    radius = size.width * 0.36f,
                    center = Offset(-size.width * 0.06f, size.height * 0.94f)
                )
            }
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftBlueSelection.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SportsSoccer,
                            contentDescription = null,
                            tint = SoftBlueSelection,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Text(
                        text = "Athletic\nClub",
                        style = MaterialTheme.typography.labelMedium.copy(lineHeight = 13.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    BanderasIdioma()
                }

                Spacer(modifier = Modifier.height(44.dp))
                Text(
                    text = stringResource(R.string.login_area_privada),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                    color = SoftBlueSelection,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.login_slogan),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 32.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Text(
                    text = stringResource(R.string.login_subtitulo),
                    color = LoginMutedText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 10.dp)
                )

                if (restaurando) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CargandoMarca(tamano = 72.dp, sobreOscuro = true)
                    }
                } else {
                    Column(modifier = Modifier.padding(top = 28.dp)) {
                        EtiquetaCampo(stringResource(R.string.login_correo_etiqueta), color = SoftBlueSelection)
                        CampoTexto(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = stringResource(R.string.login_correo_placeholder),
                            tipoTeclado = KeyboardType.Email,
                            leadingIcon = {
                                Icon(imageVector = Icons.Outlined.Email, contentDescription = null)
                            },
                            isError = emailError != null,
                            mensajeError = emailError,
                            modoOscuro = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        EtiquetaCampo(stringResource(R.string.login_password_etiqueta), color = SoftBlueSelection)
                        CampoTexto(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = stringResource(R.string.login_password_placeholder),
                            esContrasena = !passwordVisible,
                            leadingIcon = {
                                Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.Visibility
                                        else Icons.Outlined.VisibilityOff,
                                        contentDescription = if (passwordVisible) stringResource(R.string.ocultar_contrasena)
                                        else stringResource(R.string.mostrar_contrasena)
                                    )
                                }
                            },
                            imeAction = ImeAction.Done,
                            onAccion = { viewModel.login(email, password) },
                            isError = passwordError != null,
                            mensajeError = passwordError,
                            modoOscuro = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { viewModel.olvideContrasena(email) }) {
                                Text(
                                    text = stringResource(R.string.login_olvide_contrasena),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoginMutedText,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.login(email, password) },
                            enabled = !cargando,
                            modifier = modifierReflejo(
                                Modifier.fillMaxWidth().height(52.dp),
                                enabled = !cargando,
                                shape = RoundedCornerShape(12.dp)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = DeepNavy,
                                disabledContainerColor = Color.White.copy(alpha = 0.8f),
                                disabledContentColor = DeepNavy
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (cargando) {
                                    CircularProgressIndicator(
                                        color = DeepNavy,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp).align(Alignment.Center)
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.login_title),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    )
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.login_sin_cuenta),
                            style = MaterialTheme.typography.bodySmall,
                            color = LoginMutedText,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        TextButton(
                            onClick = onIrRegistro,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = stringResource(R.string.login_registrate_codigo),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
//p