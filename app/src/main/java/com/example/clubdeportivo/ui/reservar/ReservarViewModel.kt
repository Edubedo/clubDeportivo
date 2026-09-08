package com.example.clubdeportivo.ui.reservar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.MaterialAsignado
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.model.RestriccionHorario
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.repository.AreaRepository
import com.example.clubdeportivo.data.repository.ReservaRepository
import com.example.clubdeportivo.data.repository.RestriccionHorarioRepository
import com.example.clubdeportivo.data.repository.TorneoRepository
import com.example.clubdeportivo.util.Fechas
import kotlinx.coroutines.launch

class ReservarViewModel(
    private val reservaRepository: ReservaRepository = AppContainer.reservaRepository,
    private val areaRepository: AreaRepository = AppContainer.areaRepository,
    private val restriccionHorarioRepository: RestriccionHorarioRepository = AppContainer.restriccionHorarioRepository,
    private val torneoRepository: TorneoRepository = AppContainer.torneoRepository
) : ViewModel() {

    private var area: Area? = null
    private var horario: RestriccionHorario? = null
    private val esVisitanteExterno get() = SesionManager.usuarioActual?.rol == Rol.VISITANTE_EXTERNO

    private val _bloqueadoParaExterno = MutableLiveData(false)
    val bloqueadoParaExterno: LiveData<Boolean> = _bloqueadoParaExterno

    private val _horarioTexto = MutableLiveData<String>()
    val horarioTexto: LiveData<String> = _horarioTexto

    private val _slots = MutableLiveData<List<String>>(emptyList())
    val slots: LiveData<List<String>> = _slots

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _reservaConfirmada = MutableLiveData<Reserva?>()
    val reservaConfirmada: LiveData<Reserva?> = _reservaConfirmada

    private val _materialAsignado = MutableLiveData<List<MaterialAsignado>>()
    val materialAsignado: LiveData<List<MaterialAsignado>> = _materialAsignado

    private val _errorReserva = MutableLiveData<String?>()
    val errorReserva: LiveData<String?> = _errorReserva

    fun cargarArea(areaId: String) {
        viewModelScope.launch {
            val areaCargada = areaRepository.obtenerAreaPorId(areaId) ?: return@launch
            area = areaCargada

            if (esVisitanteExterno && !areaCargada.permiteExternos) {
                _bloqueadoParaExterno.value = true
                return@launch
            }

            val horarioArea = restriccionHorarioRepository.obtenerHorarioDeArea(areaCargada)
            horario = horarioArea
            _horarioTexto.value = if (esVisitanteExterno) {
                "Horario para visitantes: ${Catalogos.HORA_INICIO_EXTERNOS} - ${Catalogos.HORA_FIN_EXTERNOS} (requiere aprobación)"
            } else {
                "Horario del área: ${horarioArea.horaInicio} - ${horarioArea.horaFin}"
            }

            // El Spinner de días dispara su primera selección casi de inmediato,
            // antes de que esta corrutina termine de cargar el horario (la
            // llamada le gana la carrera). Por eso el primer cálculo de
            // horarios se dispara aquí mismo, ya con el horario disponible.
            seleccionarDia(Fechas.hoy())
        }
    }

    fun seleccionarDia(fecha: String) {
        val areaActual = area ?: return
        val horarioArea = horario ?: return

        viewModelScope.launch {
            val bloqueadaPorTorneo = torneoRepository.hayTorneoQueBloqueaArea(areaActual.id, fecha)
            if (bloqueadaPorTorneo) {
                _slots.value = emptyList()
                return@launch
            }

            val horaAperturaTexto = if (esVisitanteExterno) {
                maxOf(Catalogos.HORA_INICIO_EXTERNOS, horarioArea.horaInicio)
            } else {
                horarioArea.horaInicio
            }
            val horaCierreTexto = if (esVisitanteExterno) {
                minOf(Catalogos.HORA_FIN_EXTERNOS, horarioArea.horaFin)
            } else {
                horarioArea.horaFin
            }

            val horaApertura = horaAperturaTexto.substring(0, 2).toInt()
            val horaCierre = horaCierreTexto.substring(0, 2).toInt()

            _slots.value = (horaApertura until horaCierre)
                .map { "%02d:00".format(it) }
                .filter { hora -> Fechas.horasDesdeAhora(fecha, hora) >= Catalogos.ANTICIPACION_MINIMA_HORAS }
        }
    }

    fun reservar(fecha: String, horaInicio: String) {
        val usuario = SesionManager.usuarioActual ?: return
        val areaActual = area ?: return
        val horaFin = "%02d:00".format(horaInicio.substring(0, 2).toInt() + 1)

        viewModelScope.launch {
            _cargando.value = true
            _errorReserva.value = null

            when {
                reservaRepository.estaBloqueadoPorInasistencias(usuario.id) -> {
                    _errorReserva.value = "No puedes reservar: tienes inasistencias recientes " +
                        "(${Catalogos.INASISTENCIAS_PARA_BLOQUEO} faltas) y estás bloqueado por " +
                        "${Catalogos.DIAS_BLOQUEO_POR_INASISTENCIAS} días."
                }
                reservaRepository.contarReservasActivas(usuario.id) >= Catalogos.MAX_RESERVAS_ACTIVAS_POR_SOCIO -> {
                    _errorReserva.value = "Ya tienes ${Catalogos.MAX_RESERVAS_ACTIVAS_POR_SOCIO} " +
                        "reservaciones activas. Cancela alguna antes de crear una nueva."
                }
                Fechas.horasDesdeAhora(fecha, horaInicio) < Catalogos.ANTICIPACION_MINIMA_HORAS -> {
                    _errorReserva.value = "Ese horario ya no cumple la anticipación mínima de " +
                        "${Catalogos.ANTICIPACION_MINIMA_HORAS} horas."
                }
                else -> {
                    val reserva = reservaRepository.crearReserva(
                        usuarioId = usuario.id,
                        areaId = areaActual.id,
                        fecha = fecha,
                        horaInicio = horaInicio,
                        horaFin = horaFin,
                        esExterno = esVisitanteExterno
                    )
                    _materialAsignado.value = reservaRepository.obtenerMaterialAsignado(reserva.id)
                    _reservaConfirmada.value = reserva
                }
            }

            _cargando.value = false
        }
    }
}
