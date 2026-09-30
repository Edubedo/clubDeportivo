package com.example.clubdeportivo.ui.membresia

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.ClavesPrecio
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.esPersonal
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoFormulario
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.VerdeMarca

internal val FondoPantalla = Color(0xFFF8FAFD)
internal val TextoTitulo = Color(0xFF111827)
internal val TextoSuave = Color(0xFF64748B)

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

    Column(modifier = Modifier.fillMaxSize().background(FondoPantalla)) {
        PestanasPildora(
            opciones = listOf("Miembros", "Planes y precios"),
            seleccionada = pestana,
            onSeleccion = { pestana = it }
        )
        when (pestana) {
            0 -> MiembrosTab()
            1 -> PlanesTab(editable = true, conTuMembresia = false)
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
        containerColor = FondoPantalla,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                if (editable) "Planes y precios" else "Membresías",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextoTitulo
            )

            if (conTuMembresia) {
                Spacer(modifier = Modifier.height(16.dp))
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
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
        Text(nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextoTitulo)
        Text("Precio base del catálogo: ${dinero(precioBase)}", fontSize = 13.sp, color = TextoSuave, modifier = Modifier.padding(top = 2.dp))

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
            fontSize = 13.sp,
            color = TextoSuave,
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
        color = TextoSuave,
        modifier = Modifier.padding(top = 24.dp, bottom = 10.dp)
    )
}

@Composable
private fun Incluye(texto: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = VerdeMarca,
            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
        )
        Text(texto, fontSize = 14.sp, color = TextoTitulo)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(if (destacado) 2.dp else 1.dp, if (destacado) VerdeMarca else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoTitulo,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(precio, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VerdeMarca, maxLines = 1)
                    Text(periodo, fontSize = 12.sp, color = TextoSuave)
                }
                if (onEditar != null) {
                    IconButton(onClick = onEditar) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar precio de $titulo", tint = TextoSuave)
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("TU MEMBRESÍA", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextoSuave)
            Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextoTitulo, modifier = Modifier.padding(top = 4.dp))
            mensaje?.let { Text(it, fontWeight = FontWeight.SemiBold, color = TextoTitulo, modifier = Modifier.padding(top = 4.dp)) }
            detalle?.let { Text(it, fontSize = 13.sp, color = TextoSuave, modifier = Modifier.padding(top = 2.dp)) }
            integrantes.forEach { integrante ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = TextoSuave, modifier = Modifier.padding(end = 8.dp))
                    Text("${integrante.nombre} (${integrante.parentesco})", fontSize = 14.sp)
                }
            }
        }
    }
}
