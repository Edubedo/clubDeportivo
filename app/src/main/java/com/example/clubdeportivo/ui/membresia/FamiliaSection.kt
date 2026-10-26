package com.example.clubdeportivo.ui.membresia

import com.example.clubdeportivo.data.model.etiquetaParentesco
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
import com.example.clubdeportivo.util.Textos

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
                    Text(Textos.get("your_family"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextoSecundario, letterSpacing = 0.4.sp)
                    // Un integrante solo ve su propio lugar: el conteo del paquete es del titular.
                    Insignia(if (datos.esTitular) Textos.get("of_members", datos.miembros.size, datos.maxPersonas) else "Integrante", TipoInsignia.NEUTRO)
                }
                if (datos.esTitular) {
                    Text(
                        Textos.get("you_are_the_package_holder_add"),
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
                            texto = Textos.get("manual_agregar_familiar_lugares", datos.lugaresLibres, if (datos.lugaresLibres == 1) Textos.get("free_spot") else Textos.get("free_spots")),
                            onClick = { viewModel.limpiarError(); agregando = true }
                        )
                    } else {
                        Text(
                            if (datos.lugaresLibres == 0) Textos.get("your_package_already_has_all_its")
                            else Textos.get("your_membership_is_not_active_renew"),
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
            Text(etiquetaParentesco(persona.parentesco), style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        }
        if (puedeCompartir) {
            Insignia(Textos.get("account_not_created_yet"), TipoInsignia.ALERTA)
            TextButton(onClick = onCompartir, modifier = modifierReflejo(Modifier, enabled = true)) {
                Text(Textos.get("link"), color = Marca, fontWeight = FontWeight.Bold)
            }
        } else {
            Insignia(if (persona.cuentaCreada) Textos.get("with_account") else "Titular", if (persona.cuentaCreada) TipoInsignia.EXITO else TipoInsignia.NEUTRO)
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
        titulo = Textos.get("add_family_member"),
        onCerrar = onCerrar,
        pie = {
            if (error != null) {
                Text(error, color = Peligro, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 12.dp))
            }
            BotonPrimario(
                texto = Textos.get("add_and_generate_link"),
                cargando = guardando,
                enabled = nombre.isNotBlank() && parentesco.isNotBlank(),
                onClick = { onGuardar(nombre, parentesco, telefono, correo) }
            )
        }
    ) {
        Text(
            Textos.get("manual_en_tu_paquete", if (lugaresLibres == 1) Textos.get("n_1_spot_left") else Textos.get("spots_left", lugaresLibres)) +
                Textos.get("once_added_you_will_get_a"),
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario
        )
        EspacioCampos()
        EtiquetaCampo(Textos.get("full_name"))
        CampoTexto(value = nombre, onValueChange = { nombre = it }, placeholder = Textos.get("full_name_2"), capitalizacion = KeyboardCapitalization.Words)
        EspacioCampos()
        EtiquetaCampo(Textos.get("lbl_parentesco"))
        CampoTexto(
            value = parentesco,
            onValueChange = { parentesco = it },
            placeholder = Textos.get("spouse_son_daughter"),
            capitalizacion = KeyboardCapitalization.Sentences
        )
        EspacioCampos()
        EtiquetaCampo(Textos.get("phone_optional"))
        CampoTexto(
            value = telefono,
            onValueChange = { telefono = it.filter { c -> c.isDigit() || c == ' ' }.take(15) },
            placeholder = Textos.get("n_10_digits"),
            tipoTeclado = KeyboardType.Phone
        )
        EspacioCampos()
        EtiquetaCampo(Textos.get("email_optional"))
        CampoTexto(
            value = correo,
            onValueChange = { correo = it },
            placeholder = Textos.get("email_example_com"),
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
        titulo = Textos.get("access_link"),
        onCerrar = onCerrar,
        pie = {
            BotonPrimario(
                texto = Textos.get("share_link"),
                onClick = {
                    val envio = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, Textos.get("your_access_to_the_family_plan"))
                        putExtra(Intent.EXTRA_TEXT, mensaje)
                    }
                    contexto.startActivity(Intent.createChooser(envio, Textos.get("send_link_to", persona.nombre)))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            BotonSecundario(
                texto = Textos.get("copy_link"),
                onClick = {
                    portapapeles.setText(AnnotatedString(mensaje))
                    Toast.makeText(contexto, Textos.get("link_copied"), Toast.LENGTH_SHORT).show()
                }
            )
        }
    ) {
        Text(
            Textos.get("has_been_added_send_them_this", persona.nombre, etiquetaParentesco(persona.parentesco)) +
                Textos.get("the_app_only_asks_them_to"),
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario
        )
        Spacer(modifier = Modifier.height(16.dp))
        TarjetaClub(modifier = Modifier.fillMaxWidth(), fondo = MarcaSuave) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(Textos.get("code"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Marca, letterSpacing = 0.4.sp)
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
            Textos.get("if_the_link_doesn_t_open"),
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
