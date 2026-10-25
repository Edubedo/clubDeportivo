package com.example.clubdeportivo.ui.membresia

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Familia
import com.example.clubdeportivo.data.model.MiembroClub
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.BotonSecundario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoFormulario
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.Insignia
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.components.TipoInsignia
import com.example.clubdeportivo.ui.components.modifierReflejo
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.MarcaSuave
import com.example.clubdeportivo.ui.theme.Peligro
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.util.ReglasMembresia

/**
 * "Tu familia": las personas del paquete familiar. El titular las ve a todas y da de alta a las que faltan (cada una
 * recibe su código y un enlace de acceso para crear su cuenta); un integrante solo ve su propio lugar.
 */
@Composable
internal fun TarjetaFamilia(viewModel: FamiliaViewModel) {
    val familia by viewModel.familia.observeAsState()
    val guardando by viewModel.guardando.observeAsState(false)
    val error by viewModel.errorFormulario.observeAsState()
    val invitacion by viewModel.invitacion.observeAsState()
    var agregando by remember { mutableStateOf(false) }

    val datos = familia ?: return

    Column(modifier = Modifier.padding(top = 16.dp)) {
        TarjetaClub(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("TU FAMILIA", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextoSecundario, letterSpacing = 0.4.sp)
                    // Un integrante solo ve su propio lugar: el conteo del paquete es del titular.
                    Insignia(if (datos.esTitular) "${datos.miembros.size} de ${datos.maxPersonas} integrantes" else "Integrante", TipoInsignia.NEUTRO)
                }
                if (datos.esTitular) {
                    Text(
                        "Eres el titular del paquete: da de alta a tu familia y compárteles su enlace de acceso para que creen su cuenta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                datos.miembros.forEach { persona ->
                    FilaIntegrante(persona = persona, puedeCompartir = datos.esTitular && !persona.esTitular && !persona.cuentaCreada) {
                        viewModel.mostrarInvitacion(persona)
                    }
                }
                if (datos.esTitular) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (viewModel.puedeAgregar(datos)) {
                        BotonPrimario(
                            texto = "Agregar familiar (${datos.lugaresLibres} ${if (datos.lugaresLibres == 1) "lugar libre" else "lugares libres"})",
                            onClick = { viewModel.limpiarError(); agregando = true }
                        )
                    } else {
                        Text(
                            if (datos.lugaresLibres == 0) "Tu paquete ya tiene a todos sus integrantes."
                            else "Tu membresía no está activa: renuévala en recepción para agregar familiares.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextoSecundario
                        )
                    }
                }
            }
        }
    }

    if (agregando) {
        DialogoAgregarFamiliar(
            lugaresLibres = datos.lugaresLibres,
            error = error,
            guardando = guardando,
            onGuardar = { nombre, parentesco, telefono, correo -> viewModel.agregar(nombre, parentesco, telefono, correo) },
            onCerrar = { agregando = false }
        )
    }
    // Al terminar el alta se cierra el formulario y se abre el enlace de acceso del familiar.
    invitacion?.let { nuevo ->
        agregando = false
        DialogoInvitacion(
            persona = nuevo,
            nombreTitular = SesionManager.usuarioActual?.nombre.orEmpty(),
            onCerrar = { viewModel.cerrarInvitacion() }
        )
    }
}

@Composable
private fun FilaIntegrante(persona: MiembroClub, puedeCompartir: Boolean, onCompartir: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(persona.nombre, style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
            Text(persona.parentesco, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        }
        if (puedeCompartir) {
            Insignia("Falta crear cuenta", TipoInsignia.ALERTA)
            TextButton(onClick = onCompartir, modifier = modifierReflejo(Modifier, enabled = true)) {
                Text("Enlace", color = Marca, fontWeight = FontWeight.Bold)
            }
        } else {
            Insignia(if (persona.cuentaCreada) "Con cuenta" else "Titular", if (persona.cuentaCreada) TipoInsignia.EXITO else TipoInsignia.NEUTRO)
        }
    }
}

