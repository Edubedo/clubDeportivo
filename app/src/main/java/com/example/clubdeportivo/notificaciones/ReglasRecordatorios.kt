package com.example.clubdeportivo.notificaciones

import com.example.clubdeportivo.data.Deportes
import com.example.clubdeportivo.data.model.ArticuloInventario
import com.example.clubdeportivo.data.model.EntradaEspera
import com.example.clubdeportivo.data.model.EstadoEspera
import com.example.clubdeportivo.util.ReglasListaEspera
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.EstadoReserva
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.notificaciones.Notificacion
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.model.Torneo
import com.example.clubdeportivo.ui.navigation.Destinations
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.Textos
import java.util.Calendar
import java.util.Date
import com.example.clubdeportivo.data.model.etiquetaMotivoSuspension

/**
 * Qué recordar y cuándo. Son funciones puras (reciben los datos ya leídos y devuelven recordatorios) para poder
 * probarlas sin Firebase ni Android. [nuevo] dice si una clave todavía no se avisó: así cada cosa se avisa una sola vez.
 */
object ReglasRecordatorios {
    /**
     * Minutos antes del inicio en que se recuerda una reserva (2 h y 1 h). Cada aviso sale una vez, en cuanto falte
     * eso o menos y todavía no toque el siguiente. Con menos de [MINUTOS_MINIMOS_AVISO] ya no se avisa: llegaría tarde.
     */
    val MINUTOS_ANTES_DE_RESERVA = listOf(120, 60)
    const val MINUTOS_MINIMOS_AVISO = 10

    /** La membresía se recuerda cuando faltan estos días o menos (y de nuevo el día que vence). */
    const val DIAS_ANTES_DE_VENCER = 3

    /** Los resúmenes del administrador miran los vencimientos de esta ventana. */
    const val DIAS_RESUMEN_VENCIMIENTOS = 7

    /** Los resúmenes diarios (stock, vencimientos) no salen antes de esta hora. */
    const val HORA_RESUMEN = 8

    /** No se molesta de [SILENCIO_DESDE] a [SILENCIO_HASTA] (no se marcan como avisados: salen después). */
    const val SILENCIO_DESDE = 22
    const val SILENCIO_HASTA = 5

    /** La asistencia se recuerda desde que la reserva empezó hasta esta cantidad de horas después. */
    private const val HORAS_PARA_ASISTENCIA = 3

    fun hora(ahora: Date): Int = Calendar.getInstance().apply { time = ahora }.get(Calendar.HOUR_OF_DAY)

    fun enSilencio(ahora: Date): Boolean = hora(ahora).let { it >= SILENCIO_DESDE || it < SILENCIO_HASTA }

    /** Días de [hoy] a [fecha] ("yyyy-MM-dd"): 0 = hoy, 1 = mañana, negativo = ya pasó. */
    fun diasHasta(hoy: String, fecha: String): Int {
        val ms = Fechas.combinar(fecha, "12:00").time - Fechas.combinar(hoy, "12:00").time
        return Math.round(ms / 86_400_000.0).toInt()
    }

    private fun nombreDe(r: Reserva) = r.areaNombre.ifBlank { r.deporte }

    // ---------------------------------------------------------------- Miembros y visitantes

    fun reservasProximas(reservas: List<Reserva>, ahora: Date, nuevo: (String) -> Boolean): List<Recordatorio> =
        reservas.filter { it.estado == EstadoReserva.CONFIRMADA }.mapNotNull { reserva ->
            val minutos = Fechas.horasDesdeAhora(reserva.fecha, reserva.horaInicio, ahora) * 60
            if (minutos < MINUTOS_MINIMOS_AVISO) return@mapNotNull null
            // El aviso que toca es el más cercano que ya se alcanzó (con 90 min faltantes, el de 2 h).
            val umbral = MINUTOS_ANTES_DE_RESERVA.filter { minutos <= it }.minOrNull() ?: return@mapNotNull null
            val clave = "reserva:${reserva.id}:$umbral"
            if (!nuevo(clave)) return@mapNotNull null
            Recordatorio(
                clave, CategoriaNotificacion.RESERVA_PROXIMA,
                Textos.get("notif_reserva_proxima_titulo"),
                Textos.get("notif_reserva_proxima_mensaje", nombreDe(reserva), reserva.horaInicio, reserva.horaFin, textoFaltante(Math.round(minutos).toInt())),
                Destinations.RESERVAS
            )
        }

