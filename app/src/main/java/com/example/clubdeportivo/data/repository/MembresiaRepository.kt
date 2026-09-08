package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.IntegranteFamiliar
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.TipoMembresia
import kotlinx.coroutines.delay

interface MembresiaRepository {
    suspend fun obtenerMembresia(usuarioId: Int): Membresia?
    suspend fun obtenerIntegrantes(membresiaId: Int): List<IntegranteFamiliar>
}

/**
 * El usuario de prueba "socio" (id 5, ver FakeAuthRepository) tiene una
 * membresía familiar de ejemplo. El resto de los roles no tiene membresía:
 * son personal del club, no socios. El visitante externo no aparece aquí
 * porque su "membresía" es la visita de un solo día (ver Catalogos.PRECIO_VISITA),
 * no un registro recurrente.
 */
class FakeMembresiaRepository : MembresiaRepository {

    private val membresias = mapOf(
        5 to Membresia(
            id = 1,
            usuarioId = 5,
            tipo = TipoMembresia.FAMILIAR,
            paqueteFamiliarId = 1,
            precio = Catalogos.paqueteFamiliarPorId(1)?.precioMensual ?: 0.0,
            estado = EstadoMembresia.ACTIVA,
            fechaInicio = "2026-01-01",
            fechaVencimiento = "2026-12-31"
        )
    )

    private val integrantes = mapOf(
        1 to listOf(
            IntegranteFamiliar(1, 1, "María Pérez", "Cónyuge"),
            IntegranteFamiliar(2, 1, "Luis Pérez", "Hijo")
        )
    )

    override suspend fun obtenerMembresia(usuarioId: Int): Membresia? {
        delay(400)
        return membresias[usuarioId]
    }

    override suspend fun obtenerIntegrantes(membresiaId: Int): List<IntegranteFamiliar> {
        delay(300)
        return integrantes[membresiaId] ?: emptyList()
    }
}
