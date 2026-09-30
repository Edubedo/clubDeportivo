package com.example.clubdeportivo.ui.personal

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.DialogoFormulario
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import com.example.clubdeportivo.ui.components.PestanasPildora
import com.example.clubdeportivo.ui.components.VerdeMarca
import com.example.clubdeportivo.util.FotoPerfilManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// =====================================================================
// 1. MODELOS REALES
// =====================================================================

enum class DisponibilidadArea { DISPONIBLE, OCUPADA, MANTENIMIENTO }

data class Usuario(
    var id: String = "",
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
    val estado: String = "ACTIVO",
    val fotoUrl: String? = null
)

data class Empleado(
    var id: String = "",
    val usuarioId: String = "",
    val puesto: String = "",
    val turno: String = "",
    val areaAsignadaId: String? = null
)

data class Area(
    var id: String = "",
    val nombre: String = "",
    val tipo: String = "",
    val capacidad: Int = 0,
    val disponibilidad: DisponibilidadArea = DisponibilidadArea.DISPONIBLE,
    val permiteExternos: Boolean = false
)

data class EmpleadoUI(
    val empleadoId: String,
    val usuarioId: String,
    val nombre: String,
    val correo: String,
    val telefono: String,
    val estado: String,
    val puesto: String,
    val turno: String,
    val areaAsignadaId: String?,
    val areaNombre: String,
    val fotoUrl: String?
)

// =====================================================================
// 2. REPOSITORIO
// =====================================================================

class PersonalRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun obtenerAreas(): List<Area> {
        return db.collection("areas").get().await().documents.mapNotNull {
            it.toObject(Area::class.java)?.apply { id = it.id }
        }
    }

    suspend fun obtenerEmpleadosConUsuarios(areas: List<Area>): List<EmpleadoUI> {
        val empleadosDb = db.collection("empleados").get().await().documents.mapNotNull {
            it.toObject(Empleado::class.java)?.apply { id = it.id }
        }
        val usuariosDb = db.collection("usuarios").get().await().documents.mapNotNull {
            it.toObject(Usuario::class.java)?.apply { id = it.id }
        }

        return empleadosDb.mapNotNull { emp ->
            val usuario = usuariosDb.find { it.id == emp.usuarioId } ?: return@mapNotNull null
            val area = areas.find { it.id == emp.areaAsignadaId }

            EmpleadoUI(
                empleadoId = emp.id,
                usuarioId = usuario.id,
                nombre = usuario.nombre,
                correo = usuario.correo,
                telefono = usuario.telefono,
                estado = usuario.estado,
                puesto = emp.puesto,
                turno = emp.turno,
                areaAsignadaId = emp.areaAsignadaId,
                areaNombre = area?.nombre ?: "Sin área asignada",
                fotoUrl = usuario.fotoUrl
            )
        }
    }

    suspend fun guardarPersonal(
        empleadoId: String?, usuarioId: String?,
        nombre: String, correo: String, telefono: String, estado: String,
        puesto: String, turno: String, areaId: String?, contrasena: String, fotoUrl: String?
    ) {
        val finalUserId = if (usuarioId.isNullOrEmpty()) db.collection("usuarios").document().id else usuarioId
        val userRef = db.collection("usuarios").document(finalUserId)

        val usuario = Usuario(
            id = finalUserId,
            nombre = nombre,
            correo = correo,
            telefono = telefono,
            estado = estado,
            fotoUrl = fotoUrl
        )
        userRef.set(usuario).await()

        val finalEmpId = if (empleadoId.isNullOrEmpty()) db.collection("empleados").document().id else empleadoId
        val empRef = db.collection("empleados").document(finalEmpId)

        val empleado = Empleado(
            id = finalEmpId,
            usuarioId = finalUserId,
            puesto = puesto,
            turno = turno,
            areaAsignadaId = areaId
        )
        empRef.set(empleado).await()
    }
}