    /** "2 h", "1 h 25 min" o "40 min". */
    fun textoFaltante(minutos: Int): String {
        val horas = minutos / 60
        val resto = minutos % 60
        return when {
            horas == 0 -> Textos.get("tiempo_minutos", resto)
            resto == 0 -> Textos.get("tiempo_horas", horas)
            else -> Textos.get("tiempo_horas_minutos", horas, resto)
        }
    }

    /**
     * Momentos (en milisegundos) en que conviene despertar para dar los avisos de [reserva] que aún no llegan. Se usan
     * para programar alarmas, así el aviso sale a su hora aunque la revisión periódica de Android se atrase.
     */
    fun momentosDeAviso(reserva: Reserva, ahora: Date): List<Long> {
        if (reserva.estado != EstadoReserva.CONFIRMADA) return emptyList()
        val inicio = Fechas.combinar(reserva.fecha, reserva.horaInicio).time
        return MINUTOS_ANTES_DE_RESERVA.map { inicio - it * 60_000L }.filter { it > ahora.time }
    }

    /**
     * Avisa cuando una solicitud que estaba en revisión se aprobó o se rechazó. [estadosPrevios] son los estados que
     * se vieron en la revisión anterior (id -> nombre del estado); una reserva que no estaba ahí no avisa nada.
     */
    fun respuestasDeReserva(reservas: List<Reserva>, estadosPrevios: Map<String, String>): List<Recordatorio> =
        reservas.mapNotNull { reserva ->
            if (estadosPrevios[reserva.id] != EstadoReserva.PENDIENTE_APROBACION.name) return@mapNotNull null
            val cuando = Fechas.legible(reserva.fecha)
            when (reserva.estado) {
                EstadoReserva.CONFIRMADA -> Recordatorio(
                    "respuesta:${reserva.id}:ok", CategoriaNotificacion.RESERVA_RESPUESTA,
                    Textos.get("notif_reserva_aprobada_titulo"),
                    Textos.get("notif_reserva_aprobada_mensaje", nombreDe(reserva), cuando, reserva.horaInicio, reserva.horaFin),
                    Destinations.RESERVAS
                )
                EstadoReserva.RECHAZADA -> Recordatorio(
                    "respuesta:${reserva.id}:no", CategoriaNotificacion.RESERVA_RESPUESTA,
                    Textos.get("notif_reserva_rechazada_titulo"),
                    Textos.get("notif_reserva_rechazada_mensaje", nombreDe(reserva), cuando, reserva.horaInicio),
                    Destinations.RESERVAS
                )
                else -> null
            }
        }

    /**
     * Avisa cuando a la persona le apartaron un lugar en una hora en la que estaba en lista de espera. La clave lleva el
     * momento del aviso: si vuelve a entrar a la fila y le toca otra vez, se avisa de nuevo.
     */
    fun listaEspera(entradas: List<EntradaEspera>, ahora: Date, nuevo: (String) -> Boolean): List<Recordatorio> {
        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        return entradas
            .filter { it.estado == EstadoEspera.AVISADO && ReglasListaEspera.vigente(it, ahora) }
            .mapNotNull { entrada ->
                val clave = "espera:${entrada.id}:${entrada.avisadoMillis}"
                val limite = ReglasListaEspera.limiteMillis(entrada) ?: return@mapNotNull null
                if (!nuevo(clave)) return@mapNotNull null
                Recordatorio(
                    clave, CategoriaNotificacion.LISTA_ESPERA,
                    Textos.get("notif_espera_titulo"),
                    Textos.get(
                        "notif_espera_mensaje",
                        Deportes.titulo(entrada.deporte, entrada.areaNombre), Fechas.legible(entrada.fecha),
                        ReglasListaEspera.textoHora(entrada.hora), formatoHora.format(Date(limite))
                    ),
                    Destinations.RESERVAS
                )
            }
    }

