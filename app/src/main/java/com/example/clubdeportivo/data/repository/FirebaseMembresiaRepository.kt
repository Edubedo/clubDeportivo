package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.IntegranteFamiliar
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

internal fun DocumentSnapshot.toMembresia(): Membresia? {
    val usuarioId = getString("usuarioId") ?: return null
    return Membresia(
        id = id,
        usuarioId = usuarioId,
        tipo = TipoMembresia.valueOf(getString("tipo") ?: "INDIVIDUAL"),
        plan = getString("plan")?.let { PlanIndividual.valueOf(it) },
        paqueteFamiliarId = getLong("paqueteFamiliarId")?.toInt(),
        precio = getDouble("precio") ?: 0.0,
        estado = EstadoMembresia.valueOf(getString("estado") ?: "ACTIVA"),
        fechaInicio = getString("fechaInicio") ?: "",
        fechaVencimiento = getString("fechaVencimiento") ?: "",
        comprobanteUrl = getString("comprobanteUrl"),
        motivoSuspension = getString("motivoSuspension") ?: ""
    )
}

/**
 * Lee la membresía de una persona. Las membresías se dan de alta y se administran desde la
 * sección Membresías (ver FirebaseGestionMembresiasRepository).
 */
class FirebaseMembresiaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : MembresiaRepository {

    private val membresias = db.collection("membresias")
    private val integrantesFamiliares = db.collection("integrantesFamiliares")
    private val miembros = db.collection("miembros")
    private val usuarios = db.collection("usuarios")

    override suspend fun obtenerMembresia(usuarioId: String): Membresia? {
        val propias = membresias.whereEqualTo("usuarioId", usuarioId).get().await()
            .documents.mapNotNull { it.toMembresia() }

        // Un integrante de un paquete familiar tiene su propia cuenta y código, pero la membresía es del titular.
        val idsDeFamilia = miembros.whereEqualTo("usuarioId", usuarioId).get().await()
            .documents.mapNotNull { it.getString("membresiaId") }
        val codigoMiembro = usuarios.document(usuarioId).get().await().getString("codigoMiembro")
        val idPorCodigo = codigoMiembro
            ?.takeIf { it.isNotBlank() }
            ?.let { miembros.document(it).get().await().getString("membresiaId") }
        val deFamilia = (idsDeFamilia + listOfNotNull(idPorCodigo)).distinct().mapNotNull { id ->
            membresias.document(id).get().await().toMembresia()
        }

        return seleccionarMembresia((propias + deFamilia).distinctBy { it.id }, Fechas.hoy())
    }

    override fun observarMembresia(usuarioId: String): Flow<Membresia?> = callbackFlow {
        var propias = emptyList<Membresia>()
        var idsDeFamiliaConsulta = emptySet<String>()
        var idDeFamiliaPorCodigo: String? = null
        val membrecas = mutableMapOf<String, Membresia?>()
        val listeners = mutableMapOf<String, ListenerRegistration>()
        val idsEscuchados = mutableSetOf<String>()
        var propiasListas = false
        var miembrosListos = false
        var perfilListo = false
        var codigoMiembro: String? = null
        var miembroPorCodigo: ListenerRegistration? = null
        var membresiasListas = false

        fun idsDeFamilia() = idsDeFamiliaConsulta + listOfNotNull(idDeFamiliaPorCodigo)

        fun emitirSiLista() {
            if (propiasListas && miembrosListos && perfilListo && membresiasListas) {
                val ids = (propias.map { it.id } + idsDeFamilia()).distinct()
                trySend(seleccionarMembresia(ids.mapNotNull(membrecas::get), Fechas.hoy()))
            }
        }

        fun escucharMembresias() {
            val ids = (propias.map { it.id } + idsDeFamilia()).toSet()
            (listeners.keys - ids).forEach { id ->
                listeners.remove(id)?.remove()
                membrecas.remove(id)
                idsEscuchados.remove(id)
            }
            val nuevos = ids - listeners.keys
            if (nuevos.isEmpty()) {
                membresiasListas = idsEscuchados.containsAll(ids)
                emitirSiLista()
                return
            }
            membresiasListas = false
            nuevos.forEach { id ->
                listeners[id] = membresias.document(id).addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                    } else if (snapshot != null) {
                        membrecas[id] = snapshot.toMembresia()
                        idsEscuchados += id
                        val idsActuales = (propias.map { it.id } + idsDeFamilia()).toSet()
                        membresiasListas = idsEscuchados.containsAll(idsActuales)
                        emitirSiLista()
                    }
                }
            }
        }

        val perfilListener = usuarios.document(usuarioId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else if (snapshot != null) {
                val codigoNuevo = snapshot.getString("codigoMiembro")?.takeIf { it.isNotBlank() }
                if (codigoNuevo != codigoMiembro) {
                    miembroPorCodigo?.remove()
                    codigoMiembro = codigoNuevo
                    idDeFamiliaPorCodigo = null
                    miembroPorCodigo = codigoNuevo?.let { codigo ->
                        miembros.document(codigo).addSnapshotListener { miembro, miembroError ->
                            if (miembroError != null) {
                                close(miembroError)
                            } else if (miembro != null) {
                                idDeFamiliaPorCodigo = miembro.getString("membresiaId")
                                escucharMembresias()
                            }
                        }
                    }
                }
                perfilListo = true
                emitirSiLista()
            }
        }

        val propiaListener = membresias.whereEqualTo("usuarioId", usuarioId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else if (snapshot != null) {
                    propias = snapshot.documents.mapNotNull { it.toMembresia() }
                    propiasListas = true
                    escucharMembresias()
                }
            }

        val miembrosListener = miembros.whereEqualTo("usuarioId", usuarioId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else if (snapshot != null) {
                    idsDeFamiliaConsulta = snapshot.documents.mapNotNull { it.getString("membresiaId") }.toSet()
                    miembrosListos = true
                    escucharMembresias()
                }
            }

        awaitClose {
            perfilListener.remove()
            propiaListener.remove()
            miembrosListener.remove()
            miembroPorCodigo?.remove()
            listeners.values.forEach(ListenerRegistration::remove)
        }
    }

    override suspend fun obtenerIntegrantes(membresiaId: String): List<IntegranteFamiliar> {
        return integrantesFamiliares.whereEqualTo("membresiaId", membresiaId).get().await()
            .documents.map { doc ->
                IntegranteFamiliar(
                    id = doc.id,
                    membresiaId = membresiaId,
                    nombre = doc.getString("nombre") ?: "",
                    parentesco = doc.getString("parentesco") ?: ""
                )
            }
    }
}

internal fun seleccionarMembresia(membresias: List<Membresia>, hoy: String): Membresia? =
    membresias.maxWithOrNull(
        compareBy<Membresia> {
            when (ReglasMembresia.estadoEfectivo(it, hoy)) {
                EstadoMembresia.ACTIVA -> 2
                EstadoMembresia.VENCIDA -> 1
                EstadoMembresia.SUSPENDIDA -> 0
            }
        }.thenBy { it.fechaInicio }
            .thenBy { it.fechaVencimiento }
    )
