package com.example.clubdeportivo.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Lleva los documentos que ya existen en Firestore a la estructura actual descrita en
 * `docs/diccionario-de-datos.txt`.
 *
 * Reglas de seguridad de la migración (la base la comparte todo el equipo):
 *  - Solo AGREGA campos que faltan. Nunca sobrescribe un valor existente ni borra campos heredados
 *    (`correo`, `codigo`, `tipoTurno`, `leido`...): siguen ahí, marcados como obsoletos en el diccionario.
 *  - Es idempotente: correrla dos veces deja lo mismo que una.
 *  - Queda registrada en `config/esquema` (versión y fecha) y no vuelve a correr hasta subir [VERSION].
 *  - Las únicas eliminaciones son los horarios de áreas que ya no existen (no tienen ningún uso), el campo
 *    `id` que algunos documentos de `usuarios` guardaban dentro y, desde la versión 4, la colección `empleados`:
 *    sus datos laborales (tipoPersonal, turno, areaTrabajo) se copian primero al documento de `usuarios` de cada
 *    persona y solo entonces se borran los documentos de `empleados`.
 */
object MigracionEsquema {

    const val VERSION = 6L
    private const val TAG = "Esquema"
    private const val TAMANO_LOTE = 400
    private val ROLES_DE_PERSONAL = listOf("SUPERADMIN", "ADMIN", "ADMIN_AREA", "AYUDANTE_AREA")
    private val TODOS_LOS_DIAS = listOf("LUN", "MAR", "MIE", "JUE", "VIE", "SAB", "DOM")

    /** Corre la migración si la base todavía no está en [VERSION]. Devuelve el resumen (vacío si no hizo falta). */
    suspend fun asegurar(db: FirebaseFirestore): Map<String, Long> {
        val config = db.collection("config").document("esquema")
        if ((config.get().await().getLong("version") ?: 0L) >= VERSION) return emptyMap()

        val resumen = migrar(db)
        config.set(
            mapOf(
                "version" to VERSION,
                "migradoEn" to FieldValue.serverTimestamp(),
                "camposAgregados" to resumen
            )
        ).await()
        Log.i(TAG, "Migración a la versión $VERSION terminada: $resumen")
        auditar(db)
        return resumen
    }

    private suspend fun leer(db: FirebaseFirestore, nombre: String): List<DocumentSnapshot> =
        db.collection(nombre).get().await().documents

