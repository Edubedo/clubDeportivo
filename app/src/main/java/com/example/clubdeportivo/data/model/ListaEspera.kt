package com.example.clubdeportivo.data.model

/**
 * - ESPERANDO: en la fila, todavía no le toca.
 * - AVISADO: se liberó un lugar y se le apartó por un rato (ver Catalogos.MINUTOS_PRIORIDAD_LISTA_ESPERA).
 * - ATENDIDA: ya reservó esa hora.
 * - CANCELADA: salió de la fila.
 */
enum class EstadoEspera { ESPERANDO, AVISADO, ATENDIDA, CANCELADA }

/**
 * Una persona esperando lugar en una hora llena de un área (colección `listaEspera`). Una por persona, área, día y hora:
 * el id es `{areaId}_{fecha}_{hora}_{usuarioId}`. [hora] = 9 significa 09:00 a 10:00.
 */
data class EntradaEspera(
    val id: String,
    val usuarioId: String,
    val usuarioNombre: String,
    val areaId: String,
    val areaNombre: String,
    val deporte: String,
    val fecha: String,
    val hora: Int,
    val personas: Int,
    val estado: EstadoEspera,
    /** Orden de llegada a la fila. */
    val creadoMillis: Long,
    /** Cuándo se le apartó el lugar; null mientras siga esperando. */
    val avisadoMillis: Long? = null
)
