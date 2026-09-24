package com.example.clubdeportivo.ui.areas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

// --- MODELOS DE DATOS FAKE SOLO ES ESCRITO) ---
data class Empleado(
    val id: String,
    val inicial: String,
    val nombre: String,
    val rol: String,
    val turno: String,
    val colorAvatarFondo: Color,
    val colorAvatarTexto: Color,
    val areasAsignadas: List<AreaAsignada>
)

data class AreaAsignada(
    val icono: String,
    val nombre: String,
    val colorFondo: Color,
    val colorTexto: Color
)

data class CanchaMock(
    val id: String,
    val icono: String,
    val nombre: String
)

// ---  DATOS FAKES ---
class PersonalViewModel : ViewModel() {
    private val _empleados = MutableLiveData<List<Empleado>>()
    val empleados: LiveData<List<Empleado>> = _empleados

    init {
        cargarPersonalMock()
    }

    private fun cargarPersonalMock() {
        _empleados.value = listOf(
            Empleado(
                id = "1", inicial = "C", nombre = "Carlos Mendez", rol = "Instructor de Tenis", turno = "5:00 am a 1:30 pm",
                colorAvatarFondo = Color(0xFFE8F5E9), colorAvatarTexto = Color(0xFF4CAF50),
                areasAsignadas = listOf(
                    AreaAsignada("🎾", "Cancha A", Color(0xFFFFF9C4), Color(0xFFFBC02D)),
                    AreaAsignada("🎾", "Cancha B", Color(0xFFFFF9C4), Color(0xFFFBC02D))
                )
            ),
            Empleado(
                id = "2", inicial = "M", nombre = "Miguel Torres", rol = "Instructor de Fútbol", turno = "1:30 pm a 10:00 pm",
                colorAvatarFondo = Color(0xFFE8F5E9), colorAvatarTexto = Color(0xFF4CAF50),
                areasAsignadas = listOf(
                    AreaAsignada("⚽", "Cancha A", Color(0xFFE0F2F1), Color(0xFF26A69A)),
                    AreaAsignada("⚽", "Cancha B", Color(0xFFE0F2F1), Color(0xFF26A69A))
                )
            ),
            Empleado(
                id = "3", inicial = "S", nombre = "Sofía Ramírez", rol = "Salvavidas", turno = "5:00 am a 1:30 pm",
                colorAvatarFondo = Color(0xFFE8F5E9), colorAvatarTexto = Color(0xFF4CAF50),
                areasAsignadas = listOf(
                    AreaAsignada("🏊", "Piscina Principal", Color(0xFFE3F2FD), Color(0xFF2196F3))
                )
            )
        )
    }
}

// --- VISTA PRINCIPAL ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreasScreen(
    viewModel: PersonalViewModel = viewModel()
) {
    val empleados by viewModel.empleados.observeAsState(emptyList())
    var mostrarBottomSheet by remember { mutableStateOf(false) }
    var empleadoEnEdicion by remember { mutableStateOf<Empleado?>(null) }

    Scaffold(
        containerColor = Color(0xFFF9F9F9),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    empleadoEnEdicion = null
                    mostrarBottomSheet = true
                },
                containerColor = Color(0xFF00D15B),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Personal")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar Personal", fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Personal",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(empleados, key = { it.id }) { empleado ->
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
        ModalBottomSheet(
            onDismissRequest = { mostrarBottomSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            FormularioPersonalContent(
                empleado = empleadoEnEdicion,
                onDismiss = { mostrarBottomSheet = false }
            )
        }
    }
}