    private suspend fun migrar(db: FirebaseFirestore): Map<String, Long> {
        val areas = leer(db, "areas").associateBy { it.id }
        val usuarios = leer(db, "usuarios")
        val usuariosPorId = usuarios.associateBy { it.id }
        val miembros = leer(db, "miembros")
        val membresias = leer(db, "membresias")
        val integrantesAnteriores = leer(db, "integrantesFamiliares")
        val reservas = leer(db, "reservas").associateBy { it.id }
        val herramientas = leer(db, "herramientas").associateBy { it.id }
        val inscripciones = leer(db, "inscripcionesTorneo")
        val torneos = leer(db, "torneos")

        val resumen = linkedMapOf<String, Long>()
        val empleados = leer(db, "empleados")
        val usuariosDeEmpleados = empleados.mapNotNull { it.getString("usuarioId") }.toSet()

        resumen["usuarios"] = completar(db.collection("usuarios"), usuarios) { doc ->
            mapOf(
                // Los perfiles creados desde Personal perdieron el rol al guardarse con el formulario anterior.
                "rol" to (if (doc.id in usuariosDeEmpleados) "AYUDANTE_AREA" else null),
                "email" to doc.getString("correo"),
                "codigoMiembro" to doc.getString("codigo"),
                "telefono" to "",
                "estado" to "ACTIVO"
            )
        }

        // Todo el personal lleva siempre sus campos laborales (vacíos si aún no se capturan), para que se vean en la base.
        val personal = usuarios.filter { it.getString("rol") in ROLES_DE_PERSONAL || it.id in usuariosDeEmpleados }
        resumen["personal_campos"] = completar(db.collection("usuarios"), personal) { doc ->
            mapOf(
                "tipoPersonal" to "",
                "turno" to "",
                "areaTrabajo" to "",
                "fechaIngreso" to (doc.getString("fechaRegistro") ?: "")
            )
        }
        resumen["personal_fusionado"] = fusionarEmpleados(db, empleados, usuarios, areas)

        resumen["miembros"] = completar(db.collection("miembros"), miembros) { doc ->
            mapOf("esTitular" to (doc.getString("parentesco") == "Titular"))
        }

        resumen["registroCodigos"] = crearRegistroDeCodigos(db, miembros)

        resumen["membresias"] = completar(db.collection("membresias"), membresias) { doc ->
            val deEstaMembresia = miembros.filter { it.getString("membresiaId") == doc.id }
            val titular = deEstaMembresia.firstOrNull { it.getBoolean("esTitular") == true || it.getString("parentesco") == "Titular" }
            val anteriores = integrantesAnteriores.count { it.getString("membresiaId") == doc.id }
            mapOf(
                "titularNombre" to (titular?.getString("nombre") ?: usuariosPorId[doc.getString("usuarioId")]?.getString("nombre")),
                "titularCodigo" to titular?.id,
                "totalPersonas" to (if (deEstaMembresia.isNotEmpty()) deEstaMembresia.size else 1 + anteriores).toLong(),
                "renovaciones" to 0L
            )
        }

        resumen["pagos"] = completar(
            db.collection("pagos"), leer(db, "pagos"),
            forzar = setOf("metodoPago")
        ) { doc ->
            mapOf(
                "concepto" to (if (doc.getString("membresiaId") != null) "MENSUALIDAD" else "OTRO"),
                "moneda" to "MXN",
                // Antes se guardaba "Tarjeta"; el estándar son valores en mayúsculas (EFECTIVO, TARJETA, TRANSFERENCIA).
                "metodoPago" to doc.getString("metodoPago")?.uppercase()?.takeIf { it != doc.getString("metodoPago") }
            )
        }

        resumen["reservas"] = completar(db.collection("reservas"), reservas.values.toList()) { doc ->
            val area = areas[doc.getString("areaId")]
            val inicio = aMinutos(doc.getString("horaInicio"))
            val fin = aMinutos(doc.getString("horaFin"))
            mapOf(
                "usuarioNombre" to usuariosPorId[doc.getString("usuarioId")]?.getString("nombre"),
                "areaNombre" to area?.getString("nombre"),
                "deporte" to area?.getString("tipo"),
                "duracionHoras" to (if (inicio != null && fin != null && fin > inicio) ((fin - inicio) / 60).toLong() else null),
                "personas" to 1L
            )
        }

        resumen["checkins"] = completar(db.collection("checkins"), leer(db, "checkins")) { doc ->
            val reserva = reservas[doc.getString("reservaId")]
            mapOf(
                "usuarioId" to reserva?.getString("usuarioId"),
                "areaId" to reserva?.getString("areaId"),
                "fecha" to reserva?.getString("fecha"),
                "metodo" to "MANUAL"
            )
        }

        resumen["notificaciones"] = completar(db.collection("notificaciones"), leer(db, "notificaciones")) { doc ->
            val tipo = doc.getString("tipo").orEmpty()
            mapOf(
                "leida" to doc.getBoolean("leido"),
                "titulo" to when (tipo) {
                    "RESERVA" -> "Reserva"
                    "MEMBRESIA" -> "Membresía"
                    "TORNEO" -> "Torneo"
                    else -> "Aviso"
                },
                "creadoEn" to doc.getString("fechaEnvio")?.let(::aTimestamp)
            )
        }

        resumen["herramientas"] = completar(db.collection("herramientas"), herramientas.values.toList()) { doc ->
            val deporte = areas[doc.getString("areaId")]?.getString("tipo")
            mapOf(
                "deporte" to deporte,
                // Antes apuntaba solo a una de las áreas del deporte aunque se usa en todas.
                "areaIds" to deporte?.let { d -> areas.values.filter { it.getString("tipo") == d }.map { it.id } },
                "stockMinimo" to 0L,
                "icono" to deporte?.let { Deportes.emojiDe(it) }
            )
        }

        resumen["materialAsignado"] = completar(db.collection("materialAsignado"), leer(db, "materialAsignado")) { doc ->
            val reserva = reservas[doc.getString("reservaId")]
            mapOf(
                "areaId" to reserva?.getString("areaId"),
                "fecha" to reserva?.getString("fecha"),
                "herramientaNombre" to herramientas[doc.getString("herramientaId")]?.getString("nombre"),
                "estado" to "ASIGNADO"
            )
        }

        val horarios = leer(db, "restriccionesHorario")
        val huerfanos = horarios.filter { areas[it.getString("areaId")] == null }
        borrar(db, huerfanos)
        resumen["restriccionesHorario"] = completar(
            db.collection("restriccionesHorario"), horarios - huerfanos.toSet()
        ) { doc ->
            val dia = doc.getString("diaSemana")
            mapOf(
                "areaNombre" to areas[doc.getString("areaId")]?.getString("nombre"),
                "dias" to (if (dia == null || dia == "TODOS") TODOS_LOS_DIAS else listOf(dia)),
                "horaApertura" to doc.getString("horaInicio"),
                "horaCierre" to doc.getString("horaFin"),
                "tipo" to "HORARIO_GENERAL",
                "activa" to true
            )
        }
        resumen["restriccionesHorario_huerfanas_borradas"] = huerfanos.size.toLong()

        resumen["torneos"] = completar(db.collection("torneos"), torneos) { doc ->
            mapOf(
                "areaNombre" to areas[doc.getString("areaId")]?.getString("nombre"),
                "inscritos" to inscripciones.count { it.getString("torneoId") == doc.id }.toLong()
            )
        }

        resumen["inscripcionesTorneo"] = completar(db.collection("inscripcionesTorneo"), inscripciones) { doc ->
            mapOf(
                "usuarioNombre" to usuariosPorId[doc.getString("usuarioId")]?.getString("nombre"),
                "torneoNombre" to torneos.firstOrNull { it.id == doc.getString("torneoId") }?.getString("nombre"),
                "estado" to "CONFIRMADA"
            )
        }

        resumen["campo_id_redundante_borrado"] =
            quitarCampo(db, usuarios, "id")

        resumen["precios"] = completar(db.collection("precios"), leer(db, "precios")) { mapOf("moneda" to "MXN") }

        return resumen
    }