// =====================================================================
// 3. VIEWMODEL
// =====================================================================

class PersonalViewModel : ViewModel() {
    private val repository = PersonalRepository()

    private val _empleadosUI = MutableLiveData<List<EmpleadoUI>>()
    val empleadosUI: LiveData<List<EmpleadoUI>> = _empleadosUI

    private val _areasDisponibles = MutableLiveData<List<Area>>()
    val areasDisponibles: LiveData<List<Area>> = _areasDisponibles

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            try {
                val areas = repository.obtenerAreas()
                _areasDisponibles.value = areas
                _empleadosUI.value = repository.obtenerEmpleadosConUsuarios(areas)
            } catch (_: Exception) { }
        }
    }

    fun guardarPersonal(
        empleadoId: String?, usuarioId: String?,
        nombre: String, correo: String, telefono: String, estado: String,
        puesto: String, turno: String, areaId: String?, contrasena: String, fotoUrl: String?
    ) {
        viewModelScope.launch {
            repository.guardarPersonal(empleadoId, usuarioId, nombre, correo, telefono, estado, puesto, turno, areaId, contrasena, fotoUrl)
            cargarDatos()
        }
    }
}

// =====================================================================
// 4. VISTA PRINCIPAL (UI)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalScreen(viewModel: PersonalViewModel = viewModel()) {
    val empleados by viewModel.empleadosUI.observeAsState(emptyList())
    val areas by viewModel.areasDisponibles.observeAsState(emptyList())

    var mostrarFormulario by remember { mutableStateOf(false) }
    var empleadoEnEdicion by remember { mutableStateOf<EmpleadoUI?>(null) }

    Scaffold(
        containerColor = Color(0xFFF9F9F9),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    empleadoEnEdicion = null
                    mostrarFormulario = true
                },
                containerColor = VerdeMarca,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Personal")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar Personal", fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Personal", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B), modifier = Modifier.padding(bottom = 16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(empleados, key = { it.empleadoId }) { empleado ->
                    EmpleadoCard(
                        empleado = empleado,
                        onEditClick = {
                            empleadoEnEdicion = empleado
                            mostrarFormulario = true
                        }
                    )
                }
            }
        }
    }

    if (mostrarFormulario) {
        FormularioPersonalDialog(
            areas = areas,
            empleadoAEditar = empleadoEnEdicion,
            onGuardar = { nombre, correo, telefono, estado, puesto, turno, areaId, contrasena, fotoUrl ->
                viewModel.guardarPersonal(
                    empleadoId = empleadoEnEdicion?.empleadoId,
                    usuarioId = empleadoEnEdicion?.usuarioId,
                    nombre = nombre,
                    correo = correo,
                    telefono = telefono,
                    estado = estado,
                    puesto = puesto,
                    turno = turno,
                    areaId = areaId,
                    contrasena = contrasena,
                    fotoUrl = fotoUrl
                )
                mostrarFormulario = false
            },
            onCerrar = { mostrarFormulario = false }
        )
    }
}

// =====================================================================
// 5. FORMULARIO Y COMPONENTES
// =====================================================================