// --- FORMULARIO BOTTOM SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioPersonalContent(
    empleado: Empleado?,
    onDismiss: () -> Unit
) {
    val esModoEdicion = empleado != null
    val tituloFormulario = if (esModoEdicion) "Editar Personal" else "+ Agregar Personal"
    val textoBoton = if (esModoEdicion) "Guardar Cambios" else "+ Agregar Personal"

    val roles = listOf(
        "Instructor de Tenis", "Instructor de Baloncesto", "Instructor de Voleibol",
        "Instructor de Fútbol", "Salvavidas", "Administrador de área"
    )

    val turnos = listOf(
        "5:00 am a 1:30 pm",
        "1:30 pm a 10:00 pm"
    )

    var nombre by remember(empleado) { mutableStateOf(empleado?.nombre ?: "") }
    var correo by remember(empleado) { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    var rolExpandido by remember { mutableStateOf(false) }
    var rolSeleccionado by remember(empleado) {
        mutableStateOf(roles.find { it == empleado?.rol } ?: roles[0])
    }

    var turnoExpandido by remember { mutableStateOf(false) }
    var turnoSeleccionado by remember(empleado) {
        mutableStateOf(turnos.find { it == empleado?.turno } ?: turnos[0])
    }

    val canchasMock = listOf(
        CanchaMock("1", "🏀", "Baloncesto — Cancha A"),
        CanchaMock("2", "🏀", "Baloncesto — Cancha B"),
        CanchaMock("3", "🏐", "Voleibol — Cancha A")
    )
    val canchasSeleccionadas = remember { mutableStateListOf("1") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tituloFormulario,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF94A3B8))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            item {
                InputLabel("NOMBRE COMPLETO")
                CustomTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholder = "Nombre"
                )
            }

            item {
                InputLabel("TIPO DE PERSONAL")
                ExposedDropdownMenuBox(
                    expanded = rolExpandido,
                    onExpandedChange = { rolExpandido = !rolExpandido }
                ) {
                    CustomTextField(
                        value = rolSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        trailingIcon = {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Desplegar", tint = Color(0xFF94A3B8))
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = rolExpandido,
                        onDismissRequest = { rolExpandido = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        roles.forEach { seleccion ->
                            DropdownMenuItem(
                                text = { Text(seleccion, color = Color(0xFF333333)) },
                                onClick = {
                                    rolSeleccionado = seleccion
                                    rolExpandido = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                InputLabel("TURNO")
                ExposedDropdownMenuBox(
                    expanded = turnoExpandido,
                    onExpandedChange = { turnoExpandido = !turnoExpandido }
                ) {
                    CustomTextField(
                        value = turnoSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        trailingIcon = {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Desplegar", tint = Color(0xFF94A3B8))
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = turnoExpandido,
                        onDismissRequest = { turnoExpandido = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        turnos.forEach { seleccion ->
                            DropdownMenuItem(
                                text = { Text(seleccion, color = Color(0xFF333333)) },
                                onClick = {
                                    turnoSeleccionado = seleccion
                                    turnoExpandido = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                InputLabel("CORREO ELECTRÓNICO")
                CustomTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    placeholder = "email@club.com"
                )
            }

            item {
                InputLabel("CONTRASEÑA DE ACCESO")
                CustomTextField(
                    value = contrasena,
                    onValueChange = { contrasena = it },
                    placeholder = "Contraseña"
                )
            }

            item {
                InputLabel("CANCHAS ASIGNADAS")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    canchasMock.forEach { cancha ->
                        CanchaCheckboxItem(
                            cancha = cancha,
                            seleccionada = canchasSeleccionadas.contains(cancha.id),
                            onCheckedChange = { isChecked ->
                                if (isChecked) canchasSeleccionadas.add(cancha.id)
                                else canchasSeleccionadas.remove(cancha.id)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onDismiss() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D15B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(textoBoton, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }
    }
}

// --- COMPONENTES AUXILIARES UI ---
@Composable
fun InputLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF94A3B8),
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color(0xFF94A3B8)) },
        readOnly = readOnly,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF1F5F9),
            unfocusedContainerColor = Color(0xFFF1F5F9),
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = Color(0xFF333333),
            unfocusedTextColor = Color(0xFF333333)
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun CanchaCheckboxItem(cancha: CanchaMock, seleccionada: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val borderColor = if (seleccionada) Color(0xFF00D15B).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
    val bgColor = if (seleccionada) Color(0xFF00D15B).copy(alpha = 0.05f) else Color.White

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!seleccionada) }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Checkbox(
            checked = seleccionada,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF94A3B8), uncheckedColor = Color(0xFF94A3B8))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = cancha.icono, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = cancha.nombre,
            fontSize = 14.sp,
            color = Color(0xFF333333),
            fontWeight = if (seleccionada) FontWeight.Medium else FontWeight.Normal
        )
    }
}

// --- COMPONENTES DE LA TARJETA ---
@Composable
private fun EmpleadoCard(empleado: Empleado, onEditClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(empleado.colorAvatarFondo, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = empleado.inicial,
                        color = empleado.colorAvatarTexto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = empleado.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF333333)
                    )
                    Text(
                        text = empleado.rol,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "−",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = "Editar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                empleado.areasAsignadas.forEach { area ->
                    AreaBadge(area = area)
                }
            }
        }
    }
}

@Composable
private fun AreaBadge(area: AreaAsignada) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = area.colorFondo
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = area.icono, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = area.nombre,
                color = area.colorTexto,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}