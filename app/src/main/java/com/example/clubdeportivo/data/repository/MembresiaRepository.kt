package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.Catalogos
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.IntegranteFamiliar
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.TipoMembresia
import kotlinx.coroutines.delay

interface MembresiaRepository {
    suspend fun obtenerMembresia(usuarioId: String): Membresia?
    suspend fun obtenerIntegrantes(membresiaId: String): List<IntegranteFamiliar>

    /** Todas las membresías del club, para el panel de administración. */
    suspend fun obtenerMembresias(): List<Membresia>

    suspend fun crearMembresia(
        usuarioId: String,
        tipo: TipoMembresia,
        plan: PlanIndividual?,
        paqueteFamiliarId: Int?,
        precio: Double,
        fechaInicio: String,
        fechaVencimiento: String
    ): Membresia

    suspend fun actualizarMembresia(
        id: String,
        tipo: TipoMembresia,
        plan: PlanIndividual?,
        paqueteFamiliarId: Int?,
        precio: Double,
        estado: EstadoMembresia,
        fechaInicio: String,
        fechaVencimiento: String
    ): Membresia

    suspend fun agregarIntegrante(membresiaId: String, nombre: String, parentesco: String): IntegranteFamiliar
    suspend fun eliminarIntegrante(integranteId: String)
}

/**
 * El usuario de prueba "socio" (id 5, ver FakeAuthRepository) tiene una
 * membresía familiar de ejemplo. El resto de los roles no tiene membresía:
 * son personal del club, no socios. El visitante externo no aparece aquí
 * porque su "membresía" es la visita de un solo día (ver Catalogos.PRECIO_VISITA),
 * no un registro recurrente.
 */
class FakeMembresiaRepository : MembresiaRepository {

    private var siguienteMembresiaId = 2
    private var siguienteIntegranteId = 3

    private val membresias = mutableListOf(
        Membresia(
            id = "1",
            usuarioId = "5",
            tipo = TipoMembresia.FAMILIAR,
            paqueteFamiliarId = 1,
            precio = Catalogos.paqueteFamiliarPorId(1)?.precioMensual ?: 0.0,
            estado = EstadoMembresia.ACTIVA,
            fechaInicio = "2026-01-01",
            fechaVencimiento = "2026-12-31"
        )
    )

    private val integrantes = mutableListOf(
        IntegranteFamiliar("1", "1", "María Pérez", "Cónyuge"),
        IntegranteFamiliar("2", "1", "Luis Pérez", "Hijo")
    )

    override suspend fun obtenerMembresia(usuarioId: String): Membresia? {
        delay(400)
        return membresias.find { it.usuarioId == usuarioId }
    }

    override suspend fun obtenerIntegrantes(membresiaId: String): List<IntegranteFamiliar> {
        delay(300)
        return integrantes.filter { it.membresiaId == membresiaId }
    }

    override suspend fun obtenerMembresias(): List<Membresia> {
        delay(400)
        return membresias.toList()
    }

    override suspend fun crearMembresia(
        usuarioId: String,
        tipo: TipoMembresia,
        plan: PlanIndividual?,
        paqueteFamiliarId: Int?,
        precio: Double,
        fechaInicio: String,
        fechaVencimiento: String
    ): Membresia {
        delay(300)
        val membresia = Membresia(
            id = (siguienteMembresiaId++).toString(),
            usuarioId = usuarioId,
            tipo = tipo,
            plan = plan,
            paqueteFamiliarId = paqueteFamiliarId,
            precio = precio,
            estado = EstadoMembresia.ACTIVA,
            fechaInicio = fechaInicio,
            fechaVencimiento = fechaVencimiento
        )
        membresias.add(membresia)
        return membresia
    }

    override suspend fun actualizarMembresia(
        id: String,
        tipo: TipoMembresia,
        plan: PlanIndividual?,
        paqueteFamiliarId: Int?,
        precio: Double,
        estado: EstadoMembresia,
        fechaInicio: String,
        fechaVencimiento: String
    ): Membresia {
        delay(300)
        val indice = membresias.indexOfFirst { it.id == id }
        val actualizada = membresias[indice].copy(
            tipo = tipo,
            plan = plan,
            paqueteFamiliarId = paqueteFamiliarId,
            precio = precio,
            estado = estado,
            fechaInicio = fechaInicio,
            fechaVencimiento = fechaVencimiento
        )
        membresias[indice] = actualizada
        return actualizada
    }

    override suspend fun agregarIntegrante(membresiaId: String, nombre: String, parentesco: String): IntegranteFamiliar {
        delay(300)
        val integrante = IntegranteFamiliar((siguienteIntegranteId++).toString(), membresiaId, nombre, parentesco)
        integrantes.add(integrante)
        return integrante
    }

    override suspend fun eliminarIntegrante(integranteId: String) {
        delay(300)
        integrantes.removeAll { it.id == integranteId }
    }
}
