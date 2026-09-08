# Modelo de datos

Todas las entidades viven como `data class` / `enum class` en
`app/src/main/java/com/example/clubdeportivo/data/model/`. Hoy no hay base de datos: son los
objetos que circulan entre los repositorios "Fake" y la UI, con la forma que se espera que tenga
la futura respuesta del backend.

## Usuario y roles (`Usuario.kt`)

```kotlin
data class Usuario(
    val id: Int,
    val nombre: String,
    val correo: String,
    val rol: Rol,
    val fotoUrl: String? = null
)

enum class Rol { SUPERADMIN, ADMIN, ADMIN_AREA, AYUDANTE_AREA, SOCIO, VISITANTE_EXTERNO }

data class Empleado(val id: Int, val usuarioId: Int, val puesto: String, val areaAsignadaId: Int? = null)
```

Ver el detalle de cada rol y los usuarios de prueba para probarlos en
[`usuarios-y-guia.md`](usuarios-y-guia.md).

## Áreas e instalaciones (`Instalaciones.kt`)

```kotlin
enum class DisponibilidadArea { DISPONIBLE, OCUPADA, MANTENIMIENTO }

data class Area(
    val id: Int, val nombre: String, val tipo: String, val capacidad: Int,
    val disponibilidad: DisponibilidadArea, val permiteExternos: Boolean = false
)

data class Herramienta(val id: Int, val nombre: String, val areaId: Int, val cantidadTotal: Int, val cantidadDisponible: Int)

data class RestriccionHorario(val id: Int, val areaId: Int, val diaSemana: String, val horaInicio: String, val horaFin: String)
```

## Reservas (`Reserva.kt`)

```kotlin
enum class EstadoReserva { CONFIRMADA, PENDIENTE_APROBACION, CANCELADA, FINALIZADA }

data class Reserva(
    val id: Int, val usuarioId: Int, val areaId: Int, val fecha: String,
    val horaInicio: String, val horaFin: String, val estado: EstadoReserva, val esExterno: Boolean = false
)

data class MaterialAsignado(val id: Int, val reservaId: Int, val herramientaId: Int, val cantidad: Int)
data class Checkin(val id: Int, val reservaId: Int, val horaLlegada: String)
```

Las reservas de `VISITANTE_EXTERNO` nacen en `PENDIENTE_APROBACION` (`esExterno = true`) en vez de
`CONFIRMADA` directamente.

## Membresías (`Membresia.kt`)

```kotlin
enum class TipoMembresia { INDIVIDUAL, FAMILIAR, VISITA }
enum class EstadoMembresia { ACTIVA, VENCIDA, SUSPENDIDA }
enum class PlanIndividual { NINO, NORMAL, DELUXE }

data class PaqueteFamiliar(val id: Int, val nombre: String, val descripcion: String, val maxIntegrantes: Int, val precioMensual: Double)

data class Membresia(
    val id: Int, val usuarioId: Int, val tipo: TipoMembresia, val plan: PlanIndividual? = null,
    val paqueteFamiliarId: Int? = null, val precio: Double, val estado: EstadoMembresia,
    val fechaInicio: String, val fechaVencimiento: String
)

data class IntegranteFamiliar(val id: Int, val membresiaId: Int, val nombre: String, val parentesco: String)
```

## Torneos (`Torneo.kt`)

```kotlin
data class Torneo(
    val id: Int, val nombre: String, val disciplina: String,
    val areaId: Int, val fechaInicio: String, val fechaFin: String,
    val cupoMaximo: Int, val inscritos: Int
)

data class InscripcionTorneo(val id: Int, val torneoId: Int, val usuarioId: Int, val fechaInscripcion: String)
```

Mientras dura un torneo, el área que ocupa (`areaId`) queda bloqueada para reservas individuales.

## Pagos y notificaciones (`Pago.kt`)

```kotlin
enum class EstadoPago { PAGADO, PENDIENTE, RECHAZADO }

data class Pago(val id: Int, val usuarioId: Int, val concepto: String, val monto: Double, val estado: EstadoPago, val fecha: String)
data class Notificacion(val id: Int, val usuarioId: Int, val titulo: String, val mensaje: String, val leida: Boolean, val fecha: String)
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

Cuando exista un backend real, esta información pasa a vivir en la base de datos (tablas
`membresia` / `paquete_familiar`) y `Catalogos.kt` deja de ser necesario.