/**
 * Formulario de alta/edición de personal. Misma estructura que el de áreas: diálogo con título y X
 * fija, selectores en píldora, etiquetas en mayúsculas, campos estándar y un solo botón principal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioPersonalDialog(
    areas: List<Area>,
    empleadoAEditar: EmpleadoUI?,
    onGuardar: (String, String, String, String, String, String, String?, String, String?) -> Unit,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val esEdicion = empleadoAEditar != null

    var nombre by remember { mutableStateOf(empleadoAEditar?.nombre ?: "") }
    var telefono by remember { mutableStateOf(empleadoAEditar?.telefono ?: "") }
    var correo by remember { mutableStateOf(empleadoAEditar?.correo ?: "") }
    var estadoSeleccionado by remember { mutableStateOf(empleadoAEditar?.estado ?: "ACTIVO") }
    var contrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val tempUserId = remember { empleadoAEditar?.usuarioId ?: java.util.UUID.randomUUID().toString() }

    var fotoPerfilPath by remember {
        mutableStateOf<String?>(
            if (empleadoAEditar != null) {
                FotoPerfilManager.obtenerFoto(context, empleadoAEditar.usuarioId)?.absolutePath ?: empleadoAEditar.fotoUrl
            } else {
                null
            }
        )
    }

    val selectorFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val archivoFoto = FotoPerfilManager.guardarFoto(context = context, uri = uri, usuarioId = tempUserId)
            fotoPerfilPath = archivoFoto?.absolutePath
        }
    }

    val tiposPersonal = listOf("Instructor", "Limpieza")
    val turnos = listOf("Matutino", "Vespertino")
    val estados = listOf("ACTIVO", "INACTIVO")

    var areaExpandida by remember { mutableStateOf(false) }
    var areaSeleccionada by remember {
        mutableStateOf(areas.find { it.id == empleadoAEditar?.areaAsignadaId } ?: areas.firstOrNull())
    }
    var tipoSeleccionado by remember {
        mutableStateOf(tiposPersonal.find { empleadoAEditar?.puesto?.startsWith(it) == true } ?: tiposPersonal[0])
    }
    var turnoSeleccionado by remember { mutableStateOf(empleadoAEditar?.turno?.ifEmpty { turnos[0] } ?: turnos[0]) }

    val puestoGenerado = "$tipoSeleccionado de ${areaSeleccionada?.nombre ?: "Sin Área"}"
    val puedeGuardar = nombre.isNotBlank() && correo.isNotBlank()

    DialogoFormulario(
        titulo = if (esEdicion) "Editar personal" else "Agregar personal",
        onCerrar = onCerrar
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
                    .clickable { selectorFoto.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (fotoPerfilPath != null) {
                    AsyncImage(
                        model = fotoPerfilPath,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = "Foto", tint = Color(0xFF94A3B8))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(onClick = { selectorFoto.launch("image/*") }) {
                Text(
                    text = if (fotoPerfilPath == null) "Subir foto de perfil" else "Cambiar foto",
                    color = VerdeMarca,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        EspacioCampos()

        EtiquetaCampo("NOMBRE COMPLETO")
        CampoTexto(value = nombre, onValueChange = { nombre = it }, placeholder = "Juan Pérez")

        EspacioCampos()

        EtiquetaCampo("TELÉFONO")
        CampoTexto(
            value = telefono,
            onValueChange = { telefono = it.filter { c -> c.isDigit() || c == ' ' }.take(15) },
            placeholder = "312 000 0000",
            tipoTeclado = KeyboardType.Phone
        )

        EspacioCampos()

        EtiquetaCampo("CORREO")
        CampoTexto(
            value = correo,
            onValueChange = { correo = it },
            placeholder = "correo@gmail.com",
            tipoTeclado = KeyboardType.Email
        )

        EspacioCampos()

        EtiquetaCampo("ESTADO")
        PestanasPildora(
            opciones = listOf("Activo", "Inactivo"),
            seleccionada = estados.indexOf(estadoSeleccionado).coerceAtLeast(0),
            onSeleccion = { estadoSeleccionado = estados[it] },
            margenHorizontal = 0.dp
        )

        EspacioCampos()

        EtiquetaCampo("TIPO DE PERSONAL")
        PestanasPildora(
            opciones = tiposPersonal,
            seleccionada = tiposPersonal.indexOf(tipoSeleccionado).coerceAtLeast(0),
            onSeleccion = { tipoSeleccionado = tiposPersonal[it] },
            margenHorizontal = 0.dp
        )

        EspacioCampos()

        EtiquetaCampo("TURNO")
        PestanasPildora(
            opciones = turnos,
            seleccionada = turnos.indexOf(turnoSeleccionado).coerceAtLeast(0),
            onSeleccion = { turnoSeleccionado = turnos[it] },
            margenHorizontal = 0.dp
        )

        EspacioCampos()

        EtiquetaCampo("ÁREA DE TRABAJO")
        ExposedDropdownMenuBox(expanded = areaExpandida, onExpandedChange = { areaExpandida = it }) {
            CampoTexto(
                value = areaSeleccionada?.nombre ?: "Sin áreas registradas",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = areaExpandida) }
            )
            DropdownMenu(expanded = areaExpandida, onDismissRequest = { areaExpandida = false }) {
                areas.forEach { area ->
                    DropdownMenuItem(
                        text = { Text(area.nombre) },
                        onClick = { areaSeleccionada = area; areaExpandida = false }
                    )
                }
            }
        }

        EspacioCampos()

        EtiquetaCampo(if (esEdicion) "CONTRASEÑA (DEJAR EN BLANCO SI NO SE CAMBIA)" else "CONTRASEÑA")
        CampoTexto(
            value = contrasena,
            onValueChange = { contrasena = it },
            placeholder = "Mín. 8 caracteres, 1 mayúscula, 1 número",
            esContrasena = true
        )

        EspacioCampos()

        EtiquetaCampo("CONFIRMAR CONTRASEÑA")
        CampoTexto(
            value = confirmarContrasena,
            onValueChange = { confirmarContrasena = it },
            placeholder = "********",
            esContrasena = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (mensajeError != null) {
            Text(
                text = mensajeError.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        BotonPrimario(
            texto = if (esEdicion) "Guardar cambios" else "Agregar personal",
            enabled = puedeGuardar,
            onClick = {
                if (esEdicion && contrasena.isEmpty()) {
                    mensajeError = null
                    onGuardar(nombre, correo, telefono, estadoSeleccionado, puestoGenerado, turnoSeleccionado, areaSeleccionada?.id, "", fotoPerfilPath)
                } else {
                    when {
                        contrasena.length < 8 -> mensajeError = "La contraseña debe tener al menos 8 caracteres."
                        contrasena.none { it.isUpperCase() } -> mensajeError = "La contraseña debe incluir al menos una letra mayúscula."
                        contrasena.none { it.isDigit() } -> mensajeError = "La contraseña debe incluir al menos un número."
                        contrasena != confirmarContrasena -> mensajeError = "Las contraseñas no coinciden."
                        else -> {
                            mensajeError = null
                            onGuardar(nombre, correo, telefono, estadoSeleccionado, puestoGenerado, turnoSeleccionado, areaSeleccionada?.id, contrasena, fotoPerfilPath)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun EmpleadoCard(empleado: EmpleadoUI, onEditClick: () -> Unit) {
    val context = LocalContext.current
    val fotoPerfil = remember(empleado.usuarioId) {
        FotoPerfilManager.obtenerFoto(context, empleado.usuarioId)?.absolutePath ?: empleado.fotoUrl
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    if (fotoPerfil != null) {
                        AsyncImage(
                            model = fotoPerfil,
                            contentDescription = "Foto de empleado",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val letra = if (empleado.nombre.isNotEmpty()) empleado.nombre.take(1).uppercase() else "?"
                        Text(text = letra, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = empleado.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF333333))
                    Text(text = "${empleado.puesto} • ${empleado.turno}", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }

                val colorEstado = if (empleado.estado == "ACTIVO") Color(0xFF2E7D32) else Color(0xFFC62828)
                val fondoEstado = if (empleado.estado == "ACTIVO") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                Surface(shape = RoundedCornerShape(8.dp), color = fondoEstado) {
                    Text(
                        text = empleado.estado,
                        color = colorEstado,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFE3F2FD)) {
                    Text(
                        text = "📍 ${empleado.areaNombre}",
                        color = Color(0xFF1E88E5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                TextButton(
                    onClick = onEditClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1E88E5))
                ) {
                    Text(text = "Editar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}