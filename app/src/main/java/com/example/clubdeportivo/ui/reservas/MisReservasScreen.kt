package com.example.clubdeportivo.ui.reservas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clubdeportivo.ui.components.EmptyState
import com.example.clubdeportivo.ui.components.EncabezadoPantalla
import com.example.clubdeportivo.ui.components.MargenPantalla
import com.example.clubdeportivo.ui.theme.FondoApp

@Composable
fun MisReservasScreen(
    viewModel: ReservasViewModel = viewModel()
) {
    val reservas by viewModel.reservas.observeAsState(emptyList())
    val mensaje by viewModel.mensaje.observeAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMensajeMostrado()
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
                .padding(horizontal = MargenPantalla)
        ) {
            EncabezadoPantalla(titulo = "Mis reservas")

            if (reservas.isEmpty()) {
                EmptyState(mensaje = "No tienes reservas registradas.", icono = Icons.Outlined.EventBusy)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(reservas, key = { it.id }) { reserva ->
                        TarjetaReservaCliente(
                            reserva = reserva,
                            onCancelar = { viewModel.cancelar(reserva) }
                        )
                    }
                }
            }
        }
    }
}
