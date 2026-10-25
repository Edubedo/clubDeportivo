package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.Familia
import com.example.clubdeportivo.data.model.MiembroClub
import com.example.clubdeportivo.data.model.PersonaForm
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.clubdeportivo.R
import com.example.clubdeportivo.util.Textos

private fun DocumentSnapshot.aMiembro() = MiembroClub(
    codigo = id,
    nombre = getString("nombre") ?: "",
    telefono = getString("telefono") ?: "",
    correo = getString("correo") ?: "",
    membresiaId = getString("membresiaId") ?: "",
    parentesco = getString("parentesco") ?: MiembroClub.PARENTESCO_TITULAR,
    usuarioId = getString("usuarioId") ?: "",
    cuentaCreada = getBoolean("cuentaCreada") == true
)

class FirebaseFamiliaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FamiliaRepository {

    private val membresias = db.collection("membresias")
    private val miembros = db.collection("miembros")
    private val usuarios = db.collection("usuarios")
    private val registroCodigos = db.collection("registroCodigos")

    override suspend fun obtenerFamilia(usuarioId: String): Familia? {
        val propio = miembros.whereEqualTo("usuarioId", usuarioId).limit(1).get().await().documents.firstOrNull()?.aMiembro()
            ?: return null
        val membresia = membresias.document(propio.membresiaId).get().await().toMembresia() ?: return null
        if (membresia.tipo != TipoMembresia.FAMILIAR) return null

        // Las reglas de seguridad solo dejan ver a todos los integrantes al titular; el resto se ve a sí mismo.
        val integrantes = if (propio.esTitular) {
            miembros.whereEqualTo("membresiaId", membresia.id).get().await().documents.map { it.aMiembro() }
                .sortedWith(compareBy({ !it.esTitular }, { it.nombre.lowercase() }))
        } else {
            listOf(propio)
        }
        return Familia(
            membresia = membresia,
            esTitular = propio.esTitular,
            miembros = integrantes,
            maxPersonas = ReglasMembresia.maxPersonas(membresia.tipo, membresia.paqueteFamiliarId)
        )
    }

    override suspend fun agregarFamiliar(familia: Familia, persona: PersonaForm): MiembroClub {
        require(familia.esTitular) { Textos.get(R.string.only_the_package_holder_can_add) }
        val membresia = familia.membresia
        ReglasMembresia.validarFamiliar(persona, familia.miembros.size, membresia.tipo, membresia.paqueteFamiliarId)
            ?.let { error(it) }

        // Se lee el estado más reciente: el paquete puede haberse suspendido o llenado desde que se abrió la pantalla.
        val documento = membresias.document(membresia.id).get().await()
        val actual = documento.toMembresia() ?: error(Textos.get(R.string.your_membership_no_longer_exists))
        if (ReglasMembresia.estadoEfectivo(actual, Fechas.hoy()) != EstadoMembresia.ACTIVA) {
            error(Textos.get(R.string.your_membership_is_not_active_renew))
        }
        val total = documento.getLong("totalPersonas")?.toInt() ?: familia.miembros.size
        if (total >= familia.maxPersonas) error(Textos.get(R.string.your_package_already_has_all_its_3, familia.maxPersonas))

        val cuenta = CuentasDeAcceso.crear(miembros)
        val hoy = Fechas.hoy()
        val parentesco = persona.parentesco.trim()
        val rol = ReglasMembresia.rolDeAcceso(membresia.tipo).name

        val lote = db.batch()
        lote.set(miembros.document(cuenta.codigo), datosDeMiembro(persona, membresia.id, parentesco, cuenta.uid, hoy, esTitular = false))
        lote.set(usuarios.document(cuenta.uid), datosDeUsuarioMiembro(persona, cuenta.codigo, rol, hoy))
        lote.set(registroCodigos.document(cuenta.codigo), datosDeRegistroCodigo(cuenta.uid))
        lote.update(
            membresias.document(membresia.id),
            mapOf("totalPersonas" to (total + 1).toLong(), "actualizadoEn" to FieldValue.serverTimestamp())
        )
        lote.commit().await()

        return MiembroClub(cuenta.codigo, persona.nombre.trim(), persona.telefono.trim(), persona.correo.trim(), membresia.id, parentesco, cuenta.uid)
    }
}