    /**
     * Pasa los datos laborales de `empleados` al documento de `usuarios` de cada persona y borra `empleados`.
     * Turno: solo Matutino o Vespertino (los datos de ejemplo decían "Completo"). Área: el deporte del área
     * que tenía asignada (la cancha concreta ya no se guarda). Devuelve cuántos documentos de `empleados` se fusionaron.
     */
    private suspend fun fusionarEmpleados(
        db: FirebaseFirestore,
        empleados: List<DocumentSnapshot>,
        usuarios: List<DocumentSnapshot>,
        areas: Map<String, DocumentSnapshot>
    ): Long {
        if (empleados.isEmpty()) return 0L
        val empleadoPorUsuario = empleados.mapNotNull { e -> e.getString("usuarioId")?.let { it to e } }.toMap()
        completar(
            db.collection("usuarios"), usuarios.filter { it.id in empleadoPorUsuario },
            forzar = setOf("turno")
        ) { usuario ->
            val e = empleadoPorUsuario.getValue(usuario.id)
            val puesto = e.getString("puesto").orEmpty()
            val areaId = e.getString("areaAsignadaId") ?: e.getString("areaId")
            val turno = (e.getString("turno") ?: e.getString("tipoTurno"))
                ?.takeIf { it == "Matutino" || it == "Vespertino" } ?: "Matutino"
            mapOf(
                "tipoPersonal" to (e.getString("tipoPersonal") ?: Catalogos.tipoDePuesto(puesto)).ifBlank { null },
                // Solo se fuerza si el turno que ya tenía el usuario no es uno de los dos válidos.
                "turno" to turno.takeIf { usuario.getString("turno") !in listOf("Matutino", "Vespertino") },
                "areaTrabajo" to areaId?.let { areas[it]?.getString("tipo") },
                "fechaIngreso" to usuario.getString("fechaRegistro")
            )
        }
        borrar(db, empleados)
        return empleados.size.toLong()
    }