    fun membresia(membresia: Membresia?, hoy: String, nuevo: (String) -> Boolean): List<Recordatorio> {
        if (membresia == null) return emptyList()
        val lista = mutableListOf<Recordatorio>()
        if (membresia.estado == EstadoMembresia.SUSPENDIDA) {
            val clave = "membresia:${membresia.id}:suspendida:${membresia.motivoSuspension}"
            if (nuevo(clave)) {
                lista += Recordatorio(
                    clave, CategoriaNotificacion.MEMBRESIA,
                    Textos.get("notif_membresia_suspendida_titulo"),
                    Textos.get("notif_membresia_suspendida_mensaje", etiquetaMotivoSuspension(membresia.motivoSuspension).ifBlank { Textos.get("not_specified") }),
                    Destinations.MEMBRESIA
                )
            }
            return lista
        }
        val dias = diasHasta(hoy, membresia.fechaVencimiento)
        if (dias == 0) {
            val clave = "membresia:${membresia.id}:hoy:${membresia.fechaVencimiento}"
            if (nuevo(clave)) {
                lista += Recordatorio(
                    clave, CategoriaNotificacion.MEMBRESIA,
                    Textos.get("notif_membresia_hoy_titulo"), Textos.get("notif_membresia_hoy_mensaje"), Destinations.MEMBRESIA
                )
            }
        } else if (dias in 1..DIAS_ANTES_DE_VENCER) {
            val clave = "membresia:${membresia.id}:pronto:${membresia.fechaVencimiento}"
            if (nuevo(clave)) {
                lista += Recordatorio(
                    clave, CategoriaNotificacion.MEMBRESIA,
                    Textos.get("notif_membresia_pronto_titulo"),
                    if (dias == 1) Textos.get("notif_membresia_manana_mensaje")
                    else Textos.get("notif_membresia_pronto_mensaje", dias, Fechas.legible(membresia.fechaVencimiento)),
                    Destinations.MEMBRESIA
                )
            }
        }
        return lista
    }

    // ---------------------------------------------------------------- Todos

    /** Torneos que empiezan mañana. [comoCliente] = el torneo se muestra como "estás inscrito" en vez de como gestión. */
    fun torneosDeManana(torneos: List<Torneo>, hoy: String, comoCliente: Boolean, nuevo: (String) -> Boolean): List<Recordatorio> =
        torneos.filter { diasHasta(hoy, it.fechaInicio) == 1 }.mapNotNull { torneo ->
            val clave = "torneo:${torneo.id}:${torneo.fechaInicio}:${if (comoCliente) "c" else "p"}"
            if (!nuevo(clave)) return@mapNotNull null
            Recordatorio(
                clave, CategoriaNotificacion.TORNEO,
                Textos.get("notif_torneo_titulo", torneo.nombre),
                if (comoCliente) {
                    if (torneo.horaInicio.isNotBlank()) Textos.get("notif_torneo_cliente_hora", torneo.horaInicio)
                    else Textos.get("notif_torneo_cliente")
                } else {
                    Textos.get("notif_torneo_personal", torneo.inscritos, torneo.cupoMaximo)
                },
                Destinations.RESERVAS
            )
        }

    /** Avisos del club publicados después de [desdeMillis] por alguien distinto de [miId]. */
    fun avisos(avisos: List<Notificacion>, desdeMillis: Long, miId: String, nuevo: (String) -> Boolean): List<Recordatorio> =
        avisos.filter { it.fechaMillis > desdeMillis && it.autorId != miId }
            .sortedBy { it.fechaMillis }
            .mapNotNull { aviso ->
                val clave = "aviso:${aviso.id}"
                if (!nuevo(clave)) return@mapNotNull null
                Recordatorio(
                    clave, CategoriaNotificacion.AVISOS,
                    Textos.get("notif_aviso_titulo"), aviso.titulo.ifBlank { aviso.mensaje.take(120) }
                )
            }

    // ---------------------------------------------------------------- Encargados y administradores

