package com.example.clubdeportivo.notificaciones

import android.content.Context
import android.util.Log
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.GestorListaEspera
import com.example.clubdeportivo.data.model.EstadoEspera
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.util.ReglasListaEspera
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.data.model.Usuario
import com.example.clubdeportivo.data.model.puedeVerDeporte
import com.example.clubdeportivo.data.repository.EmuladoresFirebase
import com.example.clubdeportivo.util.Fechas
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
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
        suspend fun bloque(accion: suspend () -> Unit) {
            runCatching { accion() }
                .onFailure { Log.e(TAG, "Falló un bloque de recordatorios para $uid", it) }
        }

        when (tipo) {
            TipoUsuario.CLIENTE -> {
                bloque {
                    val reservas = AppContainer.reservaRepository.obtenerReservasDeUsuario(uid)
                    agregar(ReglasRecordatorios.reservasProximas(reservas, ahora, nuevo))
                    AlarmasRecordatorio.programar(contexto, reservas, ahora)
                    agregar(ReglasRecordatorios.respuestasDeReserva(reservas, registro.estadosDeReservas()))
                    registro.guardarEstadosDeReservas(
                        reservas.filter { it.fecha >= hoy || it.estado == EstadoReserva.PENDIENTE_APROBACION }.associate { it.id to it.estado.name }
                    )
                }
                bloque { agregar(ReglasRecordatorios.membresia(AppContainer.membresiaRepository.obtenerMembresia(uid), hoy, nuevo)) }
                bloque {
                    // Primero se apartan los lugares que se hayan liberado en las horas donde espera (nadie más lo hace
                    // si quien canceló no abrió la app), y después se avisa si ya le tocó.
                    val esperando = AppContainer.listaEsperaRepository.obtenerDeUsuario(uid)
                        .filter { it.estado == EstadoEspera.ESPERANDO && ReglasListaEspera.vigente(it, ahora) }
                    esperando.map { it.areaId to it.fecha }.distinct().forEach { (areaId, fecha) ->
                        runCatching { GestorListaEspera.promover(areaId, fecha, ahora) }
                    }
                    agregar(ReglasRecordatorios.listaEspera(AppContainer.listaEsperaRepository.obtenerDeUsuario(uid), ahora, nuevo))
                }
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
                bloque {
                    val membresias = AppContainer.gestionMembresiasRepository.obtenerTodas().map { it.membresia }
                    agregar(listOf(ReglasRecordatorios.vencimientos(membresias, hoy, ahora, nuevo)))
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

        // Lo que el servidor dejó para esta persona (renovación aprobada, cambio de plan resuelto, solicitudes nuevas...).
        val delServidor = mutableListOf<ReglasRecordatorios.NotificacionDeServidor>()
        bloque {
            FirebaseFirestore.getInstance().collection("notificaciones")
                .whereEqualTo("usuarioId", uid).whereEqualTo("leida", false)
                .get().await().documents.forEach { d ->
                    delServidor += ReglasRecordatorios.NotificacionDeServidor(
                        d.id, d.getString("titulo").orEmpty(), d.getString("mensaje").orEmpty(), d.getString("ruta")
                    )
                }
            agregar(ReglasRecordatorios.deServidor(delServidor, tipo, nuevo))
        }

        // De noche se calcula todo, pero no se muestra ni se marca como avisado: sale en la primera revisión de la mañana.
        if (silencio) return
        recordatorios.forEach { recordatorio ->
            if (recordatorio.claves.any { it.startsWith("solicitud:") } &&
                recordatorio.categoria == CategoriaNotificacion.SOLICITUDES
            ) return@forEach
            if (recordatorio.claves.none { it.startsWith("servidor:") }) {
                runCatching { guardarEnBandeja(uid, recordatorio) }
                    .onFailure { Log.e(TAG, "No se pudo guardar en la bandeja el aviso ${recordatorio.claves}", it) }
            }
            // Sin permiso (o con las notificaciones apagadas) nada se muestra: no se marca como avisado ni como leído,
            // así el aviso sale cuando la persona las active.
            if (!Notificador.mostrar(contexto, recordatorio)) return@forEach
            registro.marcarAvisado(recordatorio.claves, hoy)
        }
    }

    private suspend fun guardarEnBandeja(uid: String, recordatorio: Recordatorio) {
        val clave = recordatorio.claves.joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(clave.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val documentoId = when {
            recordatorio.claves.size == 1 && recordatorio.claves.single().startsWith("solicitud:") ->
                "reserva_${recordatorio.claves.single().substringAfter(':')}_nueva_$uid"
            recordatorio.claves.size == 1 && recordatorio.claves.single().startsWith("respuesta:") -> {
                val partes = recordatorio.claves.single().split(':')
                val estado = if (partes.lastOrNull() == "ok") "confirmada" else "rechazada"
                "reserva_${partes.getOrNull(1).orEmpty()}_resuelta_${estado}_$uid"
            }
            else -> "local_${uid}_$digest"
        }
        val referencia = FirebaseFirestore.getInstance().collection("notificaciones")
            .document(documentoId)
        // Se crea con un `set` directo y NO con una transacción que primero lee el documento: las reglas solo dejan leer
        // notificaciones propias que ya existen, así que leer una que aún no existe es PERMISSION_DENIED y el aviso nunca
        // llegaba a la campanita. Si ya existía (otra revisión lo guardó antes), las reglas rechazan el `set` porque
        // sería una edición: eso significa "ya está en la bandeja", no un error.
        try {
            referencia.set(
                mapOf(
                    "id" to referencia.id,
                    "usuarioId" to uid,
                    "titulo" to recordatorio.titulo,
                    "mensaje" to recordatorio.mensaje,
                    "tipo" to recordatorio.categoria.name,
                    "leida" to false,
                    "createdAt" to System.currentTimeMillis(),
                    "ruta" to recordatorio.ruta.orEmpty()
                )
            ).await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
        }
    }

    private const val TAG = "MotorRecordatorios"
}
