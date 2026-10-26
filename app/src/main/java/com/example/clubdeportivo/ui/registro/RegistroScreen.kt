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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
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
import com.example.clubdeportivo.ui.components.modifierReflejo
import com.example.clubdeportivo.ui.theme.BordeCampo
import com.example.clubdeportivo.ui.theme.Exito
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.MarcaSuave
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.Textos

@Composable
fun RegistroScreen(
    codigoInicial: String? = null,
    onCodigoUsado: () -> Unit = {},
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
    var aceptoTerminos by rememberSaveable { mutableStateOf(false) }
    var leyendo by remember { mutableStateOf<DocumentoLegal?>(null) }

    val paso by viewModel.paso.observeAsState(PasoRegistro.CODIGO)
    val codigo by viewModel.codigo.observeAsState("")
    val yaRegistrado by viewModel.yaRegistrado.observeAsState(false)
    val codigoError by viewModel.codigoError.observeAsState()
    val nombreError by viewModel.nombreError.observeAsState()
    val emailError by viewModel.emailError.observeAsState()
    val passwordError by viewModel.passwordError.observeAsState()
    val confirmarError by viewModel.confirmarError.observeAsState()
    val terminosError by viewModel.terminosError.observeAsState()
    val cargando by viewModel.cargando.observeAsState(false)
    val errorGeneral by viewModel.errorGeneral.observeAsState()
    val registroExitoso by viewModel.registroExitoso.observeAsState(false)

    val snackbarHostState = remember { SnackbarHostState() }

    // Llegó por un enlace de acceso: el código ya viene escrito y se verifica solo.
    LaunchedEffect(codigoInicial) {
        if (codigoInicial != null) {
            codigoTexto = codigoInicial
            viewModel.verificarCodigo(codigoInicial)
            onCodigoUsado()
        }
    }

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
                subtitulo = if (paso == PasoRegistro.CODIGO) Textos.get("member_sign_up") else Textos.get("create_your_account")
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 28.dp)
                    .navigationBarsPadding()
            ) {
                if (paso == PasoRegistro.CODIGO) {
                    Text(
                        text = Textos.get("enter_the_member_code_you_were"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    EtiquetaCampo(Textos.get("member_code"))
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
                                        text = Textos.get("this_code_already_has_an_account"),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextoPrincipal
                                    )
                                    Text(
                                        text = Textos.get("sign_in_with_the_email_and") +
                                            Textos.get("if_you_don_t_remember_them"),
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
                        BotonPrimario(texto = Textos.get("go_to_sign_in"), onClick = onVolverALogin)
                    } else {
                        BotonPrimario(
                            texto = Textos.get("k_continue"),
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
                            text = Textos.get("code_verified", codigo),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextoPrincipal,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { viewModel.cambiarCodigo() },
                            modifier = modifierReflejo(Modifier, enabled = true)
                        ) {
                            Text(Textos.get("change"), color = Marca, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    EtiquetaCampo(Textos.get("full_name"))
                    CampoTexto(
                        value = nombre,
                        onValueChange = { nombre = it },
                        placeholder = Textos.get("your_name"),
                        capitalizacion = KeyboardCapitalization.Words,
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Person, contentDescription = null) },
                        isError = nombreError != null,
                        mensajeError = nombreError
                    )

                    EspacioCampos()

                    EtiquetaCampo(Textos.get("email"))
                    CampoTexto(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = Textos.get("you_email_com"),
                        tipoTeclado = KeyboardType.Email,
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Email, contentDescription = null) },
                        isError = emailError != null,
                        mensajeError = emailError
                    )

                    EspacioCampos()

                    EtiquetaCampo(Textos.get("password"))
                    CampoTexto(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = Textos.get("create_a_strong_password"),
                        esContrasena = !passwordVisible,
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible }
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = if (passwordVisible) Textos.get("hide_password") else Textos.get("show_password")
                                )
                            }
                        },
                        isError = passwordError != null
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    RequisitosContrasena(contrasena = password)

                    EspacioCampos()

                    EtiquetaCampo(Textos.get("confirm_password"))
                    CampoTexto(
                        value = confirmar,
                        onValueChange = { confirmar = it },
                        placeholder = Textos.get("repeat_the_password"),
                        esContrasena = !passwordVisible,
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Lock, contentDescription = null) },
                        imeAction = ImeAction.Done,
                        onAccion = { viewModel.registrar(nombre, email, password, confirmar, aceptoTerminos) },
                        isError = confirmarError != null,
                        mensajeError = confirmarError
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    AceptacionDeTerminos(
                        aceptado = aceptoTerminos,
                        error = terminosError,
                        onCambio = {
                            aceptoTerminos = it
                            viewModel.limpiarErrorTerminos()
                        },
                        onLeer = { leyendo = it }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    BotonPrimario(
                        texto = Textos.get("create_account"),
                        onClick = { viewModel.registrar(nombre, email, password, confirmar, aceptoTerminos) },
                        enabled = aceptoTerminos,
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
                        text = Textos.get("already_have_an_account"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                    TextButton(
                        onClick = onVolverALogin,
                        modifier = modifierReflejo(Modifier, enabled = true)
                    ) {
                        Text(text = Textos.get("sign_in"), color = Marca, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    leyendo?.let { documento ->
        TerminosScreen(
            documentoInicial = documento,
            onCerrar = { leyendo = null },
            onAceptar = {
                aceptoTerminos = true
                viewModel.limpiarErrorTerminos()
                leyendo = null
            }
        )
    }
}

/**
 * Casilla de aceptación con enlaces a los términos y al aviso de privacidad (se abren a pantalla completa).
 * Hasta marcarla no se puede crear la cuenta.
 */
@Composable
private fun AceptacionDeTerminos(
    aceptado: Boolean,
    error: String?,
    onCambio: (Boolean) -> Unit,
    onLeer: (DocumentoLegal) -> Unit
) {
    val estiloEnlace = TextLinkStyles(
        style = SpanStyle(color = Marca, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
    )
    val texto = buildAnnotatedString {
        append(Textos.get("i_have_read_and_accept_the"))
        withLink(LinkAnnotation.Clickable("terminos", styles = estiloEnlace) { onLeer(DocumentoLegal.TERMINOS) }) {
            append(Textos.get("terms_and_conditions"))
        }
        append(Textos.get("and_the"))
        withLink(LinkAnnotation.Clickable("privacidad", styles = estiloEnlace) { onLeer(DocumentoLegal.PRIVACIDAD) }) {
            append(Textos.get("privacy_notice"))
        }
        append(Textos.get("of_the_club"))
    }

    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Checkbox(
            checked = aceptado,
            onCheckedChange = onCambio,
            colors = CheckboxDefaults.colors(
                checkedColor = Marca,
                checkmarkColor = SobreMarca,
                uncheckedColor = BordeCampo
            )
        )
        Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(text = texto, style = MaterialTheme.typography.bodyMedium, color = TextoPrincipal)
            Text(
                text = error ?: if (aceptado) Textos.get("you_accepted_version", TerminosTexto.VERSION) else Textos.get("you_must_accept_them_to_create"),
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) Peligro else TextoSecundario,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
