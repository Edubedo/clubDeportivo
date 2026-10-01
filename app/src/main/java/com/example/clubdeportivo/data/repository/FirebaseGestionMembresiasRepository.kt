package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.MembresiaDetalle
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.MiembroClub
import com.example.clubdeportivo.data.model.PersonaForm
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private fun DocumentSnapshot.toMiembro(): MiembroClub = MiembroClub(
    codigo = id,
    nombre = getString("nombre") ?: "",
    telefono = getString("telefono") ?: "",
    correo = getString("correo") ?: "",
    membresiaId = getString("membresiaId") ?: "",
    parentesco = getString("parentesco") ?: if (getBoolean("esTitular") == false) "Integrante" else MiembroClub.PARENTESCO_TITULAR,
    usuarioId = getString("usuarioId") ?: ""
)

/**
 * Cada persona con código tiene: un documento en `miembros/{codigo}` (el código ES el id, así no puede repetirse),
 * una cuenta en Firebase Authentication (correo interno derivado del código, contraseña = el código) y su perfil en
 * `usuarios/{uid}`. La membresía (`membresias/{id}`) apunta al titular.
 *
 * Las cuentas se crean con una segunda instancia de Firebase para que crear una no cierre la sesión de quien administra.
 */
class FirebaseGestionMembresiasRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : GestionMembresiasRepository {

    private val membresias = db.collection("membresias")
    private val miembros = db.collection("miembros")
    private val usuarios = db.collection("usuarios")
    private val integrantesAnteriores = db.collection("integrantesFamiliares")

    private data class Cuenta(val codigo: String, val uid: String)

    private fun authSecundaria(): FirebaseAuth {
        val principal = FirebaseApp.getInstance()
        val app = runCatching { FirebaseApp.getInstance(NOMBRE_APP_ALTAS) }
            .getOrElse { FirebaseApp.initializeApp(principal.applicationContext, principal.options, NOMBRE_APP_ALTAS) }
        return FirebaseAuth.getInstance(app)
    }

    /** Genera un código que no exista y crea su cuenta de acceso. */
    private suspend fun crearCuenta(): Cuenta {
        val auth = authSecundaria()
        repeat(MAX_INTENTOS_CODIGO) {
            val codigo = ReglasMembresia.generarCodigo()
            if (miembros.document(codigo).get().await().exists()) return@repeat
            try {
                val uid = auth.createUserWithEmailAndPassword(ReglasMembresia.emailDeCodigo(codigo), codigo).await()
                    .user?.uid ?: error("No se pudo crear la cuenta de acceso.")
                auth.signOut()
                return Cuenta(codigo, uid)
            } catch (e: FirebaseAuthUserCollisionException) {
                // Ya existía una cuenta con ese código: se genera otro.
            }
        }
        error("No se pudo generar un código único, inténtalo de nuevo.")
    }

    override suspend fun obtenerTodas(): List<MembresiaDetalle> {
        val todas = membresias.get().await().documents.mapNotNull { it.toMembresia() }
        val personasPorMembresia = miembros.get().await().documents.map { it.toMiembro() }.groupBy { it.membresiaId }

        return todas.map { membresia ->
            val personas = personasPorMembresia[membresia.id]?.sortedBy { !it.esTitular }
                ?: personasDeRegistroAnterior(membresia)
            MembresiaDetalle(membresia, personas)
        }
    }

    /** Membresías creadas antes de los códigos: se muestran (sin código) con los datos que ya existían. */
    private suspend fun personasDeRegistroAnterior(membresia: Membresia): List<MiembroClub> {
        val titular = usuarios.document(membresia.usuarioId).get().await()
        val lista = mutableListOf(
            MiembroClub(
                codigo = "",
                nombre = titular.getString("nombre") ?: "Sin nombre",
                telefono = titular.getString("telefono") ?: "",
                correo = titular.getString("email") ?: "",
                membresiaId = membresia.id,
                parentesco = MiembroClub.PARENTESCO_TITULAR,
                usuarioId = membresia.usuarioId
            )
        )
        integrantesAnteriores.whereEqualTo("membresiaId", membresia.id).get().await().documents.forEach { doc ->
            lista.add(
                MiembroClub("", doc.getString("nombre") ?: "", "", "", membresia.id, doc.getString("parentesco") ?: "", "")
            )
        }
        return lista
    }

