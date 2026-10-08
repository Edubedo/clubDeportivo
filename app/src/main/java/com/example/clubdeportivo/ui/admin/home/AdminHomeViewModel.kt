package com.example.clubdeportivo.ui.admin.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.MigracionEsquema
import com.example.clubdeportivo.data.model.DisponibilidadArea
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.util.CalculoResumenAdmin
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ResumenAdmin
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Lo que pasa en el club hoy; null mientras no se ha podido leer ese dato. */
data class OperacionHoy(
    val reservasHoy: Int? = null,
    val pendientesDeAprobar: Int? = null,
    val areas: Int? = null,
    val areasEnMantenimiento: Int? = null,
    val articulosConStockBajo: Int? = null,
    val personal: Int? = null,
    val torneosVigentes: Int? = null
)

/** Datos del dashboard del administrador: dinero y membresías primero, y después la operación del día. */
class AdminHomeViewModel : ViewModel() {

    var resumen by mutableStateOf<ResumenAdmin?>(null)
        private set
    var operacion by mutableStateOf(OperacionHoy())
        private set
    var proximasReservas by mutableStateOf<List<Reserva>?>(null)
        private set
    var cargando by mutableStateOf(true)
        private set
    /** true si no se pudieron leer las membresías o los cobros (se muestra un aviso en vez de ceros engañosos). */
    var errorFinanzas by mutableStateOf(false)
        private set

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            // Primero se deja la base en la estructura actual (solo hace algo la primera vez que entra el personal).
            try {
                MigracionEsquema.asegurar(FirebaseFirestore.getInstance())
            } catch (e: Exception) {
                Log.w("Esquema", "No se pudo migrar la estructura de la base de datos", e)
            }

            val hoy = Fechas.hoy()
            coroutineScope {
                val finanzas = async {
                    runCatching {
                        val membresias = AppContainer.gestionMembresiasRepository.obtenerTodas()
                        val pagos = AppContainer.pagosRepository.obtenerDesde(CalculoResumenAdmin.primerDiaDeLaGrafica(hoy))
                        CalculoResumenAdmin.calcular(membresias, pagos, hoy)
                    }.getOrNull()
                }
                val reservas = async { runCatching { AppContainer.reservaRepository.obtenerReservasVigentes() }.getOrNull() }
                val areas = async { runCatching { AppContainer.areaRepository.obtenerAreas() }.getOrNull() }
                val inventario = async { runCatching { AppContainer.inventarioRepository.obtenerArticulos() }.getOrNull() }
                val personal = async { runCatching { AppContainer.personalRepository.contarPersonal() }.getOrNull() }
                val torneos = async { runCatching { AppContainer.torneoRepository.obtenerTorneos() }.getOrNull() }

                val resultadoFinanzas = finanzas.await()
                resumen = resultadoFinanzas
                errorFinanzas = resultadoFinanzas == null

                val listaReservas = reservas.await()
                val listaAreas = areas.await()
                proximasReservas = listaReservas
                    // "Próximas": las de hoy que ya terminaron no cuentan.
                    ?.filter { it.fecha > hoy || (it.fecha == hoy && Fechas.horasDesdeAhora(it.fecha, it.horaFin) > 0) }
                    ?.sortedWith(compareBy({ it.fecha }, { it.horaInicio }))
                    ?.take(5)
                    ?: emptyList()

                operacion = OperacionHoy(
                    reservasHoy = listaReservas?.count { it.fecha == hoy },
                    pendientesDeAprobar = listaReservas?.count { it.estado == EstadoReserva.PENDIENTE_APROBACION && it.fecha >= hoy },
                    areas = listaAreas?.size,
                    areasEnMantenimiento = listaAreas?.count { it.disponibilidad == DisponibilidadArea.MANTENIMIENTO },
                    articulosConStockBajo = inventario.await()?.count { it.cantidad <= it.stockMinimo },
                    personal = personal.await(),
                    torneosVigentes = torneos.await()?.count { it.fechaFin >= hoy }
                )
            }
            cargando = false
        }
    }
}
