package com.example.clubdeportivo.data

import com.example.clubdeportivo.data.repository.AreaRepository
import com.example.clubdeportivo.data.repository.AuthRepository
import com.example.clubdeportivo.data.repository.FakeAreaRepository
import com.example.clubdeportivo.data.repository.FakeAuthRepository
import com.example.clubdeportivo.data.repository.FakeMembresiaRepository
import com.example.clubdeportivo.data.repository.FakeReservaRepository
import com.example.clubdeportivo.data.repository.FakeRestriccionHorarioRepository
import com.example.clubdeportivo.data.repository.FakeTorneoRepository
import com.example.clubdeportivo.data.repository.MembresiaRepository
import com.example.clubdeportivo.data.repository.ReservaRepository
import com.example.clubdeportivo.data.repository.RestriccionHorarioRepository
import com.example.clubdeportivo.data.repository.TorneoRepository

/**
 * Punto único donde viven los repositorios mientras el proyecto no tiene
 * inyección de dependencias (Hilt). Cada ViewModel pide aquí su repositorio
 * en vez de crear uno nuevo, para que todas las pantallas vean los mismos
 * datos (ej. una reserva creada en "Reservar" aparece en "Mis reservas").
 *
 * Cuando el backend real esté listo, aquí es donde se cambia
 * Fake...Repository() por la implementación que use RetrofitClient.
 */
object AppContainer {
    val authRepository: AuthRepository = FakeAuthRepository()
    val areaRepository: AreaRepository = FakeAreaRepository()
    val restriccionHorarioRepository: RestriccionHorarioRepository = FakeRestriccionHorarioRepository()
    val reservaRepository: ReservaRepository = FakeReservaRepository()
    val membresiaRepository: MembresiaRepository = FakeMembresiaRepository()
    val torneoRepository: TorneoRepository = FakeTorneoRepository()
}