    override suspend fun registrar(
        tipo: TipoMembresia,
        plan: PlanIndividual?,
        paqueteId: Int?,
        precio: Double,
        personas: List<PersonaForm>
    ): MembresiaDetalle {
        val cuentas = personas.map { crearCuenta() }
        val hoy = Fechas.hoy()
        val membresiaId = membresias.document().id
        val rol = ReglasMembresia.rolDeAcceso(tipo)

        val ahora = FieldValue.serverTimestamp()
        val datosMembresia = mutableMapOf<String, Any>(
            "usuarioId" to cuentas.first().uid,
            "titularCodigo" to cuentas.first().codigo,
            "titularNombre" to personas.first().nombre.trim(),
            "totalPersonas" to personas.size.toLong(),
            "tipo" to tipo.name,
            "precio" to precio,
            "estado" to EstadoMembresia.ACTIVA.name,
            "fechaInicio" to hoy,
            "fechaVencimiento" to ReglasMembresia.vencimientoInicial(tipo, hoy),
            "renovaciones" to 0L,
            "creadoPor" to (SesionManager.usuarioActual?.id ?: ""),
            "creadoEn" to ahora,
            "actualizadoEn" to ahora
        )
        plan?.let { datosMembresia["plan"] = it.name }
        paqueteId?.let { datosMembresia["paqueteFamiliarId"] = it.toLong() }

        val batch = db.batch()
        batch.set(membresias.document(membresiaId), datosMembresia)
        val registradas = personas.mapIndexed { indice, persona ->
            val cuenta = cuentas[indice]
            val parentesco = if (indice == 0) MiembroClub.PARENTESCO_TITULAR else persona.parentesco.ifBlank { "Integrante" }
            batch.set(miembros.document(cuenta.codigo), datosPersona(persona, membresiaId, parentesco, cuenta.uid, hoy))
            batch.set(usuarios.document(cuenta.uid), datosUsuarioDeMiembro(persona, cuenta.codigo, rol.name, hoy))
            MiembroClub(cuenta.codigo, persona.nombre.trim(), persona.telefono.trim(), persona.correo.trim(), membresiaId, parentesco, cuenta.uid)
        }
        batch.commit().await()

        val membresia = membresias.document(membresiaId).get().await().toMembresia() ?: error("No se pudo leer la membresía creada.")
        return MembresiaDetalle(membresia, registradas)
    }

    private fun datosPersona(persona: PersonaForm, membresiaId: String, parentesco: String, uid: String, hoy: String) = mapOf(
        "nombre" to persona.nombre.trim(),
        "telefono" to persona.telefono.trim(),
        "correo" to persona.correo.trim(),
        "membresiaId" to membresiaId,
        "parentesco" to parentesco,
        "esTitular" to (parentesco == MiembroClub.PARENTESCO_TITULAR),
        "usuarioId" to uid,
        "fechaRegistro" to hoy,
        "creadoEn" to FieldValue.serverTimestamp()
    )

    /** Perfil de acceso de una persona con código. `email` es el correo interno de la cuenta, no el de contacto (ese vive en `miembros`). */
    private fun datosUsuarioDeMiembro(persona: PersonaForm, codigo: String, rol: String, hoy: String) = mapOf(
        "nombre" to persona.nombre.trim(),
        "email" to ReglasMembresia.emailDeCodigo(codigo),
        "telefono" to persona.telefono.trim(),
        "rol" to rol,
        "estado" to "ACTIVO",
        "codigoMiembro" to codigo,
        "fechaRegistro" to hoy,
        "creadoEn" to FieldValue.serverTimestamp(),
        "actualizadoEn" to FieldValue.serverTimestamp()
    )

