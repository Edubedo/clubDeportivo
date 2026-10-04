package com.example.clubdeportivo.data.notificaciones

import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** A quién va dirigida la notificación. */
enum class DestinatarioNotificacion(val etiqueta: String) {
    EMPLEADOS("Empleados"),
    SOCIOS("Socios"),
    TODOS("Todos")
}

data class Notificacion(
    val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val mensaje: String,
    val destinatario: DestinatarioNotificacion,
    val autorId: String? = null,
    val fechaMillis: Long = System.currentTimeMillis()
)

/**
 * Repositorio EN MEMORIA (para que todo funcione desde ya).
 *
 * Cuando tengas tu base de datos (Room, Firebase, API REST...),
 * solo cambia el cuerpo de `publicar()` y de `notificaciones`;
 * el ViewModel y la UI no necesitan cambios.
 */
object NotificacionesRepository {

    private val _notificaciones = MutableStateFlow<List<Notificacion>>(emptyList())
    val notificaciones: StateFlow<List<Notificacion>> = _notificaciones.asStateFlow()

    suspend fun publicar(notificacion: Notificacion): Result<Unit> {
        return try {
            delay(400) // simula la llamada a red/BD
            _notificaciones.update { listOf(notificacion) + it }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lo usarás en las pantallas de Empleado y Socio:
     * recibe solo las notificaciones dirigidas a su rol (o a todos).
     */
    fun paraRol(rol: DestinatarioNotificacion) =
        notificaciones.map { lista ->
            lista.filter {
                it.destinatario == rol || it.destinatario == DestinatarioNotificacion.TODOS
            }
        }
}