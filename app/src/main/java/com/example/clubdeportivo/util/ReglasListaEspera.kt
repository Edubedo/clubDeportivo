package com.example.clubdeportivo.util

import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.model.EntradaEspera
import com.example.clubdeportivo.data.model.EstadoEspera
import java.util.Date

/**
 * Reglas de la lista de espera de una hora llena (sin Firebase ni Android, para poder probarlas):
 *
 *  - La fila se atiende en orden de llegada. Cuando hay lugares libres se apartan para los primeros de la fila, en orden,
 *    mientras alcancen (si al primero no le alcanza, nadie se le adelanta).
 *  - A quien se le aparta un lugar (AVISADO) lo conserva [Catalogos.MINUTOS_PRIORIDAD_LISTA_ESPERA] minutos; después deja
 *    de contar y el lugar pasa al siguiente.
 *  - Mientras haya gente esperando, los lugares que se liberan no los puede tomar alguien de más atrás ni alguien de fuera.
 *  - Una entrada deja de contar cuando esa hora ya empezó.
 */
object ReglasListaEspera {

    private const val MS_POR_MINUTO = 60_000L

    fun idDe(areaId: String, fecha: String, hora: Int, usuarioId: String) = "${areaId}_${fecha}_${hora}_$usuarioId"

    fun textoHora(hora: Int) = "%02d:00".format(hora)

    /** Hasta cuándo se le guarda el lugar a quien ya fue avisado; null si todavía no le toca. */
    fun limiteMillis(entrada: EntradaEspera): Long? =
        entrada.avisadoMillis?.let { it + Catalogos.MINUTOS_PRIORIDAD_LISTA_ESPERA * MS_POR_MINUTO }

    /** true si la entrada sigue en la fila: esperando, o avisada y todavía dentro de su tiempo, y la hora no ha empezado. */
    fun vigente(entrada: EntradaEspera, ahora: Date): Boolean {
        if (Fechas.horasDesdeAhora(entrada.fecha, textoHora(entrada.hora), ahora) <= 0) return false
        return when (entrada.estado) {
            EstadoEspera.ESPERANDO -> true
            EstadoEspera.AVISADO -> (limiteMillis(entrada) ?: 0L) > ahora.time
            EstadoEspera.ATENDIDA, EstadoEspera.CANCELADA -> false
        }
    }

    /** Fila vigente de una hora, en orden de llegada. */
    fun fila(entradas: List<EntradaEspera>, areaId: String, fecha: String, hora: Int, ahora: Date): List<EntradaEspera> =
        entradas
            .filter { it.areaId == areaId && it.fecha == fecha && it.hora == hora && vigente(it, ahora) }
            .sortedWith(compareBy({ it.creadoMillis }, { it.id }))

    /** Lugar (1, 2, 3...) de [usuarioId] en la [fila]; null si no está. */
    fun posicion(usuarioId: String, fila: List<EntradaEspera>): Int? =
        fila.indexOfFirst { it.usuarioId == usuarioId }.takeIf { it >= 0 }?.plus(1)

    /**
     * Cuántos de los [libres] lugares de la hora están apartados para gente que va antes que [usuarioId] en la fila
     * (si no está en la fila, todos van antes). Lo que quede, lo puede tomar.
     */
    fun apartadosPara(usuarioId: String, fila: List<EntradaEspera>, libres: Int): Int {
        val propia = fila.indexOfFirst { it.usuarioId == usuarioId }
        val antes = if (propia >= 0) fila.take(propia) else fila
        return antes.sumOf { it.personas }.coerceAtMost(libres.coerceAtLeast(0))
    }

    /** Entradas que todavía esperan y a las que ya les alcanzan los [libres] lugares: hay que avisarles. */
    fun porAvisar(fila: List<EntradaEspera>, libres: Int): List<EntradaEspera> {
        var restantes = libres
        val avisar = mutableListOf<EntradaEspera>()
        for (entrada in fila) {
            if (entrada.personas > restantes) break
            restantes -= entrada.personas
            if (entrada.estado == EstadoEspera.ESPERANDO) avisar += entrada
        }
        return avisar
    }
}
