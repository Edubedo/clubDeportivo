package com.example.clubdeportivo.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import com.example.clubdeportivo.util.FotoPerfilManager
import com.example.clubdeportivo.data.SesionManager

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

    // Cuando cargue el usuario, llenamos el formulario
    LaunchedEffect(usuario) {
        nombre = usuario?.nombre.orEmpty()
        correo = usuario?.correo.orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFD))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Avatar
        Box(
            contentAlignment = Alignment.Center
        ) {

            if (fotoPerfil != null) {

                AsyncImage(
                    model = fotoPerfil,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable {
                            selectorFoto.launch("image/*")
                        },
                    contentScale = ContentScale.Crop
                )

            } else {

                Box(
                    modifier = Modifier.clickable {
                        selectorFoto.launch("image/*")
                    }
                ) {
                    InitialsAvatar(
                        nombre = usuario?.nombre ?: "?",
                        size = 100.dp
                    )
                }
            }
        }
        TextButton(
            onClick = {
                selectorFoto.launch("image/*")
            }
        ) {
            Text(
                if (fotoPerfil == null)
                    "Agregar foto"
                else
                    "Cambiar foto"
            )
        }
        if (fotoPerfil != null) {

            TextButton(
                onClick = {

                    usuario?.let { usuarioActual ->

                        FotoPerfilManager.eliminarFoto(
                            context = context,
                            usuarioId = usuarioActual.id
                        )

                        fotoPerfil = null

                        SesionManager.notificarCambioFoto()
                    }
                }
            ) {
                Text(
                    text = "Quitar foto",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Nombre
        Text(
            text = usuario?.nombre ?: "Administrador",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Rol
        Text(
            text = usuario?.rol?.nombreLegible() ?: "Administrador",
            fontSize = 14.sp,
            color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Tarjeta con información editable
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "Información personal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Text(
                    text = "Actualiza los datos de tu perfil",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                EtiquetaCampo("NOMBRE")
                CampoTexto(
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholder = "Nombre del administrador",
                    leadingIcon = { Icon(imageVector = Icons.Filled.Person, contentDescription = null) }
                )

                EspacioCampos()

                EtiquetaCampo("CORREO ELECTRÓNICO")
                CampoTexto(
                    value = correo,
                    onValueChange = { correo = it },
                    placeholder = "correo@clubdeportivo.com",
                    tipoTeclado = KeyboardType.Email,
                    leadingIcon = { Icon(imageVector = Icons.Filled.Email, contentDescription = null) }
                )

                EspacioCampos()

                EtiquetaCampo("ROL")
                CampoTexto(
                    value = usuario?.rol?.nombreLegible() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = { Icon(imageVector = Icons.Filled.Person, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                BotonPrimario(
                    texto = "Guardar cambios",
                    onClick = { viewModel.actualizarPerfil(nombre = nombre) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sesión
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "Sesión",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Text(
                    text = "Cierra tu sesión en este dispositivo.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

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