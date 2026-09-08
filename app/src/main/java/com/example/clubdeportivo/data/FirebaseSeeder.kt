package com.example.clubdeportivo.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Datos de catálogo (áreas y torneos de ejemplo) que en la versión de prueba venían
 * escritos directamente en el código (`FakeAreaRepository`, `FakeTorneoRepository`).
 * Firestore, a diferencia de eso, empieza completamente vacío — así que la primera vez
 * que la app lee una de estas colecciones y la encuentra vacía, la llena una única vez
 * con estos mismos datos de ejemplo. Las próximas veces no hace nada (la colección ya
 * no está vacía).
 */
object FirebaseSeeder {

    suspend fun asegurarAreas(db: FirebaseFirestore) {
        val coleccion = db.collection("areas")
        if (!coleccion.limit(1).get().await().isEmpty) return

        val areas = listOf(
            "1" to mapOf("nombre" to "Cancha de fútbol 1", "tipo" to "Fútbol", "capacidad" to 22L, "disponibilidad" to "DISPONIBLE", "permiteExternos" to true),
            "2" to mapOf("nombre" to "Cancha de fútbol 2", "tipo" to "Fútbol", "capacidad" to 22L, "disponibilidad" to "OCUPADA", "permiteExternos" to true),
            "3" to mapOf("nombre" to "Cancha de básquetbol 1", "tipo" to "Básquetbol", "capacidad" to 10L, "disponibilidad" to "DISPONIBLE", "permiteExternos" to true),
            "4" to mapOf("nombre" to "Cancha de básquetbol 2", "tipo" to "Básquetbol", "capacidad" to 10L, "disponibilidad" to "DISPONIBLE", "permiteExternos" to false),
            "5" to mapOf("nombre" to "Alberca 1", "tipo" to "Natación", "capacidad" to 40L, "disponibilidad" to "DISPONIBLE", "permiteExternos" to false),
            "6" to mapOf("nombre" to "Alberca 2", "tipo" to "Natación", "capacidad" to 40L, "disponibilidad" to "MANTENIMIENTO", "permiteExternos" to false),
            "7" to mapOf("nombre" to "Cancha de tenis 1", "tipo" to "Tenis", "capacidad" to 4L, "disponibilidad" to "OCUPADA", "permiteExternos" to true),
            "8" to mapOf("nombre" to "Cancha de tenis 2", "tipo" to "Tenis", "capacidad" to 4L, "disponibilidad" to "DISPONIBLE", "permiteExternos" to true),
            "9" to mapOf("nombre" to "Gimnasio", "tipo" to "Gimnasio", "capacidad" to 30L, "disponibilidad" to "DISPONIBLE", "permiteExternos" to false),
            "10" to mapOf("nombre" to "Salón de usos múltiples", "tipo" to "Clases grupales", "capacidad" to 25L, "disponibilidad" to "DISPONIBLE", "permiteExternos" to false)
        )
        for ((id, datos) in areas) {
            coleccion.document(id).set(datos).await()
        }
    }

    suspend fun asegurarTorneos(db: FirebaseFirestore) {
        val coleccion = db.collection("torneos")
        if (!coleccion.limit(1).get().await().isEmpty) return

        val torneos = listOf(
            "1" to mapOf("nombre" to "Copa Otoño de Fútbol", "disciplina" to "Fútbol", "areaId" to "1", "fechaInicio" to "2026-10-05", "fechaFin" to "2026-10-20", "cupoMaximo" to 16L),
            "2" to mapOf("nombre" to "Torneo Relámpago de Tenis", "disciplina" to "Tenis", "areaId" to "7", "fechaInicio" to "2026-09-25", "fechaFin" to "2026-09-27", "cupoMaximo" to 8L),
            "3" to mapOf("nombre" to "Liga Interna de Básquetbol", "disciplina" to "Básquetbol", "areaId" to "3", "fechaInicio" to "2026-11-01", "fechaFin" to "2026-12-15", "cupoMaximo" to 12L)
        )
        for ((id, datos) in torneos) {
            coleccion.document(id).set(datos).await()
        }
    }

