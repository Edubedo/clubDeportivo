package com.example.clubdeportivo.data.remote

import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.InscripcionTorneo
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.Reserva
import com.example.clubdeportivo.data.model.Torneo
import com.example.clubdeportivo.data.model.Usuario
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Define los endpoints de la API del backend (Node.js + Express).
 * Esta interfaz NO tiene código: Retrofit genera la implementación real
 * a partir de estas anotaciones (@GET, @POST, etc).
 *
 * Mientras el equipo de backend no entregue la API, las pantallas usan
 * los repositorios "Fake" (ver paquete data.repository) que devuelven
 * datos de prueba. Cuando la API esté lista, se cambia la implementación
 * del repositorio para que use RetrofitClient.apiService en vez del Fake.
 */
interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body credenciales: LoginRequest): Usuario

    @GET("areas")
    suspend fun obtenerAreas(): List<Area>

    @GET("reservas/usuario/{usuarioId}")
    suspend fun obtenerReservasDeUsuario(@Path("usuarioId") usuarioId: Int): List<Reserva>

    @POST("reservas")
    suspend fun crearReserva(@Body reserva: NuevaReservaRequest): Reserva

    @GET("membresias/usuario/{usuarioId}")
    suspend fun obtenerMembresia(@Path("usuarioId") usuarioId: Int): Membresia

    @GET("torneos")
    suspend fun obtenerTorneos(): List<Torneo>

    @POST("torneos/{torneoId}/inscripciones")
    suspend fun inscribirseATorneo(@Path("torneoId") torneoId: Int, @Body usuarioId: Int): InscripcionTorneo
}

data class LoginRequest(val correo: String, val password: String)

data class NuevaReservaRequest(
    val usuarioId: Int,
    val areaId: Int,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String
)
