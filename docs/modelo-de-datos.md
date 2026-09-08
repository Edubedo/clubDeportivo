# Modelo de datos

Todas las entidades viven como `data class` / `enum class` en
`app/src/main/java/com/example/clubdeportivo/data/model/`. Son los objetos que circulan entre
los repositorios (ver [`arquitectura.md`](arquitectura.md#conectado-a-firebase)) y la UI, con la
misma forma que tienen los documentos guardados en Firestore. El `id` de cada entidad es un
`String` (así son los IDs de documento en Firestore) — la excepción es `PaqueteFamiliar`, que no
vive en la base de datos: son 3 opciones fijas definidas en `Catalogos.kt`.

## Usuario y roles (`Usuario.kt`)

```kotlin
data class Usuario(
    val id: String, // el uid que genera Firebase Authentication al crear la cuenta
    val nombre: String,
    val correo: String,
    val rol: Rol,
    val fotoUrl: String? = null
)

enum class Rol { SUPERADMIN, ADMIN, ADMIN_AREA, AYUDANTE_AREA, SOCIO, VISITANTE_EXTERNO }

data class Empleado(val id: String, val usuarioId: String, val puesto: String, val areaAsignadaId: String? = null)
```

Ver el detalle de cada rol y cómo crear una cuenta de prueba para cada uno en
[`usuarios-y-guia.md`](usuarios-y-guia.md).

## Áreas e instalaciones (`Instalaciones.kt`)

```kotlin
enum class DisponibilidadArea { DISPONIBLE, OCUPADA, MANTENIMIENTO }

data class Area(
    val id: String, val nombre: String, val tipo: String, val capacidad: Int,
    val disponibilidad: DisponibilidadArea, val permiteExternos: Boolean = false
)

data class Herramienta(val id: String, val nombre: String, val areaId: String, val cantidadTotal: Int, val cantidadDisponible: Int)

data class RestriccionHorario(val id: String, val areaId: String, val diaSemana: String, val horaInicio: String, val horaFin: String)
```

## Reservas (`Reserva.kt`)

```kotlin
enum class EstadoReserva { CONFIRMADA, PENDIENTE_APROBACION, CANCELADA, FINALIZADA }

data class Reserva(
    val id: String, val usuarioId: String, val areaId: String, val fecha: String,
    val horaInicio: String, val horaFin: String, val estado: EstadoReserva, val esExterno: Boolean = false
)

data class MaterialAsignado(val id: String, val reservaId: String, val herramientaId: String, val cantidad: Int)
data class Checkin(val id: String, val reservaId: String, val horaLlegada: String)
```

Las reservas de `VISITANTE_EXTERNO` nacen en `PENDIENTE_APROBACION` (`esExterno = true`) en vez de
`CONFIRMADA` directamente.

## Membresías (`Membresia.kt`)

```kotlin
enum class TipoMembresia { INDIVIDUAL, FAMILIAR, VISITA }
enum class EstadoMembresia { ACTIVA, VENCIDA, SUSPENDIDA }
enum class PlanIndividual { NINO, NORMAL, DELUXE }

/** No vive en Firestore: son 3 opciones fijas definidas en Catalogos.kt. */
data class PaqueteFamiliar(val id: Int, val nombre: String, val descripcion: String, val maxIntegrantes: Int, val precioMensual: Double)

data class Membresia(
    val id: String, val usuarioId: String, val tipo: TipoMembresia, val plan: PlanIndividual? = null,
    val paqueteFamiliarId: Int? = null, val precio: Double, val estado: EstadoMembresia,
    val fechaInicio: String, val fechaVencimiento: String
)

data class IntegranteFamiliar(val id: String, val membresiaId: String, val nombre: String, val parentesco: String)
```

## Torneos (`Torneo.kt`)

```kotlin
data class Torneo(
    val id: String, val nombre: String, val disciplina: String,
    val areaId: String, val fechaInicio: String, val fechaFin: String,
    val cupoMaximo: Int, val inscritos: Int // "inscritos" no se guarda: se cuenta en el momento
)

data class InscripcionTorneo(val id: String, val torneoId: String, val usuarioId: String, val fechaInscripcion: String)
```

Mientras dura un torneo, el área que ocupa (`areaId`) queda bloqueada para reservas individuales.

## Pagos y notificaciones (`Pago.kt`)

Estos dos todavía no los usa ninguna pantalla ni repositorio — quedan definidos para cuando se
agregue esa funcionalidad:

```kotlin
enum class EstadoPago { PAGADO, PENDIENTE, RECHAZADO }

data class Pago(val id: String, val usuarioId: String, val concepto: String, val monto: Double, val estado: EstadoPago, val fecha: String)
data class Notificacion(val id: String, val usuarioId: String, val titulo: String, val mensaje: String, val leida: Boolean, val fecha: String)
```

## Reglas de negocio y precios (`Catalogos.kt`)

Todos los números "mágicos" del negocio están centralizados en un único `object` para no
repetirlos en el código:

| Concepto | Valor |
|---|---|
| Precio visita puntual | $200 |
| Plan individual — Niños | $1500/mes |
| Plan individual — Normal | $1800/mes |
| Plan individual — Deluxe | $3500/mes |
| Paquete Familiar (2 adultos + 3 niños) | $7000/mes |
| Paquete Pareja (2 adultos) | $2999/mes |
| Paquete Niños (hermanos) | $2800/mes |
| Máx. reservas activas por socio | 3 |
| Anticipación mínima para reservar | 2 horas |
| Anticipación máxima para reservar | 7 días |
| Duración de una reserva | 1 hora |
| Cancelación sin penalización | hasta 4 horas antes |
| Inasistencias para bloqueo | 3 en 30 días → 7 días de bloqueo |
| Horario para visitantes externos aprobados | 08:00–20:00 |

El plan Deluxe además incluye prioridad en reservas, entrenamiento personalizado, agua y snacks.

Estos valores son configuración fija de la app (precios, reglas), no datos de un usuario en
particular — por eso siguen viviendo en código y no en Firestore.
