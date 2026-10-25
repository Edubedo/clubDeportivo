package com.example.clubdeportivo.notificaciones

import android.content.Context
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.data.model.puedeVerDeporte
import com.example.clubdeportivo.data.repository.EmuladoresFirebase
import com.example.clubdeportivo.util.Fechas
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Revisa los datos de la persona que tiene la sesión guardada y muestra los recordatorios que correspondan a su tipo
 * (ver [CategoriaNotificacion]). Corre cada ~15 minutos en segundo plano y al abrir la app.
 */
object MotorRecordatorios {

    /**
     * Solo en compilaciones contra los emuladores de Firebase (`-PfirebaseEmulador`, nunca en la app real): permite
     * "fingir" la hora para probar de día lo que depende de ella (silencio nocturno, resúmenes de las 8). Se fija con
     * `run-as <paquete>` escribiendo `ahora_simulada` (milisegundos) en `shared_prefs/notificaciones_ajustes.xml`.
     */
    private fun horaSimulada(contexto: Context): Date? {
        if (!EmuladoresFirebase.activo) return null
        val ms = contexto.getSharedPreferences("notificaciones_ajustes", Context.MODE_PRIVATE).getLong("ahora_simulada", 0L)
        return if (ms > 0L) Date(ms) else null
    }

    suspend fun ejecutar(contexto: Context, reloj: Date = Date()) {
        val ahora = horaSimulada(contexto) ?: reloj
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (!Notificador.puedeNotificar(contexto)) return

        val perfil = FirebaseFirestore.getInstance().collection("usuarios").document(uid).get().await()
        if (!perfil.exists() || perfil.getString("estado") == "INACTIVO") return
        val rol = runCatching { Rol.valueOf(perfil.getString("rol").orEmpty()) }.getOrNull() ?: return
        val area = perfil.getString("areaTrabajo").orEmpty()
        val usuario = Usuario(uid, perfil.getString("nombre").orEmpty(), perfil.getString("email").orEmpty(), rol, areaTrabajo = area)
        val tipo = TipoUsuario.de(rol)

        Notificador.crearCanales(contexto)
        val registro = PreferenciasNotificaciones.Registro(contexto, uid)
        val hoy = Fechas.hoy()
        val silencio = ReglasRecordatorios.enSilencio(ahora)
        val nuevo: (String) -> Boolean = { !registro.yaAvisado(it) }
        val activas = CategoriaNotificacion.de(tipo).filter { PreferenciasNotificaciones.activa(contexto, it) }.toSet()

        val recordatorios = mutableListOf<Recordatorio>()
        fun agregar(lista: List<Recordatorio?>) { recordatorios += lista.filterNotNull().filter { it.categoria in activas } }

        // Cada bloque falla por separado: si una consulta no se pudo (sin permiso, sin red), las demás siguen.
        suspend fun bloque(accion: suspend () -> Unit) { runCatching { accion() } }

        when (tipo) {
            TipoUsuario.CLIENTE -> {
                bloque {
                    val reservas = AppContainer.reservaRepository.obtenerReservasDeUsuario(uid)
                    agregar(ReglasRecordatorios.reservasProximas(reservas, ahora, nuevo))
                    agregar(ReglasRecordatorios.respuestasDeReserva(reservas, registro.estadosDeReservas()))
                    registro.guardarEstadosDeReservas(
                        reservas.filter { it.fecha >= hoy || it.estado == EstadoReserva.PENDIENTE_APROBACION }.associate { it.id to it.estado.name }
                    )
                }
                bloque { agregar(ReglasRecordatorios.membresia(AppContainer.membresiaRepository.obtenerMembresia(uid), hoy, nuevo)) }
                bloque {
                    val inscritos = AppContainer.torneoRepository.torneosInscritos(uid)
                    val torneos = AppContainer.torneoRepository.obtenerTorneos().filter { it.id in inscritos }
                    agregar(ReglasRecordatorios.torneosDeManana(torneos, hoy, comoCliente = true, nuevo = nuevo))
                }
            }
            TipoUsuario.ENCARGADO, TipoUsuario.ADMINISTRADOR -> {
                val esAdmin = tipo == TipoUsuario.ADMINISTRADOR
                bloque {
                    val vigentes = if (esAdmin) AppContainer.reservaRepository.obtenerReservasVigentes()
                    else AppContainer.reservaRepository.obtenerReservasPorDeporte(area)
                    agregar(listOf(ReglasRecordatorios.solicitudes(vigentes, hoy, nuevo)))
                    if (!esAdmin) {
                        val confirmadas = vigentes.filter { it.estado == EstadoReserva.CONFIRMADA && it.fecha == hoy }
                        val conAsistencia = confirmadas.filter { AppContainer.reservaRepository.obtenerAsistencia(it.id) != null }.map { it.id }.toSet()
                        agregar(listOf(ReglasRecordatorios.asistencias(confirmadas, conAsistencia, ahora, nuevo)))
                    }
                }
                bloque {
                    val articulos = AppContainer.inventarioRepository.obtenerArticulos().filter { usuario.puedeVerDeporte(it.deporte) }
                    agregar(listOf(ReglasRecordatorios.stockBajo(articulos, hoy, ahora, nuevo)))
                }
                if (esAdmin) {
                    bloque {
                        val membresias = AppContainer.gestionMembresiasRepository.obtenerTodas().map { it.membresia }
                        agregar(listOf(ReglasRecordatorios.vencimientos(membresias, hoy, ahora, nuevo)))
                    }
                }
                bloque {
                    val torneos = AppContainer.torneoRepository.obtenerTorneos().filter { esAdmin || usuario.puedeVerDeporte(it.disciplina) }
                    agregar(ReglasRecordatorios.torneosDeManana(torneos, hoy, comoCliente = false, nuevo = nuevo))
                }
            }
        }

        bloque {
            val avisos = AppContainer.avisosRepository.obtener(esPersonal = tipo != TipoUsuario.CLIENTE)
            val desde = registro.ultimoAvisoMillis
            // La primera revisión solo "se pone al día": no se avisa de lo que ya existía antes de empezar.
            if (desde > 0L) agregar(ReglasRecordatorios.avisos(avisos, desde, uid, nuevo))
            registro.ultimoAvisoMillis = maxOf(ahora.time, avisos.maxOfOrNull { it.fechaMillis } ?: 0L)
        }

        // De noche se calcula todo, pero no se muestra ni se marca como avisado: sale en la primera revisión de la mañana.
        if (silencio) return
        recordatorios.forEach { recordatorio ->
            Notificador.mostrar(contexto, recordatorio)
            registro.marcarAvisado(recordatorio.claves, hoy)
        }
    }
}
