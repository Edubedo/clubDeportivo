package com.example.clubdeportivo.ui.membresia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.ClavesPrecio
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.esAdministrador
import com.example.clubdeportivo.data.model.esPersonal
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoFormulario
import com.example.clubdeportivo.ui.components.EncabezadoPantalla
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.theme.FondoApp
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.MarcaSuave
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario

internal fun dinero(monto: Double) = "$%,.0f".format(monto)

/**
 * Sección "Membresías". El personal del club la usa para controlar a los miembros (altas con código único,
 * renovaciones, suspensiones) y para editar los precios; un socio o visitante ve el catálogo y su propia membresía.
 */
@Composable
fun MembresiaScreen() {
    val esPersonal = SesionManager.usuarioActual?.rol?.esPersonal() == true
    if (esPersonal) MembresiasAdmin() else PlanesTab(editable = false, conTuMembresia = true)
}

@Composable
private fun MembresiasAdmin() {
    var pestana by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        PestanasPildora(
            opciones = listOf("Miembros", "Planes y precios"),
            seleccionada = pestana,
            onSeleccion = { pestana = it }
        )
        when (pestana) {
            0 -> MiembrosTab()
            // Los precios los cambia solo un administrador; el resto del personal los consulta.
            1 -> PlanesTab(editable = SesionManager.usuarioActual?.rol?.esAdministrador() == true, conTuMembresia = false)
        }
    }
}

/** Catálogo de planes con su precio vigente. Con [editable] el personal puede cambiar cada precio. */
@Composable
private fun PlanesTab(editable: Boolean, conTuMembresia: Boolean, viewModel: PreciosViewModel = viewModel()) {
    val editados by viewModel.editados.observeAsState(emptyMap())
    val mensaje by viewModel.mensaje.observeAsState()

    var precioEnEdicion by remember { mutableStateOf<Triple<String, String, Double>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMensajeMostrado()
        }
    }

    fun editar(clave: String, nombre: String): (() -> Unit)? =
        if (editable) ({ precioEnEdicion = Triple(clave, nombre, ClavesPrecio.de(clave, editados)) }) else null

    Scaffold(
        containerColor = FondoApp,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MargenPantalla)
        ) {
            EncabezadoPantalla(titulo = if (editable) "Planes y precios" else "Membresías")

            if (conTuMembresia) {
                TuMembresia()
            }

            Seccion("MEMBRESÍA INDIVIDUAL")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlanIndividual.entries.forEach { plan ->
                    val clave = ClavesPrecio.individual(plan)
                    val titulo = Catalogos.nombrePlanIndividual(plan)
                    TarjetaPlan(
                        titulo = titulo,
                        precio = dinero(ClavesPrecio.de(clave, editados)),
                        periodo = "al mes",
                        incluye = Catalogos.beneficiosPlanIndividual(plan),
                        destacado = plan == PlanIndividual.DELUXE,
                        onEditar = editar(clave, titulo)
                    )
                }
            }

            Seccion("PAQUETES FAMILIARES")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Catalogos.paquetesFamiliares.forEach { paquete ->
                    val clave = ClavesPrecio.familiar(paquete.id)
                    val titulo = "Paquete ${paquete.nombre}"
                    TarjetaPlan(
                        titulo = titulo,
                        precio = dinero(ClavesPrecio.de(clave, editados)),
                        periodo = "al mes",
                        incluye = listOf(paquete.descripcion, "Hasta ${paquete.maxIntegrantes} integrantes"),
                        onEditar = editar(clave, titulo)
                    )
                }
            }

            Seccion("VISITA SENCILLA")
            TarjetaPlan(
                titulo = "Visita",
                precio = dinero(ClavesPrecio.de(ClavesPrecio.VISITA, editados)),
                periodo = "por visita",
                incluye = listOf(
                    "Acceso por un solo día",
                    "Áreas que admiten visitantes, con aprobación del club",
                    "Horario de visitantes: ${Catalogos.HORA_INICIO_EXTERNOS} a ${Catalogos.HORA_FIN_EXTERNOS}"
                ),
                onEditar = editar(ClavesPrecio.VISITA, "Visita")
            )

            Seccion("CÓMO SE PAGA")
            TarjetaClub(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Incluye("Membresías individuales y paquetes familiares: pago mensual.")
                    Incluye("Visita: pago único de ${dinero(ClavesPrecio.de(ClavesPrecio.VISITA, editados))} por cada día de visita.")
                    Incluye("Métodos de pago: ${Catalogos.METODOS_DE_PAGO.joinToString(", ")}.")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    precioEnEdicion?.let { (clave, nombre, actual) ->
        EditarPrecioDialog(
            nombre = nombre,
            precioActual = actual,
            precioBase = ClavesPrecio.base(clave) ?: actual,
            onGuardar = { nuevo ->
                viewModel.guardar(clave, nuevo)
                precioEnEdicion = null
            },
            onCerrar = { precioEnEdicion = null }
        )
    }
}