    /** Una sola notificación que resume las solicitudes en revisión que todavía no se avisaron. */
    fun solicitudes(pendientes: List<Reserva>, hoy: String, nuevo: (String) -> Boolean): Recordatorio? {
        val sinAvisar = pendientes
            .filter { it.estado == EstadoReserva.PENDIENTE_APROBACION && it.fecha >= hoy }
            .filter { nuevo("solicitud:${it.id}") }
        if (sinAvisar.isEmpty()) return null
        return Recordatorio(
            sinAvisar.map { "solicitud:${it.id}" }, CategoriaNotificacion.SOLICITUDES,
            Textos.get("notif_solicitudes_titulo"),
            if (sinAvisar.size == 1) Textos.get("notif_solicitudes_una", sinAvisar.first().usuarioNombre.ifBlank { Textos.get("user") })
            else Textos.get("notif_solicitudes_varias", sinAvisar.size),
            Destinations.RESERVAS
        )
    }

    /** Reservas confirmadas que ya empezaron (hace menos de unas horas) y a las que nadie les registró asistencia. */
    fun asistencias(confirmadas: List<Reserva>, conAsistencia: Set<String>, ahora: Date, nuevo: (String) -> Boolean): Recordatorio? {
        val pendientes = confirmadas.filter { reserva ->
            val horas = Fechas.horasDesdeAhora(reserva.fecha, reserva.horaInicio, ahora)
            reserva.estado == EstadoReserva.CONFIRMADA && horas <= 0 && horas > -HORAS_PARA_ASISTENCIA &&
                reserva.id !in conAsistencia && nuevo("asistencia:${reserva.id}")
        }
        if (pendientes.isEmpty()) return null
        return Recordatorio(
            pendientes.map { "asistencia:${it.id}" }, CategoriaNotificacion.ASISTENCIA,
            Textos.get("notif_asistencia_titulo"),
            if (pendientes.size == 1) Textos.get("notif_asistencia_una", pendientes.first().usuarioNombre.ifBlank { Textos.get("user") })
            else Textos.get("notif_asistencia_varias", pendientes.size),
            Destinations.RESERVAS
        )
    }

    /** Resumen diario de artículos en o por debajo de su mínimo (solo los que tienen un mínimo definido). */
    fun stockBajo(articulos: List<ArticuloInventario>, hoy: String, ahora: Date, nuevo: (String) -> Boolean): Recordatorio? {
        if (hora(ahora) < HORA_RESUMEN) return null
        val bajos = articulos.filter { it.stockMinimo > 0 && it.cantidad <= it.stockMinimo }
        val clave = "stock:$hoy"
        if (bajos.isEmpty() || !nuevo(clave)) return null
        val nombres = bajos.take(3).joinToString(", ") { it.nombre } + if (bajos.size > 3) "…" else ""
        return Recordatorio(
            clave, CategoriaNotificacion.STOCK,
            Textos.get("notif_stock_titulo"),
            if (bajos.size == 1) Textos.get("notif_stock_uno", nombres) else Textos.get("notif_stock_varios", bajos.size, nombres),
            Destinations.INVENTARIO
        )
    }

    /** Resumen diario del administrador: membresías activas que vencen hoy o en los próximos días. */
    fun vencimientos(membresias: List<Membresia>, hoy: String, ahora: Date, nuevo: (String) -> Boolean): Recordatorio? {
        if (hora(ahora) < HORA_RESUMEN) return null
        val porVencer = membresias.filter {
            it.estado == EstadoMembresia.ACTIVA && diasHasta(hoy, it.fechaVencimiento) in 0..DIAS_RESUMEN_VENCIMIENTOS
        }
        val clave = "vencimientos:$hoy"
        if (porVencer.isEmpty() || !nuevo(clave)) return null
        val hoyVencen = porVencer.count { diasHasta(hoy, it.fechaVencimiento) == 0 }
        return Recordatorio(
            clave, CategoriaNotificacion.VENCIMIENTOS,
            Textos.get("notif_vencimientos_titulo"),
            if (hoyVencen > 0) Textos.get("notif_vencimientos_con_hoy", porVencer.size, hoyVencen)
            else Textos.get("notif_vencimientos_mensaje", porVencer.size),
            Destinations.MEMBRESIA
        )
    }
}