    /**
     * Agrega a cada documento los campos de [calcular] que todavía no tiene (los valores null se ignoran).
     * Las claves de [forzar] sí pueden reemplazar un valor existente. Devuelve cuántos campos se escribieron.
     */
    private suspend fun completar(
        db: FirebaseFirestore,
        coleccion: CollectionReference,
        documentos: List<DocumentSnapshot>,
        forzar: Set<String> = emptySet(),
        calcular: (DocumentSnapshot) -> Map<String, Any?>
    ): Long {
        var camposEscritos = 0L
        val cambios = documentos.mapNotNull { doc ->
            val nuevos = calcular(doc)
                .filter { (clave, valor) -> valor != null && (clave in forzar || !doc.contains(clave)) }
            if (nuevos.isEmpty()) null else doc.reference to nuevos
        }
        cambios.chunked(TAMANO_LOTE).forEach { grupo ->
            val lote = db.batch()
            grupo.forEach { (referencia, campos) ->
                lote.set(referencia, campos, SetOptions.merge())
                camposEscritos += campos.size
            }
            lote.commit().await()
        }
        return camposEscritos
    }

    private suspend fun completar(
        coleccion: CollectionReference,
        documentos: List<DocumentSnapshot>,
        forzar: Set<String> = emptySet(),
        calcular: (DocumentSnapshot) -> Map<String, Any?>
    ): Long = completar(coleccion.firestore, coleccion, documentos, forzar, calcular)

    /**
     * Desde la versión 6 los miembros se registran en la app con su código, y para saber si un código existe (y si ya
     * tiene cuenta) se consulta `registroCodigos/{código}`. Esto crea ese documento para los miembros que ya existían.
     * Devuelve cuántos documentos creó; los que ya existen no se tocan.
     */
    private suspend fun crearRegistroDeCodigos(db: FirebaseFirestore, miembros: List<DocumentSnapshot>): Long {
        val existentes = leer(db, "registroCodigos").map { it.id }.toSet()
        val faltantes = miembros.filter { it.id !in existentes && !it.getString("usuarioId").isNullOrBlank() }
        faltantes.chunked(TAMANO_LOTE).forEach { grupo ->
            val lote = db.batch()
            grupo.forEach { miembro ->
                lote.set(
                    db.collection("registroCodigos").document(miembro.id),
                    mapOf(
                        "usuarioId" to miembro.getString("usuarioId"),
                        "cuentaCreada" to false,
                        "creadoEn" to FieldValue.serverTimestamp()
                    )
                )
            }
            lote.commit().await()
        }
        return faltantes.size.toLong()
    }

    /** Quita [campo] de los documentos que lo tengan. Devuelve cuántos documentos cambiaron. */
    private suspend fun quitarCampo(db: FirebaseFirestore, documentos: List<DocumentSnapshot>, campo: String): Long {
        val conCampo = documentos.filter { it.contains(campo) }
        conCampo.chunked(TAMANO_LOTE).forEach { grupo ->
            val lote = db.batch()
            grupo.forEach { lote.update(it.reference, campo, FieldValue.delete()) }
            lote.commit().await()
        }
        return conCampo.size.toLong()
    }

    private suspend fun borrar(db: FirebaseFirestore, documentos: List<DocumentSnapshot>) {
        documentos.chunked(TAMANO_LOTE).forEach { grupo ->
            val lote = db.batch()
            grupo.forEach { lote.delete(it.reference) }
            lote.commit().await()
        }
    }

    /** Escribe en el log qué campos existen de verdad en cada colección (para compararlos con el diccionario). */
    private suspend fun auditar(db: FirebaseFirestore) {
        listOf(
            "areas", "checkins", "herramientas", "inscripcionesTorneo", "integrantesFamiliares",
            "materialAsignado", "membresias", "miembros", "notificaciones", "avisos", "registroCodigos", "pagos", "precios", "reservas",
            "restriccionesHorario", "torneos", "usuarios"
        ).forEach { nombre ->
            val docs = leer(db, nombre)
            val campos = docs.flatMap { it.data.orEmpty().keys }.groupingBy { it }.eachCount().toSortedMap()
            Log.i(TAG, "AUDITORIA $nombre (${docs.size} docs): " + campos.entries.joinToString(", ") { "${it.key}=${it.value}" })
        }
    }

    private fun aMinutos(hora: String?): Int? {
        val partes = hora?.split(":") ?: return null
        val h = partes.getOrNull(0)?.toIntOrNull() ?: return null
        val m = partes.getOrNull(1)?.toIntOrNull() ?: return null
        return h * 60 + m
    }

    private fun aTimestamp(texto: String): Timestamp? =
        runCatching { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(texto) }
            .getOrNull()?.let { Timestamp(it) }
}
