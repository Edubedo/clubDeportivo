package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.ConceptoPago
import com.example.clubdeportivo.data.model.EstadoMembresia
import com.example.clubdeportivo.data.model.MetodoPago
import com.example.clubdeportivo.data.model.MembresiaDetalle
import com.example.clubdeportivo.data.model.Membresia
import com.example.clubdeportivo.data.model.MiembroClub
import com.example.clubdeportivo.data.model.PersonaForm
import com.example.clubdeportivo.data.model.PlanIndividual
import com.example.clubdeportivo.data.model.TipoMembresia
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.ReglasMembresia
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.clubdeportivo.util.Textos

private fun DocumentSnapshot.toMiembro(): MiembroClub = MiembroClub(
    codigo = id,
    nombre = getString("nombre") ?: "",
    telefono = getString("telefono") ?: "",
    correo = getString("correo") ?: "",
    membresiaId = getString("membresiaId") ?: "",
    parentesco = getString("parentesco") ?: if (getBoolean("esTitular") == false) "Integrante" else MiembroClub.PARENTESCO_TITULAR,
    usuarioId = getString("usuarioId") ?: "",
    cuentaCreada = getBoolean("cuentaCreada") == true
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
    private val registroCodigos = db.collection("registroCodigos")
    private val pagos = db.collection("pagos")

    private suspend fun crearCuenta(): CuentaDeAcceso = CuentasDeAcceso.crear(miembros)

    private fun authSecundaria(): FirebaseAuth = CuentasDeAcceso.auth()

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
                nombre = titular.getString("nombre") ?: Textos.get("no_name"),
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
        personas: List<PersonaForm>,
        metodoPago: MetodoPago
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
            batch.set(registroCodigos.document(cuenta.codigo), datosRegistro(cuenta.uid))
            MiembroClub(cuenta.codigo, persona.nombre.trim(), persona.telefono.trim(), persona.correo.trim(), membresiaId, parentesco, cuenta.uid)
        }
        // El cobro queda registrado en la misma operación que el alta: si una falla, fallan las dos.
        batch.set(
            pagos.document(),
            datosPago(
                membresiaId = membresiaId,
                uid = cuentas.first().uid,
                titular = personas.first().nombre.trim(),
                concepto = if (tipo == TipoMembresia.VISITA) ConceptoPago.VISITA else ConceptoPago.ALTA,
                monto = precio,
                metodo = metodoPago,
                hoy = hoy
            )
        )
        batch.commit().await()

        val membresia = membresias.document(membresiaId).get().await().toMembresia() ?: error(Textos.get("the_created_membership_could_not_be"))
        return MembresiaDetalle(membresia, registradas)
    }

    private fun datosRegistro(uidInterno: String) = datosDeRegistroCodigo(uidInterno)

    private fun datosPago(
        membresiaId: String,
        uid: String,
        titular: String,
        concepto: ConceptoPago,
        monto: Double,
        metodo: MetodoPago,
        hoy: String
    ) = mapOf(
        "usuarioId" to uid,
        "membresiaId" to membresiaId,
        "titularNombre" to titular,
        "concepto" to concepto.name,
        "monto" to monto,
        "moneda" to "MXN",
        "fechaPago" to hoy,
        "metodoPago" to metodo.name,
        "estado" to "PAGADO",
        "registradoPor" to (SesionManager.usuarioActual?.id ?: ""),
        "creadoEn" to FieldValue.serverTimestamp()
    )

    private fun datosPersona(persona: PersonaForm, membresiaId: String, parentesco: String, uid: String, hoy: String) =
        datosDeMiembro(persona, membresiaId, parentesco, uid, hoy, esTitular = parentesco == MiembroClub.PARENTESCO_TITULAR)

    private fun datosUsuarioDeMiembro(persona: PersonaForm, codigo: String, rol: String, hoy: String) =
        datosDeUsuarioMiembro(persona, codigo, rol, hoy)

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
            batch.delete(registroCodigos.document(quitado.codigo))
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
            batch.set(registroCodigos.document(cuenta.codigo), datosRegistro(cuenta.uid))
        }
        batch.commit().await()
    }

    override suspend fun cambiarPlan(membresiaId: String, plan: PlanIndividual?, paqueteId: Int?, precio: Double) {
        val cambios = mutableMapOf<String, Any>("precio" to precio, "actualizadoEn" to FieldValue.serverTimestamp())
        plan?.let { cambios["plan"] = it.name }
        paqueteId?.let { cambios["paqueteFamiliarId"] = it.toLong() }
        membresias.document(membresiaId).update(cambios).await()
    }

    override suspend fun cambiarEstado(membresiaId: String, estado: EstadoMembresia, motivo: String?) {
        val cambios = mutableMapOf<String, Any>("estado" to estado.name, "actualizadoEn" to FieldValue.serverTimestamp())
        if (estado == EstadoMembresia.SUSPENDIDA) {
            require(!motivo.isNullOrBlank()) { Textos.get("say_why_the_membership_is_being") }
            cambios["motivoSuspension"] = motivo
            cambios["suspendidaEn"] = Fechas.hoy()
            cambios["suspendidaPor"] = SesionManager.usuarioActual?.id ?: ""
        } else {
            cambios["motivoSuspension"] = FieldValue.delete()
            cambios["suspendidaEn"] = FieldValue.delete()
            cambios["suspendidaPor"] = FieldValue.delete()
        }
        val batch = db.batch()
        batch.update(membresias.document(membresiaId), cambios)
        // Al reactivar, quien había quedado fuera por una baja recupera su acceso.
        if (estado == EstadoMembresia.ACTIVA) cuentasDeLaMembresia(membresiaId).forEach { uid -> batch.update(usuarios.document(uid), cambiosDeAcceso("ACTIVO")) }
        batch.commit().await()
    }

    override suspend fun darDeBaja(membresiaId: String, motivo: String) {
        require(motivo.isNotBlank()) { Textos.get("say_why_the_member_is_being") }
        val batch = db.batch()
        batch.update(
            membresias.document(membresiaId),
            mapOf(
                "estado" to EstadoMembresia.SUSPENDIDA.name,
                "motivoSuspension" to motivo,
                "suspendidaEn" to Fechas.hoy(),
                "suspendidaPor" to (SesionManager.usuarioActual?.id ?: ""),
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        )
        cuentasDeLaMembresia(membresiaId).forEach { uid -> batch.update(usuarios.document(uid), cambiosDeAcceso("INACTIVO")) }
        batch.commit().await()
    }

    private fun cambiosDeAcceso(estado: String) = mapOf("estado" to estado, "actualizadoEn" to FieldValue.serverTimestamp())

    /** Ids de las cuentas (`usuarios/{uid}`) de todas las personas de la membresía, incluida la del titular de un registro anterior a los códigos. */
    private suspend fun cuentasDeLaMembresia(membresiaId: String): Set<String> {
        val deMiembros = miembros.whereEqualTo("membresiaId", membresiaId).get().await().documents
            .mapNotNull { it.getString("usuarioId")?.takeIf { uid -> uid.isNotBlank() } }
        val titular = membresias.document(membresiaId).get().await().getString("usuarioId")?.takeIf { it.isNotBlank() }
        return (deMiembros + listOfNotNull(titular)).toSet()
    }

    override suspend fun renovar(membresiaId: String, precio: Double, metodoPago: MetodoPago) {
        val documento = membresias.document(membresiaId).get().await()
        val membresia = documento.toMembresia() ?: error(Textos.get("the_membership_no_longer_exists"))
        val hoy = Fechas.hoy()
        val (inicio, vence) = ReglasMembresia.fechasDeRenovacion(membresia, hoy)
        val batch = db.batch()
        batch.update(
            membresias.document(membresiaId),
            mapOf(
                "estado" to EstadoMembresia.ACTIVA.name,
                "fechaInicio" to inicio,
                "fechaVencimiento" to vence,
                "precio" to precio,
                "renovaciones" to FieldValue.increment(1),
                "ultimaRenovacion" to hoy,
                "motivoSuspension" to FieldValue.delete(),
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        )
        // Renovar una membresía dada de baja también le devuelve el acceso a sus personas.
        cuentasDeLaMembresia(membresiaId).forEach { uid -> batch.update(usuarios.document(uid), cambiosDeAcceso("ACTIVO")) }
        batch.set(
            pagos.document(),
            datosPago(
                membresiaId = membresiaId,
                uid = membresia.usuarioId,
                titular = documento.getString("titularNombre") ?: "",
                concepto = ConceptoPago.MENSUALIDAD,
                monto = precio,
                metodo = metodoPago,
                hoy = hoy
            )
        )
        batch.commit().await()
    }

    override suspend fun restablecerAcceso(codigo: String) {
        val miembro = miembros.document(codigo).get().await()
        if (!miembro.exists()) error(Textos.get("that_member_no_longer_exists"))
        if (miembro.getBoolean("cuentaCreada") != true) error(Textos.get("this_member_hasn_t_signed_up"))

        val membresiaId = miembro.getString("membresiaId").orEmpty()
        val tipo = membresias.document(membresiaId).get().await().toMembresia()?.tipo ?: TipoMembresia.INDIVIDUAL
        val cuentaAnterior = miembro.getString("usuarioId").orEmpty()
        val persona = PersonaForm(
            codigo = codigo,
            nombre = miembro.getString("nombre").orEmpty(),
            telefono = miembro.getString("telefono").orEmpty(),
            correo = miembro.getString("correo").orEmpty(),
            parentesco = miembro.getString("parentesco").orEmpty()
        )

        // Cuenta interna nueva: el correo interno quedó libre cuando la persona se registró.
        val auth = authSecundaria()
        val uidNuevo = try {
            auth.createUserWithEmailAndPassword(ReglasMembresia.emailDeCodigo(codigo), codigo).await().user?.uid
                ?: error(Textos.get("the_code_could_not_be_prepared"))
        } catch (e: FirebaseAuthUserCollisionException) {
            error(Textos.get("this_code_still_has_its_original"))
        }
        auth.signOut()

        val batch = db.batch()
        batch.update(
            miembros.document(codigo),
            mapOf(
                "usuarioId" to uidNuevo,
                "cuentaCreada" to false,
                "correoCuenta" to FieldValue.delete(),
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            registroCodigos.document(codigo),
            mapOf("usuarioId" to uidNuevo, "cuentaCreada" to false, "creadoEn" to FieldValue.serverTimestamp())
        )
        batch.set(usuarios.document(uidNuevo), datosUsuarioDeMiembro(persona, codigo, ReglasMembresia.rolDeAcceso(tipo).name, Fechas.hoy()))
        if (cuentaAnterior.isNotBlank()) {
            batch.update(
                usuarios.document(cuentaAnterior),
                mapOf("estado" to "INACTIVO", "actualizadoEn" to FieldValue.serverTimestamp())
            )
        }
        batch.commit().await()
    }

}