@Composable
private fun DialogoAgregarFamiliar(
    lugaresLibres: Int,
    error: String?,
    guardando: Boolean,
    onGuardar: (nombre: String, parentesco: String, telefono: String, correo: String) -> Unit,
    onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var parentesco by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }

    DialogoFormulario(
        titulo = "Agregar familiar",
        onCerrar = onCerrar,
        pie = {
            if (error != null) {
                Text(error, color = Peligro, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 12.dp))
            }
            BotonPrimario(
                texto = "Dar de alta y generar enlace",
                cargando = guardando,
                enabled = nombre.isNotBlank() && parentesco.isNotBlank(),
                onClick = { onGuardar(nombre, parentesco, telefono, correo) }
            )
        }
    ) {
        Text(
            "${if (lugaresLibres == 1) "Queda 1 lugar" else "Quedan $lugaresLibres lugares"} en tu paquete. " +
                "Al darlo de alta recibirás un enlace para que cree su cuenta en la app.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario
        )
        EspacioCampos()
        EtiquetaCampo("NOMBRE COMPLETO")
        CampoTexto(value = nombre, onValueChange = { nombre = it }, placeholder = "Nombre completo", capitalizacion = KeyboardCapitalization.Words)
        EspacioCampos()
        EtiquetaCampo("PARENTESCO")
        CampoTexto(
            value = parentesco,
            onValueChange = { parentesco = it },
            placeholder = "Cónyuge, Hijo, Hija...",
            capitalizacion = KeyboardCapitalization.Sentences
        )
        EspacioCampos()
        EtiquetaCampo("TELÉFONO (OPCIONAL)")
        CampoTexto(
            value = telefono,
            onValueChange = { telefono = it.filter { c -> c.isDigit() || c == ' ' }.take(15) },
            placeholder = "10 dígitos",
            tipoTeclado = KeyboardType.Phone
        )
        EspacioCampos()
        EtiquetaCampo("CORREO (OPCIONAL)")
        CampoTexto(
            value = correo,
            onValueChange = { correo = it },
            placeholder = "correo@ejemplo.com",
            tipoTeclado = KeyboardType.Email,
            imeAction = ImeAction.Done
        )
    }
}

/** El código del familiar y su enlace de acceso, con un botón para mandárselo (WhatsApp, correo, SMS...) o copiarlo. */
@Composable
private fun DialogoInvitacion(persona: MiembroClub, nombreTitular: String, onCerrar: () -> Unit) {
    val contexto = LocalContext.current
    val portapapeles = LocalClipboardManager.current
    val enlace = ReglasMembresia.enlaceDeAcceso(persona.codigo)
    val mensaje = ReglasMembresia.mensajeDeInvitacion(persona.nombre, nombreTitular, persona.codigo)

    DialogoFormulario(
        titulo = "Enlace de acceso",
        onCerrar = onCerrar,
        pie = {
            BotonPrimario(
                texto = "Compartir enlace",
                onClick = {
                    val envio = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Tu acceso al plan familiar")
                        putExtra(Intent.EXTRA_TEXT, mensaje)
                    }
                    contexto.startActivity(Intent.createChooser(envio, "Enviar enlace a ${persona.nombre}"))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            BotonSecundario(
                texto = "Copiar enlace",
                onClick = {
                    portapapeles.setText(AnnotatedString(mensaje))
                    Toast.makeText(contexto, "Enlace copiado", Toast.LENGTH_SHORT).show()
                }
            )
        }
    ) {
        Text(
            "${persona.nombre} (${persona.parentesco}) ya está dado de alta. Mándale este enlace: al abrirlo en su teléfono " +
                "la app le pide solo crear su correo y contraseña.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario
        )
        Spacer(modifier = Modifier.height(16.dp))
        TarjetaClub(modifier = Modifier.fillMaxWidth(), fondo = MarcaSuave) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("CÓDIGO", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Marca, letterSpacing = 0.4.sp)
                Text(persona.codigo, fontSize = 24.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
                Text(
                    enlace,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        Text(
            "Si el enlace no abre la app, en Regístrate puede escribir el código a mano.",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