    /**
     * Herramientas/material que se presta automáticamente al reservar (ver
     * `FirebaseReservaRepository.herramientaPorArea`). Solo existen 4: una por deporte,
     * compartida entre las 2 áreas de ese deporte (por eso `areaId` apunta nada más a la
     * primera de las dos, aunque el material se usa en ambas).
     */
    suspend fun asegurarHerramientas(db: FirebaseFirestore) {
        val coleccion = db.collection("herramientas")
        if (!coleccion.limit(1).get().await().isEmpty) return

        val herramientas = listOf(
            "10" to mapOf("nombre" to "Balón de fútbol", "areaId" to "1", "cantidadTotal" to 6L, "cantidadDisponible" to 6L, "estado" to "BUENO"),
            "11" to mapOf("nombre" to "Tabla de natación", "areaId" to "5", "cantidadTotal" to 10L, "cantidadDisponible" to 10L, "estado" to "BUENO"),
            "12" to mapOf("nombre" to "Balón de básquetbol", "areaId" to "3", "cantidadTotal" to 6L, "cantidadDisponible" to 6L, "estado" to "BUENO"),
            "15" to mapOf("nombre" to "Raqueta de tenis", "areaId" to "7", "cantidadTotal" to 8L, "cantidadDisponible" to 8L, "estado" to "BUENO")
        )
        for ((id, datos) in herramientas) {
            coleccion.document(id).set(datos).await()
        }
    }

    /**
     * Horario de operación por área. Nota: `FakeRestriccionHorarioRepository` (la que la app
     * usa de verdad, ver AppContainer) calcula esto mismo a partir del tipo de área sin leer
     * Firestore, porque es configuración fija. Esta colección se llena igual, en paralelo,
     * solo para que el modelo de datos completo exista en la base de datos remota.
     */
    suspend fun asegurarRestriccionesHorario(db: FirebaseFirestore) {
        val coleccion = db.collection("restriccionesHorario")
        if (!coleccion.limit(1).get().await().isEmpty) return

        val horarioPorArea = listOf(
            "1" to ("06:00" to "22:00"), "2" to ("06:00" to "22:00"),
            "3" to ("06:00" to "22:00"), "4" to ("06:00" to "22:00"),
            "5" to ("06:00" to "21:00"), "6" to ("06:00" to "21:00"),
            "7" to ("06:00" to "22:00"), "8" to ("06:00" to "22:00"),
            "9" to ("05:00" to "22:00"), "10" to ("07:00" to "21:00")
        )
        for ((areaId, horario) in horarioPorArea) {
            val (inicio, fin) = horario
            coleccion.document("rh-$areaId").set(
                mapOf(
                    "areaId" to areaId,
                    "diaSemana" to "TODOS",
                    "horaInicio" to inicio,
                    "horaFin" to fin,
                    "motivo" to "Horario general del área"
                )
            ).await()
        }
    }

