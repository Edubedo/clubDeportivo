package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.Familia
import com.example.clubdeportivo.data.model.MiembroClub
import com.example.clubdeportivo.data.model.PersonaForm

/** El paquete familiar de quien tiene sesión y el alta de sus familiares (la hace el titular desde la app). */
interface FamiliaRepository {
    /** null si la persona no está en un paquete familiar. */
    suspend fun obtenerFamilia(usuarioId: String): Familia?

    /**
     * Da de alta a un familiar en la membresía del titular: crea su código y su acceso (mismo flujo que el alta del
     * personal) y lo suma al paquete. Solo funciona para el titular, mientras el paquete esté activo y tenga lugar.
     */
    suspend fun agregarFamiliar(familia: Familia, persona: PersonaForm): MiembroClub
}