    override suspend fun actualizar(
        membresiaId: String,
        plan: PlanIndividual?,
        paqueteId: Int?,
        precio: Double,
        personas: List<PersonaForm>
    ) {
        val actuales = miembros.whereEqualTo("membresiaId", membresiaId).get().await().documents.map { it.toMiembro() }
        val conservadas = personas.mapNotNull { it.codigo }.toSet()
        val tipo = membresias.document(membresiaId).get().await().toMembresia()?.tipo ?: TipoMembresia.INDIVIDUAL
        val rol = ReglasMembresia.rolDeAcceso(tipo)
        val hoy = Fechas.hoy()

        val nuevas = personas.filter { it.codigo == null }.map { it to crearCuenta() }

        val batch = db.batch()
        val cambiosMembresia = mutableMapOf<String, Any>(
            "precio" to precio,
            "totalPersonas" to personas.size.toLong(),
            "titularNombre" to personas.first().nombre.trim(),
            "actualizadoEn" to FieldValue.serverTimestamp()
        )
        plan?.let { cambiosMembresia["plan"] = it.name }
        paqueteId?.let { cambiosMembresia["paqueteFamiliarId"] = it.toLong() }
        batch.update(membresias.document(membresiaId), cambiosMembresia)

        personas.filter { it.codigo != null }.forEach { persona ->
            val actual = actuales.firstOrNull { it.codigo == persona.codigo } ?: return@forEach
            val parentesco = if (actual.esTitular) MiembroClub.PARENTESCO_TITULAR else persona.parentesco.ifBlank { "Integrante" }
            batch.update(
                miembros.document(actual.codigo),
                mapOf(
                    "nombre" to persona.nombre.trim(),
                    "telefono" to persona.telefono.trim(),
                    "correo" to persona.correo.trim(),
                    "parentesco" to parentesco,
                    "esTitular" to actual.esTitular,
                    "actualizadoEn" to FieldValue.serverTimestamp()
                )
            )
            if (actual.usuarioId.isNotBlank()) {
                batch.update(
                    usuarios.document(actual.usuarioId),
                    mapOf(
                        "nombre" to persona.nombre.trim(),
                        "telefono" to persona.telefono.trim(),
                        "actualizadoEn" to FieldValue.serverTimestamp()
                    )
                )
            }
        }
        // Quien se quita de la membresía pierde el acceso: sin su documento de miembro el código ya no entra.
        actuales.filter { it.codigo.isNotBlank() && it.codigo !in conservadas && !it.esTitular }.forEach { quitado ->
            batch.delete(miembros.document(quitado.codigo))
            if (quitado.usuarioId.isNotBlank()) {
                batch.update(
                    usuarios.document(quitado.usuarioId),
                    mapOf("estado" to "INACTIVO", "actualizadoEn" to FieldValue.serverTimestamp())
                )
            }
        }
        nuevas.forEach { (persona, cuenta) ->
            batch.set(miembros.document(cuenta.codigo), datosPersona(persona, membresiaId, persona.parentesco.ifBlank { "Integrante" }, cuenta.uid, hoy))
            batch.set(usuarios.document(cuenta.uid), datosUsuarioDeMiembro(persona, cuenta.codigo, rol.name, hoy))
        }
        batch.commit().await()
    }

    override suspend fun cambiarPlan(membresiaId: String, plan: PlanIndividual?, paqueteId: Int?, precio: Double) {
        val cambios = mutableMapOf<String, Any>("precio" to precio, "actualizadoEn" to FieldValue.serverTimestamp())
        plan?.let { cambios["plan"] = it.name }
        paqueteId?.let { cambios["paqueteFamiliarId"] = it.toLong() }
        membresias.document(membresiaId).update(cambios).await()
    }

    override suspend fun cambiarEstado(membresiaId: String, estado: EstadoMembresia) {
        membresias.document(membresiaId).update(
            mapOf("estado" to estado.name, "actualizadoEn" to FieldValue.serverTimestamp())
        ).await()
    }

    override suspend fun renovar(membresiaId: String, precio: Double) {
        val membresia = membresias.document(membresiaId).get().await().toMembresia() ?: error("La membresía ya no existe.")
        val (inicio, vence) = ReglasMembresia.fechasDeRenovacion(membresia, Fechas.hoy())
        membresias.document(membresiaId).update(
            mapOf(
                "estado" to EstadoMembresia.ACTIVA.name,
                "fechaInicio" to inicio,
                "fechaVencimiento" to vence,
                "precio" to precio,
                "renovaciones" to FieldValue.increment(1),
                "ultimaRenovacion" to Fechas.hoy(),
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private companion object {
        const val NOMBRE_APP_ALTAS = "altaMiembros"
        const val MAX_INTENTOS_CODIGO = 10
    }
}
