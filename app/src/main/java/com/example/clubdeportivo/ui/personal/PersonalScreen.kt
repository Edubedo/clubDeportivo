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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
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

    var mostrarBottomSheet by remember { mutableStateOf(false) }
    var empleadoEnEdicion by remember { mutableStateOf<EmpleadoUI?>(null) }

    Scaffold(
        containerColor = Color(0xFFF9F9F9),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    empleadoEnEdicion = null
                    mostrarBottomSheet = true
                },
                containerColor = Color(0xFF00D15B),
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
                            mostrarBottomSheet = true
                        }
                    )
                }
            }
        }
    }

    if (mostrarBottomSheet) {
        ModalBottomSheet(onDismissRequest = { mostrarBottomSheet = false }, containerColor = Color.White) {
            FormularioPersonalContent(
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
                    mostrarBottomSheet = false
                },
                onCancelar = {
                    mostrarBottomSheet = false
                }
            )
        }
    }
}

// =====================================================================
// 5. FORMULARIO Y COMPONENTES
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioPersonalContent(
    areas: List<Area>,
    empleadoAEditar: EmpleadoUI?,
    onGuardar: (String, String, String, String, String, String, String?, String, String?) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current

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
            val archivoFoto = FotoPerfilManager.guardarFoto(
                context = context,
                uri = uri,
                usuarioId = tempUserId
            )
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

    val tipoInicial = tiposPersonal.find { empleadoAEditar?.puesto?.startsWith(it) == true } ?: tiposPersonal[0]
    var tipoExpandido by remember { mutableStateOf(false) }
    var tipoSeleccionado by remember { mutableStateOf(tipoInicial) }

    var turnoExpandido by remember { mutableStateOf(false) }
    var turnoSeleccionado by remember { mutableStateOf(empleadoAEditar?.turno?.ifEmpty { turnos[0] } ?: turnos[0]) }

    var estadoExpandido by remember { mutableStateOf(false) }

    val nombreArea = areaSeleccionada?.nombre ?: "Sin Área"
    val puestoGenerado = "$tipoSeleccionado de $nombreArea"

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        Text(
            text = if (empleadoAEditar == null) "Agregar Personal" else "Editar Personal",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f, fill = false)) {
            item {
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
                            color = Color(0xFF1E88E5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            item {
                InputLabel("NOMBRE COMPLETO")
                CustomTextField(value = nombre, onValueChange = { nombre = it }, placeholder = "Juan Pérez")
            }
            item {
                InputLabel("TELÉFONO")
                CustomTextField(value = telefono, onValueChange = { telefono = it }, placeholder = "312 000 0000")
            }
            item {
                InputLabel("GMAIL / CORREO")
                CustomTextField(value = correo, onValueChange = { correo = it }, placeholder = "correo@gmail.com")
            }
            item {
                InputLabel("ESTADO")
                ExposedDropdownMenuBox(expanded = estadoExpandido, onExpandedChange = { estadoExpandido = !estadoExpandido }) {
                    CustomTextField(
                        value = estadoSeleccionado, onValueChange = {}, readOnly = true,
                        modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) }
                    )
                    ExposedDropdownMenu(expanded = estadoExpandido, onDismissRequest = { estadoExpandido = false }) {
                        estados.forEach { est ->
                            DropdownMenuItem(
                                text = { Text(est) },
                                onClick = { estadoSeleccionado = est; estadoExpandido = false }
                            )
                        }
                    }
                }
            }
            item {
                InputLabel("CONTRASEÑA (Dejar en blanco si no se cambia)")
                CustomTextField(value = contrasena, onValueChange = { contrasena = it }, placeholder = "Mín. 8 caracteres, 1 Mayús, 1 Núm", esContrasena = true)
            }
            item {
                InputLabel("CONFIRMAR CONTRASEÑA")
                CustomTextField(value = confirmarContrasena, onValueChange = { confirmarContrasena = it }, placeholder = "********", esContrasena = true)
            }

            if (mensajeError != null) {
                item {
                    Text(text = mensajeError!!, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                InputLabel("ÁREA DE TRABAJO")
                ExposedDropdownMenuBox(expanded = areaExpandida, onExpandedChange = { areaExpandida = !areaExpandida }) {
                    CustomTextField(
                        value = areaSeleccionada?.nombre ?: "Sin áreas registradas", onValueChange = {}, readOnly = true,
                        modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) }
                    )
                    ExposedDropdownMenu(expanded = areaExpandida, onDismissRequest = { areaExpandida = false }) {
                        areas.forEach { area ->
                            DropdownMenuItem(
                                text = { Text(area.nombre) },
                                onClick = { areaSeleccionada = area; areaExpandida = false }
                            )
                        }
                    }
                }
            }
            item {
                InputLabel("TIPO DE PERSONAL")
                ExposedDropdownMenuBox(expanded = tipoExpandido, onExpandedChange = { tipoExpandido = !tipoExpandido }) {
                    CustomTextField(
                        value = puestoGenerado, onValueChange = {}, readOnly = true,
                        modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) }
                    )
                    ExposedDropdownMenu(expanded = tipoExpandido, onDismissRequest = { tipoExpandido = false }) {
                        tiposPersonal.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo) },
                                onClick = { tipoSeleccionado = tipo; tipoExpandido = false }
                            )
                        }
                    }
                }
            }
            item {
                InputLabel("TURNO")
                ExposedDropdownMenuBox(expanded = turnoExpandido, onExpandedChange = { turnoExpandido = !turnoExpandido }) {
                    CustomTextField(
                        value = turnoSeleccionado, onValueChange = {}, readOnly = true,
                        modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) }
                    )
                    ExposedDropdownMenu(expanded = turnoExpandido, onDismissRequest = { turnoExpandido = false }) {
                        turnos.forEach { turno ->
                            DropdownMenuItem(
                                text = { Text(turno) },
                                onClick = { turnoSeleccionado = turno; turnoExpandido = false }
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Fila de botones: Cancelar y Guardar/Actualizar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar", fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            }

            Button(
                onClick = {
                    if (empleadoAEditar != null && contrasena.isEmpty()) {
                        mensajeError = null
                        onGuardar(nombre, correo, telefono, estadoSeleccionado, puestoGenerado, turnoSeleccionado, areaSeleccionada?.id, "", fotoPerfilPath)
                    } else {
                        val tieneMinimo8 = contrasena.length >= 8
                        val tieneMayuscula = contrasena.any { it.isUpperCase() }
                        val tieneNumero = contrasena.any { it.isDigit() }

                        when {
                            !tieneMinimo8 -> mensajeError = "La contraseña debe tener al menos 8 caracteres."
                            !tieneMayuscula -> mensajeError = "La contraseña debe incluir al menos una letra mayúscula."
                            !tieneNumero -> mensajeError = "La contraseña debe incluir al menos un número."
                            contrasena != confirmarContrasena -> mensajeError = "Las contraseñas no coinciden."
                            else -> {
                                mensajeError = null
                                onGuardar(nombre, correo, telefono, estadoSeleccionado, puestoGenerado, turnoSeleccionado, areaSeleccionada?.id, contrasena, fotoPerfilPath)
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D15B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(if (empleadoAEditar == null) "Guardar" else "Actualizar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InputLabel(text: String) {
    Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), modifier = Modifier.padding(bottom = 6.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    placeholder: String = "", readOnly: Boolean = false, esContrasena: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, placeholder = { Text(placeholder, color = Color(0xFF94A3B8)) },
        readOnly = readOnly, trailingIcon = trailingIcon, shape = RoundedCornerShape(12.dp),
        visualTransformation = if (esContrasena) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF1F5F9), unfocusedContainerColor = Color(0xFFF1F5F9),
            focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
        ),
        modifier = modifier.fillMaxWidth()
    )
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