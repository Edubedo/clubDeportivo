package com.example.clubdeportivo.data

import com.example.clubdeportivo.data.repository.AreaRepository
import com.example.clubdeportivo.data.repository.AuthRepository
import com.example.clubdeportivo.data.repository.FakeRestriccionHorarioRepository
import com.example.clubdeportivo.data.repository.FirebaseAreaRepository
import com.example.clubdeportivo.data.repository.FirebaseAuthRepository
import com.example.clubdeportivo.data.repository.FirebaseGestionMembresiasRepository
import com.example.clubdeportivo.data.repository.FirebaseMembresiaRepository
import com.example.clubdeportivo.data.repository.FirebasePreciosRepository
import com.example.clubdeportivo.data.repository.GestionMembresiasRepository
import com.example.clubdeportivo.data.repository.PreciosRepository
import com.example.clubdeportivo.data.repository.FirebaseReservaRepository
import com.example.clubdeportivo.data.repository.FirebaseTorneoRepository
import com.example.clubdeportivo.data.repository.MembresiaRepository
import com.example.clubdeportivo.data.repository.ReservaRepository
import com.example.clubdeportivo.data.repository.RestriccionHorarioRepository
import com.example.clubdeportivo.data.repository.TorneoRepository
import com.example.clubdeportivo.data.repository.FirebasePersonalRepository
import com.example.clubdeportivo.data.repository.FirebaseUsuarioRepository
import com.example.clubdeportivo.data.repository.PersonalRepository
import com.example.clubdeportivo.data.repository.UsuarioRepository

/**
 * Punto único donde viven los repositorios mientras el proyecto no tiene
 * inyección de dependencias (Hilt). Cada ViewModel pide aquí su repositorio
 * en vez de crear uno nuevo, para que todas las pantallas vean los mismos
 * datos (ej. una reserva creada en "Reservar" aparece en "Mis reservas").
 *
 * Ya conectado a Firebase (Authentication + Firestore) — ver docs/arquitectura.md.
 * `restriccionHorarioRepository` sigue siendo la versión "Fake": el horario por tipo de
 * área es configuración fija de la app, no un dato que cambie, así que no hace falta
 * guardarlo en la base de datos remota.
 */
object AppContainer {
    val authRepository: AuthRepository = FirebaseAuthRepository()
    val areaRepository: AreaRepository = FirebaseAreaRepository()
    val restriccionHorarioRepository: RestriccionHorarioRepository = FakeRestriccionHorarioRepository()
    val reservaRepository: ReservaRepository = FirebaseReservaRepository()
    val membresiaRepository: MembresiaRepository = FirebaseMembresiaRepository()
    val gestionMembresiasRepository: GestionMembresiasRepository = FirebaseGestionMembresiasRepository()
    val preciosRepository: PreciosRepository = FirebasePreciosRepository()
    val torneoRepository: TorneoRepository = FirebaseTorneoRepository()
    val personalRepository: PersonalRepository = FirebasePersonalRepository()

    val usuarioRepository: UsuarioRepository = FirebaseUsuarioRepository()
}