    /**
     * Datos de ejemplo que dependen de cuentas reales ya creadas (uids de Firebase
     * Authentication) — a diferencia de las funciones de arriba, esta NO se auto-ejecuta:
     * se corrió una única vez a mano, el 2026-09-08, después de registrar 6 cuentas de
     * prueba (una por rol) desde la pantalla "Registrate". Los uids están fijos abajo.
     * Se deja el código como referencia de qué datos existen y por qué; no hace falta
     * volver a llamarla.
     */
    suspend fun sembrarDatosDeCuentasDemo(db: FirebaseFirestore) {
        val uidSocio = "dMRzAXd576eIIdpgUgjctruRKiI2"       // socio@prueba.com
        val uidAdminArea = "pM9sQ0AGGQRtr7SXPa11eYbN8dA2"    // areadmin@clubdeportivo.com
        val uidAyudante = "f1Cfw5en8qh8klPMH8shzaSsbqw1"     // ayudante@clubdeportivo.com
        val uidExterno = "qcNNBKvOmNOdvrC1v1R4gF6N0iA3"      // externo@clubdeportivo.com

        // empleados: personal asignado al área 1 (Cancha de fútbol 1)
        db.collection("empleados").document("empleado-1").set(
            mapOf("usuarioId" to uidAdminArea, "areaId" to "1", "puesto" to "Administrador de área", "tipoTurno" to "Matutino")
        ).await()
        db.collection("empleados").document("empleado-2").set(
            mapOf("usuarioId" to uidAyudante, "areaId" to "1", "puesto" to "Ayudante de área", "tipoTurno" to "Vespertino")
        ).await()

        // membresia + integrantes familiares del socio (paqueteFamiliarId 1 = "Familiar", ver Catalogos.kt)
        val idMembresia = "membresia-socio-familiar"
        db.collection("membresias").document(idMembresia).set(
            mapOf(
                "usuarioId" to uidSocio, "tipo" to "FAMILIAR", "paqueteFamiliarId" to 1L,
                "precio" to 7000.0, "estado" to "ACTIVA",
                "fechaInicio" to "2026-01-01", "fechaVencimiento" to "2026-12-31"
            )
        ).await()
        db.collection("integrantesFamiliares").document("integrante-1").set(
            mapOf("membresiaId" to idMembresia, "nombre" to "María Pérez", "parentesco" to "Cónyuge")
        ).await()
        db.collection("integrantesFamiliares").document("integrante-2").set(
            mapOf("membresiaId" to idMembresia, "nombre" to "Luis Pérez", "parentesco" to "Hijo")
        ).await()

        // pago de la membresía
        db.collection("pagos").document("pago-1").set(
            mapOf(
                "usuarioId" to uidSocio, "membresiaId" to idMembresia, "monto" to 7000.0,
                "fechaPago" to "2026-01-05", "metodoPago" to "Tarjeta", "estado" to "PAGADO"
            )
        ).await()

        // una reserva pendiente de aprobación (socio, sin material asignado todavía)
        db.collection("reservas").document("reserva-demo-2").set(
            mapOf(
                "usuarioId" to uidSocio, "areaId" to "3", "fecha" to "2026-09-12",
                "horaInicio" to "09:00", "horaFin" to "10:00",
                "estado" to "PENDIENTE_APROBACION", "esExterno" to false
            )
        ).await()

        // una reserva ya finalizada con check-in
        db.collection("reservas").document("reserva-demo-3").set(
            mapOf(
                "usuarioId" to uidSocio, "areaId" to "9", "fecha" to "2026-09-01",
                "horaInicio" to "18:00", "horaFin" to "19:00",
                "estado" to "FINALIZADA", "esExterno" to false
            )
        ).await()
        db.collection("checkins").document("checkin-1").set(
            mapOf("reservaId" to "reserva-demo-3", "fechaHora" to "2026-09-01T18:05:00", "tipo" to "ENTRADA")
        ).await()

        // reserva de un visitante externo, pendiente de aprobación
        db.collection("reservas").document("reserva-demo-4").set(
            mapOf(
                "usuarioId" to uidExterno, "areaId" to "1", "fecha" to "2026-09-11",
                "horaInicio" to "10:00", "horaFin" to "11:00",
                "estado" to "PENDIENTE_APROBACION", "esExterno" to true
            )
        ).await()

        // inscripción del socio a un torneo
        db.collection("inscripcionesTorneo").document("inscripcion-1").set(
            mapOf("torneoId" to "1", "usuarioId" to uidSocio, "fechaInscripcion" to "2026-09-08")
        ).await()

        // notificación de ejemplo
        db.collection("notificaciones").document("notificacion-1").set(
            mapOf(
                "usuarioId" to uidSocio, "tipo" to "RESERVA",
                "mensaje" to "Tu reserva en Cancha de fútbol 1 fue confirmada.",
                "fechaEnvio" to "2026-09-08T15:00:00", "leido" to false
            )
        ).await()
    }
}
