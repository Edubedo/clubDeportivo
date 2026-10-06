package com.example.clubdeportivo.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.clubdeportivo.ui.components.BotonPrimario
import com.example.clubdeportivo.ui.components.BotonSecundario
import com.example.clubdeportivo.ui.components.CampoTexto
import com.example.clubdeportivo.ui.components.EspacioCampos
import com.example.clubdeportivo.ui.components.EtiquetaCampo
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.data.model.nombreLegible
import com.example.clubdeportivo.ui.components.InitialsAvatar
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import com.example.clubdeportivo.util.FotoPerfilManager
import com.example.clubdeportivo.data.SesionManager

private val TextoPrincipal = Color(0xFF192338)
private val TextoSecundario = Color(0xFF31487A)

@Composable
fun PerfilScreen(
    onCerrarSesion: () -> Unit,
    viewModel: PerfilViewModel = viewModel()
) {
    val usuario by viewModel.usuario.observeAsState()
    val context = LocalContext.current

    var fotoPerfil by remember(usuario?.id) {
        mutableStateOf(
            usuario?.id?.let { id ->
                FotoPerfilManager.obtenerFoto(context, id)
            }
        )
    }

    val selectorFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && usuario != null) {
            val nuevaFoto = FotoPerfilManager.guardarFoto(
                context = context,
                uri = uri,
                usuarioId = usuario!!.id
            )
            fotoPerfil = nuevaFoto
            SesionManager.notificarCambioFoto()
        }
    }

    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }

    LaunchedEffect(usuario) {
        nombre = usuario?.nombre.orEmpty()
        correo = usuario?.correo.orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEAF1F8))
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (fotoPerfil != null) {
                    AsyncImage(
                        model = fotoPerfil,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .clickable { selectorFoto.launch("image/*") },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.clickable { selectorFoto.launch("image/*") }) {
                        InitialsAvatar(nombre = usuario?.nombre ?: "?", size = 90.dp)
                    }
                }
            }
            TextButton(onClick = { selectorFoto.launch("image/*") }) {
                Text(
                    text = if (fotoPerfil == null) "Agregar foto" else "Cambiar foto",
                    color = TextoSecundario,
                    fontWeight = FontWeight.Medium
                )
            }
            if (fotoPerfil != null) {
                TextButton(
                    onClick = {
                        usuario?.let { usuarioActual ->
                            FotoPerfilManager.eliminarFoto(context = context, usuarioId = usuarioActual.id)
                            fotoPerfil = null
                            SesionManager.notificarCambioFoto()
                        }
                    }
                ) {
                    Text(text = "Quitar foto", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = usuario?.nombre ?: "Administrador",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = usuario?.rol?.nombreLegible() ?: "Administrador",
                fontSize = 13.sp,
                color = TextoSecundario,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Información personal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipal
                    )

                    Text(
                        text = "Actualiza los datos de tu perfil",
                        fontSize = 12.sp,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    EtiquetaCampo("NOMBRE")
                    CampoTexto(
                        value = nombre,
                        onValueChange = { nombre = it },
                        placeholder = "Nombre",
                        leadingIcon = { Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = TextoSecundario) }
                    )

                    EspacioCampos()

                    EtiquetaCampo("CORREO ELECTRÓNICO")
                    CampoTexto(
                        value = correo,
                        onValueChange = { correo = it },
                        placeholder = "correo@clubdeportivo.com",
                        tipoTeclado = KeyboardType.Email,
                        leadingIcon = { Icon(imageVector = Icons.Filled.Email, contentDescription = null, tint = TextoSecundario) }
                    )

                    EspacioCampos()

                    EtiquetaCampo("ROL")
                    CampoTexto(
                        value = usuario?.rol?.nombreLegible() ?: "",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = TextoSecundario) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    BotonPrimario(
                        texto = "Guardar cambios",
                        // Nota: Si tu función en el ViewModel acepta también correo, lo pasamos aquí.
                        // Si solo acepta nombre, asegúrate de que tu PerfilViewModel actualice ambos en la BD.
                        onClick = { viewModel.actualizarPerfil(nombre = nombre) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Sesión",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipal
                    )

                    Text(
                        text = "Cierra tu sesión en este dispositivo.",
                        fontSize = 12.sp,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    BotonSecundario(
                        texto = "Cerrar sesión",
                        colorTexto = MaterialTheme.colorScheme.error,
                        onClick = {
                            viewModel.cerrarSesion()
                            onCerrarSesion()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}