@Composable
private fun EditarPrecioDialog(nombre: String, precioActual: Double, precioBase: Double, onGuardar: (Double) -> Unit, onCerrar: () -> Unit) {
    var texto by remember { mutableStateOf("%.0f".format(precioActual)) }
    val nuevo = texto.toDoubleOrNull()
    val valido = nuevo != null && nuevo > 0

    DialogoFormulario(titulo = "Editar precio", onCerrar = onCerrar) {
        Text(nombre, style = MaterialTheme.typography.titleMedium, color = TextoPrincipal)
        Text(
            "Precio base del catálogo: ${dinero(precioBase)}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        EtiquetaCampo("PRECIO (MXN)")
        CampoTexto(
            value = texto,
            onValueChange = { texto = it.filter(Char::isDigit).take(6) },
            tipoTeclado = KeyboardType.Number,
            isError = texto.isNotBlank() && !valido,
            mensajeError = if (texto.isNotBlank() && !valido) "Escribe un precio mayor a 0" else null
        )

        Text(
            "Se aplica a las nuevas altas y renovaciones. Quien ya tiene membresía conserva el precio que pagó hasta que renueve.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            modifier = Modifier.padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        BotonPrimario(
            texto = "Guardar precio",
            enabled = valido && nuevo != precioActual,
            onClick = { nuevo?.let(onGuardar) }
        )
    }
}

@Composable
private fun Seccion(texto: String) {
    Text(
        text = texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextoSecundario,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(top = 24.dp, bottom = 10.dp)
    )
}

@Composable
private fun Incluye(texto: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Marca,
            modifier = Modifier
                .padding(end = 8.dp, top = 2.dp)
                .size(18.dp)
        )
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = TextoPrincipal)
    }
}

@Composable
private fun TarjetaPlan(
    titulo: String,
    precio: String,
    periodo: String,
    incluye: List<String>,
    destacado: Boolean = false,
    onEditar: (() -> Unit)? = null
) {
    TarjetaClub(modifier = Modifier.fillMaxWidth(), destacada = destacado) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoPrincipal,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(precio, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Marca, maxLines = 1)
                    Text(periodo, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                }
                if (onEditar != null) {
                    IconButton(onClick = onEditar) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar precio de $titulo", tint = TextoSecundario)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                incluye.forEach { Incluye(it) }
            }
        }
    }
}

/** La membresía propia de un socio o visitante. */
@Composable
private fun TuMembresia(viewModel: MembresiaViewModel = viewModel()) {
    val titulo by viewModel.titulo.observeAsState("")
    val mensaje by viewModel.mensaje.observeAsState()
    val detalle by viewModel.detalle.observeAsState()
    val integrantes by viewModel.integrantes.observeAsState(emptyList())

    TarjetaClub(modifier = Modifier.fillMaxWidth(), fondo = MarcaSuave) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "TU MEMBRESÍA",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Marca,
                letterSpacing = 0.4.sp
            )
            Text(
                titulo,
                style = MaterialTheme.typography.titleLarge,
                color = TextoPrincipal,
                modifier = Modifier.padding(top = 4.dp)
            )
            mensaje?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextoPrincipal,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            detalle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            integrantes.forEach { integrante ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = Marca,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp)
                    )
                    Text(
                        "${integrante.nombre} (${integrante.parentesco})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoPrincipal
                    )
                }
            }
        }
    }
}
